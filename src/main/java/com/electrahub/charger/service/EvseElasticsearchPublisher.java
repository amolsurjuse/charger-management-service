package com.electrahub.charger.service;

import com.electrahub.charger.api.dto.ChargerAdminDtos;
import com.electrahub.charger.config.ElasticsearchProperties;
import com.electrahub.charger.repository.ChargerAdminRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EvseElasticsearchPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvseElasticsearchPublisher.class);

    private final ChargerAdminRepository chargerAdminRepository;
    private final ElasticsearchProperties elasticsearchProperties;
    private final ObjectMapper objectMapper;

    public EvseElasticsearchPublisher(
            ChargerAdminRepository chargerAdminRepository,
            ElasticsearchProperties elasticsearchProperties,
            ObjectMapper objectMapper
    ) {
        this.chargerAdminRepository = chargerAdminRepository;
        this.elasticsearchProperties = elasticsearchProperties;
        this.objectMapper = objectMapper;
    }

    public ChargerAdminDtos.EvseSearchPublishResponse publishCurrentEvses(boolean recreateIndex) {
        if (!elasticsearchProperties.isEnabled()) {
            throw new IllegalStateException("Elasticsearch publishing is disabled. Enable app.elasticsearch.enabled=true");
        }

        String indexName = normalizeIndexName(elasticsearchProperties.getEvseIndex());
        LOGGER.info("Publishing EVSE inventory to Elasticsearch index '{}', recreateIndex={}", indexName, recreateIndex);

        List<ChargerAdminRepository.EvseSearchExportRow> exportRows = chargerAdminRepository.listEvsesForSearchIndexing();
        Map<String, EvseAccumulator> byEvseId = groupByEvse(exportRows);
        List<String> warnings = new ArrayList<>();
        List<OcpiEvseSearchDocument> documents = buildDocuments(byEvseId, warnings);

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(elasticsearchProperties.getConnectTimeoutMs()))
                .build();

        ensureEvseIndex(httpClient, indexName, recreateIndex);

        long indexed = 0;
        for (OcpiEvseSearchDocument document : documents) {
            String id = buildDocumentId(document);
            try {
                HttpResult result = executeRequest(
                        httpClient,
                        "PUT",
                        "/" + encode(indexName) + "/_doc/" + encode(id) + "?refresh=wait_for",
                        objectMapper.writeValueAsString(document)
                );

                if (result.statusCode() >= 200 && result.statusCode() < 300) {
                    indexed++;
                } else {
                    String warning = "Failed to index EVSE document '%s' (status=%d)".formatted(id, result.statusCode());
                    warnings.add(warning);
                    LOGGER.warn("{}: {}", warning, result.body());
                }
            } catch (Exception ex) {
                String warning = "Failed to index EVSE document '%s': %s".formatted(id, ex.getMessage());
                warnings.add(warning);
                LOGGER.error("Error while indexing EVSE document {}", id, ex);
            }
        }

        long total = byEvseId.size();
        long failed = Math.max(0, total - indexed);
        OffsetDateTime publishedAt = OffsetDateTime.now(ZoneOffset.UTC);
        LOGGER.info(
                "EVSE Elasticsearch publish completed. index='{}', total={}, indexed={}, failed={}, warnings={}",
                indexName,
                total,
                indexed,
                failed,
                warnings.size()
        );

        return new ChargerAdminDtos.EvseSearchPublishResponse(
                indexName,
                total,
                indexed,
                failed,
                publishedAt,
                warnings
        );
    }

    private void ensureEvseIndex(HttpClient httpClient, String indexName, boolean recreateIndex) {
        if (recreateIndex) {
            LOGGER.info("Recreating Elasticsearch index '{}'", indexName);
            HttpResult deleteResult = executeRequest(httpClient, "DELETE", "/" + encode(indexName), null);
            if (deleteResult.statusCode() != 200 && deleteResult.statusCode() != 404) {
                throw new IllegalStateException(
                        "Unable to delete Elasticsearch index '%s'. status=%d body=%s"
                                .formatted(indexName, deleteResult.statusCode(), deleteResult.body())
                );
            }
        }

        HttpResult exists = executeRequest(httpClient, "HEAD", "/" + encode(indexName), null);
        if (exists.statusCode() == 200) {
            return;
        }
        if (exists.statusCode() != 404) {
            throw new IllegalStateException(
                    "Unable to check Elasticsearch index '%s'. status=%d body=%s"
                            .formatted(indexName, exists.statusCode(), exists.body())
            );
        }

        LOGGER.info("Creating Elasticsearch index '{}'", indexName);
        HttpResult createResult = executeRequest(httpClient, "PUT", "/" + encode(indexName), evseIndexMappingBody());
        if (createResult.statusCode() < 200 || createResult.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Unable to create Elasticsearch index '%s'. status=%d body=%s"
                            .formatted(indexName, createResult.statusCode(), createResult.body())
            );
        }
    }

    private Map<String, EvseAccumulator> groupByEvse(List<ChargerAdminRepository.EvseSearchExportRow> rows) {
        Map<String, EvseAccumulator> byEvseId = new LinkedHashMap<>();
        for (ChargerAdminRepository.EvseSearchExportRow row : rows) {
            EvseAccumulator accumulator = byEvseId.computeIfAbsent(
                    row.evseId(),
                    key -> new EvseAccumulator(
                            row.enterpriseCountryCode(),
                            row.enterprisePartyId(),
                            row.ocpiLocationId(),
                            row.evseId(),
                            row.evseUid(),
                            row.zone(),
                            row.capabilities(),
                            row.evseEnabled(),
                            row.evseUpdatedAt(),
                            row.chargerId(),
                            row.chargerDisplayName()
                    )
            );

            if (row.connectorId() != null) {
                accumulator.connectors().add(toConnector(row));
                accumulator.maxLastUpdated(chooseLatest(row.connectorUpdatedAt(), row.evseUpdatedAt()));
            }
        }
        return byEvseId;
    }

    private List<OcpiEvseSearchDocument> buildDocuments(Map<String, EvseAccumulator> byEvseId, List<String> warnings) {
        List<OcpiEvseSearchDocument> documents = new ArrayList<>();
        for (EvseAccumulator value : byEvseId.values()) {
            if (!StringUtils.hasText(value.countryCode())
                    || !StringUtils.hasText(value.partyId())
                    || !StringUtils.hasText(value.locationId())
                    || !StringUtils.hasText(value.evseUid())) {
                warnings.add("Skipping EVSE '%s' because OCPI identifying fields are incomplete".formatted(value.evseId()));
                continue;
            }

            OffsetDateTime lastUpdated = value.lastUpdated() == null
                    ? OffsetDateTime.now(ZoneOffset.UTC)
                    : value.lastUpdated();

            documents.add(new OcpiEvseSearchDocument(
                    value.countryCode().toUpperCase(),
                    value.partyId().toUpperCase(),
                    value.locationId().toUpperCase(),
                    value.evseUid().toUpperCase(),
                    null,
                    value.evseEnabled() ? "AVAILABLE" : "INOPERATIVE",
                    parseCsvUpper(value.capabilities()),
                    value.zone(),
                    Collections.unmodifiableList(value.connectors()),
                    lastUpdated,
                    value.evseId(),
                    value.chargerId(),
                    value.chargerDisplayName()
            ));
        }
        return documents;
    }

    private OcpiConnectorSearchDocument toConnector(ChargerAdminRepository.EvseSearchExportRow row) {
        Integer maxElectricPower = null;
        if (row.connectorMaxPowerKw() != null) {
            maxElectricPower = row.connectorMaxPowerKw()
                    .multiply(BigDecimal.valueOf(1000))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
        }

        OffsetDateTime lastUpdated = row.connectorUpdatedAt() == null
                ? row.evseUpdatedAt()
                : row.connectorUpdatedAt();

        return new OcpiConnectorSearchDocument(
                row.connectorId(),
                row.connectorStandard(),
                row.connectorFormat(),
                row.connectorPowerType(),
                null,
                null,
                maxElectricPower,
                row.connectorEnabled() ? "AVAILABLE" : "UNAVAILABLE",
                parseCsv(row.connectorTariffIds()),
                lastUpdated
        );
    }

    private OffsetDateTime chooseLatest(OffsetDateTime first, OffsetDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

    private String buildDocumentId(OcpiEvseSearchDocument document) {
        return "%s-%s-%s-%s".formatted(
                safePart(document.countryCode()),
                safePart(document.partyId()),
                safePart(document.locationId()),
                safePart(document.uid())
        );
    }

    private String safePart(String value) {
        if (value == null) {
            return "UNKNOWN";
        }
        String normalized = value.trim().toUpperCase().replaceAll("[^A-Z0-9_-]", "_");
        return normalized.isBlank() ? "UNKNOWN" : normalized;
    }

    private String normalizeIndexName(String indexName) {
        String candidate = StringUtils.hasText(indexName) ? indexName : "ocpi-evses-v1";
        return candidate.trim().toLowerCase().replaceAll("[^a-z0-9-_]+", "-");
    }

    private List<String> parseCsv(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String token : raw.split(",")) {
            if (token == null) {
                continue;
            }
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return List.copyOf(values);
    }

    private List<String> parseCsvUpper(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String token : raw.split(",")) {
            if (token == null) {
                continue;
            }
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed.toUpperCase());
            }
        }
        return List.copyOf(values);
    }

    private HttpResult executeRequest(HttpClient httpClient, String method, String path, String body) {
        try {
            URI uri = URI.create(trimTrailingSlash(elasticsearchProperties.getUrl()) + path);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofMillis(elasticsearchProperties.getRequestTimeoutMs()))
                    .header("Accept", "application/json");

            if (StringUtils.hasText(elasticsearchProperties.getUsername())) {
                String auth = elasticsearchProperties.getUsername() + ":" + Objects.requireNonNullElse(elasticsearchProperties.getPassword(), "");
                builder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8)));
            }

            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("Content-Type", "application/json");
                builder.method(method, HttpRequest.BodyPublishers.ofString(body));
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Elasticsearch request was interrupted", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to communicate with Elasticsearch: " + ex.getMessage(), ex);
        }
    }

    private String trimTrailingSlash(String input) {
        if (input == null) {
            return "";
        }
        String value = input.trim();
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String evseIndexMappingBody() {
        return """
                {
                  "settings": {
                    "number_of_shards": 1,
                    "number_of_replicas": 1
                  },
                  "mappings": {
                    "dynamic": false,
                    "properties": {
                      "country_code": { "type": "keyword" },
                      "party_id": { "type": "keyword" },
                      "location_id": { "type": "keyword" },
                      "uid": { "type": "keyword" },
                      "evse_id": { "type": "keyword" },
                      "status": { "type": "keyword" },
                      "capabilities": { "type": "keyword" },
                      "physical_reference": { "type": "keyword" },
                      "last_updated": { "type": "date" },
                      "connectors": {
                        "type": "nested",
                        "properties": {
                          "id": { "type": "keyword" },
                          "standard": { "type": "keyword" },
                          "format": { "type": "keyword" },
                          "power_type": { "type": "keyword" },
                          "max_voltage": { "type": "integer" },
                          "max_amperage": { "type": "integer" },
                          "max_electric_power": { "type": "integer" },
                          "status": { "type": "keyword" },
                          "tariff_ids": { "type": "keyword" },
                          "last_updated": { "type": "date" }
                        }
                      },
                      "electrahub_internal_evse_id": { "type": "keyword" },
                      "charger_id": { "type": "keyword" },
                      "charger_display_name": {
                        "type": "text",
                        "fields": {
                          "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                          }
                        }
                      }
                    }
                  }
                }
                """;
    }

    private record HttpResult(
            int statusCode,
            String body
    ) {
    }

    private static final class EvseAccumulator {
        private final String countryCode;
        private final String partyId;
        private final String locationId;
        private final String evseId;
        private final String evseUid;
        private final String zone;
        private final String capabilities;
        private final boolean evseEnabled;
        private OffsetDateTime lastUpdated;
        private final String chargerId;
        private final String chargerDisplayName;
        private final List<OcpiConnectorSearchDocument> connectors = new ArrayList<>();

        private EvseAccumulator(
                String countryCode,
                String partyId,
                String locationId,
                String evseId,
                String evseUid,
                String zone,
                String capabilities,
                boolean evseEnabled,
                OffsetDateTime lastUpdated,
                String chargerId,
                String chargerDisplayName
        ) {
            this.countryCode = countryCode;
            this.partyId = partyId;
            this.locationId = locationId;
            this.evseId = evseId;
            this.evseUid = evseUid;
            this.zone = zone;
            this.capabilities = capabilities;
            this.evseEnabled = evseEnabled;
            this.lastUpdated = lastUpdated;
            this.chargerId = chargerId;
            this.chargerDisplayName = chargerDisplayName;
        }

        private String countryCode() {
            return countryCode;
        }

        private String partyId() {
            return partyId;
        }

        private String locationId() {
            return locationId;
        }

        private String evseId() {
            return evseId;
        }

        private String evseUid() {
            return evseUid;
        }

        private String zone() {
            return zone;
        }

        private String capabilities() {
            return capabilities;
        }

        private boolean evseEnabled() {
            return evseEnabled;
        }

        private OffsetDateTime lastUpdated() {
            return lastUpdated;
        }

        private void maxLastUpdated(OffsetDateTime candidate) {
            if (candidate == null) {
                return;
            }
            if (lastUpdated == null || candidate.isAfter(lastUpdated)) {
                lastUpdated = candidate;
            }
        }

        private String chargerId() {
            return chargerId;
        }

        private String chargerDisplayName() {
            return chargerDisplayName;
        }

        private List<OcpiConnectorSearchDocument> connectors() {
            return connectors;
        }
    }

    private record OcpiEvseSearchDocument(
            @JsonProperty("country_code")
            String countryCode,
            @JsonProperty("party_id")
            String partyId,
            @JsonProperty("location_id")
            String locationId,
            @JsonProperty("uid")
            String uid,
            @JsonProperty("evse_id")
            String evseId,
            @JsonProperty("status")
            String status,
            @JsonProperty("capabilities")
            List<String> capabilities,
            @JsonProperty("physical_reference")
            String physicalReference,
            @JsonProperty("connectors")
            List<OcpiConnectorSearchDocument> connectors,
            @JsonProperty("last_updated")
            OffsetDateTime lastUpdated,
            @JsonProperty("electrahub_internal_evse_id")
            String electrahubInternalEvseId,
            @JsonProperty("charger_id")
            String chargerId,
            @JsonProperty("charger_display_name")
            String chargerDisplayName
    ) {
    }

    private record OcpiConnectorSearchDocument(
            @JsonProperty("id")
            String id,
            @JsonProperty("standard")
            String standard,
            @JsonProperty("format")
            String format,
            @JsonProperty("power_type")
            String powerType,
            @JsonProperty("max_voltage")
            Integer maxVoltage,
            @JsonProperty("max_amperage")
            Integer maxAmperage,
            @JsonProperty("max_electric_power")
            Integer maxElectricPower,
            @JsonProperty("status")
            String status,
            @JsonProperty("tariff_ids")
            List<String> tariffIds,
            @JsonProperty("last_updated")
            OffsetDateTime lastUpdated
    ) {
    }
}

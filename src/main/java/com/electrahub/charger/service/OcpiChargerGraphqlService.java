package com.electrahub.charger.service;

import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.RequestOptions;
import co.elastic.clients.transport.rest5_client.low_level.Response;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.electrahub.charger.config.ElasticsearchProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetchingFieldSelectionSet;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class OcpiChargerGraphqlService {

    private static final String INDEX_NAME = "ocpi-connectors";
    private static final int DEFAULT_LIMIT = 25;
    private static final int MAX_LIMIT = 200;
    private static final int SEARCH_BATCH_SIZE = 500;

    private final ElasticsearchProperties elasticsearchProperties;
    private final ObjectProvider<Rest5Client> restClientProvider;
    private final ObjectMapper objectMapper;
    private final ChargerSessionLookupService chargerSessionLookupService;
    private final BillingTariffClient billingTariffClient;

    public OcpiChargerGraphqlService(
            ElasticsearchProperties elasticsearchProperties,
            ObjectProvider<Rest5Client> restClientProvider,
            ObjectMapper objectMapper,
            ChargerSessionLookupService chargerSessionLookupService,
            BillingTariffClient billingTariffClient
    ) {
        this.elasticsearchProperties = elasticsearchProperties;
        this.restClientProvider = restClientProvider;
        this.objectMapper = objectMapper;
        this.chargerSessionLookupService = chargerSessionLookupService;
        this.billingTariffClient = billingTariffClient;
    }

    public List<OcpiChargerGraphqlDto> listChargers(
            OcpiChargerFilter filter,
            DataFetchingFieldSelectionSet selectionSet
    ) {
        Rest5Client client = elasticsearchClient();
        if (client == null) {
            return List.of();
        }

        int limit = normalizeLimit(filter.limit());
        int offset = normalizeOffset(filter.offset());
        Map<String, Object> baseQuery = buildBaseQuery(filter);

        List<String> pageChargerIds;
        try {
            pageChargerIds = fetchPagedChargerIds(client, baseQuery, limit, offset);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to query charger ids from Elasticsearch", ex);
        }

        if (pageChargerIds.isEmpty()) {
            return List.of();
        }

        Set<String> sourceIncludes = resolveSourceIncludes(selectionSet);
        sourceIncludes.add("evse.chargerId");
        sourceIncludes.add("evse.id");
        sourceIncludes.add("connector.id");
        sourceIncludes.add("status");

        List<Map<String, Object>> connectorRows;
        try {
            connectorRows = fetchConnectorRows(client, baseQuery, pageChargerIds, sourceIncludes);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to query charger connectors from Elasticsearch", ex);
        }

        List<OcpiChargerGraphqlDto> chargers = toChargerDtos(pageChargerIds, connectorRows);
        chargers = enrichWithTariffDetails(chargers);
        if (chargers.isEmpty() || !selectionRequiresCurrentSession(selectionSet)) {
            return chargers;
        }

        Map<String, ChargerSessionLookupService.ActiveSessionDto> sessionsByChargerId =
                chargerSessionLookupService.findActiveSessionsByChargerIds(
                        chargers.stream().map(OcpiChargerGraphqlDto::chargerId).toList()
                );

        if (sessionsByChargerId.isEmpty()) {
            return chargers;
        }

        return chargers.stream()
                .map(charger -> withCurrentSession(charger, sessionsByChargerId.get(charger.chargerId())))
                .toList();
    }

    /**
     * Resolves a single OCPI charger view by charger id, connector id, or both.
     * When both identifiers are supplied they are applied together.
     *
     * @param chargerId optional charger id filter
     * @param connectorId optional connector id filter
     * @param selectionSet GraphQL selection set for source projection decisions
     * @return matched charger projection or {@code null} when no match exists
     */
    public OcpiChargerGraphqlDto viewCharger(
            String chargerId,
            String connectorId,
            DataFetchingFieldSelectionSet selectionSet
    ) {
        String normalizedChargerId = normalizeText(chargerId);
        String normalizedConnectorId = normalizeText(connectorId);
        if (normalizedChargerId.isBlank() && normalizedConnectorId.isBlank()) {
            return null;
        }

        OcpiChargerFilter filter = new OcpiChargerFilter(
                null,
                null,
                null,
                normalizedChargerId.isBlank() ? null : normalizedChargerId,
                null,
                null,
                normalizedConnectorId.isBlank() ? null : normalizedConnectorId,
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                0
        );

        List<OcpiChargerGraphqlDto> chargers = listChargers(filter, selectionSet);
        if (chargers.isEmpty()) {
            return null;
        }
        return chargers.getFirst();
    }

    private Rest5Client elasticsearchClient() {
        if (!elasticsearchProperties.isEnabled()) {
            return null;
        }
        return restClientProvider.getIfAvailable();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private int normalizeOffset(Integer offset) {
        if (offset == null) {
            return 0;
        }
        return Math.max(0, offset);
    }

    private Map<String, Object> buildBaseQuery(OcpiChargerFilter filter) {
        List<Map<String, Object>> filters = new ArrayList<>();

        addKeywordFilter(filters, "countryCode", filter.countryCode(), true);
        addKeywordFilter(filters, "partyId", filter.partyId(), true);
        addKeywordFilter(filters, "location.ocpiLocationId", filter.ocpiLocationId(), true);
        addCaseFlexibleKeywordFilter(filters, "evse.chargerId", filter.chargerId(), true);
        addKeywordFilter(filters, "location.city", filter.city(), false);
        addKeywordFilter(filters, "status", filter.connectorStatus(), true);
        addCaseFlexibleKeywordFilter(filters, "connector.id", filter.connectorId(), true);
        addKeywordFilter(filters, "connector.standard", filter.connectorStandard(), true);
        addKeywordFilter(filters, "connector.format", filter.connectorFormat(), true);
        addKeywordFilter(filters, "connector.powerType", filter.powerType(), true);
        addKeywordFilter(filters, "connector.tariffIds", filter.tariffId(), true);

        if (filter.active() != null) {
            filters.add(termFilter("active", filter.active()));
        }

        String search = normalizeText(filter.search());
        List<Map<String, Object>> must = new ArrayList<>();
        if (!search.isBlank()) {
            Map<String, Object> simpleQueryString = new LinkedHashMap<>();
            simpleQueryString.put("query", search);
            simpleQueryString.put("fields", List.of(
                    "location.name",
                    "location.address",
                    "location.city",
                    "evse.chargerName",
                    "evse.chargerId",
                    "connector.id"
            ));
            must.add(Map.of("simple_query_string", simpleQueryString));
        }

        if (filters.isEmpty() && must.isEmpty()) {
            return Map.of("match_all", Map.of());
        }

        Map<String, Object> bool = new LinkedHashMap<>();
        if (!filters.isEmpty()) {
            bool.put("filter", filters);
        }
        if (!must.isEmpty()) {
            bool.put("must", must);
        }
        return Map.of("bool", bool);
    }

    private boolean addKeywordFilter(
            List<Map<String, Object>> filters,
            String field,
            String rawValue,
            boolean uppercase
    ) {
        String value = normalizeText(rawValue);
        if (value.isBlank()) {
            return false;
        }

        String normalized = uppercase ? value.toUpperCase(Locale.ROOT) : value;
        filters.add(termFilter(keywordField(field), normalized));
        return true;
    }

    private boolean addCaseFlexibleKeywordFilter(
            List<Map<String, Object>> filters,
            String field,
            String rawValue,
            boolean uppercase
    ) {
        String value = normalizeText(rawValue);
        if (value.isBlank()) {
            return false;
        }

        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(value);

        if (uppercase) {
            candidates.add(value.toUpperCase(Locale.ROOT));
            candidates.add(value.toLowerCase(Locale.ROOT));
        }

        if (candidates.size() == 1) {
            filters.add(termFilter(keywordField(field), candidates.iterator().next()));
            return true;
        }

        List<Map<String, Object>> should = candidates.stream()
                .map(candidate -> termFilter(keywordField(field), candidate))
                .toList();
        filters.add(Map.of("bool", Map.of(
                "should", should,
                "minimum_should_match", 1
        )));
        return true;
    }

    private Map<String, Object> termFilter(String field, Object value) {
        return Map.of("term", Map.of(field, value));
    }

    private List<String> fetchPagedChargerIds(
            Rest5Client client,
            Map<String, Object> baseQuery,
            int limit,
            int offset
    ) throws IOException {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("from", offset);
        requestBody.put("size", limit);
        requestBody.put("query", baseQuery);
        requestBody.put("collapse", Map.of("field", keywordField("evse.chargerId")));
        requestBody.put("_source", Map.of("includes", List.of("evse.chargerId")));
        requestBody.put("sort", List.of(sortField(keywordField("evse.chargerId"))));

        List<String> chargerIds = new ArrayList<>();
        for (Map<String, Object> source : searchHits(client, requestBody)) {
            String chargerId = asString(readPath(source, "evse.chargerId"));
            if (!chargerId.isBlank()) {
                chargerIds.add(chargerId);
            }
        }
        return chargerIds;
    }

    private List<Map<String, Object>> fetchConnectorRows(
            Rest5Client client,
            Map<String, Object> baseQuery,
            List<String> chargerIds,
            Set<String> sourceIncludes
    ) throws IOException {
        if (chargerIds.isEmpty()) {
            return List.of();
        }

        Map<String, Object> chargerIdsQuery = Map.of("terms", Map.of(keywordField("evse.chargerId"), chargerIds));
        Map<String, Object> finalQuery = Map.of("bool", Map.of("filter", List.of(baseQuery, chargerIdsQuery)));

        List<Map<String, Object>> rows = new ArrayList<>();
        int from = 0;

        while (true) {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("from", from);
            requestBody.put("size", SEARCH_BATCH_SIZE);
            requestBody.put("query", finalQuery);
            requestBody.put("_source", Map.of("includes", new ArrayList<>(sourceIncludes)));
            requestBody.put("sort", List.of(
                    sortField(keywordField("evse.chargerId")),
                    sortField(keywordField("evse.id")),
                    sortField(keywordField("connector.id"))
            ));

            List<Map<String, Object>> hits = searchHits(client, requestBody);
            if (hits.isEmpty()) {
                break;
            }

            rows.addAll(hits);

            if (hits.size() < SEARCH_BATCH_SIZE) {
                break;
            }
            from += hits.size();
        }

        return rows;
    }

    private List<Map<String, Object>> searchHits(
            Rest5Client client,
            Map<String, Object> requestBody
    ) throws IOException {
        String payload = performSearchRequest(client, requestBody);
        Map<String, Object> responseMap = objectMapper.readValue(payload, new TypeReference<>() {
        });
        Object hitsValue = readPath(responseMap, "hits.hits");
        if (!(hitsValue instanceof Collection<?> collection) || collection.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> sources = new ArrayList<>();
        for (Object hitObject : collection) {
            Map<String, Object> hitMap = map(hitObject);
            Map<String, Object> sourceMap = map(hitMap.get("_source"));
            if (!sourceMap.isEmpty()) {
                sources.add(sourceMap);
            }
        }
        return sources;
    }

    private String performSearchRequest(
            Rest5Client client,
            Map<String, Object> requestBody
    ) throws IOException {
        Request request = new Request("POST", "/" + INDEX_NAME + "/_search");
        request.setJsonEntity(objectMapper.writeValueAsString(requestBody));

        RequestOptions.Builder options = RequestOptions.DEFAULT.toBuilder();
        options.removeHeader("Accept");
        options.removeHeader("Content-Type");
        options.addHeader("Accept", "application/json");
        options.addHeader("Content-Type", "application/json");
        request.setOptions(options);

        Response response = client.performRequest(request);
        String responseBody = response.getEntity() == null
                ? ""
                : new String(EntityUtils.toByteArray(response.getEntity()), StandardCharsets.UTF_8);

        if (response.getStatusCode() >= 300) {
            throw new IOException("Elasticsearch request failed with status "
                    + response.getStatusCode() + ": " + responseBody);
        }
        return responseBody;
    }

    private Map<String, Object> sortField(String field) {
        return Map.of(field, Map.of("order", "asc"));
    }

    private String keywordField(String field) {
        if (field == null || field.isBlank() || field.endsWith(".keyword")) {
            return field;
        }
        return field + ".keyword";
    }

    private Set<String> resolveSourceIncludes(DataFetchingFieldSelectionSet selectionSet) {
        Set<String> includes = new LinkedHashSet<>();

        includes.add("evse.chargerId");

        if (selectionSet.contains("countryCode")) {
            includes.add("countryCode");
        }
        if (selectionSet.contains("partyId")) {
            includes.add("partyId");
        }
        if (selectionSet.contains("chargerName")) {
            includes.add("evse.chargerName");
        }
        if (selectionSet.contains("status") || selectionSet.contains("available")) {
            includes.add("status");
        }
        if (selectionSet.contains("lastUpdated")) {
            includes.add("lastUpdated");
        }

        if (selectionSet.contains("location/*") || selectionSet.contains("location/**")) {
            includes.add("location.id");
            includes.add("location.ocpiLocationId");
            includes.add("location.name");
            includes.add("location.address");
            includes.add("location.city");
            includes.add("location.latitude");
            includes.add("location.longitude");
        }

        if (selectionSet.contains("evses/*") || selectionSet.contains("evses/**")) {
            includes.add("evse.id");
            includes.add("evse.uid");
            includes.add("evse.zone");
            includes.add("evse.capabilities");
            includes.add("status");
            includes.add("connector.id");
            includes.add("connector.standard");
            includes.add("connector.format");
            includes.add("connector.powerType");
            includes.add("connector.maxPowerKw");
            includes.add("connector.tariffIds");
        }

        if (selectionSet.contains("pricing/*") || selectionSet.contains("pricing/**")) {
            includes.add("connector.tariffIds");
        }

        return includes;
    }

    private List<OcpiChargerGraphqlDto> toChargerDtos(
            List<String> orderedChargerIds,
            List<Map<String, Object>> rows
    ) {
        Map<String, ChargerAccumulator> chargers = new LinkedHashMap<>();
        for (String chargerId : orderedChargerIds) {
            chargers.put(chargerId, new ChargerAccumulator(chargerId));
        }

        for (Map<String, Object> row : rows) {
            String chargerId = asString(readPath(row, "evse.chargerId"));
            if (chargerId.isBlank()) {
                continue;
            }

            ChargerAccumulator charger = chargers.computeIfAbsent(chargerId, ChargerAccumulator::new);
            charger.accept(row);
        }

        return orderedChargerIds.stream()
                .map(chargers::get)
                .filter(Objects::nonNull)
                .map(ChargerAccumulator::toDto)
                .toList();
    }

    private boolean selectionRequiresCurrentSession(DataFetchingFieldSelectionSet selectionSet) {
        return selectionSet.contains("currentSession")
                || selectionSet.contains("currentSession/*")
                || selectionSet.contains("currentSession/**");
    }

    private OcpiChargerGraphqlDto withCurrentSession(
            OcpiChargerGraphqlDto charger,
            ChargerSessionLookupService.ActiveSessionDto activeSession
    ) {
        OcpiCurrentSessionGraphqlDto currentSession = activeSession == null
                ? null
                : new OcpiCurrentSessionGraphqlDto(
                activeSession.id(),
                activeSession.userId(),
                activeSession.status(),
                activeSession.startedAt()
        );

        List<OcpiEvseGraphqlDto> evses = activeSession == null
                ? charger.evses()
                : overlayActiveSession(charger.evses(), activeSession);
        int totalPorts = evses.stream().mapToInt(evse -> evse.connectors().size()).sum();
        int availablePorts = (int) evses.stream()
                .flatMap(evse -> evse.connectors().stream())
                .filter(OcpiConnectorGraphqlDto::available)
                .count();
        int busyPorts = Math.max(0, totalPorts - availablePorts);
        String status = activeSession == null ? charger.status() : "CHARGING";

        return new OcpiChargerGraphqlDto(
                charger.countryCode(),
                charger.partyId(),
                charger.chargerId(),
                charger.chargerName(),
                status,
                availablePorts > 0,
                availablePorts,
                busyPorts,
                charger.lastUpdated(),
                charger.location(),
                evses,
                charger.pricing(),
                currentSession
        );
    }

    private List<OcpiEvseGraphqlDto> overlayActiveSession(
            List<OcpiEvseGraphqlDto> evses,
            ChargerSessionLookupService.ActiveSessionDto activeSession
    ) {
        String activeConnectorRef = normalizeText(activeSession.connectorRef());
        boolean hasConnectorRef = !activeConnectorRef.isBlank();
        boolean matched = evses.stream()
                .flatMap(evse -> evse.connectors().stream())
                .anyMatch(connector -> hasConnectorRef && activeConnectorRef.equalsIgnoreCase(normalizeText(connector.id())));

        boolean[] fallbackUsed = {matched};
        return evses.stream()
                .map(evse -> overlayActiveSession(evse, activeConnectorRef, fallbackUsed))
                .toList();
    }

    private OcpiEvseGraphqlDto overlayActiveSession(
            OcpiEvseGraphqlDto evse,
            String activeConnectorRef,
            boolean[] fallbackUsed
    ) {
        List<OcpiConnectorGraphqlDto> connectors = evse.connectors();
        List<OcpiConnectorGraphqlDto> overlaidConnectors = connectors.stream()
                .map(connector -> {
                    boolean matches = !activeConnectorRef.isBlank()
                            && activeConnectorRef.equalsIgnoreCase(normalizeText(connector.id()));
                    boolean fallback = !fallbackUsed[0];
                    if (!matches && !fallback) {
                        return connector;
                    }
                    fallbackUsed[0] = true;
                    return new OcpiConnectorGraphqlDto(
                            connector.id(),
                            connector.standard(),
                            connector.format(),
                            connector.powerType(),
                            connector.maxPowerKw(),
                            connector.tariffIds(),
                            connector.tariffs(),
                            "CHARGING",
                            false
                    );
                })
                .toList();

        String evseStatus = overlaidConnectors.stream().anyMatch(connector -> "CHARGING".equalsIgnoreCase(connector.status()))
                ? "CHARGING"
                : evse.status();
        return new OcpiEvseGraphqlDto(
                evse.id(),
                evse.uid(),
                evse.zone(),
                evse.capabilities(),
                evseStatus,
                overlaidConnectors
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private Object readPath(Map<String, Object> source, String path) {
        Object current = source;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private String asString(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Instant asInstant(Object value) {
        String text = asString(value);
        if (text.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(text);
        } catch (Exception ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(v -> !v.isEmpty())
                    .toList();
        }
        if (value instanceof String text) {
            if (text.isBlank()) {
                return List.of();
            }
            return List.of(text.trim());
        }
        return List.of(String.valueOf(value));
    }

    private static String normalizeStatus(Set<String> statuses) {
        if (statuses.isEmpty()) {
            return "UNKNOWN";
        }
        if (statuses.contains("CHARGING")) {
            return "CHARGING";
        }
        if (statuses.contains("OCCUPIED")) {
            return "OCCUPIED";
        }
        if (statuses.contains("AVAILABLE")) {
            return "AVAILABLE";
        }
        if (statuses.contains("RESERVED")) {
            return "RESERVED";
        }
        if (statuses.contains("PLANNED")) {
            return "PLANNED";
        }
        if (statuses.contains("INOPERATIVE")) {
            return "INOPERATIVE";
        }
        if (statuses.contains("OUTOFORDER")) {
            return "OUTOFORDER";
        }
        if (statuses.contains("UNAVAILABLE")) {
            return "UNAVAILABLE";
        }
        return statuses.iterator().next();
    }

    private static boolean isAvailableStatus(String status) {
        return "AVAILABLE".equalsIgnoreCase(status);
    }

    private List<OcpiChargerGraphqlDto> enrichWithTariffDetails(List<OcpiChargerGraphqlDto> chargers) {
        // Collect all unique tariff IDs
        Set<String> allTariffIds = new LinkedHashSet<>();
        for (OcpiChargerGraphqlDto charger : chargers) {
            if (charger.pricing() != null) {
                allTariffIds.addAll(charger.pricing().tariffIds());
            }
            for (OcpiEvseGraphqlDto evse : charger.evses()) {
                for (OcpiConnectorGraphqlDto connector : evse.connectors()) {
                    allTariffIds.addAll(connector.tariffIds());
                }
            }
        }

        if (allTariffIds.isEmpty()) return chargers;

        Map<String, BillingTariffClient.TariffDetailDto> resolved = billingTariffClient.resolveTariffs(allTariffIds);
        if (resolved.isEmpty()) return chargers;

        return chargers.stream().map(charger -> enrichCharger(charger, resolved)).toList();
    }

    private OcpiChargerGraphqlDto enrichCharger(
            OcpiChargerGraphqlDto charger,
            Map<String, BillingTariffClient.TariffDetailDto> resolvedTariffs
    ) {
        // Enrich pricing tariffs
        OcpiPricingGraphqlDto enrichedPricing = charger.pricing() != null
                ? new OcpiPricingGraphqlDto(
                        charger.pricing().tariffIds(),
                        charger.pricing().tariffIds().stream()
                                .map(resolvedTariffs::get)
                                .filter(Objects::nonNull)
                                .map(this::tariffDetailToDto)
                                .toList()
                )
                : new OcpiPricingGraphqlDto(List.of(), List.of());

        // Enrich evse connectors
        List<OcpiEvseGraphqlDto> enrichedEvses = charger.evses().stream()
                .map(evse -> enrichEvse(evse, resolvedTariffs))
                .toList();

        return new OcpiChargerGraphqlDto(
                charger.countryCode(),
                charger.partyId(),
                charger.chargerId(),
                charger.chargerName(),
                charger.status(),
                charger.available(),
                charger.availablePorts(),
                charger.busyPorts(),
                charger.lastUpdated(),
                charger.location(),
                enrichedEvses,
                enrichedPricing,
                charger.currentSession()
        );
    }

    private OcpiEvseGraphqlDto enrichEvse(
            OcpiEvseGraphqlDto evse,
            Map<String, BillingTariffClient.TariffDetailDto> resolvedTariffs
    ) {
        List<OcpiConnectorGraphqlDto> enrichedConnectors = evse.connectors().stream()
                .map(connector -> enrichConnector(connector, resolvedTariffs))
                .toList();

        return new OcpiEvseGraphqlDto(
                evse.id(),
                evse.uid(),
                evse.zone(),
                evse.capabilities(),
                evse.status(),
                enrichedConnectors
        );
    }

    private OcpiConnectorGraphqlDto enrichConnector(
            OcpiConnectorGraphqlDto connector,
            Map<String, BillingTariffClient.TariffDetailDto> resolvedTariffs
    ) {
        List<OcpiTariffGraphqlDto> tariffs = connector.tariffIds().stream()
                .map(resolvedTariffs::get)
                .filter(Objects::nonNull)
                .map(this::tariffDetailToDto)
                .toList();

        return new OcpiConnectorGraphqlDto(
                connector.id(),
                connector.standard(),
                connector.format(),
                connector.powerType(),
                connector.maxPowerKw(),
                connector.tariffIds(),
                tariffs,
                connector.status(),
                connector.available()
        );
    }

    private OcpiTariffGraphqlDto tariffDetailToDto(BillingTariffClient.TariffDetailDto detail) {
        return new OcpiTariffGraphqlDto(
                detail.tariffId(),
                detail.name(),
                detail.description(),
                detail.currency(),
                detail.energyPrice() != null ? detail.energyPrice().doubleValue() : null,
                detail.timePrice() != null ? detail.timePrice().doubleValue() : null,
                detail.parkingPrice() != null ? detail.parkingPrice().doubleValue() : null,
                detail.flatFee() != null ? detail.flatFee().doubleValue() : null,
                detail.minPrice() != null ? detail.minPrice().doubleValue() : null,
                detail.maxPrice() != null ? detail.maxPrice().doubleValue() : null
        );
    }

    public record OcpiChargerFilter(
            String countryCode,
            String partyId,
            String ocpiLocationId,
            String chargerId,
            String city,
            String connectorStatus,
            String connectorId,
            String connectorStandard,
            String connectorFormat,
            String powerType,
            String tariffId,
            Boolean active,
            String search,
            Integer limit,
            Integer offset
    ) {
    }

    public record OcpiChargerGraphqlDto(
            String countryCode,
            String partyId,
            String chargerId,
            String chargerName,
            String status,
            boolean available,
            int availablePorts,
            int busyPorts,
            String lastUpdated,
            OcpiLocationGraphqlDto location,
            List<OcpiEvseGraphqlDto> evses,
            OcpiPricingGraphqlDto pricing,
            OcpiCurrentSessionGraphqlDto currentSession
    ) {
    }

    public record OcpiLocationGraphqlDto(
            String id,
            String ocpiLocationId,
            String name,
            String address,
            String city,
            OcpiCoordinatesGraphqlDto coordinates
    ) {
    }

    public record OcpiCoordinatesGraphqlDto(
            Double latitude,
            Double longitude
    ) {
    }

    public record OcpiEvseGraphqlDto(
            String id,
            String uid,
            String zone,
            List<String> capabilities,
            String status,
            List<OcpiConnectorGraphqlDto> connectors
    ) {
    }

    public record OcpiConnectorGraphqlDto(
            String id,
            String standard,
            String format,
            String powerType,
            BigDecimal maxPowerKw,
            List<String> tariffIds,
            List<OcpiTariffGraphqlDto> tariffs,
            String status,
            boolean available
    ) {
    }

    public record OcpiTariffGraphqlDto(
            String tariffId,
            String name,
            String description,
            String currency,
            Double energyPrice,
            Double timePrice,
            Double parkingPrice,
            Double flatFee,
            Double minPrice,
            Double maxPrice
    ) {
    }

    public record OcpiPricingGraphqlDto(
            List<String> tariffIds,
            List<OcpiTariffGraphqlDto> tariffs
    ) {
    }

    public record OcpiCurrentSessionGraphqlDto(
            String id,
            String userId,
            String status,
            String startedAt
    ) {
    }

    private final class ChargerAccumulator {
        private final String chargerId;
        private String countryCode;
        private String partyId;
        private String chargerName;
        private Instant lastUpdated;
        private OcpiLocationGraphqlDto location;
        private final Set<String> statuses = new LinkedHashSet<>();
        private final Set<String> pricingTariffIds = new LinkedHashSet<>();
        private final Map<String, EvseAccumulator> evses = new LinkedHashMap<>();

        private ChargerAccumulator(String chargerId) {
            this.chargerId = chargerId;
        }

        private void accept(Map<String, Object> row) {
            String rowCountryCode = asString(readPath(row, "countryCode"));
            String rowPartyId = asString(readPath(row, "partyId"));
            String rowChargerName = asString(readPath(row, "evse.chargerName"));
            String rowStatus = asString(readPath(row, "status")).toUpperCase(Locale.ROOT);
            Instant rowLastUpdated = asInstant(readPath(row, "lastUpdated"));

            if (!rowCountryCode.isBlank()) {
                countryCode = rowCountryCode;
            }
            if (!rowPartyId.isBlank()) {
                partyId = rowPartyId;
            }
            if (!rowChargerName.isBlank()) {
                chargerName = rowChargerName;
            }
            if (!rowStatus.isBlank()) {
                statuses.add(rowStatus);
            }
            if (rowLastUpdated != null && (lastUpdated == null || rowLastUpdated.isAfter(lastUpdated))) {
                lastUpdated = rowLastUpdated;
            }

            Map<String, Object> locationMap = map(readPath(row, "location"));
            if (!locationMap.isEmpty()) {
                location = new OcpiLocationGraphqlDto(
                        asString(locationMap.get("id")),
                        asString(locationMap.get("ocpiLocationId")),
                        asString(locationMap.get("name")),
                        asString(locationMap.get("address")),
                        asString(locationMap.get("city")),
                        new OcpiCoordinatesGraphqlDto(
                                asDouble(locationMap.get("latitude")),
                                asDouble(locationMap.get("longitude"))
                        )
                );
            }

            String evseId = asString(readPath(row, "evse.id"));
            if (evseId.isBlank()) {
                return;
            }

            EvseAccumulator evseAccumulator = evses.computeIfAbsent(evseId, key -> new EvseAccumulator(
                    key,
                    asString(readPath(row, "evse.uid")),
                    asString(readPath(row, "evse.zone")),
                    asStringList(readPath(row, "evse.capabilities"))
            ));
            evseAccumulator.addConnector(
                    asString(readPath(row, "connector.id")),
                    asString(readPath(row, "connector.standard")),
                    asString(readPath(row, "connector.format")),
                    asString(readPath(row, "connector.powerType")),
                    asBigDecimal(readPath(row, "connector.maxPowerKw")),
                    asStringList(readPath(row, "connector.tariffIds")),
                    rowStatus
            );

            pricingTariffIds.addAll(asStringList(readPath(row, "connector.tariffIds")));
        }

        private OcpiChargerGraphqlDto toDto() {
            List<OcpiEvseGraphqlDto> evseDtos = evses.values().stream()
                    .map(EvseAccumulator::toDto)
                    .toList();

            String chargerStatus = normalizeStatus(statuses);
            boolean available = isAvailableStatus(chargerStatus);
            int availablePorts = 0;
            int totalPorts = 0;
            for (EvseAccumulator evseAccumulator : evses.values()) {
                totalPorts += evseAccumulator.connectors.size();
                availablePorts += evseAccumulator.connectors.values().stream()
                        .mapToInt(connector -> connector.available() ? 1 : 0)
                        .sum();
            }

            return new OcpiChargerGraphqlDto(
                    countryCode,
                    partyId,
                    chargerId,
                    chargerName,
                    chargerStatus,
                    available,
                    availablePorts,
                    Math.max(totalPorts - availablePorts, 0),
                    lastUpdated == null ? null : lastUpdated.toString(),
                    location,
                    evseDtos,
                    new OcpiPricingGraphqlDto(pricingTariffIds.stream().toList(), List.of()),
                    null
            );
        }
    }

    private final class EvseAccumulator {
        private final String id;
        private final String uid;
        private final String zone;
        private final List<String> capabilities;
        private final Set<String> statuses = new LinkedHashSet<>();
        private final Map<String, OcpiConnectorGraphqlDto> connectors = new LinkedHashMap<>();

        private EvseAccumulator(String id, String uid, String zone, List<String> capabilities) {
            this.id = id;
            this.uid = uid;
            this.zone = zone;
            this.capabilities = capabilities;
        }

        private void addConnector(
                String connectorId,
                String standard,
                String format,
                String powerType,
                BigDecimal maxPowerKw,
                List<String> tariffIds,
                String status
        ) {
            if (status != null && !status.isBlank()) {
                statuses.add(status.toUpperCase(Locale.ROOT));
            }

            if (connectorId == null || connectorId.isBlank()) {
                return;
            }

            String normalizedStatus = status == null || status.isBlank()
                    ? "UNKNOWN"
                    : status.toUpperCase(Locale.ROOT);

            connectors.put(connectorId, new OcpiConnectorGraphqlDto(
                    connectorId,
                    standard,
                    format,
                    powerType,
                    maxPowerKw,
                    tariffIds,
                    List.of(),
                    normalizedStatus,
                    isAvailableStatus(normalizedStatus)
            ));
        }

        private OcpiEvseGraphqlDto toDto() {
            return new OcpiEvseGraphqlDto(
                    id,
                    uid,
                    zone,
                    capabilities,
                    normalizeStatus(statuses),
                    connectors.values().stream().toList()
            );
        }
    }
}

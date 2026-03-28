package com.electrahub.charger.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.electrahub.charger.config.ElasticsearchProperties;
import graphql.schema.DataFetchingFieldSelectionSet;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
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
    private final ObjectProvider<ElasticsearchClient> elasticsearchClientProvider;

    public OcpiChargerGraphqlService(
            ElasticsearchProperties elasticsearchProperties,
            ObjectProvider<ElasticsearchClient> elasticsearchClientProvider
    ) {
        this.elasticsearchProperties = elasticsearchProperties;
        this.elasticsearchClientProvider = elasticsearchClientProvider;
    }

    public List<OcpiChargerGraphqlDto> listChargers(
            OcpiChargerFilter filter,
            DataFetchingFieldSelectionSet selectionSet
    ) {
        ElasticsearchClient client = elasticsearchClient();
        if (client == null) {
            return List.of();
        }

        int limit = normalizeLimit(filter.limit());
        int offset = normalizeOffset(filter.offset());
        Query baseQuery = buildBaseQuery(filter);

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

        return toChargerDtos(pageChargerIds, connectorRows);
    }

    public OcpiChargerGraphqlDto viewCharger(
            String chargerId,
            DataFetchingFieldSelectionSet selectionSet
    ) {
        if (chargerId == null || chargerId.isBlank()) {
            return null;
        }

        OcpiChargerFilter filter = new OcpiChargerFilter(
                null,
                null,
                null,
                chargerId,
                null,
                null,
                null,
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

    private ElasticsearchClient elasticsearchClient() {
        if (!elasticsearchProperties.isEnabled()) {
            return null;
        }
        return elasticsearchClientProvider.getIfAvailable();
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

    private Query buildBaseQuery(OcpiChargerFilter filter) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        boolean hasClause = false;

        hasClause |= addKeywordFilter(bool, "countryCode", filter.countryCode(), true);
        hasClause |= addKeywordFilter(bool, "partyId", filter.partyId(), true);
        hasClause |= addKeywordFilter(bool, "location.ocpiLocationId", filter.ocpiLocationId(), true);
        hasClause |= addKeywordFilter(bool, "evse.chargerId", filter.chargerId(), true);
        hasClause |= addKeywordFilter(bool, "location.city", filter.city(), false);
        hasClause |= addKeywordFilter(bool, "status", filter.connectorStatus(), true);
        hasClause |= addKeywordFilter(bool, "connector.id", filter.connectorId(), true);
        hasClause |= addKeywordFilter(bool, "connector.standard", filter.connectorStandard(), true);
        hasClause |= addKeywordFilter(bool, "connector.format", filter.connectorFormat(), true);
        hasClause |= addKeywordFilter(bool, "connector.powerType", filter.powerType(), true);
        hasClause |= addKeywordFilter(bool, "connector.tariffIds", filter.tariffId(), true);

        if (filter.active() != null) {
            bool.filter(Query.of(q -> q.term(t -> t.field("active").value(filter.active()))));
            hasClause = true;
        }

        String search = normalizeText(filter.search());
        if (!search.isBlank()) {
            bool.must(Query.of(q -> q.simpleQueryString(s -> s
                    .query(search)
                    .fields(
                            "location.name",
                            "location.address",
                            "location.city",
                            "evse.chargerName",
                            "evse.chargerId",
                            "connector.id"
                    ))));
            hasClause = true;
        }

        if (!hasClause) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        return Query.of(q -> q.bool(bool.build()));
    }

    private boolean addKeywordFilter(BoolQuery.Builder bool, String field, String rawValue, boolean uppercase) {
        String value = normalizeText(rawValue);
        if (value.isBlank()) {
            return false;
        }

        String normalized = uppercase ? value.toUpperCase(Locale.ROOT) : value;
        bool.filter(Query.of(q -> q.term(t -> t.field(field).value(FieldValue.of(normalized)))));
        return true;
    }

    private List<String> fetchPagedChargerIds(
            ElasticsearchClient client,
            Query baseQuery,
            int limit,
            int offset
    ) throws IOException {
        SearchResponse<Map> response = client.search(s -> s
                        .index(INDEX_NAME)
                        .from(offset)
                        .size(limit)
                        .query(baseQuery)
                        .collapse(c -> c.field("evse.chargerId"))
                        .source(src -> src.filter(f -> f.includes("evse.chargerId")))
                        .sort(sort -> sort.field(f -> f.field("evse.chargerId").order(SortOrder.Asc))),
                Map.class);

        List<String> chargerIds = new ArrayList<>();
        for (Hit<Map> hit : response.hits().hits()) {
            Map<String, Object> source = map(hit.source());
            String chargerId = asString(readPath(source, "evse.chargerId"));
            if (!chargerId.isBlank()) {
                chargerIds.add(chargerId);
            }
        }
        return chargerIds;
    }

    private List<Map<String, Object>> fetchConnectorRows(
            ElasticsearchClient client,
            Query baseQuery,
            List<String> chargerIds,
            Set<String> sourceIncludes
    ) throws IOException {
        if (chargerIds.isEmpty()) {
            return List.of();
        }

        Query chargerIdsQuery = Query.of(q -> q.terms(t -> t
                .field("evse.chargerId")
                .terms(values -> values.value(chargerIds.stream().map(FieldValue::of).toList()))
        ));

        Query finalQuery = Query.of(q -> q.bool(b -> b
                .filter(baseQuery)
                .filter(chargerIdsQuery)
        ));

        List<Map<String, Object>> rows = new ArrayList<>();
        int from = 0;

        while (true) {
            int pageFrom = from;
            SearchResponse<Map> response = client.search(s -> s
                            .index(INDEX_NAME)
                            .from(pageFrom)
                            .size(SEARCH_BATCH_SIZE)
                            .query(finalQuery)
                            .source(src -> src.filter(f -> f.includes(new ArrayList<>(sourceIncludes))))
                            .sort(sort -> sort.field(f -> f.field("evse.chargerId").order(SortOrder.Asc)))
                            .sort(sort -> sort.field(f -> f.field("evse.id").order(SortOrder.Asc)))
                            .sort(sort -> sort.field(f -> f.field("connector.id").order(SortOrder.Asc))),
                    Map.class);

            List<Hit<Map>> hits = response.hits().hits();
            if (hits.isEmpty()) {
                break;
            }

            for (Hit<Map> hit : hits) {
                Map<String, Object> source = map(hit.source());
                if (!source.isEmpty()) {
                    rows.add(source);
                }
            }

            if (hits.size() < SEARCH_BATCH_SIZE) {
                break;
            }
            from += hits.size();
        }

        return rows;
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
        if (statuses.contains("AVAILABLE")) {
            return "AVAILABLE";
        }
        if (statuses.contains("CHARGING")) {
            return "CHARGING";
        }
        if (statuses.contains("OCCUPIED")) {
            return "OCCUPIED";
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
            String lastUpdated,
            OcpiLocationGraphqlDto location,
            List<OcpiEvseGraphqlDto> evses,
            OcpiPricingGraphqlDto pricing
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
            String status,
            boolean available
    ) {
    }

    public record OcpiPricingGraphqlDto(
            List<String> tariffIds
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

            return new OcpiChargerGraphqlDto(
                    countryCode,
                    partyId,
                    chargerId,
                    chargerName,
                    chargerStatus,
                    available,
                    lastUpdated == null ? null : lastUpdated.toString(),
                    location,
                    evseDtos,
                    new OcpiPricingGraphqlDto(pricingTariffIds.stream().toList())
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

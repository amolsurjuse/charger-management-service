package com.electrahub.charger.repository;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.charger.api.dto.ChargerDtos;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ChargerRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChargerRepository.class);


    private static final TypeReference<List<ChargerDtos.ConnectorConfiguration>> CONNECTORS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, String>> METADATA_TYPE = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Executes charger repository for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param jdbcTemplate input consumed by ChargerRepository.
     * @param objectMapper input consumed by ChargerRepository.
     */
    public ChargerRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering ChargerRepository#ChargerRepository");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering ChargerRepository#ChargerRepository with debug context");
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes exists by id for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param chargerId input consumed by existsById.
     * @return result produced by existsById.
     */
    public boolean existsById(String chargerId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from chargers where charger_id = :chargerId",
                new MapSqlParameterSource("chargerId", chargerId),
                Integer.class
        );
        return count != null && count > 0;
    }

    /**
     * Creates create for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param request input consumed by create.
     * @return result produced by create.
     */
    public ChargerDtos.ChargerResponse create(ChargerDtos.ChargerUpsertRequest request) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        MapSqlParameterSource params = toCommonParams(request, now);
        jdbcTemplate.update("""
                insert into chargers (
                    charger_id,
                    display_name,
                    charge_point_identity,
                    vendor,
                    model,
                    serial_number,
                    site_name,
                    ocpp_version,
                    status,
                    firmware_version,
                    last_heartbeat_at,
                    enabled,
                    connectors_json,
                    configuration_json,
                    metadata_json,
                    created_at,
                    updated_at
                ) values (
                    :chargerId,
                    :displayName,
                    :chargePointIdentity,
                    :vendor,
                    :model,
                    :serialNumber,
                    :siteName,
                    :ocppVersion,
                    :status,
                    :firmwareVersion,
                    :lastHeartbeatAt,
                    :enabled,
                    :connectorsJson,
                    :configurationJson,
                    :metadataJson,
                    :createdAt,
                    :updatedAt
                )
                """, params);
        return findById(request.chargerId()).orElseThrow();
    }

    /**
     * Updates update for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param chargerId input consumed by update.
     * @param request input consumed by update.
     * @return result produced by update.
     */
    public ChargerDtos.ChargerResponse update(String chargerId, ChargerDtos.ChargerUpsertRequest request) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        MapSqlParameterSource params = toCommonParams(request, now);
        params.addValue("chargerId", chargerId);
        jdbcTemplate.update("""
                update chargers
                   set display_name = :displayName,
                       charge_point_identity = :chargePointIdentity,
                       vendor = :vendor,
                       model = :model,
                       serial_number = :serialNumber,
                       site_name = :siteName,
                       ocpp_version = :ocppVersion,
                       status = :status,
                       firmware_version = :firmwareVersion,
                       last_heartbeat_at = :lastHeartbeatAt,
                       enabled = :enabled,
                       connectors_json = :connectorsJson,
                       configuration_json = :configurationJson,
                       metadata_json = :metadataJson,
                       updated_at = :updatedAt
                 where charger_id = :chargerId
                """, params);
        return findById(chargerId).orElseThrow();
    }

    /**
     * Retrieves find by id for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param chargerId input consumed by findById.
     * @return result produced by findById.
     */
    public Optional<ChargerDtos.ChargerResponse> findById(String chargerId) {
        List<ChargerDtos.ChargerResponse> results = jdbcTemplate.query(
                "select * from chargers where charger_id = :chargerId",
                new MapSqlParameterSource("chargerId", chargerId),
                chargerRowMapper()
        );
        return results.stream().findFirst();
    }

    /**
     * Retrieves find all for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param status input consumed by findAll.
     * @param ocppVersion input consumed by findAll.
     * @param siteName input consumed by findAll.
     * @param enabled input consumed by findAll.
     * @return result produced by findAll.
     */
    public List<ChargerDtos.ChargerResponse> findAll(String status, String ocppVersion, String siteName, Boolean enabled) {
        StringBuilder sql = new StringBuilder("select * from chargers where 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (status != null && !status.isBlank()) {
            sql.append(" and status = :status");
            params.addValue("status", status);
        }
        if (ocppVersion != null && !ocppVersion.isBlank()) {
            sql.append(" and ocpp_version = :ocppVersion");
            params.addValue("ocppVersion", ocppVersion);
        }
        if (siteName != null && !siteName.isBlank()) {
            sql.append(" and site_name = :siteName");
            params.addValue("siteName", siteName);
        }
        if (enabled != null) {
            sql.append(" and enabled = :enabled");
            params.addValue("enabled", enabled);
        }
        sql.append(" order by updated_at desc, charger_id asc");
        return jdbcTemplate.query(sql.toString(), params, chargerRowMapper());
    }

    /**
     * Updates update status for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param chargerId input consumed by updateStatus.
     * @param request input consumed by updateStatus.
     * @return result produced by updateStatus.
     */
    public ChargerDtos.ChargerResponse updateStatus(String chargerId, ChargerDtos.ChargerStatusUpdateRequest request) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("chargerId", chargerId)
                .addValue("status", request.status().name())
                .addValue("enabled", request.enabled())
                .addValue("lastHeartbeatAt", toTimestamp(request.lastHeartbeatAt()))
                .addValue("updatedAt", Timestamp.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()));
        jdbcTemplate.update("""
                update chargers
                   set status = :status,
                       enabled = coalesce(:enabled, enabled),
                       last_heartbeat_at = coalesce(:lastHeartbeatAt, last_heartbeat_at),
                       updated_at = :updatedAt
                 where charger_id = :chargerId
                """, params);
        return findById(chargerId).orElseThrow();
    }

    /**
     * Removes delete for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param chargerId input consumed by delete.
     */
    public void delete(String chargerId) {
        jdbcTemplate.update(
                "delete from chargers where charger_id = :chargerId",
                new MapSqlParameterSource("chargerId", chargerId)
        );
    }

    /**
     * Executes to common params for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param request input consumed by toCommonParams.
     * @param now input consumed by toCommonParams.
     * @return result produced by toCommonParams.
     */
    private MapSqlParameterSource toCommonParams(ChargerDtos.ChargerUpsertRequest request, OffsetDateTime now) {
        return new MapSqlParameterSource()
                .addValue("chargerId", request.chargerId())
                .addValue("displayName", request.displayName())
                .addValue("chargePointIdentity", request.chargePointIdentity())
                .addValue("vendor", request.vendor())
                .addValue("model", request.model())
                .addValue("serialNumber", request.serialNumber())
                .addValue("siteName", request.siteName())
                .addValue("ocppVersion", request.ocppVersion().name())
                .addValue("status", request.status().name())
                .addValue("firmwareVersion", request.settings().firmware().currentVersion())
                .addValue("lastHeartbeatAt", toTimestamp(request.lastHeartbeatAt()))
                .addValue("enabled", request.enabled() == null || request.enabled())
                .addValue("connectorsJson", writeJson(request.connectors()))
                .addValue("configurationJson", writeJson(request.settings()))
                .addValue("metadataJson", writeJson(defaultMetadata(request.metadata())))
                .addValue("createdAt", Timestamp.from(now.toInstant()))
                .addValue("updatedAt", Timestamp.from(now.toInstant()));
    }

    /**
     * Executes charger row mapper for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @return result produced by chargerRowMapper.
     */
    private RowMapper<ChargerDtos.ChargerResponse> chargerRowMapper() {
        return (rs, rowNum) -> new ChargerDtos.ChargerResponse(
                rs.getString("charger_id"),
                rs.getString("display_name"),
                rs.getString("charge_point_identity"),
                rs.getString("vendor"),
                rs.getString("model"),
                rs.getString("serial_number"),
                rs.getString("site_name"),
                ChargerDtos.OcppVersion.valueOf(rs.getString("ocpp_version")),
                ChargerDtos.ChargerStatus.valueOf(rs.getString("status")),
                rs.getBoolean("enabled"),
                toOffsetDateTime(rs.getTimestamp("last_heartbeat_at")),
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("updated_at")),
                readConnectors(rs),
                readSettings(rs),
                readMetadata(rs)
        );
    }

    /**
     * Retrieves read connectors for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param rs input consumed by readConnectors.
     * @return result produced by readConnectors.
     */
    private List<ChargerDtos.ConnectorConfiguration> readConnectors(ResultSet rs) throws SQLException {
        return readJson(rs.getString("connectors_json"), CONNECTORS_TYPE, List.of());
    }

    /**
     * Retrieves read settings for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param rs input consumed by readSettings.
     * @return result produced by readSettings.
     */
    private ChargerDtos.ChargerSettings readSettings(ResultSet rs) throws SQLException {
        return readJson(rs.getString("configuration_json"), ChargerDtos.ChargerSettings.class, null);
    }

    /**
     * Retrieves read metadata for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param rs input consumed by readMetadata.
     * @return result produced by readMetadata.
     */
    private Map<String, String> readMetadata(ResultSet rs) throws SQLException {
        return readJson(rs.getString("metadata_json"), METADATA_TYPE, Map.of());
    }

    /**
     * Retrieves read json for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param payload input consumed by readJson.
     * @param targetType input consumed by readJson.
     * @param fallback input consumed by readJson.
     * @return result produced by readJson.
     */
    private <T> T readJson(String payload, Class<T> targetType, T fallback) {
        if (payload == null || payload.isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readValue(payload, targetType);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize stored charger payload", ex);
        }
    }

    /**
     * Retrieves read json for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param payload input consumed by readJson.
     * @param targetType input consumed by readJson.
     * @param fallback input consumed by readJson.
     * @return result produced by readJson.
     */
    private <T> T readJson(String payload, TypeReference<T> targetType, T fallback) {
        if (payload == null || payload.isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readValue(payload, targetType);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize stored charger payload", ex);
        }
    }

    /**
     * Executes write json for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param value input consumed by writeJson.
     * @return result produced by writeJson.
     */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize charger payload", ex);
        }
    }

    /**
     * Executes default metadata for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param Map<String input consumed by defaultMetadata.
     * @param metadata input consumed by defaultMetadata.
     * @return result produced by defaultMetadata.
     */
    private Map<String, String> defaultMetadata(Map<String, String> metadata) {
        return metadata == null ? Map.of() : new HashMap<>(metadata);
    }

    /**
     * Executes to timestamp for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param value input consumed by toTimestamp.
     * @return result produced by toTimestamp.
     */
    private Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    /**
     * Executes to offset date time for `ChargerRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param timestamp input consumed by toOffsetDateTime.
     * @return result produced by toOffsetDateTime.
     */
    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }
}

package com.electrahub.charger.repository;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public class OcpiConnectorIndexRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OcpiConnectorIndexRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countConnectorRows() {
        Long total = jdbcTemplate.queryForObject("select count(*) from connector_inventory", new MapSqlParameterSource(), Long.class);
        return total == null ? 0 : total;
    }

    public List<OcpiConnectorIndexRow> listConnectorRows(int limit, int offset) {
        return jdbcTemplate.query("""
                select c.connector_id,
                       c.evse_id,
                       ev.evse_uid,
                       ev.zone as evse_zone,
                       ev.capabilities as evse_capabilities,
                       ev.enabled as evse_enabled,
                       ch.charger_id,
                       ch.display_name as charger_display_name,
                       ch.enabled as charger_enabled,
                       loc.location_id,
                       loc.ocpi_location_id,
                       loc.name as location_name,
                       loc.city as location_city,
                       loc.address as location_address,
                       loc.latitude as location_latitude,
                       loc.longitude as location_longitude,
                       loc.enabled as location_enabled,
                       nw.network_id,
                       nw.name as network_name,
                       nw.enabled as network_enabled,
                       ent.enterprise_id,
                       ent.name as enterprise_name,
                       ent.country_code,
                       ent.party_id,
                       ent.enabled as enterprise_enabled,
                       c.standard,
                       c.format,
                       c.power_type,
                       c.max_power_kw,
                       c.ocpi_tariff_ids,
                       c.enabled as connector_enabled,
                       greatest(c.updated_at, ev.updated_at, ch.updated_at, loc.updated_at, nw.updated_at, ent.updated_at) as last_updated
                  from connector_inventory c
                  join evse_inventory ev on ev.evse_id = c.evse_id
                  join charger_inventory ch on ch.charger_id = ev.charger_id
                  join locations loc on loc.location_id = ch.location_id
                  join networks nw on nw.network_id = loc.network_id
                  join enterprises ent on ent.enterprise_id = nw.enterprise_id
                 order by c.updated_at desc, c.connector_id asc
                 limit :limit offset :offset
                """, new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset), rowMapper());
    }

    public Optional<OcpiConnectorIndexRow> findConnectorRowById(String connectorId) {
        List<OcpiConnectorIndexRow> rows = jdbcTemplate.query("""
                select c.connector_id,
                       c.evse_id,
                       ev.evse_uid,
                       ev.zone as evse_zone,
                       ev.capabilities as evse_capabilities,
                       ev.enabled as evse_enabled,
                       ch.charger_id,
                       ch.display_name as charger_display_name,
                       ch.enabled as charger_enabled,
                       loc.location_id,
                       loc.ocpi_location_id,
                       loc.name as location_name,
                       loc.city as location_city,
                       loc.address as location_address,
                       loc.latitude as location_latitude,
                       loc.longitude as location_longitude,
                       loc.enabled as location_enabled,
                       nw.network_id,
                       nw.name as network_name,
                       nw.enabled as network_enabled,
                       ent.enterprise_id,
                       ent.name as enterprise_name,
                       ent.country_code,
                       ent.party_id,
                       ent.enabled as enterprise_enabled,
                       c.standard,
                       c.format,
                       c.power_type,
                       c.max_power_kw,
                       c.ocpi_tariff_ids,
                       c.enabled as connector_enabled,
                       greatest(c.updated_at, ev.updated_at, ch.updated_at, loc.updated_at, nw.updated_at, ent.updated_at) as last_updated
                  from connector_inventory c
                  join evse_inventory ev on ev.evse_id = c.evse_id
                  join charger_inventory ch on ch.charger_id = ev.charger_id
                  join locations loc on loc.location_id = ch.location_id
                  join networks nw on nw.network_id = loc.network_id
                  join enterprises ent on ent.enterprise_id = nw.enterprise_id
                 where c.connector_id = :connectorId
                """, new MapSqlParameterSource("connectorId", connectorId), rowMapper());
        return rows.stream().findFirst();
    }

    public List<OcpiConnectorIndexRow> listConnectorRowsByEvseId(String evseId) {
        return jdbcTemplate.query("""
                select c.connector_id,
                       c.evse_id,
                       ev.evse_uid,
                       ev.zone as evse_zone,
                       ev.capabilities as evse_capabilities,
                       ev.enabled as evse_enabled,
                       ch.charger_id,
                       ch.display_name as charger_display_name,
                       ch.enabled as charger_enabled,
                       loc.location_id,
                       loc.ocpi_location_id,
                       loc.name as location_name,
                       loc.city as location_city,
                       loc.address as location_address,
                       loc.latitude as location_latitude,
                       loc.longitude as location_longitude,
                       loc.enabled as location_enabled,
                       nw.network_id,
                       nw.name as network_name,
                       nw.enabled as network_enabled,
                       ent.enterprise_id,
                       ent.name as enterprise_name,
                       ent.country_code,
                       ent.party_id,
                       ent.enabled as enterprise_enabled,
                       c.standard,
                       c.format,
                       c.power_type,
                       c.max_power_kw,
                       c.ocpi_tariff_ids,
                       c.enabled as connector_enabled,
                       greatest(c.updated_at, ev.updated_at, ch.updated_at, loc.updated_at, nw.updated_at, ent.updated_at) as last_updated
                  from connector_inventory c
                  join evse_inventory ev on ev.evse_id = c.evse_id
                  join charger_inventory ch on ch.charger_id = ev.charger_id
                  join locations loc on loc.location_id = ch.location_id
                  join networks nw on nw.network_id = loc.network_id
                  join enterprises ent on ent.enterprise_id = nw.enterprise_id
                 where c.evse_id = :evseId
                 order by c.updated_at desc, c.connector_id asc
                """, new MapSqlParameterSource("evseId", evseId), rowMapper());
    }

    public List<OcpiConnectorIndexRow> listConnectorRowsByChargerId(String chargerId) {
        return jdbcTemplate.query("""
                select c.connector_id,
                       c.evse_id,
                       ev.evse_uid,
                       ev.zone as evse_zone,
                       ev.capabilities as evse_capabilities,
                       ev.enabled as evse_enabled,
                       ch.charger_id,
                       ch.display_name as charger_display_name,
                       ch.enabled as charger_enabled,
                       loc.location_id,
                       loc.ocpi_location_id,
                       loc.name as location_name,
                       loc.city as location_city,
                       loc.address as location_address,
                       loc.latitude as location_latitude,
                       loc.longitude as location_longitude,
                       loc.enabled as location_enabled,
                       nw.network_id,
                       nw.name as network_name,
                       nw.enabled as network_enabled,
                       ent.enterprise_id,
                       ent.name as enterprise_name,
                       ent.country_code,
                       ent.party_id,
                       ent.enabled as enterprise_enabled,
                       c.standard,
                       c.format,
                       c.power_type,
                       c.max_power_kw,
                       c.ocpi_tariff_ids,
                       c.enabled as connector_enabled,
                       greatest(c.updated_at, ev.updated_at, ch.updated_at, loc.updated_at, nw.updated_at, ent.updated_at) as last_updated
                  from connector_inventory c
                  join evse_inventory ev on ev.evse_id = c.evse_id
                  join charger_inventory ch on ch.charger_id = ev.charger_id
                  join locations loc on loc.location_id = ch.location_id
                  join networks nw on nw.network_id = loc.network_id
                  join enterprises ent on ent.enterprise_id = nw.enterprise_id
                 where ch.charger_id = :chargerId
                 order by c.updated_at desc, c.connector_id asc
                """, new MapSqlParameterSource("chargerId", chargerId), rowMapper());
    }

    private RowMapper<OcpiConnectorIndexRow> rowMapper() {
        return (rs, rowNum) -> new OcpiConnectorIndexRow(
                rs.getString("connector_id"),
                rs.getString("evse_id"),
                rs.getString("evse_uid"),
                rs.getString("evse_zone"),
                splitCommaSeparated(rs.getString("evse_capabilities")),
                rs.getBoolean("evse_enabled"),
                rs.getString("charger_id"),
                rs.getString("charger_display_name"),
                rs.getBoolean("charger_enabled"),
                rs.getString("location_id"),
                rs.getString("ocpi_location_id"),
                rs.getString("location_name"),
                rs.getString("location_city"),
                rs.getString("location_address"),
                rs.getBigDecimal("location_latitude"),
                rs.getBigDecimal("location_longitude"),
                rs.getBoolean("location_enabled"),
                rs.getString("network_id"),
                rs.getString("network_name"),
                rs.getBoolean("network_enabled"),
                rs.getString("enterprise_id"),
                rs.getString("enterprise_name"),
                rs.getString("country_code"),
                rs.getString("party_id"),
                rs.getBoolean("enterprise_enabled"),
                rs.getString("standard"),
                rs.getString("format"),
                rs.getString("power_type"),
                rs.getBigDecimal("max_power_kw"),
                splitCommaSeparated(rs.getString("ocpi_tariff_ids")),
                rs.getBoolean("connector_enabled"),
                toOffsetDateTime(rs.getTimestamp("last_updated"))
        );
    }

    private List<String> splitCommaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .toList();
    }

    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }

    public record OcpiConnectorIndexRow(
            String connectorId,
            String evseId,
            String evseUid,
            String evseZone,
            List<String> evseCapabilities,
            boolean evseEnabled,
            String chargerId,
            String chargerDisplayName,
            boolean chargerEnabled,
            String locationId,
            String ocpiLocationId,
            String locationName,
            String locationCity,
            String locationAddress,
            BigDecimal locationLatitude,
            BigDecimal locationLongitude,
            boolean locationEnabled,
            String networkId,
            String networkName,
            boolean networkEnabled,
            String enterpriseId,
            String enterpriseName,
            String countryCode,
            String partyId,
            boolean enterpriseEnabled,
            String standard,
            String format,
            String powerType,
            BigDecimal maxPowerKw,
            List<String> tariffIds,
            boolean connectorEnabled,
            OffsetDateTime lastUpdated
    ) {
    }
}

package com.electrahub.charger.repository;

import com.electrahub.charger.api.dto.ChargerAdminDtos;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Repository
public class ChargerAdminRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ChargerAdminRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean enterpriseExists(String enterpriseId) {
        return exists("select count(*) from enterprises where enterprise_id = :id", enterpriseId);
    }

    public boolean networkExists(String networkId) {
        return exists("select count(*) from networks where network_id = :id", networkId);
    }

    public boolean locationExists(String locationId) {
        return exists("select count(*) from locations where location_id = :id", locationId);
    }

    public boolean chargerExists(String chargerId) {
        return exists("select count(*) from charger_inventory where charger_id = :id", chargerId);
    }

    public boolean evseExists(String evseId) {
        return exists("select count(*) from evse_inventory where evse_id = :id", evseId);
    }

    public boolean connectorExists(String connectorId) {
        return exists("select count(*) from connector_inventory where connector_id = :id", connectorId);
    }

    public ChargerAdminDtos.EnterpriseResponse createEnterprise(String enterpriseId, ChargerAdminDtos.EnterpriseCreateRequest request) {
        Timestamp now = Timestamp.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("enterpriseId", enterpriseId)
                .addValue("name", request.name().trim())
                .addValue("countryCode", request.countryCode().trim().toUpperCase())
                .addValue("partyId", request.partyId().trim().toUpperCase())
                .addValue("timezone", request.timezone().trim())
                .addValue("enabled", request.enabled() == null || request.enabled())
                .addValue("createdAt", now)
                .addValue("updatedAt", now);
        jdbcTemplate.update("""
                insert into enterprises (
                    enterprise_id,
                    name,
                    country_code,
                    party_id,
                    timezone,
                    enabled,
                    created_at,
                    updated_at
                ) values (
                    :enterpriseId,
                    :name,
                    :countryCode,
                    :partyId,
                    :timezone,
                    :enabled,
                    :createdAt,
                    :updatedAt
                )
                """, params);
        return findEnterpriseById(enterpriseId).orElseThrow();
    }

    public long countEnterprises(String search) {
        StringBuilder sql = new StringBuilder("select count(*) from enterprises where 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();
        appendEnterpriseSearch(sql, params, search);
        Long total = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return total == null ? 0 : total;
    }

    public List<ChargerAdminDtos.EnterpriseResponse> listEnterprises(String search, int limit, int offset) {
        StringBuilder sql = new StringBuilder("select * from enterprises where 1=1");
        MapSqlParameterSource params = pagedParams(limit, offset);
        appendEnterpriseSearch(sql, params, search);
        sql.append(" order by updated_at desc, enterprise_id asc limit :limit offset :offset");
        return jdbcTemplate.query(sql.toString(), params, enterpriseRowMapper());
    }

    public Optional<ChargerAdminDtos.EnterpriseResponse> findEnterpriseById(String enterpriseId) {
        List<ChargerAdminDtos.EnterpriseResponse> rows = jdbcTemplate.query(
                "select * from enterprises where enterprise_id = :enterpriseId",
                new MapSqlParameterSource("enterpriseId", enterpriseId),
                enterpriseRowMapper()
        );
        return rows.stream().findFirst();
    }

    public ChargerAdminDtos.NetworkResponse createNetwork(String networkId, ChargerAdminDtos.NetworkCreateRequest request) {
        Timestamp now = Timestamp.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("networkId", networkId)
                .addValue("enterpriseId", request.enterpriseId())
                .addValue("name", request.name().trim())
                .addValue("region", request.region().trim().toUpperCase())
                .addValue("operatorEmail", request.operatorEmail().trim().toLowerCase())
                .addValue("enabled", request.enabled() == null || request.enabled())
                .addValue("createdAt", now)
                .addValue("updatedAt", now);
        jdbcTemplate.update("""
                insert into networks (
                    network_id,
                    enterprise_id,
                    name,
                    region,
                    operator_email,
                    enabled,
                    created_at,
                    updated_at
                ) values (
                    :networkId,
                    :enterpriseId,
                    :name,
                    :region,
                    :operatorEmail,
                    :enabled,
                    :createdAt,
                    :updatedAt
                )
                """, params);
        return findNetworkById(networkId).orElseThrow();
    }

    public long countNetworks(String search, String enterpriseId) {
        StringBuilder sql = new StringBuilder("""
                select count(*)
                  from networks n
                  join enterprises e on e.enterprise_id = n.enterprise_id
                 where 1=1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();
        appendNetworkSearch(sql, params, search, enterpriseId);
        Long total = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return total == null ? 0 : total;
    }

    public List<ChargerAdminDtos.NetworkResponse> listNetworks(String search, String enterpriseId, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
                select n.network_id,
                       n.enterprise_id,
                       e.name as enterprise_name,
                       n.name,
                       n.region,
                       n.operator_email,
                       n.enabled,
                       n.created_at,
                       n.updated_at
                  from networks n
                  join enterprises e on e.enterprise_id = n.enterprise_id
                 where 1=1
                """);
        MapSqlParameterSource params = pagedParams(limit, offset);
        appendNetworkSearch(sql, params, search, enterpriseId);
        sql.append(" order by n.updated_at desc, n.network_id asc limit :limit offset :offset");
        return jdbcTemplate.query(sql.toString(), params, networkRowMapper());
    }

    public Optional<ChargerAdminDtos.NetworkResponse> findNetworkById(String networkId) {
        List<ChargerAdminDtos.NetworkResponse> rows = jdbcTemplate.query("""
                select n.network_id,
                       n.enterprise_id,
                       e.name as enterprise_name,
                       n.name,
                       n.region,
                       n.operator_email,
                       n.enabled,
                       n.created_at,
                       n.updated_at
                  from networks n
                  join enterprises e on e.enterprise_id = n.enterprise_id
                 where n.network_id = :networkId
                """, new MapSqlParameterSource("networkId", networkId), networkRowMapper());
        return rows.stream().findFirst();
    }

    public ChargerAdminDtos.LocationResponse createLocation(String locationId, ChargerAdminDtos.LocationCreateRequest request) {
        Timestamp now = Timestamp.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("locationId", locationId)
                .addValue("networkId", request.networkId())
                .addValue("name", request.name().trim())
                .addValue("city", request.city().trim())
                .addValue("address", request.address().trim())
                .addValue("ocpiLocationId", request.ocpiLocationId().trim().toUpperCase())
                .addValue("enabled", request.enabled() == null || request.enabled())
                .addValue("createdAt", now)
                .addValue("updatedAt", now);
        jdbcTemplate.update("""
                insert into locations (
                    location_id,
                    network_id,
                    name,
                    city,
                    address,
                    ocpi_location_id,
                    enabled,
                    created_at,
                    updated_at
                ) values (
                    :locationId,
                    :networkId,
                    :name,
                    :city,
                    :address,
                    :ocpiLocationId,
                    :enabled,
                    :createdAt,
                    :updatedAt
                )
                """, params);
        return findLocationById(locationId).orElseThrow();
    }

    public long countLocations(String search, String networkId) {
        StringBuilder sql = new StringBuilder("""
                select count(*)
                  from locations l
                  join networks n on n.network_id = l.network_id
                 where 1=1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();
        appendLocationSearch(sql, params, search, networkId);
        Long total = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return total == null ? 0 : total;
    }

    public List<ChargerAdminDtos.LocationResponse> listLocations(String search, String networkId, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
                select l.location_id,
                       l.network_id,
                       n.name as network_name,
                       l.name,
                       l.city,
                       l.address,
                       l.ocpi_location_id,
                       l.enabled,
                       l.created_at,
                       l.updated_at
                  from locations l
                  join networks n on n.network_id = l.network_id
                 where 1=1
                """);
        MapSqlParameterSource params = pagedParams(limit, offset);
        appendLocationSearch(sql, params, search, networkId);
        sql.append(" order by l.updated_at desc, l.location_id asc limit :limit offset :offset");
        return jdbcTemplate.query(sql.toString(), params, locationRowMapper());
    }

    public Optional<ChargerAdminDtos.LocationResponse> findLocationById(String locationId) {
        List<ChargerAdminDtos.LocationResponse> rows = jdbcTemplate.query("""
                select l.location_id,
                       l.network_id,
                       n.name as network_name,
                       l.name,
                       l.city,
                       l.address,
                       l.ocpi_location_id,
                       l.enabled,
                       l.created_at,
                       l.updated_at
                  from locations l
                  join networks n on n.network_id = l.network_id
                 where l.location_id = :locationId
                """, new MapSqlParameterSource("locationId", locationId), locationRowMapper());
        return rows.stream().findFirst();
    }

    public ChargerAdminDtos.ChargerResponse createCharger(ChargerAdminDtos.ChargerCreateRequest request) {
        Timestamp now = Timestamp.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("chargerId", request.chargerId().trim().toUpperCase())
                .addValue("displayName", request.displayName().trim())
                .addValue("locationId", request.locationId())
                .addValue("model", request.model().trim())
                .addValue("ocppVersion", request.ocppVersion().name())
                .addValue("maxPowerKw", request.maxPowerKw())
                .addValue("enabled", request.enabled() == null || request.enabled())
                .addValue("createdAt", now)
                .addValue("updatedAt", now);
        jdbcTemplate.update("""
                insert into charger_inventory (
                    charger_id,
                    display_name,
                    location_id,
                    model,
                    ocpp_version,
                    max_power_kw,
                    enabled,
                    created_at,
                    updated_at
                ) values (
                    :chargerId,
                    :displayName,
                    :locationId,
                    :model,
                    :ocppVersion,
                    :maxPowerKw,
                    :enabled,
                    :createdAt,
                    :updatedAt
                )
                """, params);
        return findChargerById(request.chargerId().trim().toUpperCase()).orElseThrow();
    }

    public long countChargers(String search, String locationId) {
        StringBuilder sql = new StringBuilder("""
                select count(*)
                  from charger_inventory c
                  join locations l on l.location_id = c.location_id
                  join networks n on n.network_id = l.network_id
                  join enterprises e on e.enterprise_id = n.enterprise_id
                 where 1=1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();
        appendChargerSearch(sql, params, search, locationId);
        Long total = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return total == null ? 0 : total;
    }

    public List<ChargerAdminDtos.ChargerResponse> listChargers(String search, String locationId, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
                select c.charger_id,
                       c.display_name,
                       c.location_id,
                       l.name as location_name,
                       n.network_id,
                       n.name as network_name,
                       e.enterprise_id,
                       e.name as enterprise_name,
                       c.model,
                       c.ocpp_version,
                       c.max_power_kw,
                       c.enabled,
                       c.created_at,
                       c.updated_at
                  from charger_inventory c
                  join locations l on l.location_id = c.location_id
                  join networks n on n.network_id = l.network_id
                  join enterprises e on e.enterprise_id = n.enterprise_id
                 where 1=1
                """);
        MapSqlParameterSource params = pagedParams(limit, offset);
        appendChargerSearch(sql, params, search, locationId);
        sql.append(" order by c.updated_at desc, c.charger_id asc limit :limit offset :offset");
        return jdbcTemplate.query(sql.toString(), params, chargerRowMapper());
    }

    public Optional<ChargerAdminDtos.ChargerResponse> findChargerById(String chargerId) {
        List<ChargerAdminDtos.ChargerResponse> rows = jdbcTemplate.query("""
                select c.charger_id,
                       c.display_name,
                       c.location_id,
                       l.name as location_name,
                       n.network_id,
                       n.name as network_name,
                       e.enterprise_id,
                       e.name as enterprise_name,
                       c.model,
                       c.ocpp_version,
                       c.max_power_kw,
                       c.enabled,
                       c.created_at,
                       c.updated_at
                  from charger_inventory c
                  join locations l on l.location_id = c.location_id
                  join networks n on n.network_id = l.network_id
                  join enterprises e on e.enterprise_id = n.enterprise_id
                 where c.charger_id = :chargerId
                """, new MapSqlParameterSource("chargerId", chargerId), chargerRowMapper());
        return rows.stream().findFirst();
    }

    public ChargerAdminDtos.EvseResponse createEvse(String evseId, ChargerAdminDtos.EvseCreateRequest request) {
        Timestamp now = Timestamp.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("evseId", evseId)
                .addValue("chargerId", request.chargerId())
                .addValue("evseUid", request.evseUid().trim().toUpperCase())
                .addValue("zone", request.zone() == null ? null : request.zone().trim())
                .addValue("capabilities", request.capabilities() == null ? null : request.capabilities().trim().toUpperCase())
                .addValue("enabled", request.enabled() == null || request.enabled())
                .addValue("createdAt", now)
                .addValue("updatedAt", now);
        jdbcTemplate.update("""
                insert into evse_inventory (
                    evse_id,
                    charger_id,
                    evse_uid,
                    zone,
                    capabilities,
                    enabled,
                    created_at,
                    updated_at
                ) values (
                    :evseId,
                    :chargerId,
                    :evseUid,
                    :zone,
                    :capabilities,
                    :enabled,
                    :createdAt,
                    :updatedAt
                )
                """, params);
        return findEvseById(evseId).orElseThrow();
    }

    public long countEvses(String search, String chargerId) {
        StringBuilder sql = new StringBuilder("""
                select count(*)
                  from evse_inventory e
                  join charger_inventory c on c.charger_id = e.charger_id
                 where 1=1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();
        appendEvseSearch(sql, params, search, chargerId);
        Long total = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return total == null ? 0 : total;
    }

    public List<ChargerAdminDtos.EvseResponse> listEvses(String search, String chargerId, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
                select e.evse_id,
                       e.charger_id,
                       c.display_name as charger_display_name,
                       e.evse_uid,
                       e.zone,
                       coalesce(count(ci.connector_id), 0) as connector_count,
                       e.capabilities,
                       e.enabled,
                       e.created_at,
                       e.updated_at
                  from evse_inventory e
                  join charger_inventory c on c.charger_id = e.charger_id
             left join connector_inventory ci on ci.evse_id = e.evse_id
                 where 1=1
                """);
        MapSqlParameterSource params = pagedParams(limit, offset);
        appendEvseSearch(sql, params, search, chargerId);
        sql.append("""
                 group by e.evse_id, e.charger_id, c.display_name, e.evse_uid, e.zone, e.capabilities, e.enabled, e.created_at, e.updated_at
                 order by e.updated_at desc, e.evse_id asc
                 limit :limit offset :offset
                """);
        return jdbcTemplate.query(sql.toString(), params, evseRowMapper());
    }

    public Optional<ChargerAdminDtos.EvseResponse> findEvseById(String evseId) {
        List<ChargerAdminDtos.EvseResponse> rows = jdbcTemplate.query("""
                select e.evse_id,
                       e.charger_id,
                       c.display_name as charger_display_name,
                       e.evse_uid,
                       e.zone,
                       coalesce(count(ci.connector_id), 0) as connector_count,
                       e.capabilities,
                       e.enabled,
                       e.created_at,
                       e.updated_at
                  from evse_inventory e
                  join charger_inventory c on c.charger_id = e.charger_id
             left join connector_inventory ci on ci.evse_id = e.evse_id
                 where e.evse_id = :evseId
              group by e.evse_id, e.charger_id, c.display_name, e.evse_uid, e.zone, e.capabilities, e.enabled, e.created_at, e.updated_at
                """, new MapSqlParameterSource("evseId", evseId), evseRowMapper());
        return rows.stream().findFirst();
    }

    public ChargerAdminDtos.ConnectorResponse createConnector(ChargerAdminDtos.ConnectorCreateRequest request) {
        Timestamp now = Timestamp.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("connectorId", request.connectorId().trim().toUpperCase())
                .addValue("evseId", request.evseId())
                .addValue("standard", request.standard().trim().toUpperCase())
                .addValue("format", request.format().trim().toUpperCase())
                .addValue("powerType", request.powerType().trim().toUpperCase())
                .addValue("maxPowerKw", request.maxPowerKw())
                .addValue("ocpiTariffIds", serializeTariffIds(request.ocpiTariffIds()))
                .addValue("enabled", request.enabled() == null || request.enabled())
                .addValue("createdAt", now)
                .addValue("updatedAt", now);
        jdbcTemplate.update("""
                insert into connector_inventory (
                    connector_id,
                    evse_id,
                    standard,
                    format,
                    power_type,
                    max_power_kw,
                    ocpi_tariff_ids,
                    enabled,
                    created_at,
                    updated_at
                ) values (
                    :connectorId,
                    :evseId,
                    :standard,
                    :format,
                    :powerType,
                    :maxPowerKw,
                    :ocpiTariffIds,
                    :enabled,
                    :createdAt,
                    :updatedAt
                )
                """, params);
        return findConnectorById(request.connectorId().trim().toUpperCase()).orElseThrow();
    }

    public long countConnectors(String search, String evseId) {
        StringBuilder sql = new StringBuilder("""
                select count(*)
                  from connector_inventory c
                  join evse_inventory e on e.evse_id = c.evse_id
                 where 1=1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();
        appendConnectorSearch(sql, params, search, evseId);
        Long total = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return total == null ? 0 : total;
    }

    public List<ChargerAdminDtos.ConnectorResponse> listConnectors(String search, String evseId, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
                select c.connector_id,
                       c.evse_id,
                       e.evse_uid,
                       e.charger_id,
                       c.standard,
                       c.format,
                       c.power_type,
                       c.max_power_kw,
                       c.ocpi_tariff_ids,
                       c.enabled,
                       c.created_at,
                       c.updated_at
                  from connector_inventory c
                  join evse_inventory e on e.evse_id = c.evse_id
                 where 1=1
                """);
        MapSqlParameterSource params = pagedParams(limit, offset);
        appendConnectorSearch(sql, params, search, evseId);
        sql.append(" order by c.updated_at desc, c.connector_id asc limit :limit offset :offset");
        return jdbcTemplate.query(sql.toString(), params, connectorRowMapper());
    }

    public Optional<ChargerAdminDtos.ConnectorResponse> findConnectorById(String connectorId) {
        List<ChargerAdminDtos.ConnectorResponse> rows = jdbcTemplate.query("""
                select c.connector_id,
                       c.evse_id,
                       e.evse_uid,
                       e.charger_id,
                       c.standard,
                       c.format,
                       c.power_type,
                       c.max_power_kw,
                       c.ocpi_tariff_ids,
                       c.enabled,
                       c.created_at,
                       c.updated_at
                  from connector_inventory c
                  join evse_inventory e on e.evse_id = c.evse_id
                 where c.connector_id = :connectorId
                """, new MapSqlParameterSource("connectorId", connectorId), connectorRowMapper());
        return rows.stream().findFirst();
    }

    private boolean exists(String sql, String id) {
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("id", id), Integer.class);
        return count != null && count > 0;
    }

    private void appendEnterpriseSearch(StringBuilder sql, MapSqlParameterSource params, String search) {
        if (search != null && !search.isBlank()) {
            sql.append(" and (lower(enterprise_id) like :query or lower(name) like :query or lower(party_id) like :query)");
            params.addValue("query", like(search));
        }
    }

    private void appendNetworkSearch(StringBuilder sql, MapSqlParameterSource params, String search, String enterpriseId) {
        if (enterpriseId != null && !enterpriseId.isBlank()) {
            sql.append(" and n.enterprise_id = :enterpriseId");
            params.addValue("enterpriseId", enterpriseId);
        }
        if (search != null && !search.isBlank()) {
            sql.append("""
                     and (
                        lower(n.network_id) like :query
                        or lower(n.name) like :query
                        or lower(n.region) like :query
                        or lower(n.operator_email) like :query
                        or lower(e.name) like :query
                     )
                    """);
            params.addValue("query", like(search));
        }
    }

    private void appendLocationSearch(StringBuilder sql, MapSqlParameterSource params, String search, String networkId) {
        if (networkId != null && !networkId.isBlank()) {
            sql.append(" and l.network_id = :networkId");
            params.addValue("networkId", networkId);
        }
        if (search != null && !search.isBlank()) {
            sql.append("""
                     and (
                        lower(l.location_id) like :query
                        or lower(l.name) like :query
                        or lower(l.city) like :query
                        or lower(l.address) like :query
                        or lower(l.ocpi_location_id) like :query
                        or lower(n.name) like :query
                     )
                    """);
            params.addValue("query", like(search));
        }
    }

    private void appendChargerSearch(StringBuilder sql, MapSqlParameterSource params, String search, String locationId) {
        if (locationId != null && !locationId.isBlank()) {
            sql.append(" and c.location_id = :locationId");
            params.addValue("locationId", locationId);
        }
        if (search != null && !search.isBlank()) {
            sql.append("""
                     and (
                        lower(c.charger_id) like :query
                        or lower(c.display_name) like :query
                        or lower(c.model) like :query
                        or lower(c.ocpp_version) like :query
                        or lower(l.name) like :query
                     )
                    """);
            params.addValue("query", like(search));
        }
    }

    private void appendEvseSearch(StringBuilder sql, MapSqlParameterSource params, String search, String chargerId) {
        if (chargerId != null && !chargerId.isBlank()) {
            sql.append(" and e.charger_id = :chargerId");
            params.addValue("chargerId", chargerId);
        }
        if (search != null && !search.isBlank()) {
            sql.append("""
                     and (
                        lower(e.evse_id) like :query
                        or lower(e.evse_uid) like :query
                        or lower(e.zone) like :query
                        or lower(coalesce(e.capabilities, '')) like :query
                        or lower(e.charger_id) like :query
                        or lower(c.display_name) like :query
                     )
                    """);
            params.addValue("query", like(search));
        }
    }

    private void appendConnectorSearch(StringBuilder sql, MapSqlParameterSource params, String search, String evseId) {
        if (evseId != null && !evseId.isBlank()) {
            sql.append(" and c.evse_id = :evseId");
            params.addValue("evseId", evseId);
        }
        if (search != null && !search.isBlank()) {
            sql.append("""
                     and (
                        lower(c.connector_id) like :query
                        or lower(c.standard) like :query
                        or lower(c.format) like :query
                        or lower(c.power_type) like :query
                        or lower(e.evse_uid) like :query
                        or lower(e.charger_id) like :query
                        or lower(c.ocpi_tariff_ids) like :query
                     )
                    """);
            params.addValue("query", like(search));
        }
    }

    private MapSqlParameterSource pagedParams(int limit, int offset) {
        return new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);
    }

    private String like(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }

    private RowMapper<ChargerAdminDtos.EnterpriseResponse> enterpriseRowMapper() {
        return (rs, rowNum) -> new ChargerAdminDtos.EnterpriseResponse(
                rs.getString("enterprise_id"),
                rs.getString("name"),
                rs.getString("country_code"),
                rs.getString("party_id"),
                rs.getString("timezone"),
                rs.getBoolean("enabled"),
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private RowMapper<ChargerAdminDtos.NetworkResponse> networkRowMapper() {
        return (rs, rowNum) -> new ChargerAdminDtos.NetworkResponse(
                rs.getString("network_id"),
                rs.getString("enterprise_id"),
                rs.getString("enterprise_name"),
                rs.getString("name"),
                rs.getString("region"),
                rs.getString("operator_email"),
                rs.getBoolean("enabled"),
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private RowMapper<ChargerAdminDtos.LocationResponse> locationRowMapper() {
        return (rs, rowNum) -> new ChargerAdminDtos.LocationResponse(
                rs.getString("location_id"),
                rs.getString("network_id"),
                rs.getString("network_name"),
                rs.getString("name"),
                rs.getString("city"),
                rs.getString("address"),
                rs.getString("ocpi_location_id"),
                rs.getBoolean("enabled"),
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private RowMapper<ChargerAdminDtos.ChargerResponse> chargerRowMapper() {
        return (rs, rowNum) -> new ChargerAdminDtos.ChargerResponse(
                rs.getString("charger_id"),
                rs.getString("display_name"),
                rs.getString("location_id"),
                rs.getString("location_name"),
                rs.getString("network_id"),
                rs.getString("network_name"),
                rs.getString("enterprise_id"),
                rs.getString("enterprise_name"),
                rs.getString("model"),
                ChargerAdminDtos.OcppVersion.valueOf(rs.getString("ocpp_version")),
                rs.getBigDecimal("max_power_kw"),
                rs.getBoolean("enabled"),
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private RowMapper<ChargerAdminDtos.EvseResponse> evseRowMapper() {
        return (rs, rowNum) -> new ChargerAdminDtos.EvseResponse(
                rs.getString("evse_id"),
                rs.getString("charger_id"),
                rs.getString("charger_display_name"),
                rs.getString("evse_uid"),
                rs.getString("zone"),
                rs.getInt("connector_count"),
                rs.getString("capabilities"),
                rs.getBoolean("enabled"),
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private RowMapper<ChargerAdminDtos.ConnectorResponse> connectorRowMapper() {
        return (rs, rowNum) -> new ChargerAdminDtos.ConnectorResponse(
                rs.getString("connector_id"),
                rs.getString("evse_id"),
                rs.getString("evse_uid"),
                rs.getString("charger_id"),
                rs.getString("standard"),
                rs.getString("format"),
                rs.getString("power_type"),
                rs.getBigDecimal("max_power_kw"),
                deserializeTariffIds(rs.getString("ocpi_tariff_ids")),
                rs.getBoolean("enabled"),
                toOffsetDateTime(rs.getTimestamp("created_at")),
                toOffsetDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private String serializeTariffIds(List<String> tariffIds) {
        if (tariffIds == null || tariffIds.isEmpty()) {
            return "";
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tariffId : tariffIds) {
            if (tariffId == null) {
                continue;
            }
            String value = tariffId.trim();
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return String.join(",", normalized);
    }

    private List<String> deserializeTariffIds(String rawTariffIds) {
        if (rawTariffIds == null || rawTariffIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rawTariffIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }
}

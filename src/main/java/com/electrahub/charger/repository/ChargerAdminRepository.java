package com.electrahub.charger.repository;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.charger.api.dto.ChargerAdminDtos;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Repository
public class ChargerAdminRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChargerAdminRepository.class);


    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Executes charger admin repository for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param jdbcTemplate input consumed by ChargerAdminRepository.
     */
    public ChargerAdminRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering ChargerAdminRepository#ChargerAdminRepository");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering ChargerAdminRepository#ChargerAdminRepository with debug context");
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Executes enterprise exists for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param enterpriseId input consumed by enterpriseExists.
     * @return result produced by enterpriseExists.
     */
    public boolean enterpriseExists(String enterpriseId) {
        return exists("select count(*) from enterprises where enterprise_id = :id", enterpriseId);
    }

    /**
     * Executes network exists for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param networkId input consumed by networkExists.
     * @return result produced by networkExists.
     */
    public boolean networkExists(String networkId) {
        return exists("select count(*) from networks where network_id = :id", networkId);
    }

    /**
     * Executes location exists for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param locationId input consumed by locationExists.
     * @return result produced by locationExists.
     */
    public boolean locationExists(String locationId) {
        return exists("select count(*) from locations where location_id = :id", locationId);
    }

    /**
     * Executes charger exists for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param chargerId input consumed by chargerExists.
     * @return result produced by chargerExists.
     */
    public boolean chargerExists(String chargerId) {
        return exists("select count(*) from charger_inventory where charger_id = :id", chargerId);
    }

    /**
     * Executes evse exists for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param evseId input consumed by evseExists.
     * @return result produced by evseExists.
     */
    public boolean evseExists(String evseId) {
        return exists("select count(*) from evse_inventory where evse_id = :id", evseId);
    }

    /**
     * Executes connector exists for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param connectorId input consumed by connectorExists.
     * @return result produced by connectorExists.
     */
    public boolean connectorExists(String connectorId) {
        return exists("select count(*) from connector_inventory where connector_id = :id", connectorId);
    }

    /**
     * Creates create enterprise for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param enterpriseId input consumed by createEnterprise.
     * @param request input consumed by createEnterprise.
     * @return result produced by createEnterprise.
     */
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

    /**
     * Executes count enterprises for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param search input consumed by countEnterprises.
     * @return result produced by countEnterprises.
     */
    public long countEnterprises(String search) {
        StringBuilder sql = new StringBuilder("select count(*) from enterprises where 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();
        appendEnterpriseSearch(sql, params, search);
        Long total = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return total == null ? 0 : total;
    }

    /**
     * Retrieves list enterprises for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param search input consumed by listEnterprises.
     * @param limit input consumed by listEnterprises.
     * @param offset input consumed by listEnterprises.
     * @return result produced by listEnterprises.
     */
    public List<ChargerAdminDtos.EnterpriseResponse> listEnterprises(String search, int limit, int offset) {
        StringBuilder sql = new StringBuilder("select * from enterprises where 1=1");
        MapSqlParameterSource params = pagedParams(limit, offset);
        appendEnterpriseSearch(sql, params, search);
        sql.append(" order by updated_at desc, enterprise_id asc limit :limit offset :offset");
        return jdbcTemplate.query(sql.toString(), params, enterpriseRowMapper());
    }

    /**
     * Retrieves find enterprise by id for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param enterpriseId input consumed by findEnterpriseById.
     * @return result produced by findEnterpriseById.
     */
    public Optional<ChargerAdminDtos.EnterpriseResponse> findEnterpriseById(String enterpriseId) {
        List<ChargerAdminDtos.EnterpriseResponse> rows = jdbcTemplate.query(
                "select * from enterprises where enterprise_id = :enterpriseId",
                new MapSqlParameterSource("enterpriseId", enterpriseId),
                enterpriseRowMapper()
        );
        return rows.stream().findFirst();
    }

    /**
     * Creates create network for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param networkId input consumed by createNetwork.
     * @param request input consumed by createNetwork.
     * @return result produced by createNetwork.
     */
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

    /**
     * Executes count networks for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param search input consumed by countNetworks.
     * @param enterpriseId input consumed by countNetworks.
     * @return result produced by countNetworks.
     */
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

    /**
     * Retrieves list networks for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param search input consumed by listNetworks.
     * @param enterpriseId input consumed by listNetworks.
     * @param limit input consumed by listNetworks.
     * @param offset input consumed by listNetworks.
     * @return result produced by listNetworks.
     */
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

    /**
     * Retrieves find network by id for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param networkId input consumed by findNetworkById.
     * @return result produced by findNetworkById.
     */
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

    /**
     * Creates create location for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param locationId input consumed by createLocation.
     * @param request input consumed by createLocation.
     * @return result produced by createLocation.
     */
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

    /**
     * Executes count locations for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param search input consumed by countLocations.
     * @param networkId input consumed by countLocations.
     * @return result produced by countLocations.
     */
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

    /**
     * Retrieves list locations for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param search input consumed by listLocations.
     * @param networkId input consumed by listLocations.
     * @param limit input consumed by listLocations.
     * @param offset input consumed by listLocations.
     * @return result produced by listLocations.
     */
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

    /**
     * Retrieves find location by id for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param locationId input consumed by findLocationById.
     * @return result produced by findLocationById.
     */
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

    /**
     * Creates create charger for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param request input consumed by createCharger.
     * @return result produced by createCharger.
     */
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

    /**
     * Executes count chargers for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param search input consumed by countChargers.
     * @param locationId input consumed by countChargers.
     * @return result produced by countChargers.
     */
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

    /**
     * Retrieves list chargers for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param search input consumed by listChargers.
     * @param locationId input consumed by listChargers.
     * @param limit input consumed by listChargers.
     * @param offset input consumed by listChargers.
     * @return result produced by listChargers.
     */
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

    /**
     * Retrieves find charger by id for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param chargerId input consumed by findChargerById.
     * @return result produced by findChargerById.
     */
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

    /**
     * Creates create evse for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param evseId input consumed by createEvse.
     * @param request input consumed by createEvse.
     * @return result produced by createEvse.
     */
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

    /**
     * Executes count evses for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param search input consumed by countEvses.
     * @param chargerId input consumed by countEvses.
     * @return result produced by countEvses.
     */
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

    /**
     * Retrieves list evses for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param search input consumed by listEvses.
     * @param chargerId input consumed by listEvses.
     * @param limit input consumed by listEvses.
     * @param offset input consumed by listEvses.
     * @return result produced by listEvses.
     */
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

    /**
     * Retrieves find evse by id for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param evseId input consumed by findEvseById.
     * @return result produced by findEvseById.
     */
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

    /**
     * Creates create connector for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param request input consumed by createConnector.
     * @return result produced by createConnector.
     */
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

    /**
     * Executes count connectors for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param search input consumed by countConnectors.
     * @param evseId input consumed by countConnectors.
     * @return result produced by countConnectors.
     */
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

    /**
     * Retrieves list connectors for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param search input consumed by listConnectors.
     * @param evseId input consumed by listConnectors.
     * @param limit input consumed by listConnectors.
     * @param offset input consumed by listConnectors.
     * @return result produced by listConnectors.
     */
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

    /**
     * Retrieves find connector by id for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param connectorId input consumed by findConnectorById.
     * @return result produced by findConnectorById.
     */
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

    /**
     * Retrieves list evses for search indexing for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: returns flattened EVSE and connector rows enriched with OCPI ownership and location data,
     * allowing service-level grouping into OCPI-aligned Elasticsearch documents.
     * @return result produced by listEvsesForSearchIndexing.
     */
    public List<EvseSearchExportRow> listEvsesForSearchIndexing() {
        return jdbcTemplate.query("""
                select en.country_code,
                       en.party_id,
                       l.ocpi_location_id,
                       c.charger_id,
                       c.display_name as charger_display_name,
                       e.evse_id,
                       e.evse_uid,
                       e.zone,
                       e.capabilities,
                       e.enabled as evse_enabled,
                       e.updated_at as evse_updated_at,
                       ci.connector_id,
                       ci.standard as connector_standard,
                       ci.format as connector_format,
                       ci.power_type as connector_power_type,
                       ci.max_power_kw as connector_max_power_kw,
                       ci.ocpi_tariff_ids as connector_tariff_ids,
                       ci.enabled as connector_enabled,
                       ci.updated_at as connector_updated_at
                  from evse_inventory e
                  join charger_inventory c on c.charger_id = e.charger_id
                  join locations l on l.location_id = c.location_id
                  join networks n on n.network_id = l.network_id
                  join enterprises en on en.enterprise_id = n.enterprise_id
             left join connector_inventory ci on ci.evse_id = e.evse_id
              order by e.evse_id asc, ci.connector_id asc
                """,
                (rs, rowNum) -> new EvseSearchExportRow(
                        rs.getString("country_code"),
                        rs.getString("party_id"),
                        rs.getString("ocpi_location_id"),
                        rs.getString("charger_id"),
                        rs.getString("charger_display_name"),
                        rs.getString("evse_id"),
                        rs.getString("evse_uid"),
                        rs.getString("zone"),
                        rs.getString("capabilities"),
                        rs.getBoolean("evse_enabled"),
                        toOffsetDateTime(rs.getTimestamp("evse_updated_at")),
                        rs.getString("connector_id"),
                        rs.getString("connector_standard"),
                        rs.getString("connector_format"),
                        rs.getString("connector_power_type"),
                        rs.getBigDecimal("connector_max_power_kw"),
                        rs.getString("connector_tariff_ids"),
                        rs.getObject("connector_enabled") == null || rs.getBoolean("connector_enabled"),
                        toOffsetDateTime(rs.getTimestamp("connector_updated_at"))
                )
        );
    }

    /**
     * Executes exists for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param sql input consumed by exists.
     * @param id input consumed by exists.
     * @return result produced by exists.
     */
    private boolean exists(String sql, String id) {
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("id", id), Integer.class);
        return count != null && count > 0;
    }

    /**
     * Executes append enterprise search for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param sql input consumed by appendEnterpriseSearch.
     * @param params input consumed by appendEnterpriseSearch.
     * @param search input consumed by appendEnterpriseSearch.
     */
    private void appendEnterpriseSearch(StringBuilder sql, MapSqlParameterSource params, String search) {
        if (search != null && !search.isBlank()) {
            sql.append(" and (lower(enterprise_id) like :query or lower(name) like :query or lower(party_id) like :query)");
            params.addValue("query", like(search));
        }
    }

    /**
     * Executes append network search for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param sql input consumed by appendNetworkSearch.
     * @param params input consumed by appendNetworkSearch.
     * @param search input consumed by appendNetworkSearch.
     * @param enterpriseId input consumed by appendNetworkSearch.
     */
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

    /**
     * Executes append location search for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param sql input consumed by appendLocationSearch.
     * @param params input consumed by appendLocationSearch.
     * @param search input consumed by appendLocationSearch.
     * @param networkId input consumed by appendLocationSearch.
     */
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

    /**
     * Executes append charger search for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param sql input consumed by appendChargerSearch.
     * @param params input consumed by appendChargerSearch.
     * @param search input consumed by appendChargerSearch.
     * @param locationId input consumed by appendChargerSearch.
     */
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

    /**
     * Executes append evse search for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param sql input consumed by appendEvseSearch.
     * @param params input consumed by appendEvseSearch.
     * @param search input consumed by appendEvseSearch.
     * @param chargerId input consumed by appendEvseSearch.
     */
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

    /**
     * Executes append connector search for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param sql input consumed by appendConnectorSearch.
     * @param params input consumed by appendConnectorSearch.
     * @param search input consumed by appendConnectorSearch.
     * @param evseId input consumed by appendConnectorSearch.
     */
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

    /**
     * Executes paged params for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param limit input consumed by pagedParams.
     * @param offset input consumed by pagedParams.
     * @return result produced by pagedParams.
     */
    private MapSqlParameterSource pagedParams(int limit, int offset) {
        return new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);
    }

    /**
     * Executes like for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param value input consumed by like.
     * @return result produced by like.
     */
    private String like(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }

    /**
     * Executes enterprise row mapper for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @return result produced by enterpriseRowMapper.
     */
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

    /**
     * Executes network row mapper for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @return result produced by networkRowMapper.
     */
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

    /**
     * Executes location row mapper for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @return result produced by locationRowMapper.
     */
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

    /**
     * Executes charger row mapper for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @return result produced by chargerRowMapper.
     */
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

    /**
     * Executes evse row mapper for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @return result produced by evseRowMapper.
     */
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

    /**
     * Executes connector row mapper for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @return result produced by connectorRowMapper.
     */
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

    /**
     * Executes serialize tariff ids for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param tariffIds input consumed by serializeTariffIds.
     * @return result produced by serializeTariffIds.
     */
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

    /**
     * Executes deserialize tariff ids for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param rawTariffIds input consumed by deserializeTariffIds.
     * @return result produced by deserializeTariffIds.
     */
    private List<String> deserializeTariffIds(String rawTariffIds) {
        if (rawTariffIds == null || rawTariffIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rawTariffIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    /**
     * Executes to offset date time for `ChargerAdminRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.repository`.
     * @param timestamp input consumed by toOffsetDateTime.
     * @return result produced by toOffsetDateTime.
     */
    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }

    public record EvseSearchExportRow(
            String enterpriseCountryCode,
            String enterprisePartyId,
            String ocpiLocationId,
            String chargerId,
            String chargerDisplayName,
            String evseId,
            String evseUid,
            String zone,
            String capabilities,
            boolean evseEnabled,
            OffsetDateTime evseUpdatedAt,
            String connectorId,
            String connectorStandard,
            String connectorFormat,
            String connectorPowerType,
            BigDecimal connectorMaxPowerKw,
            String connectorTariffIds,
            boolean connectorEnabled,
            OffsetDateTime connectorUpdatedAt
    ) {
    }
}

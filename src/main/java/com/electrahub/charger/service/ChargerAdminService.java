package com.electrahub.charger.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.charger.api.dto.ChargerAdminDtos;
import com.electrahub.charger.api.error.ConflictException;
import com.electrahub.charger.api.error.NotFoundException;
import com.electrahub.charger.repository.ChargerAdminRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ChargerAdminService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChargerAdminService.class);


    private final ChargerAdminRepository chargerAdminRepository;

    /**
     * Executes charger admin service for `ChargerAdminService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param chargerAdminRepository input consumed by ChargerAdminService.
     */
    public ChargerAdminService(ChargerAdminRepository chargerAdminRepository) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering ChargerAdminService#ChargerAdminService");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering ChargerAdminService#ChargerAdminService with debug context");
        this.chargerAdminRepository = chargerAdminRepository;
    }

    /**
     * Retrieves list enterprises for `ChargerAdminService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param search input consumed by listEnterprises.
     * @param limit input consumed by listEnterprises.
     * @param offset input consumed by listEnterprises.
     * @return result produced by listEnterprises.
     */
    @Transactional(readOnly = true)
    public ChargerAdminDtos.PagedResponse<ChargerAdminDtos.EnterpriseResponse> listEnterprises(String search, int limit, int offset) {
        long total = chargerAdminRepository.countEnterprises(search);
        List<ChargerAdminDtos.EnterpriseResponse> items = chargerAdminRepository.listEnterprises(search, limit, offset);
        return paged(items, total, limit, offset);
    }

    /**
     * Creates create enterprise for `ChargerAdminService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param request input consumed by createEnterprise.
     * @return result produced by createEnterprise.
     */
    @Transactional
    public ChargerAdminDtos.EnterpriseResponse createEnterprise(ChargerAdminDtos.EnterpriseCreateRequest request) {
        String enterpriseId = generateIdentifier("ENT", request.countryCode());
        try {
            return chargerAdminRepository.createEnterprise(enterpriseId, request);
        } catch (DuplicateKeyException ex) {
            throw new ConflictException("Enterprise with the same country code and party id already exists");
        }
    }

    /**
     * Retrieves list networks for `ChargerAdminService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param search input consumed by listNetworks.
     * @param enterpriseId input consumed by listNetworks.
     * @param limit input consumed by listNetworks.
     * @param offset input consumed by listNetworks.
     * @return result produced by listNetworks.
     */
    @Transactional(readOnly = true)
    public ChargerAdminDtos.PagedResponse<ChargerAdminDtos.NetworkResponse> listNetworks(String search, String enterpriseId, int limit, int offset) {
        long total = chargerAdminRepository.countNetworks(search, enterpriseId);
        List<ChargerAdminDtos.NetworkResponse> items = chargerAdminRepository.listNetworks(search, enterpriseId, limit, offset);
        return paged(items, total, limit, offset);
    }

    /**
     * Creates create network for `ChargerAdminService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param request input consumed by createNetwork.
     * @return result produced by createNetwork.
     */
    @Transactional
    public ChargerAdminDtos.NetworkResponse createNetwork(ChargerAdminDtos.NetworkCreateRequest request) {
        if (!chargerAdminRepository.enterpriseExists(request.enterpriseId())) {
            throw new NotFoundException("Enterprise '%s' was not found".formatted(request.enterpriseId()));
        }

        String networkId = generateIdentifier("NW", request.region());
        try {
            return chargerAdminRepository.createNetwork(networkId, request);
        } catch (DuplicateKeyException ex) {
            throw new ConflictException("Network with the same enterprise and name already exists");
        }
    }

    /**
     * Retrieves list locations for `ChargerAdminService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param search input consumed by listLocations.
     * @param networkId input consumed by listLocations.
     * @param limit input consumed by listLocations.
     * @param offset input consumed by listLocations.
     * @return result produced by listLocations.
     */
    @Transactional(readOnly = true)
    public ChargerAdminDtos.PagedResponse<ChargerAdminDtos.LocationResponse> listLocations(String search, String networkId, int limit, int offset) {
        long total = chargerAdminRepository.countLocations(search, networkId);
        List<ChargerAdminDtos.LocationResponse> items = chargerAdminRepository.listLocations(search, networkId, limit, offset);
        return paged(items, total, limit, offset);
    }

    /**
     * Creates create location for `ChargerAdminService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param request input consumed by createLocation.
     * @return result produced by createLocation.
     */
    @Transactional
    public ChargerAdminDtos.LocationResponse createLocation(ChargerAdminDtos.LocationCreateRequest request) {
        if (!chargerAdminRepository.networkExists(request.networkId())) {
            throw new NotFoundException("Network '%s' was not found".formatted(request.networkId()));
        }

        String locationId = generateIdentifier("LOC", request.city());
        try {
            return chargerAdminRepository.createLocation(locationId, request);
        } catch (DuplicateKeyException ex) {
            throw new ConflictException("Location with the same OCPI location id already exists");
        }
    }

    /**
     * Retrieves list chargers for `ChargerAdminService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param search input consumed by listChargers.
     * @param locationId input consumed by listChargers.
     * @param limit input consumed by listChargers.
     * @param offset input consumed by listChargers.
     * @return result produced by listChargers.
     */
    @Transactional(readOnly = true)
    public ChargerAdminDtos.PagedResponse<ChargerAdminDtos.ChargerResponse> listChargers(String search, String locationId, int limit, int offset) {
        long total = chargerAdminRepository.countChargers(search, locationId);
        List<ChargerAdminDtos.ChargerResponse> items = chargerAdminRepository.listChargers(search, locationId, limit, offset);
        return paged(items, total, limit, offset);
    }

    /**
     * Creates create charger for `ChargerAdminService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param request input consumed by createCharger.
     * @return result produced by createCharger.
     */
    @Transactional
    public ChargerAdminDtos.ChargerResponse createCharger(ChargerAdminDtos.ChargerCreateRequest request) {
        if (!chargerAdminRepository.locationExists(request.locationId())) {
            throw new NotFoundException("Location '%s' was not found".formatted(request.locationId()));
        }
        if (chargerAdminRepository.chargerExists(request.chargerId().trim().toUpperCase(Locale.ROOT))) {
            throw new ConflictException("Charger '%s' already exists".formatted(request.chargerId()));
        }

        try {
            return chargerAdminRepository.createCharger(request);
        } catch (DuplicateKeyException ex) {
            throw new ConflictException("Charger '%s' already exists".formatted(request.chargerId()));
        }
    }

    /**
     * Retrieves list evses for `ChargerAdminService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param search input consumed by listEvses.
     * @param chargerId input consumed by listEvses.
     * @param limit input consumed by listEvses.
     * @param offset input consumed by listEvses.
     * @return result produced by listEvses.
     */
    @Transactional(readOnly = true)
    public ChargerAdminDtos.PagedResponse<ChargerAdminDtos.EvseResponse> listEvses(String search, String chargerId, int limit, int offset) {
        long total = chargerAdminRepository.countEvses(search, chargerId);
        List<ChargerAdminDtos.EvseResponse> items = chargerAdminRepository.listEvses(search, chargerId, limit, offset);
        return paged(items, total, limit, offset);
    }

    /**
     * Creates create evse for `ChargerAdminService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param request input consumed by createEvse.
     * @return result produced by createEvse.
     */
    @Transactional
    public ChargerAdminDtos.EvseResponse createEvse(ChargerAdminDtos.EvseCreateRequest request) {
        if (!chargerAdminRepository.chargerExists(request.chargerId())) {
            throw new NotFoundException("Charger '%s' was not found".formatted(request.chargerId()));
        }
        String evseId = generateIdentifier("EVSE", request.evseUid());
        try {
            return chargerAdminRepository.createEvse(evseId, request);
        } catch (DuplicateKeyException ex) {
            throw new ConflictException("EVSE with the same uid already exists");
        }
    }

    /**
     * Retrieves list connectors for `ChargerAdminService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param search input consumed by listConnectors.
     * @param evseId input consumed by listConnectors.
     * @param limit input consumed by listConnectors.
     * @param offset input consumed by listConnectors.
     * @return result produced by listConnectors.
     */
    @Transactional(readOnly = true)
    public ChargerAdminDtos.PagedResponse<ChargerAdminDtos.ConnectorResponse> listConnectors(String search, String evseId, int limit, int offset) {
        long total = chargerAdminRepository.countConnectors(search, evseId);
        List<ChargerAdminDtos.ConnectorResponse> items = chargerAdminRepository.listConnectors(search, evseId, limit, offset);
        return paged(items, total, limit, offset);
    }

    /**
     * Creates create connector for `ChargerAdminService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param request input consumed by createConnector.
     * @return result produced by createConnector.
     */
    @Transactional
    public ChargerAdminDtos.ConnectorResponse createConnector(ChargerAdminDtos.ConnectorCreateRequest request) {
        if (!chargerAdminRepository.evseExists(request.evseId())) {
            throw new NotFoundException("EVSE '%s' was not found".formatted(request.evseId()));
        }
        if (chargerAdminRepository.connectorExists(request.connectorId().trim().toUpperCase(Locale.ROOT))) {
            throw new ConflictException("Connector '%s' already exists".formatted(request.connectorId()));
        }
        try {
            return chargerAdminRepository.createConnector(request);
        } catch (DuplicateKeyException ex) {
            throw new ConflictException("Connector '%s' already exists".formatted(request.connectorId()));
        }
    }

    /**
     * Executes paged for `ChargerAdminService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param items input consumed by paged.
     * @param total input consumed by paged.
     * @param limit input consumed by paged.
     * @param offset input consumed by paged.
     * @return result produced by paged.
     */
    private <T> ChargerAdminDtos.PagedResponse<T> paged(List<T> items, long total, int limit, int offset) {
        int currentPage = limit <= 0 ? 0 : offset / limit;
        int totalPages = limit <= 0 ? 0 : (int) Math.ceil((double) total / limit);
        boolean hasNext = offset + limit < total;
        boolean hasPrevious = offset > 0;
        return new ChargerAdminDtos.PagedResponse<>(
                items,
                total,
                limit,
                offset,
                currentPage,
                totalPages,
                hasNext,
                hasPrevious
        );
    }

    /**
     * Executes generate identifier for `ChargerAdminService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param prefix input consumed by generateIdentifier.
     * @param seed input consumed by generateIdentifier.
     * @return result produced by generateIdentifier.
     */
    private String generateIdentifier(String prefix, String seed) {
        String normalizedSeed = seed == null
                ? "GEN"
                : seed.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (normalizedSeed.isBlank()) {
            normalizedSeed = "GEN";
        }
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        return "%s-%s-%s".formatted(prefix, normalizedSeed.substring(0, Math.min(normalizedSeed.length(), 6)), suffix);
    }
}

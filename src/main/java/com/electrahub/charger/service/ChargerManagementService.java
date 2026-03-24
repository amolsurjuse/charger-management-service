package com.electrahub.charger.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.charger.api.dto.ChargerDtos;
import com.electrahub.charger.api.error.ConflictException;
import com.electrahub.charger.api.error.NotFoundException;
import com.electrahub.charger.repository.ChargerRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChargerManagementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChargerManagementService.class);


    private final ChargerRepository chargerRepository;

    /**
     * Executes charger management service for `ChargerManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param chargerRepository input consumed by ChargerManagementService.
     */
    public ChargerManagementService(ChargerRepository chargerRepository) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering ChargerManagementService#ChargerManagementService");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering ChargerManagementService#ChargerManagementService with debug context");
        this.chargerRepository = chargerRepository;
    }

    /**
     * Creates create for `ChargerManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param request input consumed by create.
     * @return result produced by create.
     */
    @Transactional
    public ChargerDtos.ChargerResponse create(ChargerDtos.ChargerUpsertRequest request) {
        if (chargerRepository.existsById(request.chargerId())) {
            throw new ConflictException("Charger with id '%s' already exists".formatted(request.chargerId()));
        }
        try {
            return chargerRepository.create(request);
        } catch (DuplicateKeyException ex) {
            throw new ConflictException("Charge point identity or serial number already exists");
        }
    }

    /**
     * Retrieves list for `ChargerManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param status input consumed by list.
     * @param ocppVersion input consumed by list.
     * @param siteName input consumed by list.
     * @param enabled input consumed by list.
     * @return result produced by list.
     */
    @Transactional(readOnly = true)
    public ChargerDtos.ChargerListResponse list(String status, String ocppVersion, String siteName, Boolean enabled) {
        List<ChargerDtos.ChargerSummary> items = chargerRepository.findAll(status, ocppVersion, siteName, enabled).stream()
                .map(charger -> new ChargerDtos.ChargerSummary(
                        charger.chargerId(),
                        charger.displayName(),
                        charger.chargePointIdentity(),
                        charger.siteName(),
                        charger.ocppVersion(),
                        charger.status(),
                        charger.enabled(),
                        charger.settings().firmware().currentVersion(),
                        charger.lastHeartbeatAt(),
                        charger.updatedAt()
                ))
                .toList();
        return new ChargerDtos.ChargerListResponse(items);
    }

    /**
     * Retrieves get for `ChargerManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param chargerId input consumed by get.
     * @return result produced by get.
     */
    @Transactional(readOnly = true)
    public ChargerDtos.ChargerResponse get(String chargerId) {
        return chargerRepository.findById(chargerId)
                .orElseThrow(() -> new NotFoundException("Charger '%s' was not found".formatted(chargerId)));
    }

    /**
     * Retrieves get settings for `ChargerManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param chargerId input consumed by getSettings.
     * @return result produced by getSettings.
     */
    @Transactional(readOnly = true)
    public ChargerDtos.ChargerSettings getSettings(String chargerId) {
        return get(chargerId).settings();
    }

    /**
     * Updates update for `ChargerManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param chargerId input consumed by update.
     * @param request input consumed by update.
     * @return result produced by update.
     */
    @Transactional
    public ChargerDtos.ChargerResponse update(String chargerId, ChargerDtos.ChargerUpsertRequest request) {
        if (!chargerId.equals(request.chargerId())) {
            throw new IllegalArgumentException("Path charger id must match request charger id");
        }
        ensureExists(chargerId);
        try {
            return chargerRepository.update(chargerId, request);
        } catch (DuplicateKeyException ex) {
            throw new ConflictException("Charge point identity or serial number already exists");
        }
    }

    /**
     * Updates update status for `ChargerManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param chargerId input consumed by updateStatus.
     * @param request input consumed by updateStatus.
     * @return result produced by updateStatus.
     */
    @Transactional
    public ChargerDtos.ChargerResponse updateStatus(String chargerId, ChargerDtos.ChargerStatusUpdateRequest request) {
        ensureExists(chargerId);
        return chargerRepository.updateStatus(chargerId, request);
    }

    /**
     * Removes delete for `ChargerManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param chargerId input consumed by delete.
     */
    @Transactional
    public void delete(String chargerId) {
        ensureExists(chargerId);
        chargerRepository.delete(chargerId);
    }

    /**
     * Executes ensure exists for `ChargerManagementService`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.service`.
     * @param chargerId input consumed by ensureExists.
     */
    private void ensureExists(String chargerId) {
        if (!chargerRepository.existsById(chargerId)) {
            throw new NotFoundException("Charger '%s' was not found".formatted(chargerId));
        }
    }
}

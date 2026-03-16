package com.electrahub.charger.service;

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

    private final ChargerRepository chargerRepository;

    public ChargerManagementService(ChargerRepository chargerRepository) {
        this.chargerRepository = chargerRepository;
    }

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

    @Transactional(readOnly = true)
    public ChargerDtos.ChargerResponse get(String chargerId) {
        return chargerRepository.findById(chargerId)
                .orElseThrow(() -> new NotFoundException("Charger '%s' was not found".formatted(chargerId)));
    }

    @Transactional(readOnly = true)
    public ChargerDtos.ChargerSettings getSettings(String chargerId) {
        return get(chargerId).settings();
    }

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

    @Transactional
    public ChargerDtos.ChargerResponse updateStatus(String chargerId, ChargerDtos.ChargerStatusUpdateRequest request) {
        ensureExists(chargerId);
        return chargerRepository.updateStatus(chargerId, request);
    }

    @Transactional
    public void delete(String chargerId) {
        ensureExists(chargerId);
        chargerRepository.delete(chargerId);
    }

    private void ensureExists(String chargerId) {
        if (!chargerRepository.existsById(chargerId)) {
            throw new NotFoundException("Charger '%s' was not found".formatted(chargerId));
        }
    }
}

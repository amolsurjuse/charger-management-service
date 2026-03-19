package com.electrahub.charger.service;

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

    private final ChargerAdminRepository chargerAdminRepository;

    public ChargerAdminService(ChargerAdminRepository chargerAdminRepository) {
        this.chargerAdminRepository = chargerAdminRepository;
    }

    @Transactional(readOnly = true)
    public ChargerAdminDtos.PagedResponse<ChargerAdminDtos.EnterpriseResponse> listEnterprises(String search, int limit, int offset) {
        long total = chargerAdminRepository.countEnterprises(search);
        List<ChargerAdminDtos.EnterpriseResponse> items = chargerAdminRepository.listEnterprises(search, limit, offset);
        return paged(items, total, limit, offset);
    }

    @Transactional
    public ChargerAdminDtos.EnterpriseResponse createEnterprise(ChargerAdminDtos.EnterpriseCreateRequest request) {
        String enterpriseId = generateIdentifier("ENT", request.countryCode());
        try {
            return chargerAdminRepository.createEnterprise(enterpriseId, request);
        } catch (DuplicateKeyException ex) {
            throw new ConflictException("Enterprise with the same country code and party id already exists");
        }
    }

    @Transactional(readOnly = true)
    public ChargerAdminDtos.PagedResponse<ChargerAdminDtos.NetworkResponse> listNetworks(String search, String enterpriseId, int limit, int offset) {
        long total = chargerAdminRepository.countNetworks(search, enterpriseId);
        List<ChargerAdminDtos.NetworkResponse> items = chargerAdminRepository.listNetworks(search, enterpriseId, limit, offset);
        return paged(items, total, limit, offset);
    }

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

    @Transactional(readOnly = true)
    public ChargerAdminDtos.PagedResponse<ChargerAdminDtos.LocationResponse> listLocations(String search, String networkId, int limit, int offset) {
        long total = chargerAdminRepository.countLocations(search, networkId);
        List<ChargerAdminDtos.LocationResponse> items = chargerAdminRepository.listLocations(search, networkId, limit, offset);
        return paged(items, total, limit, offset);
    }

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

    @Transactional(readOnly = true)
    public ChargerAdminDtos.PagedResponse<ChargerAdminDtos.ChargerResponse> listChargers(String search, String locationId, int limit, int offset) {
        long total = chargerAdminRepository.countChargers(search, locationId);
        List<ChargerAdminDtos.ChargerResponse> items = chargerAdminRepository.listChargers(search, locationId, limit, offset);
        return paged(items, total, limit, offset);
    }

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

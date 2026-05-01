package com.electrahub.charger.service;

import com.electrahub.charger.config.BillingServiceProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST client that resolves tariff IDs to full pricing details from the billing-service.
 *
 * <p>Used by {@link OcpiChargerGraphqlService} to enrich OCPI charger GraphQL responses
 * with human-readable pricing information (energy price, time price, flat fee, etc.).
 * Results are cached in-memory per request lifecycle to avoid redundant HTTP calls.
 *
 * <p>Gracefully degrades: if the billing-service is unreachable or a tariff ID is
 * not found, the enrichment step is silently skipped for that tariff.
 */
@Service
public class BillingTariffClient {

    private static final Logger log = LoggerFactory.getLogger(BillingTariffClient.class);

    private final BillingServiceProperties billingServiceProperties;
    private final RestClient restClient;
    private final Map<String, Optional<TariffDetailDto>> cache = new ConcurrentHashMap<>();

    public BillingTariffClient(
            BillingServiceProperties billingServiceProperties,
            RestClient.Builder restClientBuilder
    ) {
        this.billingServiceProperties = billingServiceProperties;
        this.restClient = restClientBuilder.baseUrl(billingServiceProperties.getUrl()).build();
    }

    /**
     * Resolves a collection of tariff ID strings to their full tariff details.
     * Returns a map keyed by tariff ID string. Missing or unreachable tariffs are silently skipped.
     */
    public Map<String, TariffDetailDto> resolveTariffs(Collection<String> tariffIds) {
        if (!billingServiceProperties.isEnabled() || tariffIds == null || tariffIds.isEmpty()) {
            return Map.of();
        }

        Map<String, TariffDetailDto> result = new LinkedHashMap<>();
        for (String tariffId : new LinkedHashSet<>(tariffIds)) {
            if (tariffId == null || tariffId.isBlank()) continue;
            String trimmed = tariffId.trim();

            Optional<TariffDetailDto> cached = cache.get(trimmed);
            if (cached != null) {
                cached.ifPresent(dto -> result.put(trimmed, dto));
                continue;
            }

            try {
                BillingTariffResponse response = restClient.get()
                        .uri("/api/v1/billing/tariffs/{tariffId}", UUID.fromString(trimmed))
                        .retrieve()
                        .body(BillingTariffResponse.class);

                if (response != null && response.id() != null) {
                    TariffDetailDto dto = new TariffDetailDto(
                            response.id().toString(),
                            response.name(),
                            response.description(),
                            response.currency(),
                            response.energyPrice(),
                            response.timePrice(),
                            response.parkingPrice(),
                            response.flatFee(),
                            response.minPrice(),
                            response.maxPrice()
                    );
                    cache.put(trimmed, Optional.of(dto));
                    result.put(trimmed, dto);
                } else {
                    cache.put(trimmed, Optional.empty());
                }
            } catch (RestClientException | IllegalArgumentException ex) {
                log.warn("Failed to resolve tariff {} from billing-service: {}", trimmed, ex.getMessage());
            }
        }
        return result;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BillingTariffResponse(
            UUID id,
            String name,
            String description,
            String currency,
            BigDecimal energyPrice,
            BigDecimal timePrice,
            BigDecimal parkingPrice,
            BigDecimal flatFee,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {}

    /**
     * Public DTO representing resolved tariff details exposed via GraphQL.
     */
    public record TariffDetailDto(
            String tariffId,
            String name,
            String description,
            String currency,
            BigDecimal energyPrice,
            BigDecimal timePrice,
            BigDecimal parkingPrice,
            BigDecimal flatFee,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {}
}

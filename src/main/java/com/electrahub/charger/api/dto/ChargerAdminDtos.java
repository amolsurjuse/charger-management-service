package com.electrahub.charger.api.dto;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class ChargerAdminDtos {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChargerAdminDtos.class);


    /**
     * Executes charger admin dtos for `ChargerAdminDtos`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.api.dto`.
     */
    private ChargerAdminDtos() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering ChargerAdminDtos#ChargerAdminDtos");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering ChargerAdminDtos#ChargerAdminDtos with debug context");
    }

    public enum OcppVersion {
        OCPP16J,
        OCPP201
    }

    public record PagedResponse<T>(
            List<T> items,
            long total,
            int limit,
            int offset,
            int currentPage,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {
    }

    public record EnterpriseCreateRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Pattern(regexp = "^[A-Z]{2}$", message = "countryCode must be an uppercase 2-letter ISO code") String countryCode,
            @NotBlank @Pattern(regexp = "^[A-Z0-9]{3}$", message = "partyId must be 3 uppercase letters or digits") String partyId,
            @NotBlank @Size(max = 64) String timezone,
            Boolean enabled
    ) {
    }

    public record EnterpriseResponse(
            String enterpriseId,
            String name,
            String countryCode,
            String partyId,
            String timezone,
            boolean enabled,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record NetworkCreateRequest(
            @NotBlank @Size(max = 64) String enterpriseId,
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 32) String region,
            @NotBlank @Email @Size(max = 128) String operatorEmail,
            Boolean enabled
    ) {
    }

    public record NetworkResponse(
            String networkId,
            String enterpriseId,
            String enterpriseName,
            String name,
            String region,
            String operatorEmail,
            boolean enabled,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record LocationCreateRequest(
            @NotBlank @Size(max = 64) String networkId,
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 96) String city,
            @NotBlank @Size(max = 255) String address,
            @NotBlank @Size(max = 64) String ocpiLocationId,
            Boolean enabled
    ) {
    }

    public record LocationResponse(
            String locationId,
            String networkId,
            String networkName,
            String name,
            String city,
            String address,
            String ocpiLocationId,
            boolean enabled,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record ChargerCreateRequest(
            @NotBlank @Size(max = 64) String chargerId,
            @NotBlank @Size(max = 160) String displayName,
            @NotBlank @Size(max = 64) String locationId,
            @NotBlank @Size(max = 128) String model,
            @NotNull OcppVersion ocppVersion,
            @NotNull @DecimalMin("1.0") @DecimalMax("1000.0") BigDecimal maxPowerKw,
            Boolean enabled
    ) {
    }

    public record ChargerResponse(
            String chargerId,
            String displayName,
            String locationId,
            String locationName,
            String networkId,
            String networkName,
            String enterpriseId,
            String enterpriseName,
            String model,
            OcppVersion ocppVersion,
            BigDecimal maxPowerKw,
            boolean enabled,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record EvseCreateRequest(
            @NotBlank @Size(max = 64) String chargerId,
            @NotBlank @Size(max = 64) String evseUid,
            @Size(max = 64) String zone,
            @Size(max = 255) String capabilities,
            Boolean enabled
    ) {
    }

    public record EvseResponse(
            String evseId,
            String chargerId,
            String chargerDisplayName,
            String evseUid,
            String zone,
            int connectorCount,
            String capabilities,
            boolean enabled,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record ConnectorCreateRequest(
            @NotBlank @Size(max = 64) String connectorId,
            @NotBlank @Size(max = 64) String evseId,
            @NotBlank @Size(max = 32) String standard,
            @NotBlank @Size(max = 16) String format,
            @NotBlank @Size(max = 32) String powerType,
            @NotNull @DecimalMin("0.1") @DecimalMax("1000.0") BigDecimal maxPowerKw,
            List<@NotBlank @Size(max = 36) String> ocpiTariffIds,
            Boolean enabled
    ) {
    }

    public record ConnectorResponse(
            String connectorId,
            String evseId,
            String evseUid,
            String chargerId,
            String standard,
            String format,
            String powerType,
            BigDecimal maxPowerKw,
            List<String> ocpiTariffIds,
            boolean enabled,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record PageRequest(
            @Min(1) @Max(200) int limit,
            @Min(0) int offset
    ) {
    }
}

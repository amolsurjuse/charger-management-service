package com.electrahub.charger.api.dto;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class ChargerDtos {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChargerDtos.class);


    /**
     * Executes charger dtos for `ChargerDtos`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.api.dto`.
     */
    private ChargerDtos() {
        LOGGER.info("CODEx_ENTRY_LOG: Entering ChargerDtos#ChargerDtos");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering ChargerDtos#ChargerDtos with debug context");
    }

    public enum OcppVersion {
        OCPP16J,
        OCPP201
    }

    public enum ChargerStatus {
        PROVISIONING,
        AVAILABLE,
        CHARGING,
        FAULTED,
        MAINTENANCE,
        OFFLINE,
        DECOMMISSIONED
    }

    public enum ConnectorStatus {
        AVAILABLE,
        PREPARING,
        CHARGING,
        SUSPENDED_EV,
        SUSPENDED_EVSE,
        FINISHING,
        UNAVAILABLE,
        FAULTED
    }

    public record ChargerUpsertRequest(
            @NotBlank @Size(max = 64) String chargerId,
            @NotBlank @Size(max = 128) String displayName,
            @NotBlank @Size(max = 128) String chargePointIdentity,
            @NotBlank @Size(max = 128) String vendor,
            @NotBlank @Size(max = 128) String model,
            @NotBlank @Size(max = 128) String serialNumber,
            @Size(max = 128) String siteName,
            @NotNull OcppVersion ocppVersion,
            @NotNull ChargerStatus status,
            Boolean enabled,
            OffsetDateTime lastHeartbeatAt,
            @NotEmpty List<@Valid ConnectorConfiguration> connectors,
            @NotNull @Valid ChargerSettings settings,
            Map<String, String> metadata
    ) {
    }

    public record ChargerStatusUpdateRequest(
            @NotNull ChargerStatus status,
            Boolean enabled,
            OffsetDateTime lastHeartbeatAt
    ) {
    }

    public record ChargerSummary(
            String chargerId,
            String displayName,
            String chargePointIdentity,
            String siteName,
            OcppVersion ocppVersion,
            ChargerStatus status,
            boolean enabled,
            String firmwareVersion,
            OffsetDateTime lastHeartbeatAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record ChargerListResponse(
            List<ChargerSummary> items
    ) {
    }

    public record ChargerResponse(
            String chargerId,
            String displayName,
            String chargePointIdentity,
            String vendor,
            String model,
            String serialNumber,
            String siteName,
            OcppVersion ocppVersion,
            ChargerStatus status,
            boolean enabled,
            OffsetDateTime lastHeartbeatAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            List<ConnectorConfiguration> connectors,
            ChargerSettings settings,
            Map<String, String> metadata
    ) {
    }

    public record ConnectorConfiguration(
            @NotNull @Min(1) @Max(32) Integer connectorId,
            @NotBlank @Size(max = 64) String type,
            @NotNull @DecimalMin("0.1") @DecimalMax("1000.0") BigDecimal maxPowerKw,
            @NotNull ConnectorStatus status,
            Boolean cableAttached,
            Boolean supportsPlugAndCharge,
            @Pattern(regexp = "^[A-Z0-9_\\-]{0,32}$", message = "phase map must be uppercase and underscore separated")
            String meterPhaseMap
    ) {
    }

    public record ChargerSettings(
            @NotNull @Valid ConnectivitySettings connectivity,
            @NotNull @Valid ElectricalSettings electrical,
            @NotNull @Valid MeteringSettings metering,
            @NotNull @Valid SecuritySettings security,
            @NotNull @Valid SmartChargingSettings smartCharging,
            @NotNull @Valid FirmwareSettings firmware,
            @NotNull @Valid DiagnosticsSettings diagnostics,
            @NotNull @Valid MaintenanceSettings maintenance
    ) {
    }

    public record ConnectivitySettings(
            @NotBlank @Size(max = 32) String protocol,
            @NotBlank @Size(max = 256) String websocketUrl,
            @NotBlank @Size(max = 64) String networkInterface,
            Boolean simEnabled,
            @Size(max = 64) String staticIp,
            @Size(max = 64) String dnsPrimary,
            @Size(max = 64) String dnsSecondary,
            @NotNull @Min(5) @Max(3600) Integer reconnectIntervalSec,
            @NotNull @Min(10) @Max(3600) Integer heartbeatIntervalSec
    ) {
    }

    public record ElectricalSettings(
            @NotNull @Min(110) @Max(1000) Integer nominalVoltage,
            @NotNull @DecimalMin("1.0") @DecimalMax("1000.0") BigDecimal maxCurrentA,
            @NotNull @DecimalMin("0.1") @DecimalMax("1000.0") BigDecimal siteMaxPowerKw,
            @NotNull @Min(1) @Max(3) Integer phaseCount,
            @NotNull @DecimalMin("0.10") @DecimalMax("1.00") BigDecimal powerFactor
    ) {
    }

    public record MeteringSettings(
            @NotNull @Min(1) @Max(3600) Integer sampleIntervalSec,
            Boolean signedDataEnabled,
            Boolean powerQualityCaptureEnabled,
            @NotNull @Min(1) @Max(365) Integer storageRetentionDays
    ) {
    }

    public record SecuritySettings(
            Boolean tlsEnabled,
            Boolean mutualTlsEnabled,
            @NotBlank @Size(max = 64) String certificateProfile,
            @NotNull @Min(1) @Max(365) Integer authTokenRotationDays,
            Boolean secureBootEnabled,
            Boolean allowOfflineAuthorization
    ) {
    }

    public record SmartChargingSettings(
            Boolean localLoadBalancingEnabled,
            @NotNull @Min(1) @Max(20) Integer maxProfileStackLevel,
            Boolean chargingProfileSupportEnabled,
            Boolean reservationEnabled,
            @NotNull @DecimalMin("0.1") @DecimalMax("1000.0") BigDecimal minimumChargingRateKw
    ) {
    }

    public record FirmwareSettings(
            Boolean autoUpdateEnabled,
            @NotBlank @Size(max = 32) String channel,
            @NotBlank @Size(max = 64) String currentVersion,
            @NotNull @Min(0) @Max(100) Integer stagedRolloutPercent,
            Boolean checksumValidationEnabled
    ) {
    }

    public record DiagnosticsSettings(
            Boolean remoteLogUploadEnabled,
            @NotBlank @Size(max = 16) String logLevel,
            @NotNull @Min(10) @Max(86400) Integer metricsPushIntervalSec,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal traceSamplingRate
    ) {
    }

    public record MaintenanceSettings(
            @NotNull @Min(1) @Max(365) Integer preventiveMaintenanceIntervalDays,
            Boolean remoteResetEnabled,
            @NotNull @Min(100) @Max(10000000) Integer contactorCycleLimit,
            Boolean connectorLockMonitoringEnabled
    ) {
    }
}

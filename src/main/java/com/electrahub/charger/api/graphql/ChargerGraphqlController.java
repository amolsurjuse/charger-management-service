package com.electrahub.charger.api.graphql;

import com.electrahub.charger.api.dto.ChargerDtos;
import com.electrahub.charger.service.ChargerManagementService;
import com.electrahub.charger.service.OcpiChargerGraphqlService;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ChargerGraphqlController {

    private final ChargerManagementService chargerManagementService;
    private final OcpiChargerGraphqlService ocpiChargerGraphqlService;

    public ChargerGraphqlController(
            ChargerManagementService chargerManagementService,
            OcpiChargerGraphqlService ocpiChargerGraphqlService
    ) {
        this.chargerManagementService = chargerManagementService;
        this.ocpiChargerGraphqlService = ocpiChargerGraphqlService;
    }

    @QueryMapping
    public List<ChargerSummaryGraphqlDto> chargers(
            @Argument String status,
            @Argument String ocppVersion,
            @Argument String siteName,
            @Argument Boolean enabled
    ) {
        return chargerManagementService.list(status, ocppVersion, siteName, enabled)
                .items()
                .stream()
                .map(this::toGraphqlDto)
                .toList();
    }

    @QueryMapping
    public List<OcpiChargerGraphqlService.OcpiChargerGraphqlDto> ocpiChargers(
            @Argument String countryCode,
            @Argument String partyId,
            @Argument String ocpiLocationId,
            @Argument String chargerId,
            @Argument String city,
            @Argument String connectorStatus,
            @Argument String connectorId,
            @Argument String connectorStandard,
            @Argument String connectorFormat,
            @Argument String powerType,
            @Argument String tariffId,
            @Argument Boolean active,
            @Argument String search,
            @Argument Integer limit,
            @Argument Integer offset,
            DataFetchingEnvironment environment
    ) {
        return ocpiChargerGraphqlService.listChargers(
                new OcpiChargerGraphqlService.OcpiChargerFilter(
                        countryCode,
                        partyId,
                        ocpiLocationId,
                        chargerId,
                        city,
                        connectorStatus,
                        connectorId,
                        connectorStandard,
                        connectorFormat,
                        powerType,
                        tariffId,
                        active,
                        search,
                        limit,
                        offset
                ),
                environment.getSelectionSet()
        );
    }

    @QueryMapping
    public OcpiChargerGraphqlService.OcpiChargerGraphqlDto ocpiCharger(
            @Argument String chargerId,
            @Argument String connectorId,
            DataFetchingEnvironment environment
    ) {
        return ocpiChargerGraphqlService.viewCharger(chargerId, connectorId, environment.getSelectionSet());
    }

    private ChargerSummaryGraphqlDto toGraphqlDto(ChargerDtos.ChargerSummary summary) {
        return new ChargerSummaryGraphqlDto(
                summary.chargerId(),
                summary.displayName(),
                summary.chargePointIdentity(),
                summary.siteName(),
                summary.ocppVersion().name(),
                summary.status().name(),
                summary.enabled(),
                summary.firmwareVersion(),
                summary.lastHeartbeatAt() == null ? null : summary.lastHeartbeatAt().toString(),
                summary.updatedAt() == null ? null : summary.updatedAt().toString()
        );
    }

    public record ChargerSummaryGraphqlDto(
            String chargerId,
            String displayName,
            String chargePointIdentity,
            String siteName,
            String ocppVersion,
            String status,
            boolean enabled,
            String firmwareVersion,
            String lastHeartbeatAt,
            String updatedAt
    ) {
    }
}

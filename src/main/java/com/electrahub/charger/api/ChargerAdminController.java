package com.electrahub.charger.api;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.electrahub.charger.api.dto.ChargerAdminDtos;
import com.electrahub.charger.service.ChargerAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/admin")
@Tag(name = "Charger Admin", description = "Admin APIs for enterprise, network, location, and charger management")
public class ChargerAdminController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChargerAdminController.class);


    private final ChargerAdminService chargerAdminService;

    /**
     * Executes charger admin controller for `ChargerAdminController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.api`.
     * @param chargerAdminService input consumed by ChargerAdminController.
     */
    public ChargerAdminController(ChargerAdminService chargerAdminService) {
        LOGGER.info(" Entering ChargerAdminController#ChargerAdminController");
        LOGGER.debug(" Entering ChargerAdminController#ChargerAdminController with debug context");
        this.chargerAdminService = chargerAdminService;
    }

    @GetMapping("/enterprises")
    @Operation(summary = "List enterprises")
    public ChargerAdminDtos.PagedResponse<ChargerAdminDtos.EnterpriseResponse> listEnterprises(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "25") @Min(1) @Max(200) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return chargerAdminService.listEnterprises(search, limit, offset);
    }

    @PostMapping("/enterprises")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create enterprise")
    public ChargerAdminDtos.EnterpriseResponse createEnterprise(
            @Valid @RequestBody ChargerAdminDtos.EnterpriseCreateRequest request
    ) {
        return chargerAdminService.createEnterprise(request);
    }

    @GetMapping("/networks")
    @Operation(summary = "List networks")
    public ChargerAdminDtos.PagedResponse<ChargerAdminDtos.NetworkResponse> listNetworks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String enterpriseId,
            @RequestParam(defaultValue = "25") @Min(1) @Max(200) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return chargerAdminService.listNetworks(search, enterpriseId, limit, offset);
    }

    @PostMapping("/networks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create network")
    public ChargerAdminDtos.NetworkResponse createNetwork(
            @Valid @RequestBody ChargerAdminDtos.NetworkCreateRequest request
    ) {
        return chargerAdminService.createNetwork(request);
    }

    @GetMapping("/locations")
    @Operation(summary = "List locations")
    public ChargerAdminDtos.PagedResponse<ChargerAdminDtos.LocationResponse> listLocations(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String networkId,
            @RequestParam(defaultValue = "25") @Min(1) @Max(200) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return chargerAdminService.listLocations(search, networkId, limit, offset);
    }

    @PostMapping("/locations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create location")
    public ChargerAdminDtos.LocationResponse createLocation(
            @Valid @RequestBody ChargerAdminDtos.LocationCreateRequest request
    ) {
        return chargerAdminService.createLocation(request);
    }

    @GetMapping("/chargers")
    @Operation(summary = "List chargers")
    public ChargerAdminDtos.PagedResponse<ChargerAdminDtos.ChargerResponse> listChargers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String locationId,
            @RequestParam(defaultValue = "25") @Min(1) @Max(200) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return chargerAdminService.listChargers(search, locationId, limit, offset);
    }

    @GetMapping("/chargers/connectors")
    @Operation(summary = "View chargers with nested connectors")
    public ChargerAdminDtos.ChargerConnectorViewResponse viewChargersAndConnectors(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "25") @Min(1) @Max(200) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return chargerAdminService.viewChargersAndConnectors(search, limit, offset);
    }

    @PostMapping("/chargers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create charger")
    public ChargerAdminDtos.ChargerResponse createCharger(
            @Valid @RequestBody ChargerAdminDtos.ChargerCreateRequest request
    ) {
        return chargerAdminService.createCharger(request);
    }

    @GetMapping("/evses")
    @Operation(summary = "List EVSEs")
    public ChargerAdminDtos.PagedResponse<ChargerAdminDtos.EvseResponse> listEvses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String chargerId,
            @RequestParam(defaultValue = "25") @Min(1) @Max(200) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return chargerAdminService.listEvses(search, chargerId, limit, offset);
    }

    @PostMapping("/evses")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create EVSE")
    public ChargerAdminDtos.EvseResponse createEvse(
            @Valid @RequestBody ChargerAdminDtos.EvseCreateRequest request
    ) {
        return chargerAdminService.createEvse(request);
    }

    @GetMapping("/connectors")
    @Operation(summary = "List connectors")
    public ChargerAdminDtos.PagedResponse<ChargerAdminDtos.ConnectorResponse> listConnectors(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String evseId,
            @RequestParam(defaultValue = "25") @Min(1) @Max(200) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return chargerAdminService.listConnectors(search, evseId, limit, offset);
    }

    @PostMapping("/connectors")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create connector")
    public ChargerAdminDtos.ConnectorResponse createConnector(
            @Valid @RequestBody ChargerAdminDtos.ConnectorCreateRequest request
    ) {
        return chargerAdminService.createConnector(request);
    }

    @PostMapping("/connectors/search/reindex")
    @Operation(summary = "Reindex OCPI connector search documents to Elasticsearch")
    public ChargerAdminDtos.ConnectorSearchReindexResponse reindexConnectorSearch() {
        return chargerAdminService.reindexConnectorSearch();
    }

    @PostMapping("/connectors/search/sync/{connectorId}")
    @Operation(summary = "Sync one connector OCPI search document by connector id")
    public ChargerAdminDtos.ConnectorSearchSyncResponse syncConnectorSearchByConnectorId(@PathVariable String connectorId) {
        return chargerAdminService.syncConnectorSearchByConnectorId(connectorId);
    }

    @PostMapping("/connectors/search/sync/evse/{evseId}")
    @Operation(summary = "Sync connector OCPI search documents by EVSE id")
    public ChargerAdminDtos.ConnectorSearchSyncResponse syncConnectorSearchByEvseId(@PathVariable String evseId) {
        return chargerAdminService.syncConnectorSearchByEvseId(evseId);
    }

    @PostMapping("/connectors/search/sync/charger/{chargerId}")
    @Operation(summary = "Sync connector OCPI search documents by charger id")
    public ChargerAdminDtos.ConnectorSearchSyncResponse syncConnectorSearchByChargerId(@PathVariable String chargerId) {
        return chargerAdminService.syncConnectorSearchByChargerId(chargerId);
    }
}

package com.electrahub.charger.api;

import com.electrahub.charger.api.dto.ChargerDtos;
import com.electrahub.charger.service.ChargerManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/chargers")
@Tag(name = "Chargers", description = "Manage charger inventory, lifecycle, and production configuration")
public class ChargerManagementController {

    private final ChargerManagementService chargerManagementService;

    public ChargerManagementController(ChargerManagementService chargerManagementService) {
        this.chargerManagementService = chargerManagementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new charger with full production settings")
    public ChargerDtos.ChargerResponse create(@Valid @RequestBody ChargerDtos.ChargerUpsertRequest request) {
        return chargerManagementService.create(request);
    }

    @GetMapping
    @Operation(summary = "List chargers with optional fleet filters")
    public ChargerDtos.ChargerListResponse list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ocppVersion,
            @RequestParam(required = false) String siteName,
            @RequestParam(required = false) Boolean enabled
    ) {
        return chargerManagementService.list(status, ocppVersion, siteName, enabled);
    }

    @GetMapping("/{chargerId}")
    @Operation(summary = "Get a charger and all stored settings")
    public ChargerDtos.ChargerResponse get(@PathVariable String chargerId) {
        return chargerManagementService.get(chargerId);
    }

    @GetMapping("/{chargerId}/settings")
    @Operation(summary = "Get only the production configuration block for a charger")
    public ChargerDtos.ChargerSettings getSettings(@PathVariable String chargerId) {
        return chargerManagementService.getSettings(chargerId);
    }

    @PutMapping("/{chargerId}")
    @Operation(summary = "Replace a charger definition and configuration")
    public ChargerDtos.ChargerResponse update(
            @PathVariable String chargerId,
            @Valid @RequestBody ChargerDtos.ChargerUpsertRequest request
    ) {
        return chargerManagementService.update(chargerId, request);
    }

    @PatchMapping("/{chargerId}/status")
    @Operation(summary = "Update charger lifecycle status and readiness markers")
    public ChargerDtos.ChargerResponse updateStatus(
            @PathVariable String chargerId,
            @Valid @RequestBody ChargerDtos.ChargerStatusUpdateRequest request
    ) {
        return chargerManagementService.updateStatus(chargerId, request);
    }

    @DeleteMapping("/{chargerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a charger from inventory")
    public void delete(@PathVariable String chargerId) {
        chargerManagementService.delete(chargerId);
    }
}

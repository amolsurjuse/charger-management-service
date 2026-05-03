package com.electrahub.charger.service;

import com.electrahub.charger.config.SessionServiceProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChargerSessionLookupService {

    private static final Logger log = LoggerFactory.getLogger(ChargerSessionLookupService.class);

    private final SessionServiceProperties sessionServiceProperties;
    private final RestClient restClient;

    public ChargerSessionLookupService(
            SessionServiceProperties sessionServiceProperties,
            RestClient.Builder restClientBuilder
    ) {
        this.sessionServiceProperties = sessionServiceProperties;
        this.restClient = restClientBuilder.baseUrl(sessionServiceProperties.getUrl()).build();
    }

    public Map<String, ActiveSessionDto> findActiveSessionsByChargerIds(Collection<String> chargerIds) {
        if (!sessionServiceProperties.isEnabled() || chargerIds == null || chargerIds.isEmpty()) {
            return Map.of();
        }

        Map<String, ActiveSessionDto> sessionsByChargerId = new LinkedHashMap<>();
        LinkedHashSet<String> unresolvedChargerIds = new LinkedHashSet<>();
        for (String chargerId : new LinkedHashSet<>(chargerIds)) {
            UUID stationId = parseStationId(chargerId);
            if (stationId == null) {
                if (chargerId != null && !chargerId.isBlank()) {
                    unresolvedChargerIds.add(chargerId.trim());
                }
                continue;
            }

            Optional<ActiveSessionDto> activeSession = fetchActiveSession(stationId);
            activeSession.ifPresent(session -> sessionsByChargerId.put(chargerId, session));
        }

        if (!unresolvedChargerIds.isEmpty()) {
            Map<String, ActiveSessionDto> fallbackSessions = fetchActiveSessionsByStringStationId(unresolvedChargerIds);
            fallbackSessions.forEach(sessionsByChargerId::putIfAbsent);
        }

        return sessionsByChargerId;
    }

    private Optional<ActiveSessionDto> fetchActiveSession(UUID stationId) {
        Optional<ActiveSessionDto> active = fetchActiveSession(stationId, "ACTIVE");
        if (active.isPresent()) {
            return active;
        }
        return fetchActiveSession(stationId, "SUSPENDED");
    }

    private Optional<ActiveSessionDto> fetchActiveSession(UUID stationId, String status) {
        try {
            SessionPageResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/sessions")
                            .queryParam("page", 0)
                            .queryParam("size", 1)
                            .queryParam("stationId", stationId)
                            .queryParam("status", status)
                            .build())
                    .retrieve()
                    .body(SessionPageResponse.class);

            if (response == null || response.content() == null || response.content().isEmpty()) {
                return Optional.empty();
            }

            SessionSummary summary = response.content().getFirst();
            String sessionId = summary.id() == null ? null : summary.id().toString();
            if (sessionId == null || sessionId.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(new ActiveSessionDto(
                    sessionId,
                    summary.driverId() == null ? null : summary.driverId().toString(),
                    summary.status(),
                    summary.startedAt() == null ? null : summary.startedAt().toString(),
                    null
            ));
        } catch (RestClientException ex) {
            log.debug("Failed to lookup {} charging session for station {}", status, stationId, ex);
            return Optional.empty();
        }
    }

    private UUID parseStationId(String chargerId) {
        if (chargerId == null || chargerId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(chargerId.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Map<String, ActiveSessionDto> fetchActiveSessionsByStringStationId(LinkedHashSet<String> chargerIds) {
        try {
            ChargerActiveSession[] sessions = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/api/v1/sessions/internal/active-by-chargers");
                        chargerIds.forEach(chargerId -> builder.queryParam("chargerIds", chargerId));
                        return builder.build();
                    })
                    .retrieve()
                    .body(ChargerActiveSession[].class);

            if (sessions == null || sessions.length == 0) {
                return Map.of();
            }

            Map<String, ActiveSessionDto> sessionsByChargerId = new LinkedHashMap<>();
            for (ChargerActiveSession session : sessions) {
                String chargerId = trimToNull(session.chargerId());
                if (chargerId == null || !chargerIds.contains(chargerId)) {
                    continue;
                }

                if (!isPotentiallyActive(session.status())) {
                    continue;
                }

                String sessionId = trimToNull(session.id());
                if (sessionId == null) {
                    continue;
                }

                String userId = trimToNull(session.userId());
                if (userId == null) {
                    userId = trimToNull(session.driverId());
                }

                sessionsByChargerId.putIfAbsent(
                        chargerId,
                        new ActiveSessionDto(
                                sessionId,
                                userId,
                                trimToNull(session.status()),
                                trimToNull(session.startedAt()),
                                trimToNull(session.connectorRef())
                        )
                );
            }
            return sessionsByChargerId;
        } catch (RestClientException ex) {
            log.debug("Failed to lookup active charging sessions by charger ids", ex);
            return Map.of();
        }
    }

    private boolean isPotentiallyActive(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return true;
        }

        String value = normalized.toUpperCase();
        return value.equals("ACTIVE")
                || value.equals("CHARGING")
                || value.equals("PREPARING")
                || value.equals("SUSPENDED");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ActiveSessionDto(
            String id,
            String userId,
            String status,
            String startedAt,
            String connectorRef
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SessionPageResponse(List<SessionSummary> content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SessionSummary(
            UUID id,
            UUID driverId,
            String status,
            OffsetDateTime startedAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChargerActiveSession(
            String id,
            String chargerId,
            String connectorRef,
            String status,
            String startedAt,
            String userId,
            String driverId
    ) {
    }
}

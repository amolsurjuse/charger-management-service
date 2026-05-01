package com.electrahub.charger.api;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthController.class);


    /**
     * Executes healthz for `HealthController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.api`.
     * @return result produced by healthz.
     */
    @GetMapping("/healthz")
    public Map<String, String> healthz() {
        LOGGER.info(" Entering HealthController#healthz");
        LOGGER.debug(" Entering HealthController#healthz with debug context");
        return Map.of("status", "ok");
    }

    /**
     * Retrieves readyz for `HealthController`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.api`.
     * @return result produced by readyz.
     */
    @GetMapping("/readyz")
    public Map<String, String> readyz() {
        return Map.of("status", "ready");
    }
}

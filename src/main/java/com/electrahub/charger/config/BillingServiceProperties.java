package com.electrahub.charger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the billing-service integration.
 *
 * <p>Allows the charger-management-service to resolve tariff IDs into
 * full pricing details by calling the billing-service REST API.
 * Disable via {@code app.services.billing.enabled=false} to skip
 * tariff enrichment when the billing-service is unavailable.
 */
@Component
@ConfigurationProperties(prefix = "app.services.billing")
public class BillingServiceProperties {

    private boolean enabled = true;
    private String url = "http://billing-service:8085";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

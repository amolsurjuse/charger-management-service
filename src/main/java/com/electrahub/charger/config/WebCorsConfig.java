package com.electrahub.charger.config;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebCorsConfig.class);


    private final CorsProperties corsProperties;

    /**
     * Executes web cors config for `WebCorsConfig`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.config`.
     * @param corsProperties input consumed by WebCorsConfig.
     */
    public WebCorsConfig(CorsProperties corsProperties) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering WebCorsConfig#WebCorsConfig");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering WebCorsConfig#WebCorsConfig with debug context");
        this.corsProperties = corsProperties;
    }

    /**
     * Creates add cors mappings for `WebCorsConfig`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.config`.
     * @param registry input consumed by addCorsMappings.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(corsProperties.getAllowedOriginPatterns().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

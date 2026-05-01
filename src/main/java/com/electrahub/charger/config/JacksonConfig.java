package com.electrahub.charger.config;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(JacksonConfig.class);


    /**
     * Executes object mapper for `JacksonConfig`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.config`.
     * @return result produced by objectMapper.
     */
    @Bean
    ObjectMapper objectMapper() {
        LOGGER.info(" Entering JacksonConfig#objectMapper");
        LOGGER.debug(" Entering JacksonConfig#objectMapper with debug context");
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }
}

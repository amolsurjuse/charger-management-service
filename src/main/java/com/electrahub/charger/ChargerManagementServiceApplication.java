package com.electrahub.charger;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChargerManagementServiceApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChargerManagementServiceApplication.class);


    /**
     * Executes main for `ChargerManagementServiceApplication`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger`.
     * @param args input consumed by main.
     */
    public static void main(String[] args) {
        LOGGER.info(" Entering ChargerManagementServiceApplication#main");
        LOGGER.debug(" Entering ChargerManagementServiceApplication#main with debug context");
        SpringApplication.run(ChargerManagementServiceApplication.class, args);
    }
}

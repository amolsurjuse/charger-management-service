package com.electrahub.charger.api.error;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
public class NotFoundException extends RuntimeException {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotFoundException.class);


    /**
     * Executes not found exception for `NotFoundException`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.api.error`.
     * @param message input consumed by NotFoundException.
     */
    public NotFoundException(String message) {
        super(message);
        LOGGER.info(" Entering NotFoundException#NotFoundException");
        LOGGER.debug(" Entering NotFoundException#NotFoundException with debug context");
    }
}

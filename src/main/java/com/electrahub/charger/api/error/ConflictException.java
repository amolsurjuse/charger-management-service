package com.electrahub.charger.api.error;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
public class ConflictException extends RuntimeException {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConflictException.class);


    /**
     * Executes conflict exception for `ConflictException`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.api.error`.
     * @param message input consumed by ConflictException.
     */
    public ConflictException(String message) {
        super(message);
        LOGGER.info("CODEx_ENTRY_LOG: Entering ConflictException#ConflictException");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering ConflictException#ConflictException with debug context");
    }
}

package com.electrahub.charger.api.error;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class RestExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestExceptionHandler.class);


    /**
     * Processes handle not found for `RestExceptionHandler`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.api.error`.
     * @param ex input consumed by handleNotFound.
     * @return result produced by handleNotFound.
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        LOGGER.info(" Entering RestExceptionHandler#handleNotFound");
        LOGGER.debug(" Entering RestExceptionHandler#handleNotFound with debug context");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("NOT_FOUND", ex.getMessage(), OffsetDateTime.now(), List.of()));
    }

    /**
     * Processes handle conflict for `RestExceptionHandler`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.api.error`.
     * @param ex input consumed by handleConflict.
     * @return result produced by handleConflict.
     */
    @ExceptionHandler({ConflictException.class, DuplicateKeyException.class})
    public ResponseEntity<ApiError> handleConflict(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("CONFLICT", ex.getMessage(), OffsetDateTime.now(), List.of()));
    }

    /**
     * Processes handle validation for `RestExceptionHandler`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.api.error`.
     * @param ex input consumed by handleValidation.
     * @return result produced by handleValidation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getAllErrors().stream()
                .map(err -> err instanceof FieldError fe ? fe.getField() + ": " + fe.getDefaultMessage() : err.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_ERROR", "Request validation failed", OffsetDateTime.now(), details));
    }

    /**
     * Processes handle constraint violation for `RestExceptionHandler`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.api.error`.
     * @param ex input consumed by handleConstraintViolation.
     * @return result produced by handleConstraintViolation.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_ERROR", "Request validation failed", OffsetDateTime.now(), details));
    }

    /**
     * Processes handle illegal argument for `RestExceptionHandler`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.api.error`.
     * @param ex input consumed by handleIllegalArgument.
     * @return result produced by handleIllegalArgument.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError("BAD_REQUEST", ex.getMessage(), OffsetDateTime.now(), List.of()));
    }

    /**
     * Processes handle generic for `RestExceptionHandler`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.charger.api.error`.
     * @param ex input consumed by handleGeneric.
     * @return result produced by handleGeneric.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_ERROR", ex.getMessage(), OffsetDateTime.now(), List.of()));
    }
}

package com.salon.management.dto;

import java.util.Map;

/** Standard error envelope for every API failure (docs/DATA_API.md).
 * Never carries stack traces, hashes, or tokens. */
public class ErrorResponse {

    private final String message;
    private final Map<String, String> fieldErrors;

    public ErrorResponse(String message) {
        this(message, Map.of());
    }

    public ErrorResponse(String message, Map<String, String> fieldErrors) {
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}

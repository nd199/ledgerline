package com.ledgerline.common.error;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        ErrorCode code,
        String message,
        Map<String, String> details,
        Instant timestamp) {

    public static ApiErrorResponse of(ErrorCode code, String message) {
        return new ApiErrorResponse(code, message, null, Instant.now());
    }

    public static ApiErrorResponse of(ErrorCode code, String message, Map<String, String> details) {
        return new ApiErrorResponse(code, message, details, Instant.now());
    }
}

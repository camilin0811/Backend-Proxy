package com.ucc.aiproxy.dto;

import java.time.Instant;

/** Uniform error body returned by the API. */
public record ApiError(int status, String error, String message, Long retryAfterSeconds, Instant timestamp) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, null, Instant.now());
    }
}

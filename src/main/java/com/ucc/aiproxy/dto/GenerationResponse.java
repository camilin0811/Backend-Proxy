package com.ucc.aiproxy.dto;

import java.time.Instant;

/**
 * Response produced by the AI service.
 */
public record GenerationResponse(
        String text,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        String model,
        long latencyMs,
        Instant createdAt) {
}

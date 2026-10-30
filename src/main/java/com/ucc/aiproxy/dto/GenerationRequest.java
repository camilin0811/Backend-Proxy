package com.ucc.aiproxy.dto;

/**
 * Request that travels through the proxy chain.
 *
 * @param userId identifier of the user (taken from the X-User-Id header)
 * @param prompt text sent to the AI service
 */
public record GenerationRequest(String userId, String prompt) {
}

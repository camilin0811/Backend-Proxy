package com.ucc.aiproxy.service;

import com.ucc.aiproxy.dto.GenerationRequest;
import com.ucc.aiproxy.dto.GenerationResponse;

/**
 * SUBJECT of the Proxy pattern.
 * <p>
 * Common interface shared by the real service ({@link MockAIGenerationService}) and every proxy
 * ({@code RateLimitProxyService}, {@code QuotaProxyService}). The client (controller) only knows this
 * interface, so it cannot tell whether it is talking to the real object or to a proxy.
 */
public interface AIGenerationService {

    GenerationResponse generate(GenerationRequest request);
}

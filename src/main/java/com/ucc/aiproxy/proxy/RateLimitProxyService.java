package com.ucc.aiproxy.proxy;

import com.ucc.aiproxy.dto.GenerationRequest;
import com.ucc.aiproxy.dto.GenerationResponse;
import com.ucc.aiproxy.exception.RateLimitExceededException;
import com.ucc.aiproxy.model.UserAccount;
import com.ucc.aiproxy.repository.UserAccountRepository;
import com.ucc.aiproxy.service.AIGenerationService;
import com.ucc.aiproxy.util.MinuteWindow;

import java.time.Clock;

/**
 * PROXY #1 — Rate limiting (protection proxy).
 * <p>
 * Implements the same {@link AIGenerationService} interface as the real service. Before delegating,
 * it checks that the user has not exceeded N requests per minute (N depends on the plan).
 * If the limit is reached it throws {@link RateLimitExceededException} (HTTP 429 + Retry-After)
 * and the request never reaches the next element of the chain.
 */
public class RateLimitProxyService implements AIGenerationService {

    private final AIGenerationService next;
    private final UserAccountRepository repository;
    private final Clock clock;

    public RateLimitProxyService(AIGenerationService next, UserAccountRepository repository, Clock clock) {
        this.next = next;
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public GenerationResponse generate(GenerationRequest request) {
        UserAccount account = repository.getOrCreate(request.userId());

        if (!account.tryAcquireRequest()) {
            long retryAfter = MinuteWindow.secondsUntilReset(clock);
            throw new RateLimitExceededException(
                    "Rate limit exceeded: plan %s allows %d requests per minute. Try again in %d seconds."
                            .formatted(account.getPlan(), account.getPlan().getRequestsPerMinute(), retryAfter),
                    retryAfter);
        }

        return next.generate(request);
    }
}

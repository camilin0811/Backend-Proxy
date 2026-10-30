package com.ucc.aiproxy.proxy;

import com.ucc.aiproxy.dto.GenerationRequest;
import com.ucc.aiproxy.dto.GenerationResponse;
import com.ucc.aiproxy.exception.QuotaExceededException;
import com.ucc.aiproxy.model.UserAccount;
import com.ucc.aiproxy.repository.UserAccountRepository;
import com.ucc.aiproxy.service.AIGenerationService;
import com.ucc.aiproxy.util.TokenEstimator;

import java.time.Clock;
import java.time.LocalDate;

/**
 * PROXY #2 — Monthly token quota (protection proxy).
 * <p>
 * Before delegating, it verifies that the user still has enough tokens for (at least) the prompt.
 * If not, it throws {@link QuotaExceededException} (HTTP 402 Payment Required).
 * After the real service answers, it deducts the tokens actually consumed (prompt + completion)
 * and records them in the daily usage history.
 */
public class QuotaProxyService implements AIGenerationService {

    private final AIGenerationService next;
    private final UserAccountRepository repository;
    private final Clock clock;

    public QuotaProxyService(AIGenerationService next, UserAccountRepository repository, Clock clock) {
        this.next = next;
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public GenerationResponse generate(GenerationRequest request) {
        UserAccount account = repository.getOrCreate(request.userId());
        account.refreshQuotaWindow(LocalDate.now(clock));

        int estimatedPromptTokens = Math.max(1, TokenEstimator.estimate(request.prompt()));
        if (!account.hasTokensAvailable(estimatedPromptTokens)) {
            throw new QuotaExceededException(
                    "Monthly token quota exhausted for plan %s (%d tokens remaining). Upgrade your plan to continue."
                            .formatted(account.getPlan(), account.getRemainingTokens()));
        }

        GenerationResponse response = next.generate(request);

        account.consumeTokens(response.totalTokens(), LocalDate.now(clock));
        return response;
    }
}

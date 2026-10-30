package com.ucc.aiproxy;

import com.ucc.aiproxy.dto.GenerationRequest;
import com.ucc.aiproxy.dto.GenerationResponse;
import com.ucc.aiproxy.exception.QuotaExceededException;
import com.ucc.aiproxy.exception.RateLimitExceededException;
import com.ucc.aiproxy.model.Plan;
import com.ucc.aiproxy.model.UserAccount;
import com.ucc.aiproxy.proxy.QuotaProxyService;
import com.ucc.aiproxy.proxy.RateLimitProxyService;
import com.ucc.aiproxy.repository.UserAccountRepository;
import com.ucc.aiproxy.service.AIGenerationService;
import com.ucc.aiproxy.service.MockAIGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProxyChainTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-30T15:00:45Z"), ZoneId.of("UTC"));
    private UserAccountRepository repository;
    private AtomicInteger realCalls;
    private AIGenerationService chain;

    @BeforeEach
    void setUp() {
        repository = new UserAccountRepository(clock);
        realCalls = new AtomicInteger();
        MockAIGenerationService mock = new MockAIGenerationService(0, clock);
        AIGenerationService countingReal = request -> {
            realCalls.incrementAndGet();
            return mock.generate(request);
        };
        chain = new RateLimitProxyService(new QuotaProxyService(countingReal, repository, clock), repository, clock);
    }

    @Test
    void freePlanAllowsTenRequestsPerMinuteThenReturnsRateLimitError() {
        for (int i = 0; i < 10; i++) {
            chain.generate(new GenerationRequest("alice", "hello"));
        }
        assertThatThrownBy(() -> chain.generate(new GenerationRequest("alice", "hello")))
                .isInstanceOf(RateLimitExceededException.class)
                .extracting(e -> ((RateLimitExceededException) e).getRetryAfterSeconds())
                .isEqualTo(15L); // clock is at second 45
        assertThat(realCalls.get()).isEqualTo(10); // blocked request never reached the real service
    }

    @Test
    void rateLimitResetAllowsNewRequests() {
        for (int i = 0; i < 10; i++) {
            chain.generate(new GenerationRequest("bob", "hi"));
        }
        repository.getOrCreate("bob").resetRateLimit();
        GenerationResponse response = chain.generate(new GenerationRequest("bob", "hi"));
        assertThat(response.text()).isNotBlank();
    }

    @Test
    void quotaProxyDeductsTokensAndRecordsDailyUsage() {
        GenerationResponse response = chain.generate(new GenerationRequest("carol", "Explain the proxy pattern"));
        UserAccount account = repository.getOrCreate("carol");

        assertThat(account.getTokensUsedThisMonth()).isEqualTo(response.totalTokens());
        assertThat(account.getUsageFor(LocalDate.now(clock)).requests()).isEqualTo(1);
    }

    @Test
    void exhaustedQuotaReturnsPaymentRequired() {
        UserAccount account = repository.getOrCreate("dave");
        account.consumeTokens(Plan.FREE.getMonthlyTokens(), LocalDate.now(clock));

        assertThatThrownBy(() -> chain.generate(new GenerationRequest("dave", "hello")))
                .isInstanceOf(QuotaExceededException.class);
        assertThat(realCalls.get()).isZero();
    }

    @Test
    void enterprisePlanIsUnlimited() {
        repository.getOrCreate("erin").changePlan(Plan.ENTERPRISE);
        for (int i = 0; i < 100; i++) {
            chain.generate(new GenerationRequest("erin", "hello"));
        }
        assertThat(realCalls.get()).isEqualTo(100);
        assertThat(repository.getOrCreate("erin").getRemainingTokens()).isEqualTo(-1);
    }

    @Test
    void monthlyResetRestoresQuota() {
        UserAccount account = repository.getOrCreate("frank");
        account.consumeTokens(1000, LocalDate.now(clock));
        account.resetMonthlyQuota(LocalDate.now(clock));
        assertThat(account.getTokensUsedThisMonth()).isZero();
        assertThat(account.getQuotaResetDate()).isEqualTo(LocalDate.of(2026, 11, 1));
    }
}

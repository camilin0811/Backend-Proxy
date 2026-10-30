package com.ucc.aiproxy.config;

import com.ucc.aiproxy.proxy.QuotaProxyService;
import com.ucc.aiproxy.proxy.RateLimitProxyService;
import com.ucc.aiproxy.repository.UserAccountRepository;
import com.ucc.aiproxy.service.AIGenerationService;
import com.ucc.aiproxy.service.MockAIGenerationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Assembles the proxy chain:
 *
 * <pre>
 *  Controller ──► RateLimitProxyService ──► QuotaProxyService ──► MockAIGenerationService
 *                 (429 Too Many Requests)   (402 Payment Required)  (real subject, 1200 ms)
 * </pre>
 *
 * Only the head of the chain is exposed as an {@link AIGenerationService} bean, so the controller
 * can never bypass the proxies.
 */
@Configuration
public class ProxyChainConfig {

    @Bean
    public Clock clock(@Value("${app.timezone}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }

    @Bean
    public AIGenerationService aiGenerationService(UserAccountRepository repository,
                                                   Clock clock,
                                                   @Value("${app.ai.mock-latency-ms}") long latencyMs) {
        AIGenerationService realService = new MockAIGenerationService(latencyMs, clock);
        AIGenerationService quotaProxy = new QuotaProxyService(realService, repository, clock);
        return new RateLimitProxyService(quotaProxy, repository, clock);
    }
}

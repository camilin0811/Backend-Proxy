package com.ucc.aiproxy.scheduler;

import com.ucc.aiproxy.model.UserAccount;
import com.ucc.aiproxy.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Scheduled tasks:
 * <ul>
 *   <li>Every minute (second 0): reset the per-minute request counters (rate limit).</li>
 *   <li>First day of each month at 00:00: reset the monthly token quotas.</li>
 * </ul>
 */
@Component
public class QuotaResetScheduler {

    private static final Logger log = LoggerFactory.getLogger(QuotaResetScheduler.class);

    private final UserAccountRepository repository;
    private final Clock clock;

    public QuotaResetScheduler(UserAccountRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 * * * * *", zone = "${app.timezone}")
    public void resetRateLimits() {
        repository.findAll().forEach(UserAccount::resetRateLimit);
    }

    @Scheduled(cron = "0 0 0 1 * *", zone = "${app.timezone}")
    public void resetMonthlyQuotas() {
        LocalDate today = LocalDate.now(clock);
        repository.findAll().forEach(account -> account.resetMonthlyQuota(today));
        log.info("Monthly token quotas reset for {} users", repository.findAll().size());
    }
}

package com.ucc.aiproxy.service;

import com.ucc.aiproxy.dto.QuotaStatusResponse;
import com.ucc.aiproxy.exception.InvalidUpgradeException;
import com.ucc.aiproxy.model.DailyUsage;
import com.ucc.aiproxy.model.Plan;
import com.ucc.aiproxy.model.UserAccount;
import com.ucc.aiproxy.repository.UserAccountRepository;
import com.ucc.aiproxy.util.MinuteWindow;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Read/administrative operations over quotas: status, history and plan upgrades.
 */
@Service
public class QuotaService {

    private static final int HISTORY_DAYS = 7;

    private final UserAccountRepository repository;
    private final Clock clock;

    public QuotaService(UserAccountRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public QuotaStatusResponse getStatus(String userId) {
        UserAccount account = repository.getOrCreate(userId);
        account.refreshQuotaWindow(LocalDate.now(clock));
        Plan plan = account.getPlan();

        return new QuotaStatusResponse(
                account.getUserId(),
                plan,
                account.getTokensUsedThisMonth(),
                account.getRemainingTokens(),
                plan.getMonthlyTokens(),
                plan.hasUnlimitedTokens(),
                account.getQuotaResetDate(),
                account.getRequestsInCurrentMinute(),
                plan.getRequestsPerMinute(),
                MinuteWindow.secondsUntilReset(clock));
    }

    /** Usage of the last 7 days (oldest first, today last), filling days without usage with zeros. */
    public List<DailyUsage> getHistory(String userId) {
        UserAccount account = repository.getOrCreate(userId);
        LocalDate today = LocalDate.now(clock);
        List<DailyUsage> history = new ArrayList<>(HISTORY_DAYS);
        for (int i = HISTORY_DAYS - 1; i >= 0; i--) {
            history.add(account.getUsageFor(today.minusDays(i)));
        }
        return history;
    }

    public QuotaStatusResponse upgrade(String userId, Plan targetPlan) {
        UserAccount account = repository.getOrCreate(userId);
        Plan target = targetPlan != null ? targetPlan : Plan.PRO;

        if (!account.getPlan().canUpgradeTo(target)) {
            throw new InvalidUpgradeException(
                    "Cannot change plan from %s to %s: only upgrades are allowed."
                            .formatted(account.getPlan(), target));
        }
        account.changePlan(target);
        return getStatus(userId);
    }
}

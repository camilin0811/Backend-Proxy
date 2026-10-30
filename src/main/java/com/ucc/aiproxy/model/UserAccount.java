package com.ucc.aiproxy.model;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory state of a user: plan, rate-limit window counter, monthly token quota and daily history.
 * All mutating methods are synchronized so concurrent requests of the same user stay consistent.
 */
public class UserAccount {

    private final String userId;
    private Plan plan;

    private int requestsInCurrentMinute;

    private long tokensUsedThisMonth;
    private LocalDate quotaResetDate;

    private final Map<LocalDate, DailyUsage> dailyUsage = new ConcurrentHashMap<>();

    public UserAccount(String userId, Plan plan, LocalDate today) {
        this.userId = userId;
        this.plan = plan;
        this.quotaResetDate = firstDayOfNextMonth(today);
    }

    // ---------- Rate limiting ----------

    /**
     * Registers a request in the current minute window.
     * @return true if the request is allowed, false if the per-minute limit was reached.
     */
    public synchronized boolean tryAcquireRequest() {
        if (plan.hasUnlimitedRequests()) {
            requestsInCurrentMinute++;
            return true;
        }
        if (requestsInCurrentMinute >= plan.getRequestsPerMinute()) {
            return false;
        }
        requestsInCurrentMinute++;
        return true;
    }

    public synchronized void resetRateLimit() {
        requestsInCurrentMinute = 0;
    }

    // ---------- Monthly quota ----------

    /** Lazily resets the quota if the reset date has passed (safety net besides the scheduler). */
    public synchronized void refreshQuotaWindow(LocalDate today) {
        if (!today.isBefore(quotaResetDate)) {
            resetMonthlyQuota(today);
        }
    }

    public synchronized void resetMonthlyQuota(LocalDate today) {
        tokensUsedThisMonth = 0;
        quotaResetDate = firstDayOfNextMonth(today);
    }

    public synchronized boolean hasTokensAvailable(long requiredTokens) {
        return plan.hasUnlimitedTokens() || getRemainingTokens() >= requiredTokens;
    }

    public synchronized void consumeTokens(long tokens, LocalDate today) {
        tokensUsedThisMonth += tokens;
        dailyUsage.merge(today, DailyUsage.empty(today).add(tokens), (old, ignored) -> old.add(tokens));
    }

    /** Remaining tokens, or -1 when the plan is unlimited. */
    public synchronized long getRemainingTokens() {
        if (plan.hasUnlimitedTokens()) {
            return -1;
        }
        return Math.max(0, plan.getMonthlyTokens() - tokensUsedThisMonth);
    }

    // ---------- Plan ----------

    public synchronized void changePlan(Plan newPlan) {
        this.plan = newPlan;
    }

    // ---------- Getters ----------

    public String getUserId() {
        return userId;
    }

    public synchronized Plan getPlan() {
        return plan;
    }

    public synchronized int getRequestsInCurrentMinute() {
        return requestsInCurrentMinute;
    }

    public synchronized long getTokensUsedThisMonth() {
        return tokensUsedThisMonth;
    }

    public synchronized LocalDate getQuotaResetDate() {
        return quotaResetDate;
    }

    public DailyUsage getUsageFor(LocalDate date) {
        return dailyUsage.getOrDefault(date, DailyUsage.empty(date));
    }

    private static LocalDate firstDayOfNextMonth(LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfNextMonth());
    }
}

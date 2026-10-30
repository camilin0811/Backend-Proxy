package com.ucc.aiproxy.dto;

import com.ucc.aiproxy.model.Plan;

import java.time.LocalDate;

/**
 * Current quota and rate-limit state of a user.
 * For unlimited plans, tokenLimit / tokensRemaining / requestsPerMinuteLimit are -1.
 */
public record QuotaStatusResponse(
        String userId,
        Plan plan,
        long tokensUsed,
        long tokensRemaining,
        long tokenLimit,
        boolean unlimited,
        LocalDate resetDate,
        int requestsThisMinute,
        int requestsPerMinuteLimit,
        long rateLimitResetInSeconds) {
}

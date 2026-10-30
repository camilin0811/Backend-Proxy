package com.ucc.aiproxy.model;

import java.time.LocalDate;

/** Aggregated consumption of a user for a single day. */
public record DailyUsage(LocalDate date, long tokens, int requests) {

    public static DailyUsage empty(LocalDate date) {
        return new DailyUsage(date, 0, 0);
    }

    public DailyUsage add(long moreTokens) {
        return new DailyUsage(date, tokens + moreTokens, requests + 1);
    }
}

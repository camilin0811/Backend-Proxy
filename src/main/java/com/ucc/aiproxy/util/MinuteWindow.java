package com.ucc.aiproxy.util;

import java.time.Clock;
import java.time.LocalTime;

/**
 * The rate-limit window is aligned to the wall-clock minute, because the scheduler resets
 * every counter at second 0 of each minute.
 */
public final class MinuteWindow {

    private MinuteWindow() {
    }

    /** Seconds left until the next reset (1..60). */
    public static long secondsUntilReset(Clock clock) {
        return 60L - LocalTime.now(clock).getSecond();
    }
}

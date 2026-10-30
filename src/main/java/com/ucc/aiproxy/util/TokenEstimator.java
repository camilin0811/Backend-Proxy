package com.ucc.aiproxy.util;

/**
 * Approximate token counter. Common heuristic: ~4 characters per token.
 * The frontend uses the same formula for its pre-send estimator.
 */
public final class TokenEstimator {

    private static final double CHARS_PER_TOKEN = 4.0;

    private TokenEstimator() {
    }

    public static int estimate(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (int) Math.ceil(text.trim().length() / CHARS_PER_TOKEN);
    }
}

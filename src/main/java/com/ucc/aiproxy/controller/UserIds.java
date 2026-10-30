package com.ucc.aiproxy.controller;

import java.util.regex.Pattern;

/** Validates and normalizes the X-User-Id header value. */
final class UserIds {

    private static final Pattern VALID = Pattern.compile("^[a-zA-Z0-9_.-]{1,64}$");

    private UserIds() {
    }

    static String normalize(String rawUserId) {
        String userId = rawUserId == null ? "" : rawUserId.trim().toLowerCase();
        if (!VALID.matcher(userId).matches()) {
            throw new IllegalArgumentException(
                    "Invalid X-User-Id header: use 1-64 characters (letters, digits, '.', '_' or '-').");
        }
        return userId;
    }
}

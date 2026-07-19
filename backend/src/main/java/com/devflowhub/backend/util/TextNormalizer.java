package com.devflowhub.backend.util;

import java.util.Locale;

public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public static String trimToNull(String value) {
        String normalized = trim(value);
        return normalized.isEmpty() ? null : normalized;
    }

    public static String lower(String value) {
        return trim(value).toLowerCase(Locale.ROOT);
    }

    public static String upperOrDefault(String value, String fallback) {
        String normalized = trim(value);
        return normalized.isEmpty() ? fallback : normalized.toUpperCase(Locale.ROOT);
    }
}

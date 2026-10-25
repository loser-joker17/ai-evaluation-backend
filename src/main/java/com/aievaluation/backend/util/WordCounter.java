package com.aievaluation.backend.util;

import java.util.regex.Pattern;

public final class WordCounter {

    // Unicode-aware like Python's str.split(): also splits on non-breaking spaces etc.
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private WordCounter() {
    }

    public static int count(String text) {
        if (text == null) {
            return 0;
        }
        String trimmed = text.strip();
        if (trimmed.isEmpty()) {
            return 0;
        }
        return WHITESPACE.split(trimmed).length;
    }
}
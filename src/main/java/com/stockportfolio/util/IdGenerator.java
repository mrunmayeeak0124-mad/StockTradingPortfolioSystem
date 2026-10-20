package com.stockportfolio.util;

import java.util.UUID;

/**
 * Creates short unique IDs such as ORD-3F9A12BC.
 */
public final class IdGenerator {

    private IdGenerator() { }

    public static String generate(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
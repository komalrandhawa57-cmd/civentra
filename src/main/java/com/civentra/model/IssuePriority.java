package com.civentra.model;

/**
 * Community issue priority levels.
 */
public enum IssuePriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static IssuePriority fromString(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        try {
            return IssuePriority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MEDIUM;
        }
    }

    public String displayName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
package com.civentra.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class SecurityUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private SecurityUtil() {}

    public static String hashPassword(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        return ENCODER.encode(raw);
    }

    public static boolean checkPassword(String raw, String storedHash) {
        if (raw == null || storedHash == null) return false;
        
        // Demo fallback check

        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
            try {
                return ENCODER.matches(raw, storedHash);
            } catch (Exception e) {
                return false;
            }
        }
        return raw.equals(storedHash);
    }

    public static boolean verifyPassword(String raw, String storedHash) {
        return checkPassword(raw, storedHash);
    }

    public static boolean isEncodedPassword(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}

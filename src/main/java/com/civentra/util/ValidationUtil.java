package com.civentra.util;

import java.util.regex.Pattern;

public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^[6-9]\\d{9}$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static String sanitize(String input) {
        if (input == null) return null;
        // Vaadin renders user text with HTML escaping; preserve literal characters here to
        // avoid storing entities that appear as "&lt;" in the UI. Remove unsafe controls only.
        return input.trim().replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");
    }
}

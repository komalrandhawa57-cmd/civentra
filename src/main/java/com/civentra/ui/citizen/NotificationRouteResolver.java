package com.civentra.ui.citizen;

import java.util.Locale;

/** Keeps notification destinations inside the recipient's authorized workspace. */
final class NotificationRouteResolver {
    private NotificationRouteResolver() {}

    static String issueRoute(String roleName) {
        return switch (normalize(roleName)) {
            case "ADMIN" -> "admin/assignments";
            case "OFFICER" -> "officer";
            case "PROVIDER" -> "provider";
            case "CITIZEN" -> "citizen/my-issues";
            default -> "login";
        };
    }

    static String serviceRequestRoute(String roleName) {
        return switch (normalize(roleName)) {
            case "ADMIN" -> "admin/service-requests";
            case "PROVIDER" -> "provider";
            case "CITIZEN" -> "citizen/service-requests";
            default -> "login";
        };
    }

    private static String normalize(String roleName) {
        return roleName == null ? "" : roleName.trim().toUpperCase(Locale.ROOT);
    }
}

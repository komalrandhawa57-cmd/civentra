package com.civentra.util;

import com.vaadin.flow.server.VaadinSession;
import com.civentra.model.User;

/**
 * Centralized session helper for Vaadin application context.
 */
public final class SessionUtil {

    private static final String SESSION_USER_ID = "CIVENTRA_USER_ID";
    private static final String SESSION_USER_EMAIL = "CIVENTRA_USER_EMAIL";
    private static final String SESSION_USER_ROLE = "CIVENTRA_USER_ROLE";

    private SessionUtil() {
    }

    public static void setCurrentUser(long userId, String email, String role) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(SESSION_USER_ID, userId);
            session.setAttribute(SESSION_USER_EMAIL, email);
            session.setAttribute(SESSION_USER_ROLE, role);
            User user = (User) session.getAttribute("user");
            if (user != null && user.getUserId() == userId) {
                user.setEmail(email);
                user.setRoleName(role);
            }
        }
    }

    public static long getCurrentUserId() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            Object current = session.getAttribute("user");
            if (current instanceof User user) return user.getUserId();
        }
        return 0L;
    }

    public static String getCurrentUserEmail() {
        VaadinSession session = VaadinSession.getCurrent();
        Object current = session == null ? null : session.getAttribute("user");
        if (current instanceof User user) return user.getEmail();
        return null;
    }

    public static String getCurrentUserRole() {
        VaadinSession session = VaadinSession.getCurrent();
        Object current = session == null ? null : session.getAttribute("user");
        if (current instanceof User user) return user.getRoleName();
        return null;
    }

    public static boolean isLoggedIn() {
        VaadinSession session = VaadinSession.getCurrent();
        return session != null && session.getAttribute("user") instanceof User;
    }

    public static void clearSession() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(SESSION_USER_ID, null);
            session.setAttribute(SESSION_USER_EMAIL, null);
            session.setAttribute(SESSION_USER_ROLE, null);
            session.setAttribute("user", null);
            session.setAttribute("CIVENTRA_CURRENT_USER", null);
            try {
                if (session.getSession() != null) {
                    session.getSession().removeAttribute("civentra.user");
                    session.getSession().removeAttribute("CIVENTRA_USER_ID");
                    session.getSession().removeAttribute("CIVENTRA_USER_ROLE");
                    session.getSession().invalidate();
                }
            } catch (Exception ignored) {}
            try {
                session.close();
            } catch (Exception ignored) {}
        }
    }
}

package com.civentra.security;

import com.civentra.dao.UserDAO;
import com.civentra.model.User;
import com.vaadin.flow.server.VaadinSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Optional;

/**
 * Session-scoped auth holder used by all layouts and dashboards.
 * Provides both User access and convenience getters (getFullName, getUserId as Long, etc.)
 * so UI code compiles cleanly.
 */
@Component
public class AuthenticatedUser implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticatedUser.class);
    private static final String SESSION_KEY = "CIVENTRA_CURRENT_USER";

    private UserDAO userDAO;

    public AuthenticatedUser() {
    }

    /** Used by unit tests */
    public AuthenticatedUser(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Autowired(required = false)
    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // -------------------------------------------------------------------------
    // Session write / clear
    // -------------------------------------------------------------------------

    public void login(User user) {
        if (user == null) {
            return;
        }
        normalizeRole(user);
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(SESSION_KEY, user);
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("role", user.getRoleName());
        }
    }

    /** Alias used by some call sites / tests */
    public void setUser(User user) {
        login(user);
    }

    /** Alias used by tests */
    public void refreshUser(User user) {
        login(user);
    }

    public void logout() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(SESSION_KEY, null);
            session.setAttribute("userId", null);
            session.setAttribute("role", null);
            try {
                session.close();
            } catch (Exception ignored) {
            }
        }
    }

    // -------------------------------------------------------------------------
    // Core read
    // -------------------------------------------------------------------------

    public Optional<User> get() {
        return Optional.ofNullable(getUser());
    }

    public User getUser() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return null;
        }

        Object attr = session.getAttribute(SESSION_KEY);
        if (!(attr instanceof User)) {
            return null;
        }

        User sessionUser = (User) attr;

        // Best-effort DB refresh — NEVER clear session on failure (prevents login loop)
        try {
            if (userDAO != null && sessionUser.getUserId() > 0) {
                User dbUser = userDAO.findById(sessionUser.getUserId());
                if (dbUser != null) {
                    normalizeRole(dbUser);
                    session.setAttribute(SESSION_KEY, dbUser);
                    return dbUser;
                }
            }
        } catch (Exception e) {
            logger.warn("DB refresh skipped, keeping session user: {}", e.getMessage());
        }

        normalizeRole(sessionUser);
        return sessionUser;
    }

    public boolean isLoggedIn() {
        return getUser() != null;
    }

    // -------------------------------------------------------------------------
    // Convenience getters expected by AdminLayout, CitizenDashboard, etc.
    // -------------------------------------------------------------------------

    /**
     * Nullable Long so UI can write:
     *   Long id = authenticatedUser.getUserId();
     *   if (authenticatedUser.getUserId() == null) { ... }
     */
    public Long getUserId() {
        User u = getUser();
        if (u == null || u.getUserId() <= 0) {
            return null;
        }
        return (long) u.getUserId();
    }

    /** Primitive helper when a raw int is needed */
    public int getUserIdInt() {
        Long id = getUserId();
        return id != null ? id.intValue() : 0;
    }

    public String getFullName() {
        User u = getUser();
        if (u == null) {
            return "";
        }
        String name = u.getFullName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (u.getEmail() != null) {
            return u.getEmail();
        }
        return "User";
    }

    public String getName() {
        return getFullName();
    }

    public String getEmail() {
        User u = getUser();
        return u != null && u.getEmail() != null ? u.getEmail() : "";
    }

    public String getRoleName() {
        User u = getUser();
        return u != null && u.getRoleName() != null ? u.getRoleName() : "";
    }

    public String getRole() {
        return getRoleName();
    }

    public int getRoleId() {
        User u = getUser();
        return u != null ? u.getRoleId() : 0;
    }

    public boolean isActive() {
        User u = getUser();
        return u != null && u.isActive();
    }

    public boolean hasRole(String role) {
        if (role == null) {
            return false;
        }
        return role.equalsIgnoreCase(getRoleName());
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isOfficer() {
        String r = getRoleName().toUpperCase();
        return r.equals("OFFICER") || r.equals("MUNICIPAL_OFFICER");
    }

    public boolean isProvider() {
        String r = getRoleName().toUpperCase();
        return r.equals("PROVIDER") || r.equals("SERVICE_PROVIDER");
    }

    public boolean isCitizen() {
        return hasRole("CITIZEN") || getRoleName().isEmpty();
    }

    // -------------------------------------------------------------------------
    // Role normalization (DB may store MUNICIPAL_OFFICER / SERVICE_PROVIDER)
    // -------------------------------------------------------------------------

    private void normalizeRole(User user) {
        if (user == null || user.getRoleName() == null) {
            return;
        }
        String r = user.getRoleName().trim().toUpperCase(java.util.Locale.ROOT);
        switch (r) {
            case "MUNICIPAL_OFFICER":
            case "MUNICIPAL OFFICER":
            case "OFFICER":
                user.setRoleName("OFFICER");
                break;
            case "SERVICE_PROVIDER":
            case "SERVICE PROVIDER":
            case "PROVIDER":
                user.setRoleName("PROVIDER");
                break;
            case "ADMIN":
            case "ADMINISTRATOR":
                user.setRoleName("ADMIN");
                break;
            default:
                user.setRoleName("CITIZEN");
                break;
        }
    }
}
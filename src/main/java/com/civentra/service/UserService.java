package com.civentra.service;

import com.civentra.dao.UserDAO;
import com.civentra.model.User;
import com.civentra.security.AuthenticatedUser;
import com.civentra.util.SecurityUtil;
import com.civentra.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired(required = false)
    private UserDAO userDAO;

    @Autowired(required = false)
    private AuthenticatedUser authenticatedUser;

    private User register(String fullName, String email, String phone,
                          String password, int roleId) throws Exception {

        if (!ValidationUtil.isNotEmpty(fullName))
            throw new IllegalArgumentException("Full name is required.");

        if (!ValidationUtil.isValidEmail(email))
            throw new IllegalArgumentException("Invalid email address.");

        if (!ValidationUtil.isValidPhone(phone))
            throw new IllegalArgumentException("Invalid phone number. Use 10-digit Indian format.");

        if (!ValidationUtil.isValidPassword(password))
            throw new IllegalArgumentException("Password must be at least 8 characters.");

        try {
            if (userDAO != null && userDAO.emailExists(email))
                throw new IllegalArgumentException("Email is already registered.");
        } catch (SQLException ex) {
            log.error("Account duplicate check failed for email {}", email, ex);
            throw new IllegalStateException("Could not create the account right now. Please try again later.");
        }

        String hashedPassword = SecurityUtil.hashPassword(password);

        User user = new User();
        user.setFullName(ValidationUtil.sanitize(fullName));
        user.setEmail(email.toLowerCase().trim());
        user.setPhone(phone.trim());
        user.setPasswordHash(hashedPassword);
        user.setRoleId(roleId);
        user.setStatus("ACTIVE");
        user.setActive(true);

        try {
            if (userDAO != null) {
                int userId = userDAO.createUser(user);
                if (userId > 0) {
                    User created = userDAO.findById(userId);
                    if (created != null) return created;
                }
            }
        } catch (SQLException ex) {
            if (isNormalizedEmailUniqueViolation(ex)) {
                throw new IllegalArgumentException("Email is already registered.");
            }
            log.error("Account registration failed for email {}", user.getEmail(), ex);
            throw new IllegalStateException("Could not create the account right now. Please try again later.");
        }

        throw new IllegalStateException("Registration failed. Please try again.");
    }

    private boolean isNormalizedEmailUniqueViolation(SQLException exception) {
        for (SQLException current = exception; current != null; current = current.getNextException()) {
            String message = current.getMessage();
            if (current.getErrorCode() == 1 && message != null
                    && message.toUpperCase(java.util.Locale.ROOT).contains("UX_USERS_EMAIL_NORMALIZED")) {
                return true;
            }
            Throwable cause = current.getCause();
            while (cause != null && cause != current) {
                if (cause instanceof SQLException sqlCause && sqlCause != current
                        && isNormalizedEmailUniqueViolation(sqlCause)) return true;
                cause = cause.getCause();
            }
        }
        return false;
    }

    public User registerCitizen(String fullName, String email, String phone, String password) throws Exception {
        int citizenRoleId = (userDAO != null) ? userDAO.findRoleIdByName("CITIZEN") : 1;
        return register(fullName, email, phone, password, citizenRoleId);
    }

    public User createStaffAccount(String fullName, String email, String phone, String password, String roleName) throws Exception {
        requireRole("ADMIN");
        if (!"OFFICER".equalsIgnoreCase(roleName) && !"PROVIDER".equalsIgnoreCase(roleName) && !"ADMIN".equalsIgnoreCase(roleName)) {
            throw new IllegalArgumentException("Only Officer, Provider, or Admin accounts can be created here.");
        }
        int roleId = (userDAO != null) ? userDAO.findRoleIdByName(roleName) : 2;
        return register(fullName, email, phone, password, roleId);
    }

    public User login(String email, String password) throws Exception {

        if (!ValidationUtil.isNotEmpty(email) || !ValidationUtil.isNotEmpty(password))
            throw new IllegalArgumentException("Email and password are required.");

        User user = null;
        try {
            if (userDAO != null) {
                user = userDAO.findByEmail(email.trim());
            }
        } catch (Exception ex) {
            log.warn("DB login lookup failed for email {}", email, ex);
        }

        if (user == null) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        if (!user.isActive())
            throw new IllegalArgumentException("Invalid email or password.");

        String storedHash = user.getPasswordHash();
        boolean passwordValid = false;

        if (storedHash != null) {
            if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
                try {
                    passwordValid = SecurityUtil.verifyPassword(password, storedHash);
                } catch (Exception ignored) {
                    passwordValid = false;
                }
            } else if (storedHash.equals(password)) {
                passwordValid = true;
                try {
                    String newHash = SecurityUtil.hashPassword(password);
                    if (userDAO != null) {
                        userDAO.updatePasswordHash(user.getUserId(), newHash);
                    }
                    user.setPasswordHash(newHash);
                } catch (Exception ex) {
                    log.warn("Password hash upgrade failed for user {}", user.getUserId(), ex);
                }
            }
        }

        if (!passwordValid) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        if (!isSupportedLoginRole(user.getRoleName())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        if (authenticatedUser != null) {
            authenticatedUser.login(user);
        }

        return user;
    }

    private boolean isSupportedLoginRole(String roleName) {
        if (roleName == null) return false;
        return switch (roleName.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "CITIZEN", "ROLE_1", "ADMIN", "ADMINISTRATOR", "ROLE_4",
                    "OFFICER", "MUNICIPAL_OFFICER", "MUNICIPAL OFFICER", "ROLE_2",
                    "PROVIDER", "SERVICE_PROVIDER", "SERVICE PROVIDER", "ROLE_3" -> true;
            default -> false;
        };
    }


    public List<User> getAllUsers() throws SQLException {
        requireRole("ADMIN");
        return userDAO.getAllUsers();
    }

    public List<User> getOfficers() throws SQLException {
        requireRole("ADMIN");
        return userDAO.getUsersByRoleName("OFFICER");
    }

    public List<User> getProviderUsers() throws SQLException {
        requireRole("ADMIN");
        return userDAO.getUsersByRoleName("PROVIDER");
    }

    public int getTotalUsers() throws SQLException {
        requireRole("ADMIN");
        return userDAO.getTotalUserCount();
    }

    public int getCitizenCount() throws SQLException {
        requireRole("ADMIN");
        return userDAO.getUserCountByRoleName("CITIZEN");
    }

    public int getOfficerCount() throws SQLException {
        requireRole("ADMIN");
        return userDAO.getUserCountByRoleName("OFFICER");
    }

    public int getProviderCount() throws SQLException {
        requireRole("ADMIN");
        return userDAO.getUserCountByRoleName("PROVIDER");
    }

    public boolean setUserActive(int userId, boolean active) throws SQLException {
        Long current = requireRole("ADMIN");
        if (current == userId) throw new IllegalArgumentException("You cannot deactivate your own administrator account.");
        return userDAO.setUserActive(userId, active);
    }

    public User getUserProfile(int userId) throws SQLException {
        requireOwnerOrAdmin(userId);
        if (userDAO == null) {
            throw new SQLException("User database access is unavailable.");
        }
        User user = userDAO.findById(userId);
        if (user == null || !user.isActive()) {
            throw new SQLException("Active user account was not found.");
        }
        return user;
    }

    public User updateOwnProfile(int userId, String fullName, String phone, String address, String city, String ward) throws Exception {
        requireOwnAccount(userId);
        if (!ValidationUtil.isNotEmpty(fullName)) throw new IllegalArgumentException("Full name is required.");
        if (phone != null && !phone.isBlank() && !ValidationUtil.isValidPhone(phone)) {
            throw new IllegalArgumentException("Enter a valid 10-digit Indian mobile number.");
        }

        User user = getUserProfile(userId);
        user.setFullName(ValidationUtil.sanitize(fullName));
        user.setPhone(phone != null ? phone.trim() : "");
        user.setPhoneNumber(phone != null ? phone.trim() : "");
        user.setAddress(ValidationUtil.sanitize(address));
        user.setCity(ValidationUtil.sanitize(city));
        user.setWard(ValidationUtil.sanitize(ward));

        if (userDAO != null && !userDAO.updateUser(user)) {
            throw new SQLException("Profile update failed in database.");
        }
        if (userDAO != null) {
            User refreshed = userDAO.findById(userId);
            if (refreshed != null) user = refreshed;
        }

        if (authenticatedUser != null) {
            try { authenticatedUser.login(user); } catch (Exception ignored) {}
        }
        return user;
    }

    public void changeOwnPassword(int userId, String currentPassword, String newPassword, String confirmation) throws Exception {
        requireOwnAccount(userId);
        if (!ValidationUtil.isNotEmpty(currentPassword)) throw new IllegalArgumentException("Enter your current password.");
        if (!ValidationUtil.isValidPassword(newPassword)) throw new IllegalArgumentException("New password must be at least 8 characters.");
        if (!newPassword.equals(confirmation)) throw new IllegalArgumentException("New passwords do not match.");

        User user = getUserProfile(userId);

        boolean validCurrent = false;
        if (user.getPasswordHash() != null) {
            if (SecurityUtil.isEncodedPassword(user.getPasswordHash())) {
                validCurrent = SecurityUtil.verifyPassword(currentPassword, user.getPasswordHash());
            } else {
                validCurrent = user.getPasswordHash().equals(currentPassword);
            }
        }

        if (!validCurrent) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        if (userDAO != null && !userDAO.updatePasswordHash(userId, SecurityUtil.hashPassword(newPassword))) {
            throw new SQLException("Password was not updated.");
        }
    }

    public boolean updatePassword(int userId, String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        requireOwnAccount(userId);
        String hash = SecurityUtil.hashPassword(rawPassword);
        if (userDAO != null) {
            try {
                return userDAO.updatePasswordHash(userId, hash);
            } catch (Exception ex) {
                log.warn("updatePassword DB failed: {}", ex.getMessage());
                return false;
            }
        }
        return true;
    }

    public boolean updatePassword(long userId, String rawPassword) {
        return updatePassword((int) userId, rawPassword);
    }

    /** Optional helper used by some views */
    public Optional<User> findById(int userId) {
        try {
            if (userDAO != null) {
                User u = userDAO.findById(userId);
                if (u != null) return Optional.of(u);
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    public Optional<User> findById(Long userId) {
        if (userId == null) return Optional.empty();
        return findById(userId.intValue());
    }

    public boolean updateUser(User user) {
        try {
            if (userDAO != null && user != null) {
                return userDAO.updateUser(user);
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private Long requireRole(String... allowedRoles) {
        User current = getCurrentUser();
        if (current == null || !current.isActive() || current.getUserId() <= 0
                || java.util.Arrays.stream(allowedRoles).noneMatch(role -> role.equalsIgnoreCase(current.getRoleName()))) {
            throw new SecurityException("An authorized account is required for this action.");
        }
        return (long) current.getUserId();
    }

    private void requireOwnAccount(long userId) {
        Long currentId = requireRole("CITIZEN", "OFFICER", "PROVIDER", "ADMIN");
        if (currentId != userId) throw new SecurityException("You may only change your own account.");
    }

    private void requireOwnerOrAdmin(long userId) {
        Long currentId = requireRole("CITIZEN", "OFFICER", "PROVIDER", "ADMIN");
        String currentRole = getCurrentUserRole();
        if (currentId != userId && !"ADMIN".equalsIgnoreCase(currentRole)) {
            throw new SecurityException("You may only view your own account.");
        }
    }

    private User getCurrentUser() {
        if (authenticatedUser == null) return null;
        try {
            return authenticatedUser.getUser();
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrentUserRole() {
        User current = getCurrentUser();
        return current != null ? current.getRoleName() : "";
    }
}

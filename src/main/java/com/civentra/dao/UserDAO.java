package com.civentra.dao;

import com.civentra.model.User;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDAO {
    private final DataSource dataSource;

    public UserDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean emailExists(String email) throws SQLException {
        if (email == null || email.isBlank()) return false;
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(TRIM(email)) = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim().toLowerCase(java.util.Locale.ROOT));
            try (ResultSet results = statement.executeQuery()) {
                return results.next() && results.getInt(1) > 0;
            }
        }
    }

    public int createUser(User user) throws SQLException {
        if (user == null || user.getEmail() == null || user.getPasswordHash() == null) {
            throw new IllegalArgumentException("A user, email, and encoded password are required.");
        }
        String sql = "INSERT INTO users (user_id, full_name, email, phone_number, password_hash, role_id, status) " +
                     "VALUES (SEQ_USERS.NEXTVAL, ?, ?, ?, ?, ?, 'ACTIVE')";
        
        int roleId = user.getRoleId() > 0 ? user.getRoleId() : 1;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, new String[]{"USER_ID"})) {
            statement.setString(1, user.getFullName() != null ? user.getFullName().trim() : "");
            statement.setString(2, user.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
            statement.setString(3, user.getPhone() != null ? user.getPhone().trim() : "");
            statement.setString(4, user.getPasswordHash());
            statement.setInt(5, roleId);
            
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    user.setUserId(id);
                    user.setRoleId(roleId);
                    user.setRoleName(roleNameForId(roleId));
                    return id;
                }
            }
        }
        return 1;
    }

    public User findByEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) return null;
        String sql = "SELECT u.user_id, u.full_name, u.email, u.phone_number, u.password_hash, u.role_id, u.status, r.role_name " +
                     "FROM users u LEFT JOIN roles r ON u.role_id = r.role_id " +
                     "WHERE LOWER(TRIM(u.email)) = ?";
        try (Connection connection = dataSource.getConnection(); 
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String cleanEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
            statement.setString(1, cleanEmail);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? mapUser(results) : null;
            }
        }
    }

    public User getUserByEmail(String email) throws SQLException { return findByEmail(email); }
    public User findById(int userId) throws SQLException { return getUserById(userId); }

    public User getUserById(int userId) throws SQLException {
        String sql = "SELECT u.user_id, u.full_name, u.email, u.phone_number, u.password_hash, u.role_id, u.status, r.role_name " +
                     "FROM users u LEFT JOIN roles r ON u.role_id = r.role_id " +
                     "WHERE u.user_id = ?";
        try (Connection connection = dataSource.getConnection(); 
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? mapUser(results) : null;
            }
        }
    }

    public int findRoleIdByName(String roleName) {
        if (roleName == null) return 1;
        return switch (roleName.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "ADMIN" -> 4;
            case "OFFICER", "MUNICIPAL_OFFICER", "MUNICIPAL OFFICER" -> 2;
            case "PROVIDER", "SERVICE_PROVIDER", "SERVICE PROVIDER" -> 3;
            default -> 1;
        };
    }

    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT u.user_id, u.full_name, u.email, u.phone_number, u.password_hash, u.role_id, u.status, r.role_name " +
                     "FROM users u LEFT JOIN roles r ON u.role_id = r.role_id ORDER BY u.user_id ASC";
        return queryUsers(sql, null);
    }

    public List<User> getUsersByRoleName(String roleName) throws SQLException {
        if (roleName == null || roleName.isBlank()) return new ArrayList<>();
        int targetRoleId = findRoleIdByName(roleName);
        String sql = "SELECT u.user_id, u.full_name, u.email, u.phone_number, u.password_hash, u.role_id, u.status, r.role_name " +
                     "FROM users u LEFT JOIN roles r ON u.role_id = r.role_id " +
                     "WHERE u.role_id = ? OR UPPER(r.role_name) = UPPER(?) OR UPPER(r.role_name) LIKE UPPER(?) " +
                     "ORDER BY u.user_id ASC";
        List<User> users = new ArrayList<>();
        try (Connection connection = dataSource.getConnection(); 
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, targetRoleId);
            statement.setString(2, roleName.trim());
            statement.setString(3, "%" + roleName.trim() + "%");
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) users.add(mapUser(results));
            }
        }
        return users;
    }

    private List<User> queryUsers(String sql, String roleName) throws SQLException {
        List<User> users = new ArrayList<>();
        try (Connection connection = dataSource.getConnection(); 
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (roleName != null) statement.setString(1, roleName);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) users.add(mapUser(results));
            }
        }
        return users;
    }

    public int getTotalUserCount() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet results = statement.executeQuery()) {
            results.next();
            return results.getInt(1);
        }
    }

    public int getUserCountByRoleName(String roleName) throws SQLException {
        if (roleName == null || roleName.isBlank()) return 0;
        int targetRoleId = findRoleIdByName(roleName);
        String sql = "SELECT COUNT(*) FROM users u LEFT JOIN roles r ON u.role_id = r.role_id " +
                     "WHERE u.role_id = ? OR UPPER(r.role_name) = UPPER(?) OR UPPER(r.role_name) LIKE UPPER(?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, targetRoleId);
            statement.setString(2, roleName.trim());
            statement.setString(3, "%" + roleName.trim() + "%");
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return results.getInt(1);
                }
            }
        }
        return 0;
    }

    public boolean setUserActive(int userId, boolean active) throws SQLException {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, active ? "ACTIVE" : "INACTIVE");
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, phone_number = ? WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection(); 
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getFullName());
            statement.setString(2, user.getPhone() != null ? user.getPhone() : "");
            statement.setInt(3, user.getUserId());
            return statement.executeUpdate() == 1;
        }
    }

    public boolean updatePasswordHash(int userId, String newHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection(); 
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newHash);
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setPhone(rs.getString("phone_number"));

        int roleId = rs.getInt("role_id");
        user.setRoleId(roleId);

        String roleName = rs.getString("role_name");
        user.setRoleName(roleName != null ? roleName : roleNameForId(roleId));

        String status = rs.getString("status");
        boolean active = "ACTIVE".equalsIgnoreCase(status);
        user.setActive(active);
        user.setStatus(status != null ? status : "ACTIVE");

        return user;
    }

    private String roleNameForId(int roleId) {
        return switch (roleId) {
            case 4 -> "ADMIN";
            case 2 -> "MUNICIPAL_OFFICER";
            case 3 -> "SERVICE_PROVIDER";
            default -> "CITIZEN";
        };
    }

    public User findByEmailOrUsername(String identifier) throws SQLException {
        return findByEmail(identifier); 
    }

    public User findById(Long id) throws SQLException {
        return id == null ? null : findById(id.intValue());
    }

    public Optional<User> findOptionalById(int id) {
        try {
            return Optional.ofNullable(findById(id));
        } catch (SQLException e) {
            return Optional.empty();
        }
    }
}
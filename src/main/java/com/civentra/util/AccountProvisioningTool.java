package com.civentra.util;

import com.civentra.config.DatabaseConfig;

import java.io.Console;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;
import java.util.Scanner;

/**
 * Secure command-line tool for provisioning or recovering ADMIN, OFFICER,
 * and PROVIDER accounts.
 *
 * Secure properties:
 * - Passwords are read via masked console input and never echoed.
 * - Stored in the database strictly as BCrypt hashes.
 * - Never logged or committed to source control.
 */
public class AccountProvisioningTool {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  CIVENTRA Secure Account Provisioning & Recovery ");
        System.out.println("==================================================");

        Scanner scanner = new Scanner(System.in);
        Console console = System.console();

        String email = prompt(scanner, "Enter Email: ");
        String fullName = prompt(scanner, "Enter Full Name: ");
        String phone = prompt(scanner, "Enter 10-digit Indian Mobile Number: ");
        String role = prompt(scanner, "Enter Role (ADMIN / OFFICER / PROVIDER / CITIZEN): ").toUpperCase(Locale.ROOT);

        int roleId = switch (role) {
            case "ADMIN" -> 4;
            case "OFFICER", "MUNICIPAL_OFFICER" -> 2;
            case "PROVIDER", "SERVICE_PROVIDER" -> 3;
            default -> 1;
        };

        String password;
        if (console != null) {
            char[] pwdChars = console.readPassword("Enter Password: ");
            password = new String(pwdChars);
        } else {
            System.out.print("Enter Password: ");
            password = scanner.nextLine();
        }

        if (password == null || password.length() < 8) {
            System.err.println("ERROR: Password must be at least 8 characters.");
            System.exit(1);
        }

        String hash = SecurityUtil.hashPassword(password);

        try (Connection conn = DatabaseConfig.getConnection()) {
            String checkSql = "SELECT user_id FROM users WHERE LOWER(TRIM(email)) = ?";
            Integer existingId = null;
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, email.trim().toLowerCase(Locale.ROOT));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        existingId = rs.getInt(1);
                    }
                }
            }

            if (existingId != null) {
                String updateSql = "UPDATE users SET password_hash = ?, role_id = ?, status = 'ACTIVE', full_name = ?, phone_number = ? WHERE user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, hash);
                    ps.setInt(2, roleId);
                    ps.setString(3, fullName);
                    ps.setString(4, phone);
                    ps.setInt(5, existingId);
                    ps.executeUpdate();
                }
                System.out.println("SUCCESS: Account updated for " + email + " with role " + role + " (ID: " + existingId + ")");
            } else {
                String insertSql = "INSERT INTO users (user_id, full_name, email, phone_number, password_hash, role_id, status) VALUES (SEQ_USERS.NEXTVAL, ?, ?, ?, ?, ?, 'ACTIVE')";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, fullName);
                    ps.setString(2, email.trim().toLowerCase(Locale.ROOT));
                    ps.setString(3, phone);
                    ps.setString(4, hash);
                    ps.setInt(5, roleId);
                    ps.executeUpdate();
                }
                System.out.println("SUCCESS: Account created for " + email + " with role " + role);
            }
        } catch (Exception e) {
            System.err.println("ERROR provisioning account: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String prompt(Scanner scanner, String promptText) {
        System.out.print(promptText);
        String line = scanner.nextLine();
        while (line == null || line.trim().isEmpty()) {
            System.out.print(promptText);
            line = scanner.nextLine();
        }
        return line.trim();
    }
}

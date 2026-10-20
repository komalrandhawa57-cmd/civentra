package com.civentra.util;

import java.sql.Connection;
import java.sql.SQLException;
import com.civentra.config.DatabaseConfig;

/**
 * JDBC Connection Manager for Oracle XE database.
 */
public final class DatabaseConnection {

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DatabaseConfig.getConnection();
    }
}

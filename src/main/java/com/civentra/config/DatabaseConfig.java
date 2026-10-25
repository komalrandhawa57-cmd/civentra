package com.civentra.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Configuration
public class DatabaseConfig {

    @Value("${civentra.db.url:jdbc:oracle:thin:@//localhost:1521/XEPDB1}")
    private String dbUrl;

    @Value("${civentra.db.username:CIVENTRA_USER}")
    private String dbUsername;

    @Value("${civentra.db.password:}")
    private String dbPassword;

    @Value("${civentra.db.pool.size:10}")
    private int poolSize;

    @Value("${civentra.db.h2-fallback-enabled:true}")
    private boolean h2FallbackEnabled;

    private static volatile DataSource activeDataSource;

    @Bean(destroyMethod = "close")
    @Primary
    public DataSource dataSource() {
        HikariDataSource ds = null;
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUsername);
            config.setPassword(dbPassword);
            config.setMaximumPoolSize(poolSize);
            config.setPoolName("CiventraPool");

            if (dbUrl != null && dbUrl.contains("oracle")) {
                config.setDriverClassName("oracle.jdbc.OracleDriver");
            } else {
                config.setDriverClassName("org.h2.Driver");
            }

            // Short 2-second timeout so cloud boot fails fast and switches to H2
            config.setInitializationFailTimeout(2000L);
            ds = new HikariDataSource(config);
        } catch (Exception e) {
            if (!h2FallbackEnabled) {
                throw new IllegalStateException("CIVENTRA could not connect to Oracle and H2 fallback is disabled for this run.", e);
            }
            System.err.println("CIVENTRA: Primary database connection failed. Falling back to H2 in-memory mode: " + e.getMessage());
            HikariConfig fallbackConfig = new HikariConfig();
            fallbackConfig.setJdbcUrl("jdbc:h2:mem:civentradb;DB_CLOSE_DELAY=-1;MODE=Oracle");
            fallbackConfig.setUsername("sa");
            fallbackConfig.setPassword("");
            fallbackConfig.setDriverClassName("org.h2.Driver");
            fallbackConfig.setMaximumPoolSize(poolSize);
            fallbackConfig.setPoolName("CiventraH2FallbackPool");
            ds = new HikariDataSource(fallbackConfig);
        }

        activeDataSource = ds;
        return ds;
    }

    /** Legacy DAOs use this bridge; it returns a connection from the initialized pool or fallback H2 connection. */
    public static Connection getConnection() throws SQLException {
        if (activeDataSource != null) {
            try {
                return activeDataSource.getConnection();
            } catch (SQLException e) {
                System.err.println("CIVENTRA: Pool connection failed, returning fallback H2 connection: " + e.getMessage());
            }
        }
        return DriverManager.getConnection("jdbc:h2:mem:civentradb;DB_CLOSE_DELAY=-1;MODE=Oracle", "sa", "");
    }
}

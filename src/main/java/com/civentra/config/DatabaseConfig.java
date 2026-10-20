package com.civentra.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf.Logger;
import org.slf.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${civentra.db.url:${spring.datasource.url:jdbc:oracle:thin:@//localhost:1521/XEPDB1}}")
    private String dbUrl;

    @Value("${civentra.db.username:${spring.datasource.username:CIVENTRA_USER}}")
    private String dbUsername;

    @Value("${civentra.db.password:${spring.datasource.password:civentra2026}}")
    private String dbPassword;

    @Value("${civentra.db.pool.size:10}")
    private int poolSize;

    private static volatile HikariDataSource dataSource;

    @Bean(destroyMethod = "close")
    @Primary
    public DataSource dataSource() {
        HikariDataSource ds = createDataSource(dbUrl, dbUsername, dbPassword);
        dataSource = ds;
        return ds;
    }

    private HikariDataSource createDataSource(String url, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setPoolName("CiventraPool");

        // Dynamically assign driver based on URL
        if (url != null && url.startsWith("jdbc:oracle")) {
            config.setDriverClassName("oracle.jdbc.OracleDriver");
        } else {
            config.setDriverClassName("org.h2.Driver");
        }

        // Fast fail timeout (1 second) so cloud boot doesn't hang
        config.setInitializationFailTimeout(1000);

        try {
            log.info("Attempting primary database connection to: {}", url);
            return new HikariDataSource(config);
        } catch (Exception e) {
            log.warn("Primary Oracle DB unreachable ({}): {}. Switching automatically to H2 In-Memory Fallback DB.", url, e.getMessage());
            
            HikariConfig h2Config = new HikariConfig();
            h2Config.setJdbcUrl("jdbc:h2:mem:civentradb;DB_CLOSE_DELAY=-1;MODE=Oracle");
            h2Config.setUsername("sa");
            h2Config.setPassword("");
            h2Config.setDriverClassName("org.h2.Driver");
            h2Config.setMaximumPoolSize(poolSize);
            h2Config.setPoolName("CiventraH2FallbackPool");
            return new HikariDataSource(h2Config);
        }
    }

    /** Legacy DAOs use this bridge; it uses the initialized Hikari pool safely. */
    public static java.sql.Connection getConnection() throws java.sql.SQLException {
        HikariDataSource current = dataSource;
        if (current == null || current.isClosed()) {
            throw new java.sql.SQLException("CIVENTRA database pool is not initialized");
        }
        return current.getConnection();
    }
}
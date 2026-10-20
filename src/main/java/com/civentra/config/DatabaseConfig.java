package com.civentra.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Value("${civentra.db.url}")
    private String dbUrl;

    @Value("${civentra.db.username}")
    private String dbUsername;

    @Value("${civentra.db.password}")
    private String dbPassword;

    @Value("${civentra.db.pool.size:10}")
    private int poolSize;

    private static volatile HikariDataSource dataSource;

    @Bean(destroyMethod = "close")
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(poolSize);
        config.setPoolName("CiventraOraclePool");
        config.setDriverClassName("oracle.jdbc.OracleDriver");

        HikariDataSource created = new HikariDataSource(config);
        dataSource = created;
        return created;
    }

    /** Legacy DAOs use this bridge; it still uses the configured pool and never opens a second hard-coded connection. */
    public static java.sql.Connection getConnection() throws java.sql.SQLException {
        HikariDataSource current = dataSource;
        if (current == null || current.isClosed()) {
            throw new java.sql.SQLException("CIVENTRA database pool is not initialized");
        }
        return current.getConnection();
    }
}

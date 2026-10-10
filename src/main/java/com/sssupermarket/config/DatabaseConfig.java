package com.sssupermarket.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@Profile("!test")
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        String rawUrl = getEnvOrProperty(env, "DATABASE_URL", "DB_URL", "spring.datasource.url");
        String username = getEnvOrProperty(env, "DB_USERNAME", "DATABASE_USERNAME", "spring.datasource.username");
        String password = getEnvOrProperty(env, "DB_PASSWORD", "DATABASE_PASSWORD", "spring.datasource.password");

        DbParams params = resolveConnection(rawUrl, username, password);

        try {
            return createHikariDataSource(params);
        } catch (Exception e) {
            log.error("Failed to initialize database connection for {}: {}. Starting with embedded database fallback.",
                    sanitizeUrl(params.jdbcUrl), e.getMessage());
            DbParams fallback = fallbackH2("Primary database connection could not be established");
            return createHikariDataSource(fallback);
        }
    }

    private DataSource createHikariDataSource(DbParams params) {
        log.info("Initializing DataSource with URL: {}", sanitizeUrl(params.jdbcUrl));
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(params.jdbcUrl);
        if (params.username != null) {
            config.setUsername(params.username);
        }
        if (params.password != null) {
            config.setPassword(params.password);
        }
        if (params.driverClassName != null) {
            config.setDriverClassName(params.driverClassName);
        }
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000);
        config.setInitializationFailTimeout(10000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }

    private DbParams resolveConnection(String rawUrl, String defaultUser, String defaultPass) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return fallbackH2("No database URL provided");
        }

        String trimmed = rawUrl.trim();

        // Render / Cloud PostgreSQL format: postgres://user:pass@host:port/dbname
        if (trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://")) {
            String u = defaultUser;
            String p = defaultPass;
            String host = "localhost";
            int port = 5432;
            String path = "/sssupermarket";
            String query = null;

            Pattern pattern = Pattern.compile("^(?:postgres(?:ql)?):\\/\\/(?:([^:@]+)(?::([^@]*))?@)?([^\\/:]+)(?::(\\d+))?(\\/[^?]*)?(?:\\?(.*))?$");
            Matcher m = pattern.matcher(trimmed);
            if (m.matches()) {
                if (m.group(1) != null && !m.group(1).isBlank()) u = m.group(1);
                if (m.group(2) != null && !m.group(2).isBlank()) p = m.group(2);
                if (m.group(3) != null && !m.group(3).isBlank()) host = m.group(3);
                if (m.group(4) != null && !m.group(4).isBlank()) {
                    try { port = Integer.parseInt(m.group(4)); } catch (NumberFormatException ignored) {}
                }
                if (m.group(5) != null && !m.group(5).isBlank()) path = m.group(5);
                if (m.group(6) != null && !m.group(6).isBlank()) query = m.group(6);
            } else {
                try {
                    URI uri = new URI(trimmed);
                    if (uri.getHost() != null) host = uri.getHost();
                    if (uri.getPort() > 0) port = uri.getPort();
                    if (uri.getPath() != null) path = uri.getPath();
                    String userInfo = uri.getUserInfo();
                    if (userInfo != null && !userInfo.isBlank()) {
                        String[] parts = userInfo.split(":", 2);
                        u = parts[0];
                        if (parts.length > 1) p = parts[1];
                    }
                    query = uri.getQuery();
                } catch (Exception ignored) {}
            }

            if (host.contains("dpg-") && System.getenv("RENDER") == null && !isPortReachable(host, port, 800)) {
                log.info("Detected Render internal PostgreSQL host outside of Render cloud. Checking local MySQL at localhost:3306...");
                if (isPortReachable("localhost", 3306, 600)) {
                    log.info("Connecting to local MySQL at localhost:3306...");
                    return new DbParams(
                            "jdbc:mysql://localhost:3306/sssupermarket?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                            defaultUser != null && !defaultUser.isBlank() ? defaultUser : "root",
                            defaultPass != null && !defaultPass.isBlank() ? defaultPass : "root",
                            "com.mysql.cj.jdbc.Driver"
                    );
                }
                return fallbackH2("Render internal PostgreSQL host is only accessible inside Render cloud network");
            }

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
            if (query != null && !query.isBlank()) {
                jdbcUrl += "?" + query;
            }
            if (!jdbcUrl.contains("sslmode=")) {
                jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
            }
            log.info("Detected PostgreSQL URL from environment. Configured JDBC URL: {}", sanitizeUrl(jdbcUrl));
            return new DbParams(jdbcUrl, u, p, "org.postgresql.Driver");
        }

        // MySQL URI format: mysql://user:pass@host:port/dbname
        if (trimmed.startsWith("mysql://")) {
            try {
                URI uri = new URI(trimmed);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 3306;
                String path = uri.getPath() != null ? uri.getPath() : "/sssupermarket";
                String u = defaultUser;
                String p = defaultPass;
                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isBlank()) {
                    String[] parts = userInfo.split(":", 2);
                    u = parts[0];
                    p = parts.length > 1 ? parts[1] : "";
                }
                String jdbcUrl = "jdbc:mysql://" + host + ":" + port + path
                        + "?createDatabaseIfNotExist=true&useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC";
                String query = uri.getQuery();
                if (query != null && !query.isBlank()) {
                    jdbcUrl += "&" + query;
                }
                log.info("Detected MySQL URI from environment.");
                return new DbParams(jdbcUrl, u, p, "com.mysql.cj.jdbc.Driver");
            } catch (Exception e) {
                log.warn("Could not parse MySQL URI, attempting direct connection: {}", e.getMessage());
            }
        }

        // Standard JDBC PostgreSQL URL
        if (trimmed.startsWith("jdbc:postgresql:")) {
            return new DbParams(trimmed, defaultUser, defaultPass, "org.postgresql.Driver");
        }

        // Standard JDBC MySQL URL
        if (trimmed.startsWith("jdbc:mysql:")) {
            boolean isLocal = trimmed.contains("://localhost") || trimmed.contains("://127.0.0.1");
            boolean isRender = System.getenv("RENDER") != null;

            if (isLocal && (isRender || !isPortReachable("localhost", 3306, 600))) {
                return fallbackH2("Local MySQL at localhost:3306 is not reachable");
            }

            return new DbParams(trimmed, defaultUser, defaultPass, "com.mysql.cj.jdbc.Driver");
        }

        // Standard JDBC H2 or other database
        if (trimmed.startsWith("jdbc:h2:")) {
            return new DbParams(trimmed, defaultUser, defaultPass, "org.h2.Driver");
        }

        return new DbParams(trimmed, defaultUser, defaultPass, null);
    }

    private DbParams fallbackH2(String reason) {
        log.warn("=========================================================================================");
        log.warn("{}. Falling back to embedded H2 database (MySQL compatibility mode).", reason);
        log.warn("Sample data and default users (admin, seller) will be seeded automatically.");
        log.warn("To connect a persistent database on Render, configure environment variables in Dashboard:");
        log.warn("  DB_URL = jdbc:mysql://<host>:<port>/<dbname>?useSSL=true&serverTimezone=UTC");
        log.warn("  DB_USERNAME = <username>");
        log.warn("  DB_PASSWORD = <password>");
        log.warn("  (Or attach a Render PostgreSQL database to auto-configure DATABASE_URL)");
        log.warn("=========================================================================================");

        return new DbParams(
                "jdbc:h2:mem:sssupermarket;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
                "sa",
                "",
                "org.h2.Driver"
        );
    }

    private boolean isPortReachable(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String getEnvOrProperty(Environment env, String... keys) {
        for (String key : keys) {
            String val = System.getenv(key);
            if (val != null && !val.isBlank()) {
                return val;
            }
            val = env.getProperty(key);
            if (val != null && !val.isBlank()) {
                return val;
            }
        }
        return null;
    }

    private String sanitizeUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll(":[^/@:]+@", ":***@");
    }

    private record DbParams(String jdbcUrl, String username, String password, String driverClassName) {}
}

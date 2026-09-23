package com.playground.fondoahorro.infrastructure.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the local SQLite database file: creates the data directory if missing,
 * runs Flyway migrations on startup, and hands out JDBC connections with
 * foreign key enforcement enabled (SQLite disables it per-connection by default).
 */
public final class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    // Overridable via -Dfondoahorro.data.dir=... so tests can point this at an
    // isolated temp directory instead of the real data/ folder. Read once at
    // class load, so it must be set before anything in this class is touched.
    private static final Path DB_DIR = Path.of(System.getProperty("fondoahorro.data.dir", "data"));
    private static final Path DB_FILE = DB_DIR.resolve("fondo-ahorro.db");
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_FILE;

    private static boolean initialized = false;

    private DatabaseManager() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        try {
            Files.createDirectories(DB_DIR);
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible crear la carpeta de datos: " + DB_DIR, e);
        }

        log.info("Inicializando base de datos en {}", DB_FILE.toAbsolutePath());

        Flyway flyway = Flyway.configure()
                .dataSource(JDBC_URL, null, null)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        initialized = true;
    }

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(JDBC_URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static Path getDatabaseFilePath() {
        return DB_FILE;
    }
}

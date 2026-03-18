package com.piggypro.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DBConnection.java
 * ─────────────────────────────────────────────────────
 * Singleton that manages a single SQLite JDBC connection
 * for the entire application lifetime.
 *
 * Database file location:
 *   <user home>/PiggyPro/piggypro.db
 *   e.g. C:/Users/Vinayak/PiggyPro/piggypro.db  (Windows)
 *        /home/vinayak/PiggyPro/piggypro.db      (Linux/Mac)
 *
 * Usage:
 *   Connection conn = DBConnection.getInstance().getConnection();
 *
 * On first run, createTables() is called automatically to
 * set up all required tables.
 */
public class DBConnection {

    // ── Singleton ──────────────────────────────────
    private static DBConnection instance;

    private Connection connection;

    // Database file path inside user's home directory
    private static final String DB_DIR  = System.getProperty("user.home")
            + File.separator + "PiggyPro";
    private static final String DB_FILE = DB_DIR + File.separator + "piggypro.db";
    private static final String DB_URL  = "jdbc:sqlite:" + DB_FILE;

    // ── Constructor ────────────────────────────────
    private DBConnection() {
        try {
            // Ensure directory exists
            File dir = new File(DB_DIR);
            if (!dir.exists()) dir.mkdirs();

            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Open connection
            connection = DriverManager.getConnection(DB_URL);

            // Enable WAL mode for better performance
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL;");
                st.execute("PRAGMA foreign_keys=ON;");
            }

            // Create all tables on first run
            createTables();

            System.out.println("DBConnection: database ready at " + DB_FILE);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "SQLite JDBC driver not found. Add sqlite-jdbc to pom.xml.", e);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to open SQLite database: " + DB_FILE, e);
        }
    }

    // ── Singleton accessor ─────────────────────────
    public static synchronized DBConnection getInstance() {
        if (instance == null) instance = new DBConnection();
        return instance;
    }

    public Connection getConnection() {
        try {
            // Reconnect if connection was closed or timed out
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reconnect to database.", e);
        }
        return connection;
    }

    // ── Close ──────────────────────────────────────
    /** Call this from App.stop() when the application exits. */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("DBConnection: connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("DBConnection: error closing connection — " + e.getMessage());
        }
    }

    // ── Schema creation ────────────────────────────
    /**
     * Creates all tables if they do not already exist.
     * Safe to call on every startup — uses CREATE TABLE IF NOT EXISTS.
     */
    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {

            // ── users ──────────────────────────────
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    username     TEXT    NOT NULL UNIQUE,
                    full_name    TEXT    NOT NULL,
                    email        TEXT    NOT NULL UNIQUE,
                    password_hash TEXT   NOT NULL,
                    created_at   TEXT    NOT NULL DEFAULT (datetime('now'))
                );
            """);

            // ── categories ─────────────────────────
            st.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    name    TEXT    NOT NULL UNIQUE,
                    color   TEXT    NOT NULL DEFAULT '#64748B',
                    is_default INTEGER NOT NULL DEFAULT 0
                );
            """);

            // ── expenses ───────────────────────────
            st.execute("""
                CREATE TABLE IF NOT EXISTS expenses (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    description TEXT    NOT NULL,
                    amount      REAL    NOT NULL,
                    type        TEXT    NOT NULL CHECK(type IN ('Expense','Income')),
                    category    TEXT    NOT NULL,
                    date        TEXT    NOT NULL,
                    note        TEXT    DEFAULT '',
                    created_at  TEXT    NOT NULL DEFAULT (datetime('now'))
                );
            """);

            // ── budgets ────────────────────────────
            st.execute("""
                CREATE TABLE IF NOT EXISTS budgets (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    category    TEXT    NOT NULL,
                    amount_limit REAL   NOT NULL,
                    month       TEXT    NOT NULL,
                    created_at  TEXT    NOT NULL DEFAULT (datetime('now')),
                    UNIQUE(user_id, category, month)
                );
            """);

            // Seed default categories if table is empty
            st.execute("""
                INSERT OR IGNORE INTO categories (name, color, is_default) VALUES
                    ('Food and Dining', '#F59E0B', 1),
                    ('Shopping',        '#2563EB', 1),
                    ('Transport',       '#8B5CF6', 1),
                    ('Utilities',       '#EF4444', 1),
                    ('Entertainment',   '#EC4899', 1),
                    ('Rent',            '#2563EB', 1),
                    ('Income',          '#10B981', 1),
                    ('Other',           '#64748B', 1);
            """);
        }
    }
}

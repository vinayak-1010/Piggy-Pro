package com.piggypro.dao;

import com.piggypro.db.DBConnection;
import com.piggypro.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * UserDAO.java
 * ─────────────────────────────────────────────────────
 * Data Access Object for the 'users' table.
 * All SQL operations relating to users go here.
 *
 * Every method uses PreparedStatements to prevent
 * SQL injection.
 *
 * Usage (via AuthService — not called directly from UI):
 *   UserDAO dao = new UserDAO();
 *   Optional<User> user = dao.findByUsername("vinayak");
 */
public class UserDAO {

    private final Connection conn;

    public UserDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ══════════════════════════════════════════════
    // CREATE
    // ══════════════════════════════════════════════

    /**
     * Inserts a new user into the database.
     * The user's id field is updated with the generated key.
     *
     * @param user a User object with passwordHash already set
     * @return the same User object with id populated
     * @throws SQLException if username/email already exists
     */
    public User insert(User user) throws SQLException {
        String sql = """
            INSERT INTO users (username, full_name, email, password_hash)
            VALUES (?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPasswordHash());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) user.setId(keys.getInt(1));
            }
        }
        return user;
    }

    // ══════════════════════════════════════════════
    // READ
    // ══════════════════════════════════════════════

    /**
     * Finds a user by their username (case-insensitive).
     * Used by AuthService for login.
     *
     * @param username the username to search for
     * @return Optional containing the User, or empty if not found
     */
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a user by their email address (case-insensitive).
     * Used during signup to check for duplicates.
     */
    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a user by their primary key id.
     */
    public Optional<User> findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    // ══════════════════════════════════════════════
    // EXISTS CHECKS
    // ══════════════════════════════════════════════

    /** Returns true if a user with this username already exists. */
    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Returns true if a user with this email already exists. */
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ══════════════════════════════════════════════
    // UPDATE
    // ══════════════════════════════════════════════

    /**
     * Updates the user's full name and email.
     * Used from a future profile/settings screen.
     */
    public void update(User user) throws SQLException {
        String sql = """
            UPDATE users SET full_name = ?, email = ?
            WHERE id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setInt(3, user.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Updates only the password hash.
     * Used for password-change / forgot-password flow.
     */
    public void updatePassword(int userId, String newHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════

    /**
     * Deletes a user and all their associated data
     * (expenses, budgets) via ON DELETE CASCADE.
     */
    public void delete(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════
    // ROW MAPPER
    // ══════════════════════════════════════════════

    /** Maps a ResultSet row to a User object. */
    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                LocalDateTime.parse(rs.getString("created_at")
                        .replace(" ", "T"))
        );
    }
}

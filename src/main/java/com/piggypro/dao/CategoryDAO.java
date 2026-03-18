package com.piggypro.dao;

import com.piggypro.db.DBConnection;
import com.piggypro.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CategoryDAO.java
 * ─────────────────────────────────────────────────────
 * Data Access Object for the 'categories' table.
 * All SQL for categories goes here.
 *
 * Default categories are seeded automatically in
 * DBConnection.createTables() — this DAO handles
 * reading them and managing custom categories.
 */
public class CategoryDAO {

    private final Connection conn;

    public CategoryDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ══════════════════════════════════════════════
    // READ
    // ══════════════════════════════════════════════

    /**
     * Returns all categories (default + custom),
     * ordered alphabetically.
     */
    public List<Category> findAll() throws SQLException {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM categories ORDER BY is_default DESC, name ASC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Returns only the names of all categories as strings.
     * Useful for populating ComboBox dropdowns.
     */
    public List<String> findAllNames() throws SQLException {
        List<String> names = new ArrayList<>();
        String sql = "SELECT name FROM categories ORDER BY is_default DESC, name ASC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) names.add(rs.getString("name"));
        }
        return names;
    }

    /** Finds a category by its exact name. */
    public Optional<Category> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM categories WHERE LOWER(name) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /** Finds a category by its id. */
    public Optional<Category> findById(int id) throws SQLException {
        String sql = "SELECT * FROM categories WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    // ══════════════════════════════════════════════
    // CREATE
    // ══════════════════════════════════════════════

    /**
     * Inserts a new custom category.
     * The id is set on the Category object after insert.
     *
     * @throws SQLException if name already exists
     */
    public Category insert(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name, color, is_default) VALUES (?, ?, 0)";
        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getColor());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) category.setId(keys.getInt(1));
            }
        }
        return category;
    }

    // ══════════════════════════════════════════════
    // UPDATE
    // ══════════════════════════════════════════════

    /**
     * Updates a custom category's name and color.
     * Default categories cannot be renamed.
     */
    public void update(Category category) throws SQLException {
        String sql = """
            UPDATE categories SET name = ?, color = ?
            WHERE id = ? AND is_default = 0
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getColor());
            ps.setInt(3, category.getId());
            ps.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════

    /**
     * Deletes a custom category by id.
     * Default categories are protected and cannot be deleted.
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM categories WHERE id = ? AND is_default = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════
    // EXISTS CHECK
    // ══════════════════════════════════════════════

    /** Returns true if a category with this name already exists. */
    public boolean nameExists(String name) throws SQLException {
        String sql = "SELECT 1 FROM categories WHERE LOWER(name) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ══════════════════════════════════════════════
    // ROW MAPPER
    // ══════════════════════════════════════════════

    private Category mapRow(ResultSet rs) throws SQLException {
        return new Category(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("color"),
                rs.getInt("is_default") == 1
        );
    }
}
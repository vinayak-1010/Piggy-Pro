package com.piggypro.dao;

import com.piggypro.db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * BudgetDAO.java
 * ─────────────────────────────────────────────────────
 * Data Access Object for the 'budgets' table.
 *
 * Schema reminder:
 *   id, user_id, category, amount_limit, month (YYYY-MM), created_at
 *   UNIQUE constraint: (user_id, category, month)
 */
public class BudgetDAO {

    // ── Simple budget record ───────────────────────
    public static class Budget {
        public int    id;
        public int    userId;
        public String category;
        public double limit;
        public String month;   // "YYYY-MM"

        public Budget(int id, int userId, String category,
                      double limit, String month) {
            this.id       = id;
            this.userId   = userId;
            this.category = category;
            this.limit    = limit;
            this.month    = month;
        }
    }

    private final Connection conn;

    public BudgetDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ══════════════════════════════════════════════
    // CREATE / UPSERT
    // ══════════════════════════════════════════════

    /**
     * Inserts a new budget or replaces an existing one
     * for the same (user_id, category, month) combination.
     */
    public Budget upsert(int userId, String category,
                         double limit, YearMonth month) throws SQLException {
        String sql = """
            INSERT INTO budgets (user_id, category, amount_limit, month)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(user_id, category, month)
            DO UPDATE SET amount_limit = excluded.amount_limit
        """;
        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,    userId);
            ps.setString(2, category);
            ps.setDouble(3, limit);
            ps.setString(4, month.toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                int id = keys.next() ? keys.getInt(1) : -1;
                return new Budget(id, userId, category, limit, month.toString());
            }
        }
    }

    // ══════════════════════════════════════════════
    // READ
    // ══════════════════════════════════════════════

    /** Returns all budgets for a user in a given month. */
    public List<Budget> findByMonth(int userId, YearMonth month) throws SQLException {
        String sql = """
            SELECT * FROM budgets
            WHERE user_id = ? AND month = ?
            ORDER BY category ASC
        """;
        List<Budget> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,    userId);
            ps.setString(2, month.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** Returns a single budget for a user, category, and month. */
    public Optional<Budget> find(int userId, String category,
                                 YearMonth month) throws SQLException {
        String sql = """
            SELECT * FROM budgets
            WHERE user_id = ? AND LOWER(category) = LOWER(?) AND month = ?
            LIMIT 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,    userId);
            ps.setString(2, category);
            ps.setString(3, month.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    // ══════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════

    public void delete(int id, int userId) throws SQLException {
        String sql = "DELETE FROM budgets WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════
    // ROW MAPPER
    // ══════════════════════════════════════════════

    private Budget mapRow(ResultSet rs) throws SQLException {
        return new Budget(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("category"),
                rs.getDouble("amount_limit"),
                rs.getString("month")
        );
    }
}
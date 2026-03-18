package com.piggypro.dao;

import com.piggypro.db.DBConnection;
import com.piggypro.model.Expense;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ExpenseDAO.java
 * ─────────────────────────────────────────────────────
 * Data Access Object for the 'expenses' table.
 *
 * All filtering is done in SQL (not in memory) for
 * performance even with large datasets.
 *
 * Every method is scoped to a specific userId so one
 * user never sees another user's data.
 */
public class ExpenseDAO {

    private final Connection conn;

    public ExpenseDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // ══════════════════════════════════════════════
    // CREATE
    // ══════════════════════════════════════════════

    /**
     * Inserts a new expense/income record.
     * Sets the generated id on the Expense object.
     */
    public Expense insert(Expense expense) throws SQLException {
        String sql = """
            INSERT INTO expenses
              (user_id, description, amount, type, category, date, note)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,    expense.getUserId());
            ps.setString(2, expense.getDescription());
            ps.setDouble(3, expense.getAmount());
            ps.setString(4, expense.getType());
            ps.setString(5, expense.getCategory());
            ps.setString(6, expense.getDate().toString());
            ps.setString(7, expense.getNote() != null ? expense.getNote() : "");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) expense.setId(keys.getInt(1));
            }
        }
        return expense;
    }

    // ══════════════════════════════════════════════
    // READ — Single record
    // ══════════════════════════════════════════════

    /** Finds a single expense by its id, scoped to a user. */
    public Optional<Expense> findById(int id, int userId) throws SQLException {
        String sql = "SELECT * FROM expenses WHERE id = ? AND user_id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    // ══════════════════════════════════════════════
    // READ — All for user
    // ══════════════════════════════════════════════

    /**
     * Returns all expenses for a user, newest first.
     */
    public List<Expense> findAllByUser(int userId) throws SQLException {
        String sql = """
            SELECT * FROM expenses
            WHERE user_id = ?
            ORDER BY date DESC, created_at DESC
        """;
        return queryList(sql, userId);
    }

    // ══════════════════════════════════════════════
    // READ — Filtered
    // ══════════════════════════════════════════════

    /**
     * Returns expenses filtered by date range, category, type, and
     * a search term matched against description and note.
     *
     * Any parameter can be null to skip that filter.
     *
     * @param userId   required — scopes to this user only
     * @param from     start date (inclusive), or null
     * @param to       end date (inclusive), or null
     * @param category category name, or null for all
     * @param type     "Expense" or "Income", or null for both
     * @param search   substring to match in description/note, or null
     * @param minAmt   minimum amount, or null
     * @param maxAmt   maximum amount, or null
     */
    public List<Expense> findFiltered(int userId,
                                      LocalDate from, LocalDate to,
                                      String category, String type,
                                      String search,
                                      Double minAmt, Double maxAmt)
            throws SQLException {

        StringBuilder sql = new StringBuilder(
                "SELECT * FROM expenses WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (from != null) {
            sql.append(" AND date >= ?");
            params.add(from.toString());
        }
        if (to != null) {
            sql.append(" AND date <= ?");
            params.add(to.toString());
        }
        if (category != null && !category.isBlank()
                && !category.equalsIgnoreCase("All Categories")) {
            sql.append(" AND LOWER(category) = LOWER(?)");
            params.add(category);
        }
        if (type != null && !type.isBlank()
                && !type.equalsIgnoreCase("All Types")) {
            sql.append(" AND type = ?");
            params.add(type);
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(description) LIKE ? OR LOWER(note) LIKE ?)");
            String like = "%" + search.toLowerCase() + "%";
            params.add(like);
            params.add(like);
        }
        if (minAmt != null) {
            sql.append(" AND amount >= ?");
            params.add(minAmt);
        }
        if (maxAmt != null) {
            sql.append(" AND amount <= ?");
            params.add(maxAmt);
        }
        sql.append(" ORDER BY date DESC, created_at DESC");

        List<Expense> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++)
                ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ══════════════════════════════════════════════
    // READ — For dashboard / analytics
    // ══════════════════════════════════════════════

    /**
     * Returns the most recent N expenses for a user.
     * Used by the Dashboard "Recent Transactions" section.
     */
    public List<Expense> findRecent(int userId, int limit) throws SQLException {
        String sql = """
            SELECT * FROM expenses WHERE user_id = ?
            ORDER BY date DESC, created_at DESC LIMIT ?
        """;
        List<Expense> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Returns all expenses for a specific month.
     * month format: "YYYY-MM" e.g. "2026-03"
     */
    public List<Expense> findByMonth(int userId, String month) throws SQLException {
        String sql = """
            SELECT * FROM expenses
            WHERE user_id = ? AND strftime('%Y-%m', date) = ?
            ORDER BY date DESC
        """;
        List<Expense> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ══════════════════════════════════════════════
    // AGGREGATES — for Analytics + Dashboard
    // ══════════════════════════════════════════════

    /**
     * Returns total expenses grouped by category for a date range.
     * Returns a map of category name → total amount, sorted by amount desc.
     */
    public Map<String, Double> sumByCategory(int userId,
                                             LocalDate from,
                                             LocalDate to)
            throws SQLException {
        String sql = """
            SELECT category, SUM(amount) as total
            FROM expenses
            WHERE user_id = ? AND type = 'Expense'
              AND date >= ? AND date <= ?
            GROUP BY category
            ORDER BY total DESC
        """;
        Map<String, Double> result = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, from.toString());
            ps.setString(3, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    result.put(rs.getString("category"), rs.getDouble("total"));
            }
        }
        return result;
    }

    /**
     * Returns total expenses grouped by month for the last N months.
     * Returns a map of "YYYY-MM" → total amount.
     */
    public Map<String, Double> sumByMonth(int userId, int months) throws SQLException {
        String sql = """
            SELECT strftime('%Y-%m', date) as month, SUM(amount) as total
            FROM expenses
            WHERE user_id = ? AND type = 'Expense'
              AND date >= date('now', ? || ' months')
            GROUP BY month
            ORDER BY month ASC
        """;
        Map<String, Double> result = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, "-" + months);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    result.put(rs.getString("month"), rs.getDouble("total"));
            }
        }
        return result;
    }

    /**
     * Returns the total amount of all expenses in a date range.
     */
    public double totalExpenses(int userId, LocalDate from, LocalDate to)
            throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(amount), 0) FROM expenses
            WHERE user_id = ? AND type = 'Expense'
              AND date >= ? AND date <= ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, from.toString());
            ps.setString(3, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    /**
     * Returns the total amount of all income in a date range.
     */
    public double totalIncome(int userId, LocalDate from, LocalDate to)
            throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(amount), 0) FROM expenses
            WHERE user_id = ? AND type = 'Income'
              AND date >= ? AND date <= ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, from.toString());
            ps.setString(3, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    /**
     * Returns total spent in a specific category for a given month.
     * Used by BudgetsController to check budget usage.
     * month format: "YYYY-MM"
     */
    public double totalByCategory(int userId, String category, String month)
            throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(amount), 0) FROM expenses
            WHERE user_id = ? AND type = 'Expense'
              AND LOWER(category) = LOWER(?)
              AND strftime('%Y-%m', date) = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, category);
            ps.setString(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    // ══════════════════════════════════════════════
    // UPDATE
    // ══════════════════════════════════════════════

    /**
     * Updates all editable fields of an expense.
     * Scoped to userId for security.
     */
    public void update(Expense expense) throws SQLException {
        String sql = """
            UPDATE expenses
            SET description = ?, amount = ?, type = ?,
                category = ?, date = ?, note = ?
            WHERE id = ? AND user_id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, expense.getDescription());
            ps.setDouble(2, expense.getAmount());
            ps.setString(3, expense.getType());
            ps.setString(4, expense.getCategory());
            ps.setString(5, expense.getDate().toString());
            ps.setString(6, expense.getNote() != null ? expense.getNote() : "");
            ps.setInt(7,    expense.getId());
            ps.setInt(8,    expense.getUserId());
            ps.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════

    /**
     * Deletes a single expense by id, scoped to userId.
     */
    public void delete(int id, int userId) throws SQLException {
        String sql = "DELETE FROM expenses WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════

    private List<Expense> queryList(String sql, int userId) throws SQLException {
        List<Expense> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Expense mapRow(ResultSet rs) throws SQLException {
        return new Expense(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("description"),
                rs.getDouble("amount"),
                rs.getString("type"),
                rs.getString("category"),
                LocalDate.parse(rs.getString("date")),
                rs.getString("note"),
                rs.getString("created_at")
        );
    }
}
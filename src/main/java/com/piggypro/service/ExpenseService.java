package com.piggypro.service;

import com.piggypro.dao.ExpenseDAO;
import com.piggypro.model.Expense;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ExpenseService.java
 * ─────────────────────────────────────────────────────
 * Business logic layer for expenses and income records.
 * Validates all input before passing to ExpenseDAO.
 *
 * Usage from controllers:
 *   ExpenseService svc = ExpenseService.getInstance();
 *
 *   // Add a new expense
 *   ExpenseResult result = svc.addExpense(
 *       userId, "Swiggy", 340.0, "Expense",
 *       "Food and Dining", LocalDate.now(), "Dinner");
 *
 *   // Get all for current user
 *   List<Expense> list = svc.getAllForUser(userId);
 *
 *   // Get filtered
 *   List<Expense> filtered = svc.getFiltered(
 *       userId, from, to, "Shopping", "Expense", "amazon", null, null);
 */
public class ExpenseService {

    // ── Singleton ──────────────────────────────────
    private static ExpenseService instance;

    private final ExpenseDAO expenseDAO;

    private ExpenseService() {
        this.expenseDAO = new ExpenseDAO();
    }

    public static synchronized ExpenseService getInstance() {
        if (instance == null) instance = new ExpenseService();
        return instance;
    }

    // ══════════════════════════════════════════════
    // RESULT WRAPPER
    // ══════════════════════════════════════════════

    public record ExpenseResult(boolean success, String message, Expense expense) {
        public static ExpenseResult ok(Expense e)    { return new ExpenseResult(true,  "Success", e);    }
        public static ExpenseResult fail(String msg) { return new ExpenseResult(false, msg, null);        }
    }

    // ══════════════════════════════════════════════
    // CREATE
    // ══════════════════════════════════════════════

    /**
     * Validates and adds a new expense or income record.
     *
     * @param userId      the logged-in user's id
     * @param description what the expense was for
     * @param amount      positive value (sign is set by type)
     * @param type        "Expense" or "Income"
     * @param category    category name
     * @param date        transaction date
     * @param note        optional extra note
     */
    public ExpenseResult addExpense(int userId, String description,
                                    double amount, String type,
                                    String category, LocalDate date,
                                    String note) {
        // Validation
        String err = validate(description, amount, type, category, date);
        if (err != null) return ExpenseResult.fail(err);

        try {
            Expense expense = new Expense(userId, description.trim(),
                    amount, type, category,
                    date, note != null ? note.trim() : "");
            expenseDAO.insert(expense);
            return ExpenseResult.ok(expense);
        } catch (SQLException e) {
            System.err.println("ExpenseService.addExpense() error: " + e.getMessage());
            return ExpenseResult.fail("Failed to save expense. Please try again.");
        }
    }

    // ══════════════════════════════════════════════
    // READ
    // ══════════════════════════════════════════════

    /**
     * Returns all expenses for a user, newest first.
     */
    public List<Expense> getAllForUser(int userId) {
        try {
            return expenseDAO.findAllByUser(userId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load expenses.", e);
        }
    }

    /**
     * Returns filtered expenses based on provided criteria.
     * Any parameter except userId can be null to skip that filter.
     */
    public List<Expense> getFiltered(int userId,
                                     LocalDate from, LocalDate to,
                                     String category, String type,
                                     String search,
                                     Double minAmt, Double maxAmt) {
        try {
            return expenseDAO.findFiltered(userId, from, to,
                    category, type, search,
                    minAmt, maxAmt);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to filter expenses.", e);
        }
    }

    /**
     * Returns the most recent N expenses.
     * Used by Dashboard's recent transactions list.
     */
    public List<Expense> getRecent(int userId, int limit) {
        try {
            return expenseDAO.findRecent(userId, limit);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load recent expenses.", e);
        }
    }

    /**
     * Returns all expenses for a specific YearMonth.
     * Used by Budgets screen to compute spending vs limit.
     */
    public List<Expense> getForMonth(int userId, YearMonth month) {
        try {
            return expenseDAO.findByMonth(userId, month.toString());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load monthly expenses.", e);
        }
    }

    // ══════════════════════════════════════════════
    // AGGREGATES
    // ══════════════════════════════════════════════

    /**
     * Returns spending totals grouped by category for a date range.
     * Map: category name → total amount (expenses only).
     * Used by Analytics donut chart.
     */
    public Map<String, Double> getCategoryTotals(int userId,
                                                 LocalDate from,
                                                 LocalDate to) {
        try {
            return expenseDAO.sumByCategory(userId, from, to);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load category totals.", e);
        }
    }

    /**
     * Returns spending totals grouped by month for the last N months.
     * Map: "YYYY-MM" → total amount (expenses only).
     * Used by Analytics bar chart.
     */
    public Map<String, Double> getMonthlyTotals(int userId, int months) {
        try {
            return expenseDAO.sumByMonth(userId, months);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load monthly totals.", e);
        }
    }

    /**
     * Returns total expenses in a date range.
     */
    public double getTotalExpenses(int userId, LocalDate from, LocalDate to) {
        try {
            return expenseDAO.totalExpenses(userId, from, to);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to calculate total expenses.", e);
        }
    }

    /**
     * Returns total income in a date range.
     */
    public double getTotalIncome(int userId, LocalDate from, LocalDate to) {
        try {
            return expenseDAO.totalIncome(userId, from, to);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to calculate total income.", e);
        }
    }

    /**
     * Returns total spent in a specific category for a month.
     * Used by BudgetsController to show real spent amounts.
     * month: YearMonth object e.g. YearMonth.now()
     */
    public double getSpentInCategory(int userId, String category, YearMonth month) {
        try {
            return expenseDAO.totalByCategory(userId, category, month.toString());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load category spending.", e);
        }
    }

    // ══════════════════════════════════════════════
    // UPDATE
    // ══════════════════════════════════════════════

    /**
     * Updates an existing expense record after validation.
     */
    public ExpenseResult updateExpense(int userId, int expenseId,
                                       String description, double amount,
                                       String type, String category,
                                       LocalDate date, String note) {
        String err = validate(description, amount, type, category, date);
        if (err != null) return ExpenseResult.fail(err);

        try {
            Optional<Expense> existing = expenseDAO.findById(expenseId, userId);
            if (existing.isEmpty())
                return ExpenseResult.fail("Expense not found.");

            Expense e = existing.get();
            e.setDescription(description.trim());
            e.setAmount(amount);
            e.setType(type);
            e.setCategory(category);
            e.setDate(date);
            e.setNote(note != null ? note.trim() : "");
            expenseDAO.update(e);
            return ExpenseResult.ok(e);
        } catch (SQLException e) {
            System.err.println("ExpenseService.updateExpense() error: " + e.getMessage());
            return ExpenseResult.fail("Failed to update expense. Please try again.");
        }
    }

    // ══════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════

    /**
     * Deletes an expense by id, scoped to userId.
     */
    public ExpenseResult deleteExpense(int userId, int expenseId) {
        try {
            Optional<Expense> existing = expenseDAO.findById(expenseId, userId);
            if (existing.isEmpty())
                return ExpenseResult.fail("Expense not found.");
            expenseDAO.delete(expenseId, userId);
            return ExpenseResult.ok(existing.get());
        } catch (SQLException e) {
            System.err.println("ExpenseService.deleteExpense() error: " + e.getMessage());
            return ExpenseResult.fail("Failed to delete expense. Please try again.");
        }
    }

    // ══════════════════════════════════════════════
    // VALIDATION
    // ══════════════════════════════════════════════

    /**
     * Validates all fields for add and update.
     * Returns an error message string, or null if valid.
     */
    private String validate(String description, double amount,
                            String type, String category, LocalDate date) {
        if (description == null || description.isBlank())
            return "Description is required.";
        if (description.trim().length() > 100)
            return "Description must be under 100 characters.";
        if (amount <= 0)
            return "Amount must be greater than zero.";
        if (amount > 10_000_000)
            return "Amount seems too large. Please check and try again.";
        if (!"Expense".equals(type) && !"Income".equals(type))
            return "Type must be either Expense or Income.";
        if (category == null || category.isBlank())
            return "Please select a category.";
        if (date == null)
            return "Please select a date.";
        if (date.isAfter(LocalDate.now()))
            return "Date cannot be in the future.";
        return null;
    }
}
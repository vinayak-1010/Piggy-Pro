package com.piggypro.service;

import com.piggypro.dao.BudgetDAO;
import com.piggypro.dao.BudgetDAO.Budget;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * BudgetService.java
 * ─────────────────────────────────────────────────────
 * Business logic for budget management.
 * Validates inputs, delegates to BudgetDAO.
 *
 * Usage:
 *   BudgetService svc = BudgetService.getInstance();
 *   svc.setBudget(userId, "Food and Dining", 6000, YearMonth.now());
 *   List<Budget> budgets = svc.getBudgetsForMonth(userId, YearMonth.now());
 */
public class BudgetService {

    private static BudgetService instance;
    private final BudgetDAO budgetDAO;

    private BudgetService() {
        this.budgetDAO = new BudgetDAO();
    }

    public static synchronized BudgetService getInstance() {
        if (instance == null) instance = new BudgetService();
        return instance;
    }

    // ── Result wrapper ─────────────────────────────
    public record BudgetResult(boolean success, String message, Budget budget) {
        public static BudgetResult ok(Budget b)     { return new BudgetResult(true,  "Success", b);  }
        public static BudgetResult fail(String msg) { return new BudgetResult(false, msg, null);      }
    }

    // ══════════════════════════════════════════════
    // SET (Insert or update)
    // ══════════════════════════════════════════════

    public BudgetResult setBudget(int userId, String category,
                                  double limit, YearMonth month) {
        if (category == null || category.isBlank())
            return BudgetResult.fail("Category is required.");
        if (limit <= 0)
            return BudgetResult.fail("Budget limit must be greater than zero.");
        if (month == null)
            return BudgetResult.fail("Month is required.");
        try {
            Budget b = budgetDAO.upsert(userId, category, limit, month);
            return BudgetResult.ok(b);
        } catch (SQLException e) {
            System.err.println("BudgetService.setBudget() error: " + e.getMessage());
            return BudgetResult.fail("Failed to save budget. Please try again.");
        }
    }

    // ══════════════════════════════════════════════
    // READ
    // ══════════════════════════════════════════════

    public List<Budget> getBudgetsForMonth(int userId, YearMonth month) {
        try {
            return budgetDAO.findByMonth(userId, month);
        } catch (SQLException e) {
            System.err.println("BudgetService.getBudgetsForMonth() error: " + e.getMessage());
            return List.of();
        }
    }

    public Optional<Budget> getBudget(int userId, String category, YearMonth month) {
        try {
            return budgetDAO.find(userId, category, month);
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    // ══════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════

    public BudgetResult deleteBudget(int budgetId, int userId) {
        try {
            budgetDAO.delete(budgetId, userId);
            return BudgetResult.ok(null);
        } catch (SQLException e) {
            System.err.println("BudgetService.deleteBudget() error: " + e.getMessage());
            return BudgetResult.fail("Failed to delete budget.");
        }
    }
}
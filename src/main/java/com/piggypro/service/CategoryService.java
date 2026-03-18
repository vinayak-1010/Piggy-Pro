package com.piggypro.service;

import com.piggypro.dao.CategoryDAO;
import com.piggypro.model.Category;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * CategoryService.java
 * ─────────────────────────────────────────────────────
 * Business logic layer for categories.
 * Validates inputs before passing them to CategoryDAO.
 *
 * Usage from any controller:
 *   CategoryService svc = CategoryService.getInstance();
 *   List<String> names = svc.getAllNames();
 *   svc.addCustomCategory("Travel", "#8B5CF6");
 */
public class CategoryService {

    // ── Singleton ──────────────────────────────────
    private static CategoryService instance;

    private final CategoryDAO categoryDAO;

    private CategoryService() {
        this.categoryDAO = new CategoryDAO();
    }

    public static synchronized CategoryService getInstance() {
        if (instance == null) instance = new CategoryService();
        return instance;
    }

    // ══════════════════════════════════════════════
    // READ
    // ══════════════════════════════════════════════

    /**
     * Returns all categories (default + custom).
     */
    public List<Category> getAll() {
        try {
            return categoryDAO.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load categories.", e);
        }
    }

    /**
     * Returns all category names as strings.
     * Used to populate ComboBox dropdowns.
     */
    public List<String> getAllNames() {
        try {
            return categoryDAO.findAllNames();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load category names.", e);
        }
    }

    /**
     * Finds a category by name. Returns empty if not found.
     */
    public Optional<Category> findByName(String name) {
        try {
            return categoryDAO.findByName(name);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find category.", e);
        }
    }

    // ══════════════════════════════════════════════
    // CREATE
    // ══════════════════════════════════════════════

    /**
     * Result wrapper for add/update operations.
     */
    public record ServiceResult(boolean success, String message, Category category) {
        public static ServiceResult ok(Category c)     { return new ServiceResult(true,  "Success", c);       }
        public static ServiceResult fail(String msg)   { return new ServiceResult(false, msg, null);           }
    }

    /**
     * Adds a new custom category with validation.
     *
     * @param name  category name (2–30 chars)
     * @param color hex color string e.g. "#FF5733"
     */
    public ServiceResult addCustomCategory(String name, String color) {
        if (name == null || name.isBlank())
            return ServiceResult.fail("Category name is required.");
        name = name.trim();
        if (name.length() < 2 || name.length() > 30)
            return ServiceResult.fail("Category name must be 2–30 characters.");
        if (color == null || !color.matches("^#[0-9A-Fa-f]{6}$"))
            return ServiceResult.fail("Please provide a valid hex color (e.g. #FF5733).");

        try {
            if (categoryDAO.nameExists(name))
                return ServiceResult.fail("Category '" + name + "' already exists.");

            Category cat = categoryDAO.insert(new Category(name, color));
            return ServiceResult.ok(cat);
        } catch (SQLException e) {
            return ServiceResult.fail("Failed to save category. Please try again.");
        }
    }

    // ══════════════════════════════════════════════
    // UPDATE
    // ══════════════════════════════════════════════

    /**
     * Updates an existing custom category.
     * Default categories cannot be modified.
     */
    public ServiceResult updateCategory(int id, String name, String color) {
        if (name == null || name.isBlank())
            return ServiceResult.fail("Category name is required.");
        name = name.trim();
        if (color == null || !color.matches("^#[0-9A-Fa-f]{6}$"))
            return ServiceResult.fail("Please provide a valid hex color.");

        try {
            Optional<Category> existing = categoryDAO.findById(id);
            if (existing.isEmpty())
                return ServiceResult.fail("Category not found.");
            if (existing.get().isDefault())
                return ServiceResult.fail("Default categories cannot be modified.");

            Category cat = existing.get();
            cat.setName(name);
            cat.setColor(color);
            categoryDAO.update(cat);
            return ServiceResult.ok(cat);
        } catch (SQLException e) {
            return ServiceResult.fail("Failed to update category.");
        }
    }

    // ══════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════

    /**
     * Deletes a custom category by id.
     * Default categories are protected.
     */
    public ServiceResult deleteCategory(int id) {
        try {
            Optional<Category> existing = categoryDAO.findById(id);
            if (existing.isEmpty())
                return ServiceResult.fail("Category not found.");
            if (existing.get().isDefault())
                return ServiceResult.fail("Default categories cannot be deleted.");

            categoryDAO.delete(id);
            return ServiceResult.ok(existing.get());
        } catch (SQLException e) {
            return ServiceResult.fail("Failed to delete category.");
        }
    }
}
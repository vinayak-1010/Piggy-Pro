package com.piggypro.model;

/**
 * Category.java
 * ─────────────────────────────────────────────────────
 * Model class representing a row in the 'categories'
 * table. Categories are shared across all users
 * (predefined defaults + user-added custom ones).
 *
 * Default categories are seeded in DBConnection.
 * Custom categories can be added by any user.
 */
public class Category {

    private int     id;
    private String  name;
    private String  color;       // Hex color e.g. "#F59E0B"
    private boolean isDefault;   // true = predefined, false = custom

    // ── Constructors ───────────────────────────────

    /** Full constructor — used when reading from DB. */
    public Category(int id, String name, String color, boolean isDefault) {
        this.id        = id;
        this.name      = name;
        this.color     = color;
        this.isDefault = isDefault;
    }

    /** Constructor for creating a new custom category. */
    public Category(String name, String color) {
        this(-1, name, color, false);
    }

    // ── Getters ────────────────────────────────────
    public int     getId()        { return id;        }
    public String  getName()      { return name;      }
    public String  getColor()     { return color;     }
    public boolean isDefault()    { return isDefault; }

    // ── Setters ────────────────────────────────────
    public void setId(int id)              { this.id        = id;        }
    public void setName(String name)       { this.name      = name;      }
    public void setColor(String color)     { this.color     = color;     }
    public void setDefault(boolean def)    { this.isDefault = def;       }

    @Override
    public String toString() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category c)) return false;
        return name != null && name.equalsIgnoreCase(c.name);
    }

    @Override
    public int hashCode() {
        return name == null ? 0 : name.toLowerCase().hashCode();
    }
}
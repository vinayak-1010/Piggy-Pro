package com.piggypro.model;

import java.time.LocalDate;

/**
 * Expense.java
 * ─────────────────────────────────────────────────────
 * Model class representing a single financial record
 * (either an Expense or an Income) stored in the
 * 'expenses' table.
 *
 * Used by ExpenseDAO, ExpenseService, and all
 * controllers that display or manipulate transactions.
 */
public class Expense {

    private int       id;
    private int       userId;
    private String    description;
    private double    amount;
    private String    type;        // "Expense" or "Income"
    private String    category;
    private LocalDate date;
    private String    note;
    private String    createdAt;

    // ── Constructors ───────────────────────────────

    /** Full constructor — used when reading from DB. */
    public Expense(int id, int userId, String description,
                   double amount, String type, String category,
                   LocalDate date, String note, String createdAt) {
        this.id          = id;
        this.userId      = userId;
        this.description = description;
        this.amount      = amount;
        this.type        = type;
        this.category    = category;
        this.date        = date;
        this.note        = note;
        this.createdAt   = createdAt;
    }

    /** Constructor without id/createdAt — used before DB insert. */
    public Expense(int userId, String description, double amount,
                   String type, String category,
                   LocalDate date, String note) {
        this(-1, userId, description, amount, type,
                category, date, note, null);
    }

    // ── Getters ────────────────────────────────────
    public int       getId()          { return id;          }
    public int       getUserId()      { return userId;      }
    public String    getDescription() { return description; }
    public double    getAmount()      { return amount;      }
    public String    getType()        { return type;        }
    public String    getCategory()    { return category;    }
    public LocalDate getDate()        { return date;        }
    public String    getNote()        { return note;        }
    public String    getCreatedAt()   { return createdAt;   }

    // ── Setters ────────────────────────────────────
    public void setId(int id)                      { this.id          = id;          }
    public void setUserId(int userId)              { this.userId      = userId;      }
    public void setDescription(String description) { this.description = description; }
    public void setAmount(double amount)           { this.amount      = amount;      }
    public void setType(String type)               { this.type        = type;        }
    public void setCategory(String category)       { this.category    = category;    }
    public void setDate(LocalDate date)            { this.date        = date;        }
    public void setNote(String note)               { this.note        = note;        }
    public void setCreatedAt(String createdAt)     { this.createdAt   = createdAt;   }

    // ── Utility ────────────────────────────────────

    /** Returns true if this record is an expense (not income). */
    public boolean isExpense() { return "Expense".equals(type); }

    /** Returns true if this record is income. */
    public boolean isIncome()  { return "Income".equals(type);  }

    /** Returns amount as negative for expenses, positive for income. */
    public double getSignedAmount() {
        return isExpense() ? -amount : amount;
    }

    @Override
    public String toString() {
        return "Expense{id=" + id + ", desc='" + description
                + "', amount=" + amount + ", type='" + type
                + "', category='" + category + "', date=" + date + "}";
    }
}
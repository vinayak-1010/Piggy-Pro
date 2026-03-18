package com.piggypro.model;

import java.time.LocalDateTime;

/**
 * User.java
 * ─────────────────────────────────────────────────────
 * Model class representing a row in the 'users' table.
 *
 * Used by:
 *   UserDAO     — to build objects from ResultSet rows
 *   AuthService — to return the authenticated user
 *   SessionManager — to store session data
 *
 * passwordHash is only populated when read from the DB
 * for authentication. It is never exposed to the UI.
 */
public class User {

    private int           id;
    private String        username;
    private String        fullName;
    private String        email;
    private String        passwordHash;   // BCrypt hash — never shown in UI
    private LocalDateTime createdAt;

    // ── Constructors ───────────────────────────────

    /** Full constructor — used when reading from DB. */
    public User(int id, String username, String fullName,
                String email, String passwordHash,
                LocalDateTime createdAt) {
        this.id           = id;
        this.username     = username;
        this.fullName     = fullName;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.createdAt    = createdAt;
    }

    /** Constructor without id — used when creating a new user (before DB insert). */
    public User(String username, String fullName,
                String email, String passwordHash) {
        this(-1, username, fullName, email, passwordHash, LocalDateTime.now());
    }

    // ── Getters ────────────────────────────────────
    public int           getId()           { return id;           }
    public String        getUsername()     { return username;     }
    public String        getFullName()     { return fullName;     }
    public String        getEmail()        { return email;        }
    public String        getPasswordHash() { return passwordHash; }
    public LocalDateTime getCreatedAt()    { return createdAt;    }

    // ── Setters ────────────────────────────────────
    public void setId(int id)                     { this.id = id;                 }
    public void setUsername(String username)       { this.username = username;     }
    public void setFullName(String fullName)       { this.fullName = fullName;     }
    public void setEmail(String email)             { this.email = email;           }
    public void setPasswordHash(String hash)       { this.passwordHash = hash;     }
    public void setCreatedAt(LocalDateTime time)   { this.createdAt = time;        }

    // ── Utility ────────────────────────────────────

    /**
     * Returns the user's initials from their full name.
     * e.g. "Vinayak Singh" → "VS"
     *      "Alice"         → "A"
     */
    public String getInitials() {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1)
            return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1)
                + parts[parts.length - 1].substring(0, 1))
                .toUpperCase();
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username
                + "', fullName='" + fullName + "', email='" + email + "'}";
    }
}

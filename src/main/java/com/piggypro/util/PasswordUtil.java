package com.piggypro.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * PasswordUtil.java
 * ─────────────────────────────────────────────────────
 * Utility class for BCrypt password hashing and verification.
 *
 * Uses jBCrypt library (org.mindrot:jbcrypt:0.4).
 * Make sure this dependency is in pom.xml:
 *
 *   <dependency>
 *       <groupId>org.mindrot</groupId>
 *       <artifactId>jbcrypt</artifactId>
 *       <version>0.4</version>
 *   </dependency>
 *
 * BCrypt work factor = 12 (2^12 = 4096 iterations).
 * Higher = more secure but slower. 12 is a good balance.
 *
 * Usage:
 *   String hash  = PasswordUtil.hash("myPassword123");
 *   boolean ok   = PasswordUtil.verify("myPassword123", hash);
 */
public class PasswordUtil {

    // BCrypt cost factor — increase for more security (range: 4–31)
    private static final int WORK_FACTOR = 12;

    // Private constructor — utility class, not instantiated
    private PasswordUtil() {}

    /**
     * Hashes a plain-text password using BCrypt.
     *
     * @param plainPassword the raw password entered by the user
     * @return a 60-character BCrypt hash string (safe to store in DB)
     * @throws IllegalArgumentException if password is null or empty
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank())
            throw new IllegalArgumentException("Password cannot be empty.");
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    /**
     * Verifies a plain-text password against a stored BCrypt hash.
     *
     * @param plainPassword the raw password to check
     * @param storedHash    the hash retrieved from the database
     * @return true if the password matches, false otherwise
     */
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) return false;
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (Exception e) {
            // Malformed hash — treat as mismatch
            return false;
        }
    }

    /**
     * Validates password strength before hashing.
     * Rules: min 8 chars, at least 1 uppercase, 1 digit, 1 special char.
     *
     * @param password the plain-text password to check
     * @return a human-readable error message, or null if valid
     */
    public static String validate(String password) {
        if (password == null || password.length() < 8)
            return "Password must be at least 8 characters.";
        if (!password.matches(".*[A-Z].*"))
            return "Password must contain at least one uppercase letter.";
        if (!password.matches(".*[0-9].*"))
            return "Password must contain at least one number.";
        if (!password.matches(".*[^a-zA-Z0-9].*"))
            return "Password must contain at least one special character.";
        return null; // valid
    }
}

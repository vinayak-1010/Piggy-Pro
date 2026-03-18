package com.piggypro.service;

import com.piggypro.dao.UserDAO;
import com.piggypro.model.User;
import com.piggypro.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Optional;

/**
 * AuthService.java
 * ─────────────────────────────────────────────────────
 * Business logic layer for user authentication.
 * Sits between the LoginController and UserDAO.
 *
 * Usage from LoginController:
 *
 *   AuthService auth = AuthService.getInstance();
 *
 *   // Login
 *   AuthResult result = auth.login("vinayak", "MyPass@123");
 *   if (result.isSuccess()) {
 *       SessionManager.login(result.getUser());
 *       SceneManager.navigateTo(Screen.DASHBOARD);
 *   } else {
 *       showError(result.getMessage());
 *   }
 *
 *   // Register
 *   AuthResult result = auth.register("vinayak", "Vinayak Singh",
 *                                     "v@email.com", "MyPass@123");
 */
public class AuthService {

    // ── Singleton ──────────────────────────────────
    private static AuthService instance;

    private final UserDAO userDAO;

    private AuthService() {
        this.userDAO = new UserDAO();
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    // ══════════════════════════════════════════════
    // RESULT WRAPPER
    // ══════════════════════════════════════════════

    /**
     * Returned by login() and register() to carry both
     * success/failure and a user-facing message.
     */
    public static class AuthResult {
        private final boolean success;
        private final String  message;
        private final User    user;

        private AuthResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user    = user;
        }

        public static AuthResult ok(User user) {
            return new AuthResult(true, "Success", user);
        }
        public static AuthResult fail(String message) {
            return new AuthResult(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String  getMessage(){ return message;  }
        public User    getUser()   { return user;     }
    }

    // ══════════════════════════════════════════════
    // LOGIN
    // ══════════════════════════════════════════════

    /**
     * Authenticates a user by username and password.
     *
     * Steps:
     *   1. Find user by username in DB
     *   2. Verify BCrypt password hash
     *   3. Return AuthResult with User on success
     *
     * @param username the username entered in the login form
     * @param password the plain-text password entered
     * @return AuthResult — check isSuccess() before using getUser()
     */
    public AuthResult login(String username, String password) {
        // Basic null / empty guard
        if (username == null || username.isBlank())
            return AuthResult.fail("Username is required.");
        if (password == null || password.isBlank())
            return AuthResult.fail("Password is required.");

        try {
            Optional<User> userOpt = userDAO.findByUsername(username.trim());

            if (userOpt.isEmpty())
                return AuthResult.fail("Incorrect username or password.");

            User user = userOpt.get();

            if (!PasswordUtil.verify(password, user.getPasswordHash()))
                return AuthResult.fail("Incorrect username or password.");

            return AuthResult.ok(user);

        } catch (SQLException e) {
            System.err.println("AuthService.login() DB error: " + e.getMessage());
            return AuthResult.fail("A database error occurred. Please try again.");
        }
    }

    // ══════════════════════════════════════════════
    // REGISTER
    // ══════════════════════════════════════════════

    /**
     * Registers a new user account.
     *
     * Validation order:
     *   1. Check all fields are non-empty
     *   2. Validate email format
     *   3. Validate password strength (via PasswordUtil)
     *   4. Check username not already taken
     *   5. Check email not already taken
     *   6. Hash password and insert into DB
     *
     * @param username  desired username
     * @param fullName  user's full name
     * @param email     user's email address
     * @param password  plain-text password (will be hashed)
     * @return AuthResult — check isSuccess() before using getUser()
     */
    public AuthResult register(String username, String fullName,
                               String email, String password) {
        // Field presence
        if (username == null || username.isBlank())
            return AuthResult.fail("Username is required.");
        if (fullName == null || fullName.isBlank())
            return AuthResult.fail("Full name is required.");
        if (email == null || email.isBlank())
            return AuthResult.fail("Email address is required.");
        if (password == null || password.isBlank())
            return AuthResult.fail("Password is required.");

        // Trim all inputs
        username = username.trim();
        fullName = fullName.trim();
        email    = email.trim();

        // Email format
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$"))
            return AuthResult.fail("Please enter a valid email address.");

        // Username format: 3–20 chars, alphanumeric + underscores
        if (!username.matches("^[a-zA-Z0-9_]{3,20}$"))
            return AuthResult.fail(
                    "Username must be 3–20 characters (letters, numbers, underscores only).");

        // Password strength
        String pwError = PasswordUtil.validate(password);
        if (pwError != null) return AuthResult.fail(pwError);

        try {
            // Duplicate checks
            if (userDAO.usernameExists(username))
                return AuthResult.fail("Username '" + username + "' is already taken.");
            if (userDAO.emailExists(email))
                return AuthResult.fail("An account with this email already exists.");

            // Hash password and insert
            String hash = PasswordUtil.hash(password);
            User newUser = new User(username, fullName, email, hash);
            userDAO.insert(newUser);   // id is set on newUser after insert

            return AuthResult.ok(newUser);

        } catch (SQLException e) {
            System.err.println("AuthService.register() DB error: " + e.getMessage());
            return AuthResult.fail("A database error occurred. Please try again.");
        }
    }

    // ══════════════════════════════════════════════
    // PASSWORD CHANGE
    // ══════════════════════════════════════════════

    /**
     * Changes the password for a logged-in user.
     * Verifies the current password before updating.
     *
     * @param userId          the logged-in user's id
     * @param currentPassword the user's current plain-text password
     * @param newPassword     the desired new plain-text password
     * @return AuthResult — success message or error
     */
    public AuthResult changePassword(int userId,
                                     String currentPassword,
                                     String newPassword) {
        try {
            Optional<User> userOpt = userDAO.findById(userId);
            if (userOpt.isEmpty())
                return AuthResult.fail("User not found.");

            User user = userOpt.get();

            if (!PasswordUtil.verify(currentPassword, user.getPasswordHash()))
                return AuthResult.fail("Current password is incorrect.");

            String pwError = PasswordUtil.validate(newPassword);
            if (pwError != null) return AuthResult.fail(pwError);

            userDAO.updatePassword(userId, PasswordUtil.hash(newPassword));
            return AuthResult.ok(user);

        } catch (SQLException e) {
            System.err.println("AuthService.changePassword() DB error: " + e.getMessage());
            return AuthResult.fail("A database error occurred.");
        }
    }
}

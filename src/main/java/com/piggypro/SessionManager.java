package com.piggypro;

/**
 * SessionManager.java
 * ─────────────────────────────────────────────────────
 * Simple static session store that holds the currently
 * logged-in user's data across all screens.
 *
 * Set by LoginController after successful authentication.
 * Read by every controller to get userId, username, etc.
 *
 * Usage:
 *   // On successful login (LoginController):
 *   SessionManager.login(user);
 *
 *   // In any controller:
 *   int    uid      = SessionManager.getUserId();
 *   String username = SessionManager.getUsername();
 *
 *   // On logout:
 *   SessionManager.logout();
 *   SceneManager.navigateTo(SceneManager.Screen.LOGIN);
 */
public class SessionManager {

    // ── Session data class ─────────────────────────
    /**
     * Holds all data about the currently logged-in user.
     * Populated from the DB row returned by UserDAO.
     *
     * Your teammate (Teammate 2) will define the full
     * User model in com.piggypro.model.User —
     * update this to match once their code is ready.
     */
    public static class UserSession {
        private final int    userId;
        private final String username;
        private final String fullName;
        private final String email;

        public UserSession(int userId, String username,
                           String fullName, String email) {
            this.userId   = userId;
            this.username = username;
            this.fullName = fullName;
            this.email    = email;
        }

        public int    getUserId()  { return userId;   }
        public String getUsername(){ return username;  }
        public String getFullName(){ return fullName;  }
        public String getEmail()   { return email;     }

        /** Returns initials e.g. "VS" for "Vinayak Singh" */
        public String getInitials() {
            if (fullName == null || fullName.isBlank()) return "?";
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length == 1)
                return parts[0].substring(0, 1).toUpperCase();
            return (parts[0].substring(0, 1)
                    + parts[parts.length - 1].substring(0, 1))
                    .toUpperCase();
        }
    }

    // ── Singleton session ──────────────────────────
    private static UserSession activeSession = null;

    // ── Login ──────────────────────────────────────

    /**
     * Call this after the user is authenticated.
     * Typically called from LoginController.handleLogin().
     */
    public static void login(int userId, String username,
                             String fullName, String email) {
        activeSession = new UserSession(userId, username, fullName, email);
    }

    /**
     * Convenience overload — pass a pre-built UserSession.
     * Use this once Teammate 2's User model is integrated.
     */
    public static void login(UserSession session) {
        activeSession = session;
    }

    // ── Logout ─────────────────────────────────────

    /** Clears the session. Always call before navigating back to login. */
    public static void logout() {
        activeSession = null;
    }

    // ── Accessors ──────────────────────────────────

    public static boolean isLoggedIn() {
        return activeSession != null;
    }

    public static UserSession getSession() {
        if (activeSession == null)
            throw new IllegalStateException(
                    "No active session. User is not logged in.");
        return activeSession;
    }

    public static int    getUserId()  { return getSession().getUserId();   }
    public static String getUsername(){ return getSession().getUsername();  }
    public static String getFullName(){ return getSession().getFullName();  }
    public static String getEmail()   { return getSession().getEmail();     }
    public static String getInitials(){ return getSession().getInitials();  }
}

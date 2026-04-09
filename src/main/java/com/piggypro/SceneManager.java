package com.piggypro;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Objects;

/**
 * SceneManager.java
 * ─────────────────────────────────────────────────────
 * Singleton utility that manages all screen transitions
 * for Piggy Pro.
 *
 * Usage from any controller:
 *   SceneManager.navigateTo(SceneManager.Screen.DASHBOARD);
 *
 * Every screen transition has a short fade so the switch
 * feels smooth rather than abrupt.
 *
 * FXML files are expected at:
 *   src/main/resources/com/piggypro/view/<name>.fxml
 *
 * Window sizes per screen:
 *   LOGIN       → 1020 × 660
 *   DASHBOARD   → 1100 × 680
 *   TRANSACTIONS→ 1100 × 680
 *   ANALYTICS   → 1100 × 680
 *   BUDGETS     → 1100 × 680
 *   REPORTS     → 1100 × 680
 */
public class SceneManager {

    // ── Screen enum ────────────────────────────────
    public enum Screen {
        LOGIN         ("LoginView.fxml",        1020, 660),
        DASHBOARD     ("DashboardView.fxml",    1100, 680),
        TRANSACTIONS  ("TransactionsView.fxml", 1100, 680),
        ANALYTICS     ("AnalyticsView.fxml",    1100, 680),
        BUDGETS       ("BudgetsView.fxml",      1100, 680),
        REPORTS       ("ReportsView.fxml",      1100, 680);

        final String fxml;
        final int    width;
        final int    height;

        Screen(String fxml, int width, int height) {
            this.fxml   = fxml;
            this.width  = width;
            this.height = height;
        }
    }

    // ── Singleton state ────────────────────────────
    private static Stage  stage;
    private static Screen current;

    /** Called once from App.start() before any navigation. */
    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    // ── Navigation ─────────────────────────────────

    /**
     * Navigate to a screen with a fade transition.
     * Safe to call from any thread — internally dispatched
     * on the JavaFX Application Thread.
     */
    public static void navigateTo(Screen screen) {
        if (stage == null)
            throw new IllegalStateException(
                    "SceneManager.init() must be called before navigateTo()");

        javafx.application.Platform.runLater(() -> switchTo(screen));
    }

    private static void switchTo(Screen screen) {
        try {
            // Load FXML
            java.net.URL fxmlUrl = SceneManager.class.getResource(
                    "/com/piggypro/view/" + screen.fxml);

            if (fxmlUrl == null) {
                System.err.println("FXML not found at: /com/piggypro/view/" + screen.fxml);
                System.err.println("Make sure the file exists in: src/main/resources/com/piggypro/view/");
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Navigation Error");
                alert.setHeaderText("Screen not found: " + screen.fxml);
                alert.setContentText(
                        "The file was not found at:\n" +
                                "src/main/resources/com/piggypro/view/" + screen.fxml +
                                "\n\nMake sure all FXML files are in the correct folder and run Maven Reload.");
                alert.showAndWait();
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            // Fade out current scene (if one exists)
            if (stage.getScene() != null) {
                Parent oldRoot = stage.getScene().getRoot();
                FadeTransition fadeOut = new FadeTransition(Duration.millis(120), oldRoot);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> applyScene(screen, root));
                fadeOut.play();
            } else {
                applyScene(screen, root);
            }

        } catch (IOException ex) {
            throw new RuntimeException(
                    "Failed to load FXML: " + screen.fxml, ex);
        }
    }

    private static void applyScene(Screen screen, Parent root) {
        // Fade in new root
        root.setOpacity(0);

        Scene scene = new Scene(root, screen.width, screen.height);
        stage.setScene(scene);
        stage.setWidth(screen.width);
        stage.setHeight(screen.height);
        stage.centerOnScreen();

        current = screen;

        FadeTransition fadeIn = new FadeTransition(Duration.millis(160), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    // ── Helpers ────────────────────────────────────

    /** Returns the currently displayed screen, or null before first navigation. */
    public static Screen getCurrent() { return current; }

    /** Returns the primary Stage. */
    public static Stage  getStage()   { return stage; }
}
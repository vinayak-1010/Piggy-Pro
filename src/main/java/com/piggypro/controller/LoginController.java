package com.piggypro.controller;
import com.piggypro.SceneManager;
import com.piggypro.service.AuthService;
import com.piggypro.SessionManager;


import javafx.animation.FadeTransition;

import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * LoginController
 * ─────────────────────────────────────────────
 * Handles all login/signup UI logic.
 * Icons are loaded as SVG PNG exports from resources/images/icons/
 *
 * Icon files needed in src/main/resources/com/piggypro/images/icons/:
 *   user.png       — person silhouette
 *   lock.png       — padlock
 *   mail.png       — envelope
 *   at.png         — @ symbol
 *   check.png      — checkmark
 *   eye.png        — eye (show password)
 *   eye-off.png    — eye with slash (hide password)
 *   arrow.png      — right arrow
 *   logo.png       — piggy bank mark
 *   chart.png      — line chart
 *   bell.png       — bell
 *   file.png       — document
 *
 * You can get these free from lucide.dev — download as PNG 16x16 or 24x24.
 */
public class LoginController implements Initializable {

    // Root
    @FXML private StackPane rootPane;

    // Theme toggle
    @FXML private ToggleButton themeToggle;

    // Tabs
    @FXML private ToggleButton loginTab, signupTab;

    // Forms
    @FXML private VBox loginForm, signupForm;

    // Login fields
    @FXML private TextField     loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private Button        loginBtn, togglePasswordBtn;
    @FXML private Label         loginError;

    // Signup fields
    @FXML private TextField     signupName, signupUsername, signupEmail;
    @FXML private PasswordField signupPassword, signupConfirm;
    @FXML private Button        signupBtn;
    @FXML private Label         signupError;

    // Strength bars
    @FXML private Region strengthBar1, strengthBar2, strengthBar3, strengthBar4;

    // Icon ImageViews — brand panel
    @FXML private ImageView logoIcon;
    @FXML private ImageView featureIcon1, featureIcon2, featureIcon3, featureIcon4;

    // Icon ImageViews — input fields
    @FXML private ImageView iconUser1, iconUser2;
    @FXML private ImageView iconLock1, iconLock2;
    @FXML private ImageView iconAt, iconMail, iconCheck;
    @FXML private ImageView eyeIcon;

    // Icon ImageViews — buttons
    @FXML private ImageView loginArrow, signupArrow;

    // State
    private boolean isDarkTheme  = false; // light by default
    private boolean showingLogin = true;
    private boolean passwordVisible = false;

    // ─────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadIcons();
        setupPasswordStrength();
    }

    // ══════════════════════════════════════════════════
    // ICON LOADING
    // ══════════════════════════════════════════════════

    /**
     * Loads all PNG icons from resources/images/icons/.
     * Download icons from https://lucide.dev as 24x24 PNG files.
     * Place them in: src/main/resources/com/piggypro/images/icons/
     */
    private void loadIcons() {
        // Brand panel icons
        setIcon(logoIcon,     "piggy-bank.png");
        setIcon(featureIcon1, "chart-bar-big.png");
        setIcon(featureIcon2, "bell.png");
        setIcon(featureIcon3, "file-text.png");
        setIcon(featureIcon4, "lock.png");

        // Input field icons
        setIcon(iconUser1, "user.png");
        setIcon(iconUser2, "user.png");
        setIcon(iconLock1, "lock.png");
        setIcon(iconLock2, "lock.png");
        setIcon(iconAt,       "at-sign.png");
        setIcon(iconMail,  "mail.png");
        setIcon(iconCheck, "check.png");
        setIcon(eyeIcon,   "eye.png");

        // Button arrow icons
        setIcon(loginArrow,   "arrow-right.png");
        setIcon(signupArrow,  "arrow-right.png");
    }

    /**
     * Safely loads an icon. If the file is missing, the ImageView
     * stays empty rather than crashing the app.
     */
    private void setIcon(ImageView view, String filename) {
        if (view == null) return;
        try {
            URL resource = getClass().getResource(
                    "/com/piggypro/images/icons/" + filename
            );
            if (resource != null) {
                view.setImage(new Image(resource.toExternalForm()));
            }
        } catch (Exception e) {
            // Icon missing — silently skip, layout still works
            System.out.println("Icon not found: " + filename);
        }
    }

    // ══════════════════════════════════════════════════
    // THEME TOGGLE
    // ══════════════════════════════════════════════════
    @FXML
    private void handleThemeToggle() {
        isDarkTheme = !isDarkTheme;
        if (isDarkTheme) {
            if (!rootPane.getStyleClass().contains("dark"))
                rootPane.getStyleClass().add("dark");
            themeToggle.setText("Dark mode");
        } else {
            rootPane.getStyleClass().remove("dark");
            themeToggle.setText("Light mode");
        }
    }

    // ══════════════════════════════════════════════════
    // TAB SWITCHING
    // ══════════════════════════════════════════════════
    @FXML
    private void showLogin() {
        if (showingLogin) return;
        showingLogin = true;
        crossFade(signupForm, loginForm);
        clearErrors();
    }

    @FXML
    private void showSignup() {
        if (!showingLogin) return;
        showingLogin = false;
        crossFade(loginForm, signupForm);
        clearErrors();
    }

    private void crossFade(VBox hide, VBox show) {
        FadeTransition out = new FadeTransition(Duration.millis(150), hide);
        out.setFromValue(1.0);
        out.setToValue(0.0);
        out.setOnFinished(e -> {
            hide.setVisible(false);
            hide.setManaged(false);
            show.setOpacity(0.0);
            show.setVisible(true);
            show.setManaged(true);
            FadeTransition in = new FadeTransition(Duration.millis(200), show);
            in.setFromValue(0.0);
            in.setToValue(1.0);
            in.play();
        });
        out.play();
    }

    // ══════════════════════════════════════════════════
    // LOGIN
    // ══════════════════════════════════════════════════
    @FXML
    private void handleLogin() {
        String username = loginUsername.getText().trim();
        String password = loginPassword.getText();
        loginError.setText("");

        if (username.isEmpty() || password.isEmpty()) {
            showError(loginError, "Please fill in all fields.");
            shakeNode(loginBtn);
            return;
        }

        AuthService.AuthResult result = AuthService.getInstance().login(username, password);

        if (result.isSuccess()) {
            animateSuccess(loginBtn, "Signed in successfully", () -> {
                com.piggypro.model.User u = result.getUser();
                SessionManager.login(u.getId(), u.getUsername(),
                        u.getFullName(), u.getEmail());
                SceneManager.navigateTo(SceneManager.Screen.DASHBOARD);
            });
        } else {
            showError(loginError, result.getMessage());
            shakeNode(loginBtn);
        }
    }

    // ══════════════════════════════════════════════════
    // SIGNUP
    // ══════════════════════════════════════════════════
    @FXML
    private void handleSignup() {
        String name     = signupName.getText().trim();
        String username = signupUsername.getText().trim();
        String email    = signupEmail.getText().trim();
        String password = signupPassword.getText();
        String confirm  = signupConfirm.getText();
        signupError.setText("");

        if (name.isEmpty() || username.isEmpty() || email.isEmpty()
                || password.isEmpty() || confirm.isEmpty()) {
            showError(signupError, "Please fill in all fields.");
            shakeNode(signupBtn);
            return;
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            showError(signupError, "Please enter a valid email address.");
            return;
        }
        if (password.length() < 8) {
            showError(signupError, "Password must be at least 8 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showError(signupError, "Passwords do not match.");
            shakeNode(signupBtn);
            return;
        }


        AuthService.AuthResult result = AuthService.getInstance()
                .register(name, username, email, password);

        if (result.isSuccess()) {
            animateSuccess(signupBtn, "Account created");
            PauseTransition pause = new PauseTransition(Duration.seconds(1.4));
            pause.setOnFinished(e -> showLogin());
            pause.play();
        } else {
            showError(signupError, result.getMessage());
            shakeNode(signupBtn);
        }
    }

    // ══════════════════════════════════════════════════
    // FORGOT PASSWORD
    // ══════════════════════════════════════════════════
    @FXML
    private void handleForgot() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reset Password");
        alert.setHeaderText(null);
        alert.setContentText("Password reset is not available in offline mode.\nPlease contact your system administrator.");
        alert.showAndWait();
    }

    // ══════════════════════════════════════════════════
    // PASSWORD VISIBILITY TOGGLE
    // ══════════════════════════════════════════════════
    @FXML
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        // Swap eye icon
        setIcon(eyeIcon, passwordVisible ? "eye-off.png" : "eye.png");
        // Note: To actually reveal text in a PasswordField, the standard
        // JavaFX approach is to swap it with a TextField in code.
        // This is wired up here as a hook — implement overlay swap if needed.
    }

    // ══════════════════════════════════════════════════
    // PASSWORD STRENGTH
    // ══════════════════════════════════════════════════
    private void setupPasswordStrength() {
        signupPassword.textProperty().addListener((obs, old, text) -> {
            int score = calcStrength(text);
            Region[] bars    = { strengthBar1, strengthBar2, strengthBar3, strengthBar4 };
            String[] classes = { "weak", "fair", "good", "strong" };
            for (int i = 0; i < 4; i++) {
                bars[i].getStyleClass().removeAll("weak", "fair", "good", "strong");
                if (i < score) {
                    bars[i].getStyleClass().add(classes[Math.min(score - 1, 3)]);
                }
            }
        });
    }

    private int calcStrength(String pw) {
        if (pw.isEmpty()) return 0;
        int score = 0;
        if (pw.length() >= 8)               score++;
        if (pw.matches(".*[A-Z].*"))        score++;
        if (pw.matches(".*[0-9].*"))        score++;
        if (pw.matches(".*[^a-zA-Z0-9].*")) score++;
        return score;
    }

    // ══════════════════════════════════════════════════
    // UTILITIES
    // ══════════════════════════════════════════════════
    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(55), node);
        shake.setFromX(0);
        shake.setByX(9);
        shake.setCycleCount(5);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> node.setTranslateX(0));
        shake.play();
    }

    private void animateSuccess(Button btn, String msg) {
        animateSuccess(btn, msg, null);
    }

    private void animateSuccess(Button btn, String msg, Runnable onComplete) {
        String original = btn.getText();
        btn.setText(msg);
        btn.setDisable(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        pause.setOnFinished(e -> {
            btn.setText(original);
            btn.setDisable(false);
            if (onComplete != null) onComplete.run();
        });
        pause.play();
    }

    private void showError(Label label, String message) {
        label.setText(message);
        FadeTransition ft = new FadeTransition(Duration.millis(250), label);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void clearErrors() {
        loginError.setText("");
        signupError.setText("");
    }


}
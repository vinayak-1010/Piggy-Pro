package com.piggypro.controller;

import com.piggypro.SceneManager;
import com.piggypro.SessionManager;
import com.piggypro.service.AuthService;
import com.piggypro.util.UserPopupUtil;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML private BorderPane settingsRoot;

    // Sidebar nav
    @FXML private Button navOverview, navTransactions, navAnalytics;
    @FXML private Button navBudgets, navReports, navSettings, navHelp, btnExport;
    @FXML private ImageView sidebarLogoIcon, iconOverview, iconTransactions;
    @FXML private ImageView iconAnalytics, iconBudgets, iconReports;
    @FXML private ImageView iconSettings, iconHelp, iconExport;

    // Topbar
    @FXML private Button     notifBtn;
    @FXML private Circle     notifDot;
    @FXML private Label      avatarInitials, userDisplayName;
    @FXML private ImageView  notifIcon, chevronIcon;

    // Profile section
    @FXML private TextField   fieldFullName, fieldUsername;
    @FXML private Button      btnSaveProfile;
    @FXML private Label       profileMsg;
    @FXML private ImageView   iconProfile, iconEmail;

    // Password section
    @FXML private PasswordField fieldCurrentPw, fieldNewPw, fieldConfirmPw;
    @FXML private Button        btnChangePassword;
    @FXML private Label         passwordMsg;
    @FXML private ImageView     iconLock, iconLock2, iconLock3;

    // Account section
    @FXML private Label       dbPathLabel;
    @FXML private ImageView   iconDb, iconLogout;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadIcons();
        populateProfile();
        if (SessionManager.isLoggedIn()) {
            userDisplayName.setText(SessionManager.getUsername());
            avatarInitials.setText(SessionManager.getInitials());
            dbPathLabel.setText(System.getProperty("user.home")
                    + "/PiggyPro/piggypro.db");
        }
    }

    private void loadIcons() {
        setIcon(sidebarLogoIcon,  "piggy-bank.png");
        setIcon(iconOverview,     "grid.png");
        setIcon(iconTransactions, "bookmark.png");
        setIcon(iconAnalytics,    "chart-bar-big.png");
        setIcon(iconBudgets,      "clock.png");
        setIcon(iconReports,      "file-text.png");
        setIcon(iconSettings,     "settings.png");
        setIcon(iconHelp,         "circle-question-mark.png");
        setIcon(iconExport,       "zap.png");
        setIcon(notifIcon,        "bell.png");
        setIcon(chevronIcon,      "chevron-down.png");
        setIcon(iconProfile,      "user.png");
        setIcon(iconEmail,        "at-sign.png");
        setIcon(iconLock,         "lock.png");
        setIcon(iconLock2,        "lock.png");
        setIcon(iconLock3,        "check.png");
        setIcon(iconDb,           "database.png");
        setIcon(iconLogout,       "log-out.png");
    }

    private void setIcon(ImageView view, String filename) {
        if (view == null) return;
        try {
            URL res = getClass().getResource("/com/piggypro/images/icons/" + filename);
            if (res != null) view.setImage(new Image(res.toExternalForm()));
        } catch (Exception e) { System.out.println("Icon not found: " + filename); }
    }

    private void populateProfile() {
        if (!SessionManager.isLoggedIn()) return;
        fieldFullName.setText(SessionManager.getFullName());
        fieldUsername.setText(SessionManager.getUsername());
    }

    // ── Save profile ───────────────────────────────
    @FXML
    private void handleSaveProfile() {
        profileMsg.setText("");
        String newName = fieldFullName.getText().trim();
        if (newName.isBlank()) {
            profileMsg.setStyle("-fx-text-fill:#EF4444;");
            profileMsg.setText("Full name cannot be empty.");
            return;
        }
        // Update in DB via UserDAO directly (simple update)
        try {
            com.piggypro.dao.UserDAO dao = new com.piggypro.dao.UserDAO();
            com.piggypro.model.User user = dao.findById(SessionManager.getUserId())
                    .orElse(null);
            if (user != null) {
                user.setFullName(newName);
                dao.update(user);
                // Refresh session
                SessionManager.login(user);
                avatarInitials.setText(SessionManager.getInitials());
                userDisplayName.setText(SessionManager.getUsername());
                showSuccess(btnSaveProfile, profileMsg, "Profile updated!");
            }
        } catch (Exception e) {
            profileMsg.setStyle("-fx-text-fill:#EF4444;");
            profileMsg.setText("Failed to update profile.");
        }
    }

    // ── Change password ────────────────────────────
    @FXML
    private void handleChangePassword() {
        passwordMsg.setText("");
        String current = fieldCurrentPw.getText();
        String newPw   = fieldNewPw.getText();
        String confirm = fieldConfirmPw.getText();

        if (current.isBlank() || newPw.isBlank() || confirm.isBlank()) {
            showError(passwordMsg, "All password fields are required.");
            return;
        }
        if (!newPw.equals(confirm)) {
            showError(passwordMsg, "New passwords do not match.");
            return;
        }

        AuthService.AuthResult result = AuthService.getInstance()
                .changePassword(SessionManager.getUserId(), current, newPw);

        if (result.isSuccess()) {
            fieldCurrentPw.clear();
            fieldNewPw.clear();
            fieldConfirmPw.clear();
            showSuccess(btnChangePassword, passwordMsg, "Password updated!");
        } else {
            showError(passwordMsg, result.getMessage());
        }
    }

    // ── Logout ─────────────────────────────────────
    @FXML
    private void handleLogout() {
        SessionManager.logout();
        SceneManager.navigateTo(SceneManager.Screen.LOGIN);
    }

    // ── User chip dropdown ─────────────────────────
    @FXML
    private void handleUserChip(javafx.scene.input.MouseEvent e) {
        UserPopupUtil.show((javafx.scene.Node) e.getSource(),
                settingsRoot.getScene().getWindow());
    }

    @FXML private void handleNotifications() { notifDot.setVisible(false); }
    @FXML private void handleExport() { SceneManager.navigateTo(SceneManager.Screen.REPORTS); }

    // ── Nav ────────────────────────────────────────
    @FXML private void handleNavOverview()     { SceneManager.navigateTo(SceneManager.Screen.DASHBOARD); }
    @FXML private void handleNavTransactions() { SceneManager.navigateTo(SceneManager.Screen.TRANSACTIONS); }
    @FXML private void handleNavAnalytics()    { SceneManager.navigateTo(SceneManager.Screen.ANALYTICS); }
    @FXML private void handleNavBudgets()      { SceneManager.navigateTo(SceneManager.Screen.BUDGETS); }
    @FXML private void handleNavReports()      { SceneManager.navigateTo(SceneManager.Screen.REPORTS); }
    @FXML private void handleNavSettings()     { /* already here */ }
    @FXML private void handleNavHelp()         { SceneManager.navigateTo(SceneManager.Screen.HELP); }

    // ── Helpers ────────────────────────────────────
    private void showError(Label label, String msg) {
        label.setStyle("-fx-text-fill:#EF4444;");
        label.setText(msg);
    }

    private void showSuccess(Button btn, Label label, String msg) {
        label.setStyle("-fx-text-fill:#10B981;");
        label.setText(msg);
        String orig = btn.getText();
        btn.setDisable(true);
        PauseTransition p = new PauseTransition(Duration.seconds(2));
        p.setOnFinished(e -> {
            btn.setDisable(false);
            label.setText("");
        });
        p.play();
    }
}
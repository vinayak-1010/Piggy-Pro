package com.piggypro.util;

import com.piggypro.SceneManager;
import com.piggypro.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.net.URL;

/**
 * UserPopupUtil.java
 * ─────────────────────────────────────────────────────
 * Shared utility that shows a user dropdown popup
 * anchored to the topbar user chip. Used by ALL
 * controller screens.
 *
 * Usage in any controller:
 *   UserPopupUtil.show(userChipNode, stage);
 */
public class UserPopupUtil {

    private static Popup activePopup;

    public static void show(Node anchor, Window window) {
        // Close existing popup if already open
        if (activePopup != null && activePopup.isShowing()) {
            activePopup.hide();
            activePopup = null;
            return;
        }

        VBox card = buildCard();
        activePopup = new Popup();
        activePopup.setAutoHide(true);
        activePopup.setHideOnEscape(true);
        activePopup.getContent().add(card);

        // Position below the anchor node
        Point2D screen = anchor.localToScreen(0, anchor.getBoundsInLocal().getHeight() + 6);
        if (screen != null) {
            // Align right edge of popup with right edge of anchor
            double popupWidth = 240;
            double anchorRight = anchor.localToScreen(
                    anchor.getBoundsInLocal().getWidth(), 0).getX();
            activePopup.show(window, anchorRight - popupWidth, screen.getY());
        }
    }

    private static VBox buildCard() {
        VBox card = new VBox(0);
        card.getStyleClass().add("user-popup-card");
        card.setPrefWidth(240);
        card.setMinWidth(240);

        // ── User info header ──────────────────────
        VBox header = new VBox(3);
        header.setPadding(new Insets(16, 18, 12, 18));
        header.setStyle("-fx-border-color: transparent transparent #F1F4FB transparent;"
                + "-fx-border-width: 0 0 1 0;");

        HBox avatarRow = new HBox(10);
        avatarRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.setMinSize(40, 40);
        avatar.setPrefSize(40, 40);
        avatar.setStyle("-fx-background-color:#2563EB;-fx-background-radius:12px;");
        Label initials = new Label(SessionManager.isLoggedIn()
                ? SessionManager.getInitials() : "?");
        initials.setStyle("-fx-font-family:'Plus Jakarta Sans';-fx-font-size:15px;"
                + "-fx-font-weight:700;-fx-text-fill:white;");
        avatar.getChildren().add(initials);

        VBox nameBox = new VBox(2);
        Label name = new Label(SessionManager.isLoggedIn()
                ? SessionManager.getFullName() : "Guest");
        name.getStyleClass().add("popup-user-name");
        Label email = new Label(SessionManager.isLoggedIn()
                ? "@" + SessionManager.getUsername() : "");
        email.getStyleClass().add("popup-user-email");
        nameBox.getChildren().addAll(name, email);

        avatarRow.getChildren().addAll(avatar, nameBox);
        header.getChildren().add(avatarRow);
        card.getChildren().add(header);

        // ── Menu items ────────────────────────────
        card.getChildren().add(menuItem("user.png",     "My Profile",
                () -> navigate(SceneManager.Screen.SETTINGS)));
        card.getChildren().add(menuItem("settings.png", "Settings",
                () -> navigate(SceneManager.Screen.SETTINGS)));
        card.getChildren().add(menuItem("help-circle.png", "Help Center",
                () -> navigate(SceneManager.Screen.HELP)));

        // ── Separator ─────────────────────────────
        Region sep = new Region();
        sep.getStyleClass().add("popup-menu-sep");
        sep.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(sep, new Insets(4, 0, 4, 0));
        card.getChildren().add(sep);

        // ── Logout ────────────────────────────────
        Button logout = new Button("Sign Out");
        logout.getStyleClass().add("popup-logout-btn");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setGraphic(icon("log-out.png"));
        logout.setOnAction(e -> {
            if (activePopup != null) activePopup.hide();
            SessionManager.logout();
            SceneManager.navigateTo(SceneManager.Screen.LOGIN);
        });
        card.getChildren().add(logout);

        return card;
    }

    private static Button menuItem(String iconFile, String label, Runnable action) {
        Button btn = new Button(label);
        btn.getStyleClass().add("popup-menu-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setGraphic(icon(iconFile));
        btn.setOnAction(e -> {
            if (activePopup != null) activePopup.hide();
            action.run();
        });
        return btn;
    }

    private static ImageView icon(String filename) {
        ImageView iv = new ImageView();
        iv.setFitWidth(14); iv.setFitHeight(14); iv.setPreserveRatio(true);
        try {
            URL res = UserPopupUtil.class.getResource(
                    "/com/piggypro/images/icons/" + filename);
            if (res != null) iv.setImage(new Image(res.toExternalForm()));
        } catch (Exception ignored) {}
        return iv;
    }

    private static void navigate(SceneManager.Screen screen) {
        SceneManager.navigateTo(screen);
    }
}

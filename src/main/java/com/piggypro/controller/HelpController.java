package com.piggypro.controller;

import com.piggypro.SceneManager;
import com.piggypro.SessionManager;
import com.piggypro.util.UserPopupUtil;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class HelpController implements Initializable {

    // ── FAQ data model ─────────────────────────────
    private record FAQ(String category, String question, String answer) {}

    // ── FXML fields ────────────────────────────────
    @FXML private BorderPane helpRoot;
    @FXML private Button navOverview, navTransactions, navAnalytics;
    @FXML private Button navBudgets, navReports, navSettings, navHelp, btnExport;
    @FXML private ImageView sidebarLogoIcon, iconOverview, iconTransactions;
    @FXML private ImageView iconAnalytics, iconBudgets, iconReports;
    @FXML private ImageView iconSettings, iconHelp, iconExport;
    @FXML private Button     notifBtn;
    @FXML private Circle     notifDot;
    @FXML private Label      avatarInitials, userDisplayName;
    @FXML private ImageView  notifIcon, chevronIcon, searchIcon;
    @FXML private TextField  searchField;
    @FXML private VBox       faqContainer;
    @FXML private Button     btnAll, btnGettingStarted, btnTransactions;
    @FXML private Button     btnBudgets, btnReports, btnAccount;

    private String activeCategory = "All";
    private final List<FAQ> allFaqs = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadIcons();
        populateFAQs();
        buildFAQList(allFaqs);
        if (SessionManager.isLoggedIn()) {
            userDisplayName.setText(SessionManager.getUsername());
            avatarInitials.setText(SessionManager.getInitials());
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
        setIcon(searchIcon,       "search.png");
    }

    private void setIcon(ImageView view, String filename) {
        if (view == null) return;
        try {
            URL res = getClass().getResource("/com/piggypro/images/icons/" + filename);
            if (res != null) view.setImage(new Image(res.toExternalForm()));
        } catch (Exception e) { System.out.println("Icon not found: " + filename); }
    }

    // ══════════════════════════════════════════════
    // FAQ DATA
    // ══════════════════════════════════════════════
    private void populateFAQs() {
        allFaqs.addAll(List.of(
                new FAQ("Getting Started", "How do I create an account?",
                        "Click 'Sign Up' on the login screen. Enter your full name, a unique username, " +
                                "your email address, and a strong password (min 8 characters with uppercase, number, " +
                                "and special character). Click 'Create Account' and you will be logged in automatically."),

                new FAQ("Getting Started", "How do I log in?",
                        "On the login screen, enter your username and password, then click 'Sign In'. " +
                                "If you forget your password, use the 'Forgot password?' link. " +
                                "All data is stored locally — no internet connection is required."),

                new FAQ("Getting Started", "Is my data safe?",
                        "Yes. Piggy Pro stores all data locally on your device at ~/PiggyPro/piggypro.db. " +
                                "Your password is never stored in plain text — it is hashed using BCrypt with a " +
                                "work factor of 12. No data is sent to any server."),

                new FAQ("Transactions", "How do I add a transaction?",
                        "Go to the Transactions screen and click 'Add Transaction'. Fill in the description, " +
                                "amount, type (Expense or Income), category, date, and an optional note. " +
                                "Click 'Save' to store it in the database."),

                new FAQ("Transactions", "How do I edit or delete a transaction?",
                        "In the Transactions screen, each row in the table has an edit (pencil) and delete " +
                                "(trash) button on the right. Click edit to open the form pre-filled with that " +
                                "transaction's data. Click delete to permanently remove it."),

                new FAQ("Transactions", "How do I filter transactions?",
                        "Use the filter bar above the transaction table. You can filter by category, type " +
                                "(Expense/Income), date range, and amount range. The summary chips at the top " +
                                "update automatically to reflect the filtered results."),

                new FAQ("Budgets", "How do I set a budget?",
                        "Go to the Budgets screen and click 'Set Budget'. Select a category, enter a monthly " +
                                "limit amount, choose the month, and click Save. The budget card will appear showing " +
                                "your real spending versus the limit."),

                new FAQ("Budgets", "What does the red badge mean?",
                        "A red 'Over by Rs. X' badge appears on a budget card when your actual spending " +
                                "in that category has exceeded the limit you set. The progress bar also turns red " +
                                "and fills completely to indicate the overage."),

                new FAQ("Budgets", "Can I set budgets for past months?",
                        "Yes. When setting a budget, the month dropdown includes current month ±5 months. " +
                                "Use the prev/next arrows on the Budgets screen to navigate to any month and view " +
                                "or set budgets for that period."),

                new FAQ("Reports", "How do I generate a report?",
                        "Go to the Reports screen, select the report type (By Category, Monthly, or All " +
                                "Transactions), choose a date range, and click 'Generate'. The table updates with " +
                                "live data from your database for that period."),

                new FAQ("Reports", "What formats can I export reports in?",
                        "Piggy Pro supports PDF export (formatted with tables and a summary section) and " +
                                "Excel (.xlsx) export (raw data with styled headers). Select your format in the " +
                                "Export panel on the right of the Reports screen and click Download Report."),

                new FAQ("Reports", "Where are exported files saved?",
                        "All exported files are saved to ~/PiggyPro/exports/ on your device. " +
                                "The folder is created automatically on first export. " +
                                "You can find recent exports listed in the 'Recent Exports' panel."),

                new FAQ("Account", "How do I change my password?",
                        "Go to Settings → Change Password. Enter your current password, then your new " +
                                "password twice. Your new password must be at least 8 characters with one uppercase " +
                                "letter, one number, and one special character."),

                new FAQ("Account", "How do I update my profile name?",
                        "Go to Settings → Profile Information. Edit your full name and click 'Save Changes'. " +
                                "Your username cannot be changed as it is used to identify your account."),

                new FAQ("Account", "How do I log out?",
                        "Click your name in the top-right corner to open the user menu, then click " +
                                "'Sign Out'. You can also go to Settings → Account → Sign Out. " +
                                "Your data is saved automatically — nothing is lost on logout.")
        ));
    }

    // ══════════════════════════════════════════════
    // BUILD FAQ ACCORDION
    // ══════════════════════════════════════════════
    private void buildFAQList(List<FAQ> faqs) {
        faqContainer.getChildren().clear();
        if (faqs.isEmpty()) {
            Label empty = new Label("No results found. Try a different search term.");
            empty.setStyle("-fx-font-family:'DM Sans';-fx-font-size:13px;-fx-text-fill:#94A3B8;");
            faqContainer.getChildren().add(empty);
            return;
        }
        for (FAQ faq : faqs) {
            faqContainer.getChildren().add(buildFAQCard(faq));
        }
    }

    private VBox buildFAQCard(FAQ faq) {
        VBox card = new VBox(0);
        card.getStyleClass().add("faq-card");

        // Question button (acts as toggle)
        Button questionBtn = new Button();
        questionBtn.setMaxWidth(Double.MAX_VALUE);
        questionBtn.getStyleClass().add("faq-question");

        HBox qRow = new HBox(10);
        qRow.setAlignment(Pos.CENTER_LEFT);
        Label qLabel = new Label(faq.question());
        qLabel.setStyle("-fx-font-family:'Plus Jakarta Sans';-fx-font-size:13px;"
                + "-fx-font-weight:700;-fx-text-fill:#0F172A;-fx-wrap-text:true;");
        HBox.setHgrow(qLabel, Priority.ALWAYS);
        Label chevron = new Label("▼");
        chevron.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:10px;");
        qRow.getChildren().addAll(qLabel, chevron);
        questionBtn.setGraphic(qRow);

        // Answer label (hidden by default)
        Label answerLabel = new Label(faq.answer());
        answerLabel.getStyleClass().add("faq-answer");
        answerLabel.setWrapText(true);
        answerLabel.setMaxWidth(Double.MAX_VALUE);
        answerLabel.setVisible(false);
        answerLabel.setManaged(false);

        // Toggle on click
        questionBtn.setOnAction(e -> {
            boolean showing = answerLabel.isVisible();
            answerLabel.setVisible(!showing);
            answerLabel.setManaged(!showing);
            chevron.setText(showing ? "▼" : "▲");
            if (!showing) {
                FadeTransition ft = new FadeTransition(Duration.millis(150), answerLabel);
                ft.setFromValue(0); ft.setToValue(1); ft.play();
            }
        });

        card.getChildren().addAll(questionBtn, answerLabel);
        return card;
    }

    // ══════════════════════════════════════════════
    // FILTER + SEARCH HANDLERS
    // ══════════════════════════════════════════════
    @FXML private void handleFilterAll()             { setFilter("All"); }
    @FXML private void handleFilterGettingStarted()  { setFilter("Getting Started"); }
    @FXML private void handleFilterTransactions()    { setFilter("Transactions"); }
    @FXML private void handleFilterBudgets()         { setFilter("Budgets"); }
    @FXML private void handleFilterReports()         { setFilter("Reports"); }
    @FXML private void handleFilterAccount()         { setFilter("Account"); }

    private void setFilter(String cat) {
        activeCategory = cat;
        List<Button> btns = List.of(btnAll, btnGettingStarted, btnTransactions,
                btnBudgets, btnReports, btnAccount);
        btns.forEach(b -> b.getStyleClass().remove("active"));
        Button active = switch (cat) {
            case "Getting Started" -> btnGettingStarted;
            case "Transactions"    -> btnTransactions;
            case "Budgets"         -> btnBudgets;
            case "Reports"         -> btnReports;
            case "Account"         -> btnAccount;
            default                -> btnAll;
        };
        active.getStyleClass().add("active");
        applyFilter();
    }

    @FXML
    private void handleSearch() {
        applyFilter();
    }

    private void applyFilter() {
        String query = searchField.getText().toLowerCase().trim();
        List<FAQ> filtered = allFaqs.stream()
                .filter(f -> activeCategory.equals("All") || f.category().equals(activeCategory))
                .filter(f -> query.isBlank()
                        || f.question().toLowerCase().contains(query)
                        || f.answer().toLowerCase().contains(query))
                .toList();
        buildFAQList(filtered);
    }

    // ══════════════════════════════════════════════
    // CONTACT / DOCS
    // ══════════════════════════════════════════════
    @FXML
    private void handleEmailSupport() {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        a.setTitle("Email Support");
        a.setHeaderText(null);
        a.setContentText("For support, please contact:\nteam.techtonics@example.com\n\n"
                + "Include your username and a description of the issue.");
        a.showAndWait();
    }

    @FXML
    private void handleViewDocs() {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        a.setTitle("Documentation");
        a.setHeaderText(null);
        a.setContentText("Piggy Pro v1.0 — Team Tech Tonics (T-131)\n"
                + "Graphic Era University\n\n"
                + "All documentation is available in the project report submitted with this application.");
        a.showAndWait();
    }

    // ══════════════════════════════════════════════
    // NAV + MISC
    // ══════════════════════════════════════════════
    @FXML private void handleUserChip(javafx.scene.input.MouseEvent e) {
        UserPopupUtil.show((javafx.scene.Node) e.getSource(),
                helpRoot.getScene().getWindow());
    }
    @FXML private void handleNotifications() { notifDot.setVisible(false); }
    @FXML private void handleExport()        { SceneManager.navigateTo(SceneManager.Screen.REPORTS); }
    @FXML private void handleNavOverview()     { SceneManager.navigateTo(SceneManager.Screen.DASHBOARD); }
    @FXML private void handleNavTransactions() { SceneManager.navigateTo(SceneManager.Screen.TRANSACTIONS); }
    @FXML private void handleNavAnalytics()    { SceneManager.navigateTo(SceneManager.Screen.ANALYTICS); }
    @FXML private void handleNavBudgets()      { SceneManager.navigateTo(SceneManager.Screen.BUDGETS); }
    @FXML private void handleNavReports()      { SceneManager.navigateTo(SceneManager.Screen.REPORTS); }
    @FXML private void handleNavSettings()     { SceneManager.navigateTo(SceneManager.Screen.SETTINGS); }
    @FXML private void handleNavHelp()         { /* already here */ }
}
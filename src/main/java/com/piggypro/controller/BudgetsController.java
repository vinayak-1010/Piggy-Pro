package com.piggypro.controller;

import com.piggypro.SceneManager;
import com.piggypro.util.UserPopupUtil;
import com.piggypro.SessionManager;
import com.piggypro.service.ExpenseService;
import com.piggypro.service.BudgetService;
import com.piggypro.dao.BudgetDAO;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * BudgetsController
 * ─────────────────────────────────────────────────────
 * Drives BudgetsView.fxml.
 * Budget cards are built programmatically so each card
 * can show dynamic data (spent vs limit, progress bar,
 * over-budget badge) without needing separate FXMLs.
 *
 * Required icons (lucide.dev 24x24 PNG):
 *   logo.png, grid.png, bookmark.png, bar-chart.png,
 *   clock.png, file-text.png, settings.png,
 *   help-circle.png, zap.png, search.png, bell.png,
 *   chevron-down.png, chevron-left.png, chevron-right.png,
 *   plus.png, x.png, edit.png, trash.png,
 *   alert-triangle.png   (over-budget icon)
 *   send.png, shopping-bag.png, map-pin.png,
 *   phone.png, monitor.png, home.png, tag.png
 */
public class BudgetsController implements Initializable {

    // ── Budget model ───────────────────────────────
    public static class Budget {
        String   id;
        String   category;
        double   limit;
        double   spent;
        YearMonth month;

        public Budget(String id, String category, double limit,
                      double spent, YearMonth month) {
            this.id = id; this.category = category;
            this.limit = limit; this.spent = spent; this.month = month;
        }

        public double pct()       { return limit > 0 ? Math.min(spent / limit, 1.0) : 0; }
        public boolean isOver()   { return spent > limit; }
        public double remaining() { return limit - spent; }
    }

    // ── Root ───────────────────────────────────────
    @FXML private BorderPane budgetsRoot;

    // ── Sidebar ────────────────────────────────────
    @FXML private Button navOverview, navTransactions, navAnalytics;
    @FXML private Button navBudgets, navReports, navSettings, navHelp;
    @FXML private Button btnExport;
    @FXML private ImageView sidebarLogoIcon;
    @FXML private ImageView iconOverview, iconTransactions, iconAnalytics;
    @FXML private ImageView iconBudgets, iconReports, iconSettings, iconHelp;
    @FXML private ImageView iconExport;

    // ── Topbar ─────────────────────────────────────
    @FXML private TextField  searchField;
    @FXML private Button     notifBtn;
    @FXML private Circle     notifDot;
    @FXML private Label      avatarInitials, userDisplayName;
    @FXML private ImageView  searchIcon, notifIcon, chevronIcon;

    // ── Page header ────────────────────────────────
    @FXML private Button    setBudgetBtn;
    @FXML private ImageView addIcon;

    // ── Month selector ─────────────────────────────
    @FXML private Label     monthNameLabel;
    @FXML private Label     summaryTotalBudget, summaryTotalSpent, summaryRemaining;
    @FXML private ImageView prevMonthIcon, nextMonthIcon;

    // ── Overall card ───────────────────────────────
    @FXML private Label  overallSpentLabel, overallLimitLabel, overallSubLabel;
    @FXML private Label  daysLeftLabel;
    @FXML private Region overallFill;

    // ── Form ───────────────────────────────────────
    @FXML private VBox     budgetFormCard;
    @FXML private Label    formHeadingLabel, formError;
    @FXML private Button   btnSave;
    @FXML private ComboBox<String> fieldCategory, fieldMonth;
    @FXML private TextField        fieldLimit;
    @FXML private ImageView        formHeaderIcon, closeIcon;

    // ── Budget grid container ──────────────────────
    @FXML private VBox budgetGridContainer;

    // ── State ──────────────────────────────────────
    private final List<Budget> allBudgets = new ArrayList<>();
    private YearMonth displayMonth = YearMonth.now();
    private boolean editMode = false;
    private Budget  editingBudget = null;

    private static final int COLS = 3;
    private static final DateTimeFormatter MONTH_FMT =
            DateTimeFormatter.ofPattern("MMMM yyyy");

    private static final List<String> CATEGORIES = List.of(
            "Food and Dining", "Shopping", "Transport",
            "Utilities", "Entertainment", "Rent", "Other"
    );

    // Category → accent color hex
    private static final Map<String, String> CAT_COLOR = new LinkedHashMap<>() {{
        put("Food and Dining", "#F59E0B");
        put("Shopping",        "#2563EB");
        put("Transport",       "#8B5CF6");
        put("Utilities",       "#EF4444");
        put("Entertainment",   "#EC4899");
        put("Rent",            "#2563EB");
        put("Other",           "#64748B");
    }};

    // Category → icon filename
    private static final Map<String, String> CAT_ICON = new LinkedHashMap<>() {{
        put("Food and Dining", "send.png");
        put("Shopping",        "shopping-bag.png");
        put("Transport",       "map-pin.png");
        put("Utilities",       "phone.png");
        put("Entertainment",   "monitor.png");
        put("Rent",            "home.png");
        put("Other",           "tag.png");
    }};

    // ──────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadIcons();
        setupFormCombos();
        if (SessionManager.isLoggedIn()) {
            userDisplayName.setText(SessionManager.getUsername());
            avatarInitials.setText(SessionManager.getInitials());
        }
        refreshView();
    }

    // Load budgets from DB and populate real spent amounts
    private void loadBudgetsFromDB() {
        if (!SessionManager.isLoggedIn()) return;
        int userId = SessionManager.getUserId();
        allBudgets.clear();
        List<BudgetDAO.Budget> dbBudgets =
                BudgetService.getInstance().getBudgetsForMonth(userId, displayMonth);
        for (BudgetDAO.Budget db : dbBudgets) {
            double spent = ExpenseService.getInstance()
                    .getSpentInCategory(userId, db.category,
                            YearMonth.parse(db.month));
            allBudgets.add(new Budget(
                    String.valueOf(db.id), db.category, db.limit, spent,
                    YearMonth.parse(db.month)));
        }
    }

    // ══════════════════════════════════════════════
    // ICONS
    // ══════════════════════════════════════════════
    private void loadIcons() {
        setIcon(sidebarLogoIcon,  "piggy-bank.png");
        setIcon(iconOverview,     "grid.png");
        setIcon(iconTransactions, "bookmark.png");
        setIcon(iconAnalytics,    "bar-chart.png");
        setIcon(iconBudgets,      "clock.png");
        setIcon(iconReports,      "file-text.png");
        setIcon(iconSettings,     "settings.png");
        setIcon(iconHelp,         "help-circle.png");
        setIcon(iconExport,       "zap.png");
        setIcon(searchIcon,       "search.png");
        setIcon(notifIcon,        "bell.png");
        setIcon(chevronIcon,      "chevron-down.png");
        setIcon(prevMonthIcon,    "chevron-left.png");
        setIcon(nextMonthIcon,    "chevron-right.png");
        setIcon(addIcon,          "plus.png");
        setIcon(formHeaderIcon,   "plus.png");
        setIcon(closeIcon,        "x.png");
    }

    private void setIcon(ImageView view, String filename) {
        if (view == null) return;
        try {
            URL res = getClass().getResource(
                    "/com/piggypro/images/icons/" + filename);
            if (res != null) view.setImage(new Image(res.toExternalForm()));
        } catch (Exception e) {
            System.out.println("Icon not found: " + filename);
        }
    }

    // ══════════════════════════════════════════════
    // SETUP
    // ══════════════════════════════════════════════
    private void setupFormCombos() {
        fieldCategory.setItems(FXCollections.observableArrayList(CATEGORIES));
        fieldCategory.setValue("Food and Dining");

        // Month options: current month ± 5
        List<String> months = new ArrayList<>();
        YearMonth base = YearMonth.now();
        for (int i = -2; i <= 5; i++)
            months.add(base.plusMonths(i).format(MONTH_FMT));
        fieldMonth.setItems(FXCollections.observableArrayList(months));
        fieldMonth.setValue(YearMonth.now().format(MONTH_FMT));
    }



    // ══════════════════════════════════════════════
    // REFRESH VIEW
    // ══════════════════════════════════════════════
    private void refreshView() {
        loadBudgetsFromDB();
        monthNameLabel.setText(displayMonth.format(MONTH_FMT));

        List<Budget> forMonth = new java.util.ArrayList<>(allBudgets);

        double totalBudget = forMonth.stream().mapToDouble(b -> b.limit).sum();
        double totalSpent  = forMonth.stream().mapToDouble(b -> b.spent).sum();
        double remaining   = totalBudget - totalSpent;

        // Summary bar
        summaryTotalBudget.setText("Rs. " + fmt(totalBudget));
        summaryTotalSpent.setText("Rs. " + fmt(totalSpent));
        summaryRemaining.setText("Rs. " + fmt(Math.max(remaining, 0)));

        // Overall card
        overallSpentLabel.setText("Rs. " + fmt(totalSpent));
        overallLimitLabel.setText("/ Rs. " + fmt(totalBudget));
        double overallPct = totalBudget > 0 ? totalSpent / totalBudget * 100 : 0;
        overallSubLabel.setText(String.format("%.1f%% used  •  Rs. %s remaining",
                overallPct, fmt(Math.max(remaining, 0))));

        // Days left in month
        LocalDate today   = LocalDate.now();
        LocalDate lastDay = displayMonth.atEndOfMonth();
        long daysLeft = today.getMonthValue() == displayMonth.getMonthValue()
                && today.getYear() == displayMonth.getYear()
                ? java.time.temporal.ChronoUnit.DAYS.between(today, lastDay)
                : 0;
        daysLeftLabel.setText(String.valueOf(daysLeft));

        // Overall fill width — bind after layout
        overallFill.parentProperty().addListener((obs, o, parent) -> {
            if (parent instanceof StackPane sp) {
                sp.widthProperty().addListener((o2, ov, w) ->
                        overallFill.setPrefWidth(w.doubleValue() *
                                Math.min(overallPct / 100.0, 1.0)));
            }
        });

        // Build grid
        buildBudgetGrid(forMonth);
    }

    // ══════════════════════════════════════════════
    // BUILD BUDGET CARDS GRID
    // ══════════════════════════════════════════════
    private void buildBudgetGrid(List<Budget> budgets) {
        budgetGridContainer.getChildren().clear();

        // Build rows of COLS cards
        HBox currentRow = null;
        for (int i = 0; i < budgets.size(); i++) {
            if (i % COLS == 0) {
                currentRow = new HBox(14);
                currentRow.setAlignment(Pos.TOP_LEFT);
                budgetGridContainer.getChildren().add(currentRow);
            }
            VBox card = buildBudgetCard(budgets.get(i));
            HBox.setHgrow(card, Priority.ALWAYS);
            currentRow.getChildren().add(card);
        }

        // Fill last row with empty spacers so grid stays 3 columns
        if (currentRow != null) {
            int remainder = budgets.size() % COLS;
            if (remainder != 0) {
                for (int i = 0; i < COLS - remainder; i++) {
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    currentRow.getChildren().add(spacer);
                }
            }
        }
    }

    private VBox buildBudgetCard(Budget budget) {
        boolean over       = budget.isOver();
        String  colorHex   = CAT_COLOR.getOrDefault(budget.category, "#2563EB");
        String  iconFile   = CAT_ICON.getOrDefault(budget.category, "tag.png");
        double  pct        = budget.pct() * 100;

        VBox card = new VBox(10);
        card.getStyleClass().add(over ? "budget-card-over" : "budget-card");

        // ── Top row: icon | name | edit | delete ──
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("budget-cat-icon");
        iconBox.setStyle("-fx-background-color: " + hexWithAlpha(colorHex, 0.10) + ";");
        ImageView iconView = new ImageView();
        iconView.setFitWidth(15); iconView.setFitHeight(15);
        iconView.setPreserveRatio(true);
        setIcon(iconView, iconFile);
        iconBox.getChildren().add(iconView);

        Label nameLabel = new Label(budget.category);
        nameLabel.getStyleClass().add("budget-cat-name");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Button editBtn = new Button();
        editBtn.getStyleClass().add("budget-edit-btn");
        ImageView editIv = new ImageView();
        editIv.setFitWidth(12); editIv.setFitHeight(12); editIv.setPreserveRatio(true);
        setIcon(editIv, "edit.png");
        editBtn.setGraphic(editIv);
        editBtn.setOnAction(e -> openEditForm(budget));

        Button delBtn = new Button();
        delBtn.getStyleClass().add("budget-delete-btn");
        ImageView delIv = new ImageView();
        delIv.setFitWidth(12); delIv.setFitHeight(12); delIv.setPreserveRatio(true);
        setIcon(delIv, "trash.png");
        delBtn.setGraphic(delIv);
        delBtn.setOnAction(e -> deleteBudget(budget));

        topRow.getChildren().addAll(iconBox, nameLabel, editBtn, delBtn);

        // ── Amounts row ────────────────────────────
        HBox amtRow = new HBox(5);
        amtRow.setAlignment(Pos.BASELINE_LEFT);
        Label spentLbl = new Label("Rs. " + fmt(budget.spent));
        spentLbl.getStyleClass().add(over ? "budget-spent-over" : "budget-spent");
        Label ofLbl = new Label("of");
        ofLbl.getStyleClass().add("budget-of");
        Label limitLbl = new Label("Rs. " + fmt(budget.limit));
        limitLbl.getStyleClass().add("budget-limit");
        amtRow.getChildren().addAll(spentLbl, ofLbl, limitLbl);

        // ── Progress bar ───────────────────────────
        StackPane trackPane = new StackPane();
        trackPane.getStyleClass().add("budget-track");
        Region fill = new Region();
        fill.getStyleClass().add(getFillStyle(pct, over));
        fill.setPrefHeight(7); fill.setMinHeight(7); fill.setMaxHeight(7);
        // Bind fill to track width
        trackPane.widthProperty().addListener((obs, o, w) ->
                fill.setPrefWidth(w.doubleValue() * Math.min(pct / 100.0, 1.0)));
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        trackPane.getChildren().addAll(new Region(), fill);
        // Make track full width
        ((Region) trackPane.getChildren().get(0))
                .getStyleClass().add("budget-track");
        ((Region) trackPane.getChildren().get(0))
                .setMaxWidth(Double.MAX_VALUE);

        // ── Status row ─────────────────────────────
        HBox statusRow = new HBox();
        statusRow.setAlignment(Pos.CENTER_LEFT);

        if (over) {
            double overBy = budget.spent - budget.limit;
            HBox badge = new HBox(4);
            badge.getStyleClass().add("over-badge");
            badge.setAlignment(Pos.CENTER_LEFT);
            ImageView alertIv = new ImageView();
            alertIv.setFitWidth(10); alertIv.setFitHeight(10);
            setIcon(alertIv, "alert-triangle.png");
            Label badgeTxt = new Label("Over by Rs. " + fmt(overBy));
            badgeTxt.getStyleClass().add("over-badge-text");
            badge.getChildren().addAll(alertIv, badgeTxt);

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Label pctLbl = new Label(String.format("%.1f%% used", pct));
            pctLbl.getStyleClass().add("budget-pct-label");
            pctLbl.setStyle("-fx-text-fill:#EF4444;");
            statusRow.getChildren().addAll(badge, sp, pctLbl);
        } else {
            Label pctLbl = new Label(String.format("%.1f%% used", pct));
            pctLbl.getStyleClass().add("budget-pct-label");
            pctLbl.setStyle("-fx-text-fill:" + colorHex + ";");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Label remLbl = new Label("Rs. " + fmt(budget.remaining()) + " left");
            remLbl.getStyleClass().add("budget-rem-label");
            statusRow.getChildren().addAll(pctLbl, sp, remLbl);
        }

        card.getChildren().addAll(topRow, amtRow, trackPane, statusRow);
        return card;
    }

    // ══════════════════════════════════════════════
    // FORM — TOGGLE / OPEN / CLOSE
    // ══════════════════════════════════════════════
    @FXML
    private void handleToggleForm() {
        if (budgetFormCard.isVisible()) {
            closeForm();
        } else {
            openAddForm();
        }
    }

    private void openAddForm() {
        editMode = false; editingBudget = null;
        formHeadingLabel.setText("Set New Budget");
        setIcon(formHeaderIcon, "plus.png");
        fieldCategory.setValue("Food and Dining");
        fieldLimit.clear();
        fieldMonth.setValue(displayMonth.format(MONTH_FMT));
        formError.setText("");
        showForm();
    }

    private void openEditForm(Budget b) {
        editMode = true; editingBudget = b;
        formHeadingLabel.setText("Edit Budget");
        setIcon(formHeaderIcon, "edit.png");
        fieldCategory.setValue(b.category);
        fieldLimit.setText(String.format("%.0f", b.limit));
        fieldMonth.setValue(b.month.format(MONTH_FMT));
        formError.setText("");
        showForm();
    }

    private void showForm() {
        budgetFormCard.setOpacity(0);
        budgetFormCard.setVisible(true);
        budgetFormCard.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(180), budgetFormCard);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void closeForm() {
        FadeTransition ft = new FadeTransition(Duration.millis(150), budgetFormCard);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> {
            budgetFormCard.setVisible(false);
            budgetFormCard.setManaged(false);
        });
        ft.play();
        editMode = false; editingBudget = null;
    }

    // ══════════════════════════════════════════════
    // FORM — SAVE
    // ══════════════════════════════════════════════
    @FXML
    private void handleSave() {
        formError.setText("");
        String cat   = fieldCategory.getValue();
        String amtStr = fieldLimit.getText().trim();
        String monthStr = fieldMonth.getValue();

        double limit;
        try {
            limit = Double.parseDouble(amtStr);
            if (limit <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showFormError("Please enter a valid positive amount.");
            return;
        }

        YearMonth month;
        try {
            month = YearMonth.parse(monthStr, MONTH_FMT);
        } catch (Exception ex) {
            showFormError("Invalid month selected.");
            return;
        }

        if (!SessionManager.isLoggedIn()) return;
        int userId = SessionManager.getUserId();
        BudgetService.BudgetResult result =
                BudgetService.getInstance().setBudget(userId, cat, limit, month);
        if (!result.success()) {
            showFormError(result.message());
            return;
        }

        animateSaveSuccess();
        refreshView();
    }

    private void showFormError(String msg) {
        formError.setText(msg);
        FadeTransition ft = new FadeTransition(Duration.millis(220), formError);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void animateSaveSuccess() {
        String orig = btnSave.getText();
        btnSave.setText("Saved!");
        btnSave.setDisable(true);
        PauseTransition p = new PauseTransition(Duration.seconds(1.2));
        p.setOnFinished(e -> {
            btnSave.setText(orig);
            btnSave.setDisable(false);
            closeForm();
        });
        p.play();
    }

    // ══════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════
    private void deleteBudget(Budget b) {
        if (SessionManager.isLoggedIn()) {
            try {
                int id = Integer.parseInt(b.id);
                BudgetService.getInstance().deleteBudget(id, SessionManager.getUserId());
            } catch (Exception e) {
                System.out.println("Budget delete error: " + e.getMessage());
            }
        }
        refreshView();
    }

    // ══════════════════════════════════════════════
    // MONTH NAVIGATION
    // ══════════════════════════════════════════════
    @FXML
    private void handlePrevMonth() {
        displayMonth = displayMonth.minusMonths(1);
        refreshView();
    }

    @FXML
    private void handleNextMonth() {
        displayMonth = displayMonth.plusMonths(1);
        refreshView();
    }

    // ══════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════
    private String getFillStyle(double pct, boolean over) {
        if (over)       return "budget-fill-red";
        if (pct >= 85)  return "budget-fill-amber";
        if (pct >= 60)  return "budget-fill-amber";
        return "budget-fill-green";
    }

    private String fmt(double v) {
        return String.format("%,.0f", v);
    }

    /** Returns rgba CSS string for a hex color with given alpha 0.0–1.0 */
    private String hexWithAlpha(String hex, double alpha) {
        hex = hex.replace("#", "");
        int r = Integer.parseInt(hex.substring(0,2), 16);
        int g = Integer.parseInt(hex.substring(2,4), 16);
        int b = Integer.parseInt(hex.substring(4,6), 16);
        return String.format("rgba(%d,%d,%d,%.2f)", r, g, b, alpha);
    }

    // ══════════════════════════════════════════════
    // NAV + MISC HANDLERS
    // ══════════════════════════════════════════════
    @FXML private void handleNavOverview()     {
        SceneManager.navigateTo(SceneManager.Screen.DASHBOARD);
        setActiveNav(navOverview);
    }
    @FXML private void handleNavTransactions() {
        SceneManager.navigateTo(SceneManager.Screen.TRANSACTIONS);
        setActiveNav(navTransactions);
    }
    @FXML private void handleNavAnalytics()    {
        SceneManager.navigateTo(SceneManager.Screen.ANALYTICS);
        setActiveNav(navAnalytics);
    }
    @FXML private void handleNavBudgets()      {
        SceneManager.navigateTo(SceneManager.Screen.BUDGETS);
        setActiveNav(navBudgets);
    }
    @FXML private void handleNavReports()      {
        SceneManager.navigateTo(SceneManager.Screen.REPORTS);
        setActiveNav(navReports);
    }
    @FXML private void handleNavSettings()     { SceneManager.navigateTo(SceneManager.Screen.SETTINGS); }
    @FXML private void handleNavHelp()         { SceneManager.navigateTo(SceneManager.Screen.HELP); }

    @FXML
    private void handleUserChip(MouseEvent e) {
        UserPopupUtil.show((javafx.scene.Node) e.getSource(),
                budgetsRoot.getScene().getWindow());
    }
    @FXML private void handleNotifications()   { notifDot.setVisible(false); }
    @FXML private void handleExport()          { SceneManager.navigateTo(SceneManager.Screen.REPORTS); }

    private void setActiveNav(Button selected) {
        for (Button b : new Button[]{navOverview, navTransactions, navAnalytics,
                navBudgets, navReports, navSettings, navHelp}) {
            b.getStyleClass().remove("active");
        }
        if (!selected.getStyleClass().contains("active"))
            selected.getStyleClass().add("active");
    }
}
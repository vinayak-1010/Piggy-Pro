package com.piggypro.controller;

import com.piggypro.SceneManager;
import com.piggypro.SessionManager;
import com.piggypro.model.Expense;
import com.piggypro.service.ExpenseService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * DashboardController
 * ─────────────────────────────────────────────────────
 * Drives DashboardView.fxml.
 *
 * Charts are drawn onto Canvas nodes that replace the
 * placeholder Region fx:id fields after layout.
 *
 * Icons (all 24x24 PNG from lucide.dev) go in:
 *   src/main/resources/com/piggypro/images/icons/
 *
 * Required icons:
 *   logo.png          sidebar piggy bank logo
 *   grid.png          overview nav
 *   bookmark.png      transactions nav
 *   bar-chart.png     analytics nav
 *   clock.png         budgets nav
 *   file-text.png     reports nav
 *   settings.png      settings nav
 *   help-circle.png   help nav
 *   zap.png           export button
 *   search.png        topbar search
 *   bell.png          topbar notification
 *   chevron-down.png  user chip chevron
 *   calendar.png      date badge
 *   more-h.png        more (horizontal dots) button
 *   arrow-right.png   view all button
 *   shopping-bag.png  txn: shopping
 *   credit-card.png   txn: income
 *   send.png          txn: food
 *   phone.png         txn: utilities
 */
public class DashboardController implements Initializable {

    /* ── Root ── */
    @FXML private BorderPane dashboardRoot;

    /* ── Sidebar nav buttons ── */
    @FXML private Button navOverview;
    @FXML private Button navTransactions;
    @FXML private Button navAnalytics;
    @FXML private Button navBudgets;
    @FXML private Button navReports;
    @FXML private Button navSettings;
    @FXML private Button navHelp;
    @FXML private Button btnExport;

    /* ── Sidebar icons ── */
    @FXML private ImageView sidebarLogoIcon;
    @FXML private ImageView iconOverview;
    @FXML private ImageView iconTransactions;
    @FXML private ImageView iconAnalytics;
    @FXML private ImageView iconBudgets;
    @FXML private ImageView iconReports;
    @FXML private ImageView iconSettings;
    @FXML private ImageView iconHelp;
    @FXML private ImageView iconExport;

    /* ── Topbar ── */
    @FXML private TextField searchField;
    @FXML private Button    notifBtn;
    @FXML private Circle    notifDot;
    @FXML private Label     avatarInitials;
    @FXML private Label     userDisplayName;

    /* ── Topbar icons ── */
    @FXML private ImageView searchIcon;
    @FXML private ImageView notifIcon;
    @FXML private ImageView chevronIcon;
    @FXML private ImageView calendarIcon;

    /* ── Page header ── */
    @FXML private Label dateRangeLabel;

    /* ── Stat card labels ── */
    @FXML private Label valueBalance;
    @FXML private Label valueExpenses;
    @FXML private Label valueSavings;
    @FXML private Label badgeBalance;
    @FXML private Label badgeExpenses;
    @FXML private Label badgeSavings;

    /* ── Sparkline placeholder regions ── */
    @FXML private Region sparkBalance;
    @FXML private Region sparkExpenses;
    @FXML private Region sparkSavings;

    /* ── Chart controls ── */
    @FXML private Button    periodSpending;
    @FXML private ImageView moreIconSpending;
    @FXML private ImageView moreIconCategory;

    /* ── Chart placeholder regions ── */
    @FXML private Region spendingChartArea;
    @FXML private Region topCatBarsArea;

    /* ── Top category card ── */
    @FXML private Label topCatValue;
    @FXML private Label topCatBadge;
    @FXML private Label topCatHint;

    /* ── Transaction rows ── */
    @FXML private javafx.scene.layout.HBox    txnRow1, txnRow2, txnRow3, txnRow4;
    @FXML private javafx.scene.layout.StackPane txnIconBox1, txnIconBox2, txnIconBox3, txnIconBox4;
    @FXML private ImageView txnIcon1, txnIcon2, txnIcon3, txnIcon4;
    @FXML private Label txnName1, txnName2, txnName3, txnName4;
    @FXML private Label txnCategory1, txnCategory2, txnCategory3, txnCategory4;
    @FXML private Label txnDate1, txnDate2, txnDate3, txnDate4;
    @FXML private Label txnAmount1, txnAmount2, txnAmount3, txnAmount4;

    /* ── Budget progress bars ── */
    @FXML private Region budgetBarFood;
    @FXML private Region budgetBarTransport;
    @FXML private Region budgetBarShopping;
    @FXML private Region budgetBarEntertainment;
    @FXML private Region budgetBarUtilities;

    /* ── Period / more buttons ── */
    @FXML private Button periodBudget;

    // ──────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadIcons();
        setDateRange();
        loadDashboardData();
        drawChartsAfterLayout();
        // Set user info in topbar from session
        if (SessionManager.isLoggedIn()) {
            userDisplayName.setText(SessionManager.getUsername());
            avatarInitials.setText(SessionManager.getInitials());
        }
    }

    // ══════════════════════════════════════════════════
    // LIVE DATA — from ExpenseService
    // ══════════════════════════════════════════════════
    private void loadDashboardData() {
        if (!SessionManager.isLoggedIn()) return;
        int userId = SessionManager.getUserId();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        try {
            ExpenseService svc = ExpenseService.getInstance();

            double expenses = svc.getTotalExpenses(userId, monthStart, today);
            double income   = svc.getTotalIncome(userId, monthStart, today);
            double balance  = income - expenses;

            valueBalance.setText("Rs. " + String.format("%,.0f", balance));
            valueExpenses.setText("Rs. " + String.format("%,.0f", expenses));
            valueSavings.setText("Rs. " + String.format("%,.0f", Math.max(balance, 0)));

            // Top category
            Map<String, Double> catTotals = svc.getCategoryTotals(userId, monthStart, today);
            if (!catTotals.isEmpty()) {
                String topCat = catTotals.entrySet().iterator().next().getKey();
                double topAmt = catTotals.entrySet().iterator().next().getValue();
                topCatValue.setText("Rs. " + String.format("%,.0f", topAmt));
                topCatHint.setText(topCat + "  Last 30 days");
            }

            // Recent transactions — last 4
            java.util.List<com.piggypro.model.Expense> recent =
                    svc.getRecent(userId, 4);
            populateRecentTxns(recent);

        } catch (Exception e) {
            System.out.println("Dashboard data load error: " + e.getMessage());
        }
    }

    private void populateRecentTxns(java.util.List<com.piggypro.model.Expense> list) {
        Label[]     names   = {txnName1,     txnName2,     txnName3,     txnName4};
        Label[]     cats    = {txnCategory1, txnCategory2, txnCategory3, txnCategory4};
        Label[]     dates   = {txnDate1,     txnDate2,     txnDate3,     txnDate4};
        Label[]     amounts = {txnAmount1,   txnAmount2,   txnAmount3,   txnAmount4};
        javafx.scene.layout.HBox[] rows = {txnRow1, txnRow2, txnRow3, txnRow4};

        java.time.format.DateTimeFormatter fmt =
                java.time.format.DateTimeFormatter.ofPattern("dd MMM");

        for (int i = 0; i < 4; i++) {
            if (rows[i] == null) continue;
            if (i < list.size()) {
                com.piggypro.model.Expense e = list.get(i);
                rows[i].setVisible(true);
                rows[i].setManaged(true);
                names[i].setText(e.getDescription());
                cats[i].setText(e.getCategory());
                dates[i].setText(e.getDate().format(fmt));
                boolean isIncome = e.isIncome();
                String amtText = (isIncome ? "+" : "-") + "Rs." +
                        String.format("%,.0f", e.getAmount());
                amounts[i].setText(amtText);
                amounts[i].getStyleClass().removeAll("txn-amount-neg","txn-amount-pos");
                amounts[i].getStyleClass().add(isIncome ? "txn-amount-pos" : "txn-amount-neg");
            } else {
                rows[i].setVisible(false);
                rows[i].setManaged(false);
            }
        }
    }

    // ══════════════════════════════════════════════════
    // ICON LOADING
    // ══════════════════════════════════════════════════
    private void loadIcons() {
        // Sidebar
        setIcon(sidebarLogoIcon,   "piggy-bank.png");
        setIcon(iconOverview,      "grid.png");
        setIcon(iconTransactions,  "bookmark.png");
        setIcon(iconAnalytics,     "bar-chart.png");
        setIcon(iconBudgets,       "clock.png");
        setIcon(iconReports,       "file-text.png");
        setIcon(iconSettings,      "settings.png");
        setIcon(iconHelp,          "circle-question-mark.png");
        setIcon(iconExport,        "zap.png");
        // Topbar
        setIcon(searchIcon,        "search.png");
        setIcon(notifIcon,         "bell.png");
        setIcon(chevronIcon,       "chevron-down.png");
        setIcon(calendarIcon,      "calendar.png");
        // Chart more buttons
        setIcon(moreIconSpending,  "more-horizontal.png");
        setIcon(moreIconCategory,  "more-horizontal.png");
        // Transaction row icons set dynamically based on category
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

    // ══════════════════════════════════════════════════
    // DATE RANGE
    // ══════════════════════════════════════════════════
    private void setDateRange() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.withDayOfMonth(1);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");
        dateRangeLabel.setText(
                start.format(fmt) + " – " + today.format(fmt) + ", " + today.getYear()
        );
    }

    // ══════════════════════════════════════════════════
    // CHARTS  — drawn after layout pass so Region sizes
    //           are known (via listener on width property)
    // ══════════════════════════════════════════════════
    private void drawChartsAfterLayout() {
        // Sparklines — draw once width is available
        drawSparklineWhenReady(sparkBalance,  new double[]{26,20,9,13,17,7,5,3,9,7},  "#2563EB", false);
        drawSparklineWhenReady(sparkExpenses, new double[]{9,13,21,17,13,25,21,17,27,23}, "#EF4444", false);
        drawSparklineWhenReady(sparkSavings,  new double[]{25,19,23,15,7,13,9,5,9,7},  "#10B981", false);

        // Spending overview line chart
        spendingChartArea.widthProperty().addListener((obs, o, w) -> {
            if (w.doubleValue() > 10)
                drawSpendingChart(spendingChartArea);
        });

        // Top category mini bars
        topCatBarsArea.widthProperty().addListener((obs, o, w) -> {
            if (w.doubleValue() > 10)
                drawTopCatBars(topCatBarsArea);
        });

        // Budget progress widths — set via prefWidth binding to parent
        setBudgetBarWidth(budgetBarFood,          0.80);
        setBudgetBarWidth(budgetBarTransport,     0.40);
        setBudgetBarWidth(budgetBarShopping,      1.00);
        setBudgetBarWidth(budgetBarEntertainment, 0.40);
        setBudgetBarWidth(budgetBarUtilities,     0.64);
    }

    /** Replaces a Region placeholder with a Canvas sparkline. */
    private void drawSparklineWhenReady(Region region,
                                        double[] points,
                                        String colorHex,
                                        boolean redStroke) {
        region.widthProperty().addListener((obs, o, w) -> {
            if (w.doubleValue() > 10)
                replaceWithSparkline(region, points, colorHex);
        });
    }

    private void replaceWithSparkline(Region region, double[] pts, String colorHex) {
        double W = region.getWidth();
        double H = region.getHeight() > 0 ? region.getHeight() : 32;
        Canvas c = new Canvas(W, H);
        GraphicsContext gc = c.getGraphicsContext2D();

        double min = pts[0], max = pts[0];
        for (double p : pts) { min = Math.min(min, p); max = Math.max(max, p); }
        double range = (max == min) ? 1 : max - min;
        double pad = 2;
        double step = (W - pad * 2) / (pts.length - 1);

        // Build x/y arrays
        double[] xs = new double[pts.length];
        double[] ys = new double[pts.length];
        for (int i = 0; i < pts.length; i++) {
            xs[i] = pad + i * step;
            ys[i] = H - pad - ((pts[i] - min) / range) * (H - pad * 2);
        }

        Color stroke = Color.web(colorHex);

        // Gradient fill
        LinearGradient fill = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, stroke.deriveColor(0, 1, 1, 0.14)),
                new Stop(1, stroke.deriveColor(0, 1, 1, 0.0))
        );
        gc.setFill(fill);
        gc.beginPath();
        gc.moveTo(xs[0], ys[0]);
        for (int i = 1; i < pts.length; i++) {
            double cpx = (xs[i - 1] + xs[i]) / 2.0;
            gc.bezierCurveTo(cpx, ys[i - 1], cpx, ys[i], xs[i], ys[i]);
        }
        gc.lineTo(xs[pts.length - 1], H);
        gc.lineTo(xs[0], H);
        gc.closePath();
        gc.fill();

        // Stroke line
        gc.setStroke(stroke);
        gc.setLineWidth(1.8);
        gc.beginPath();
        gc.moveTo(xs[0], ys[0]);
        for (int i = 1; i < pts.length; i++) {
            double cpx = (xs[i - 1] + xs[i]) / 2.0;
            gc.bezierCurveTo(cpx, ys[i - 1], cpx, ys[i], xs[i], ys[i]);
        }
        gc.stroke();

        // Swap region for canvas inside its parent StackPane
        if (region.getParent() instanceof javafx.scene.layout.VBox vbox) {
            int idx = vbox.getChildren().indexOf(region);
            if (idx >= 0) {
                StackPane wrap = new StackPane(c);
                wrap.setPrefHeight(H);
                wrap.setMinHeight(H);
                wrap.setMaxHeight(H);
                vbox.getChildren().set(idx, wrap);
            }
        }
    }

    /** Draws the spending overview line chart into a Region placeholder. */
    private void drawSpendingChart(Region region) {
        double W = region.getWidth();
        double H = region.getHeight() > 0 ? region.getHeight() : 140;
        Canvas c = new Canvas(W, H);
        GraphicsContext gc = c.getGraphicsContext2D();

        double[] yPts = {88, 74, 36, 50, 64, 22, 18, 46, 36, 42, 32};
        double padL = 30, padR = 4, padT = 14, padB = 20;
        double chartW = W - padL - padR;
        double chartH = H - padT - padB;
        int n = yPts.length;

        // Grid lines
        gc.setStroke(Color.web("#F1F4FB"));
        gc.setLineWidth(1);
        for (int r = 0; r <= 3; r++) {
            double y = padT + r * (chartH / 3.0);
            gc.strokeLine(padL, y, W - padR, y);
        }

        // Y labels
        gc.setFill(Color.web("#C8D0E0"));
        gc.setFont(javafx.scene.text.Font.font("DM Sans", 8));
        String[] ylbls = {"5K", "3K", "2K", "1K"};
        for (int r = 0; r <= 3; r++) {
            double y = padT + r * (chartH / 3.0);
            gc.fillText("Rs." + ylbls[r], 0, y + 3);
        }

        // X labels
        String[] xlbls = {"28Feb","1Mar","2Mar","3Mar","4Mar","5Mar","6Mar"};
        double xStep = chartW / (xlbls.length - 1);
        for (int i = 0; i < xlbls.length; i++) {
            gc.fillText(xlbls[i], padL + i * xStep - 8, H - 2);
        }

        // Map y values to canvas coords
        double min = 10, max = 100, range = max - min;
        double step = chartW / (n - 1);
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = padL + i * step;
            ys[i] = padT + chartH - ((yPts[i] - min) / range) * chartH;
        }

        Color accent = Color.web("#2563EB");

        // Fill under curve
        LinearGradient fillGrad = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, accent.deriveColor(0, 1, 1, 0.11)),
                new Stop(1, accent.deriveColor(0, 1, 1, 0.0))
        );
        gc.setFill(fillGrad);
        gc.beginPath();
        gc.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            double cpx = (xs[i - 1] + xs[i]) / 2.0;
            gc.bezierCurveTo(cpx, ys[i - 1], cpx, ys[i], xs[i], ys[i]);
        }
        gc.lineTo(xs[n - 1], padT + chartH);
        gc.lineTo(xs[0], padT + chartH);
        gc.closePath();
        gc.fill();

        // Curve stroke
        gc.setStroke(accent);
        gc.setLineWidth(2.0);
        gc.beginPath();
        gc.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            double cpx = (xs[i - 1] + xs[i]) / 2.0;
            gc.bezierCurveTo(cpx, ys[i - 1], cpx, ys[i], xs[i], ys[i]);
        }
        gc.stroke();

        // Peak dot (index 6)
        int peakIdx = 6;
        gc.setFill(accent.deriveColor(0, 1, 1, 0.15));
        gc.fillOval(xs[peakIdx] - 7, ys[peakIdx] - 7, 14, 14);
        gc.setFill(accent);
        gc.fillOval(xs[peakIdx] - 4, ys[peakIdx] - 4, 8, 8);

        // Dashed vertical line at peak
        gc.setStroke(accent.deriveColor(0, 1, 1, 0.35));
        gc.setLineWidth(1);
        gc.setLineDashes(3, 3);
        gc.strokeLine(xs[peakIdx], ys[peakIdx], xs[peakIdx], padT + chartH);
        gc.setLineDashes(); // reset

        // Swap placeholder
        if (region.getParent() instanceof javafx.scene.layout.VBox vbox) {
            int idx = vbox.getChildren().indexOf(region);
            if (idx >= 0) vbox.getChildren().set(idx, c);
        }
    }

    /** Draws the mini bar chart for top category. */
    private void drawTopCatBars(Region region) {
        double W = region.getWidth();
        double H = region.getHeight() > 0 ? region.getHeight() : 60;
        Canvas c = new Canvas(W, H);
        GraphicsContext gc = c.getGraphicsContext2D();

        double[] heights = {0.33, 0.52, 0.40, 0.76, 0.58, 0.46, 0.62};
        int activeIdx = 3;
        double gap = 4;
        double barW = (W - gap * (heights.length - 1)) / heights.length;
        Color inactive = Color.web("#E8ECF4");
        Color active   = Color.web("#2563EB");

        for (int i = 0; i < heights.length; i++) {
            double bH = heights[i] * H;
            double x  = i * (barW + gap);
            double y  = H - bH;
            gc.setFill(i == activeIdx ? active : inactive);
            // Rounded top
            double arc = Math.min(3, barW / 2);
            gc.fillRoundRect(x, y, barW, bH, arc, arc);
        }

        if (region.getParent() instanceof javafx.scene.layout.VBox vbox) {
            int idx = vbox.getChildren().indexOf(region);
            if (idx >= 0) vbox.getChildren().set(idx, c);
        }
    }

    /**
     * Sets the preferred width of a budget bar as a fraction of its
     * parent StackPane. Uses a listener so the parent width is known.
     */
    private void setBudgetBarWidth(Region bar, double fraction) {
        if (bar == null) return;
        bar.setMaxWidth(Double.MAX_VALUE);
        // Wait until parent has a real width
        bar.parentProperty().addListener((obs, oldP, newP) -> {
            if (newP instanceof StackPane sp) {
                Runnable apply = () -> bar.setPrefWidth(sp.getWidth() * fraction);
                apply.run();
                sp.widthProperty().addListener((o2, o, w) ->
                        bar.setPrefWidth(w.doubleValue() * fraction));
            }
        });
    }

    // ══════════════════════════════════════════════════
    // NAV HANDLERS
    // ══════════════════════════════════════════════════
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
    @FXML private void handleNavSettings()     { setActiveNav(navSettings); }
    @FXML private void handleNavHelp()         { setActiveNav(navHelp); }

    private void setActiveNav(Button selected) {
        Button[] all = {navOverview, navTransactions, navAnalytics,
                navBudgets, navReports, navSettings, navHelp};
        for (Button b : all) {
            b.getStyleClass().remove("active");
        }
        if (!selected.getStyleClass().contains("active"))
            selected.getStyleClass().add("active");
        // TODO: load the corresponding view into the center pane
    }

    // ══════════════════════════════════════════════════
    // TOPBAR HANDLERS
    // ══════════════════════════════════════════════════
    @FXML
    private void handleNotifications() {
        // TODO: show notifications popover
        notifDot.setVisible(false);
    }

    // ══════════════════════════════════════════════════
    // CONTENT HANDLERS
    // ══════════════════════════════════════════════════
    @FXML
    private void handlePeriodSpending() {
        // TODO: cycle through period options (7d / 30d / 90d)
    }

    @FXML private void handleMoreSpending()  { /* TODO: context menu */ }
    @FXML private void handleMoreCategory()  { /* TODO: context menu */ }

    @FXML
    private void handleViewAllTransactions() {
        setActiveNav(navTransactions);
    }

    @FXML
    private void handlePeriodBudget() {
        // TODO: cycle months
    }

    @FXML
    private void handleExport() {
        // TODO: trigger ExportService (PDF / Excel)
    }
}
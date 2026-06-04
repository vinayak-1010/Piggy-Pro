package com.piggypro.controller;

import com.piggypro.SceneManager;
import com.piggypro.util.UserPopupUtil;
import com.piggypro.SessionManager;
import com.piggypro.service.ExpenseService;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.net.URL;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * AnalyticsController
 * ─────────────────────────────────────────────────────
 * Drives AnalyticsView.fxml.
 *
 * Charts are drawn onto Canvas nodes that replace
 * the Region placeholder (fx:id) fields after layout.
 *
 * Required icons (24x24 PNG from lucide.dev):
 *   logo.png, grid.png, bookmark.png, bar-chart.png,
 *   clock.png, file-text.png, settings.png,
 *   help-circle.png, zap.png, search.png, bell.png,
 *   chevron-down.png, more-h.png,
 *   dollar-sign.png  (Total Spent stat)
 *   star.png         (Top Category stat)
 *   calendar.png     (Avg per Day stat)
 *   list.png         (Transaction Count stat)
 */
public class AnalyticsController implements Initializable {

    // ── Root ───────────────────────────────────────
    @FXML private BorderPane analyticsRoot;

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

    // ── Range bar ──────────────────────────────────
    @FXML private Button   btnRange7d, btnRange30d, btnRange3m, btnRange6m, btnRange1y;
    @FXML private Button   btnApply;
    @FXML private DatePicker dateFrom, dateTo;

    // ── Stat cards ─────────────────────────────────
    @FXML private Label statTotalSpent, statSpentSub;
    @FXML private Label statTopCategory, statTopCatSub;
    @FXML private Label statAvgDay;
    @FXML private Label statTxnCount;

    @FXML private ImageView iconStatSpent, iconStatTop, iconStatAvg, iconStatCount;

    // ── Chart placeholders ─────────────────────────
    @FXML private Region donutChartArea;
    @FXML private Region barChartArea;
    @FXML private Label  donutCenterValue;

    // ── Legend container ───────────────────────────
    @FXML private VBox legendBox;

    // ── Chart more buttons ─────────────────────────
    @FXML private ImageView moreIconDonut, moreIconBar;

    // ── Palette ────────────────────────────────────
    private static final Color[] PALETTE = {
            Color.web("#F59E0B"), Color.web("#2563EB"), Color.web("#8B5CF6"),
            Color.web("#EF4444"), Color.web("#10B981"), Color.web("#E2E8F0")
    };

    // ── Live data (populated from DB) ──────────────
    private Map<String, Double> liveCategoryData = new LinkedHashMap<>();
    private Map<String, Double> liveMonthlyData  = new LinkedHashMap<>();

    // ── Active date range ──────────────────────────
    private LocalDate activeFrom;
    private LocalDate activeTo;

    // ──────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadIcons();
        activeTo   = LocalDate.now();
        activeFrom = activeTo.minusDays(29);
        dateFrom.setValue(activeFrom);
        dateTo.setValue(activeTo);
        refreshStats();
        drawChartsAfterLayout();
        if (SessionManager.isLoggedIn()) {
            userDisplayName.setText(SessionManager.getUsername());
            avatarInitials.setText(SessionManager.getInitials());
        }
    }

    // ══════════════════════════════════════════════
    // ICONS
    // ══════════════════════════════════════════════
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
        setIcon(searchIcon,       "search.png");
        setIcon(notifIcon,        "bell.png");
        setIcon(chevronIcon,      "chevron-down.png");
        setIcon(moreIconDonut,    "move-horizontal.png");
        setIcon(moreIconBar,      "move-horizontal.png");
        setIcon(iconStatSpent,    "dollar-sign.png");
        setIcon(iconStatTop,      "star.png");
        setIcon(iconStatAvg,      "calendar.png");
        setIcon(iconStatCount,    "list.png");
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
    // STATS
    // ══════════════════════════════════════════════
    private void loadLiveData() {
        if (!SessionManager.isLoggedIn()) return;
        int userId = SessionManager.getUserId();
        ExpenseService svc = ExpenseService.getInstance();
        liveCategoryData = svc.getCategoryTotals(userId, activeFrom, activeTo);
        liveMonthlyData  = svc.getMonthlyTotals(userId, 6);
    }

    private void refreshStats() {
        loadLiveData();
        double total   = liveCategoryData.values().stream().mapToDouble(Double::doubleValue).sum();
        long   days    = Math.max(1,
                java.time.temporal.ChronoUnit.DAYS.between(activeFrom, activeTo) + 1);
        double avgDay  = total / days;

        String topCat  = liveCategoryData.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("—");
        double topAmt  = liveCategoryData.getOrDefault(topCat, 0.0);

        // Real transaction count for the period
        int txnCount = 0;
        if (SessionManager.isLoggedIn()) {
            txnCount = ExpenseService.getInstance()
                    .getFiltered(SessionManager.getUserId(), activeFrom, activeTo,
                            null, null, null, null, null).size();
        }

        statTotalSpent.setText("Rs. " + String.format("%,.0f", total));
        statSpentSub.setText("From " + activeFrom + " to " + activeTo);
        statTopCategory.setText(topCat.split(" ")[0]);
        statTopCatSub.setText("Rs. " + String.format("%,.0f", topAmt) + " spent");
        statAvgDay.setText("Rs. " + String.format("%,.0f", avgDay));
        statTxnCount.setText(String.valueOf(txnCount));
        donutCenterValue.setText("Rs." + String.format("%,.0f", total / 1000) + "K");
    }

    // ══════════════════════════════════════════════
    // CHARTS — drawn after layout via width listener
    // ══════════════════════════════════════════════
    private void drawChartsAfterLayout() {
        donutChartArea.widthProperty().addListener((obs, o, w) -> {
            if (w.doubleValue() > 10) drawDonutChart(donutChartArea);
        });
        barChartArea.widthProperty().addListener((obs, o, w) -> {
            if (w.doubleValue() > 10) drawBarChart(barChartArea);
        });
    }

    // ── Donut chart ────────────────────────────────
    private void drawDonutChart(Region region) {
        double size = Math.min(region.getWidth(), region.getHeight());
        if (size <= 0) size = 160;
        Canvas c = new Canvas(size, size);
        GraphicsContext gc = c.getGraphicsContext2D();

        double cx = size / 2, cy = size / 2;
        double outerR = size * 0.46;
        double innerR = size * 0.28;
        double strokeW = outerR - innerR;

        double total = liveCategoryData.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0) { replaceRegionWithCanvas(region, c); return; }
        double startAngle = -90.0;

        int i = 0;
        Map<String, Double> chartData = liveCategoryData.isEmpty()
                ? new LinkedHashMap<>() : liveCategoryData;
        for (Map.Entry<String, Double> entry : chartData.entrySet()) {
            double sweep = (entry.getValue() / total) * 360.0;
            Color col = PALETTE[Math.min(i, PALETTE.length - 1)];

            gc.setStroke(col);
            gc.setLineWidth(strokeW);
            gc.setLineCap(javafx.scene.shape.StrokeLineCap.BUTT);
            gc.strokeArc(
                    cx - outerR + strokeW / 2,
                    cy - outerR + strokeW / 2,
                    (outerR - strokeW / 2) * 2,
                    (outerR - strokeW / 2) * 2,
                    startAngle,
                    -sweep,
                    javafx.scene.shape.ArcType.OPEN
            );

            // Gap between segments
            startAngle -= sweep;
            i++;
        }

        // Swap placeholder with canvas
        replaceRegionWithCanvas(region, c);

        // Build legend
        buildLegend(total);
    }

    // ── Bar chart ──────────────────────────────────
    private void drawBarChart(Region region) {
        double W = region.getWidth();
        double H = Math.max(region.getHeight(), 200);
        Canvas c = new Canvas(W, H);
        GraphicsContext gc = c.getGraphicsContext2D();

        int n = 6;   // last 6 months
        LocalDate now = LocalDate.now();
        double padL = 36, padR = 8, padT = 12, padB = 26;
        double chartW = W - padL - padR;
        double chartH = H - padT - padB;

        // Determine last 6 months
        int[] monthIdxs = new int[n];
        String[] monthLabels = new String[n];
        double[] values = new double[n];
        for (int i = 0; i < n; i++) {
            LocalDate m = now.minusMonths(n - 1 - i);
            monthIdxs[i]  = m.getMonthValue() - 1;
            monthLabels[i] = m.getMonth()
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            // Build YYYY-MM key to look up in live data
            String key = String.format("%04d-%02d",
                    m.getYear(), m.getMonthValue());
            values[i] = liveMonthlyData.getOrDefault(key, 0.0);
        }

        double maxVal = 0;
        for (double v : values) maxVal = Math.max(maxVal, v);
        maxVal = Math.ceil(maxVal / 5000) * 5000;
        if (maxVal == 0) maxVal = 20000;

        int ySteps = 4;
        // Grid lines + Y labels
        gc.setFont(Font.font("DM Sans", 9));
        for (int r = 0; r <= ySteps; r++) {
            double y = padT + chartH - r * (chartH / ySteps);
            gc.setStroke(Color.web("#F1F4FB"));
            gc.setLineWidth(1);
            gc.strokeLine(padL, y, W - padR, y);
            gc.setFill(Color.web("#C8D0E0"));
            double lbl = maxVal * r / ySteps;
            gc.fillText(lbl >= 1000
                    ? String.format("%.0fK", lbl / 1000)
                    : String.format("%.0f", lbl), 0, y + 3);
        }

        double barW   = (chartW / n) * 0.55;
        double groupW = chartW / n;
        Color accent  = Color.web("#2563EB");

        for (int i = 0; i < n; i++) {
            double bH  = (values[i] / maxVal) * chartH;
            double x   = padL + i * groupW + (groupW - barW) / 2;
            double y   = padT + chartH - bH;
            boolean cur = (i == n - 1);

            Color fillColor = cur ? accent : accent.deriveColor(0, 1, 1, 0.22);

            // Rounded top rect — approximate with fillRoundRect
            double arc = Math.min(6, barW / 2);
            gc.setFill(fillColor);
            gc.fillRoundRect(x, y, barW, bH, arc, arc);

            // Amount label above bar
            gc.setFill(cur ? accent : Color.web("#94A3B8"));
            gc.setFont(Font.font("DM Sans", 8.5));
            String lbl = values[i] >= 1000
                    ? String.format("%.0fK", values[i] / 1000)
                    : String.format("%.0f", values[i]);
            double lblX = x + barW / 2 - lbl.length() * 2.5;
            gc.fillText(lbl, lblX, y - 3);

            // Month label below
            gc.setFill(cur ? accent : Color.web("#94A3B8"));
            gc.setFont(Font.font("DM Sans", cur ? 10 : 10));
            double mlblX = x + barW / 2 - monthLabels[i].length() * 2.8;
            gc.fillText(monthLabels[i], mlblX, padT + chartH + 16);
        }

        replaceRegionWithCanvas(region, c);
    }

    // ── Legend builder ─────────────────────────────
    private void buildLegend(double total) {
        legendBox.getChildren().clear();
        int i = 0;
        Map<String, Double> chartData = liveCategoryData.isEmpty()
                ? new LinkedHashMap<>() : liveCategoryData;
        for (Map.Entry<String, Double> entry : chartData.entrySet()) {
            double pct = (entry.getValue() / total) * 100;
            Color col  = PALETTE[Math.min(i, PALETTE.length - 1)];

            HBox row = new HBox(9);
            row.setAlignment(Pos.CENTER_LEFT);

            // Coloured dot
            Rectangle dot = new Rectangle(10, 10);
            dot.setArcWidth(3); dot.setArcHeight(3);
            dot.setFill(col);

            // Category name
            Label name = new Label(entry.getKey());
            name.getStyleClass().add("legend-name");
            name.setMaxWidth(100);
            name.setMinWidth(80);
            HBox.setHgrow(name, javafx.scene.layout.Priority.NEVER);

            // Mini progress bar
            StackPane trackWrap = new StackPane();
            trackWrap.setMinWidth(60);
            trackWrap.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(trackWrap, javafx.scene.layout.Priority.ALWAYS);
            Region track = new Region();
            track.getStyleClass().add("legend-track");
            track.setMaxWidth(Double.MAX_VALUE);
            Region fill = new Region();
            fill.setStyle("-fx-background-color:" + toHex(col)
                    + ";-fx-background-radius:2px;");
            fill.setPrefHeight(4); fill.setMinHeight(4); fill.setMaxHeight(4);
            fill.setPrefWidth(pct);   // will be proportional after binding
            trackWrap.getChildren().addAll(track, fill);
            StackPane.setAlignment(fill, javafx.geometry.Pos.CENTER_LEFT);

            // Bind fill width to track width * fraction
            track.widthProperty().addListener((obs, o, w) ->
                    fill.setPrefWidth(w.doubleValue() * pct / 100.0));

            // Percentage label
            Label pctLbl = new Label(String.format("%.1f%%", pct));
            pctLbl.getStyleClass().add("legend-pct");

            row.getChildren().addAll(dot, name, trackWrap, pctLbl);
            legendBox.getChildren().add(row);
            i++;
        }
    }

    // ── Utility: swap Region with Canvas in VBox/HBox ──
    private void replaceRegionWithCanvas(Region region, Canvas canvas) {
        javafx.scene.Parent parent = region.getParent();
        if (parent instanceof StackPane sp) {
            int idx = sp.getChildren().indexOf(region);
            if (idx >= 0) sp.getChildren().set(idx, canvas);
        } else if (parent instanceof VBox vbox) {
            int idx = vbox.getChildren().indexOf(region);
            if (idx >= 0) vbox.getChildren().set(idx, canvas);
        } else if (parent instanceof HBox hbox) {
            int idx = hbox.getChildren().indexOf(region);
            if (idx >= 0) hbox.getChildren().set(idx, canvas);
        }
    }

    // ── Color to hex string ────────────────────────
    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int)(c.getRed() * 255),
                (int)(c.getGreen() * 255),
                (int)(c.getBlue() * 255));
    }

    // ══════════════════════════════════════════════
    // DATE RANGE HANDLERS
    // ══════════════════════════════════════════════
    @FXML private void handleRange7d()  { setPreset(7);   }
    @FXML private void handleRange30d() { setPreset(30);  }
    @FXML private void handleRange3m()  { setPreset(90);  }
    @FXML private void handleRange6m()  { setPreset(180); }
    @FXML private void handleRange1y()  { setPreset(365); }

    private void setPreset(int days) {
        activeTo   = LocalDate.now();
        activeFrom = activeTo.minusDays(days - 1);
        dateFrom.setValue(activeFrom);
        dateTo.setValue(activeTo);

        // Update active button style
        List<Button> presets = List.of(btnRange7d, btnRange30d,
                btnRange3m, btnRange6m, btnRange1y);
        int[] daysMap = {7, 30, 90, 180, 365};
        for (int i = 0; i < presets.size(); i++) {
            presets.get(i).getStyleClass().remove("active");
            if (daysMap[i] == days)
                presets.get(i).getStyleClass().add("active");
        }
        refreshStats();
        rebuildCharts();
    }

    @FXML
    private void handleApply() {
        // Clear all preset active states
        for (Button b : List.of(btnRange7d, btnRange30d,
                btnRange3m, btnRange6m, btnRange1y)) {
            b.getStyleClass().remove("active");
        }
        if (dateFrom.getValue() != null) activeFrom = dateFrom.getValue();
        if (dateTo.getValue()   != null) activeTo   = dateTo.getValue();
        refreshStats();
        rebuildCharts();
    }

    // ══════════════════════════════════════════════
    // MISC HANDLERS
    // ══════════════════════════════════════════════
    private void rebuildCharts() {
        if (donutChartArea.getParent() != null)
            drawDonutChart(donutChartArea);
        if (barChartArea.getParent() != null)
            drawBarChart(barChartArea);
    }

    @FXML private void handleMoreDonut() { SceneManager.navigateTo(SceneManager.Screen.REPORTS); }
    @FXML private void handleMoreBar()   { SceneManager.navigateTo(SceneManager.Screen.REPORTS); }

    @FXML
    private void handleUserChip(MouseEvent e) {
        UserPopupUtil.show((javafx.scene.Node) e.getSource(),
                analyticsRoot.getScene().getWindow());
    }
    @FXML private void handleNotifications() { notifDot.setVisible(false); }
    @FXML private void handleExport()    { SceneManager.navigateTo(SceneManager.Screen.REPORTS); }

    // ══════════════════════════════════════════════
    // NAV HANDLERS
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

    private void setActiveNav(Button selected) {
        for (Button b : new Button[]{navOverview, navTransactions, navAnalytics,
                navBudgets, navReports, navSettings, navHelp}) {
            b.getStyleClass().remove("active");
        }
        if (!selected.getStyleClass().contains("active"))
            selected.getStyleClass().add("active");
    }
}
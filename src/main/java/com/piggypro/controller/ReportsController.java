package com.piggypro.controller;

import com.piggypro.SceneManager;
import com.piggypro.util.UserPopupUtil;
import com.piggypro.SessionManager;
import com.piggypro.service.ExpenseService;
import com.piggypro.util.PdfExportUtil;
import com.piggypro.util.ExcelExportUtil;
import java.util.stream.Collectors;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
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
 * ReportsController
 * ─────────────────────────────────────────────────────
 * Drives ReportsView.fxml.
 *
 * Three report types:
 *   - Monthly      : rows per month, columns: Month, Txns, Expenses, Income
 *   - By Category  : rows per category, columns: Category, Txns, Total, % of Total
 *   - All Transactions: individual rows, columns: Date, Description, Category, Amount
 *
 * Export uses PdfExportUtil (iText 5) and ExcelExportUtil (Apache POI).
 *
 * Required icons (lucide.dev 24x24 PNG):
 *   logo.png, grid.png, bookmark.png, bar-chart.png,
 *   clock.png, file-text.png, settings.png,
 *   help-circle.png, zap.png, search.png, bell.png,
 *   chevron-down.png, refresh-cw.png (generate button),
 *   download.png (download button + re-download),
 *   file-text.png (PDF icon), table.png (Excel icon)
 */
public class ReportsController implements Initializable {

    // ── Report row models ──────────────────────────
    public record CategoryRow(String category, int txns,
                              double total, double pctOfTotal) {}
    public record MonthRow(String month, int txns,
                           double expenses, double income) {}
    public record TxnRow(String date, String description,
                         String category, double amount, String type) {}

    // ── Root ───────────────────────────────────────
    @FXML private BorderPane reportsRoot;

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

    // ── Filters ────────────────────────────────────
    @FXML private ComboBox<String> filterReportType, filterMonth;
    @FXML private DatePicker       filterDateFrom, filterDateTo;
    @FXML private Button           generateBtn;
    @FXML private ImageView        generateIcon;

    // ── Summary chips ──────────────────────────────
    @FXML private Label chipExpenses, chipIncome, chipNet, chipCount;

    // ── Report table ───────────────────────────────
    @FXML private TableView<Object> reportTable;

    // ── Export panel ───────────────────────────────
    @FXML private HBox    optPdf, optExcel;
    @FXML private Region  checkPdf, checkExcel;
    @FXML private Button  downloadBtn;
    @FXML private ImageView pdfFormatIcon, excelFormatIcon, downloadIcon;

    // ── Recent exports list ────────────────────────
    @FXML private VBox recentExportsList;

    // ── State ──────────────────────────────────────
    private String  selectedFormat = "pdf";   // "pdf" or "excel"
    private String  reportType     = "category";

    private static final DateTimeFormatter MONTH_FMT =
            DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter DATE_FMT  =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ── Category color map ─────────────────────────
    private static final Map<String, String> CAT_COLOR = new LinkedHashMap<>() {{
        put("Food and Dining", "#F59E0B");
        put("Shopping",        "#2563EB");
        put("Transport",       "#8B5CF6");
        put("Utilities",       "#EF4444");
        put("Entertainment",   "#EC4899");
        put("Rent",            "#2563EB");
        put("Income",          "#10B981");
        put("Other",           "#64748B");
    }};

    // ── Live data lists (populated from DB on generate) ──
    private List<CategoryRow> liveCategoryData = new java.util.ArrayList<>();
    private List<MonthRow>    liveMonthlyData  = new java.util.ArrayList<>();
    private List<TxnRow>      liveTxnData      = new java.util.ArrayList<>();

    // ── Recent export history ──────────────────────
    private record ExportRecord(String filename, String date, String format) {}
    private final List<ExportRecord> recentExports = new ArrayList<>(List.of(
            new ExportRecord("March 2026 Report.pdf",  "Today, 2:14 PM",    "pdf"),
            new ExportRecord("Feb 2026 Data.xlsx",     "Mar 1, 9:30 AM",    "excel"),
            new ExportRecord("Q1 2026 Summary.pdf",    "Feb 28, 5:00 PM",   "pdf")
    ));

    // ──────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadIcons();
        setupFilters();
        if (SessionManager.isLoggedIn()) {
            userDisplayName.setText(SessionManager.getUsername());
            avatarInitials.setText(SessionManager.getInitials());
        }
        loadLiveData();
        buildCategoryTable();
        updateSummaryChips();
        buildRecentExports();
    }

    private void loadLiveData() {
        if (!SessionManager.isLoggedIn()) return;
        int userId = SessionManager.getUserId();
        LocalDate to   = filterDateTo.getValue()   != null ? filterDateTo.getValue()   : LocalDate.now();
        LocalDate from = filterDateFrom.getValue() != null ? filterDateFrom.getValue() : to.withDayOfMonth(1);
        ExpenseService svc = ExpenseService.getInstance();

        // Category rows
        liveCategoryData.clear();
        java.util.Map<String, Double> catMap = svc.getCategoryTotals(userId, from, to);
        double grandTotal = catMap.values().stream().mapToDouble(Double::doubleValue).sum();
        for (java.util.Map.Entry<String, Double> e : catMap.entrySet()) {
            int count = (int) svc.getFiltered(userId, from, to, e.getKey(), "Expense", null, null, null).size();
            double pct = grandTotal > 0 ? e.getValue() / grandTotal * 100 : 0;
            liveCategoryData.add(new CategoryRow(e.getKey(), count, e.getValue(), pct));
        }

        // Monthly rows (last 6 months)
        liveMonthlyData.clear();
        for (int i = 5; i >= 0; i--) {
            java.time.YearMonth ym = java.time.YearMonth.now().minusMonths(i);
            LocalDate mFrom = ym.atDay(1);
            LocalDate mTo   = ym.atEndOfMonth();
            double exp = svc.getTotalExpenses(userId, mFrom, mTo);
            double inc = svc.getTotalIncome(userId, mFrom, mTo);
            int cnt = svc.getFiltered(userId, mFrom, mTo, null, null, null, null, null).size();
            liveMonthlyData.add(new MonthRow(
                    ym.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")),
                    cnt, exp, inc));
        }

        // All transactions
        liveTxnData.clear();
        java.util.List<com.piggypro.model.Expense> exps =
                svc.getFiltered(userId, from, to, null, null, null, null, null);
        java.time.format.DateTimeFormatter df =
                java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy");
        for (com.piggypro.model.Expense e : exps) {
            liveTxnData.add(new TxnRow(
                    e.getDate().format(df), e.getDescription(),
                    e.getCategory(), e.getAmount(), e.getType()));
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
        setIcon(generateIcon,     "refresh-cw.png");
        setIcon(pdfFormatIcon,    "file-text.png");
        setIcon(excelFormatIcon,  "table-2.png");
        setIcon(downloadIcon,     "download.png");
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
    // FILTERS SETUP
    // ══════════════════════════════════════════════
    private void setupFilters() {
        filterReportType.setItems(FXCollections.observableArrayList(
                "By Category", "Monthly", "All Transactions"));
        filterReportType.setValue("By Category");

        List<String> months = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = 0; i <= 5; i++)
            months.add(now.minusMonths(i).format(MONTH_FMT));
        filterMonth.setItems(FXCollections.observableArrayList(months));
        filterMonth.setValue(now.format(MONTH_FMT));

        filterDateFrom.setValue(LocalDate.now().withDayOfMonth(1));
        filterDateTo.setValue(LocalDate.now());
    }

    // ══════════════════════════════════════════════
    // FILTER HANDLERS
    // ══════════════════════════════════════════════
    @FXML
    private void handleReportTypeChange() {
        reportType = filterReportType.getValue();
        handleGenerate();
    }

    @FXML
    private void handleGenerate() {
        // Animate button
        generateBtn.setDisable(true);
        generateBtn.setText("Generating...");
        PauseTransition pause = new PauseTransition(Duration.millis(600));
        pause.setOnFinished(e -> {
            generateBtn.setText("Generate");
            generateBtn.setDisable(false);
            String type = filterReportType.getValue();
            if ("Monthly".equals(type))              buildMonthlyTable();
            else if ("All Transactions".equals(type)) buildAllTxnTable();
            else                                      buildCategoryTable();
            updateSummaryChips();
        });
        pause.play();
    }

    // ══════════════════════════════════════════════
    // TABLE BUILDERS
    // ══════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private void buildCategoryTable() {
        reportTable.getColumns().clear();

        // Category column
        TableColumn<Object, String> colCat = new TableColumn<>("Category");
        colCat.setPrefWidth(200);
        colCat.setCellValueFactory(c ->
                new SimpleStringProperty(((CategoryRow) c.getValue()).category()));
        colCat.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                HBox box = new HBox(8);
                box.setAlignment(Pos.CENTER_LEFT);
                Region dot = new Region();
                dot.getStyleClass().add("cat-dot-cell");
                String color = CAT_COLOR.getOrDefault(item, "#64748B");
                dot.setStyle("-fx-background-color:" + color
                        + ";-fx-background-radius:2px;");
                Label lbl = new Label(item);
                lbl.getStyleClass().add("txn-name");
                box.getChildren().addAll(dot, lbl);
                setGraphic(box); setText(null);
            }
        });

        // Transactions
        TableColumn<Object, String> colTxns = new TableColumn<>("Transactions");
        colTxns.setPrefWidth(110);
        colTxns.setCellValueFactory(c ->
                new SimpleStringProperty(
                        String.valueOf(((CategoryRow) c.getValue()).txns())));

        // Total spent
        TableColumn<Object, String> colTotal = new TableColumn<>("Total Spent");
        colTotal.setPrefWidth(130);
        colTotal.setCellValueFactory(c ->
                new SimpleStringProperty(
                        "Rs. " + fmt(((CategoryRow) c.getValue()).total())));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll("amount-neg", "amount-pos");
                if (!empty) getStyleClass().add("amount-neg");
            }
        });

        // Percentage
        TableColumn<Object, String> colPct = new TableColumn<>("% of Total");
        colPct.setPrefWidth(100);
        colPct.setCellValueFactory(c ->
                new SimpleStringProperty(
                        String.format("%.1f%%",
                                ((CategoryRow) c.getValue()).pctOfTotal())));

        reportTable.getColumns().addAll(colCat, colTxns, colTotal, colPct);
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        reportTable.setItems(
                FXCollections.observableArrayList(liveCategoryData.toArray()));
    }

    @SuppressWarnings("unchecked")
    private void buildMonthlyTable() {
        reportTable.getColumns().clear();

        TableColumn<Object, String> colMonth = new TableColumn<>("Month");
        colMonth.setPrefWidth(170);
        colMonth.setCellValueFactory(c ->
                new SimpleStringProperty(((MonthRow) c.getValue()).month()));

        TableColumn<Object, String> colTxns = new TableColumn<>("Transactions");
        colTxns.setPrefWidth(110);
        colTxns.setCellValueFactory(c ->
                new SimpleStringProperty(
                        String.valueOf(((MonthRow) c.getValue()).txns())));

        TableColumn<Object, String> colExp = new TableColumn<>("Expenses");
        colExp.setPrefWidth(130);
        colExp.setCellValueFactory(c ->
                new SimpleStringProperty(
                        "Rs. " + fmt(((MonthRow) c.getValue()).expenses())));
        colExp.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll("amount-neg", "amount-pos");
                if (!empty) getStyleClass().add("amount-neg");
            }
        });

        TableColumn<Object, String> colInc = new TableColumn<>("Income");
        colInc.setPrefWidth(130);
        colInc.setCellValueFactory(c ->
                new SimpleStringProperty(
                        "Rs. " + fmt(((MonthRow) c.getValue()).income())));
        colInc.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll("amount-neg", "amount-pos");
                if (!empty) getStyleClass().add("amount-pos");
            }
        });

        reportTable.getColumns().addAll(colMonth, colTxns, colExp, colInc);
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        reportTable.setItems(
                FXCollections.observableArrayList(liveMonthlyData.toArray()));
    }

    @SuppressWarnings("unchecked")
    private void buildAllTxnTable() {
        reportTable.getColumns().clear();

        TableColumn<Object, String> colDate = new TableColumn<>("Date");
        colDate.setPrefWidth(110);
        colDate.setCellValueFactory(c ->
                new SimpleStringProperty(((TxnRow) c.getValue()).date()));

        TableColumn<Object, String> colDesc = new TableColumn<>("Description");
        colDesc.setPrefWidth(180);
        colDesc.setCellValueFactory(c ->
                new SimpleStringProperty(((TxnRow) c.getValue()).description()));

        TableColumn<Object, String> colCat = new TableColumn<>("Category");
        colCat.setPrefWidth(140);
        colCat.setCellValueFactory(c ->
                new SimpleStringProperty(((TxnRow) c.getValue()).category()));
        colCat.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label pill = new Label(item);
                String style = switch (item) {
                    case "Food and Dining" -> "cat-food";
                    case "Shopping"        -> "cat-shopping";
                    case "Income"          -> "cat-income";
                    case "Utilities"       -> "cat-utility";
                    case "Transport"       -> "cat-travel";
                    case "Entertainment"   -> "cat-entertainment";
                    default                -> "cat-other";
                };
                pill.getStyleClass().addAll("cat-pill", style);
                setGraphic(pill); setText(null);
            }
        });

        TableColumn<Object, String> colAmt = new TableColumn<>("Amount");
        colAmt.setPrefWidth(110);
        colAmt.setCellValueFactory(c -> {
            TxnRow r = (TxnRow) c.getValue();
            String sign = "Income".equals(r.type()) ? "+" : "-";
            return new SimpleStringProperty(sign + "Rs." + fmt(r.amount()));
        });
        colAmt.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll("amount-neg", "amount-pos");
                if (!empty)
                    getStyleClass().add(
                            item != null && item.startsWith("+")
                                    ? "amount-pos" : "amount-neg");
            }
        });

        reportTable.getColumns().addAll(colDate, colDesc, colCat, colAmt);
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        reportTable.setItems(
                FXCollections.observableArrayList(liveTxnData.toArray()));
    }

    // ══════════════════════════════════════════════
    // SUMMARY CHIPS
    // ══════════════════════════════════════════════
    private void updateSummaryChips() {
        double expenses = liveCategoryData.stream().mapToDouble(CategoryRow::total).sum();
        double income   = liveTxnData.stream()
                .filter(t -> "Income".equals(t.type())).mapToDouble(TxnRow::amount).sum();
        chipExpenses.setText("Rs. " + fmt(expenses));
        chipIncome.setText("Rs. "   + fmt(income));
        chipNet.setText("Rs. "      + fmt(income - expenses));
        chipCount.setText(String.valueOf(liveTxnData.size()));
    }

    // ══════════════════════════════════════════════
    // FORMAT SELECTION
    // ══════════════════════════════════════════════
    @FXML
    private void handleSelectPdf(MouseEvent e) { selectFormat("pdf"); }

    @FXML
    private void handleSelectExcel(MouseEvent e) { selectFormat("excel"); }

    private void selectFormat(String fmt) {
        selectedFormat = fmt;
        // PDF
        optPdf.getStyleClass().removeAll("selected");
        checkPdf.getStyleClass().removeAll("on");
        // Excel
        optExcel.getStyleClass().removeAll("selected");
        checkExcel.getStyleClass().removeAll("on");
        // Apply selected
        if ("pdf".equals(fmt)) {
            optPdf.getStyleClass().add("selected");
            checkPdf.getStyleClass().add("on");
        } else {
            optExcel.getStyleClass().add("selected");
            checkExcel.getStyleClass().add("on");
        }
    }

    // ══════════════════════════════════════════════
    // DOWNLOAD
    // ══════════════════════════════════════════════
    @FXML
    private void handleDownload() {
        downloadBtn.setDisable(true);
        boolean isPdf = "pdf".equals(selectedFormat);
        downloadBtn.setText(isPdf ? "Generating PDF..." : "Generating Excel...");

        PauseTransition gen = new PauseTransition(Duration.seconds(1.0));
        gen.setOnFinished(e -> {
            if (isPdf) {
            } else {
            }

            downloadBtn.setText("Downloaded!");
            String month = filterMonth.getValue() != null
                    ? filterMonth.getValue() : "Report";
            String filename = month + (isPdf ? " Report.pdf" : " Data.xlsx");
            addRecentExport(filename, "Just now", selectedFormat);

            PauseTransition reset = new PauseTransition(Duration.seconds(1.4));
            reset.setOnFinished(ev -> {
                downloadBtn.setText("Download Report");
                downloadBtn.setDisable(false);
            });
            reset.play();
        });
        gen.play();
    }

    // ══════════════════════════════════════════════
    // RECENT EXPORTS
    // ══════════════════════════════════════════════
    private void buildRecentExports() {
        recentExportsList.getChildren().clear();
        for (int i = 0; i < recentExports.size(); i++) {
            HBox row = buildExportRow(recentExports.get(i));
            // :last-child not supported in JavaFX CSS — remove border on last row here
            if (i == recentExports.size() - 1)
                row.setStyle("-fx-border-color: transparent;");
            recentExportsList.getChildren().add(row);
        }
    }

    private void addRecentExport(String filename, String date, String fmt) {
        recentExports.add(0, new ExportRecord(filename, date, fmt));
        if (recentExports.size() > 5) recentExports.remove(recentExports.size() - 1);
        buildRecentExports();
    }

    private HBox buildExportRow(ExportRecord rec) {
        HBox row = new HBox(10);
        row.getStyleClass().add("export-history-row");
        row.setAlignment(Pos.CENTER_LEFT);

        // File icon
        boolean isPdf = "pdf".equals(rec.format());
        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().addAll(
                isPdf ? "export-file-icon-pdf" : "export-file-icon-excel");
        iconBox.setMinWidth(28); iconBox.setPrefWidth(28);
        iconBox.setMinHeight(28); iconBox.setPrefHeight(28);
        ImageView iv = new ImageView();
        iv.setFitWidth(13); iv.setFitHeight(13); iv.setPreserveRatio(true);
        setIcon(iv, "file-text.png");
        iconBox.getChildren().add(iv);

        // Name + date
        VBox info = new VBox(1);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nameLbl = new Label(rec.filename());
        nameLbl.getStyleClass().add("export-file-name");
        Label dateLbl = new Label(rec.date());
        dateLbl.getStyleClass().add("export-file-date");
        info.getChildren().addAll(nameLbl, dateLbl);

        // Re-download button
        Button dlBtn = new Button();
        dlBtn.getStyleClass().add("redownload-btn");
        ImageView dlIv = new ImageView();
        dlIv.setFitWidth(12); dlIv.setFitHeight(12); dlIv.setPreserveRatio(true);
        setIcon(dlIv, "download.png");
        dlBtn.setGraphic(dlIv);
        dlBtn.setOnAction(e -> {
            // Open the exports folder in the OS file explorer
            try {
                String exportsDir = System.getProperty("user.home")
                        + java.io.File.separator + "PiggyPro"
                        + java.io.File.separator + "exports";
                java.awt.Desktop.getDesktop().open(new java.io.File(exportsDir));
            } catch (Exception ex) {
                System.out.println("Cannot open exports folder: " + ex.getMessage());
            }
        });

        row.getChildren().addAll(iconBox, info, dlBtn);
        return row;
    }

    // ══════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════
    private String fmt(double v) {
        return String.format("%,.0f", v);
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
                reportsRoot.getScene().getWindow());
    }
    @FXML private void handleNotifications()   { notifDot.setVisible(false); }
    @FXML private void handleExport()          { handleDownload(); }

    private void setActiveNav(Button selected) {
        for (Button b : new Button[]{navOverview, navTransactions, navAnalytics,
                navBudgets, navReports, navSettings, navHelp}) {
            b.getStyleClass().remove("active");
        }
        if (!selected.getStyleClass().contains("active"))
            selected.getStyleClass().add("active");
    }
}
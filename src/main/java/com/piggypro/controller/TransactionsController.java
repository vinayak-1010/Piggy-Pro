package com.piggypro.controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * TransactionsController
 * ─────────────────────────────────────────────────────
 * Handles the Transactions screen: inline add/edit form,
 * filter bar, summary chips, TableView, and pagination.
 *
 * Icons (24x24 PNG from lucide.dev) go in:
 *   src/main/resources/com/piggypro/images/icons/
 *
 * Required icons (in addition to shared dashboard icons):
 *   plus.png          add button icon
 *   x.png             close form button
 *   edit.png          row edit button
 *   trash.png         row delete button
 *   chevron-left.png  pagination previous
 *   chevron-right.png pagination next
 */
public class TransactionsController implements Initializable {

    // ── Data model ─────────────────────────────────
    public static class Transaction {
        private String id;
        private String description;
        private double amount;
        private String type;       // "Expense" or "Income"
        private String category;
        private LocalDate date;
        private String note;

        public Transaction(String id, String description, double amount,
                           String type, String category,
                           LocalDate date, String note) {
            this.id = id; this.description = description;
            this.amount = amount; this.type = type;
            this.category = category; this.date = date;
            this.note = note;
        }
        public String getId()          { return id; }
        public String getDescription() { return description; }
        public double getAmount()      { return amount; }
        public String getType()        { return type; }
        public String getCategory()    { return category; }
        public LocalDate getDate()     { return date; }
        public String getNote()        { return note; }

        public void setDescription(String v) { description = v; }
        public void setAmount(double v)      { amount = v; }
        public void setType(String v)        { type = v; }
        public void setCategory(String v)    { category = v; }
        public void setDate(LocalDate v)     { date = v; }
        public void setNote(String v)        { note = v; }
    }

    // ── Root ───────────────────────────────────────
    @FXML private BorderPane transactionsRoot;

    // ── Sidebar ────────────────────────────────────
    @FXML private Button navOverview, navTransactions, navAnalytics;
    @FXML private Button navBudgets, navReports, navSettings, navHelp;
    @FXML private Button btnExport;

    // ── Sidebar icons ──────────────────────────────
    @FXML private ImageView sidebarLogoIcon;
    @FXML private ImageView iconOverview, iconTransactions, iconAnalytics;
    @FXML private ImageView iconBudgets, iconReports, iconSettings, iconHelp;
    @FXML private ImageView iconExport;

    // ── Topbar ─────────────────────────────────────
    @FXML private TextField  topSearchField;
    @FXML private Button     notifBtn;
    @FXML private Circle     notifDot;
    @FXML private Label      avatarInitials, userDisplayName;
    @FXML private ImageView  searchIcon, notifIcon, chevronIcon;

    // ── Page header ────────────────────────────────
    @FXML private Button    addTxnBtn;
    @FXML private ImageView addIcon;

    // ── Inline form ────────────────────────────────
    @FXML private VBox      txnFormCard;
    @FXML private Label     formHeadingLabel;
    @FXML private Button    closeFormBtn, btnSave;
    @FXML private ImageView formHeaderIcon, closeIcon;
    @FXML private TextField fieldDescription, fieldAmount, fieldNote;
    @FXML private ComboBox<String> fieldType, fieldCategory;
    @FXML private DatePicker fieldDate;
    @FXML private Label     formError;

    // ── Filter bar ─────────────────────────────────
    @FXML private ComboBox<String> filterCategory, filterType, filterAmount;
    @FXML private DatePicker filterDateFrom, filterDateTo;

    // ── Summary chips ──────────────────────────────
    @FXML private Label chipTotal, chipExpenses, chipIncome, chipNet;

    // ── Table ──────────────────────────────────────
    @FXML private TableView<Transaction> transactionTable;

    // ── Pagination ─────────────────────────────────
    @FXML private Label  pagInfoLabel;
    @FXML private Button pagPrev, pagNext;
    @FXML private Button pagBtn1, pagBtn2, pagBtn3, pagBtn4;
    @FXML private ImageView pagPrevIcon, pagNextIcon;

    // ── State ──────────────────────────────────────
    private final ObservableList<Transaction> allTransactions =
            FXCollections.observableArrayList();
    private FilteredList<Transaction> filteredList;

    private static final int PAGE_SIZE = 8;
    private int currentPage  = 0;
    private boolean editMode = false;
    private Transaction editingTx = null;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    private static final List<String> CATEGORIES = List.of(
            "Food and Dining", "Shopping", "Transport",
            "Utilities", "Entertainment", "Rent", "Income", "Other"
    );

    // ──────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadIcons();
        setupFormCombos();
        setupFilterCombos();
        setupTable();
        loadSampleData();
        applyFilters();
    }

    // ══════════════════════════════════════════════
    // ICON LOADING
    // ══════════════════════════════════════════════
    private void loadIcons() {
        // Sidebar
        setIcon(sidebarLogoIcon,  "logo.png");
        setIcon(iconOverview,     "grid.png");
        setIcon(iconTransactions, "bookmark.png");
        setIcon(iconAnalytics,    "bar-chart.png");
        setIcon(iconBudgets,      "clock.png");
        setIcon(iconReports,      "file-text.png");
        setIcon(iconSettings,     "settings.png");
        setIcon(iconHelp,         "help-circle.png");
        setIcon(iconExport,       "zap.png");
        // Topbar
        setIcon(searchIcon,       "search.png");
        setIcon(notifIcon,        "bell.png");
        setIcon(chevronIcon,      "chevron-down.png");
        // Form
        setIcon(addIcon,          "plus.png");
        setIcon(formHeaderIcon,   "plus.png");
        setIcon(closeIcon,        "x.png");
        // Pagination
        setIcon(pagPrevIcon,      "chevron-left.png");
        setIcon(pagNextIcon,      "chevron-right.png");
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
        fieldType.setItems(FXCollections.observableArrayList("Expense", "Income"));
        fieldType.setValue("Expense");
        fieldCategory.setItems(FXCollections.observableArrayList(CATEGORIES));
        fieldCategory.setValue("Food and Dining");
        fieldDate.setValue(LocalDate.now());
    }

    private void setupFilterCombos() {
        filterCategory.setItems(FXCollections.observableArrayList(
                List.of("All Categories", "Food and Dining", "Shopping",
                        "Transport", "Utilities", "Entertainment",
                        "Rent", "Income", "Other")));
        filterCategory.setValue("All Categories");

        filterType.setItems(FXCollections.observableArrayList(
                "All Types", "Expense", "Income"));
        filterType.setValue("All Types");

        filterAmount.setItems(FXCollections.observableArrayList(
                "Any Amount", "Under Rs.500",
                "Rs.500 - Rs.2,000", "Above Rs.2,000"));
        filterAmount.setValue("Any Amount");
    }

    @SuppressWarnings("unchecked")
    private void setupTable() {
        // Description column
        TableColumn<Transaction, String> colDesc = new TableColumn<>("Description");
        colDesc.setPrefWidth(220);
        colDesc.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDescription()));
        colDesc.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Transaction tx = getTableView().getItems().get(getIndex());
                HBox box = buildDescriptionCell(tx);
                setGraphic(box);
                setText(null);
            }
        });

        // Category column
        TableColumn<Transaction, String> colCat = new TableColumn<>("Category");
        colCat.setPrefWidth(140);
        colCat.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCategory()));
        colCat.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label pill = new Label(item);
                pill.getStyleClass().addAll("cat-pill", getCatStyle(item));
                setGraphic(pill);
                setText(null);
            }
        });

        // Amount column
        TableColumn<Transaction, String> colAmt = new TableColumn<>("Amount");
        colAmt.setPrefWidth(130);
        colAmt.setCellValueFactory(c -> {
            Transaction tx = c.getValue();
            String sign = tx.getType().equals("Income") ? "+" : "-";
            return new SimpleStringProperty(
                    sign + "Rs." + String.format("%,.0f", tx.getAmount()));
        });
        colAmt.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item);
                getStyleClass().removeAll("amount-neg", "amount-pos");
                getStyleClass().add(item.startsWith("+") ? "amount-pos" : "amount-neg");
            }
        });

        // Date column
        TableColumn<Transaction, String> colDate = new TableColumn<>("Date");
        colDate.setPrefWidth(120);
        colDate.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDate().format(DATE_FMT)));

        // Note column
        TableColumn<Transaction, String> colNote = new TableColumn<>("Note");
        colNote.setPrefWidth(180);
        colNote.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNote()));

        // Actions column
        TableColumn<Transaction, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(90);
        colActions.setSortable(false);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = buildActionBtn("edit");
            private final Button deleteBtn = buildActionBtn("delete");
            private final HBox   box       = new HBox(6, editBtn, deleteBtn);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                editBtn.setOnAction(e -> {
                    Transaction tx = getTableView().getItems().get(getIndex());
                    openEditForm(tx);
                });
                deleteBtn.setOnAction(e -> {
                    Transaction tx = getTableView().getItems().get(getIndex());
                    deleteTransaction(tx);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        transactionTable.getColumns().addAll(
                colDesc, colCat, colAmt, colDate, colNote, colActions);
        transactionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        transactionTable.setPlaceholder(buildEmptyState());
    }

    // ══════════════════════════════════════════════
    // SAMPLE DATA
    // ══════════════════════════════════════════════
    private void loadSampleData() {
        allTransactions.addAll(
                new Transaction("1","Salary Credit",      45000, "Income",  "Income",        LocalDate.of(2026,3,14), "Monthly salary"),
                new Transaction("2","Amazon Shopping",     1240, "Expense", "Shopping",      LocalDate.of(2026,3,14), "Electronics accessories"),
                new Transaction("3","Swiggy Order",         340, "Expense", "Food and Dining",LocalDate.of(2026,3,13),"Dinner for two"),
                new Transaction("4","Phone Recharge",       399, "Expense", "Utilities",     LocalDate.of(2026,3,12), "Airtel prepaid 84-day"),
                new Transaction("5","Uber Ride",            180, "Expense", "Transport",     LocalDate.of(2026,3,11), "Office commute"),
                new Transaction("6","House Rent",          8500, "Expense", "Rent",          LocalDate.of(2026,3,10), "March rent payment"),
                new Transaction("7","Netflix Subscription", 649, "Expense", "Entertainment", LocalDate.of(2026,3,9),  "Monthly plan"),
                new Transaction("8","Zomato Order",         285, "Expense", "Food and Dining",LocalDate.of(2026,3,8), "Lunch delivery"),
                new Transaction("9","Electricity Bill",    1100, "Expense", "Utilities",     LocalDate.of(2026,3,7),  "BSES March bill"),
                new Transaction("10","Metro Card Recharge", 500, "Expense", "Transport",     LocalDate.of(2026,3,6),  "Monthly commute"),
                new Transaction("11","Freelance Payment", 12000, "Income",  "Income",        LocalDate.of(2026,3,5),  "Website project"),
                new Transaction("12","Grocery Shopping",   2100, "Expense", "Shopping",      LocalDate.of(2026,3,4),  "Weekly grocery run"),
                new Transaction("13","Coffee Shop",         180, "Expense", "Food and Dining",LocalDate.of(2026,3,3), "Work meeting"),
                new Transaction("14","Gym Membership",      800, "Expense", "Entertainment", LocalDate.of(2026,3,2),  "Monthly membership"),
                new Transaction("15","Book Purchase",       450, "Expense", "Shopping",      LocalDate.of(2026,3,1),  "Clean Code by R.Martin")
        );
    }

    // ══════════════════════════════════════════════
    // FORM — TOGGLE / OPEN / CLOSE
    // ══════════════════════════════════════════════
    @FXML
    private void handleToggleForm() {
        if (txnFormCard.isVisible()) {
            closeForm();
        } else {
            openAddForm();
        }
    }

    private void openAddForm() {
        editMode = false;
        editingTx = null;
        formHeadingLabel.setText("Add New Transaction");
        setIcon(formHeaderIcon, "plus.png");
        clearFormFields();
        formError.setText("");
        showForm();
    }

    private void openEditForm(Transaction tx) {
        editMode  = true;
        editingTx = tx;
        formHeadingLabel.setText("Edit Transaction");
        setIcon(formHeaderIcon, "edit.png");
        fieldDescription.setText(tx.getDescription());
        fieldAmount.setText(String.format("%.2f", tx.getAmount()));
        fieldType.setValue(tx.getType());
        fieldCategory.setValue(tx.getCategory());
        fieldDate.setValue(tx.getDate());
        fieldNote.setText(tx.getNote());
        formError.setText("");
        showForm();
        txnFormCard.requestFocus();
    }

    private void showForm() {
        txnFormCard.setOpacity(0);
        txnFormCard.setVisible(true);
        txnFormCard.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(180), txnFormCard);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void closeForm() {
        FadeTransition ft = new FadeTransition(Duration.millis(150), txnFormCard);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            txnFormCard.setVisible(false);
            txnFormCard.setManaged(false);
        });
        ft.play();
        editMode  = false;
        editingTx = null;
    }

    // ══════════════════════════════════════════════
    // FORM — SAVE
    // ══════════════════════════════════════════════
    @FXML
    private void handleSave() {
        formError.setText("");

        String desc   = fieldDescription.getText().trim();
        String amtStr = fieldAmount.getText().trim();
        String type   = fieldType.getValue();
        String cat    = fieldCategory.getValue();
        LocalDate date = fieldDate.getValue();
        String note   = fieldNote.getText().trim();

        // Validation
        if (desc.isEmpty()) {
            showFormError("Description is required.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amtStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showFormError("Please enter a valid positive amount.");
            return;
        }
        if (date == null) {
            showFormError("Please select a date.");
            return;
        }

        if (editMode && editingTx != null) {
            // Update existing
            editingTx.setDescription(desc);
            editingTx.setAmount(amount);
            editingTx.setType(type);
            editingTx.setCategory(cat);
            editingTx.setDate(date);
            editingTx.setNote(note);
            transactionTable.refresh();
        } else {
            // Add new
            String newId = String.valueOf(allTransactions.size() + 1);
            allTransactions.add(0,
                    new Transaction(newId, desc, amount, type, cat, date, note));
        }

        animateSaveSuccess();
        applyFilters();
    }

    private void showFormError(String msg) {
        formError.setText(msg);
        FadeTransition ft = new FadeTransition(Duration.millis(220), formError);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void animateSaveSuccess() {
        String original = btnSave.getText();
        btnSave.setText("Saved!");
        btnSave.setDisable(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        pause.setOnFinished(e -> {
            btnSave.setText(original);
            btnSave.setDisable(false);
            closeForm();
        });
        pause.play();
    }

    private void clearFormFields() {
        fieldDescription.clear();
        fieldAmount.clear();
        fieldType.setValue("Expense");
        fieldCategory.setValue("Food and Dining");
        fieldDate.setValue(LocalDate.now());
        fieldNote.clear();
    }

    // ══════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════
    private void deleteTransaction(Transaction tx) {
        allTransactions.remove(tx);
        applyFilters();
    }

    // ══════════════════════════════════════════════
    // FILTERS
    // ══════════════════════════════════════════════
    @FXML
    private void applyFilters() {
        String catFilter    = filterCategory.getValue();
        String typeFilter   = filterType.getValue();
        String amtFilter    = filterAmount.getValue();
        LocalDate from      = filterDateFrom.getValue();
        LocalDate to        = filterDateTo.getValue();
        String searchText   = topSearchField.getText().trim().toLowerCase();

        filteredList = new FilteredList<>(allTransactions, tx -> {
            // Category
            if (catFilter != null && !catFilter.equals("All Categories")
                    && !tx.getCategory().equals(catFilter)) return false;
            // Type
            if (typeFilter != null && !typeFilter.equals("All Types")
                    && !tx.getType().equals(typeFilter)) return false;
            // Date range
            if (from != null && tx.getDate().isBefore(from)) return false;
            if (to   != null && tx.getDate().isAfter(to))    return false;
            // Amount range
            if (amtFilter != null) {
                double a = tx.getAmount();
                switch (amtFilter) {
                    case "Under Rs.500"         -> { if (a >= 500)           return false; }
                    case "Rs.500 - Rs.2,000"    -> { if (a < 500 || a > 2000) return false; }
                    case "Above Rs.2,000"       -> { if (a <= 2000)          return false; }
                }
            }
            // Search
            if (!searchText.isEmpty()) {
                boolean match = tx.getDescription().toLowerCase().contains(searchText)
                        || tx.getCategory().toLowerCase().contains(searchText)
                        || tx.getNote().toLowerCase().contains(searchText);
                if (!match) return false;
            }
            return true;
        });

        currentPage = 0;
        updatePage();
        updateSummaryChips();
    }

    @FXML
    private void handleClearFilters() {
        filterCategory.setValue("All Categories");
        filterType.setValue("All Types");
        filterAmount.setValue("Any Amount");
        filterDateFrom.setValue(null);
        filterDateTo.setValue(null);
        topSearchField.clear();
        applyFilters();
    }

    @FXML
    private void handleTopSearch() {
        applyFilters();
    }

    // ══════════════════════════════════════════════
    // PAGINATION
    // ══════════════════════════════════════════════
    private void updatePage() {
        if (filteredList == null) return;

        int total    = filteredList.size();
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (currentPage >= totalPages && totalPages > 0)
            currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        int start = currentPage * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, total);

        ObservableList<Transaction> pageItems =
                FXCollections.observableArrayList(
                        filteredList.subList(start, Math.max(start, end)));
        transactionTable.setItems(pageItems);

        // Pagination info
        pagInfoLabel.setText(total == 0
                ? "No transactions found"
                : "Showing " + (start + 1) + "–" + end + " of " + total + " transactions");

        // Page buttons — show up to 4
        Button[] btns = {pagBtn1, pagBtn2, pagBtn3, pagBtn4};
        int firstPage = Math.max(0, Math.min(currentPage - 1, totalPages - 4));
        for (int i = 0; i < btns.length; i++) {
            int pg = firstPage + i;
            if (pg < totalPages) {
                btns[i].setText(String.valueOf(pg + 1));
                btns[i].setVisible(true);
                btns[i].setManaged(true);
                btns[i].getStyleClass().remove("active");
                if (pg == currentPage) btns[i].getStyleClass().add("active");
            } else {
                btns[i].setVisible(false);
                btns[i].setManaged(false);
            }
        }

        pagPrev.setDisable(currentPage == 0);
        pagNext.setDisable(currentPage >= totalPages - 1 || totalPages == 0);
    }

    @FXML
    private void handlePagPrev() {
        if (currentPage > 0) { currentPage--; updatePage(); }
    }

    @FXML
    private void handlePagNext() {
        currentPage++;
        updatePage();
    }

    @FXML
    private void handlePagBtn(javafx.event.ActionEvent e) {
        Button src = (Button) e.getSource();
        try {
            currentPage = Integer.parseInt(src.getText()) - 1;
            updatePage();
        } catch (NumberFormatException ignored) {}
    }

    // ══════════════════════════════════════════════
    // SUMMARY CHIPS
    // ══════════════════════════════════════════════
    private void updateSummaryChips() {
        if (filteredList == null) return;
        int    count    = filteredList.size();
        double expenses = filteredList.stream()
                .filter(t -> t.getType().equals("Expense"))
                .mapToDouble(Transaction::getAmount).sum();
        double income   = filteredList.stream()
                .filter(t -> t.getType().equals("Income"))
                .mapToDouble(Transaction::getAmount).sum();
        double net      = income - expenses;

        chipTotal.setText(String.valueOf(count));
        chipExpenses.setText("Rs. " + String.format("%,.0f", expenses));
        chipIncome.setText("Rs. "   + String.format("%,.0f", income));
        chipNet.setText("Rs. "      + String.format("%,.0f", net));
    }

    // ══════════════════════════════════════════════
    // CELL BUILDERS
    // ══════════════════════════════════════════════
    private HBox buildDescriptionCell(Transaction tx) {
        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().addAll("txn-icon-box", getIconBoxStyle(tx));
        ImageView icon = new ImageView();
        icon.setFitWidth(13); icon.setFitHeight(13);
        icon.setPreserveRatio(true);
        setIcon(icon, getTxnIconFile(tx.getCategory()));
        iconBox.getChildren().add(icon);

        Label name = new Label(tx.getDescription());
        name.getStyleClass().add("txn-name");

        HBox box = new HBox(9, iconBox, name);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Button buildActionBtn(String type) {
        Button btn = new Button();
        ImageView iv = new ImageView();
        iv.setFitWidth(13); iv.setFitHeight(13); iv.setPreserveRatio(true);
        setIcon(iv, type.equals("edit") ? "edit.png" : "trash.png");
        btn.setGraphic(iv);
        btn.getStyleClass().add(type.equals("edit") ? "row-edit-btn" : "row-delete-btn");
        return btn;
    }

    private Label buildEmptyState() {
        Label lbl = new Label("No transactions found. Add one to get started.");
        lbl.getStyleClass().add("form-section-sub");
        lbl.setStyle("-fx-font-size:13px;");
        return lbl;
    }

    // ── Helper: category → CSS style ──────────────
    private String getCatStyle(String cat) {
        return switch (cat) {
            case "Food and Dining" -> "cat-food";
            case "Shopping"        -> "cat-shopping";
            case "Income"          -> "cat-income";
            case "Utilities"       -> "cat-utility";
            case "Transport"       -> "cat-travel";
            case "Rent"            -> "cat-rent";
            case "Entertainment"   -> "cat-entertainment";
            default                -> "cat-other";
        };
    }

    // ── Helper: category → icon file ──────────────
    private String getTxnIconFile(String cat) {
        return switch (cat) {
            case "Food and Dining" -> "send.png";
            case "Shopping"        -> "shopping-bag.png";
            case "Income"          -> "credit-card.png";
            case "Utilities"       -> "phone.png";
            case "Transport"       -> "map-pin.png";
            case "Rent"            -> "home.png";
            case "Entertainment"   -> "monitor.png";
            default                -> "tag.png";
        };
    }

    // ── Helper: category → icon box tint ──────────
    private String getIconBoxStyle(Transaction tx) {
        return switch (tx.getCategory()) {
            case "Income"          -> "txn-icon-green";
            case "Shopping"        -> "txn-icon-blue";
            case "Food and Dining" -> "txn-icon-amber";
            case "Utilities"       -> "txn-icon-red";
            case "Transport"       -> "txn-icon-purple";
            default                -> "txn-icon-blue";
        };
    }

    // ══════════════════════════════════════════════
    // NAV HANDLERS
    // ══════════════════════════════════════════════
    @FXML private void handleNavOverview()     { setActiveNav(navOverview); }
    @FXML private void handleNavTransactions() { setActiveNav(navTransactions); }
    @FXML private void handleNavAnalytics()    { setActiveNav(navAnalytics); }
    @FXML private void handleNavBudgets()      { setActiveNav(navBudgets); }
    @FXML private void handleNavReports()      { setActiveNav(navReports); }
    @FXML private void handleNavSettings()     { setActiveNav(navSettings); }
    @FXML private void handleNavHelp()         { setActiveNav(navHelp); }
    @FXML private void handleNotifications()   { notifDot.setVisible(false); }
    @FXML private void handleExport()          { /* TODO: ExportService */ }

    private void setActiveNav(Button selected) {
        for (Button b : new Button[]{navOverview, navTransactions, navAnalytics,
                navBudgets, navReports, navSettings, navHelp}) {
            b.getStyleClass().remove("active");
        }
        if (!selected.getStyleClass().contains("active"))
            selected.getStyleClass().add("active");
    }
}

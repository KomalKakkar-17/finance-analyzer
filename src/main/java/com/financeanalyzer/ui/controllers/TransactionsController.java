package com.financeanalyzer.ui.controllers;

import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.Transaction;
import com.financeanalyzer.model.enums.TransactionType;
import com.financeanalyzer.service.InModalAnalyticsService;
import com.financeanalyzer.service.CategoryService;
import com.financeanalyzer.service.TransactionService;
import com.financeanalyzer.exception.DataAccessException;
import com.financeanalyzer.exception.InvalidFixedCategoryException;
import com.financeanalyzer.ui.AlertHelper;
import com.financeanalyzer.ui.components.SpendPreviewCard;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.DateUtil;
import com.financeanalyzer.util.MoneyUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Controller for the Transactions screen.
 * Implements FR-02, FR-03, FR-04, FR-05.
 * Features sortable TableView, comprehensive multi-criteria filtering, context menus,
 * and keyboard shortcut (Ctrl+N) for rapid data entry.
 */
public class TransactionsController {

    private static final Logger LOGGER = AppLogger.getLogger(TransactionsController.class);

    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final InModalAnalyticsService inModalAnalyticsService; // can be null in Sprint 1, passed in Sprint 3
    private final Long userId;
    private final Stage primaryStage;

    private TableView<Transaction> tableView;
    private final ObservableList<Transaction> tableData = FXCollections.observableArrayList();

    // Filters
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private ComboBox<Category> categoryFilterCombo;
    private ComboBox<String> typeFilterCombo;
    private TextField minAmountField;
    private TextField maxAmountField;
    private TextField keywordField;

    public TransactionsController(TransactionService transactionService,
                                  CategoryService categoryService,
                                  InModalAnalyticsService inModalAnalyticsService,
                                  Long userId,
                                  Stage primaryStage) {
        this.transactionService = Objects.requireNonNull(transactionService, "TransactionService must not be null.");
        this.categoryService = Objects.requireNonNull(categoryService, "CategoryService must not be null.");
        this.inModalAnalyticsService = inModalAnalyticsService;
        this.userId = Objects.requireNonNull(userId, "userId must not be null.");
        this.primaryStage = primaryStage;
    }

    public Parent getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("content-area");

        // Header and Primary Action
        HBox headerBox = new HBox(16);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Transactions");
        title.getStyleClass().add("card-title");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newTxnBtn = new Button("+ New Transaction (Ctrl+N)");
        newTxnBtn.getStyleClass().addAll("button", "primary-button");
        newTxnBtn.setOnAction(e -> handleAddTransaction());

        Button addCatBtn = new Button("+ Add Category");
        addCatBtn.getStyleClass().add("button");
        addCatBtn.setOnAction(e -> handleAddCategory());

        headerBox.getChildren().addAll(title, spacer, addCatBtn, newTxnBtn);

        // Filter Card
        VBox filterCard = createFilterBar();

        // Table View
        tableView = createTableView();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        root.getChildren().addAll(headerBox, filterCard, tableView);

        // Setup keyboard shortcut Ctrl+N on the scene when available
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                KeyCombination ctrlN = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
                newScene.getAccelerators().put(ctrlN, this::handleAddTransaction);
            }
        });

        // Initial Data Load
        refreshTable();

        return root;
    }

    private VBox createFilterBar() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));

        Label filterTitle = new Label("Search & Filters");
        filterTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text-muted; -fx-font-size: 11px;");

        // Row 1: Date Range, Type, Category
        HBox row1 = new HBox(12);
        row1.setAlignment(Pos.CENTER_LEFT);

        fromDatePicker = new DatePicker();
        fromDatePicker.setPromptText("From Date");
        fromDatePicker.setPrefWidth(140);

        toDatePicker = new DatePicker();
        toDatePicker.setPromptText("To Date");
        toDatePicker.setPrefWidth(140);

        typeFilterCombo = new ComboBox<>();
        typeFilterCombo.getItems().addAll("All Types", "EXPENSE", "INCOME");
        typeFilterCombo.setValue("All Types");
        typeFilterCombo.setPrefWidth(120);

        categoryFilterCombo = new ComboBox<>();
        categoryFilterCombo.setPromptText("All Categories");
        categoryFilterCombo.setPrefWidth(180);
        populateCategoryFilter();

        row1.getChildren().addAll(
                new Label("From:"), fromDatePicker,
                new Label("To:"), toDatePicker,
                new Label("Type:"), typeFilterCombo,
                new Label("Category:"), categoryFilterCombo
        );

        // Row 2: Amount Range, Notes Keyword, Actions
        HBox row2 = new HBox(12);
        row2.setAlignment(Pos.CENTER_LEFT);

        minAmountField = new TextField();
        minAmountField.setPromptText("Min \u20B9");
        minAmountField.setPrefWidth(90);

        maxAmountField = new TextField();
        maxAmountField.setPromptText("Max \u20B9");
        maxAmountField.setPrefWidth(90);

        keywordField = new TextField();
        keywordField.setPromptText("Keyword in notes or category");
        HBox.setHgrow(keywordField, Priority.ALWAYS);
        keywordField.setOnAction(e -> applyFilters());

        Button applyBtn = new Button("Filter");
        applyBtn.getStyleClass().addAll("button", "primary-button");
        applyBtn.setOnAction(e -> applyFilters());

        Button resetBtn = new Button("Reset");
        resetBtn.getStyleClass().add("button");
        resetBtn.setOnAction(e -> resetFilters());

        row2.getChildren().addAll(
                new Label("Amount:"), minAmountField, new Label("-"), maxAmountField,
                new Label("Search:"), keywordField,
                applyBtn, resetBtn
        );

        card.getChildren().addAll(filterTitle, row1, row2);
        return card;
    }

    private void populateCategoryFilter() {
        List<Category> allCats = transactionService.getCategories(userId, null);
        ObservableList<Category> options = FXCollections.observableArrayList();
        options.add(null); // represents "All Categories"
        options.addAll(allCats);
        categoryFilterCombo.setItems(options);
        categoryFilterCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All Categories" : item.getName());
            }
        });
        categoryFilterCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All Categories" : item.getName() + " (" + item.getType() + ")");
            }
        });
    }

    private TableView<Transaction> createTableView() {
        TableView<Transaction> table = new TableView<>(tableData);
        table.setPlaceholder(new Label("No transactions match your search criteria."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Date Column
        TableColumn<Transaction, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(DateUtil.formatDisplayDate(data.getValue().getDate())));
        dateCol.setPrefWidth(110);

        // Type Column with styled Badge
        TableColumn<Transaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType().name()));
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().addAll("badge", item.equals("INCOME") ? "badge-income" : "badge-expense");
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        typeCol.setPrefWidth(100);

        // Category Column
        TableColumn<Transaction, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoryName()));
        catCol.setPrefWidth(150);

        // Amount Column
        TableColumn<Transaction, String> amtCol = new TableColumn<>("Amount (\u20B9)");
        amtCol.setCellValueFactory(data -> new SimpleStringProperty(MoneyUtil.formatSignedCurrency(data.getValue().getSignedAmount())));
        amtCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER_RIGHT);
                    if (item.startsWith("+")) {
                        setStyle("-fx-text-fill: -color-success; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: -color-danger; -fx-font-weight: bold;");
                    }
                }
            }
        });
        amtCol.setPrefWidth(120);

        // Payment Mode Column
        TableColumn<Transaction, String> modeCol = new TableColumn<>("Payment Mode");
        modeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaymentMode() != null ? data.getValue().getPaymentMode() : "-"));
        modeCol.setPrefWidth(120);

        // Notes Column
        TableColumn<Transaction, String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNotes() != null ? data.getValue().getNotes() : ""));
        notesCol.setPrefWidth(200);

        table.getColumns().addAll(dateCol, typeCol, catCol, amtCol, modeCol, notesCol);

        // Context Menu on Rows
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit Transaction");
        editItem.setOnAction(e -> handleEditSelected());

        MenuItem deleteItem = new MenuItem("Delete Transaction");
        deleteItem.setOnAction(e -> handleDeleteSelected());

        contextMenu.getItems().addAll(editItem, deleteItem);

        table.setRowFactory(tv -> {
            TableRow<Transaction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    handleEditSelected();
                }
            });
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );
            return row;
        });

        return table;
    }

    public void refreshTable() {
        applyFilters();
    }

    private void applyFilters() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        Category cat = categoryFilterCombo.getValue();
        Long catId = (cat != null) ? cat.getId() : null;

        String typeStr = typeFilterCombo.getValue();
        TransactionType type = ("EXPENSE".equalsIgnoreCase(typeStr)) ? TransactionType.EXPENSE :
                ("INCOME".equalsIgnoreCase(typeStr)) ? TransactionType.INCOME : null;

        BigDecimal min = parseBigDecimal(minAmountField.getText());
        BigDecimal max = parseBigDecimal(maxAmountField.getText());
        String kw = keywordField.getText() != null ? keywordField.getText().trim() : null;

        List<Transaction> list = transactionService.findFiltered(userId, from, to, catId, type, min, max, kw);
        tableData.setAll(list);
    }

    private void resetFilters() {
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        typeFilterCombo.setValue("All Types");
        categoryFilterCombo.setValue(null);
        minAmountField.clear();
        maxAmountField.clear();
        keywordField.clear();
        applyFilters();
    }

    private void handleAddTransaction() {
        TransactionDialogController dialog = new TransactionDialogController(transactionService, userId, null);
        if (inModalAnalyticsService != null) {
            dialog.setSpendPreviewCard(new SpendPreviewCard(inModalAnalyticsService));
        }
        boolean saved = dialog.showAndWait(primaryStage);
        if (saved) {
            refreshTable();
            AlertHelper.showToast(primaryStage, "Transaction added successfully.");
        }
    }

    private void handleEditSelected() {
        Transaction selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("No Selection", null, "Please select a transaction to edit.");
            return;
        }

        TransactionDialogController dialog = new TransactionDialogController(transactionService, userId, selected);
        if (inModalAnalyticsService != null) {
            dialog.setSpendPreviewCard(new SpendPreviewCard(inModalAnalyticsService));
        }
        boolean saved = dialog.showAndWait(primaryStage);
        if (saved) {
            refreshTable();
            AlertHelper.showToast(primaryStage, "Transaction updated successfully.");
        }
    }

    private void handleDeleteSelected() {
        Transaction selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("No Selection", null, "Please select a transaction to delete.");
            return;
        }

        // FR-04: Confirmation Alert Dialog required
        boolean confirmed = AlertHelper.showConfirmation(
                "Confirm Deletion",
                "Are you sure you want to delete this " + selected.getType() + " of " +
                        MoneyUtil.formatCurrency(selected.getAmount()) + " (" + selected.getCategoryName() + ")?"
        );

        if (confirmed) {
            boolean deleted = transactionService.deleteTransaction(selected.getId(), userId);
            if (deleted) {
                refreshTable();
                AlertHelper.showToast(primaryStage, "Transaction deleted.");
            } else {
                AlertHelper.showError("Error", null, "Failed to delete transaction.");
            }
        }
    }

    private void handleAddCategory() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(primaryStage);
        dialog.setTitle("Add Category");
        dialog.setHeaderText("Create a custom category");

        TextField nameField = new TextField();
        nameField.setPromptText("Category name");
        ComboBox<TransactionType> typeBox = new ComboBox<>(
                FXCollections.observableArrayList(TransactionType.EXPENSE, TransactionType.INCOME));
        typeBox.setValue(TransactionType.EXPENSE);
        CheckBox fixedExpense = new CheckBox("Fixed expense");
        typeBox.valueProperty().addListener((observable, oldType, newType) -> {
            boolean expense = newType == TransactionType.EXPENSE;
            fixedExpense.setVisible(expense);
            fixedExpense.setManaged(expense);
            if (!expense) {
                fixedExpense.setSelected(false);
            }
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Category name:"), nameField);
        form.addRow(1, new Label("Type:"), typeBox);
        form.add(fixedExpense, 1, 2);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                AlertHelper.showWarning("Category Required", null, "Enter a category name.");
                return;
            }
            try {
                Category created = transactionService.addCustomCategory(userId, name, typeBox.getValue());
                if (fixedExpense.isSelected()) {
                    categoryService.setFixed(userId, created.getId(), true);
                }
                populateCategoryFilter();
                AlertHelper.showToast(primaryStage, "Category '" + name + "' created.");
            } catch (InvalidFixedCategoryException ex) {
                AlertHelper.showError("Invalid Fixed Expense", null, ex.getMessage());
            } catch (DataAccessException ex) {
                LOGGER.log(java.util.logging.Level.WARNING,
                        "Could not create category for userId={0}", userId);
                AlertHelper.showError("Save Failed", null,
                        "The category could not be created. Please try again.");
            } catch (Exception ex) {
                AlertHelper.showError("Error", "Could not create category", ex.getMessage());
            }
        }
    }

    private BigDecimal parseBigDecimal(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            return new BigDecimal(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

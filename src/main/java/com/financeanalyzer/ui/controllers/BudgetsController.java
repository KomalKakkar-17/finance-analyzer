package com.financeanalyzer.ui.controllers;

import com.financeanalyzer.dao.CategoryDao;
import com.financeanalyzer.dao.TransactionDao;
import com.financeanalyzer.model.Budget;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.enums.AlertLevel;
import com.financeanalyzer.model.enums.TransactionType;
import com.financeanalyzer.service.BudgetService;
import com.financeanalyzer.ui.AlertHelper;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.DateUtil;
import com.financeanalyzer.util.MoneyUtil;
import com.financeanalyzer.util.Validators;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Controller for managing monthly category budgets and visual threshold alerts.
 * Implements FR-06 and FR-07.
 * Features color-coded progress bars (green < 80%, amber >= 80%, red >= 100%) and month navigation.
 */
public class BudgetsController {

    private static final Logger LOGGER = AppLogger.getLogger(BudgetsController.class);

    private final BudgetService budgetService;
    private final TransactionDao transactionDao;
    private final CategoryDao categoryDao;
    private final Long userId;
    private final Stage primaryStage;

    private YearMonth currentMonth;
    private Label monthLabel;
    private Label totalBudgetLabel;
    private Label totalSpentLabel;
    private Label totalUtilisationLabel;

    private TableView<BudgetItemViewModel> tableView;
    private final ObservableList<BudgetItemViewModel> tableData = FXCollections.observableArrayList();

    public BudgetsController(BudgetService budgetService,
                             TransactionDao transactionDao,
                             CategoryDao categoryDao,
                             Long userId,
                             Stage primaryStage) {
        this.budgetService = Objects.requireNonNull(budgetService, "BudgetService must not be null.");
        this.transactionDao = Objects.requireNonNull(transactionDao, "TransactionDao must not be null.");
        this.categoryDao = Objects.requireNonNull(categoryDao, "CategoryDao must not be null.");
        this.userId = Objects.requireNonNull(userId, "userId must not be null.");
        this.primaryStage = primaryStage;
        this.currentMonth = YearMonth.now();
    }

    public Parent getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("content-area");

        // Header with Month Selector & Action
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Monthly Budgets");
        title.getStyleClass().add("card-title");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800;");

        // Month Navigation controls
        Button prevMonthBtn = new Button("\u25C0");
        prevMonthBtn.getStyleClass().add("button");
        prevMonthBtn.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            refreshView();
        });

        monthLabel = new Label(DateUtil.formatDisplayMonth(currentMonth));
        monthLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 0 8px;");

        Button nextMonthBtn = new Button("\u25B6");
        nextMonthBtn.getStyleClass().add("button");
        nextMonthBtn.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            refreshView();
        });

        Button thisMonthBtn = new Button("Current Month");
        thisMonthBtn.getStyleClass().add("button");
        thisMonthBtn.setOnAction(e -> {
            currentMonth = YearMonth.now();
            refreshView();
        });

        HBox monthNav = new HBox(6, prevMonthBtn, monthLabel, nextMonthBtn, thisMonthBtn);
        monthNav.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button setBudgetBtn = new Button("+ Set Category Budget");
        setBudgetBtn.getStyleClass().addAll("button", "primary-button");
        setBudgetBtn.setOnAction(e -> handleAddOrEditBudget(null));

        header.getChildren().addAll(title, monthNav, spacer, setBudgetBtn);

        // Month Summary KPI Card
        HBox summaryCard = createSummaryCard();

        // Table
        tableView = createTableView();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        root.getChildren().addAll(header, summaryCard, tableView);

        refreshView();
        return root;
    }

    private HBox createSummaryCard() {
        HBox card = new HBox(24);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setAlignment(Pos.CENTER_LEFT);

        VBox box1 = new VBox(4);
        Label l1 = new Label("Total Budgeted");
        l1.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");
        totalBudgetLabel = new Label("\u20B90.00");
        totalBudgetLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
        box1.getChildren().addAll(l1, totalBudgetLabel);

        Separator sep1 = new Separator(javafx.geometry.Orientation.VERTICAL);

        VBox box2 = new VBox(4);
        Label l2 = new Label("Total Month Spent");
        l2.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");
        totalSpentLabel = new Label("\u20B90.00");
        totalSpentLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
        box2.getChildren().addAll(l2, totalSpentLabel);

        Separator sep2 = new Separator(javafx.geometry.Orientation.VERTICAL);

        VBox box3 = new VBox(4);
        Label l3 = new Label("Overall Utilisation");
        l3.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");
        totalUtilisationLabel = new Label("0.0%");
        totalUtilisationLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        box3.getChildren().addAll(l3, totalUtilisationLabel);

        card.getChildren().addAll(box1, sep1, box2, sep2, box3);
        return card;
    }

    private TableView<BudgetItemViewModel> createTableView() {
        TableView<BudgetItemViewModel> table = new TableView<>(tableData);
        table.setPlaceholder(new Label("No budgets set for " + DateUtil.formatDisplayMonth(currentMonth) + ". Click '+ Set Category Budget' to configure limits."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Category Column
        TableColumn<BudgetItemViewModel, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoryName()));
        catCol.setPrefWidth(160);

        // Budget Limit Column
        TableColumn<BudgetItemViewModel, String> limitCol = new TableColumn<>("Budget Limit (\u20B9)");
        limitCol.setCellValueFactory(data -> new SimpleStringProperty(MoneyUtil.formatCurrency(data.getValue().getLimit())));
        limitCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");
        limitCol.setPrefWidth(130);

        // Spent Column
        TableColumn<BudgetItemViewModel, String> spentCol = new TableColumn<>("Spent (\u20B9)");
        spentCol.setCellValueFactory(data -> new SimpleStringProperty(MoneyUtil.formatCurrency(data.getValue().getSpent())));
        spentCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        spentCol.setPrefWidth(130);

        // Remaining Column
        TableColumn<BudgetItemViewModel, String> remCol = new TableColumn<>("Remaining (\u20B9)");
        remCol.setCellValueFactory(data -> new SimpleStringProperty(MoneyUtil.formatCurrency(data.getValue().getRemaining())));
        remCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        remCol.setPrefWidth(130);

        // Progress Bar & Utilisation % Column
        TableColumn<BudgetItemViewModel, BudgetItemViewModel> progressCol = new TableColumn<>("Utilisation Progress");
        progressCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        progressCol.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar();
            private final Label label = new Label();
            private final HBox container = new HBox(10, bar, label);

            {
                bar.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(bar, Priority.ALWAYS);
                container.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(BudgetItemViewModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    double pct = item.getUtilisation().doubleValue();
                    bar.setProgress(Math.min(1.0, pct / 100.0));
                    label.setText(String.format("%.1f%%", pct));
                    label.setStyle("-fx-font-weight: bold; -fx-min-width: 50px;");

                    // FR-07 color coding: green <80%, amber >=80% and <100%, red >=100%
                    if (item.getAlertLevel() == AlertLevel.EXCEEDED) {
                        bar.setStyle("-fx-accent: #EF4444;"); // Red
                        label.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-min-width: 50px;");
                    } else if (item.getAlertLevel() == AlertLevel.WARNING) {
                        bar.setStyle("-fx-accent: #F59E0B;"); // Amber
                        label.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-min-width: 50px;");
                    } else {
                        bar.setStyle("-fx-accent: #10B981;"); // Green
                        label.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-min-width: 50px;");
                    }
                    setGraphic(container);
                }
            }
        });
        progressCol.setPrefWidth(240);

        // Alert Status Badge Column
        TableColumn<BudgetItemViewModel, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAlertLevel().name()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge");
                    if ("EXCEEDED".equals(item)) badge.getStyleClass().add("badge-critical");
                    else if ("WARNING".equals(item)) badge.getStyleClass().add("badge-warning");
                    else badge.getStyleClass().add("badge-income");

                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        statusCol.setPrefWidth(110);

        table.getColumns().addAll(catCol, limitCol, spentCol, remCol, progressCol, statusCol);

        // Context Menu
        ContextMenu cm = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit Budget");
        editItem.setOnAction(e -> handleEditSelected());

        MenuItem delItem = new MenuItem("Delete Budget");
        delItem.setOnAction(e -> handleDeleteSelected());
        cm.getItems().addAll(editItem, delItem);

        table.setRowFactory(tv -> {
            TableRow<BudgetItemViewModel> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    handleEditSelected();
                }
            });
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(cm)
            );
            return row;
        });

        return table;
    }

    public void refreshView() {
        monthLabel.setText(DateUtil.formatDisplayMonth(currentMonth));
        List<Budget> budgets = budgetService.getBudgetsForMonth(userId, currentMonth);

        BigDecimal sumBudget = BigDecimal.ZERO;
        BigDecimal sumSpent = BigDecimal.ZERO;
        tableData.clear();

        for (Budget b : budgets) {
            BigDecimal spent = transactionDao.getCategorySpendInMonth(userId, b.getCategoryId(), currentMonth);
            BigDecimal remaining = b.getLimit().subtract(spent);
            if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

            BigDecimal util = budgetService.calculateUtilisation(userId, b.getCategoryId(), currentMonth, BigDecimal.ZERO);
            AlertLevel level = AlertLevel.fromUtilisation(util);

            tableData.add(new BudgetItemViewModel(b.getId(), b.getCategoryId(), b.getCategoryName(), b.getLimit(), spent, remaining, util, level));
            sumBudget = sumBudget.add(b.getLimit());
            sumSpent = sumSpent.add(spent);
        }

        totalBudgetLabel.setText(MoneyUtil.formatCurrency(sumBudget));
        totalSpentLabel.setText(MoneyUtil.formatCurrency(sumSpent));

        if (sumBudget.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalUtil = sumSpent.divide(sumBudget, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
            totalUtilisationLabel.setText(totalUtil + "%");
            if (totalUtil.doubleValue() >= 100.0) {
                totalUtilisationLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-danger;");
            } else if (totalUtil.doubleValue() >= 80.0) {
                totalUtilisationLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-warning;");
            } else {
                totalUtilisationLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-success;");
            }
        } else {
            totalUtilisationLabel.setText("0.0%");
            totalUtilisationLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        }
    }

    private void handleEditSelected() {
        BudgetItemViewModel sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertHelper.showWarning("No Selection", null, "Please select a category budget to edit.");
            return;
        }
        handleAddOrEditBudget(sel);
    }

    private void handleDeleteSelected() {
        BudgetItemViewModel sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertHelper.showWarning("No Selection", null, "Please select a budget to delete.");
            return;
        }
        boolean ok = AlertHelper.showConfirmation("Delete Budget", "Delete budget limit for '" + sel.getCategoryName() + "'?");
        if (ok) {
            budgetService.deleteBudget(sel.getBudgetId(), userId);
            refreshView();
            AlertHelper.showToast(primaryStage, "Budget deleted.");
        }
    }

    private void handleAddOrEditBudget(BudgetItemViewModel existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle(existing == null ? "Set Category Budget" : "Edit Category Budget");

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        ComboBox<Category> catCombo = new ComboBox<>();
        List<Category> expenseCats = categoryDao.findCategoriesForUser(userId, TransactionType.EXPENSE);
        catCombo.setItems(FXCollections.observableArrayList(expenseCats));
        if (existing != null) {
            for (Category c : expenseCats) {
                if (c.getId().equals(existing.getCategoryId())) {
                    catCombo.setValue(c);
                    catCombo.setDisable(true); // Don't change category on edit
                    break;
                }
            }
        } else if (!expenseCats.isEmpty()) {
            catCombo.setValue(expenseCats.get(0));
        }

        TextField limitField = new TextField();
        limitField.setPromptText("Monthly limit in \u20B9");
        if (existing != null) {
            limitField.setText(existing.getLimit().toPlainString());
        }

        Label targetMonthLabel = new Label("For Month: " + DateUtil.formatDisplayMonth(currentMonth));
        targetMonthLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-primary;");

        Button saveBtn = new Button("Save Budget");
        saveBtn.getStyleClass().addAll("button", "primary-button");
        saveBtn.setOnAction(e -> {
            try {
                Category cat = catCombo.getValue();
                if (cat == null) throw new IllegalArgumentException("Category is required.");
                BigDecimal limit = new BigDecimal(limitField.getText().trim());
                Validators.validateBudgetLimit(limit);

                budgetService.setBudget(userId, cat.getId(), currentMonth, limit);
                dialog.close();
                refreshView();
                AlertHelper.showToast(primaryStage, "Budget saved for " + cat.getName());
            } catch (Exception ex) {
                AlertHelper.showError("Error", "Validation failed", ex.getMessage());
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("button");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox btnBox = new HBox(10, cancelBtn, saveBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        form.getChildren().addAll(
                targetMonthLabel,
                new Label("Expense Category:"), catCombo,
                new Label("Monthly Spending Limit (\u20B9):"), limitField,
                btnBox
        );

        Scene scene = new Scene(form, 360, 260);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /**
     * ViewModel for Budget table display.
     */
    public static class BudgetItemViewModel {
        private final Long budgetId;
        private final Long categoryId;
        private final String categoryName;
        private final BigDecimal limit;
        private final BigDecimal spent;
        private final BigDecimal remaining;
        private final BigDecimal utilisation;
        private final AlertLevel alertLevel;

        public BudgetItemViewModel(Long budgetId, Long categoryId, String categoryName,
                                   BigDecimal limit, BigDecimal spent, BigDecimal remaining,
                                   BigDecimal utilisation, AlertLevel alertLevel) {
            this.budgetId = budgetId;
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.limit = limit;
            this.spent = spent;
            this.remaining = remaining;
            this.utilisation = utilisation;
            this.alertLevel = alertLevel;
        }

        public Long getBudgetId() { return budgetId; }
        public Long getCategoryId() { return categoryId; }
        public String getCategoryName() { return categoryName; }
        public BigDecimal getLimit() { return limit; }
        public BigDecimal getSpent() { return spent; }
        public BigDecimal getRemaining() { return remaining; }
        public BigDecimal getUtilisation() { return utilisation; }
        public AlertLevel getAlertLevel() { return alertLevel; }
    }
}

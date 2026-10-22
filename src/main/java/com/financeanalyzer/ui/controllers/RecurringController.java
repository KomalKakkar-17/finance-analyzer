package com.financeanalyzer.ui.controllers;

import com.financeanalyzer.dao.RecurringTransactionDao;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.RecurringTransaction;
import com.financeanalyzer.model.enums.Frequency;
import com.financeanalyzer.model.enums.RuleStatus;
import com.financeanalyzer.model.enums.TransactionType;
import com.financeanalyzer.service.RecurringScheduler;
import com.financeanalyzer.service.TransactionService;
import com.financeanalyzer.ui.AlertHelper;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.DateUtil;
import com.financeanalyzer.util.MoneyUtil;
import com.financeanalyzer.util.Validators;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Controller for managing recurring transaction rules.
 * Implements FR-10.
 */
public class RecurringController {

    private static final Logger LOGGER = AppLogger.getLogger(RecurringController.class);

    private final RecurringTransactionDao recurringDao;
    private final TransactionService transactionService;
    private final RecurringScheduler recurringScheduler;
    private final Long userId;
    private final Stage primaryStage;

    private TableView<RecurringTransaction> tableView;
    private final ObservableList<RecurringTransaction> tableData = FXCollections.observableArrayList();

    public RecurringController(RecurringTransactionDao recurringDao,
                               TransactionService transactionService,
                               RecurringScheduler recurringScheduler,
                               Long userId,
                               Stage primaryStage) {
        this.recurringDao = Objects.requireNonNull(recurringDao, "RecurringTransactionDao must not be null.");
        this.transactionService = Objects.requireNonNull(transactionService, "TransactionService must not be null.");
        this.recurringScheduler = Objects.requireNonNull(recurringScheduler, "RecurringScheduler must not be null.");
        this.userId = Objects.requireNonNull(userId, "userId must not be null.");
        this.primaryStage = primaryStage;
    }

    public Parent getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("content-area");

        // Header and Toolbar
        HBox headerBox = new HBox(16);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Recurring Transactions");
        title.getStyleClass().add("card-title");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button catchUpBtn = new Button("\u21BB Run Catch-up Now");
        catchUpBtn.getStyleClass().add("button");
        catchUpBtn.setOnAction(e -> handleRunCatchUp());

        Button pauseResumeBtn = new Button("Pause / Resume");
        pauseResumeBtn.getStyleClass().add("button");
        pauseResumeBtn.setOnAction(e -> handleTogglePauseResume());

        Button deleteBtn = new Button("Delete Rule");
        deleteBtn.getStyleClass().addAll("button", "danger-button");
        deleteBtn.setOnAction(e -> handleDeleteRule());

        Button newRuleBtn = new Button("+ New Recurring Rule");
        newRuleBtn.getStyleClass().addAll("button", "primary-button");
        newRuleBtn.setOnAction(e -> handleAddRule());

        headerBox.getChildren().addAll(title, spacer, catchUpBtn, pauseResumeBtn, deleteBtn, newRuleBtn);

        // Table
        tableView = createTableView();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        root.getChildren().addAll(headerBox, tableView);

        refreshTable();
        return root;
    }

    private TableView<RecurringTransaction> createTableView() {
        TableView<RecurringTransaction> table = new TableView<>(tableData);
        table.setPlaceholder(new Label("No recurring rules set. Click '+ New Recurring Rule' to automate fixed payments."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Status Badge Column
        TableColumn<RecurringTransaction, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge");
                    if ("ACTIVE".equals(item)) badge.getStyleClass().add("badge-active");
                    else if ("PAUSED".equals(item)) badge.getStyleClass().add("badge-paused");
                    else badge.getStyleClass().add("badge-critical");

                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        statusCol.setPrefWidth(90);

        // Type Badge Column
        TableColumn<RecurringTransaction, String> typeCol = new TableColumn<>("Type");
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
        typeCol.setPrefWidth(90);

        // Category Column
        TableColumn<RecurringTransaction, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoryName()));
        catCol.setPrefWidth(140);

        // Amount Column
        TableColumn<RecurringTransaction, String> amtCol = new TableColumn<>("Amount (\u20B9)");
        amtCol.setCellValueFactory(data -> new SimpleStringProperty(MoneyUtil.formatCurrency(data.getValue().getAmount())));
        amtCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER_RIGHT);
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });
        amtCol.setPrefWidth(110);

        // Frequency Column
        TableColumn<RecurringTransaction, String> freqCol = new TableColumn<>("Frequency");
        freqCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFrequency().name()));
        freqCol.setPrefWidth(100);

        // Next Due Date Column
        TableColumn<RecurringTransaction, String> dueCol = new TableColumn<>("Next Due Date");
        dueCol.setCellValueFactory(data -> new SimpleStringProperty(DateUtil.formatDisplayDate(data.getValue().getNextDueDate())));
        dueCol.setPrefWidth(120);

        // End Date Column
        TableColumn<RecurringTransaction, String> endCol = new TableColumn<>("End Date");
        endCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getEndDate() != null ? DateUtil.formatDisplayDate(data.getValue().getEndDate()) : "No End Date"));
        endCol.setPrefWidth(120);

        table.getColumns().addAll(statusCol, typeCol, catCol, amtCol, freqCol, dueCol, endCol);
        return table;
    }

    public void refreshTable() {
        List<RecurringTransaction> list = recurringDao.findAll(userId);
        tableData.setAll(list);
    }

    private void handleTogglePauseResume() {
        RecurringTransaction selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("No Selection", null, "Please select a recurring rule from the table.");
            return;
        }

        if (selected.getStatus() == RuleStatus.EXPIRED) {
            AlertHelper.showWarning("Rule Expired", null, "This rule has expired and cannot be reactivated.");
            return;
        }

        RuleStatus newStatus = (selected.getStatus() == RuleStatus.ACTIVE) ? RuleStatus.PAUSED : RuleStatus.ACTIVE;
        boolean updated = recurringDao.updateStatus(selected.getId(), userId, newStatus);
        if (updated) {
            refreshTable();
            AlertHelper.showToast(primaryStage, "Rule is now " + newStatus);
        }
    }

    private void handleDeleteRule() {
        RecurringTransaction selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("No Selection", null, "Please select a recurring rule to delete.");
            return;
        }

        boolean confirmed = AlertHelper.showConfirmation(
                "Delete Recurring Rule",
                "Are you sure you want to delete this recurring " + selected.getType() + " of " +
                        MoneyUtil.formatCurrency(selected.getAmount()) + "?"
        );

        if (confirmed) {
            boolean deleted = recurringDao.deleteById(selected.getId(), userId);
            if (deleted) {
                refreshTable();
                AlertHelper.showToast(primaryStage, "Recurring rule deleted.");
            }
        }
    }

    private void handleRunCatchUp() {
        recurringScheduler.runCatchUp();
        refreshTable();
        AlertHelper.showToast(primaryStage, "Catch-up check completed.");
    }

    private void handleAddRule() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Create Recurring Rule");

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        // Type
        ComboBox<TransactionType> typeCombo = new ComboBox<>(FXCollections.observableArrayList(TransactionType.EXPENSE, TransactionType.INCOME));
        typeCombo.setValue(TransactionType.EXPENSE);

        // Category
        ComboBox<Category> catCombo = new ComboBox<>();
        Runnable loadCategories = () -> {
            List<Category> cats = transactionService.getCategories(userId, typeCombo.getValue());
            catCombo.setItems(FXCollections.observableArrayList(cats));
            if (!cats.isEmpty()) catCombo.setValue(cats.get(0));
        };
        loadCategories.run();
        typeCombo.setOnAction(e -> loadCategories.run());

        // Amount
        TextField amtField = new TextField();
        amtField.setPromptText("Amount in \u20B9");

        // Frequency
        ComboBox<Frequency> freqCombo = new ComboBox<>(FXCollections.observableArrayList(Frequency.values()));
        freqCombo.setValue(Frequency.MONTHLY);

        // Start Date
        DatePicker startPicker = new DatePicker(LocalDate.now());

        // End Date (optional)
        DatePicker endPicker = new DatePicker();
        endPicker.setPromptText("Optional End Date");

        Button saveBtn = new Button("Create Rule");
        saveBtn.getStyleClass().addAll("button", "primary-button");
        saveBtn.setOnAction(e -> {
            try {
                BigDecimal amt = new BigDecimal(amtField.getText().trim());
                Validators.validateAmount(amt);
                Category cat = catCombo.getValue();
                if (cat == null) throw new IllegalArgumentException("Category must be selected.");
                LocalDate startDate = startPicker.getValue();
                if (startDate == null) throw new IllegalArgumentException("Start date is required.");
                LocalDate endDate = endPicker.getValue();
                if (endDate != null && endDate.isBefore(startDate)) {
                    throw new IllegalArgumentException("End date cannot be earlier than start date.");
                }

                RecurringTransaction rule = new RecurringTransaction(
                        null, userId, cat.getId(), cat.getName(), typeCombo.getValue(),
                        amt, freqCombo.getValue(), startDate, startDate, endDate, RuleStatus.ACTIVE
                );

                recurringDao.save(rule);
                dialog.close();
                refreshTable();
                AlertHelper.showToast(primaryStage, "Recurring rule created.");
            } catch (Exception ex) {
                AlertHelper.showError("Validation Error", null, ex.getMessage());
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("button");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox btnBox = new HBox(10, cancelBtn, saveBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        form.getChildren().addAll(
                new Label("Rule Type:"), typeCombo,
                new Label("Category:"), catCombo,
                new Label("Amount (\u20B9):"), amtField,
                new Label("Frequency:"), freqCombo,
                new Label("Start Date:"), startPicker,
                new Label("End Date (optional):"), endPicker,
                btnBox
        );

        Scene scene = new Scene(form, 380, 480);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}

package com.financeanalyzer.ui.controllers;

import com.financeanalyzer.exception.AllocationRatioException;
import com.financeanalyzer.model.SavingsGoal;
import com.financeanalyzer.service.GoalService;
import com.financeanalyzer.service.RatioAllocationService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Controller for Savings Goals and atomic ratio-based deposit distributions.
 * Implements FR-08 and FR-09.
 * Enforces the 100% cumulative ratio constraint with real-time UI feedback.
 */
public class GoalsController {

    private static final Logger LOGGER = AppLogger.getLogger(GoalsController.class);

    private final GoalService goalService;
    private final RatioAllocationService ratioAllocationService;
    private final Long userId;
    private final Stage primaryStage;

    private Label totalAllocatedRatioLabel;
    private ProgressBar ratioProgressBar;
    private TableView<SavingsGoal> tableView;
    private final ObservableList<SavingsGoal> tableData = FXCollections.observableArrayList();

    public GoalsController(GoalService goalService,
                           RatioAllocationService ratioAllocationService,
                           Long userId,
                           Stage primaryStage) {
        this.goalService = Objects.requireNonNull(goalService, "GoalService must not be null.");
        this.ratioAllocationService = Objects.requireNonNull(ratioAllocationService, "RatioAllocationService must not be null.");
        this.userId = Objects.requireNonNull(userId, "userId must not be null.");
        this.primaryStage = primaryStage;
    }

    public Parent getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("content-area");

        // Header & Actions
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Savings Goals");
        title.getStyleClass().add("card-title");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button depositBtn = new Button("\u2935 Allocate Savings Deposit");
        depositBtn.getStyleClass().add("button");
        depositBtn.setOnAction(e -> handleDepositModal());

        Button newGoalBtn = new Button("+ New Savings Goal");
        newGoalBtn.getStyleClass().addAll("button", "primary-button");
        newGoalBtn.setOnAction(e -> handleAddOrEditGoal(null));

        header.getChildren().addAll(title, spacer, depositBtn, newGoalBtn);

        // Ratio Allocation Tracker Banner Card
        HBox ratioTrackerCard = createRatioTrackerCard();

        // Goals TableView
        tableView = createTableView();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        root.getChildren().addAll(header, ratioTrackerCard, tableView);

        refreshView();
        return root;
    }

    private HBox createRatioTrackerCard() {
        HBox card = new HBox(20);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14, 20, 14, 20));
        card.setAlignment(Pos.CENTER_LEFT);

        VBox infoBox = new VBox(4);
        Label title = new Label("Active Allocation Ratio Pool (FR-09)");
        title.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");

        totalAllocatedRatioLabel = new Label("Total Allocated: 0% / 100%");
        totalAllocatedRatioLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-primary;");
        infoBox.getChildren().addAll(title, totalAllocatedRatioLabel);

        ratioProgressBar = new ProgressBar(0.0);
        ratioProgressBar.setPrefWidth(260);
        ratioProgressBar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(ratioProgressBar, Priority.ALWAYS);

        Label note = new Label("Surplus ratio remains unallocated savings.");
        note.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");

        card.getChildren().addAll(infoBox, ratioProgressBar, note);
        return card;
    }

    private TableView<SavingsGoal> createTableView() {
        TableView<SavingsGoal> table = new TableView<>(tableData);
        table.setPlaceholder(new Label("No savings goals configured. Click '+ New Savings Goal' to get started."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Status Column
        TableColumn<SavingsGoal, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isActive() ? "ACTIVE" : "PAUSED"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge");
                    if ("ACTIVE".equals(item)) badge.getStyleClass().add("badge-active");
                    else badge.getStyleClass().add("badge-paused");
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        statusCol.setPrefWidth(90);

        // Title Column
        TableColumn<SavingsGoal, String> titleCol = new TableColumn<>("Goal Title");
        titleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        titleCol.setPrefWidth(160);

        // Target Amount Column
        TableColumn<SavingsGoal, String> targetCol = new TableColumn<>("Target (\u20B9)");
        targetCol.setCellValueFactory(data -> new SimpleStringProperty(MoneyUtil.formatCurrency(data.getValue().getTargetAmount())));
        targetCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");
        targetCol.setPrefWidth(120);

        // Saved Amount Column
        TableColumn<SavingsGoal, String> savedCol = new TableColumn<>("Saved (\u20B9)");
        savedCol.setCellValueFactory(data -> new SimpleStringProperty(MoneyUtil.formatCurrency(data.getValue().getSavedAmount())));
        savedCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        savedCol.setPrefWidth(120);

        // Progress Bar Column
        TableColumn<SavingsGoal, SavingsGoal> progressCol = new TableColumn<>("Goal Progress");
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
            protected void updateItem(SavingsGoal goal, boolean empty) {
                super.updateItem(goal, empty);
                if (empty || goal == null) {
                    setGraphic(null);
                } else {
                    double pct = goal.getProgressPercentage();
                    bar.setProgress(pct / 100.0);
                    bar.setStyle("-fx-accent: #3B82F6;");
                    label.setText(String.format("%.1f%%", pct));
                    label.setStyle("-fx-font-weight: bold; -fx-min-width: 50px;");
                    setGraphic(container);
                }
            }
        });
        progressCol.setPrefWidth(200);

        // Ratio Column
        TableColumn<SavingsGoal, String> ratioCol = new TableColumn<>("Ratio %");
        ratioCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAllocationRatio() + "%"));
        ratioCol.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
        ratioCol.setPrefWidth(80);

        // Deadline Column
        TableColumn<SavingsGoal, String> deadCol = new TableColumn<>("Deadline");
        deadCol.setCellValueFactory(data -> new SimpleStringProperty(DateUtil.formatDisplayDate(data.getValue().getDeadline())));
        deadCol.setPrefWidth(110);

        table.getColumns().addAll(statusCol, titleCol, targetCol, savedCol, progressCol, ratioCol, deadCol);

        // Context Menu
        ContextMenu cm = new ContextMenu();
        MenuItem toggleItem = new MenuItem("Toggle Active / Paused");
        toggleItem.setOnAction(e -> handleToggleActive());

        MenuItem editItem = new MenuItem("Edit Goal");
        editItem.setOnAction(e -> handleEditSelected());

        MenuItem delItem = new MenuItem("Delete Goal");
        delItem.setOnAction(e -> handleDeleteSelected());

        cm.getItems().addAll(toggleItem, editItem, delItem);

        table.setRowFactory(tv -> {
            TableRow<SavingsGoal> row = new TableRow<>();
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
        List<SavingsGoal> allGoals = goalService.getAllGoals(userId);
        tableData.setAll(allGoals);

        int totalActiveRatio = goalService.getTotalActiveRatio(userId);
        totalAllocatedRatioLabel.setText("Total Allocated: " + totalActiveRatio + "% / 100%");
        ratioProgressBar.setProgress(totalActiveRatio / 100.0);

        if (totalActiveRatio > 100) {
            totalAllocatedRatioLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-danger;");
            ratioProgressBar.setStyle("-fx-accent: #EF4444;");
        } else if (totalActiveRatio == 100) {
            totalAllocatedRatioLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-success;");
            ratioProgressBar.setStyle("-fx-accent: #10B981;");
        } else {
            totalAllocatedRatioLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-primary;");
            ratioProgressBar.setStyle("-fx-accent: #3B82F6;");
        }
    }

    private void handleToggleActive() {
        SavingsGoal sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            goalService.toggleGoalActive(sel.getId(), userId);
            refreshView();
            AlertHelper.showToast(primaryStage, "Goal status updated.");
        } catch (AllocationRatioException ex) {
            AlertHelper.showWarning("Ratio Exceeded", null, ex.getMessage());
        }
    }

    private void handleEditSelected() {
        SavingsGoal sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertHelper.showWarning("No Selection", null, "Please select a goal to edit.");
            return;
        }
        handleAddOrEditGoal(sel);
    }

    private void handleDeleteSelected() {
        SavingsGoal sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        boolean ok = AlertHelper.showConfirmation("Delete Goal", "Delete goal '" + sel.getTitle() + "'?");
        if (ok) {
            goalService.deleteGoal(sel.getId(), userId);
            refreshView();
            AlertHelper.showToast(primaryStage, "Goal deleted.");
        }
    }

    private void handleAddOrEditGoal(SavingsGoal existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle(existing == null ? "New Savings Goal" : "Edit Savings Goal");

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        TextField titleField = new TextField();
        titleField.setPromptText("e.g. Car Downpayment");
        if (existing != null) titleField.setText(existing.getTitle());

        TextField targetField = new TextField();
        targetField.setPromptText("Target Amount in \u20B9");
        if (existing != null) targetField.setText(existing.getTargetAmount().toPlainString());

        DatePicker deadlinePicker = new DatePicker(existing != null ? existing.getDeadline() : LocalDate.now().plusMonths(6));

        Spinner<Integer> ratioSpinner = new Spinner<>(0, 100, existing != null ? existing.getAllocationRatio() : 20);
        ratioSpinner.setEditable(true);

        Label currentPoolLabel = new Label("Current Active Pool: " + goalService.getTotalActiveRatio(userId) + "%");
        currentPoolLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");

        Button saveBtn = new Button("Save Goal");
        saveBtn.getStyleClass().addAll("button", "primary-button");
        saveBtn.setOnAction(e -> {
            try {
                String title = titleField.getText() != null ? titleField.getText().trim() : "";
                BigDecimal target = new BigDecimal(targetField.getText().trim());
                LocalDate deadline = deadlinePicker.getValue();
                int ratio = ratioSpinner.getValue();

                if (existing == null) {
                    goalService.createGoal(userId, title, target, deadline, ratio);
                } else {
                    existing.setTitle(title);
                    existing.setTargetAmount(target);
                    existing.setDeadline(deadline);
                    existing.setAllocationRatio(ratio);
                    goalService.updateGoal(existing);
                }

                dialog.close();
                refreshView();
                AlertHelper.showToast(primaryStage, "Goal saved successfully.");
            } catch (AllocationRatioException ex) {
                AlertHelper.showWarning("Ratio Limit Exceeded", null, ex.getMessage());
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
                new Label("Goal Title:"), titleField,
                new Label("Target Amount (\u20B9):"), targetField,
                new Label("Target Deadline:"), deadlinePicker,
                new Label("Allocation Ratio (0% - 100%):"), ratioSpinner, currentPoolLabel,
                btnBox
        );

        Scene scene = new Scene(form, 380, 360);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void handleDepositModal() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Allocate Savings Deposit (Atomic ACID Transaction)");

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        Label intro = new Label("Enter savings deposit to distribute across active goals by ratio:");
        intro.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-main;");

        TextField amtField = new TextField();
        amtField.setPromptText("e.g. 10000.00");

        VBox previewBox = new VBox(6);
        previewBox.setStyle("-fx-background-color: rgba(0,0,0,0.03); -fx-padding: 12px; -fx-background-radius: 8px;");
        Label previewHeader = new Label("Distribution Preview:");
        previewHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        previewBox.getChildren().add(previewHeader);

        Runnable updatePreview = () -> {
            previewBox.getChildren().clear();
            previewBox.getChildren().add(previewHeader);

            BigDecimal amt = BigDecimal.ZERO;
            try {
                if (amtField.getText() != null && !amtField.getText().isBlank()) {
                    amt = new BigDecimal(amtField.getText().trim());
                }
            } catch (Exception ignored) {}

            List<SavingsGoal> activeGoals = goalService.getActiveGoals(userId);
            BigDecimal totalAllocated = BigDecimal.ZERO;

            for (SavingsGoal g : activeGoals) {
                BigDecimal share = amt.multiply(BigDecimal.valueOf(g.getAllocationRatio()))
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                totalAllocated = totalAllocated.add(share);
                Label line = new Label(String.format("\u2022 %s (%d%%): %s", g.getTitle(), g.getAllocationRatio(), MoneyUtil.formatCurrency(share)));
                line.setStyle("-fx-font-size: 11px;");
                previewBox.getChildren().add(line);
            }

            BigDecimal unallocated = amt.subtract(totalAllocated).max(BigDecimal.ZERO);
            Label unallocatedLine = new Label("Unallocated Savings Surplus: " + MoneyUtil.formatCurrency(unallocated));
            unallocatedLine.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-primary; -fx-font-size: 11px;");
            previewBox.getChildren().add(unallocatedLine);
        };

        amtField.textProperty().addListener((obs, oldV, newV) -> updatePreview.run());
        updatePreview.run();

        Button confirmBtn = new Button("Commit Deposit (ACID)");
        confirmBtn.getStyleClass().addAll("button", "primary-button");
        confirmBtn.setOnAction(e -> {
            try {
                BigDecimal amt = new BigDecimal(amtField.getText().trim());
                Validators.validateAmount(amt);
                RatioAllocationService.AllocationResult res = ratioAllocationService.allocateDeposit(userId, amt);
                dialog.close();
                refreshView();
                AlertHelper.showToast(primaryStage, "Successfully deposited " + MoneyUtil.formatCurrency(res.getTotalDeposit()) + " across goals.");
            } catch (Exception ex) {
                AlertHelper.showError("Deposit Failed", null, ex.getMessage());
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("button");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox btnBox = new HBox(10, cancelBtn, confirmBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        form.getChildren().addAll(intro, new Label("Deposit Amount (\u20B9):"), amtField, previewBox, btnBox);

        Scene scene = new Scene(form, 440, 360);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}

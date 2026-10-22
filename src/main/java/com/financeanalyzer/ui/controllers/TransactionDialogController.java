package com.financeanalyzer.ui.controllers;

import com.financeanalyzer.exception.InvalidTransactionException;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.Expense;
import com.financeanalyzer.model.Income;
import com.financeanalyzer.model.Transaction;
import com.financeanalyzer.model.enums.TransactionType;
import com.financeanalyzer.service.TransactionService;
import com.financeanalyzer.ui.AlertHelper;
import com.financeanalyzer.ui.components.SpendPreviewCard;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.Validators;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Add / Edit Transaction modal dialog.
 * Houses the in-modal live spend preview card and validation logic.
 * Visibly demonstrates Unit 5 event-driven UI, Modal stages, and real-time listeners.
 */
public class TransactionDialogController {

    private static final Logger LOGGER = AppLogger.getLogger(TransactionDialogController.class);

    private final TransactionService transactionService;
    private final Long userId;
    private final Transaction existingTransaction; // null if creating new
    private boolean saved = false;

    private Stage dialogStage;
    private ToggleGroup typeToggleGroup;
    private RadioButton expenseRadio;
    private RadioButton incomeRadio;
    private TextField amountField;
    private ComboBox<Category> categoryComboBox;
    private DatePicker datePicker;
    private ComboBox<String> paymentModeComboBox;
    private TextArea notesArea;
    private Label errorLabel;
    private StackPane previewContainer;

    // Optional preview card hook for Sprint 3
    private SpendPreviewCard spendPreviewCard;

    public TransactionDialogController(TransactionService transactionService, Long userId, Transaction existingTransaction) {
        this.transactionService = Objects.requireNonNull(transactionService, "TransactionService must not be null.");
        this.userId = Objects.requireNonNull(userId, "userId must not be null.");
        this.existingTransaction = existingTransaction;
    }

    public void setSpendPreviewCard(SpendPreviewCard spendPreviewCard) {
        this.spendPreviewCard = spendPreviewCard;
    }

    /**
     * Displays the modal stage and waits for user interaction.
     *
     * @param ownerStage the parent stage
     * @return true if a transaction was saved/updated, false otherwise
     */
    public boolean showAndWait(Stage ownerStage) {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(ownerStage);
        dialogStage.setTitle(existingTransaction == null ? "Add New Transaction" : "Edit Transaction");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setPadding(new Insets(20));

        // Form Grid
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(10));

        // 1. Transaction Type Toggle
        Label typeLabel = new Label("Type:");
        typeLabel.setStyle("-fx-font-weight: bold;");
        typeToggleGroup = new ToggleGroup();
        expenseRadio = new RadioButton("Expense");
        expenseRadio.setToggleGroup(typeToggleGroup);
        expenseRadio.setSelected(existingTransaction == null || existingTransaction.getType() == TransactionType.EXPENSE);

        incomeRadio = new RadioButton("Income");
        incomeRadio.setToggleGroup(typeToggleGroup);
        incomeRadio.setSelected(existingTransaction != null && existingTransaction.getType() == TransactionType.INCOME);

        HBox typeBox = new HBox(16, expenseRadio, incomeRadio);
        typeBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(typeLabel, 0, 0);
        grid.add(typeBox, 1, 0);

        // 2. Amount Field
        Label amountLabel = new Label("Amount (\u20B9):");
        amountLabel.setStyle("-fx-font-weight: bold;");
        amountField = new TextField();
        amountField.setPromptText("e.g. 1250.00");
        if (existingTransaction != null && existingTransaction.getAmount() != null) {
            amountField.setText(existingTransaction.getAmount().toPlainString());
        }
        grid.add(amountLabel, 0, 1);
        grid.add(amountField, 1, 1);

        // 3. Category ComboBox
        Label catLabel = new Label("Category:");
        catLabel.setStyle("-fx-font-weight: bold;");
        categoryComboBox = new ComboBox<>();
        categoryComboBox.setMaxWidth(Double.MAX_VALUE);
        Button addCatBtn = new Button("+");
        addCatBtn.setTooltip(new Tooltip("Create Custom Category"));
        addCatBtn.setOnAction(e -> handleAddCustomCategory());
        HBox catBox = new HBox(8, categoryComboBox, addCatBtn);
        HBox.setHgrow(categoryComboBox, Priority.ALWAYS);
        grid.add(catLabel, 0, 2);
        grid.add(catBox, 1, 2);

        // 4. Date Picker
        Label dateLabel = new Label("Date:");
        dateLabel.setStyle("-fx-font-weight: bold;");
        datePicker = new DatePicker(existingTransaction != null ? existingTransaction.getDate() : LocalDate.now());
        datePicker.setMaxWidth(Double.MAX_VALUE);
        grid.add(dateLabel, 0, 3);
        grid.add(datePicker, 1, 3);

        // 5. Payment Mode
        Label payLabel = new Label("Payment Mode:");
        payLabel.setStyle("-fx-font-weight: bold;");
        paymentModeComboBox = new ComboBox<>();
        paymentModeComboBox.getItems().addAll("UPI", "Cash", "Credit Card", "Debit Card", "Net Banking", "Bank Transfer");
        paymentModeComboBox.setMaxWidth(Double.MAX_VALUE);
        if (existingTransaction != null && existingTransaction.getPaymentMode() != null) {
            paymentModeComboBox.setValue(existingTransaction.getPaymentMode());
        } else {
            paymentModeComboBox.setValue("UPI");
        }
        grid.add(payLabel, 0, 4);
        grid.add(paymentModeComboBox, 1, 4);

        // 6. Notes
        Label notesLabel = new Label("Notes:");
        notesLabel.setStyle("-fx-font-weight: bold;");
        notesArea = new TextArea();
        notesArea.setPromptText("Optional notes or tags");
        notesArea.setPrefRowCount(3);
        if (existingTransaction != null && existingTransaction.getNotes() != null) {
            notesArea.setText(existingTransaction.getNotes());
        }
        grid.add(notesLabel, 0, 5);
        grid.add(notesArea, 1, 5);

        // Error Feedback Label
        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        grid.add(errorLabel, 1, 6);

        // Populate Categories based on Type
        refreshCategories();
        typeToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            refreshCategories();
            triggerPreviewUpdate();
        });

        // Live Preview Card Container (Right/Bottom panel)
        previewContainer = new StackPane();
        previewContainer.setPrefWidth(320);
        previewContainer.getStyleClass().add("card");
        if (spendPreviewCard != null) {
            previewContainer.getChildren().add(spendPreviewCard.getView());
        } else {
            Label placeholder = new Label("In-modal Live Spend Analysis\nSelect category and amount to preview impact.");
            placeholder.setStyle("-fx-text-fill: -color-text-muted; -fx-text-alignment: center;");
            previewContainer.getChildren().add(placeholder);
        }

        // Live Preview Change Listeners
        amountField.textProperty().addListener((obs, oldVal, newVal) -> triggerPreviewUpdate());
        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> triggerPreviewUpdate());

        HBox centerLayout = new HBox(20, grid, previewContainer);
        HBox.setHgrow(grid, Priority.ALWAYS);
        root.setCenter(centerLayout);

        // Action Buttons (Bottom Bar)
        Button saveBtn = new Button("Save Transaction");
        saveBtn.getStyleClass().addAll("button", "primary-button");
        saveBtn.setOnAction(e -> handleSave());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("button");
        cancelBtn.setOnAction(e -> dialogStage.close());

        HBox buttonBar = new HBox(12, cancelBtn, saveBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(16, 0, 0, 0));
        root.setBottom(buttonBar);

        Scene scene = new Scene(root, 760, 480);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        dialogStage.setScene(scene);

        // Trigger initial preview if editing
        if (existingTransaction != null) {
            triggerPreviewUpdate();
        }

        dialogStage.showAndWait();
        return saved;
    }

    private void refreshCategories() {
        TransactionType selectedType = expenseRadio.isSelected() ? TransactionType.EXPENSE : TransactionType.INCOME;
        List<Category> cats = transactionService.getCategories(userId, selectedType);
        categoryComboBox.setItems(FXCollections.observableArrayList(cats));

        if (existingTransaction != null && existingTransaction.getCategoryId() != null) {
            for (Category c : cats) {
                if (c.getId().equals(existingTransaction.getCategoryId())) {
                    categoryComboBox.setValue(c);
                    break;
                }
            }
        } else if (!cats.isEmpty()) {
            categoryComboBox.setValue(cats.get(0));
        }
    }

    private void triggerPreviewUpdate() {
        if (spendPreviewCard == null) return;
        boolean isExpense = expenseRadio.isSelected();
        Category cat = categoryComboBox.getValue();
        String amtStr = amountField.getText();

        BigDecimal amt = BigDecimal.ZERO;
        try {
            if (amtStr != null && !amtStr.isBlank()) {
                amt = new BigDecimal(amtStr.trim());
            }
        } catch (NumberFormatException ignored) {
        }

        spendPreviewCard.updatePreview(userId, cat, amt, isExpense);
    }

    private void handleAddCustomCategory() {
        TransactionType type = expenseRadio.isSelected() ? TransactionType.EXPENSE : TransactionType.INCOME;
        Optional<String> nameOpt = AlertHelper.showTextInput("New Category", "Add Custom " + type + " Category", "Category Name:");
        if (nameOpt.isPresent() && !nameOpt.get().trim().isEmpty()) {
            try {
                Category created = transactionService.addCustomCategory(userId, nameOpt.get().trim(), type);
                refreshCategories();
                categoryComboBox.setValue(created);
            } catch (Exception ex) {
                AlertHelper.showError("Error", "Could not create category", ex.getMessage());
            }
        }
    }

    private void handleSave() {
        clearError();
        String amtText = amountField.getText() != null ? amountField.getText().trim() : "";
        Category category = categoryComboBox.getValue();
        LocalDate date = datePicker.getValue();
        String paymentMode = paymentModeComboBox.getValue();
        String notes = notesArea.getText() != null ? notesArea.getText().trim() : "";
        boolean isExpense = expenseRadio.isSelected();

        BigDecimal amount;
        try {
            amount = new BigDecimal(amtText);
            Validators.validateAmount(amount);
        } catch (Exception ex) {
            showError("Please enter a valid positive amount with up to 2 decimal places.", amountField);
            return;
        }

        if (category == null) {
            showError("Please select a category.", categoryComboBox);
            return;
        }

        if (date == null) {
            showError("Please choose a transaction date.", datePicker);
            return;
        }

        if (isExpense && date.isAfter(LocalDate.now())) {
            showError("Expense transaction date cannot be in the future.", datePicker);
            return;
        }

        try {
            if (existingTransaction == null) {
                // Create New
                if (isExpense) {
                    transactionService.addExpense(userId, category.getId(), amount, date, paymentMode, notes);
                } else {
                    transactionService.addIncome(userId, category.getId(), amount, date, paymentMode, notes);
                }
            } else {
                // Update Existing
                existingTransaction.setCategoryId(category.getId());
                existingTransaction.setAmount(amount);
                existingTransaction.setDate(date);
                existingTransaction.setPaymentMode(paymentMode);
                existingTransaction.setNotes(notes);
                transactionService.updateTransaction(existingTransaction);
            }

            saved = true;
            dialogStage.close();
        } catch (InvalidTransactionException ex) {
            LOGGER.log(Level.WARNING, "Transaction validation failed: {0}", ex.getMessage());
            showError(ex.getMessage(), null);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error saving transaction.", e);
            showError("Error saving transaction: " + e.getMessage(), null);
        }
    }

    private void showError(String msg, Control field) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        if (field != null) {
            field.getStyleClass().add("field-error");
            field.requestFocus();
        }
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        amountField.getStyleClass().remove("field-error");
        categoryComboBox.getStyleClass().remove("field-error");
        datePicker.getStyleClass().remove("field-error");
    }
}

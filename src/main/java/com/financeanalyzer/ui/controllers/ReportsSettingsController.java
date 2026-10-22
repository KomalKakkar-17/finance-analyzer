package com.financeanalyzer.ui.controllers;

import com.financeanalyzer.dao.CategoryDao;
import com.financeanalyzer.dao.FixedCategoryDao;
import com.financeanalyzer.model.Category;
import com.financeanalyzer.model.User;
import com.financeanalyzer.model.enums.TransactionType;
import com.financeanalyzer.service.AuthService;
import com.financeanalyzer.service.ReportService;
import com.financeanalyzer.ui.AlertHelper;
import com.financeanalyzer.util.AppLogger;
import com.financeanalyzer.util.DateUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for Reports and User Settings screen.
 * Implements FR-13 (toggleable recommendations), FR-14 (monthly reports),
 * and FR-16 (user-defined fixed expense categories excluded from recommendations).
 */
public class ReportsSettingsController {

    private static final Logger LOGGER = AppLogger.getLogger(ReportsSettingsController.class);

    private final ReportService reportService;
    private final AuthService authService;
    private final CategoryDao categoryDao;
    private final FixedCategoryDao fixedCategoryDao;
    private final User currentUser;
    private final Stage primaryStage;

    private YearMonth selectedMonth = YearMonth.now();
    private Label monthLabel;
    private TextArea reportTextArea;
    private CheckBox recsCheckBox;

    // Callbacks for file operations
    private Runnable onExportCsv;
    private Runnable onImportCsv;
    private Runnable onBackupData;
    private Runnable onRestoreData;

    public ReportsSettingsController(ReportService reportService,
                                     AuthService authService,
                                     CategoryDao categoryDao,
                                     FixedCategoryDao fixedCategoryDao,
                                     User currentUser,
                                     Stage primaryStage) {
        this.reportService = Objects.requireNonNull(reportService, "ReportService must not be null.");
        this.authService = Objects.requireNonNull(authService, "AuthService must not be null.");
        this.categoryDao = Objects.requireNonNull(categoryDao, "CategoryDao must not be null.");
        this.fixedCategoryDao = Objects.requireNonNull(fixedCategoryDao, "FixedCategoryDao must not be null.");
        this.currentUser = Objects.requireNonNull(currentUser, "currentUser must not be null.");
        this.primaryStage = primaryStage;
    }

    public void setFileOperationHandlers(Runnable onExportCsv, Runnable onImportCsv,
                                         Runnable onBackupData, Runnable onRestoreData) {
        this.onExportCsv = onExportCsv;
        this.onImportCsv = onImportCsv;
        this.onBackupData = onBackupData;
        this.onRestoreData = onRestoreData;
    }

    public Parent getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("content-area");

        // Header
        Label title = new Label("Reports & Preferences");
        title.getStyleClass().add("card-title");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800;");

        // Cards
        VBox settingsCard = createPreferencesCard();
        VBox fixedCatCard = createFixedCategoriesCard();
        VBox reportCard = createReportCard();
        VBox.setVgrow(reportCard, Priority.ALWAYS);

        root.getChildren().addAll(title, settingsCard, fixedCatCard, reportCard);

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return sp;
    }

    // ------------------------------------------------------------------ preferences card

    private VBox createPreferencesCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));

        Label sectionTitle = new Label("System Preferences & Data Management");
        sectionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        // FR-13: Toggle Smart Recommendations
        recsCheckBox = new CheckBox("Enable Smart Heuristic Recommendations (FR-13)");
        recsCheckBox.setSelected(currentUser.isRecommendationsEnabled());
        recsCheckBox.setStyle("-fx-font-weight: 600; -fx-font-size: 13px;");
        recsCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            authService.updateRecommendationsPreference(newVal);
            AlertHelper.showToast(primaryStage, newVal ? "Smart recommendations enabled." : "Smart recommendations disabled.");
        });

        Label recsHelp = new Label("Analyzes category spikes, expense-to-income burn rate, savings rate, and micro-transactions.");
        recsHelp.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-padding: 0 0 0 24px;");

        // Data Management Buttons Row
        HBox dataBtns = new HBox(12);
        dataBtns.setPadding(new Insets(6, 0, 0, 0));

        Button exportCsvBtn = new Button("\u2913 Export Transactions (CSV)");
        exportCsvBtn.getStyleClass().add("button");
        exportCsvBtn.setOnAction(e -> { if (onExportCsv != null) onExportCsv.run(); });

        Button importCsvBtn = new Button("\u2912 Import Transactions (CSV)");
        importCsvBtn.getStyleClass().add("button");
        importCsvBtn.setOnAction(e -> { if (onImportCsv != null) onImportCsv.run(); });

        Button backupBtn = new Button("\u290A Backup Data (.dat)");
        backupBtn.getStyleClass().add("button");
        backupBtn.setOnAction(e -> { if (onBackupData != null) onBackupData.run(); });

        Button restoreBtn = new Button("\u290B Restore Data (.dat)");
        restoreBtn.getStyleClass().add("button");
        restoreBtn.setOnAction(e -> { if (onRestoreData != null) onRestoreData.run(); });

        dataBtns.getChildren().addAll(exportCsvBtn, importCsvBtn, backupBtn, restoreBtn);
        card.getChildren().addAll(sectionTitle, recsCheckBox, recsHelp, new Separator(), dataBtns);
        return card;
    }

    // ------------------------------------------------------------------ fixed categories card (FR-16)

    /**
     * Builds the Fixed Expense Categories management panel.
     *
     * <p>Shows all EXPENSE categories with a toggle CheckBox. Checked = fixed (excluded from
     * discretionary heuristic recommendations). Changes persist immediately to the DB.</p>
     */
    private VBox createFixedCategoriesCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));

        Label sectionTitle = new Label("Fixed Expense Categories (FR-16)");
        sectionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label help = new Label(
                "Mark categories as Fixed (e.g. Rent, EMI, Utilities) to exclude them from "
                + "heuristic recommendations. Fixed costs are expected and not actionable.");
        help.setWrapText(true);
        help.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");

        // Load expense categories and current fixed set
        List<Category> expenseCategories = categoryDao.findCategoriesForUser(currentUser.getId(), TransactionType.EXPENSE);
        Set<Long> fixedIds = fixedCategoryDao.getFixedCategoryIds(currentUser.getId());

        // Grid layout: 3 columns
        FlowPane checkboxPane = new FlowPane(12, 8);
        checkboxPane.setPadding(new Insets(8, 0, 0, 0));

        for (Category cat : expenseCategories) {
            CheckBox cb = new CheckBox(cat.getName());
            cb.setSelected(fixedIds.contains(cat.getId()));
            cb.setStyle("-fx-font-size: 13px;");

            cb.selectedProperty().addListener((obs, wasFixed, nowFixed) -> {
                if (nowFixed) {
                    fixedCategoryDao.addFixedCategory(currentUser.getId(), cat.getId());
                    AlertHelper.showToast(primaryStage, "\"" + cat.getName() + "\" marked as Fixed.");
                } else {
                    fixedCategoryDao.removeFixedCategory(currentUser.getId(), cat.getId());
                    AlertHelper.showToast(primaryStage, "\"" + cat.getName() + "\" unmarked as Fixed.");
                }
            });

            checkboxPane.getChildren().add(cb);
        }

        Label note = new Label("Changes take effect immediately. Recommendations refresh on next Dashboard load.");
        note.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted; -fx-font-style: italic;");

        card.getChildren().addAll(sectionTitle, help, checkboxPane, new Separator(), note);
        return card;
    }

    // ------------------------------------------------------------------ report card

    private VBox createReportCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));

        HBox topRow = new HBox(14);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label rTitle = new Label("Monthly Financial Statement (FR-14)");
        rTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Button prevBtn = new Button("\u25C0");
        prevBtn.getStyleClass().add("button");
        prevBtn.setOnAction(e -> {
            selectedMonth = selectedMonth.minusMonths(1);
            monthLabel.setText(DateUtil.formatDisplayMonth(selectedMonth));
            generateReport();
        });

        monthLabel = new Label(DateUtil.formatDisplayMonth(selectedMonth));
        monthLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 6px;");

        Button nextBtn = new Button("\u25B6");
        nextBtn.getStyleClass().add("button");
        nextBtn.setOnAction(e -> {
            selectedMonth = selectedMonth.plusMonths(1);
            monthLabel.setText(DateUtil.formatDisplayMonth(selectedMonth));
            generateReport();
        });

        Button refreshBtn = new Button("Refresh Report");
        refreshBtn.getStyleClass().addAll("button", "primary-button");
        refreshBtn.setOnAction(e -> generateReport());

        Button saveTxtBtn = new Button("Save as .txt");
        saveTxtBtn.getStyleClass().add("button");
        saveTxtBtn.setOnAction(e -> handleSaveReportAsText());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topRow.getChildren().addAll(rTitle, prevBtn, monthLabel, nextBtn, spacer, refreshBtn, saveTxtBtn);

        reportTextArea = new TextArea();
        reportTextArea.setEditable(false);
        reportTextArea.setWrapText(false);
        reportTextArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px;");
        reportTextArea.setPrefRowCount(18);
        VBox.setVgrow(reportTextArea, Priority.ALWAYS);

        card.getChildren().addAll(topRow, reportTextArea);

        // Generate initial report
        generateReport();

        return card;
    }

    private void generateReport() {
        String text = reportService.generateMonthlyReportText(currentUser, selectedMonth);
        reportTextArea.setText(text);
    }

    private void handleSaveReportAsText() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Monthly Financial Report");
        chooser.setInitialFileName("Financial_Report_" + selectedMonth.toString() + ".txt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt"));

        File file = chooser.showSaveDialog(primaryStage);
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(reportTextArea.getText());
                AlertHelper.showToast(primaryStage, "Report exported to " + file.getName());
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed saving report text", e);
                AlertHelper.showError("Export Error", null, "Failed to save file: " + e.getMessage());
            }
        }
    }
}

package com.financeanalyzer.app;

import com.financeanalyzer.dao.*;
import com.financeanalyzer.dao.jdbc.*;
import com.financeanalyzer.io.BackupManager;
import com.financeanalyzer.io.CsvExporter;
import com.financeanalyzer.io.CsvImporter;
import com.financeanalyzer.model.User;
import com.financeanalyzer.service.*;
import com.financeanalyzer.ui.AlertHelper;
import com.financeanalyzer.ui.controllers.*;
import com.financeanalyzer.util.AppLogger;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main JavaFX Application class.
 * Visibly demonstrates the JavaFX Application Lifecycle: init(), start(), and stop().
 */
public class MainApp extends Application {

    private static final Logger LOGGER = AppLogger.getLogger(MainApp.class);

    // Core DAOs
    private UserDao userDao;
    private CategoryDao categoryDao;
    private TransactionDao transactionDao;
    private RecurringTransactionDao recurringDao;
    private AuditLogDao auditLogDao;
    private BudgetDao budgetDao;
    private SavingsGoalDao savingsGoalDao;
    private com.financeanalyzer.dao.FixedCategoryDao fixedCategoryDao;

    // Services
    private AuthService authService;
    private TransactionService transactionService;
    private RecurringScheduler recurringScheduler;
    private BudgetService budgetService;
    private GoalService goalService;
    private RatioAllocationService ratioAllocationService;
    private InModalAnalyticsService inModalAnalyticsService;
    private HeuristicEngine heuristicEngine;
    private ReportService reportService;

    // File IO
    private BackupManager backupManager;
    private CsvExporter csvExporter;
    private CsvImporter csvImporter;

    private Stage primaryStage;
    private Scene mainScene;

    /**
     * JavaFX init() lifecycle phase.
     * Invoked on launcher thread before start() is called.
     * Initializes database schema, DAOs, services, and background scheduler.
     */
    @Override
    public void init() throws Exception {
        super.init();
        LOGGER.info("[LIFECYCLE: init] Application initializing on thread: " + Thread.currentThread().getName());

        // 1. Initialize SQLite Database & Schema
        DatabaseManager.initializeDatabase();

        // 2. Instantiate DAOs
        userDao = new JdbcUserDao();
        categoryDao = new JdbcCategoryDao();
        transactionDao = new JdbcTransactionDao();
        recurringDao = new JdbcRecurringTransactionDao();
        auditLogDao = new JdbcAuditLogDao();
        budgetDao = new JdbcBudgetDao();
        savingsGoalDao = new JdbcSavingsGoalDao();
        fixedCategoryDao = new JdbcFixedCategoryDao();

        // 3. Instantiate Services
        authService = new AuthService(userDao);
        transactionService = new TransactionService(transactionDao, categoryDao);
        recurringScheduler = new RecurringScheduler(recurringDao, transactionDao, auditLogDao);
        budgetService = new BudgetService(budgetDao, transactionDao, categoryDao);
        goalService = new GoalService(savingsGoalDao);
        ratioAllocationService = new RatioAllocationService(savingsGoalDao);
        inModalAnalyticsService = new DefaultInModalAnalyticsService(budgetDao, transactionDao, savingsGoalDao);
        heuristicEngine = new HeuristicEngine();
        reportService = new ReportService(transactionDao, budgetDao, savingsGoalDao, heuristicEngine, fixedCategoryDao);

        // 4. Instantiate File I/O Managers
        backupManager = new BackupManager(categoryDao, transactionDao, budgetDao, savingsGoalDao, recurringDao);
        csvExporter = new CsvExporter();
        csvImporter = new CsvImporter(transactionService, categoryDao);

        // 5. Start Background Scheduler
        recurringScheduler.start();

        LOGGER.info("[LIFECYCLE: init] All DAOs, services, and background recurring scheduler initialized.");
    }

    /**
     * JavaFX start() lifecycle phase.
     * Invoked on the JavaFX Application Thread.
     * Builds Primary Stage and presents the Login scene.
     */
    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        LOGGER.info("[LIFECYCLE: start] Application starting on UI thread: " + Thread.currentThread().getName());

        primaryStage.setTitle("Enhanced Personal Finance Analyzer");
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(640);

        showLoginScene();
        primaryStage.show();
    }

    /**
     * Presents the Login/Registration view.
     */
    public void showLoginScene() {
        LoginController loginController = new LoginController(authService, this::onLoginSuccess);
        Parent root = loginController.getView();
        mainScene = new Scene(root, 1024, 680);
        mainScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        primaryStage.setScene(mainScene);
    }

    /**
     * Invoked when user successfully signs in or registers.
     */
    private void onLoginSuccess(User user) {
        LOGGER.log(Level.INFO, "Transitioning to main shell for user: {0}", user.getUsername());
        showMainShell(user);
    }

    /**
     * Builds and displays the main application shell for the authenticated user.
     */
    public void showMainShell(User user) {
        MainShellController shellController = new MainShellController(
                authService, user, primaryStage, this::showLoginScene
        );

        // Controllers for active modules
        DashboardController dashController = new DashboardController(
                transactionDao, budgetDao, savingsGoalDao, transactionService, heuristicEngine, fixedCategoryDao, user
        );
        Node dashView = dashController.getView();

        TransactionsController txnController = new TransactionsController(
                transactionService, inModalAnalyticsService, user.getId(), primaryStage
        );
        Node txnView = txnController.getView();

        BudgetsController budgetsController = new BudgetsController(
                budgetService, transactionDao, categoryDao, user.getId(), primaryStage
        );
        Node budgetsView = budgetsController.getView();

        GoalsController goalsController = new GoalsController(
                goalService, ratioAllocationService, user.getId(), primaryStage
        );
        Node goalsView = goalsController.getView();

        RecurringController recController = new RecurringController(
                recurringDao, transactionService, recurringScheduler, user.getId(), primaryStage
        );
        Node recView = recController.getView();

        ReportsSettingsController reportsController = new ReportsSettingsController(
                reportService, authService, categoryDao, fixedCategoryDao, user, primaryStage
        );

        // Wire File Operation Handlers in Settings
        reportsController.setFileOperationHandlers(
                // 1. Export CSV
                () -> {
                    FileChooser fc = new FileChooser();
                    fc.setTitle("Export Transactions to CSV");
                    fc.setInitialFileName("transactions_" + user.getUsername() + ".csv");
                    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
                    File file = fc.showSaveDialog(primaryStage);
                    if (file != null) {
                        try {
                            int count = csvExporter.exportTransactions(transactionDao.findAll(user.getId()), file);
                            AlertHelper.showToast(primaryStage, "Exported " + count + " transactions to CSV.");
                        } catch (Exception ex) {
                            AlertHelper.showError("Export Failed", null, ex.getMessage());
                        }
                    }
                },
                // 2. Import CSV
                () -> {
                    FileChooser fc = new FileChooser();
                    fc.setTitle("Import Transactions from CSV");
                    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
                    File file = fc.showOpenDialog(primaryStage);
                    if (file != null) {
                        try {
                            CsvImporter.ImportSummary summary = csvImporter.importTransactions(user.getId(), file);
                            txnController.refreshTable();
                            budgetsController.refreshView();
                            String msg = String.format("Imported: %d transactions. Skipped: %d.",
                                    summary.importedCount(), summary.getSkippedCount());
                            AlertHelper.showInfo("Import Completed", null, msg);
                        } catch (Exception ex) {
                            AlertHelper.showError("Import Failed", null, ex.getMessage());
                        }
                    }
                },
                // 3. Backup Data (.dat)
                () -> {
                    FileChooser fc = new FileChooser();
                    fc.setTitle("Save Account Data Snapshot (.dat)");
                    fc.setInitialFileName("finance_backup_" + user.getUsername() + ".dat");
                    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Data Snapshot (*.dat)", "*.dat"));
                    File file = fc.showSaveDialog(primaryStage);
                    if (file != null) {
                        try {
                            backupManager.createBackup(user, file);
                            AlertHelper.showToast(primaryStage, "Backup saved successfully to " + file.getName());
                        } catch (Exception ex) {
                            AlertHelper.showError("Backup Failed", null, ex.getMessage());
                        }
                    }
                },
                // 4. Restore Data (.dat)
                () -> {
                    FileChooser fc = new FileChooser();
                    fc.setTitle("Select Data Snapshot to Restore (.dat)");
                    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Data Snapshot (*.dat)", "*.dat"));
                    File file = fc.showOpenDialog(primaryStage);
                    if (file != null) {
                        try {
                            backupManager.restoreBackup(user, file);
                            txnController.refreshTable();
                            budgetsController.refreshView();
                            goalsController.refreshView();
                            recController.refreshTable();
                            AlertHelper.showToast(primaryStage, "Data restored successfully from " + file.getName());
                        } catch (Exception ex) {
                            AlertHelper.showError("Restore Failed", null, ex.getMessage());
                        }
                    }
                }
        );

        Node reportsView = reportsController.getView();

        // Wire scheduler UI refresh callback
        recurringScheduler.setUiRefreshCallback(() -> {
            txnController.refreshTable();
            budgetsController.refreshView();
        });

        shellController.setViews(dashView, txnView, budgetsView, goalsView, recView, reportsView);

        Parent shellRoot = shellController.getView();
        mainScene = new Scene(shellRoot, 1080, 720);
        mainScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        primaryStage.setScene(mainScene);
    }

    /**
     * JavaFX stop() lifecycle phase.
     * Invoked when the application terminates.
     * Stops background recurring scheduler and flushes logs.
     */
    @Override
    public void stop() throws Exception {
        LOGGER.info("[LIFECYCLE: stop] Application shutting down on thread: " + Thread.currentThread().getName());
        if (recurringScheduler != null) {
            recurringScheduler.stop();
        }
        AppLogger.shutdown();
        super.stop();
    }

    public static void main(String[] args) {
        LOGGER.info("Launching JavaFX Application from Launcher...");
        launch(args);
    }
}

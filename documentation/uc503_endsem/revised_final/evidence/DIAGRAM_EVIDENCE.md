# Diagram evidence and editable sources

Blue denotes UI; green services/rules; purple models; orange persistence; teal actors and supporting components.

| Diagram | Type | Source evidence | Canonical editable source |
|---|---|---|---|
| 01_usecase_track | Use case | `src/main/java/com/financeanalyzer/ui/controllers/LoginController.java`<br>`src/main/java/com/financeanalyzer/ui/controllers/TransactionDialogController.java`<br>`src/main/java/com/financeanalyzer/ui/controllers/TransactionsController.java`<br>`src/main/java/com/financeanalyzer/service/AuthService.java`<br>`src/main/java/com/financeanalyzer/service/TransactionService.java` | dot |
| 02_usecase_plan | Use case | `src/main/java/com/financeanalyzer/ui/controllers/BudgetsController.java`<br>`src/main/java/com/financeanalyzer/ui/controllers/GoalsController.java`<br>`src/main/java/com/financeanalyzer/ui/controllers/RecurringController.java`<br>`src/main/java/com/financeanalyzer/ui/controllers/ReportsSettingsController.java` | dot |
| 03_activity_expense | Activity | `src/main/java/com/financeanalyzer/ui/controllers/TransactionDialogController.java`<br>`src/main/java/com/financeanalyzer/service/TransactionService.java` | dot |
| 04_activity_savings | Activity | `src/main/java/com/financeanalyzer/ui/controllers/GoalsController.java`<br>`src/main/java/com/financeanalyzer/service/GoalService.java`<br>`src/main/java/com/financeanalyzer/service/RatioAllocationService.java` | dot |
| 05_swimlane_expense | Swimlane | `src/main/java/com/financeanalyzer/ui/controllers/TransactionDialogController.java`<br>`src/main/java/com/financeanalyzer/service/TransactionService.java`<br>`src/main/java/com/financeanalyzer/dao/jdbc/JdbcCategoryDao.java` | svg |
| 06_dfd_context | DFD | `src/main/java/com/financeanalyzer/app/MainApp.java`<br>`src/main/java/com/financeanalyzer/io/BackupManager.java`<br>`src/main/java/com/financeanalyzer/io/CsvImporter.java`<br>`src/main/java/com/financeanalyzer/io/CsvExporter.java` | dot |
| 07_dfd_level1 | DFD | `src/main/java/com/financeanalyzer/service/AuthService.java`<br>`src/main/java/com/financeanalyzer/service/TransactionService.java`<br>`src/main/java/com/financeanalyzer/service/GoalService.java`<br>`src/main/java/com/financeanalyzer/service/HeuristicEngine.java`<br>`src/main/java/com/financeanalyzer/service/ReportService.java` | dot |
| 08_dfd_level2 | DFD | `src/main/java/com/financeanalyzer/service/TransactionService.java`<br>`src/main/java/com/financeanalyzer/service/RecurringScheduler.java`<br>`src/main/java/com/financeanalyzer/dao/jdbc/JdbcTransactionDao.java` | dot |
| 09_class_transactions | Class | `src/main/java/com/financeanalyzer/model/Transaction.java`<br>`src/main/java/com/financeanalyzer/model/Income.java`<br>`src/main/java/com/financeanalyzer/model/Expense.java`<br>`src/main/java/com/financeanalyzer/model/Category.java`<br>`src/main/java/com/financeanalyzer/service/TransactionService.java`<br>`src/main/java/com/financeanalyzer/dao/TransactionDao.java`<br>`src/main/java/com/financeanalyzer/dao/jdbc/JdbcTransactionDao.java` | dot |
| 10_class_planning | Class | `src/main/java/com/financeanalyzer/model/Budget.java`<br>`src/main/java/com/financeanalyzer/model/SavingsGoal.java`<br>`src/main/java/com/financeanalyzer/service/BudgetService.java`<br>`src/main/java/com/financeanalyzer/service/GoalService.java`<br>`src/main/java/com/financeanalyzer/service/RatioAllocationService.java`<br>`src/main/java/com/financeanalyzer/dao/BudgetDao.java`<br>`src/main/java/com/financeanalyzer/dao/SavingsGoalDao.java` | dot |
| 11_sequence_expense | Sequence | `src/main/java/com/financeanalyzer/ui/controllers/TransactionDialogController.java`<br>`src/main/java/com/financeanalyzer/service/TransactionService.java`<br>`src/main/java/com/financeanalyzer/dao/jdbc/JdbcCategoryDao.java`<br>`src/main/java/com/financeanalyzer/dao/jdbc/DatabaseManager.java` | puml + svg |
| 12_sequence_goal | Sequence | `src/main/java/com/financeanalyzer/ui/controllers/GoalsController.java`<br>`src/main/java/com/financeanalyzer/service/GoalService.java`<br>`src/main/java/com/financeanalyzer/dao/jdbc/JdbcSavingsGoalDao.java` | puml + svg |
| 13_communication_transaction | Communication | `src/main/java/com/financeanalyzer/ui/controllers/TransactionDialogController.java`<br>`src/main/java/com/financeanalyzer/service/TransactionService.java`<br>`src/main/java/com/financeanalyzer/dao/jdbc/DatabaseManager.java` | dot |
| 14_communication_advice | Communication | `src/main/java/com/financeanalyzer/ui/controllers/DashboardController.java`<br>`src/main/java/com/financeanalyzer/service/HeuristicEngine.java`<br>`src/main/java/com/financeanalyzer/service/CategoryService.java`<br>`src/main/java/com/financeanalyzer/rules/AnalysisContext.java`<br>`src/main/java/com/financeanalyzer/rules/Recommendation.java` | dot |
| 15_state_transaction | State chart | `src/main/java/com/financeanalyzer/ui/controllers/TransactionDialogController.java` | dot |
| 16_state_goal | State chart | `src/main/java/com/financeanalyzer/model/SavingsGoal.java`<br>`src/main/java/com/financeanalyzer/service/GoalService.java`<br>`src/main/java/com/financeanalyzer/service/RatioAllocationService.java` | dot |
| 17_component | Component | `src/main/java/com/financeanalyzer/app/MainApp.java`<br>`src/main/java/com/financeanalyzer/service/HeuristicEngine.java`<br>`src/main/java/com/financeanalyzer/service/RecurringScheduler.java`<br>`src/main/java/com/financeanalyzer/service/TransactionService.java` | dot |
| 18_deployment | Deployment | `src/main/java/com/financeanalyzer/app/Launcher.java`<br>`src/main/java/com/financeanalyzer/app/MainApp.java`<br>`src/main/java/com/financeanalyzer/dao/jdbc/DatabaseManager.java`<br>`src/main/java/com/financeanalyzer/util/ConfigLoader.java` | svg |

## Modeling boundaries

- Transaction dialog states describe UI workflow; transactions has no lifecycle/status column.
- Savings goals persist is_active and saved_paise; target attainment is derived, not a stored Completed state. Deleted means row absence.
- Transaction–Category multiplicity is a relational identifier association, not a nested Java object field.
- Expense insertion and savings allocation use direct SQL; diagrams do not replace those calls with invented DAO saves.
- Sequence self messages for Validators summarize static calls, not methods declared in the service.
- DFD data stores group related tables; repeated external entities/stores refer to the same logical sources.
- No external server, bank connector, REST API or cloud deployment is modeled.
- Native SVG sources for swimlane/deployment are editable vectors. DOT sources for use cases run through finish_diagrams.py to draw UML actors; the rendered SVG is also retained as an editable source.
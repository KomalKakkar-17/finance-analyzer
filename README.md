# Enhanced Personal Finance Analyzer (Desktop Prototype)

A robust, enterprise-grade desktop financial management application designed for university course evaluation (**UCS503L**). Built using pure modern Java with a 3-tier architecture: **JavaFX 21** for presentation, a decoupled **Service Layer** for business logic, and **Plain JDBC + DAO** over **SQLite** for relational data persistence.

---

## 1. Quick Start & Execution (Windows PowerShell)

### JVM / JRE / JDK Basics
* The **JDK** (Java Development Kit) provides the compiler (`javac`) and tools to build applications; the **JRE** (Java Runtime Environment) provides libraries to run them; the **JVM** (Java Virtual Machine) executes the compiled bytecode across platforms.

### Prerequisites
* **Java 21 (LTS)**: Temurin/Adoptium OpenJDK 21
* **Apache Maven 3.9+**

### Running the Application
Open PowerShell in the project root directory and execute:
```powershell
mvn clean javafx:run
```
*(Alternatively, compile and test first with `mvn clean test`)*

### Default Demo Account (Pre-Seeded)
The application automatically creates the database schema (`data/finance.db`) and seeds realistic financial transactions, categories, budgets, and savings goals on first startup.
* **Username**: `demo`
* **Password**: `demo123`

---

## 2. Mapping from Report Architecture to Prototype

The course project report describes a web stack (React frontend, Spring Boot/Hibernate backend, REST APIs). In accordance with Java course grading requirements, this prototype faithfully maps the identical 3-tier separation of concerns to native desktop Java:

| Report Architectural Tier | Web Stack in Report | Desktop Prototype Equivalent | Implementation Details |
| :--- | :--- | :--- | :--- |
| **Presentation Tier** | React.js SPA (Components, State, CSS) | **JavaFX 21 Desktop GUI** | FXML/Controllers (`MainShellController`, `TransactionsController`, etc.), custom component `SpendPreviewCard`, `style.css` design system. |
| **Business Logic Tier** | Spring Boot `@Service` beans, REST Controllers | **Core Java Service Layer** | `AuthService`, `TransactionService`, `BudgetService`, `GoalService`, `InModalAnalyticsService`, `RatioAllocationService`, `HeuristicEngine`, `ReportService`. |
| **Data Tier** | Spring Data JPA / Hibernate ORM + PostgreSQL | **Plain JDBC + DAO Pattern + SQLite** | Generic `Dao<T, ID>`, `DatabaseManager` (enforcing `PRAGMA foreign_keys=ON;`), minor-unit integer arithmetic (paise), zero ORM dependencies. |
| **Asynchronous Scheduling** | Spring `@Scheduled` worker | **ScheduledExecutorService** | `RecurringScheduler` running automated catch-ups and interval checks with `Platform.runLater` UI dispatch. |
| **Security & Passwords** | Spring Security BCryptPasswordEncoder | **jBCrypt Standalone Library** | Salting embedded in standard BCrypt hash; no plain text or custom salt columns. |

---

## 3. Syllabus Unit & Grading Objectives Mapping Table

Every single grading objective from the university syllabus is visibly demonstrated across the codebase:

| Unit & Objective | Demonstrated Features & Patterns | Code Location / Exact Classes |
| :--- | :--- | :--- |
| **Unit 1: Java Fundamentals & OOP** | • Abstract class `Transaction` with abstract `getSignedAmount()`<br>• Concrete subclasses `Income` (+amount) & `Expense` (-amount)<br>• Interface `Recommendation` with 5 polymorphic rule implementations<br>• Encapsulation, inheritance, static utility constants vs instance services<br>• Javadoc on all public interfaces and classes | • [`Transaction.java`](file:///src/main/java/com/financeanalyzer/model/Transaction.java)<br>• [`Income.java`](file:///src/main/java/com/financeanalyzer/model/Income.java)<br>• [`Expense.java`](file:///src/main/java/com/financeanalyzer/model/Expense.java)<br>• [`Recommendation.java`](file:///src/main/java/com/financeanalyzer/rules/Recommendation.java)<br>• [`HighCategorySpendRule.java`](file:///src/main/java/com/financeanalyzer/rules/HighCategorySpendRule.java) |
| **Unit 2: Exceptions, Collections, Generics & Utilities** | • Custom exception hierarchy (`AppException`, `BudgetExceededException`, `AllocationRatioException`, `DataAccessException`, etc.)<br>• Collections: `ArrayList` (transactions), `LinkedList` (recent activity queue), `HashSet` (unique modes), `HashMap` (category totals), `TreeMap` (sorted report categories)<br>• Explicit `Iterator` processing recurring rules<br>• Generics: generic `Dao<T, ID>`, `Result<T>`, `Pair<A,B>`, and bounded wildcard generic summation `<T extends Transaction> BigDecimal sumAmounts(List<T>)`<br>• Utilities: `Objects`, `Optional`, `BigDecimal`, `LocalDate/YearMonth/Period` | • [`AppException.java`](file:///src/main/java/com/financeanalyzer/exception/AppException.java)<br>• [`Dao.java`](file:///src/main/java/com/financeanalyzer/dao/Dao.java)<br>• [`MoneyUtil.java`](file:///src/main/java/com/financeanalyzer/util/MoneyUtil.java)<br>• [`DateUtil.java`](file:///src/main/java/com/financeanalyzer/util/DateUtil.java)<br>• [`Result.java`](file:///src/main/java/com/financeanalyzer/util/Result.java)<br>• [`RecurringScheduler.java`](file:///src/main/java/com/financeanalyzer/service/RecurringScheduler.java) |
| **Unit 3: File Handling, Serialization & Logging** | • Byte streams: `BackupManager` serializing full account snapshot via `ObjectOutputStream`/`ObjectInputStream` with `serialVersionUID`<br>• Character streams: `CsvExporter` (`BufferedWriter`) and `CsvImporter` (`BufferedReader` with per-line fault tolerance and skipped line reports)<br>• `ReportWriter` export to `.txt`<br>• Rotating `FileHandler` (1MB limit, 5 files) & `ConsoleHandler` via `AppLogger` (`java.util.logging`)<br>• External configuration loading via `app.properties` | • [`BackupManager.java`](file:///src/main/java/com/financeanalyzer/io/BackupManager.java)<br>• [`UserDataSnapshot.java`](file:///src/main/java/com/financeanalyzer/io/UserDataSnapshot.java)<br>• [`CsvExporter.java`](file:///src/main/java/com/financeanalyzer/io/CsvExporter.java)<br>• [`CsvImporter.java`](file:///src/main/java/com/financeanalyzer/io/CsvImporter.java)<br>• [`AppLogger.java`](file:///src/main/java/com/financeanalyzer/util/AppLogger.java)<br>• [`ConfigLoader.java`](file:///src/main/java/com/financeanalyzer/util/ConfigLoader.java) |
| **Unit 4: Database JDBC & DAO Design Pattern** | • SQLite `DatabaseManager` with 3NF normalized schema, foreign key enforcement (`PRAGMA foreign_keys=ON;`), and index optimization<br>• Generic `Dao<T, ID>` with `Jdbc*Dao` implementations<br>• 100% PreparedStatements (no SQL concatenation)<br>• Multi-criteria filtered SELECT (dates, categories, type, keywords, amounts)<br>• Explicit ACID database transactions (`setAutoCommit(false)`, `commit()`, `rollback()`):<br>&nbsp;&nbsp;(a) Recording an expense transaction atomically in `TransactionService`<br>&nbsp;&nbsp;(b) Distributing savings deposits across goals atomically in `RatioAllocationService`<br>&nbsp;&nbsp;(c) Replacing a user's full fixed-category selection atomically in `JdbcUserFixedCategoryDao.replaceAll`<br>• Per-user fixed categories stored in a foreign-key join table<br>• jBCrypt password hashing (no salt column stored) | • [`DatabaseManager.java`](file:///src/main/java/com/financeanalyzer/dao/jdbc/DatabaseManager.java)<br>• [`JdbcTransactionDao.java`](file:///src/main/java/com/financeanalyzer/dao/jdbc/JdbcTransactionDao.java)<br>• [`TransactionService.java`](file:///src/main/java/com/financeanalyzer/service/TransactionService.java)<br>• [`RatioAllocationService.java`](file:///src/main/java/com/financeanalyzer/service/RatioAllocationService.java)<br>• [`JdbcUserFixedCategoryDao.java`](file:///src/main/java/com/financeanalyzer/dao/jdbc/JdbcUserFixedCategoryDao.java)<br>• [`AuthService.java`](file:///src/main/java/com/financeanalyzer/service/AuthService.java) |
| **Unit 5: JavaFX GUI & Event-Driven Architecture** | • `Launcher` (classpath mode) and `MainApp` with full lifecycle (`init()`, `start()`, `stop()`)<br>• Collective layout composition: `BorderPane` (shell), `VBox`, `HBox`, `GridPane` (dialog forms), `StackPane` (overlays/badges)<br>• Modal Dialog with **live in-modal spend preview card**, background `Task`, debouncing, and `Platform.runLater`<br>• Color-coded progress bars (green <80%, amber >=80%, red >=100%)<br>• Dynamic charts: `PieChart` and `BarChart`<br>• Keyboard shortcuts (`Ctrl+N`), tooltips, Toast notifications, and CSS theme toggle (Light / Dark) | • [`MainApp.java`](file:///src/main/java/com/financeanalyzer/app/MainApp.java)<br>• [`SpendPreviewCard.java`](file:///src/main/java/com/financeanalyzer/ui/components/SpendPreviewCard.java)<br>• [`TransactionDialogController.java`](file:///src/main/java/com/financeanalyzer/ui/controllers/TransactionDialogController.java)<br>• [`BudgetsController.java`](file:///src/main/java/com/financeanalyzer/ui/controllers/BudgetsController.java)<br>• [`DashboardController.java`](file:///src/main/java/com/financeanalyzer/ui/controllers/DashboardController.java)<br>• [`style.css`](file:///src/main/resources/style.css) |

---

## 4. Functional Requirements Traceability Matrix

| Requirement ID | Description | Primary Implementing Component |
| :--- | :--- | :--- |
| **FR-01** | Secure registration and login (BCrypt hashing, min 6 characters) | `AuthService`, `LoginController` |
| **FR-02** | Manually record income and expense transactions | `TransactionService`, `TransactionDialogController` |
| **FR-03** | System default and custom category management | `CategoryDao`, `JdbcCategoryDao`, `TransactionService` |
| **FR-04** | View, edit, and delete transactions with confirmation alert | `TransactionsController`, `AlertHelper` |
| **FR-05** | Multi-criteria search and filtering (dates, category, type, amount, keyword) | `JdbcTransactionDao.findFiltered()`, `TransactionsController` |
| **FR-06** | Monthly category budget limits (one per category per month) | `BudgetService`, `JdbcBudgetDao`, `BudgetsController` |
| **FR-07** | Visual budget threshold alerts (80% Amber Warning, 100% Red Critical) | `AlertLevel`, `BudgetsController`, `SpendPreviewCard` |
| **FR-08** | Create and track multiple savings goals | `GoalService`, `JdbcSavingsGoalDao`, `GoalsController` |
| **FR-09** | Define savings ratios; enforce cumulative active ratio cap <= 100% | `GoalService`, `RatioAllocationService` |
| **FR-10** | Automated recurring transactions with background executor | `RecurringScheduler`, `RecurringController` |
| **FR-11** | Category-wise breakdown and monthly cashflow analysis | `JdbcTransactionDao`, `DashboardController`, `ReportService` |
| **FR-12** | **Signature Feature**: In-modal live preview card before saving | `InModalAnalyticsService`, `SpendPreviewCard` |
| **FR-13** | User toggle to enable/disable smart heuristic recommendations | `User.recommendationsEnabled`, `HeuristicEngine`, `ReportsSettingsController` |
| **FR-14** | Monthly financial statements viewable and exportable to .txt | `ReportService`, `ReportWriter`, `ReportsSettingsController` |
| **FR-15** | Comprehensive dashboard overview with KPIs, charts, and activity queue | `DashboardController` |
| **FR-16** | Per-user fixed expense categories excluded from flexible-spending insights; monthly reports show fixed/flexible totals and category tags | `CategoryService`, `JdbcUserFixedCategoryDao`, `HeuristicEngine`, `ReportService` |

---

## 5. Project Directory Structure

```
finance-analyzer/
├── pom.xml                                # Maven build specification with JavaFX, SQLite, jBCrypt, JUnit 5
├── data/
│   └── finance.db                         # SQLite relational database (auto-generated on startup)
├── logs/
│   └── app.log                            # Central rotating log output
├── src/
│   ├── main/
│   │   ├── java/com/financeanalyzer/
│   │   │   ├── app/                       # Launcher, MainApp (JavaFX lifecycle init/start/stop)
│   │   │   ├── model/                     # Transaction (abstract), Income, Expense, Category, etc.
│   │   │   │   └── enums/                 # TransactionType, Frequency, RuleStatus, AlertLevel
│   │   │   ├── dao/                       # Dao<T, ID> generic interface & domain interfaces
│   │   │   │   └── jdbc/                  # DatabaseManager, JdbcUserDao, JdbcTransactionDao, etc.
│   │   │   ├── service/                   # AuthService, TransactionService, BudgetService, etc.
│   │   │   ├── rules/                     # Recommendation interface + 5 heuristic rule classes
│   │   │   ├── exception/                 # AppException hierarchy
│   │   │   ├── util/                      # Validators, DateUtil, MoneyUtil, AppLogger, ConfigLoader
│   │   │   ├── io/                        # CsvExporter, CsvImporter, BackupManager, ReportWriter
│   │   │   └── ui/
│   │   │       ├── controllers/           # JavaFX screen controllers
│   │   │       ├── components/            # Custom UI components (SpendPreviewCard)
│   │   │       └── AlertHelper.java       # Standard dialogs, toasts, and confirmations
│   │   └── resources/
│   │       ├── app.properties             # Database path, thresholds, scheduler interval
│   │       └── style.css                  # Modern design system (Light/Dark themes, badges, cards)
│   └── test/
│       ├── java/com/financeanalyzer/      # Comprehensive JUnit 5 test suite (21 unit tests)
│       └── resources/app.properties       # Test configuration targeting isolated test database
├── README.md                              # Complete setup, architectural mapping, and grading guide
├── DEMO_SCRIPT.md                         # 5-minute step-by-step viva presentation click-path
└── VIVA_QA.md                             # 8 anticipated viva examination questions with detailed answers
```

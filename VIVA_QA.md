# University Examination Viva Q&A Guide

This document contains 9 anticipated viva examination questions with concise, textbook-correct answers grounded in the actual codebase of the **Enhanced Personal Finance Analyzer**.

---

### Q1: How does your application implement the 3-Tier architecture without Spring or Hibernate?
**Answer:**
The application enforces strict separation of concerns into three independent layers:
1. **Presentation Tier (JavaFX 21)**: Built with controllers (`TransactionsController`, `BudgetsController`, etc.) and UI components (`SpendPreviewCard`). Controllers contain zero business logic or SQL; they strictly capture user events and bind data to views.
2. **Business Logic Tier (Services)**: Standalone Java services (`TransactionService`, `BudgetService`, `RatioAllocationService`, etc.) that enforce business constraints, calculations, and coordination. They receive dependencies via constructor injection.
3. **Data Tier (DAO Pattern + SQLite JDBC)**: Abstracted behind generic interface `Dao<T, ID>` and concrete JDBC implementations (`JdbcTransactionDao`, `JdbcBudgetDao`, etc.) that execute parameterized queries (`PreparedStatement`) against SQLite.

---

### Q2: How does the application visibly demonstrate Object-Oriented Polymorphism?
**Answer:**
Polymorphism is demonstrated through the `Transaction` class hierarchy:
* `Transaction` is an abstract base class defining `public abstract BigDecimal getSignedAmount();`.
* `Income` extends `Transaction` and overrides `getSignedAmount()` to return positive `+amount`.
* `Expense` extends `Transaction` and overrides `getSignedAmount()` to return negative `-amount` (`amount.negate()`).
* In `MoneyUtil.sumSignedAmounts(List<T> transactions)` and `TransactionsController`, caller code invokes `txn.getSignedAmount()` polymorphically without needing `instanceof` or switch statements; dynamic method dispatch automatically computes the correct net balance.
* Polymorphism is also demonstrated by the `Recommendation` interface with 5 distinct heuristic rule implementations evaluated uniformly by `HeuristicEngine`.

---

### Q3: Where and why are explicit ACID database transactions used in the application?
**Answer:**
Explicit JDBC transactions are implemented using `conn.setAutoCommit(false)`, `conn.commit()`, and `conn.rollback()` in two critical workflows:
1. **Recording an Expense Transaction (`TransactionService.addExpense`)**: Ensures that inserting the transaction record and updating dependent audit/balance records occur atomically. If any query fails, the entire transaction rolls back cleanly.
2. **Distributing Savings Deposits (`RatioAllocationService.allocateDeposit`)**: When a user deposits funds (e.g., ₹10,000) split across multiple active savings goals (e.g., 50% to Goal A, 30% to Goal B), all goal balances are updated in a single batched database transaction. If any update fails, none are committed, preventing partial deposit allocations and data corruption.
3. **Replacing a user's fixed expense categories (`JdbcUserFixedCategoryDao.replaceAll`)**: The old category links are deleted and the selected links are inserted in one transaction. If an insert fails, rollback preserves the complete previous selection instead of leaving a partial list.

---

### Q4: Why use a per-user fixed-category join table instead of a column on `categories`?
**Answer:**
The fixed status belongs to the relationship between a user and a category, not to the category itself. A shared system category such as Rent can be fixed for one user and flexible for another. A column on `categories` would apply one global value and cause users to affect one another. The `user_fixed_categories(user_id, category_id)` join table represents that many-to-many preference explicitly, enforces uniqueness with a composite primary key, and uses foreign keys with cascading deletes.

---

### Q5: Why is `BigDecimal` and integer minor units (paise) used for monetary values instead of `double` or `float`?
**Answer:**
* Floating-point types (`double` and `float`) use binary base-2 IEEE 754 representations, which cannot precisely represent fractional decimal values like `0.10` or `0.01`, leading to cumulative rounding errors (e.g. `0.1 + 0.2 = 0.30000000000000004`).
* In financial applications, precision is non-negotiable. In our application:
  1. In the database, money is stored as an exact `INTEGER` in **paise** (minor units, 1 Rupee = 100 paise), guaranteeing that SQL aggregations (`SUM`, `AVG`) remain 100% exact without precision drift.
  2. In Java code, money is represented as `BigDecimal` with explicit scale and `RoundingMode.HALF_UP` for exact rounding and percentage math.

---

### Q6: How does the "In-Modal Live Spend Analysis" work under the hood without freezing the UI?
**Answer:**
The live preview feature in `SpendPreviewCard` coordinates three mechanisms:
1. **Property Change Listeners**: Listens to changes in the amount `TextField` and category `ComboBox`.
2. **Debouncing via ScheduledExecutorService**: Delays triggering calculation by 200 milliseconds to avoid issuing database queries on every single keystroke.
3. **Background JavaFX `Task<AnalyticsDTO>`**: The database aggregation (current month category spend, budget limit lookup, goal impact calculation) runs asynchronously on a worker thread. When complete, the result is dispatched back to the UI thread via `Platform.runLater` to update the preview card, progress bars, and alert banners smoothly at 60 FPS.

---

### Q7: What custom exceptions exist in your application, and how do checked vs unchecked exceptions differ?
**Answer:**
The application defines an `AppException` hierarchy:
* **Checked Exceptions (must be handled or declared)**:
  - `BudgetExceededException`: Thrown when an expense exceeds a hard monthly budget cap.
  - `AuthenticationException`: Thrown on invalid login credentials or registration violations.
  - `InvalidTransactionException`: Thrown when transaction inputs violate business rules (e.g., future expense date).
  - `AllocationRatioException`: Thrown when cumulative active savings goal ratios exceed 100%.
  - `InsufficientDataException`: Thrown when an analytical heuristic lacks sufficient historical data.
* **Unchecked Exceptions (RuntimeExceptions)**:
  - `DataAccessException`: Wraps low-level JDBC `SQLException`s. This decouples the service and presentation tiers from SQL-specific exceptions while avoiding silent exception swallowing.

---

### Q8: Explain the difference between the byte streams and character streams implemented in Unit 3.
**Answer:**
* **Byte Streams (`ObjectOutputStream` & `ObjectInputStream`)**: Used in `BackupManager` to serialize entire Java object graphs (`UserDataSnapshot` containing users, transactions, budgets, goals) into a binary `.dat` file. Classes declare `private static final long serialVersionUID = 1L;` to maintain version compatibility.
* **Character Streams (`BufferedWriter` & `BufferedReader`)**: Used in `CsvExporter` and `CsvImporter` to process human-readable textual CSV files line-by-line with UTF-8 encoding. `CsvImporter` implements per-line error isolation so a single malformed row does not abort the entire import and reports skipped lines. `ReportWriter` similarly uses `BufferedWriter` to write `.txt` statements.

---

### Q9: How were Java Collections specifically chosen across the project?
**Answer:**
Different collection data structures were deliberately chosen based on their performance and ordering characteristics:
1. **`ArrayList`**: Default transaction and goal lists providing fast $O(1)$ indexed access for JavaFX `TableView` observation.
2. **`LinkedList`**: Used in `TransactionService.recentActivityQueue` to implement a bounded FIFO/LIFO activity queue of the last 20 user actions with $O(1)$ insertion at head and removal at tail.
3. **`HashSet`**: Used in `TransactionService.getUniquePaymentModes()` to gather deduplicated sets of payment modes and categories with $O(1)$ contains and add operations.
4. **`HashMap`**: Used for mapping categories to month-to-date spent amounts (`Map<Category, BigDecimal>`) where fast key lookup is primary.
5. **`TreeMap`**: Used in `ReportService` to automatically sort category breakdown entries alphabetically by category name for clean report presentation.
6. **Explicit `Iterator`**: Used in `RecurringScheduler.runCatchUp()` to iterate over and mutate due recurring rules safely.

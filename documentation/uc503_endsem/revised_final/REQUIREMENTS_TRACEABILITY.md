# Requirements and source evidence

Implementation status describes connected code paths. Passing automated assertions cover only the named service/rule fixtures; GUI-only workflows remain Not Executed.

| Feature | Requirement / use case | Source files and methods | Data tables | Screen | Implementation / limitations | Test coverage |
|---|---|---|---|---|---|---|
| Accounts | FR-01 / UC-01 | `src/main/java/com/financeanalyzer/service/AuthService.java:44 register()`<br>`src/main/java/com/financeanalyzer/service/AuthService.java:77 login()`<br>`src/main/java/com/financeanalyzer/service/AuthService.java:106 logout()` | users; user_fixed_categories; user_fixed_category_initializations | Login/Register | Implemented; GUI Not Executed | T01,T02 |
| Transaction creation | FR-02 / UC-02 | `src/main/java/com/financeanalyzer/service/TransactionService.java:55 addIncome()`<br>`src/main/java/com/financeanalyzer/service/TransactionService.java:77 addExpense()` | transactions; categories | Transactions / New Transaction | Implemented expense SQL path; A19/A21/A22 pass | T03,T04; A19,A21,A22 |
| Edit and delete | FR-02 / UC-03 | `src/main/java/com/financeanalyzer/ui/controllers/TransactionsController.java:292 handleEditSelected()`<br>`src/main/java/com/financeanalyzer/ui/controllers/TransactionsController.java:295 handleDeleteSelected()` | transactions | Transactions / row menu | Implemented; type lock inspected | T05,T06; A19,A21,A22 |
| Search and filters | FR-02 / UC-04 | `src/main/java/com/financeanalyzer/service/TransactionService.java:170 findFiltered()` | transactions; categories | Transactions / Search & Filters | Partial: invalid numeric filter ignored | T07; A19,A21,A22 |
| Custom categories | FR-03 / UC-04 | `src/main/java/com/financeanalyzer/service/TransactionService.java:200 addCustomCategory()`<br>`src/main/java/com/financeanalyzer/service/TransactionService.java:193 getCategories()` | categories | Transactions / Add Category | Implemented; duplicate rejection inspected | T08; A20 |
| Budgets and alerts | FR-04 / UC-05 | `src/main/java/com/financeanalyzer/service/BudgetService.java:45 setBudget()`<br>`src/main/java/com/financeanalyzer/service/BudgetService.java:88 calculateUtilisation()`<br>`src/main/java/com/financeanalyzer/service/BudgetService.java:107 checkBudgetAlert()` | budgets; transactions | Budgets | Implemented; GUI Not Executed | T09,T10; A23,A24 |
| Goal lifecycle | FR-05 / UC-06 | `src/main/java/com/financeanalyzer/service/GoalService.java:36 createGoal()`<br>`src/main/java/com/financeanalyzer/service/GoalService.java:58 updateGoal()`<br>`src/main/java/com/financeanalyzer/service/GoalService.java:89 toggleGoalActive()`<br>`src/main/java/com/financeanalyzer/service/GoalService.java:107 deleteGoal()` | savings_goals | Savings Goals | Implemented; A25 cap check passes | T11,T12; A25,A26 |
| Deposit allocation | FR-05 / UC-06 | `src/main/java/com/financeanalyzer/service/RatioAllocationService.java:43 allocateDeposit()` | savings_goals | Savings Goals / Allocate Savings Deposit | Partial: independent rounding; surplus not persisted | T13; A25,A26 |
| Recurring entries | FR-06 / UC-07 | `src/main/java/com/financeanalyzer/service/RecurringScheduler.java:66 start()`<br>`src/main/java/com/financeanalyzer/service/RecurringScheduler.java:69 runCatchUp()`<br>`src/main/java/com/financeanalyzer/service/RecurringScheduler.java:95 processRule()`<br>`src/main/java/com/financeanalyzer/service/RecurringScheduler.java:168 stop()` | recurring_rules; transactions; execution_audit_log | Recurring Rules | Partial: occurrence operations not atomic together | T14 |
| Live preview | FR-07 / UC-02 | `src/main/java/com/financeanalyzer/service/DefaultInModalAnalyticsService.java:45 calculatePreview()` | budgets; transactions; savings_goals | Transaction dialog | Partial: current month and additive edit preview | T15 |
| Recommendations | FR-08 / UC-08 | `src/main/java/com/financeanalyzer/service/HeuristicEngine.java:59 generateRecommendations()` | transactions; categories; user_fixed_categories | Dashboard / Monthly report | Partial: historical micro-spend period limitation | A08-A18,T16 |
| Monthly statement | FR-08 / UC-08 | `src/main/java/com/financeanalyzer/service/ReportService.java:58 generateMonthlyReportText()` | transactions; budgets; savings_goals | Reports & Settings | Implemented text generation; export not executed | T17 |
| CSV import/export | EXT-02 / UC-09 | `src/main/java/com/financeanalyzer/io/CsvImporter.java:47 importTransactions()`<br>`src/main/java/com/financeanalyzer/io/CsvImporter.java:70 processLine()`<br>`src/main/java/com/financeanalyzer/io/CsvImporter.java:87 parseCsvLine()` | transactions; categories | Reports & Settings | Partial: quoting/multiline round-trip limitations | T18 |
| Backup and restore | EXT-02 / UC-09 | `src/main/java/com/financeanalyzer/io/BackupManager.java:44 createBackup()`<br>`src/main/java/com/financeanalyzer/io/BackupManager.java:77 restoreBackup()` | categories; transactions; budgets; savings_goals; recurring_rules | Reports & Settings | Partial: restore omits recurring/preferences; duplicates | T19 |
| Dashboard refresh | EXT-01 / UC-08 | `src/main/java/com/financeanalyzer/ui/controllers/DashboardController.java:74 getView()`<br>`src/main/java/com/financeanalyzer/ui/controllers/DashboardController.java:96 refreshView()`<br>`src/main/java/com/financeanalyzer/ui/controllers/DashboardController.java:104 refreshInsights()` | transactions; savings_goals | Dashboard | Implemented callback path; UI not executed | T20 |
| Fixed preferences | EXT-03 / UC-08 | `src/main/java/com/financeanalyzer/service/CategoryService.java:35 findFixedCategoryIds()`<br>`src/main/java/com/financeanalyzer/service/CategoryService.java:39 setFixed()`<br>`src/main/java/com/financeanalyzer/service/CategoryService.java:47 replaceAll()` | user_fixed_categories | Reports & Settings / Fixed Expenses | Implemented; A27/A28 rollback/isolation pass; category list loaded once | T16; A27,A28 |
| Advice preference | FR-08 / UC-08 | `src/main/java/com/financeanalyzer/ui/controllers/ReportsSettingsController.java:96 createPreferencesCard()` | users | Reports & Settings / Recommendations | Implemented successful-save refresh callback | T16 |
| Goal dialog access | EXT-04 / UC-06 | `src/main/java/com/financeanalyzer/ui/controllers/GoalsController.java:84 handleAddOrEditGoal()` | none for layout | New / Edit Savings Goal | Implemented layout fix; scaling unverified | T11 |

## Executed tests

| ID | Existing test method | Assertions / fixture | Observed result |
|---|---|---|---|
| A01 | `MoneyUtilTest.testPaiseConversion()` | 100.50 rupees ↔ 10050 paise; zero and five paise | Pass |
| A02 | `MoneyUtilTest.testFormatCurrency()` | 1250.50 formatted with 1,250.50 | Pass |
| A03 | `MoneyUtilTest.testGenericSumAmountsMethod()` | Absolute sum 6500; signed sum 3500 | Pass |
| A04 | `ValidatorsTest.testUsernameValidation()` | Valid identifiers/demo and invalid username classes | Pass |
| A05 | `ValidatorsTest.testPasswordValidation()` | Six-plus characters vs null/five characters | Pass |
| A06 | `ValidatorsTest.testAmountValidation()` | Positive amounts vs zero/negative/excess scale | Pass |
| A07 | `ValidatorsTest.testExpenseDateValidation()` | Today/past accepted; tomorrow rejected | Pass |
| A08 | `HeuristicEngineTest.testRule1HighCategorySpend()` | Concentration threshold trigger | Pass |
| A09 | `HeuristicEngineTest.highCategorySpendSkipsFixedCategoryAndUsesFlexibleDenominator()` | Fixed categories excluded; flexible denominator | Pass |
| A10 | `HeuristicEngineTest.highCategorySpendDoesNotRecommendWhenAllSpendingIsFixed()` | All fixed spending produces no advice | Pass |
| A11 | `HeuristicEngineTest.highCategorySpendWithNoFixedCategoriesBehavesAsBefore()` | 50% concentration; test assumes Food first | Pass |
| A12 | `HeuristicEngineTest.testRule2HighExpenseToIncome()` | 95% expense/income triggers | Pass |
| A13 | `HeuristicEngineTest.testRule3CategorySpike()` | 40% month-over-month increase triggers | Pass |
| A14 | `HeuristicEngineTest.categorySpikeSkipsFixedCategory()` | Fixed spike suppressed | Pass |
| A15 | `HeuristicEngineTest.categorySpikeWithoutFixedCategoriesBehavesAsBefore()` | Unfixed spike remains eligible | Pass |
| A16 | `HeuristicEngineTest.testRule4LowSavingsRate()` | 4% net savings triggers | Pass |
| A17 | `HeuristicEngineTest.testRule5FrequentMicroTransactions()` | Five expenses below 200 trigger | Pass |
| A18 | `HeuristicEngineTest.testRecommendationsMutedWhenDisabled()` | Disabled user returns empty advice | Pass |
| A19 | `TransactionServiceTest.testAddIncomeAndExpensePolymorphism()` | Income 50000; expense 1200: generated IDs and signed amounts | Pass |
| A20 | `TransactionServiceTest.testCustomCategoryCreationAndDuplicateRejection()` | Create category; reject case-insensitive duplicate | Pass |
| A21 | `TransactionServiceTest.testFutureExpenseDateValidation()` | Expense INR 500 dated today + 2 days: InvalidTransactionException | Pass |
| A22 | `TransactionServiceTest.testInvalidAmountValidation()` | Zero and -100 expense amounts: InvalidTransactionException | Pass |
| A23 | `BudgetServiceTest.testSetAndRetrieveBudget()` | Save limit; reload category/month budget | Pass |
| A24 | `BudgetServiceTest.testAlertThresholdLevels()` | 50%, 85%, 105%: NORMAL, WARNING, EXCEEDED | Pass |
| A25 | `RatioAllocationServiceTest.testRatioCapValidation()` | 60% + 30% accepted; additional 20% rejected | Pass |
| A26 | `RatioAllocationServiceTest.testAtomicDepositAllocation()` | 10000 deposit; 50%/30%: balances 5000/3000, remainder 2000 | Pass |
| A27 | `JdbcUserFixedCategoryDaoTest.replaceAllRollsBackEntireUpdateWhenAnyCategoryIsInvalid()` | Invalid replacement category: original preferences retained | Pass |
| A28 | `JdbcUserFixedCategoryDaoTest.fixedCategoryListsAreIsolatedPerUser()` | Separate account preference lists remain independent | Pass |

## Coverage limits

- All 28 existing @Test methods ran through JupiterTestEngine with lifecycle callbacks. The eight test source files include a database-isolation helper; seven classes contain the tests.
- Budget thresholds sampled: 50%, 85%, 105%. Exact 80%/100% checks remain manual.
- Ratio allocation verifies successful balances/remainder, not rollback under an injected failure. Fixed-category replacement does test rollback.
- Authentication UI/BCrypt login, transaction editing/deleting, goal toggles, recurring processing, CSV, snapshots, reports, chart rendering and scaling are not covered by these automated assertions.
- Equal-share recommendation assertion A11 depends on map iteration order. Its observed pass is not proof of portability.

## Pending acceptance cases

- T01 / FR-01 / UC-01: Register valid unused username and password >=6 characters. Expected: Account saved; shell opens; plaintext password absent. Actual: not recorded. Status: Not Executed.
- T02 / FR-01 / UC-01: Sign in with wrong password; then correct password; sign out. Expected: Wrong password rejected; correct login works; logout returns to login. Actual: not recorded. Status: Not Executed.
- T03 / FR-02 / UC-02: Add Salary income INR 50,000 today with Bank Transfer. Expected: One INCOME row; positive signed impact; values persist. Actual: not recorded. Status: Not Executed.
- T04 / FR-02 / UC-02: Try expense 0, -5, 10.555 and tomorrow; then 1,000 today. Expected: Invalid cases rejected; valid EXPENSE saves. Actual: not recorded. Status: Not Executed.
- T05 / FR-02 / UC-03: Edit amount/notes on existing income and expense. Expected: Type controls disabled; other changes save; new dialog type enabled. Actual: not recorded. Status: Not Executed.
- T06 / FR-02 / UC-03: Delete selected transaction: cancel first, confirm next. Expected: Cancel preserves row; confirm removes only chosen account row. Actual: not recorded. Status: Not Executed.
- T07 / FR-02 / UC-04: Filter by date/type/category/amount/keyword; reset. Expected: Matching user records returned; reset clears filters. Invalid numeric bound limitation recorded. Actual: not recorded. Status: Not Executed.
- T08 / FR-03 / UC-04: Create a custom expense category; repeat case-insensitive name. Expected: First saves; duplicate rejected; category selectable. Actual: not recorded. Status: Not Executed.
- T09 / FR-04 / UC-05: Set INR 1,200 monthly category budget; update same month/category. Expected: Single limit updated; zero/negative limit rejected. Actual: not recorded. Status: Not Executed.
- T10 / FR-04 / UC-05: Use controlled category spend of 79%, 80%, 99%, 100%. Expected: NORMAL, WARNING, WARNING, EXCEEDED respectively. Actual: not recorded. Status: Not Executed.
- T11 / FR-05/EXT-04 / UC-06: Create goal; inspect dialog at 100%,125%,150% scaling. Expected: Valid goal saves; fields scroll; Save/Cancel remain accessible. Actual: not recorded. Status: Not Executed.
- T12 / FR-05 / UC-06: Active total 90%; attempt new 20% goal; deactivate/reactivate. Expected: Over-cap operation rejected; valid toggles reflected. Actual: not recorded. Status: Not Executed.
- T13 / FR-05 / UC-06: Allocate INR 1,000 across 60%/30%; test induced SQL failure in isolated copy. Expected: 600/300 shares; 100 returned surplus; SQL failure rolls back batch. Actual: not recorded. Status: Not Executed.
- T14 / FR-06 / UC-07: Create overdue rule in isolated copy; catch up twice; pause rule. Expected: Dates advance; normal repeat adds no duplicate; paused rule skipped. Failure atomicity remains risk. Actual: not recorded. Status: Not Executed.
- T15 / FR-07 / UC-02: Preview new current-month expense, then edit/backdate it. Expected: New-entry budget impact agrees; edit/date limitation identified rather than hidden. Actual: not recorded. Status: Not Executed.
- T16 / FR-08/EXT-03 / UC-08: Toggle advice off/on; save fixed-category choices. Expected: Persisted preference refreshes advice; fixed concentration/spike excluded. Actual: not recorded. Status: Not Executed.
- T17 / FR-08 / UC-08: Refresh selected-month report; save TXT; include empty month. Expected: Correct monthly totals; file matches displayed text; empty month handled. Actual: not recorded. Status: Not Executed.
- T18 / EXT-02 / UC-09: CSV export then import controlled ordinary and malformed rows. Expected: Valid rows accepted, invalid counted; quoted/multiline limitation assessed. Actual: not recorded. Status: Not Executed.
- T19 / EXT-02 / UC-09: Backup/restore only in disposable database copy; restore twice. Expected: Snapshot produced; duplicate/omission risks documented; no exact recovery claim. Actual: not recorded. Status: Not Executed.
- T20 / EXT-01 / UC-08: Change income/expense/goals; return repeatedly to Dashboard/Budgets. Expected: Latest persisted totals/charts/goals/utilization; no duplicate sections. Actual: not recorded. Status: Not Executed.
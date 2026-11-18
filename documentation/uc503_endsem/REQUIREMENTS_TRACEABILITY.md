# Requirements traceability

Baseline: original FR-01 through FR-08 from MST slide 5. EXT IDs identify final-scope additions. Status refers to implementation inspection; executed evidence is separately labeled.

| Feature | Requirement | Use case | Java methods and locations | Tables | Screen | Status | Tests |
|---|---|---|---|---|---|---|---|
| Accounts | FR-01 | UC-01 | src/main/java/com/financeanalyzer/service/AuthService.java:44 :: register()<br>src/main/java/com/financeanalyzer/service/AuthService.java:77 :: login()<br>src/main/java/com/financeanalyzer/service/AuthService.java:106 :: logout() | users; user_fixed_categories; user_fixed_category_initializations | Login/Register | Implemented; current GUI not executed | T01,T02 |
| Transaction creation | FR-02 | UC-02 | src/main/java/com/financeanalyzer/service/TransactionService.java:55 :: addIncome()<br>src/main/java/com/financeanalyzer/service/TransactionService.java:77 :: addExpense() | transactions; categories | Transactions / New Transaction | Implemented; SQL path inspected | T03,T04 |
| Edit and delete | FR-02 | UC-03 | src/main/java/com/financeanalyzer/ui/controllers/TransactionsController.java:292 :: handleEditSelected()<br>src/main/java/com/financeanalyzer/ui/controllers/TransactionsController.java:295 :: handleDeleteSelected() | transactions | Transactions / row menu | Implemented; type lock inspected | T05,T06 |
| Search and filters | FR-02 | UC-04 | src/main/java/com/financeanalyzer/service/TransactionService.java:170 :: findFiltered() | transactions; categories | Transactions / Search & Filters | Partial: invalid numeric filter ignored | T07 |
| Custom categories | FR-03 | UC-04 | src/main/java/com/financeanalyzer/service/TransactionService.java:200 :: addCustomCategory()<br>src/main/java/com/financeanalyzer/service/TransactionService.java:193 :: getCategories() | categories | Transactions / Add Category | Implemented; duplicate rejection inspected | T08 |
| Budgets and alerts | FR-04 | UC-05 | src/main/java/com/financeanalyzer/service/BudgetService.java:45 :: setBudget()<br>src/main/java/com/financeanalyzer/service/BudgetService.java:88 :: calculateUtilisation()<br>src/main/java/com/financeanalyzer/service/BudgetService.java:107 :: checkBudgetAlert() | budgets; transactions | Budgets | Implemented; GUI integration not executed | T09,T10 |
| Goal lifecycle | FR-05 | UC-06 | src/main/java/com/financeanalyzer/service/GoalService.java:36 :: createGoal()<br>src/main/java/com/financeanalyzer/service/GoalService.java:58 :: updateGoal()<br>src/main/java/com/financeanalyzer/service/GoalService.java:89 :: toggleGoalActive()<br>src/main/java/com/financeanalyzer/service/GoalService.java:107 :: deleteGoal() | savings_goals | Savings Goals | Implemented; active-ratio guard inspected | T11,T12 |
| Deposit allocation | FR-05 | UC-06 | src/main/java/com/financeanalyzer/service/RatioAllocationService.java:43 :: allocateDeposit() | savings_goals | Savings Goals / Allocate Savings Deposit | Partial: independent rounding; surplus not persisted | T13 |
| Recurring entries | FR-06 | UC-07 | src/main/java/com/financeanalyzer/service/RecurringScheduler.java:66 :: start()<br>src/main/java/com/financeanalyzer/service/RecurringScheduler.java:69 :: runCatchUp()<br>src/main/java/com/financeanalyzer/service/RecurringScheduler.java:95 :: processRule()<br>src/main/java/com/financeanalyzer/service/RecurringScheduler.java:168 :: stop() | recurring_rules; transactions; execution_audit_log | Recurring Rules | Partial: occurrence operations not atomic together | T14 |
| Live preview | FR-07 | UC-02 | src/main/java/com/financeanalyzer/service/DefaultInModalAnalyticsService.java:45 :: calculatePreview() | budgets; transactions; savings_goals | Transaction dialog | Partial: current month and additive edit preview | T15 |
| Recommendations | FR-08 | UC-08 | src/main/java/com/financeanalyzer/service/HeuristicEngine.java:59 :: generateRecommendations() | transactions; categories; user_fixed_categories | Dashboard / Monthly report | Partial: historical micro-spend period limitation | A08-A18,T16 |
| Monthly statement | FR-08 | UC-08 | src/main/java/com/financeanalyzer/service/ReportService.java:58 :: generateMonthlyReportText() | transactions; budgets; savings_goals | Reports & Settings | Implemented text generation; export not executed | T17 |
| CSV import/export | EXT-02 | UC-09 | src/main/java/com/financeanalyzer/io/CsvImporter.java:47 :: importTransactions()<br>src/main/java/com/financeanalyzer/io/CsvImporter.java:70 :: processLine()<br>src/main/java/com/financeanalyzer/io/CsvImporter.java:87 :: parseCsvLine() | transactions; categories | Reports & Settings | Partial: quoting/multiline round-trip limitations | T18 |
| Backup and restore | EXT-02 | UC-09 | src/main/java/com/financeanalyzer/io/BackupManager.java:44 :: createBackup()<br>src/main/java/com/financeanalyzer/io/BackupManager.java:77 :: restoreBackup() | categories; transactions; budgets; savings_goals; recurring_rules | Reports & Settings | Partial: restore omits recurring/preferences; duplicates | T19 |
| Dashboard refresh | EXT-01 | UC-08 | src/main/java/com/financeanalyzer/ui/controllers/DashboardController.java:74 :: getView()<br>src/main/java/com/financeanalyzer/ui/controllers/DashboardController.java:96 :: refreshView()<br>src/main/java/com/financeanalyzer/ui/controllers/DashboardController.java:104 :: refreshInsights() | transactions; savings_goals | Dashboard | Implemented callback path; UI not executed | T20 |
| Fixed preferences | EXT-03 | UC-08 | src/main/java/com/financeanalyzer/service/CategoryService.java:35 :: findFixedCategoryIds()<br>src/main/java/com/financeanalyzer/service/CategoryService.java:39 :: setFixed()<br>src/main/java/com/financeanalyzer/service/CategoryService.java:47 :: replaceAll() | user_fixed_categories | Reports & Settings / Fixed Expenses | Implemented persistence; category list loaded once | T16 |
| Advice preference | FR-08 | UC-08 | src/main/java/com/financeanalyzer/ui/controllers/ReportsSettingsController.java:96 :: createPreferencesCard() | users | Reports & Settings / Recommendations | Implemented successful-save refresh callback | T16 |
| Goal dialog access | EXT-04 | UC-06 | src/main/java/com/financeanalyzer/ui/controllers/GoalsController.java:84 :: handleAddOrEditGoal() | none for layout | New / Edit Savings Goal | Implemented layout fix; scaling unverified | T11 |

## Automated case mapping

- A01: `MoneyUtilTest.testPaiseConversion()` — 100.50 rupees ↔ 10050 paise; zero and five paise.
- A02: `MoneyUtilTest.testFormatCurrency()` — 1250.50 formatted with 1,250.50.
- A03: `MoneyUtilTest.testGenericSumAmountsMethod()` — Absolute sum 6500; signed sum 3500.
- A04: `ValidatorsTest.testUsernameValidation()` — Valid identifiers/demo and invalid username classes.
- A05: `ValidatorsTest.testPasswordValidation()` — Six-plus characters vs null/five characters.
- A06: `ValidatorsTest.testAmountValidation()` — Positive amounts vs zero/negative/excess scale.
- A07: `ValidatorsTest.testExpenseDateValidation()` — Today/past accepted; tomorrow rejected.
- A08: `HeuristicEngineTest.testRule1HighCategorySpend()` — Concentration threshold trigger.
- A09: `HeuristicEngineTest.highCategorySpendSkipsFixedCategoryAndUsesFlexibleDenominator()` — Fixed categories excluded; flexible denominator.
- A10: `HeuristicEngineTest.highCategorySpendDoesNotRecommendWhenAllSpendingIsFixed()` — All fixed spending produces no advice.
- A11: `HeuristicEngineTest.highCategorySpendWithNoFixedCategoriesBehavesAsBefore()` — 50% concentration; test assumes Food first.
- A12: `HeuristicEngineTest.testRule2HighExpenseToIncome()` — 95% expense/income triggers.
- A13: `HeuristicEngineTest.testRule3CategorySpike()` — 40% month-over-month increase triggers.
- A14: `HeuristicEngineTest.categorySpikeSkipsFixedCategory()` — Fixed spike suppressed.
- A15: `HeuristicEngineTest.categorySpikeWithoutFixedCategoriesBehavesAsBefore()` — Unfixed spike remains eligible.
- A16: `HeuristicEngineTest.testRule4LowSavingsRate()` — 4% net savings triggers.
- A17: `HeuristicEngineTest.testRule5FrequentMicroTransactions()` — Five expenses below 200 trigger.
- A18: `HeuristicEngineTest.testRecommendationsMutedWhenDisabled()` — Disabled user returns empty advice.

## Planned manual case mapping

- T01 (FR-01, UC-01): Register valid unused username and password >=6 characters. Expected: Account saved; shell opens; plaintext password absent. Actual: not available. Status: Not Executed.
- T02 (FR-01, UC-01): Sign in with wrong password; then correct password; sign out. Expected: Wrong password rejected; correct login works; logout returns to login. Actual: not available. Status: Not Executed.
- T03 (FR-02, UC-02): Add Salary income INR 50,000 today with Bank Transfer. Expected: One INCOME row; positive signed impact; values persist. Actual: not available. Status: Not Executed.
- T04 (FR-02, UC-02): Try expense 0, -5, 10.555 and tomorrow; then 1,000 today. Expected: Invalid cases rejected; valid EXPENSE saves. Actual: not available. Status: Not Executed.
- T05 (FR-02, UC-03): Edit amount/notes on existing income and expense. Expected: Type controls disabled; other changes save; new dialog type enabled. Actual: not available. Status: Not Executed.
- T06 (FR-02, UC-03): Delete selected transaction: cancel first, confirm next. Expected: Cancel preserves row; confirm removes only chosen account row. Actual: not available. Status: Not Executed.
- T07 (FR-02, UC-04): Filter by date/type/category/amount/keyword; reset. Expected: Matching user records returned; reset clears filters. Invalid numeric bound limitation recorded. Actual: not available. Status: Not Executed.
- T08 (FR-03, UC-04): Create a custom expense category; repeat case-insensitive name. Expected: First saves; duplicate rejected; category selectable. Actual: not available. Status: Not Executed.
- T09 (FR-04, UC-05): Set INR 1,200 monthly category budget; update same month/category. Expected: Single limit updated; zero/negative limit rejected. Actual: not available. Status: Not Executed.
- T10 (FR-04, UC-05): Use controlled category spend of 79%, 80%, 99%, 100%. Expected: NORMAL, WARNING, WARNING, EXCEEDED respectively. Actual: not available. Status: Not Executed.
- T11 (FR-05/EXT-04, UC-06): Create goal; inspect dialog at 100%,125%,150% scaling. Expected: Valid goal saves; fields scroll; Save/Cancel remain accessible. Actual: not available. Status: Not Executed.
- T12 (FR-05, UC-06): Active total 90%; attempt new 20% goal; deactivate/reactivate. Expected: Over-cap operation rejected; valid toggles reflected. Actual: not available. Status: Not Executed.
- T13 (FR-05, UC-06): Allocate INR 1,000 across 60%/30%; test induced SQL failure in isolated copy. Expected: 600/300 shares; 100 returned surplus; SQL failure rolls back batch. Actual: not available. Status: Not Executed.
- T14 (FR-06, UC-07): Create overdue rule in isolated copy; catch up twice; pause rule. Expected: Dates advance; normal repeat adds no duplicate; paused rule skipped. Failure atomicity remains risk. Actual: not available. Status: Not Executed.
- T15 (FR-07, UC-02): Preview new current-month expense, then edit/backdate it. Expected: New-entry budget impact agrees; edit/date limitation identified rather than hidden. Actual: not available. Status: Not Executed.
- T16 (FR-08/EXT-03, UC-08): Toggle advice off/on; save fixed-category choices. Expected: Persisted preference refreshes advice; fixed concentration/spike excluded. Actual: not available. Status: Not Executed.
- T17 (FR-08, UC-08): Refresh selected-month report; save TXT; include empty month. Expected: Correct monthly totals; file matches displayed text; empty month handled. Actual: not available. Status: Not Executed.
- T18 (EXT-02, UC-09): CSV export then import controlled ordinary and malformed rows. Expected: Valid rows accepted, invalid counted; quoted/multiline limitation assessed. Actual: not available. Status: Not Executed.
- T19 (EXT-02, UC-09): Backup/restore only in disposable database copy; restore twice. Expected: Snapshot produced; duplicate/omission risks documented; no exact recovery claim. Actual: not available. Status: Not Executed.
- T20 (EXT-01, UC-08): Change income/expense/goals; return repeatedly to Dashboard/Budgets. Expected: Latest persisted totals/charts/goals/utilization; no duplicate sections. Actual: not available. Status: Not Executed.

## MST and source-comment crosswalk

- FR-01 retains authentication.
- MST FR-02/03 encompass code transaction comments FR-02..05.
- MST FR-04 budgets correspond to code FR-06/07.
- MST FR-05 goals correspond to code FR-08/09.
- MST FR-06 recurring retains its functional meaning, irrespective of comment aliases.
- MST FR-07 preview retains its functional meaning.
- MST FR-08 advice/reports correspond to code FR-13/14.
- EXT-01 dashboard corresponds to code FR-15.
- EXT-03 fixed categories corresponds to code FR-16.
- React, Spring, Hibernate, REST, MySQL/PostgreSQL and browser/cloud assumptions are not implemented.
- MST sprint/interview claims are historical proposals; no evidence of actual ceremonies or user studies supplied.
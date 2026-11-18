# Final report verification

Verified 16 November 2026. The previous report and original diagram files remain in the parent directory and were not overwritten.

## Deliverables and pagination

- Editable DOCX: `Enhanced_Personal_Finance_Analyzer_UCS503_Final.docx`.
- PDF: `Enhanced_Personal_Finance_Analyzer_UCS503_Final.pdf`, exported directly from that DOCX using LibreOffice Writer 26.8.2.2.
- **Actual rendered page count: 51**, including cover and contents. Pages 46–50 use A4 landscape for seven-column testing tables; other pages use A4 portrait.
- The DOCX opens through python-docx and LibreOffice Writer. The PDF opens through pypdf and renders through Poppler. The PDF was not independently typeset.
- Microsoft Word itself was not available for an interactive opening/pagination check. The 51-page count is real Writer pagination of the DOCX, not a text-length estimate. Word may reflow content after edits, font substitution, TOC refresh, or screenshot insertion.
- Real Word TOC and PAGE fields are present. The TOC has a populated result with page numbers matching this PDF. Numbered official headings use the corresponding Word heading levels.
- All 51 pages were rendered at 1600-pixel maximum dimension and visually reviewed across nine contact sheets. Enlarged page checks included dense requirement/test tables; diagram images were also inspected separately. No empty pages, visibly clipped diagrams, overlapping objects, broken images, or tables extending beyond the page margins were found in the final render.
- `evidence/pagination.json` records every page and its dimensions. `evidence/final_checks.json` contains structural checks. Contact sheets are retained under `evidence/layout_review/`.

## Mandatory content

All required official sections are present: 1.1–1.2; 2.1 and 2.1.1–2.1.2; 2.2; 2.3 and 2.3.1–2.3.3; 2.4; 2.5; 3.1–3.4; 4.1–4.3; and 5.1–5.3. Chapter headings 1–5 are present. The Level 2 DFD uses section 2.3.3.

The main report contains no references to the preliminary presentation, Codex, AI generation, internal source-evidence notes, or documentation-correction history. Detailed source mappings remain in supporting files.

Formatting uses Times New Roman, 12-point body text, 1.15 body line spacing, justified body paragraphs, consistent academic margins, pale blue table headers, repeating table headers, non-splitting table rows, numbered captions, and centered diagrams. Tables use a smaller readable font. The cover uses the genuine TIET mark appearing in the supplied reference; no application screenshot was fabricated.

## Diagram inventory

**18 diagram cases; all have rendered SVG and 2000-pixel-wide PNG files.** SVGs parse as XML. All 18 PNGs are embedded in the DOCX and matched by SHA-256 to the delivered renders. The additional embedded image is the institution logo.

| Type | Count | Cases |
|---|---:|---|
| Use case | 2 | Account/tracking; planning/analysis |
| Activity | 2 | Expense entry; goal creation and funding |
| Swimlane | 1 | Expense responsibilities across four lanes |
| DFD | 3 | Context; Level 1; Level 2 tracking decomposition |
| Class | 2 | Transactions; budgets and savings |
| Sequence | 2 | Expense insertion; goal creation |
| Communication | 2 | Expense persistence; recommendation generation |
| State chart | 2 | Transaction-dialog workflow; savings-goal lifecycle |
| Component | 1 | Desktop application components |
| Deployment | 1 | Local JavaFX/JVM and SQLite deployment |

The palette assigns blue to UI, green to services/rules, purple to domain models, orange to persistence, and teal to actors/supporting elements. Use-case actors are vector UML figures; sequence diagrams have activation intervals. Class/interface arrows distinguish inheritance and realization. DFDs use external entities, processes and parallel-line stores.

`evidence/DIAGRAM_EVIDENCE.md` maps every case to Java source files and explains abstraction boundaries. Canonical editable sources are DOT, PlantUML plus SVG for sequences, and native SVG for swimlane/deployment. Use-case SVG sources are retained alongside DOT; the renderer finishes the actor figures programmatically.

## Source and functional accuracy

- The application uses Java 21, JavaFX 21.0.5, Maven, SQLite JDBC 3.46.1.0 and jBCrypt 0.4. Views are constructed in Java.
- Expense creation uses direct JDBC in `TransactionService.addExpense()`; income uses the transaction DAO. Diagrams preserve that distinction.
- Savings deposits change goal balances independently of ledger cashflow; active ratios are service-validated and the remainder is returned rather than persisted as an account.
- Five recommendation rules and fixed-category exclusions are described with their actual thresholds. Preference refresh is documented without asserting GUI execution.
- The schema has nine tables. Account ownership is generally represented by identifiers in domain objects.
- Recurring entries use startup catch-up and an in-process scheduled executor. No always-on external scheduler is implied.
- Transaction state charts describe UI states; savings completion is derived progress, not an invented stored state.
- CSV/snapshot limitations, recurring transaction-boundary limits, preview/date limits and small-allocation rounding are retained as product constraints.

`REQUIREMENTS_TRACEABILITY.md` links feature, requirement, use case, classes/method locations, tables, screen and tests. Source locations were resolved against the repository.

## Compilation and executed testing

| Check | Observed result | Evidence |
|---|---|---|
| Fresh Java compilation | 85 production Java files, exit 0, `javac --release 21` | `evidence/compile.txt` |
| Test compilation | 8 test source files, exit 0 | `evidence/test_compile.txt` |
| Maven offline compile | BUILD SUCCESS, exit 0; Maven found target classes up to date | `evidence/maven_compile.txt` |
| Existing JUnit tests | **28 passed, 0 failed, 0 aborted** | `evidence/junit_results.txt` |
| Application integrity | All baseline source/resource/pom/data hashes unchanged | `evidence/application_baseline.json`, `evidence/final_checks.json` |

The existing tests ran through `JupiterTestEngine`, including lifecycle callbacks, in seven test classes. A temporary classpath and a temporary copy of test configuration selected a unique disposable SQLite database. A safety guard checked that database URL before tests initialized persistence. The application was not launched, and the scheduler did not run against the application database.

The rollback integration test intentionally logs a foreign-key SQL error; its assertions verify that the previous fixed-category set survives. This expected log is not a failed test. No application or dependency changes were used to obtain passing results.

Coverage limits:

- Authentication screen/BCrypt login, editing/deletion UI, chart rendering, recurring catch-up, reports, CSV, snapshot recovery and display scaling remain **Not Executed** as complete workflows.
- Budget alert tests sample 50%, 85% and 105%, not the exact 80%/100% boundaries.
- The allocation test checks a successful 50%/30% distribution and reloaded balances; it does not inject failure to prove allocation rollback.
- The equal-share concentration test has an order-sensitive assertion. Its observed pass does not eliminate that risk.
- No performance benchmark, security assessment, usability study or peer-test execution is claimed.

The report contains 28 executed automated cases A01–A28 and 20 manual/additional acceptance cases T01–T20. Actual input fixtures and assertion scope were checked against the test bodies. Manual cases remain Not Executed.

## Remaining completion fields

1. Eight screenshot spaces in Section 4.3, figures 4.3–4.10: Login/Registration, Dashboard, Transactions, Budgets, Savings Goals, Recurring Transactions, Reports/Analytics, Settings/Recommendations.
2. Cover: group number, faculty name, and submission month/year. Student names and roll numbers are retained; BE third-year Computer Engineering matches the supplied project context.
3. Section 5.3: real peer identity, date, observations, acceptance and signature fields remain blank.
4. GUI and additional acceptance checks require genuine execution before their statuses can be changed.

No Java source, pom.xml, database, schema, dependency version or application behavior was changed. Existing user source modifications were preserved. Nothing was committed or pushed.

# UCS503 report verification

Prepared: 16 November 2026. Application source, Maven configuration and production database were not modified for this documentation task. Existing uncommitted pre-demo fixes were preserved. No commit or push was performed.

## Deliverables and page count

- Editable report: `Enhanced_Personal_Finance_Analyzer_UCS503_EndSem.docx`.
- PDF: `Enhanced_Personal_Finance_Analyzer_UCS503_EndSem.pdf`.
- PDF actual page count: **50**, checked using pypdf and rendered with the bundled Poppler executable.
- DOCX rendered page count: **unverified**. The document reopens with python-docx and its OOXML archive is valid, but Word/LibreOffice layout was not available through the packaged renderer.
- The packaged DOCX renderer was attempted and failed: `LibreOffice soffice.exe was not found on PATH`. Word COM extraction was also unavailable (`E_ACCESSDENIED`). No environment or dependency changes were made to bypass these restrictions.
- The PDF is generated independently from the same complete content/page model. It is **not a verified conversion of the DOCX**, so page-layout identity between formats is not claimed. Cached contents-page numbers follow the verified PDF model. Update Word's table of contents/page fields after final editing and verify its pagination before submission.
- Approximate substantive length: more than 10,000 extracted PDF words plus figures, tables and screenshot spaces. No blank padding pages were inserted.

## Structure and formatting

All official sections are present:

- 1 Project Selection Phase: 1.1 Software Bid; 1.2 Project Overview.
- 2 Analysis Phase: 2.1 Use Cases; 2.1.1 diagrams; 2.1.2 templates; 2.2 activity/swimlane diagrams; 2.3 DFDs with 2.3.1, 2.3.2 and corrected 2.3.3; 2.4 IEEE-style SRS; 2.5 stories/cards.
- 3 Design Phase: 3.1 class; 3.2 sequence; 3.3 collaboration; 3.4 state charts.
- 4 Implementation: 4.1 component; 4.2 deployment; 4.3 screenshots.
- 5 Testing: 5.1 plan; 5.2 cases; 5.3 peer report template.

The cover follows the official specimen's information order. The outdated second-year cohort wording is replaced with an explicit cohort placeholder. Three names/roll numbers come from the supplied MST title slide and require team confirmation. The official duplicated DFD subsection number is corrected without dropping a required section.

A4; academic serif/Times New Roman; approximately 12-point body; numbered figures and tables; page footer numbers; hierarchical headings; automatic Word TOC field with cached entries. DOCX contains 18 embedded diagram images and 19 tables. PDF tables were checked for frame overflow programmatically; none was left outside a page frame. Page images were reviewed for clipping, spacing, diagram placement and obvious layout defects. DOCX visual rendering remains an open gate.

## Diagram inventory

Total: **18**, each with editable source plus SVG and 2000-pixel-wide PNG rendering. All rendering jobs completed successfully.

| Type | Cases | Count |
|---|---|---:|
| Use case | Account/tracking; planning/analysis | 2 |
| Activity | Expense entry; create/fund goal | 2 |
| Swimlane | Expense responsibilities | 1 |
| DFD | Context; Level 1; Level 2 tracking | 3 |
| Class | Transaction management; budget/savings services | 2 |
| Sequence | Add expense; create goal | 2 |
| Collaboration/communication | Expense persistence; recommendation generation | 2 |
| State chart | Conceptual transaction dialog; goal active flag | 2 |
| Component | Local Java application components | 1 |
| Deployment | Single-device Java desktop | 1 |

Design-phase minimums are met: two each of class, sequence, collaboration and state-chart diagrams. DOT sources are editable Graphviz representations; sequence sources use PlantUML. SVGs are also editable vector files. The custom sequence SVG renderer abbreviates long message arguments for readability; the PlantUML files retain their fuller labels.

DFD circles are processes, rectangles are external entities, and parallel horizontal rules are logical stores. Level 1 repeats entity/store symbols for readability; repeated labels refer to the same entity or logical stores. Context-level grouped inputs/outputs are decomposed at Level 1; Level 2 retains the tracking process's parent boundary. Scheduler and SQLite are internal to the system, not external use-case actors. Class relationships using userId/categoryId are described as relational identifier references, not invented nested Java objects. Goal completion is derived, not a persisted COMPLETED state.

## Source evidence

- `evidence/source_manifest.json`: SHA-256 hashes of application source/resources/tests, pom.xml and the existing database file.
- `evidence/source_index.json`: all 85 production Java files and method locations.
- `REQUIREMENTS_TRACEABILITY.md`: feature/evidence matrix, Java methods, tables, screens, implementation status, requirement/use-case/test mappings and original-ID crosswalk.
- Requirements retain MST FR-01..FR-08. Source comments have a different granular numbering; aliases are documented, not silently conflated.
- Actual technology: Java 21, JavaFX 21.0.5, Maven, SQLite JDBC 3.46.1.0, jBCrypt 0.4 and JUnit Jupiter 5.10.2. React/Spring/Hibernate/REST/MySQL/PostgreSQL/cloud descriptions are historical MST errors, not implemented components.
- No evidence of actual interviews, sprint ceremonies, peer reviews or production performance was fabricated.

## Current testing evidence

- Fresh isolated production compilation: **85 files, exit 0** (`evidence/compile.txt`).
- Isolated test-source compilation: **8 files, exit 0** (`evidence/test_compile.txt`).
- Existing database-free tests: **18 passed, 0 failed in this run** (`evidence/existing_pure_tests.txt`). Classes: MoneyUtilTest, ValidatorsTest, HeuristicEngineTest.
- The temporary runner invokes existing @BeforeEach and @Test methods. This is not a complete Maven/Surefire run and not a GUI test.
- Integration cases for transactions, budgets, ratio allocation and fixed categories exist but were not executed for this report. A prior audit recorded a cached Surefire plugin failure; it was not represented as a current passing Maven test run.
- A11 has an ordering-sensitive assertion demanding Food when two equally eligible 50% categories exist. Its current pass does not establish deterministic cross-process ordering.
- Manual/GUI cases T01..T20: **Not Executed**, actual results unavailable. Expected outcomes are specified acceptance criteria, not invented observations.
- No application startup, recurring catch-up or restore was executed. No production database write was required.

## Missing screenshots and personal details

Eight genuine screenshots are missing: login/registration, dashboard, transactions, budgets, savings goals, recurring transactions, monthly report, settings/recommendations. No `report_references/screenshots/` images were available. All eight spaces explicitly say that a screenshot was not supplied; no fake UI image is used.

Cover placeholders: year/branch, group number, faculty name, submission month/year. Confirm the three MST team names and roll numbers. Peer-testing fields, tester/date/environment, observations/defects, retest result and signatures require completion by a real tester.

## Known application limitations retained in the report

1. Restore appends records, omits recurring rules/preferences and is not atomic overall; repeat restore can duplicate data.
2. Live preview uses the current month and adds the entire amount, including when editing/backdating.
3. Tiny deposits may over-allocate because each goal share rounds independently; unallocated remainder has no persisted ledger.
4. Recurring transaction/audit/date updates are not one atomic occurrence operation.
5. Historical micro-spend advice uses today and bounded recent lists rather than an exact report-month query.
6. Local SQLite/snapshots are not encrypted; snapshot contains account/financial data and uses unrestricted Java deserialization.
7. Invalid numeric search bounds are silently ignored; some DAO methods are not account-scoped.
8. Navigation refresh, locked edit type, preference refresh and goal-dialog scrolling are in the source, but current GUI and display-scaling verification remain open.

## Exact completion steps before submission

1. Open the DOCX in Microsoft Word, update the entire table of contents and page fields, then inspect pagination, tables and figures. Export a matching PDF from Word if exact cross-format parity is required.
2. Fill the cover details and confirm the team information.
3. Replace the eight labeled spaces with genuine screenshots from an isolated demonstration copy; do not overwrite the production database.
4. Run/record manual cases T01..T20 as time permits, including 100%, 125%, 150% goal-dialog scaling. Update actual result/status only with observed evidence.
5. Have a real peer complete the testing record and signatures if required.

The content and diagram generation are complete. Submission readiness is conditional on those missing evidence/details and the Word rendering check, not falsely certified as final acceptance.

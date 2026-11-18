# UCS503 end-semester report deliverables

Start with the editable DOCX and the 50-page PDF. `REPORT_VERIFICATION.md` records completed checks and all remaining submission work. `REQUIREMENTS_TRACEABILITY.md` contains the expanded evidence matrix and requirement mappings.

The PDF and DOCX use the same report content model, but the PDF is generated directly, not by converting Word. DOCX rendering/pagination could not be verified in this environment; update Word's contents/page fields and inspect the document before final submission.

## Editable diagrams

- `diagrams/source/*.dot`: Graphviz sources for UML/DFD diagrams.
- `diagrams/source/*.puml`: PlantUML sequence-diagram sources.
- `diagrams/rendered/*.svg`: vector diagrams.
- `diagrams/rendered/*.png`: high-resolution diagrams embedded in the report.
- `diagrams/manifest.json`: diagram inventory.

## Rebuilding documentation

The scripts do not import/run the application or connect to SQLite. They read source text and existing test logs. This machine's Codex runtime paths are explicit in `render_diagrams.cjs`; adjust those documentation-only paths when rebuilding elsewhere. Dependencies required by the builders are python-docx, ReportLab, Pillow, Graphviz WASM (@viz-js/viz) and sharp, already supplied by the runtime used here. No application dependencies were installed or upgraded.

1. Run `build_diagrams.py` with the document runtime's Python.
2. Run `render_diagrams.cjs` with its Node runtime.
3. Run `build_report.py` with the document runtime's Python.
4. Render/inspect the resulting Word document in an available document renderer.

The report's verified test outcomes are inputs from `evidence/existing_pure_tests.txt`. Regeneration does not execute tests or claim new results. Source hashes describe the source snapshot at report generation. `report_content.py` and `report_content.json` preserve the editable text/page model.

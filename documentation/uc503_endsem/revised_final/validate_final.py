from pathlib import Path
import json,re,hashlib,ast,zipfile,collections,xml.etree.ElementTree as ET
from docx import Document
from pypdf import PdfReader
ROOT=Path(__file__).resolve().parent;REPO=ROOT.parents[2]
docpath=ROOT/'Enhanced_Personal_Finance_Analyzer_UCS503_Final.docx';pdfpath=docpath.with_suffix('.pdf')
doc=Document(docpath);pdf=PdfReader(pdfpath)
alltext='\n'.join([p.text for p in doc.paragraphs]+[c.text for t in doc.tables for r in t.rows for c in r.cells])
pdftext='\n'.join(p.extract_text() for p in pdf.pages)
required=['1. Project Selection Phase','1.1 Software Bid','1.2 Project Overview','2. Analysis Phase','2.1 Use Cases','2.1.1 Use-Case Diagrams','2.1.2 Use Case Templates','2.2 Activity Diagram and Swimlane Diagrams','2.3 Data Flow Diagrams','2.3.1 DFD Level 0','2.3.2 DFD Level 1','2.3.3 DFD Level 2','2.4 Software Requirement Specification in IEEE Format','2.5 User Stories and Story Cards','3. Design Phase','3.1 Class Diagram','3.2 Sequence Diagram','3.3 Collaboration Diagram','3.4 State Chart Diagrams','4. Implementation','4.1 Component Diagrams','4.2 Deployment Diagrams','4.3 Screenshots','5. Testing','5.1 Test Plan','5.2 Test Cases','5.3 Test Reports by Peers']
norm=lambda s:re.sub(r'\s+',' ',s).lower()
missing=[s for s in required if norm(s) not in norm(alltext) or norm(s) not in norm(pdftext)]
forbidden=re.findall(r'\bMST\b|\bCodex\b|AI.generation|Source evidence:|documentation correction',alltext,re.I)
baseline=json.loads((ROOT/'evidence/application_baseline.json').read_text(encoding='utf-8-sig'))
changed=[]
for row in baseline:
 p=Path(row['path']);new=hashlib.sha256(p.read_bytes()).hexdigest().upper() if p.exists() else 'MISSING'
 if new!=row['sha256'].upper():changed.append(str(p))
manifest=json.loads((ROOT/'diagrams/manifest.json').read_text());counts=collections.Counter(x['type'] for x in manifest)
diagram_errors=[]
for m in manifest:
 svg=ROOT/'diagrams/rendered'/(m['id']+'.svg');png=svg.with_suffix('.png')
 if not svg.exists() or not png.exists():diagram_errors.append(m['id']+' missing')
 else:
  ET.parse(svg)
  if not re.search('#(?:[dDeEfF][0-9a-fA-F]{5}|[245aA][0-9a-fA-F]{5})',svg.read_text(encoding='utf8')):diagram_errors.append(m['id']+' palette not detected')
with zipfile.ZipFile(docpath) as z:
 media={hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if n.startswith('word/media/')}
 unembedded=[m['id'] for m in manifest if hashlib.sha256((ROOT/'diagrams/rendered'/(m['id']+'.png')).read_bytes()).hexdigest() not in media]
 xml=z.read('word/document.xml').decode();toc=' TOC ' in xml and '\\o' in xml
images=len(doc.inline_shapes);placeholders=alltext.count('Screenshot to be inserted')
empty=[i+1 for i,p in enumerate(pdf.pages) if len(p.extract_text())<90]
logs=(ROOT/'evidence/junit_results.txt').read_text(encoding='utf-8-sig');passed=logs.count('CASE|SUCCESSFUL|');failed=logs.count('CASE|FAILED|')
summary={'page_count':len(pdf.pages),'mandatory_sections_missing':missing,'forbidden_text':forbidden,'diagram_counts':dict(counts),'diagram_errors':diagram_errors,'unembedded_diagrams':unembedded,'embedded_images':images,'screenshot_placeholders':placeholders,'empty_pages':empty,'application_files_changed':changed,'automated_passed':passed,'automated_failed':failed,'toc_field':toc,'docx_opened':True,'pdf_readable':True}
(ROOT/'evidence/final_checks.json').write_text(json.dumps(summary,indent=2),encoding='utf8')
assert not missing and not forbidden and not changed and not diagram_errors and not unembedded and not empty,summary
assert 45<=len(pdf.pages)<=55 and passed==28 and failed==0 and placeholders==8 and images==19
for k in ['Class','Sequence','Communication','State chart']:assert counts[k]>=2
# Source evidence and test coverage remain separate from the submitted report.
tree=ast.parse((ROOT.parent/'report_content.py').read_text(encoding='utf8'));features=[]
for n in tree.body:
 if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='features' for t in n.targets):features=ast.literal_eval(n.value)
mapping=json.loads((ROOT/'evidence/test_mapping.json').read_text())
extra={'FR-02':'A19,A21,A22','FR-03':'A20','FR-04':'A23,A24','FR-05':'A25,A26','EXT-03':'A27,A28'}
md=['# Requirements and source evidence','', 'Implementation status describes connected code paths. Passing automated assertions cover only the named service/rule fixtures; GUI-only workflows remain Not Executed.','', '| Feature | Requirement / use case | Source files and methods | Data tables | Screen | Implementation / limitations | Test coverage |','|---|---|---|---|---|---|---|']
for feat,cls,methods,tables,screen,status,req,uc,tests in features:
 files=list((REPO/'src/main/java').rglob(cls+'.java'));refs=[]
 for method in methods.split(','):
  if files:
   p=files[0];s=p.read_text(encoding='utf8');match=re.search(r'\b'+re.escape(method)+r'\s*\(',s);line=str(s[:match.start()].count('\n')+1) if match else 'unresolved'
   refs.append('`'+p.relative_to(REPO).as_posix()+':'+line+' '+method+'()`')
 if req in extra:tests+='; '+extra[req]
 status=status.replace('current GUI not executed','GUI Not Executed').replace('GUI integration not executed','GUI Not Executed').replace('Implemented; SQL path inspected','Implemented expense SQL path; A19/A21/A22 pass').replace('Implemented; active-ratio guard inspected','Implemented; A25 cap check passes').replace('Implemented persistence; category list loaded once','Implemented; A27/A28 rollback/isolation pass; category list loaded once')
 md.append('| '+' | '.join([feat,req+' / '+uc,'<br>'.join(refs),tables,screen,status,tests])+' |')
md+=['','## Executed tests','', '| ID | Existing test method | Assertions / fixture | Observed result |','|---|---|---|---|']
for ident,cls,method,expected in mapping['automated']:md.append(f'| {ident} | `{cls}.{method}()` | {expected} | Pass |')
md+=['','## Coverage limits','','- All 28 existing @Test methods ran through JupiterTestEngine with lifecycle callbacks. The eight test source files include a database-isolation helper; seven classes contain the tests.','- Budget thresholds sampled: 50%, 85%, 105%. Exact 80%/100% checks remain manual.','- Ratio allocation verifies successful balances/remainder, not rollback under an injected failure. Fixed-category replacement does test rollback.','- Authentication UI/BCrypt login, transaction editing/deleting, goal toggles, recurring processing, CSV, snapshots, reports, chart rendering and scaling are not covered by these automated assertions.','- Equal-share recommendation assertion A11 depends on map iteration order. Its observed pass is not proof of portability.','', '## Pending acceptance cases','']
for ident,req,inp,expected,uc in mapping['manual']:md.append(f'- {ident} / {req} / {uc}: {inp} Expected: {expected} Actual: not recorded. Status: Not Executed.')
(ROOT/'REQUIREMENTS_TRACEABILITY.md').write_text('\n'.join(md),encoding='utf8')
evidence={'01_usecase_track':['LoginController','TransactionDialogController','TransactionsController','AuthService','TransactionService'],'02_usecase_plan':['BudgetsController','GoalsController','RecurringController','ReportsSettingsController'],'03_activity_expense':['TransactionDialogController','TransactionService'],'04_activity_savings':['GoalsController','GoalService','RatioAllocationService'],'05_swimlane_expense':['TransactionDialogController','TransactionService','JdbcCategoryDao'],'06_dfd_context':['MainApp','BackupManager','CsvImporter','CsvExporter'],'07_dfd_level1':['AuthService','TransactionService','GoalService','HeuristicEngine','ReportService'],'08_dfd_level2':['TransactionService','RecurringScheduler','JdbcTransactionDao'],'09_class_transactions':['Transaction','Income','Expense','Category','TransactionService','TransactionDao','JdbcTransactionDao'],'10_class_planning':['Budget','SavingsGoal','BudgetService','GoalService','RatioAllocationService','BudgetDao','SavingsGoalDao'],'11_sequence_expense':['TransactionDialogController','TransactionService','JdbcCategoryDao','DatabaseManager'],'12_sequence_goal':['GoalsController','GoalService','JdbcSavingsGoalDao'],'13_communication_transaction':['TransactionDialogController','TransactionService','DatabaseManager'],'14_communication_advice':['DashboardController','HeuristicEngine','CategoryService','AnalysisContext','Recommendation'],'15_state_transaction':['TransactionDialogController'],'16_state_goal':['SavingsGoal','GoalService','RatioAllocationService'],'17_component':['MainApp','HeuristicEngine','RecurringScheduler','TransactionService'],'18_deployment':['Launcher','MainApp','DatabaseManager','ConfigLoader']}
dm=['# Diagram evidence and editable sources','','Blue denotes UI; green services/rules; purple models; orange persistence; teal actors and supporting components.','', '| Diagram | Type | Source evidence | Canonical editable source |','|---|---|---|---|']
for m in manifest:
 refs=[]
 for cls in evidence[m['id']]:
  files=list((REPO/'src/main/java').rglob(cls+'.java'));refs += ['`'+p.relative_to(REPO).as_posix()+'`' for p in files]
 name=m['id'];ext='svg' if name in ['05_swimlane_expense','18_deployment'] else ('puml + svg' if m['type']=='Sequence' else 'dot')
 dm.append('| '+name+' | '+m['type']+' | '+'<br>'.join(refs)+' | '+ext+' |')
dm+=['','## Modeling boundaries','','- Transaction dialog states describe UI workflow; transactions has no lifecycle/status column.','- Savings goals persist is_active and saved_paise; target attainment is derived, not a stored Completed state. Deleted means row absence.','- Transaction–Category multiplicity is a relational identifier association, not a nested Java object field.','- Expense insertion and savings allocation use direct SQL; diagrams do not replace those calls with invented DAO saves.','- Sequence self messages for Validators summarize static calls, not methods declared in the service.','- DFD data stores group related tables; repeated external entities/stores refer to the same logical sources.','- No external server, bank connector, REST API or cloud deployment is modeled.','- Native SVG sources for swimlane/deployment are editable vectors. DOT sources for use cases run through finish_diagrams.py to draw UML actors; the rendered SVG is also retained as an editable source.']
(ROOT/'evidence/DIAGRAM_EVIDENCE.md').write_text('\n'.join(dm),encoding='utf8')
print(json.dumps(summary,indent=2))

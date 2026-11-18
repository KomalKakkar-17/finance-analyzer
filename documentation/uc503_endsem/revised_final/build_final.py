from pathlib import Path
import json,re,sys,copy
from docx import Document
from docx.shared import Inches,Pt,RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.section import WD_SECTION_START,WD_ORIENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from PIL import Image
ROOT=Path(__file__).resolve().parent; OLD=ROOT.parent
P=json.loads((OLD/'report_content.json').read_text(encoding='utf8'))
def para(t):return {'kind':'p','text':t}
def head(t):return {'kind':'h','text':t}
def tbl(c,h,r,w=None):return {'kind':'table','caption':c,'headers':h,'rows':r,'widths':w}
def setpage(i,title,items):P[i-1]['title']=title;P[i-1]['items']=items
setpage(6,'Application Modules and Operating Environment',[
 P[5]['items'][0],head('Desktop architecture'),
 para('The interface is constructed programmatically with JavaFX controls, layouts and charts. Controllers call Java services through ordinary method calls. DAO interfaces define persistence operations, and JDBC implementations read and write the local SQLite database. TransactionService and RatioAllocationService also contain direct JDBC transactions for expense insertion and goal allocation.'),
 head('Session and navigation'),para('Registration or sign-in establishes one current local account. MainShellController presents Dashboard, Transactions, Budgets, Savings Goals, Recurring Rules, and Reports & Settings. Navigation callbacks refresh financial views from saved records. Signing out clears the session and returns to the account screen.'),
 head('Operating constraints'),para('The application requires Java 21, compatible JavaFX native libraries and a writable local working directory. Its relative database path is data/finance.db. Recurring rules are processed while the application runs; startup catch-up handles overdue dates. The application records financial activity manually and does not connect to a bank or initiate payments.')])
P[5]['section']='1.2 Application modules'
# Remove internal editorial material while retaining financial and implementation detail.
drop=[r'MST',r'retrospective academic statement',r'The official contents repeat',r'not a Java functional interface despite',r'There is no deployed React',r'The report build does not',r'Example expectation, not an executed']
for i,p in enumerate(P):
 p['items']=[x for x in p['items'] if x['kind']!='source' and not (x['kind'] in ['h','p'] and any(re.search(q,x.get('text',''),re.I) for q in drop))]
for p in P:
 for x in p['items']:
  if x['kind']=='p':
   x['text']=x['text'].replace('The proposed benefit is','The benefit is').replace('No administrator or password-reset workflow is modeled because the source does not expose those capabilities.','The application has no administrator role or password-reset workflow.')
   x['text']=x['text'].replace('No JdbcTransactionDao.save() call is inserted into the expense sequence because it is absent from addExpense().','The expense path executes SQL directly in addExpense(); income persistence uses the transaction DAO.')
   if 'Security limitations include' in x['text']:x['text']='The local SQLite database and serialized snapshots are unencrypted. Snapshots include a User object and its password hash, so access depends on operating-system file permissions. Snapshot deserialization is suitable only for trusted local files. Account authentication uses BCrypt; password recovery and remote roles are outside the application scope.'
P[4]['items'] += [head('Development approach'),para('The system is organized into functional modules with separate presentation, service, domain and persistence responsibilities. Interface-based DAOs allow storage operations to be implemented independently of most controller code. Existing unit and integration tests check monetary utilities, validation, recommendation rules and selected persistence operations. This modular approach supports focused implementation and verification of individual workflows.')]
P[20]['items'][1]=para('The SRS defines the purpose, operating environment, user characteristics, functional requirements, quality requirements, interfaces and data constraints of the desktop application. Functional requirements are grouped by user goal; acceptance criteria specify observable behavior and connect requirements to use cases and tests.')
P[20]['items'][2]=para('The product boundary is one JavaFX application process and its local files. Its principal functions are manual ledger management, budget planning, savings allocation, recurring entries and interpretable financial analysis. The account holder supplies financial data and controls local preferences and file operations.')
for item in P[20]['items']:
 if item['kind']=='table':
  item['rows'][-1]=['FR / EXT / UC / T / A','Functional requirement / additional requirement / use case / manual test / automated test.']
setpage(27,'Financial Recommendation Requirements',[
 para('HeuristicEngine evaluates five independent Recommendation implementations against an AnalysisContext. Each rule returns Optional<String>: a present value is displayed as advice; an empty value indicates that the rule has not triggered. The account preference can suppress the complete recommendation list.'),
 tbl('Financial recommendation rules',['Rule','Condition','Interpretation'],[
 ['HighCategorySpendRule','Flexible category share > 40%','Fixed-category expenses are removed from the denominator and excluded as candidates.'],
 ['HighExpenseToIncomeRule','Expenses / income > 90%','Warns when the monthly expense burden approaches income; zero income is handled separately.'],
 ['CategorySpikeRule','Increase >= 25%; previous amount >= INR 500','Compares category spending with the preceding month; fixed categories are excluded.'],
 ['LowSavingsRateRule','Net savings / income < 10%','Uses income minus expenses rather than savings-goal deposits.'],
 ['FrequentMicroTransactionsRule','At least 5 expenses below INR 200 in the recent window','Uses the supplied recent transaction list and today-based date window.']],[.25,.31,.44]),
 head('Preference handling'),para('CategoryService supplies account-specific fixed-category identifiers. The engine enriches the analysis context before iterating over rules. ReportsSettingsController persists recommendation preference changes through AuthService and invokes the dashboard insight callback after a successful update.'),
 head('Failure handling and analytical limits'),para('An exception in one rule is logged without preventing the remaining rules from running. Advice is explanatory and does not block a transaction. Monthly totals are selected by report month, while the micro-transaction rule uses a bounded recent list and a today-based window; its advice can therefore differ from a historical-month interpretation.')])
P[26]['section']='2.4 Recommendation requirements'
setpage(28,'Financial Data Integrity and Business Constraints',[
 tbl('Domain constraints and enforcement',['Area','Rule','Enforcement'],[
 ['Money','Positive transaction amounts with at most two decimal places','Validators checks input; MoneyUtil converts decimal rupees to integer paise.'],
 ['Expense date','Today or an earlier date','validateExpense rejects a future expense date.'],
 ['Category','A selected category must exist and match the transaction type','TransactionService validates category identity/type before persistence.'],
 ['Budgets','One positive limit per account, category and month','BudgetService validates; a unique database constraint supports upsert.'],
 ['Savings ratio','Active allocation percentages total at most 100','GoalService checks creation, active updates and reactivation.'],
 ['Allocation','Only active goals receive deposit shares','RatioAllocationService reads active goals and batches saved_paise updates.'],
 ['Recurring dates','End date cannot precede start date','Rule validation checks dates; scheduler advances due dates using frequency.']],[.17,.42,.41]),
 head('Persistence transaction boundaries'),para('Expense insertion runs with auto-commit disabled and commits after generated-key retrieval. On SQL failure it rolls back. Savings allocation also batches balance updates within one JDBC transaction. Fixed-category replacement deletes and inserts account preferences within a transaction, so an invalid category rolls back the replacement.'),
 head('Known system constraints'),para('Recurring occurrence insertion, audit logging and next-date advancement are separate operations. Snapshot restoration appends selected records and omits recurring rules and fixed-category preferences. Independent rounding of very small goal allocations can exceed the original deposit. These limits affect recovery and edge-case behavior and do not represent additional persisted states.')])
P[27]['section']='2.4 Data integrity requirements'
P[28]['title']='Requirements Traceability';P[28]['section']='2.4 Requirements traceability'
P[28]['items']=[x for x in P[28]['items'] if x['kind']=='table']
P[28]['items'] += [para('The matrix connects user requirements to principal use cases and implementation modules. Automated cases A01–A28 record executed assertions; cases T01–T20 cover screen interaction and additional acceptance scenarios. An automated result applies to its asserted service behavior, not to the entire associated graphical workflow.')]
for p in P:
 for x in p['items']:
  if x['kind']=='table':
   x['caption']=x['caption'].replace('Verified technology stack','Technology stack').replace('Tracking requirements retaining MST identifiers','Functional requirements for tracking').replace('Non-functional requirements and verification status','Non-functional requirements')
   x['headers']=[v.replace('Evidence or limitation','Implementation and constraints').replace('Verification','Related use cases and tests') for v in x['headers']]
   replacements={'category test exists but not run.':'A20.','BudgetServiceTest exists, not run.':'A23–A24.','ratio integration tests not run.':'A25–A26.','source inspection only.':'Pending manual execution.','BCrypt and prepared statements inspected; no penetration test or database encryption.':'BCrypt password hashing and prepared SQL statements; local storage is unencrypted.','Explicit JDBC boundaries inspected; recurring/restore are not fully atomic.':'Explicit JDBC commit/rollback; recurring execution and restore are not fully atomic.'}
   x['rows']=[[str(c) for c in row] for row in x['rows']]
   for row in x['rows']:
    for j,c in enumerate(row):
     for a,b in replacements.items():c=c.replace(a,b)
     row[j]=c
for x in P[28]['items']:
 if x['kind']=='table':
  for r in x['rows']:
   extra={'FR-02':'; A19,A21,A22','FR-03':'; A20','FR-04':'; A23,A24','FR-05':'; A25,A26','EXT-03':'; A27,A28'}.get(r[0],'');r[-1]+=extra
P[36]['items'] += [para('Recommendation defines evaluate(AnalysisContext) and getRuleName(). Concrete rules implement both methods. The collection List<Recommendation> allows the engine to invoke different financial checks through the same interface.')]
for p in P:
 p['items']=[x for x in p['items'] if not(x['kind']=='p' and ('These story cards specify' in x['text'] or 'This distinction between persisted' in x['text']))]
 for x in p['items']:
  if x['kind']=='p' and 'The diagram intentionally shows' in x['text']:x['text']='The JDBC lane includes the expense SQL executed by TransactionService.addExpense(). The service validates the selected category and the financial fields, then commits the insert before recording recent activity. Income creation follows the same domain validation pattern but delegates its save operation to JdbcTransactionDao.'
  if x['kind']=='p' and 'The participant named TransactionDialog represents' in x['text']:x['text']='TransactionDialogController invokes TransactionService.addExpense(). The service obtains the category through JdbcCategoryDao and calls Validators.validateExpense(). The JDBC participant groups connection, statement and database interactions. The success path returns an Expense with its generated identifier.'
  if x['kind']=='p' and 'invented web MVC architecture' in x['text']:x['text']='Controllers coordinate screens and events, services enforce financial rules, and DAOs map persisted records. DashboardController also performs direct DAO queries, while TransactionService.addExpense() and RatioAllocationService.allocateDeposit() contain SQL transactions. MainApp wires dependencies and file-operation callbacks; stop() shuts down the scheduler and logger.'
  if x['kind']=='table' and 'Final-scope extensions' in x['caption']:
   x['caption']='Additional functional requirements';x['headers']=['ID','Required behavior']
   x['rows'][0][1]='Dashboard displays monthly KPIs, category pie chart, a three-month income/expense chart, goals and recent activity. Navigation reloads saved records.'
   x['rows'][3][1]='Keep New/Edit Savings Goal controls and action buttons accessible through content sizing, screen-height limits and a scrollable form.'
for p in P[8:14]:
 items=[];current=None
 for x in p['items']:
  if x['kind']=='h' and x['text'].startswith('UC-'):
   if current:items.append(current)
   items.append(x);current=tbl(x['text'].split('  ')[0]+' specification',['Field','Description'],[],[.23,.77])
  elif current and x['kind']=='p' and ': ' in x['text']:
   field,value=x['text'].split(': ',1);current['rows'].append([field,value])
  else:
   if current:items.append(current);current=None
   items.append(x)
 if current:items.append(current)
 p['items']=items
P[40]['items'] += [head('Execution and local storage'),para('Maven compiles and launches the Java application with its configured dependencies. JavaFX native libraries must match the operating system. Configuration and CSS are packaged resources; the SQLite database, application logs and user-selected CSV, text and snapshot files remain local. Changing the working directory changes the location resolved by the relative database URL.')]
P[32]['items'] += [head('Model and service responsibilities'),para('SavingsGoal stores the target, saved amount, deadline, allocation percentage and active flag. Its progress method calculates a display value from savedAmount and targetAmount. GoalService.getTotalActiveRatio() iterates over active goals; createGoal(), updateGoal() and toggleGoalActive() use this total to protect the allocation pool. RatioAllocationService uses the same active-goal set when distributing an entered deposit.'),para('Budget stores a category, month and limit. BudgetService combines this plan with transaction totals to calculate utilization. There is no Java ownership collection linking a Budget to transactions: the service joins their meaning through account, category and month identifiers. The DAO interfaces isolate ordinary save and lookup operations, while allocation uses a direct JDBC transaction for its batch update.')]
for p in P:
 for x in p['items']:
  if x['kind']=='table' and 'Feasibility and deliverables' in x['caption']:
   x['rows'][-1][1]='Java desktop application, Maven build configuration, source code, automated tests and user-facing financial views.'
  if x['kind']=='p' and 'All database transactions' in x['text']:pass
  if x['kind']=='p' and 'A database transaction does not have a stored DRAFT' in x['text']:
   x['text']='Successful persistence closes the dialog and returns the saved record. Cancel closes it without a persistence call. Dialog workflow states are not stored in the transactions table. Editing an existing object can change its in-memory values before a failed update, while saved database values change only when persistence succeeds.'
# Eight replaceable, bordered screenshot areas without editorial instructions.
names=['Login and Registration','Dashboard','Transaction Management','Budget Management','Savings Goals','Recurring Transactions','Reports and Analytics','Settings and Financial Recommendations']
for j in range(4):
 p=P[41+j];p['items']=[]
 for k in range(2):p['items'].append({'kind':'screenshot','title':names[j*2+k],'caption':f'Figure 4.{j*2+k+3}: {names[j*2+k]} Interface — Screenshot to be inserted'})
# Observed test results from the real Jupiter engine, including lifecycle callbacks.
log=(ROOT/'evidence/junit_results.txt').read_text(encoding='utf-8-sig')
results=[line.split('|',3) for line in log.splitlines() if line.startswith('CASE|')]
assert len(results)==28 and all(r[1]=='SUCCESSFUL' for r in results)
oldmod={};exec(compile((OLD/'report_content.py').read_text(encoding='utf8'),str(OLD/'report_content.py'),'exec'),{'__file__':str(OLD/'report_content.py'),'__name__':'report_old'},oldmod) if False else None
# Preserve published A01–A18 identifiers.
import ast
tree=ast.parse((OLD/'report_content.py').read_text(encoding='utf8'))
for node in tree.body:
 if isinstance(node,ast.Assign) and any(isinstance(t,ast.Name) and t.id in ['AUTOS','MANUAL'] for t in node.targets):oldmod[node.targets[0].id]=ast.literal_eval(node.value)
auto=oldmod['AUTOS']+[
 ('A19','TransactionServiceTest','testAddIncomeAndExpensePolymorphism','Income 50000; expense 1200: generated IDs and signed amounts'),
 ('A20','TransactionServiceTest','testCustomCategoryCreationAndDuplicateRejection','Create category; reject case-insensitive duplicate'),
 ('A21','TransactionServiceTest','testFutureExpenseDateValidation','Expense INR 500 dated today + 2 days: InvalidTransactionException'),
 ('A22','TransactionServiceTest','testInvalidAmountValidation','Zero and -100 expense amounts: InvalidTransactionException'),
 ('A23','BudgetServiceTest','testSetAndRetrieveBudget','Save limit; reload category/month budget'),
 ('A24','BudgetServiceTest','testAlertThresholdLevels','50%, 85%, 105%: NORMAL, WARNING, EXCEEDED'),
 ('A25','RatioAllocationServiceTest','testRatioCapValidation','60% + 30% accepted; additional 20% rejected'),
 ('A26','RatioAllocationServiceTest','testAtomicDepositAllocation','10000 deposit; 50%/30%: balances 5000/3000, remainder 2000'),
 ('A27','JdbcUserFixedCategoryDaoTest','replaceAllRollsBackEntireUpdateWhenAnyCategoryIsInvalid','Invalid replacement category: original preferences retained'),
 ('A28','JdbcUserFixedCategoryDaoTest','fixedCategoryListsAreIsolatedPerUser','Separate account preference lists remain independent')]
fixture={
 'A01':('100.50, 0, 0.05','10050, 0, 5 paise'), 'A02':('1250.50','Formatted amount includes 1,250.50'),
 'A03':('Income and expense list','Absolute sum 6500; signed sum 3500'),
 'A04':('Valid names, demo; invalid classes','Valid accepted; invalid rejected'),
 'A05':('secret123; null; 12345','Eight-character password accepted; null and five characters rejected'),
 'A06':('Positive, 0, negative, >2 decimals','Only valid positive scale accepted'),
 'A07':('Today, past, tomorrow','Future expense rejected'),
 'A08':('Concentrated category spending','Concentration advice present'),
 'A09':('Fixed and flexible categories','Fixed excluded; flexible share used'),
 'A10':('All monthly spending fixed','No concentration advice'),
 'A11':('Food 500; Transport 500','Existing assertion expects Food advice'),
 'A12':('Expense/income = 95%','High expense warning'),
 'A13':('Category increase = 40%','Spike warning'),
 'A14':('Fixed-category increase','No fixed-category spike warning'),
 'A15':('Eligible unfixed category spike','Spike warning retained'),
 'A16':('Net savings rate = 4%','Low savings warning'),
 'A17':('Five expenses below 200','Micro-spending warning'),
 'A18':('Recommendations disabled','Empty recommendation list'),
 'A21':('INR 500; today + 2 days','InvalidTransactionException'),
 'A22':('Zero; -100 expense amounts','InvalidTransactionException'),
 'A23':('Rent; 2026-11; INR 25000','ID assigned; reloaded limit 25000')}
testrows=[]
for ident,cls,meth,exp in auto:
 assert any(r[2].endswith(cls+'#'+meth) for r in results),(ident,meth)
 inp,expected=fixture.get(ident,(exp.split(':')[0],exp))
 remark=cls+'.'+meth+'()'
 if ident=='A11':remark+='; ordering-sensitive assertion'
 if ident=='A26':remark+='; successful allocation only; rollback not injected'
 if ident=='A24':remark+='; exact 80/100 boundaries not exercised'
 testrows.append([ident,re.sub(r'(?<!^)(?=[A-Z])',' ',meth.removeprefix('test')).strip(),inp,expected,'Assertions completed','Pass',remark])
plan=P[45];plan['items']=[head('5.1 Test Plan'),para('Testing checks financial validation, monetary calculations, recommendation rules and selected database workflows. The execution on 16 November 2026 compiled all 85 production Java files and 8 test source files using Java 21. All 28 existing @Test methods passed under the JUnit Jupiter engine. Lifecycle methods were executed by the engine. Database tests used a new disposable SQLite file with isolated configuration.'),
 tbl('Test environment and strategy',['Item','Specification'],[
 ['Environment','Windows; Java 21; JavaFX 21.0.5; JUnit Jupiter 5.10.2; cached project dependencies.'],
 ['Isolation','Temporary compiled classes and app.properties; unique SQLite test file; application database unchanged.'],
 ['Unit scope','Money conversion/aggregation, username/password/amount/date validation, five recommendation rules and preference suppression.'],
 ['Integration scope','Transaction creation/validation, category duplicates, budget save/query and alert levels, goal ratio cap, deposit balances, fixed-category rollback and account isolation.'],
 ['Manual scope','Authentication screens, edit/delete, charts, navigation refresh, recurring scheduling, file exchange and display scaling.'],
 ['Entry criteria','Sources compile; test resources point to a disposable database; required libraries available.'],
 ['Exit criteria','Automated assertions pass; manual acceptance results recorded; critical defects resolved before operational acceptance.']],[.24,.76]),
 para('Boundary-value and equivalence-class testing cover positive and invalid monetary inputs, future expense dates, budget utilization classes and the active savings ratio cap. The budget test samples 50%, 85% and 105%; exact 80% and 100% boundaries remain manual acceptance cases. The allocation test verifies a successful update and reloaded balances, not an induced rollback failure.'),
 para('GUI, performance, security and display-scaling checks are pending manual verification. The complete existing automated suite passed, but this does not establish screen usability, CSV round-trip reliability or complete snapshot recovery. The equal-share concentration test has an order-sensitive assertion and remains a portability risk.')]
headers=['Test Case ID','Test Scenario','Input / Test Data','Expected Result','Actual Result','Status','Remarks']
newtests=[]
for part,(start,end) in enumerate([(0,10),(10,19),(19,28)]):
 newtests.append({'title':'5.2 Test Cases' if part==0 else 'Automated Test Cases continued','section':'5.2 Automated tests','landscape':True,'items':[para('Executed on 16 November 2026. Pass means the assertions in the named existing test method completed successfully.'),tbl('Automated tests '+str(part+1),headers,testrows[start:end],[.06,.14,.17,.20,.12,.07,.24])]})
manual=oldmod['MANUAL']
for part in range(2):
 rows=[]
 for ident,req,inp,expect,uc in manual[part*10:(part+1)*10]:
  rows.append([ident,uc+' / '+req,inp,expect,'Not recorded','Not Executed','Pending manual verification'])
 newtests.append({'title':'Manual Acceptance Test Cases','section':'5.2 Manual acceptance','landscape':True,'items':[para('These screen and additional acceptance scenarios have not been executed. Automated service checks do not replace the complete graphical workflow.'),tbl('Manual acceptance cases '+str(part+1),headers,rows,[.06,.12,.23,.28,.10,.09,.12])]})
peer=P[49];peer['items']=[para('Peer testing record'),tbl('Peer tester details',['Field','Entry'],[['Tester name / roll number',''],['Date / application revision',''],['Operating system / Java version',''],['Use cases / test case IDs','']],[.4,.6]),
 tbl('Peer observations',['Test ID / scenario','Observed result','Defect / severity','Retest status'],[['','','',''] for _ in range(5)],[.25,.3,.25,.2]),
 tbl('Peer acceptance',['Field','Entry'],[['Overall observations',''],['Acceptance recommendation',''],['Tester signature / date',''],['Evaluator acknowledgement','']],[.4,.6])]
P=P[:46]+newtests+[peer]
# One compact contents page replaces the two sparse pages in the previous file.
P.pop(2)
for p in P:
 for x in p['items']:
  if x['kind']=='table':
   x['caption']=re.sub(r'^Table \d+\. ','',x['caption'])
   x['rows']=[[re.sub('[\x00-\x08\x0b-\x1f]',' → ',str(c)) for c in row] for row in x['rows']]
  if x['kind']=='figure':x['caption']=re.sub(r'^Figure \d+\. ','',x['caption'])
(ROOT/'final_content.json').write_text(json.dumps(P,ensure_ascii=False,indent=2),encoding='utf8')
(ROOT/'evidence/test_mapping.json').write_text(json.dumps({'automated':auto,'results':results,'manual':manual},indent=2),encoding='utf8')

doc=Document();sec=doc.sections[0]
def config(s,land=False):
 s.orientation=WD_ORIENT.LANDSCAPE if land else WD_ORIENT.PORTRAIT
 s.page_width=Inches(11.6929 if land else 8.2677);s.page_height=Inches(8.2677 if land else 11.6929)
 s.top_margin=Inches(.72);s.bottom_margin=Inches(.68);s.left_margin=Inches(.85);s.right_margin=Inches(.75)
 s.header_distance=Inches(.28);s.footer_distance=Inches(.3)
config(sec);sec.different_first_page_header_footer=True
styles=doc.styles
for n in ['Normal','Body Text']:
 styles[n].font.name='Times New Roman';styles[n].font.size=Pt(12)
 styles[n].paragraph_format.line_spacing=1.15;styles[n].paragraph_format.space_after=Pt(7)
for n,size in [('Title',25),('Heading 1',18),('Heading 2',15),('Heading 3',12.5)]:
 styles[n].font.name='Times New Roman';styles[n].font.size=Pt(size);styles[n].font.color.rgb=RGBColor.from_string('000000');styles[n].font.bold=True
 styles[n].paragraph_format.space_before=Pt(8);styles[n].paragraph_format.space_after=Pt(10);styles[n].paragraph_format.keep_with_next=True
for style in styles:
 for e in list(style.element.iter(qn('w:pBdr'))):e.getparent().remove(e)
styles['Caption'].font.name='Times New Roman';styles['Caption'].font.size=Pt(10.5);styles['Caption'].font.italic=True;styles['Caption'].font.color.rgb=RGBColor.from_string('25394D')
styles['Caption'].paragraph_format.space_after=Pt(10)
for sn in ['Title','Heading 1','Heading 2','Heading 3','Caption','Normal','Body Text']:
 rf=styles[sn].element.get_or_add_rPr().find(qn('w:rFonts'))
 if rf is None:rf=OxmlElement('w:rFonts');styles[sn].element.get_or_add_rPr().append(rf)
 for key in list(rf.attrib):
  if 'Theme' in key:del rf.attrib[key]
 for key in ['ascii','hAnsi','eastAsia','cs']:rf.set(qn('w:'+key),'Times New Roman')
for sn,base in [('Page Heading','Heading 2'),('Subheading','Heading 3')]:
 styles.add_style(sn,1);styles[sn].base_style=styles[base]
 outline=OxmlElement('w:outlineLvl');outline.set(qn('w:val'),'9');styles[sn].element.get_or_add_pPr().append(outline)
footer=sec.footer.paragraphs[0];footer.alignment=WD_ALIGN_PARAGRAPH.CENTER
fld=OxmlElement('w:fldSimple');fld.set(qn('w:instr'),'PAGE');footer._p.append(fld)
header=sec.header.paragraphs[0];header.text='ENHANCED PERSONAL FINANCE ANALYZER';header.style='Caption';header.alignment=WD_ALIGN_PARAGRAPH.RIGHT
settings=doc.settings.element;up=OxmlElement('w:updateFields');up.set(qn('w:val'),'true');settings.append(up)
def h(t,level=2):
 numbered=re.match(r'^\d+\.',t)
 if numbered:level=min(3,len(re.match(r'^\d+(?:\.\d+)*',t).group(0).split('.')))
 p=doc.add_paragraph(t,style=f'Heading {level}' if numbered else ('Subheading' if level==3 else 'Page Heading'));return p
def body(t):
 p=doc.add_paragraph(t);p.alignment=WD_ALIGN_PARAGRAPH.JUSTIFY;return p
table_count={};fig_count={};allfig=[]
def caption(text):
 p=doc.add_paragraph(text,'Caption');p.alignment=WD_ALIGN_PARAGRAPH.CENTER;return p
def table(item,chapter,land=False):
 table_count[chapter]=table_count.get(chapter,0)+1
 cp=caption(f'Table {chapter}.{table_count[chapter]}: '+item['caption']);cp.paragraph_format.keep_with_next=True
 cols=len(item['headers']);t=doc.add_table(rows=1,cols=cols);t.autofit=False
 width=(11.6929 if land else 8.2677)-1.6
 weights=item.get('widths') or [1/cols]*cols;weights=[w/sum(weights) for w in weights]
 for i,col in enumerate(t.columns):col.width=Inches(width*weights[i])
 for rowidx,data in enumerate([item['headers']]+item['rows']):
  row=t.rows[0] if rowidx==0 else t.add_row()
  trPr=row._tr.get_or_add_trPr();cant=OxmlElement('w:cantSplit');trPr.append(cant)
  if rowidx==0:rep=OxmlElement('w:tblHeader');trPr.append(rep)
  for j,txt in enumerate(data):
   c=row.cells[j];c.width=Inches(width*weights[j]);c.text=str(txt)
   pr=c._tc.get_or_add_tcPr();shade=OxmlElement('w:shd');shade.set(qn('w:fill'),'DDE8F2' if rowidx==0 else ('F5F8FB' if rowidx%2==0 else 'FFFFFF'));pr.append(shade)
   for p in c.paragraphs:
    p.paragraph_format.space_after=Pt(2.5 if land else 4);p.paragraph_format.space_before=Pt(2.5 if land else 3);p.paragraph_format.line_spacing=1.04
    for r in p.runs:r.font.name='Times New Roman';r.font.size=Pt(10 if land else 10.5);r.bold=rowidx==0
  if rowidx>0:
   for p in row.cells[-1].paragraphs:p.paragraph_format.keep_with_next=False
 borders=OxmlElement('w:tblBorders')
 for tag in ['top','left','bottom','right','insideH','insideV']:
  e=OxmlElement('w:'+tag);e.set(qn('w:val'),'single');e.set(qn('w:sz'),'4');e.set(qn('w:color'),'9BAFC0');borders.append(e)
 t._tbl.tblPr.append(borders)
 ep=doc.add_paragraph();ep.paragraph_format.space_after=Pt(0);ep.paragraph_format.space_before=Pt(0);ep.paragraph_format.line_spacing=Pt(1);ep.add_run().font.size=Pt(1)
 return t
land=False;chapter=1
toc_entries=[]
for ix,p in enumerate(P):
 targetland=p.get('landscape',False)
 if ix>0:
  if targetland!=land:
   ns=doc.add_section(WD_SECTION_START.NEW_PAGE);config(ns,targetland);ns.different_first_page_header_footer=False;land=targetland
  else:doc.add_page_break()
 title=p['title']
 if p.get('cover'):
  doc.add_paragraph().paragraph_format.space_after=Pt(8)
  q=doc.add_paragraph('ENHANCED PERSONAL\nFINANCE ANALYZER','Title');q.alignment=WD_ALIGN_PARAGRAPH.CENTER;q.paragraph_format.space_after=Pt(28)
  for txt in ['UCS503 Software Engineering Project Report','End-Semester Evaluation','', 'Submitted by', 'Komal Kakkar — 1024030872','Vibhuti Mittal — 1024030873','Marushika Gupta — 1024030887','', 'BE Third Year, Computer Engineering  |  Group [Number]','', 'Submitted to','[Faculty Name]','', 'Computer Science and Engineering Department','Thapar Institute of Engineering & Technology','Patiala','', '[Submission Month and Year]']:
   if txt=='Computer Science and Engineering Department' and (ROOT/'assets/institution_logo.png').exists():
    logo=doc.add_paragraph();logo.alignment=WD_ALIGN_PARAGRAPH.CENTER;logo.add_run().add_picture(str(ROOT/'assets/institution_logo.png'),width=Inches(1.35));logo.paragraph_format.space_after=Pt(8)
   q=doc.add_paragraph(txt);q.alignment=WD_ALIGN_PARAGRAPH.CENTER;q.paragraph_format.space_after=Pt(9)
   if txt in ['Submitted by','Submitted to']:q.runs[0].bold=True
  continue
 if p.get('toc'):
  h('Table of Contents',1)
  # Real Word TOC field with a populated result, refreshed during rendering.
  q=doc.add_paragraph();begin=OxmlElement('w:fldChar');begin.set(qn('w:fldCharType'),'begin');q.add_run()._r.append(begin)
  ins=OxmlElement('w:instrText');ins.set(qn('xml:space'),'preserve');ins.text=' TOC \\o "1-3" \\h \\z \\u ';q.add_run()._r.append(ins)
  sep=OxmlElement('w:fldChar');sep.set(qn('w:fldCharType'),'separate');q.add_run()._r.append(sep)
  toc_pages=[3,3,4,6,6,6,8,14,17,17,18,19,20,29,31,31,33,35,37,39,39,40,41,45,45,46,51]
  if (ROOT/'evidence/toc_pages.json').exists():toc_pages=json.loads((ROOT/'evidence/toc_pages.json').read_text())
  for line,num in zip(['1. Project Selection Phase','  1.1 Software Bid','  1.2 Project Overview','2. Analysis Phase','  2.1 Use Cases','    2.1.1 Use-Case Diagrams','    2.1.2 Use Case Templates','  2.2 Activity Diagram and Swimlane Diagrams','  2.3 Data Flow Diagrams','    2.3.1 DFD Level 0','    2.3.2 DFD Level 1','    2.3.3 DFD Level 2','  2.4 Software Requirement Specification in IEEE Format','  2.5 User Stories and Story Cards','3. Design Phase','  3.1 Class Diagram','  3.2 Sequence Diagram','  3.3 Collaboration Diagram','  3.4 State Chart Diagrams','4. Implementation','  4.1 Component Diagrams','  4.2 Deployment Diagrams','  4.3 Screenshots','5. Testing','  5.1 Test Plan','  5.2 Test Cases','  5.3 Test Reports by Peers'],toc_pages):
   q.add_run(line+'\t'+str(num)+'\n')
  from docx.enum.text import WD_TAB_ALIGNMENT,WD_TAB_LEADER
  q.paragraph_format.tab_stops.add_tab_stop(Inches(6.55),WD_TAB_ALIGNMENT.RIGHT,WD_TAB_LEADER.DOTS)
  end=OxmlElement('w:fldChar');end.set(qn('w:fldCharType'),'end');q.add_run()._r.append(end)
  q.paragraph_format.line_spacing=1.1;q.paragraph_format.space_after=Pt(0)
  for r in q.runs:r.font.size=Pt(11)
  continue
 m=re.match(r'([1-5])\.',title)
 if m:chapter=int(m.group(1))
 level=1 if re.match(r'^[1-5]\. [A-Z ]+$',title) else 2
 hp=h(title.title() if level==1 else title,level)
 # Limit TOC to required numbered headings; continuation pages remain visible headings.
 if not re.match(r'^\d+\.',title):hp._p.get_or_add_pPr().append(OxmlElement('w:suppressAutoHyphens'))
 # Add required headings missing from the first use-case page.
 # The use-case page already contains the required numbered subheadings.
 for x in p['items']:
  kind=x['kind']
  if kind=='p':body(x['text'])
  elif kind=='h':h(x['text'],3)
  elif kind=='table':table(x,chapter,land)
  elif kind=='figure':
   img=ROOT/'diagrams/rendered'/(x['id']+'.png');w,hh=Image.open(img).size
   maxw=6.65;maxh=5.5 if x['id'] not in ['03_activity_expense','04_activity_savings'] else 5.8
   scale=min(maxw/w,maxh/hh);q=doc.add_paragraph();q.alignment=WD_ALIGN_PARAGRAPH.CENTER;q.paragraph_format.keep_with_next=True
   run=q.add_run();run.add_picture(str(img),width=Inches(w*scale),height=Inches(hh*scale))
   run._r.xpath('.//wp:docPr')[0].set('descr',x['caption'])
   fig_count[chapter]=fig_count.get(chapter,0)+1;cap=f'Figure {chapter}.{fig_count[chapter]}: '+x['caption'];caption(cap);allfig.append(cap)
  elif kind=='screenshot':
   t=doc.add_table(rows=1,cols=1);t.autofit=False;t.columns[0].width=Inches(6.65);row=t.rows[0];row.height=Inches(3.45)
   from docx.enum.table import WD_ROW_HEIGHT_RULE,WD_CELL_VERTICAL_ALIGNMENT
   row.height_rule=WD_ROW_HEIGHT_RULE.EXACTLY;c=row.cells[0];c.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER;c.text=x['title'];c.paragraphs[0].alignment=WD_ALIGN_PARAGRAPH.CENTER
   pr=c._tc.get_or_add_tcPr();border=OxmlElement('w:tcBorders')
   for tag in ['top','left','bottom','right']:
    e=OxmlElement('w:'+tag);e.set(qn('w:val'),'single');e.set(qn('w:sz'),'6');e.set(qn('w:color'),'A8B8C8');border.append(e)
   pr.append(border);caption(x['caption'])
doc.core_properties.title='Enhanced Personal Finance Analyzer UCS503 End-Semester Report'
doc.core_properties.author='Komal Kakkar; Vibhuti Mittal; Marushika Gupta'
doc.core_properties.subject='UCS503 Software Engineering Lab'
doc.sections[0].different_first_page_header_footer=True
doc.sections[0].first_page_footer.paragraphs[0].clear()
doc.sections[0].first_page_header.paragraphs[0].clear()
out=ROOT/'Enhanced_Personal_Finance_Analyzer_UCS503_Final.docx';doc.save(out)
print('Saved',out,'blocks',len(P),'tables',len(doc.tables),'diagrams',len(doc.inline_shapes))

"""Rebuild editable Graphviz/UML sources; no application imports or database access."""
from pathlib import Path
import json,html,re
ROOT=Path(__file__).resolve().parent; SRC=ROOT/'diagrams/source'; OUT=ROOT/'diagrams/rendered'
SRC.mkdir(parents=True,exist_ok=True);OUT.mkdir(parents=True,exist_ok=True)
manifest=[]
def dot(name,title,body,kind,rank='TB'):
 if kind=='Class' and rank=='LR':
  # Preserve the usual vertical UML compartments under a left-to-right graph layout.
  body=re.sub(r'label="(\{[^"\n]*\})"',lambda m:'label="{'+m.group(1)+'}"',body)
 text='digraph G {\ngraph [rankdir='+rank+', bgcolor="white", pad="0.25", nodesep="0.35", ranksep="0.55", fontname="Arial", fontsize=16];\nnode [shape=box, fontname="Arial", fontsize=14, color="#344655", style="filled", fillcolor="#f5f7fa", margin="0.15,0.10"];\nedge [fontname="Arial", fontsize=11, color="#344655", arrowsize=0.8];\n'+body+'\n}\n'
 text=text.replace(r'\u00ab','«').replace(r'\u00bb','»')
 (SRC/(name+'.dot')).write_text(text,encoding='utf8');manifest.append(dict(id=name,title=title,type=kind))
dot('01_usecase_track','Tracking and account use cases',r'''
 user [label="«actor»\nAccount holder",shape=box];
 subgraph cluster_app {label="Enhanced Personal Finance Analyzer";color="#344655";
 node [shape=ellipse];auth [label="Register / sign in"];add [label="Record income / expense"];edit [label="Edit transaction"];del [label="Delete transaction"];search [label="Search / filter history"];cat [label="Create custom category"];valid [label="Validate transaction"];
 add->valid [label="\u00abinclude\u00bb",style=dashed]; edit->valid [label="\u00abinclude\u00bb",style=dashed];}
 user->auth [dir=none];user->add [dir=none];user->edit [dir=none];user->del [dir=none];user->search [dir=none];user->cat [dir=none];
''','Use case','LR')
dot('02_usecase_plan','Planning and analysis use cases',r'''
 user [label="«actor»\nAccount holder",shape=box];
 subgraph cluster_app {label="Enhanced Personal Finance Analyzer";color="#344655";node [shape=ellipse];
 budget [label="Manage monthly budgets"];goal [label="Create / activate goal"];alloc [label="Allocate deposit"];ratio [label="Check active ratio cap"];rec [label="Manage recurring rules"];report [label="View charts / monthly report"];files [label="Import / export / snapshot"];prefs [label="Set advice / fixed categories"];
 goal->ratio [label="\u00abinclude\u00bb",style=dashed];}
 user->budget [dir=none];user->goal [dir=none];user->alloc [dir=none];user->rec [dir=none];user->report [dir=none];user->files [dir=none];user->prefs [dir=none];
''','Use case','LR')
dot('03_activity_expense','Record an expense',r'''
 start [shape=circle,label="",width=.18,fillcolor="#344655"];open [label="Open New Transaction"];input [label="Enter expense details"];preview [label="Calculate current-month preview\n(no write)"];check [shape=diamond,label="Valid input?"];error [label="Display validation error"];save [label="addExpense(): insert and commit"];dbok [shape=diamond,label="SQL succeeds?"];rollback [label="Rollback and display error"];done [label="Refresh transaction table"];end [shape=doublecircle,label="",width=.2];
 start->open->input->preview->check;check->error [label="no"];error->input;check->save [label="yes"];save->dbok;dbok->rollback [label="no"];rollback->input;dbok->done [label="yes"];done->end;
''','Activity')
dot('04_activity_savings','Create and fund a savings goal',r'''
 start [shape=circle,label="",width=.18,fillcolor="#344655"];input [label="Enter title, target, deadline, ratio"];valid [shape=diamond,label="Valid fields and\nactive ratio <=100%?"];reject [label="Show validation / ratio error"];save [label="goalDao.save(goal)"];deposit [label="Enter positive deposit"];shares [label="Read active goals\nCalculate rounded ratio shares"];commit [label="Batch saved_paise updates\nCommit or rollback together"];done [label="Display allocation result\nRefresh goals table"];end [shape=doublecircle,label="",width=.2];
 start->input->valid;valid->reject [label="no"];reject->input;valid->save [label="yes"];save->deposit->shares->commit->done->end;
''','Activity')
dot('05_swimlane_expense','Expense responsibilities across layers',r'''
 subgraph cluster_user {label="User"; u1 [label="Enter details"];u2 [label="Select Save"];u3 [label="Read result"]}
 subgraph cluster_ui {label="JavaFX controller";c1 [label="handleSave()\nParse and validate fields"];c2 [label="Close dialog on success\nrefreshTable()"]}
 subgraph cluster_service {label="Business logic";s1 [label="addExpense()\nValidate category / amount / date"];s2 [label="recordActivity()"]}
 subgraph cluster_db {label="JDBC persistence";d1 [label="findById(categoryId)"];d2 [label="INSERT transactions\ncommit / rollback"]}
 u1->u2->c1->s1->d1->d2->s2->c2->u3;
''','Swimlane')
dot('06_dfd_context','DFD Level 0',r'''
 u [shape=box,label="Account holder"];f [shape=box,label="User-selected files"];p [shape=circle,label="0\nFinance\nAnalyzer",width=2.0];
 u->p [label="Credentials; entry/category details;\nbudget/goal/rule settings;\nqueries/preferences"];
 p->u [label="Authentication/operation results;\nrecords / previews;\ncharts / advice / reports"];
 f->p [label="CSV rows;\nsnapshot data"];p->f [label="CSV rows; report text;\nsnapshot data"];
''','DFD')
dot('07_dfd_level1','DFD Level 1',r'''
 graph [nodesep="0.25",ranksep="0.4"];
 node [shape=box];u1 [label="Account holder\n(repeated)"];u2 [label="Account holder\n(repeated)"];u3 [label="Account holder\n(repeated)"];u4 [label="Account holder\n(repeated)"];u5 [label="Account holder\n(repeated)"];f [label="User-selected files"];
 node [shape=circle,width=1.3];p1 [label="1.0\nAuthenticate"];p2 [label="2.0\nTrack records"];p3 [label="3.0\nPlan finances"];p4 [label="4.0\nAnalyze / report"];p5 [label="5.0\nTransfer files"];
 node [shape=plain];d1 [label=<<TABLE BORDER="0" CELLBORDER="0"><TR><TD BORDER="1" SIDES="TB">D1 Accounts / preferences</TD></TR></TABLE>>];d2 [label=<<TABLE BORDER="0" CELLBORDER="0"><TR><TD BORDER="1" SIDES="TB">D2 Tracking records</TD></TR></TABLE>>];d3 [label=<<TABLE BORDER="0" CELLBORDER="0"><TR><TD BORDER="1" SIDES="TB">D3 Budgets / goals</TD></TR></TABLE>>];d4 [label=<<TABLE BORDER="0" CELLBORDER="0"><TR><TD BORDER="1" SIDES="TB">D1, D2, D3<BR/>(repeated stores)</TD></TR></TABLE>>];d5 [label=<<TABLE BORDER="0" CELLBORDER="0"><TR><TD BORDER="1" SIDES="TB">D2, D3<BR/>(repeated stores)</TD></TR></TABLE>>];
 u1->p1 [label="Credentials"];p1->u1 [label="Authentication result",constraint=false];p1->d1 [label="Account data"];d1->p1 [label="Stored account",constraint=false];
 u2->p2 [label="Entry / category / rule\ndetails; queries"];p2->u2 [label="Results; records",constraint=false];p2->d2 [label="Record changes"];d2->p2 [label="Stored records",constraint=false];
 u3->p3 [label="Budget / goal settings"];p3->u3 [label="Results; records",constraint=false];p3->d3 [label="Plan changes"];d3->p3 [label="Stored plans",constraint=false];d2->p3 [label="Spending totals",constraint=false];
 u4->p4 [label="Queries / preferences"];p4->u4 [label="Previews; charts;\nadvice; reports",constraint=false];p4->d4 [label="Preference changes"];d4->p4 [label="Preferences; records; plans",constraint=false];
 u5->p5 [label="File operation query"];p5->u5 [label="Operation results",constraint=false];f->p5 [label="CSV / snapshot data"];p5->f [label="CSV / TXT / snapshot",constraint=false];p5->d5 [label="Imported / restored data"];d5->p5 [label="Export / snapshot data",constraint=false];p4->p5 [label="Report text",constraint=false];
 {rank=same;u1;u2;u3;u4;u5;f;} {rank=same;p1;p2;p3;p4;p5;} {rank=same;d1;d2;d3;d4;d5;}
''','DFD','LR')
dot('08_dfd_level2','DFD Level 2 for process 2.0',r'''
 node [shape=box];u [label="Account holder"];
 node [shape=circle,width=1.2];a [label="2.1\nValidate entry"];b [label="2.2\nPersist entry"];c [label="2.3\nFind records"];d [label="2.4\nManage categories\nand recurring rules"];
 store [shape=plain,label=<<TABLE BORDER="0" CELLBORDER="0"><TR><TD BORDER="1" SIDES="TB">D2 Categories / transactions / rules</TD></TR></TABLE>>];
 u->a [label="Transaction details"];a->b [label="Validated transaction"];a->u [label="Validation result"];store->a [label="Stored category"];b->store [label="Record changes"];b->u [label="Operation result"];
 u->c [label="Queries"];store->c [label="Stored records"];c->u [label="Records"];
 u->d [label="Category details; rule settings"];store->d [label="Stored records"];d->store [label="Record changes"];d->u [label="Operation results; records"];
''','DFD')
dot('09_class_transactions','Transaction management class diagram',r'''
 node [shape=record,fontsize=12];
 t [label="{Transaction\n«abstract»|# id: Long\l# userId: Long\l# categoryId: Long\l# amount: BigDecimal\l# date: LocalDate\l|+ getType(): TransactionType\l+ getSignedAmount(): BigDecimal\l}"];
 i [label="{Income||+ getType()\l+ getSignedAmount()\l}"];e [label="{Expense||+ getType()\l+ getSignedAmount()\l}"];
 s [label="{TransactionService|- transactionDao: TransactionDao\l- categoryDao: CategoryDao\l|+ addIncome(...)\l+ addExpense(...)\l+ updateTransaction(t)\l+ findFiltered(...)\l}"];
 d [label="{«interface» TransactionDao||+ save(t): Transaction (inherited)\l+ findFiltered(...): List\l+ getTotalExpenseInMonth(...)\l}"];
 j [label="{JdbcTransactionDao||+ save(t)\l+ update(t)\l- mapRowToTransaction(rs)\l}"];
 c [label="{Category|- id: Long\l- userId: Long\l- name: String\l- type: TransactionType\l|+ getName()\l}"];
 i->t [arrowhead=empty];e->t [arrowhead=empty];j->d [arrowhead=empty,style=dashed];s->d [label="uses",style=dashed];s->t [label="creates / validates",style=dashed];t->c [label="categoryId reference",headlabel="1",taillabel="0..*",dir=none];j->t [label="maps",style=dashed];
''','Class')
dot('10_class_planning','Budget and savings class diagram',r'''
 node [shape=record,fontsize=12];
 b [label="{Budget|- userId: Long\l- categoryId: Long\l- yearMonth: YearMonth\l- limit: BigDecimal\l|+ getLimit()\l}"];
 bs [label="{BudgetService||+ setBudget(...)\l+ calculateUtilisation(...)\l+ checkBudgetAlert(...)\l}"];
 bd [label="{«interface» BudgetDao||+ save(b) (inherited)\l+ findByCategoryAndMonth(...)\l}"];
 g [label="{SavingsGoal|- userId: Long\l- targetAmount: BigDecimal\l- savedAmount: BigDecimal\l- allocationRatio: int\l- active: boolean\l|+ getProgressPercentage(): double\l}"];
 gs [label="{GoalService||+ createGoal(...)\l+ updateGoal(g)\l+ toggleGoalActive(...)\l}"];
 gd [label="{«interface» SavingsGoalDao||+ save(g) (inherited)\l+ findActiveGoals(userId)\l+ updateActiveStatus(...)\l}"];
 ra [label="{RatioAllocationService||+ allocateDeposit(...)\l}"];
 bs->bd [style=dashed,label="uses"];bs->b [style=dashed,label="creates"];gs->gd [style=dashed,label="uses"];gs->g [style=dashed,label="creates"];ra->gd [style=dashed,label="reads active goals"];ra->g [style=dashed,label="calculates shares"];
''','Class','LR')
def seq(name,title,participants,messages,note):
 W=1160;H=130+len(messages)*70+80;xs=[110+i*(W-220)/(len(participants)-1) for i in range(len(participants))];parts=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}"><defs><marker id="a" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto"><path d="M0,0 L10,4 L0,8" fill="none" stroke="#253746"/></marker></defs><rect width="100%" height="100%" fill="white"/>']
 for i,p in enumerate(participants):
  x=xs[i];parts.append(f'<rect x="{x-102}" y="15" width="204" height="52" fill="#f2f5f8" stroke="#253746"/><text x="{x}" y="45" font-family="Arial" font-size="20" text-anchor="middle">{html.escape(p)}</text><line x1="{x}" y1="67" x2="{x}" y2="{H-60}" stroke="#647482" stroke-dasharray="7 5"/>')
 for k,(a,b,label,ret) in enumerate(messages):
  y=110+k*70;x1,x2=xs[a],xs[b];dash=' stroke-dasharray="7 5"' if ret else ''
  if a==b:
   parts.append(f'<path d="M{x1},{y} h45 v28 h-45" fill="none" stroke="#253746" marker-end="url(#a)"/><text x="{x1+50}" y="{y+15}" font-family="Arial" font-size="16">{html.escape(label)}</text>')
  else:
   parts.append(f'<line x1="{x1}" y1="{y}" x2="{x2}" y2="{y}" stroke="#253746" marker-end="url(#a)"{dash}/><text x="{(x1+x2)/2}" y="{y-10}" font-family="Arial" font-size="16" text-anchor="middle">{html.escape(label)}</text>')
 parts.append(f'<text x="25" y="{H-15}" font-family="Arial" font-size="15">{html.escape(note)}</text></svg>')
 svg=''.join(parts).replace('font-size="16"','font-size="20"')
 svg=svg.replace('addExpense(userId, categoryId, amount, date, ...)','addExpense(...)').replace('createGoal(userId, title, target, deadline, ratio)','createGoal(...)')
 (OUT/(name+'.svg')).write_text(svg,encoding='utf8')
 puml=['@startuml','hide footbox']+[f'participant "{p}" as p{i}' for i,p in enumerate(participants)]
 for a,b,label,ret in messages:puml.append(f'p{a} '+('-->' if ret else '->')+f' p{b}: {label}')
 puml+=['note over p0: '+note,'@enduml'];(SRC/(name+'.puml')).write_text('\n'.join(puml),encoding='utf8')
 manifest.append(dict(id=name,title=title,type='Sequence'))
seq('11_sequence_expense','Adding an expense transaction',['TransactionDialog','TransactionService','JdbcCategoryDao','JDBC / SQLite'],[(0,1,'addExpense(userId, categoryId, amount, date, ...)',False),(1,2,'findById(categoryId)',False),(2,1,'Optional<Category>',True),(1,1,'validateExpense(...)',False),(1,3,'getConnection(); setAutoCommit(false)',False),(1,3,'INSERT transactions; generated key',False),(1,3,'commit()',False),(1,0,'Expense with generated id',True)],'TransactionService inserts expenses directly; the failure branch rolls back.')
seq('12_sequence_goal','Creating a savings goal',['GoalsController','GoalService','JdbcSavingsGoalDao','JDBC / SQLite'],[(0,1,'createGoal(userId, title, target, deadline, ratio)',False),(1,1,'validateSavingsGoal(...)',False),(1,2,'findActiveGoals(userId)',False),(2,1,'List<SavingsGoal>',True),(1,1,'check total active ratio <= 100%',False),(1,2,'save(new SavingsGoal(...))',False),(2,3,'INSERT savings_goals; generated key',False),(3,2,'generated id',True),(2,1,'saved goal',True),(1,0,'SavingsGoal',True)],'Invalid input or excessive ratios throw before the INSERT.')
dot('13_communication_transaction','Transaction persistence communication',r'''
 a [label="dialog:TransactionDialogController"];b [label="service:TransactionService"];c [label="categoryDao:JdbcCategoryDao"];d [label="connection:JDBC Connection"];e [label="SQLite transactions",shape=cylinder];
 a->b [label="1: addExpense(...)"];b->c [label="1.1: findById(categoryId)"];b->b [label="1.2: validateExpense(...)"];b->d [label="1.3: setAutoCommit(false)\n1.4: prepareStatement(...)\n1.6: commit() / rollback()"];
 d->e [label="1.5: executeUpdate()\nINSERT + generated key"];b->a [label="1.7: Expense",style=dashed];
''','Communication')
dot('14_communication_advice','Recommendation generation communication',r'''
 a [label="dashboard:DashboardController"];b [label="engine:HeuristicEngine"];c [label="categories:CategoryService"];d [label="ctx:AnalysisContext"];e [label="rule:Recommendation\n(one of five implementations)"];
 a->b [label="1: generateRecommendations(user, ctx)"];b->c [label="1.1: findExpenseCategories(userId)\n1.2: findFixedCategoryIds(userId)"];b->d [label="1.3: withFixedCategoryData(...)"];b->e [label="1.4*: evaluate(ctx)"];e->b [label="Optional<String>",style=dashed];b->a [label="1.5: List<String>",style=dashed];
''','Communication')
dot('15_state_transaction','Transaction dialog workflow states',r'''
 start [shape=circle,label="",width=.18,fillcolor="#344655"];n [label="New form"];edit [label="Edit form\nType locked"];v [label="Validating"];err [label="Validation / save error"];saved [label="Saved and closed"];cancel [label="Cancelled and closed"];end [shape=doublecircle,label="",width=.2];
 start->n [label="new"];start->edit [label="existing record"];n->v [label="Save"];edit->v [label="Save"];v->err [label="invalid / SQL failure"];err->v [label="correct and retry"];v->saved [label="success"];n->cancel [label="Cancel"];edit->cancel [label="Cancel"];err->cancel [label="Cancel"];saved->end;cancel->end;
''','State chart')
dot('16_state_goal','Savings goal persisted active flag',r'''
 start [shape=circle,label="",width=.18,fillcolor="#344655"];active [label="Active\nis_active = 1"];inactive [label="Inactive\nis_active = 0"];deleted [shape=doublecircle,label="Deleted\n(row absent)"];
 start->active [label="createGoal() [valid; total <=100%]"];active->inactive [label="toggleGoalActive() / deactivate"];inactive->active [label="toggleGoalActive() [ratio cap allows]"];active->active [label="allocateDeposit() / add saved_paise\nupdateGoal() [valid]"];inactive->inactive [label="updateGoal() [valid]"];active->deleted [label="deleteGoal()"];inactive->deleted [label="deleteGoal()"];
''','State chart')
dot('17_component','Application component structure',r'''
 node [shape=component];app [label="MainApp / Launcher"];ui [label="JavaFX controller-built views\nCSS / SpendPreviewCard"];services [label="Auth / Transaction / Budget / Goal\nRatio / Analytics / Report services"];rules [label="HeuristicEngine + Recommendation rules"];sched [label="RecurringScheduler"];dao [label="DAO interfaces + JDBC implementations"];files [label="CSV / snapshot I/O"];db [label="SQLite finance.db",shape=cylinder];
 app->ui [label="constructs"];app->services [label="injects"];app->sched [label="starts / stops"];ui->services;ui->dao [label="some direct queries"];services->rules;services->dao;services->db [label="expense / allocation SQL"];sched->dao;ui->files [label="MainApp callbacks"];files->services;files->dao;dao->db;
''','Component')
dot('18_deployment','Local desktop deployment',r'''
 subgraph cluster_device {label="«device» Windows desktop / laptop";color="#344655";
 subgraph cluster_jvm {label="«executionEnvironment» Java 21 JVM";
 app [label="«artifact» Application classes / JAR\nJavaFX 21.0.5 controls and CSS"];libs [label="«artifact» SQLite JDBC 3.46.1.0\njBCrypt 0.4"];app->libs [label="classpath dependencies"];}
 db [shape=cylinder,label="«artifact» data/finance.db"];cfg [label="«artifact» app.properties"];log [label="«artifact» logs/app.log.*"];files [label="«artifact» User-selected CSV / TXT / DAT"];
 app->db [label="local JDBC file access"];app->cfg [label="resource load"];app->log [label="logging"];app->files [label="file streams"];
 }
 build [label="Maven build tool\ncompile / test / javafx:run"];build->app [label="launches locally"];
''','Deployment','LR')
(ROOT/'diagrams/manifest.json').write_text(json.dumps(manifest,indent=2),encoding='utf8')
print('Created',len(manifest),'diagram specifications')

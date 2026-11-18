"""Editable, role-coloured diagrams; reads earlier specifications without overwriting them."""
from pathlib import Path
import json,re,html
ROOT=Path(__file__).resolve().parent
SRC=ROOT/'diagrams/source'; OUT=ROOT/'diagrams/rendered'
SRC.mkdir(parents=True,exist_ok=True);OUT.mkdir(parents=True,exist_ok=True)
PAL={'ui':('#E5F0FC','#245D91'),'service':('#E7F3E8','#387445'),'model':('#EFE7F7','#755095'),'db':('#FFF0DE','#A36A26'),'actor':('#DDF3F0','#247B75')}
manifest=json.loads((ROOT.parent/'diagrams/manifest.json').read_text())
for m in manifest:
 name=m['id']; old=ROOT.parent/'diagrams/source'/(name+'.dot')
 if not old.exists():continue
 s=old.read_text(encoding='utf8').replace('#344655','#33465A').replace('#f5f7fa','#E7F3E8')
 s=s.replace('pad="0.25"','pad="0.3", penwidth=1.4').replace('arrowsize=0.8','arrowsize=0.8, penwidth=1.4').replace('margin="0.15,0.10"','margin="0.18,0.12", penwidth=1.4')
 s=s.replace('Â«actorÂ»\\nAccount holder','Account holder')
 # HTML stick figures remain ordinary editable Graphviz node labels.
 if m['type']=='Use case':
  actor='user [shape=plain, label=<<TABLE BORDER="0" CELLBORDER="0"><TR><TD><FONT COLOR="#247B75" POINT-SIZE="28">&#9675;</FONT></TD></TR><TR><TD><FONT COLOR="#247B75" POINT-SIZE="25">&#9585;&#9474;&#9586;</FONT></TD></TR><TR><TD><FONT COLOR="#247B75" POINT-SIZE="25">&#9585; &#9586;</FONT></TD></TR><TR><TD>Account holder</TD></TR></TABLE>>];'
  s=re.sub(r'user \[label="[^"]*",shape=box\];',lambda _:actor,s)
  s=s.replace('node [shape=ellipse]','node [shape=ellipse,fillcolor="#E5F0FC",color="#245D91"]')
  s=s.replace('color="#33465A";\n node','color="#AFC7DF"; style="rounded,filled";fillcolor="#F7FAFE";\n node')
 # Explicit semantic colours override defaults, without altering topology.
 if m['type']=='DFD':
  s=s.replace('shape=circle','fillcolor="#E7F3E8",color="#387445",shape=circle')
  s=s.replace('shape=plain','fillcolor="#FFF0DE",color="#A36A26",shape=plain')
  s=s.replace('BORDER="1" SIDES="TB"','BORDER="1" SIDES="TB" BGCOLOR="#FFF0DE" CELLPADDING="8"')
  s=s.replace('node [shape=box]','node [shape=box,fillcolor="#DDF3F0",color="#247B75"]')
  s=s.replace('D2, D3<BR/>(repeated stores)','D1, D2, D3<BR/>(repeated stores)')
  for n in ['u','f']:s=s[:-2]+f'\n{n} [fillcolor="#DDF3F0",color="#247B75"];\n}}\n' if f'{n} [' in s else s
 if m['type']=='Class':
  for n in (['t','i','e','c'] if name.startswith('09') else ['b','g']):s=s[:-2]+f'\n{n} [fillcolor="#EFE7F7",color="#755095"];\n}}\n'
  for n in (['d','j'] if name.startswith('09') else ['bd','gd']):s=s[:-2]+f'\n{n} [fillcolor="#FFF0DE",color="#A36A26"];\n}}\n'
  if name.startswith('10'):
   s=s.replace('rankdir=LR','rankdir=TB')
   s=re.sub(r'label="\{\{(.*?)\}\}"',r'label="{\1}"',s)
 if m['type']=='Swimlane':
  for lane,role in [('user','actor'),('ui','ui'),('service','service'),('db','db')]:
   fill,stroke=PAL[role];s=s.replace(f'subgraph cluster_{lane} {{',f'subgraph cluster_{lane} {{ style="rounded,filled";fillcolor="{fill}";color="{stroke}";')
 if m['type'] in ['Activity','State chart']:
  s=s.replace('style="filled"','style="rounded,filled"')
  s=s.replace('shape=diamond','fillcolor="#FFF0DE",color="#A36A26",shape=diamond')
  s=s.replace('fillcolor="#33465A"','fillcolor="#247B75"')
  s=s.replace('shape=doublecircle,label=""','fillcolor="#247B75",shape=doublecircle,label=""')
  if name.startswith('04'):
   s=s.replace('Batch saved_paise updates\\nCommit or rollback together','Batch saved_paise updates\\nCommit transaction')
   s=s.replace('save->deposit->shares->commit->done->end;','save->deposit->shares->commit;commit->sqlok;sqlok->done [label="yes"];sqlok->failed [label="no"];failed->deposit;done->end;sqlok [shape=diamond,label="SQL succeeds?",fillcolor="#FFF0DE",color="#A36A26"];failed [label="Rollback and display error"];')
  if name.startswith('16'):
   s=s.replace('deleted [shape=doublecircle','deleted [shape=box')
   s=s[:-2]+'\nend [shape=doublecircle,label="",width=.2,fillcolor="#247B75"];deleted->end;\n}\n'
 if m['type'] in ['Communication','Component','Deployment']:
  roles={'a':'ui','b':'service','c':'service','d':'model','e':'service'} if name.startswith('14') else {'a':'ui','b':'service','c':'db','d':'db','e':'db'}
  if m['type']=='Component':roles={'app':'actor','ui':'ui','services':'service','rules':'service','sched':'service','dao':'db','files':'actor','db':'db'}
  if m['type']=='Deployment':roles={'app':'ui','libs':'service','db':'db','cfg':'model','log':'actor','files':'actor','build':'actor'}
  for n,role in roles.items():
   fill,stroke=PAL[role];s=s[:-2]+f'\n{n} [fillcolor="{fill}",color="{stroke}"];\n}}\n'
 if name not in ['05_swimlane_expense','18_deployment']:(SRC/(name+'.dot')).write_text(s,encoding='utf8')
 elif (SRC/(name+'.dot')).exists():(SRC/(name+'.dot')).unlink()

def svgdoc(w,h):return [f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}"><defs><marker id="ar" markerWidth="9" markerHeight="8" refX="8" refY="4" orient="auto"><path d="M0 0 L8 4 L0 8" fill="#33465A"/></marker></defs><rect width="100%" height="100%" fill="white"/>']
def box(a,x,y,w,h,labels,role,fs=19):
 fill,stroke=PAL[role];a.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="{fill}" stroke="{stroke}" stroke-width="2"/>')
 for j,line in enumerate(labels):a.append(f'<text x="{x+w/2}" y="{y+h/2+(j-(len(labels)-1)/2)*25+7}" font-family="Arial" font-size="{fs}" text-anchor="middle" fill="#203346">{html.escape(line)}</text>')
def arrow(a,x1,y1,x2,y2,label=''):
 a.append(f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="#33465A" stroke-width="2" marker-end="url(#ar)"/>')
 if label:a.append(f'<text x="{(x1+x2)/2}" y="{(y1+y2)/2-12}" font-family="Arial" font-size="18" text-anchor="middle">{html.escape(label)}</text>')
def savesvg(name,a):
 s=''.join(a+['</svg>']);(SRC/(name+'.svg')).write_text(s,encoding='utf8');(OUT/(name+'.svg')).write_text(s,encoding='utf8')
a=svgdoc(1320,1080)
for i,(label,role) in enumerate([('Account holder','actor'),('JavaFX controller','ui'),('Service / validation','service'),('JDBC persistence','db')]):
 fill,stroke=PAL[role];a.append(f'<rect x="{10+i*325}" y="10" width="325" height="1050" fill="{fill}" fill-opacity=".55" stroke="{stroke}"/>');box(a,10+i*325,10,325,58,[label],role)
steps=[(0,['Enter expense details','Select Save'],'actor'),(1,['handleSave()','Parse form fields'],'ui'),(2,['addExpense(...)'],'service'),(3,['findById(categoryId)','Read Category'],'db'),(2,['Validators.validateExpense(...)'],'service'),(3,['INSERT transactions','commit / rollback'],'db'),(2,['recordActivity(...)'],'service'),(1,['Close on success','refreshTable()'],'ui'),(0,['Read updated records'],'actor')]
prev=None
for j,(lane,labels,role) in enumerate(steps):
 x=25+lane*325;y=100+j*101
 if prev:arrow(a,prev[0],prev[1],x+147,y)
 box(a,x,y,295,68,labels,role,18);prev=(x+147,y+68)
savesvg('05_swimlane_expense',a)
a=svgdoc(1400,850)
a.append('<rect x="260" y="20" width="1120" height="805" fill="#F8FBFE" stroke="#33465A" stroke-width="2"/><text x="820" y="55" text-anchor="middle" font-family="Arial" font-size="24">«device» Windows desktop or laptop</text>')
a.append('<rect x="295" y="90" width="1050" height="330" fill="white" stroke="#245D91" stroke-width="2"/><text x="820" y="125" text-anchor="middle" font-family="Arial" font-size="23">«executionEnvironment» Java 21 JVM</text>')
box(a,340,170,440,120,['«artifact» Application classes / JAR','JavaFX 21.0.5 controls and CSS'],'ui',21)
box(a,900,170,410,120,['«artifact» Libraries','SQLite JDBC 3.46.1.0 / jBCrypt 0.4'],'service',20)
arrow(a,780,225,900,225)
box(a,15,182,220,100,['Maven build tool','compile / javafx:run'],'actor',19);arrow(a,235,230,340,230)
for x,y,w,labels,role in [(340,510,440,['«artifact» data/finance.db','Local SQLite file'],'db'),(900,510,410,['«artifact» app.properties','Packaged configuration'],'model'),(340,700,440,['«artifact» logs/app.log.*','Local application logging'],'actor'),(900,700,410,['«artifact» CSV / TXT / DAT','User-selected files'],'actor')]:
 box(a,x,y,w,90,labels,role,20)
arrow(a,460,290,460,510,'JDBC')
arrow(a,680,290,1105,510,'load')
arrow(a,340,270,315,745)
arrow(a,315,745,340,745)
a.append('<path d="M780 280 H820 V460 H1360 V745 H1310" fill="none" stroke="#33465A" stroke-width="2" marker-end="url(#ar)"/>')
savesvg('18_deployment',a)

def seq(name,participants,messages,note):
 W=1400;H=160+len(messages)*100;xs=[145+i*370 for i in range(4)]
 a=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}"><defs><marker id="arr" markerWidth="9" markerHeight="8" refX="8" refY="4" orient="auto"><path d="M0 0 L8 4 L0 8" fill="none" stroke="#33465A" stroke-width="1.5"/></marker></defs><rect width="100%" height="100%" fill="white"/>']
 puml=['@startuml','skinparam backgroundColor white','skinparam shadowing false','skinparam sequence {','ArrowColor #33465A','LifeLineBorderColor #8A9BAB','ParticipantFontColor #203346','}','hide footbox']
 for i,(label,role) in enumerate(participants):
  x=xs[i];fill,stroke=PAL[role]
  a+=[f'<rect x="{x-138}" y="12" width="276" height="70" rx="6" fill="{fill}" stroke="{stroke}" stroke-width="2"/><text x="{x}" y="53" text-anchor="middle" font-family="Arial" font-size="21" fill="#203346">{html.escape(label)}</text><line x1="{x}" y1="82" x2="{x}" y2="{H-35}" stroke="#8A9BAB" stroke-dasharray="7 6"/>']
  puml.append(f'participant "{label}" as p{i} {fill}')
 for i,(label,role) in enumerate(participants):
  if i==0:continue
  opened=None;last=0;intervals=[]
  for k,(fr,to,lab,ret) in enumerate(messages):
   y=120+k*100
   if to==i and fr!=i and not ret:
    if opened is None:opened=y
    last=y
   if fr==i and to!=i and ret and opened is not None:intervals.append((opened,y));opened=None
  if opened is not None:intervals.append((opened,last+38))
  fill,stroke=PAL[role]
  for top,bottom in intervals:a.append(f'<rect x="{xs[i]-7}" y="{top}" width="14" height="{bottom-top}" fill="{fill}" stroke="{stroke}"/>')
 active=set()
 for k,(fr,to,label,ret) in enumerate(messages):
  y=120+k*100;x1,x2=xs[fr],xs[to];dash='stroke-dasharray="7 5"' if ret else ''
  if fr==to:
   a+=[f'<path d="M{x1+7} {y} h65 v28 h-65" fill="none" stroke="#33465A" stroke-width="1.7" marker-end="url(#arr)"/><text x="{x1+78}" y="{y+13}" font-family="Arial" font-size="20">{k+1}. {html.escape(label)}</text>']
  else:a+=[f'<line x1="{x1}" y1="{y}" x2="{x2}" y2="{y}" stroke="#33465A" stroke-width="1.7" {dash} marker-end="url(#arr)"/><text x="{(x1+x2)/2}" y="{y-10}" text-anchor="middle" font-family="Arial" font-size="20">{k+1}. {html.escape(label)}</text>']
  puml.append(f'p{fr} '+('-->' if ret else '->')+f' p{to}: {label}')
  if not ret and to!=fr and to not in active:puml.append(f'activate p{to}');active.add(to)
  if ret and fr in active:puml.append(f'deactivate p{fr}');active.remove(fr)
 a+=['</svg>'];svg=''.join(a)
 (OUT/(name+'.svg')).write_text(svg,encoding='utf8');(SRC/(name+'.svg')).write_text(svg,encoding='utf8')
 puml+=['note over p0,p3: '+note,'@enduml'];(SRC/(name+'.puml')).write_text('\n'.join(puml),encoding='utf8')
seq('11_sequence_expense',[('TransactionDialogController','ui'),('TransactionService','service'),('JdbcCategoryDao','db'),('JDBC / SQLite','db')],[(0,1,'addExpense(...)',0),(1,2,'findById(categoryId)',0),(2,1,'Optional<Category>',1),(1,1,'Validators.validateExpense(...)',0),(1,3,'getConnection(); begin',0),(1,3,'INSERT; generated key',0),(1,3,'commit()',0),(1,0,'Expense with id',1)],'SQL failure rolls back; addExpense uses direct JDBC.')
seq('12_sequence_goal',[('GoalsController','ui'),('GoalService','service'),('JdbcSavingsGoalDao','db'),('JDBC / SQLite','db')],[(0,1,'createGoal(...)',0),(1,1,'Validators.validateSavingsGoal(...)',0),(1,1,'getTotalActiveRatio()',0),(1,2,'findActiveGoals(userId)',0),(2,1,'List<SavingsGoal>',1),(1,2,'save(new SavingsGoal(...))',0),(2,3,'INSERT savings_goals',0),(3,2,'generated id',1),(2,1,'saved goal',1),(1,0,'SavingsGoal',1)],'Input and active-ratio cap are checked before save.')
(ROOT/'diagrams/manifest.json').write_text(json.dumps(manifest,indent=2),encoding='utf8')
print('Rebuilt',len(manifest),'editable diagram cases')

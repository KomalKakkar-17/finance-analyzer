"""Generate the editable report and an equivalent page-model PDF without app execution."""
from pathlib import Path
import sys,json,re,html,zipfile
from copy import deepcopy
from PIL import Image as PILImage
from docx import Document
from docx.shared import Inches,Pt,RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH,WD_BREAK,WD_TAB_ALIGNMENT,WD_TAB_LEADER
from docx.enum.table import WD_TABLE_ALIGNMENT,WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import Paragraph,Spacer,Image,Table,TableStyle,Frame,KeepTogether
from report_content import ROOT,REPO,pages,figures,tables,save_content
sys.stdout.reconfigure(encoding='utf8')
save_content();STEM='Enhanced_Personal_Finance_Analyzer_UCS503_EndSem'
FONT=Path('C:/Windows/Fonts')
for name,file in [('TimesAcademic','times.ttf'),('TimesAcademic-Bold','timesbd.ttf'),('TimesAcademic-Italic','timesi.ttf')]:pdfmetrics.registerFont(TTFont(name,str(FONT/file)))
pdfmetrics.registerFontFamily('TimesAcademic',normal='TimesAcademic',bold='TimesAcademic-Bold',italic='TimesAcademic-Italic',boldItalic='TimesAcademic-Bold')
PW,PH=A4;M=54;CW=PW-2*M;CH=PH-2*M-12
styles={
 'p':ParagraphStyle('body',fontName='TimesAcademic',fontSize=12,leading=15.5,spaceAfter=7),
 'h':ParagraphStyle('sub',fontName='TimesAcademic-Bold',fontSize=12,leading=15,spaceBefore=5,spaceAfter=6),
 'title':ParagraphStyle('title',fontName='TimesAcademic-Bold',fontSize=17,leading=20,spaceAfter=11),
 'caption':ParagraphStyle('caption',fontName='TimesAcademic-Italic',fontSize=10.5,leading=13,spaceBefore=5,spaceAfter=6),
 'source':ParagraphStyle('source',fontName='TimesAcademic',fontSize=9,leading=11,spaceBefore=5,spaceAfter=4),
 'cell':ParagraphStyle('cell',fontName='TimesAcademic',fontSize=10.2,leading=12),
 'headcell':ParagraphStyle('headcell',fontName='TimesAcademic-Bold',fontSize=10.2,leading=12),
 'toc':ParagraphStyle('toc',fontName='TimesAcademic',fontSize=12,leading=16,spaceAfter=4),
}
def esc(s):return html.escape(str(s)).replace('\n','<br/>')
def para(s,style='p'):return Paragraph(esc(s),styles[style])
TOC=[('1. Project Selection Phase',4),('1.1 Software Bid',4),('1.2 Project Overview',5),('2. Analysis Phase',7),('2.1 Use Cases',7),('2.1.1 Use-Case Diagrams',7),('2.1.2 Use Case Templates',9),('2.2 Activity Diagram and Swimlane Diagrams',15),('2.3 Data Flow Diagrams',18),('2.3.1 DFD Level 0',18),('2.3.2 DFD Level 1',19),('2.3.3 DFD Level 2',20),('2.4 Software Requirement Specification in IEEE Format',21),('2.5 User Stories and Story Cards',30),('3. Design Phase',32),('3.1 Class Diagram',32),('3.2 Sequence Diagram',34),('3.3 Collaboration Diagram',36),('3.4 State Chart Diagrams',38),('4. Implementation',40),('4.1 Component Diagrams',40),('4.2 Deployment Diagrams',41),('4.3 Screenshots',42),('5. Testing',46),('5.1 Test Plan',46),('5.2 Test Cases',47),('5.3 Test Reports by Peers',50)]
FIGPAGES=[7,8,15,16,17,18,19,20,32,33,34,35,36,37,38,39,40,41]

def pdf_table(item):
 widths=[CW*x for x in (item.get('widths') or [1/len(item['headers'])]*len(item['headers']))]
 data=[[para(s,'headcell') for s in item['headers']]]+[[para(s,'cell') for s in row] for row in item['rows']]
 t=Table(data,colWidths=widths,hAlign='LEFT',repeatRows=1)
 t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#e4eaf0')),('GRID',(0,0),(-1,-1),.45,colors.HexColor('#d9d9d9')),('VALIGN',(0,0),(-1,-1),'MIDDLE'),('LEFTPADDING',(0,0),(-1,-1),6),('RIGHTPADDING',(0,0),(-1,-1),6),('TOPPADDING',(0,0),(-1,-1),5),('BOTTOMPADDING',(0,0),(-1,-1),5)]))
 return [para(item['caption'],'caption'),t,Spacer(1,7)]
def image_flow(item):
 file=ROOT/'diagrams/rendered'/f"{item['id']}.png";w,h=PILImage.open(file).size;scale=min(CW/w,item['height']/h)
 return [Image(str(file),width=w*scale,height=h*scale),para(item['caption'],'caption')]
def placeholder_flow(item):
 data=[[para(item['title'],'h')],[para(item['text'])]];t=Table(data,colWidths=[CW],rowHeights=[33,item['height']-33]);t.setStyle(TableStyle([('BOX',(0,0),(-1,-1),.6,colors.gray),('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),12),('TOPPADDING',(0,0),(-1,-1),9)]));return [t,Spacer(1,8)]

pdf=ROOT/(STEM+'.pdf');c=canvas.Canvas(str(pdf),pagesize=A4);c.setTitle('Enhanced Personal Finance Analyzer UCS503 End Semester Report');c.setAuthor('Project team')
layout=[]
for num,p in enumerate(pages,1):
 if p.get('cover'):
  y=PH-105;c.setFont('TimesAcademic-Bold',21)
  for line in ['ENHANCED PERSONAL','FINANCE ANALYZER']:c.drawCentredString(PW/2,y,line);y-=28
  y-=25
  for i,item in enumerate(p['items']):
   s=item['text'];c.setFont('TimesAcademic-Bold' if i in (0,1,2,9) else 'TimesAcademic',13 if i<2 else 12)
   if i in (2,6,8,10):y-=18
   c.drawCentredString(PW/2,y,s);y-=21
  layout.append({'page':num,'remaining':y});c.showPage();continue
 c.setFont('TimesAcademic',9);c.drawString(M,PH-32,'Enhanced Personal Finance Analyzer');c.drawRightString(PW-M,PH-32,'UCS503 | End-Semester')
 c.setFont('TimesAcademic',10);c.drawCentredString(PW/2,28,str(num))
 flows=[para(p['title'] if num!=3 else 'List of Figures and Report Guide','title')]
 if p.get('toc')==1:
  for title,n in TOC:flows.append(para(title+'  '+'. '*max(2,38-len(title)//2)+'  '+str(n),'toc'))
 elif p.get('toc')==2:
  for i,((id,title),n) in enumerate(zip(figures,FIGPAGES),1):flows.append(para(f'Figure {i}. {title}  —  {n}','toc'))
  flows += [Spacer(1,12),para('Reading the evidence','h'),para('Requirements retain the eight original MST IDs. EXT IDs label final-scope additions. A01-A18 identify executed automated cases; T01-T20 are planned manual or integration cases. “Implemented” describes inspected paths and does not certify GUI execution.'),para('Student names and roll numbers are transcribed from the MST title slide and should be confirmed. Faculty, group, cohort and submission details remain placeholders. Screenshot spaces and the peer-testing form require genuine completion before submission.')]
 else:
  for item in p['items']:
   k=item['kind']
   if k in ('p','h','source'):flows.append(para(item['text'],k))
   elif k=='table':flows+=pdf_table(item)
   elif k=='figure':flows+=image_flow(item)
   elif k=='placeholder':flows+=placeholder_flow(item)
 frame=Frame(M,M+8,CW,CH,leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0)
 pending=list(flows);frame.addFromList(pending,c)
 if pending:
  print('OVERFLOW',num,p['title'],'pending=',len(pending));layout.append({'page':num,'overflow':len(pending)})
 else:layout.append({'page':num,'remaining':round(frame._y-(M+8),1)})
 c.showPage()
c.save();(ROOT/'evidence/pdf_layout.json').write_text(json.dumps(layout,indent=2),encoding='utf8')

doc=Document();sec=doc.sections[0];sec.page_width=Pt(PW);sec.page_height=Pt(PH);sec.top_margin=sec.bottom_margin=Pt(M);sec.left_margin=sec.right_margin=Pt(M);sec.header_distance=Pt(20);sec.footer_distance=Pt(20)
normal=doc.styles['Normal'];normal.font.name='Times New Roman';normal.font.size=Pt(12);normal.paragraph_format.line_spacing=1.08;normal.paragraph_format.space_after=Pt(7)
for name,size in [('Title',21),('Heading 1',17),('Heading 2',15),('Heading 3',12)]:
 st=doc.styles[name];st.font.name='Times New Roman';st.font.size=Pt(size);st.font.color.rgb=RGBColor(0,0,0);st.font.bold=True;st.paragraph_format.space_after=Pt(9);st.paragraph_format.space_before=Pt(0)
for name,size,bold in [('PageTitle',17,True),('Subheading',12,True),('Source',9,False),('TOC Entry',12,False)]:
 st=doc.styles.add_style(name,1);st.base_style=normal;st.font.name='Times New Roman';st.font.size=Pt(size);st.font.bold=bold;st.font.color.rgb=RGBColor(0,0,0);st.paragraph_format.space_after=Pt(7)
doc.styles['Caption'].font.name='Times New Roman';doc.styles['Caption'].font.size=Pt(10.5);doc.styles['Caption'].font.color.rgb=RGBColor(0,0,0)
sec.different_first_page_header_footer=True
hp=sec.header.paragraphs[0];hp.add_run('Enhanced Personal Finance Analyzer').font.size=Pt(9);hp.paragraph_format.tab_stops.add_tab_stop(Pt(CW),WD_TAB_ALIGNMENT.RIGHT);hp.add_run('\tUCS503 | End-Semester').font.size=Pt(9)
fp=sec.footer.paragraphs[0];fp.alignment=WD_ALIGN_PARAGRAPH.CENTER
def field(run,instruction,cached='1'):
 for typ in ('begin','separate'):
  el=OxmlElement('w:fldChar');el.set(qn('w:fldCharType'),typ);run._r.append(el)
  if typ=='begin':ins=OxmlElement('w:instrText');ins.set(qn('xml:space'),'preserve');ins.text=instruction;run._r.append(ins)
 tx=OxmlElement('w:t');tx.text=cached;run._r.append(tx);el=OxmlElement('w:fldChar');el.set(qn('w:fldCharType'),'end');run._r.append(el)
field(fp.add_run(),' PAGE ')
settings=doc.settings.element;update=OxmlElement('w:updateFields');update.set(qn('w:val'),'true');settings.append(update)
def dpara(s,style=None):
 p=doc.add_paragraph(s,style=style);return p
def dtable(item):
 cap=dpara(item['caption'],'Caption');cap.paragraph_format.keep_with_next=True
 t=doc.add_table(rows=1,cols=len(item['headers']));t.alignment=WD_TABLE_ALIGNMENT.CENTER;t.autofit=False
 widths=item.get('widths') or [1/len(item['headers'])]*len(item['headers'])
 for col,w in zip(t.columns,widths):col.width=Pt(CW*w)
 for i,row in enumerate([item['headers']]+item['rows']):
  cells=t.rows[0].cells if i==0 else t.add_row().cells
  for j,(cell,s) in enumerate(zip(cells,row)):
   cell.width=Pt(CW*widths[j]);cell.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
   p=cell.paragraphs[0];p.paragraph_format.space_after=Pt(2);p.paragraph_format.space_before=Pt(2);p.paragraph_format.line_spacing=1.0
   r=p.add_run(str(s));r.font.name='Times New Roman';r.font.size=Pt(10.2);r.bold=i==0
   pr=cell._tc.get_or_add_tcPr();b=OxmlElement('w:tcBorders')
   for edge in ('top','left','bottom','right'):
    e=OxmlElement('w:'+edge);e.set(qn('w:val'),'single');e.set(qn('w:sz'),'4');e.set(qn('w:color'),'D9D9D9');b.append(e)
   pr.append(b);mar=OxmlElement('w:tcMar')
   for edge,val in [('top',65),('bottom',65),('left',85),('right',85)]:
    e=OxmlElement('w:'+edge);e.set(qn('w:w'),str(val));e.set(qn('w:type'),'dxa');mar.append(e)
   pr.append(mar)
   if i==0:shade=OxmlElement('w:shd');shade.set(qn('w:fill'),'E4EAF0');pr.append(shade)
  if i==0:repeat=OxmlElement('w:tblHeader');t.rows[0]._tr.get_or_add_trPr().append(repeat)
 dpara('').paragraph_format.space_after=Pt(1)
def dimage(item):
 file=ROOT/'diagrams/rendered'/f"{item['id']}.png";w,h=PILImage.open(file).size;scale=min(CW/w,item['height']/h)
 p=doc.add_paragraph();p.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.paragraph_format.space_after=Pt(2);p.paragraph_format.keep_with_next=True
 r=p.add_run();r.add_picture(str(file),width=Pt(w*scale),height=Pt(h*scale));inline=r._r.xpath('.//wp:docPr')[0];inline.set('descr',item['caption']);dpara(item['caption'],'Caption')
mandatory={'1. PROJECT SELECTION PHASE','1.2 Project Overview','2. ANALYSIS PHASE','2.1.2 Use Case Templates','2.2 Activity Diagram and Swimlane Diagrams','2.3 Data Flow Diagrams','2.3.2 DFD Level 1','2.3.3 DFD Level 2','2.4 Software Requirement Specification in IEEE Format','2.5 User Stories and Story Cards','3. DESIGN PHASE','3.2 Sequence Diagram','3.3 Collaboration Diagram','3.4 State Chart Diagrams','4. IMPLEMENTATION','4.2 Deployment Diagrams','4.3 Screenshots','5. TESTING','5.2 Test Cases','5.3 Test Reports by Peers'}
for num,p in enumerate(pages,1):
 if num>1:doc.add_page_break()
 if p.get('cover'):
  dpara('');q=dpara(p['title'],'Title');q.alignment=WD_ALIGN_PARAGRAPH.CENTER
  for i,item in enumerate(p['items']):
   q=dpara(item['text']);q.alignment=WD_ALIGN_PARAGRAPH.CENTER;q.paragraph_format.space_after=Pt(10 if i in (1,5,7,9) else 5)
   if i in (0,1,2,9):q.runs[0].bold=True
  continue
 heading=p['title'] if num!=3 else 'List of Figures and Report Guide'
 dpara(heading,('Heading 1' if p['level']==1 else 'Heading 2') if heading in mandatory and (num!=43 and num!=44 and num!=45) else 'PageTitle')
 if p.get('toc')==1:
  q=doc.add_paragraph();r=q.add_run();el=OxmlElement('w:fldChar');el.set(qn('w:fldCharType'),'begin');r._r.append(el);ins=OxmlElement('w:instrText');ins.set(qn('xml:space'),'preserve');ins.text=' TOC \\o "1-3" \\h \\z \\u ';r._r.append(ins);el=OxmlElement('w:fldChar');el.set(qn('w:fldCharType'),'separate');r._r.append(el)
  for title,n in TOC:
   q=dpara(title+'\t'+str(n),'TOC Entry');q.paragraph_format.tab_stops.add_tab_stop(Pt(CW),WD_TAB_ALIGNMENT.RIGHT,WD_TAB_LEADER.DOTS);q.paragraph_format.space_after=Pt(3)
  el=OxmlElement('w:fldChar');el.set(qn('w:fldCharType'),'end');q.add_run()._r.append(el)
 elif p.get('toc')==2:
  for i,((id,title),n) in enumerate(zip(figures,FIGPAGES),1):q=dpara(f'Figure {i}. {title}\t{n}','TOC Entry');q.paragraph_format.space_after=Pt(4);q.paragraph_format.tab_stops.add_tab_stop(Pt(CW),WD_TAB_ALIGNMENT.RIGHT,WD_TAB_LEADER.DOTS)
  dpara('Reading the evidence','Subheading');dpara('Requirements retain FR-01 through FR-08 from the MST. EXT IDs mark additions. A01-A18 identify executed automated cases; T01-T20 are planned manual/integration cases. Implementation inspection does not certify GUI behavior.');dpara('Confirm the team details transcribed from the MST and replace the remaining cover, screenshot and peer-testing placeholders before submission. Word can update this table of contents and page fields after editing.')
 else:
  for item in p['items']:
   k=item['kind']
   if k=='p':dpara(item['text'])
   elif k=='source':dpara(item['text'],'Source')
   elif k=='h':
    match=re.match(r'^(\d+(?:\.\d+)+)',item['text'])
    dpara(item['text'],'Heading '+str(min(3,len(match.group(1).split('.')))) if match else 'Subheading')
   elif k=='table':dtable(item)
   elif k=='figure':dimage(item)
   elif k=='placeholder':
    q=dpara(item['title'],'Subheading');q.paragraph_format.space_before=Pt(12);dpara(item['text']);q=dpara('[Genuine application screenshot to be inserted here]');q.paragraph_format.space_after=Pt(28);q.runs[0].italic=True
doc.core_properties.title='Enhanced Personal Finance Analyzer UCS503 End Semester Report';doc.core_properties.subject='Source-grounded software engineering report';doc.core_properties.author='Project team';doc.core_properties.keywords='UCS503 JavaFX SQLite Software Engineering'
doc.save(ROOT/(STEM+'.docx'))
Document(ROOT/(STEM+'.docx'))
with zipfile.ZipFile(ROOT/(STEM+'.docx')) as z:embedded=len([x for x in z.namelist() if x.startswith('word/media/')]);xml=z.read('word/document.xml').decode();assert embedded==18,embedded;assert xml.count('TOC \\o')==1
print('DOCX written and reopened. Embedded images:',embedded)
print('PDF page-model pages:',len(pages));print('Overflow pages:',[x for x in layout if 'overflow' in x])

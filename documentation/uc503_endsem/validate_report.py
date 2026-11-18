from pathlib import Path
import json,hashlib,zipfile,xml.etree.ElementTree as ET
from pypdf import PdfReader
from docx import Document
ROOT=Path(__file__).resolve().parent;REPO=ROOT.parents[1];STEM='Enhanced_Personal_Finance_Analyzer_UCS503_EndSem'
pdf=PdfReader(ROOT/(STEM+'.pdf'));doc=Document(ROOT/(STEM+'.docx'))
text='\n'.join(p.text for p in doc.paragraphs)
required=['1.1 Software Bid','1.2 Project Overview','2.1 Use Cases','2.1.1 Use-Case Diagrams','2.1.2 Use Case Templates','2.2 Activity Diagram and Swimlane Diagrams','2.3 Data Flow Diagrams','2.3.1 DFD Level 0','2.3.2 DFD Level 1','2.3.3 DFD Level 2','2.4 Software Requirement Specification in IEEE Format','2.5 User Stories and Story Cards','3.1 Class Diagram','3.2 Sequence Diagram','3.3 Collaboration Diagram','3.4 State Chart Diagrams','4.1 Component Diagrams','4.2 Deployment Diagrams','4.3 Screenshots','5.1 Test Plan','5.2 Test Cases','5.3 Test Reports by Peers']
assert all(h in text for h in required)
assert len(pdf.pages)==50
assert len(doc.inline_shapes)==18
assert text.count('SCREENSHOT NOT SUPPLIED')==8
assert not any('overflow' in x for x in json.loads((ROOT/'evidence/pdf_layout.json').read_text()))
inventory=json.loads((ROOT/'diagrams/manifest.json').read_text());counts={}
for d in inventory:
 counts[d['type']]=counts.get(d['type'],0)+1
 ET.parse(ROOT/'diagrams/rendered'/(d['id']+'.svg'))
 assert (ROOT/'diagrams/rendered'/(d['id']+'.png')).is_file()
 assert any((ROOT/'diagrams/source'/(d['id']+ext)).is_file() for ext in ['.dot','.puml'])
for typ in ['Class','Sequence','Communication','State chart']:assert counts[typ]>=2
manifest=json.loads((ROOT/'evidence/source_manifest.json').read_text());drift=[p for p,h in manifest.items() if hashlib.sha256((REPO/p).read_bytes()).hexdigest()!=h];assert not drift,drift
refs=[REPO/'report_references'/f for f in ['UCS503_EndSem_TOC.doc','PeerWise_Seniors_Report.pdf','Finance_Analyzer_MST.pptx']]
(ROOT/'evidence/reference_inventory.json').write_text(json.dumps([{'file':str(p.relative_to(REPO)),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in refs],indent=2),encoding='utf8')
result={'pdf_actual_page_count':50,'docx_rendered_page_count':'unverified','diagram_counts':counts,'total_diagrams':len(inventory),'embedded_docx_images':len(doc.inline_shapes),'docx_tables':len(doc.tables),'mandatory_headings_present':True,'source_hash_drift':drift,'screenshot_placeholders':8,'pdf_frame_overflow':False,'pdf_origin':'shared content/page model; not DOCX conversion','source_files_untouched_by_documentation':True}
(ROOT/'evidence/final_validation.json').write_text(json.dumps(result,indent=2),encoding='utf8')
archive=ROOT/(STEM+'_Package.zip')
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted(ROOT.rglob('*')):
  if p.is_file() and p.suffix!='.zip' and '__pycache__' not in p.parts:z.write(p,'uc503_endsem/'+str(p.relative_to(ROOT)).replace('\\','/'))
print(json.dumps(result,indent=2));print('Package:',archive,'bytes:',archive.stat().st_size)

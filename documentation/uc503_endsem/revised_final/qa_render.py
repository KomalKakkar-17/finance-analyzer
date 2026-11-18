from pathlib import Path
import subprocess,json,re
from pypdf import PdfReader
from PIL import Image,ImageOps,ImageDraw
ROOT=Path(__file__).resolve().parent
TMP=Path('C:/Users/marus/AppData/Local/Temp/finance-report-revision/approved-qa');TMP.mkdir(parents=True,exist_ok=True)
NATIVE=Path('C:/Program Files/WindowsApps/OpenAI.CodexPrimaryRuntime.v26-1112-737-0_26.1112.737.0_x64__3k8sg7r9htsxt/dependencies/native/poppler/Library/bin')
pdf=ROOT/'Enhanced_Personal_Finance_Analyzer_UCS503_Final.pdf'
subprocess.run([str(NATIVE/'pdftoppm.exe'),'-scale-to','1600','-png',str(pdf),str(TMP/'page')],check=True)
reader=PdfReader(pdf);texts=[x.extract_text() for x in reader.pages]
for p in TMP.glob('page-*.png'):
 if int(p.stem.split('-')[-1])>len(texts):p.unlink()
summary=[{'page':i+1,'characters':len(t),'heading':t[:140].replace('\n',' | '),'size':[float(reader.pages[i].mediabox.width),float(reader.pages[i].mediabox.height)]} for i,t in enumerate(texts)]
(ROOT/'evidence/pagination.json').write_text(json.dumps(summary,indent=2),encoding='utf8')
imgs=sorted(TMP.glob('page-*.png'))
for start in range(0,len(imgs),6):
 canvas=Image.new('RGB',(1800,1800),'#CBD5DF');draw=ImageDraw.Draw(canvas)
 for k,p in enumerate(imgs[start:start+6]):
  im=Image.open(p).convert('RGB');im.thumbnail((575,840));x=(k%3)*600+(600-im.width)//2;y=(k//3)*900+30
  canvas.paste(im,(x,y));draw.text(((k%3)*600+12,(k//3)*900+8),str(start+k+1),fill='black')
 canvas.save(TMP/f'contact-{start//6+1:02}.png')
if (NATIVE/'pdftotext.exe').exists():subprocess.run([str(NATIVE/'pdftotext.exe'),'-bbox',str(pdf),str(TMP/'layout.html')],check=True)
print('Rendered',len(imgs),'pages; contact sheets',len(list(TMP.glob('contact-*.png'))))

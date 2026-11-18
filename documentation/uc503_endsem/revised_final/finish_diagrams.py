from pathlib import Path
import xml.etree.ElementTree as E
ROOT=Path(__file__).resolve().parent
ns='http://www.w3.org/2000/svg';E.register_namespace('',ns)
for name in ['01_usecase_track','02_usecase_plan']:
 p=ROOT/'diagrams/rendered'/(name+'.svg');tree=E.parse(p)
 for g in tree.iter('{'+ns+'}g'):
  title=g.find('{'+ns+'}title')
  if title is not None and title.text=='user':
   poly=g.find('{'+ns+'}polygon');pts=[tuple(map(float,q.split(','))) for q in poly.get('points').split()]
   x=(min(q[0] for q in pts)+max(q[0] for q in pts))/2;y=min(q[1] for q in pts)+19
   for c in list(g):
    if c is not title:g.remove(c)
   def el(tag,attrs):return E.SubElement(g,'{'+ns+'}'+tag,{k:str(v) for k,v in attrs.items()})
   el('circle',{'cx':x,'cy':y,'r':11,'fill':'#DDF3F0','stroke':'#247B75','stroke-width':2})
   for coords in [(x,y+11,x,y+60),(x-25,y+30,x+25,y+30),(x,y+60,x-22,y+90),(x,y+60,x+22,y+90)]:
    el('line',dict(zip(['x1','y1','x2','y2'],coords))|{'stroke':'#247B75','stroke-width':2})
   t=el('text',{'x':x,'y':y+112,'font-family':'Arial','font-size':14,'text-anchor':'middle','fill':'#203346'});t.text='Account holder'
 tree.write(p,encoding='utf-8',xml_declaration=True)
 (ROOT/'diagrams/source'/(name+'.svg')).write_bytes(p.read_bytes())
print('Finished UML actor symbols')

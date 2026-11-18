const fs=require('fs'),path=require('path');
const mods='C:/Program Files/WindowsApps/OpenAI.CodexPrimaryRuntime.v26-1112-737-0_26.1112.737.0_x64__3k8sg7r9htsxt/dependencies/node/node_modules';
const {instance}=require(mods+'/@viz-js/viz/dist/viz.cjs');
const sharp=require(mods+'/sharp');
(async()=>{const viz=await instance();const src=path.join(__dirname,'diagrams/source'),out=path.join(__dirname,'diagrams/rendered');
for(const file of fs.readdirSync(src).filter(f=>f.endsWith('.dot'))){const svg=viz.renderString(fs.readFileSync(path.join(src,file),'utf8'),{format:'svg',engine:'dot'});fs.writeFileSync(path.join(out,file.replace('.dot','.svg')),svg);}
for(const file of fs.readdirSync(out).filter(f=>f.endsWith('.svg'))){await sharp(Buffer.from(fs.readFileSync(path.join(out,file),'utf8')),{density:220}).resize({width:2000,withoutEnlargement:false}).png().toFile(path.join(out,file.replace('.svg','.png')));console.log('Rendered '+file);}
})().catch(e=>{console.error(e);process.exit(1)});

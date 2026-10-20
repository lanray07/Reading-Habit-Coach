/** Kotlin string scanner: understands nested interpolation, quoted expressions and comments.
 * The reviewed source manifest distinguishes presentation text from stable database/protocol IDs.
 */
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
const root='app/src/main/java';
function walk(d){return fs.readdirSync(d,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(d,e.name)):[path.join(d,e.name)]);}
function quoted(s,start){let i=start+1,parts=[''],args=[];for(;i<s.length;){
 if(s[i]==='\\'){parts[parts.length-1]+=s.slice(i,i+2);i+=2;continue;}
 if(s[i]==='"')return {start,end:i+1,parts,args,raw:s.slice(start,i+1)};
 if(s[i]==='$'&&s[i+1]==='{'){let j=i+2,depth=1;for(;j<s.length&&depth;j++){if(s[j]==='"'){j=quoted(s,j).end-1;continue;}if(s[j]==='{')depth++;if(s[j]==='}')depth--;}
  args.push(s.slice(i+2,j-1));parts.push('');i=j;continue;}
 if(s[i]==='$'&&/[A-Za-z_]/.test(s[i+1]||'')){let j=i+1;while(/[\w]/.test(s[j]||''))j++;args.push(s.slice(i+1,j));parts.push('');i=j;continue;}
 parts[parts.length-1]+=s[i++];
 }throw Error('Unclosed string');}
const found=[];
for(const file of walk(root).filter(f=>f.endsWith('.kt')&&!f.replaceAll('\\','/').includes('/i18n/'))){const s=fs.readFileSync(file,'utf8');
if(process.argv.includes('--apply')&&s.includes('L10n.text('))throw Error('This source has already been extracted. Review new candidates and add resources explicitly; do not reapply an old selection.');
for(let i=0;i<s.length;){
 if(s.slice(i,i+2)==='//'){i=s.indexOf('\n',i);if(i<0)break;continue;}
 if(s.slice(i,i+2)==='/*'){i=s.indexOf('*/',i+2)+2;continue;}
 if(s[i]==="'"){i++;while(i<s.length){if(s[i]==='\\'){i+=2;continue;}if(s[i++]==="'")break;}continue;}
 if(s[i]!=='"'){i++;continue;}
 const q=quoted(s,i);i=q.end;
 let value;try{value=q.parts.map(p=>JSON.parse('"'+p+'"')).map((p,n)=>p+(n<q.args.length?`%${n+1}$s`:'')).join('');}catch{continue;}
 if(!/[A-Za-z]/.test(value.replace(/%\d+\$s/g,'')))continue;
 found.push({...q,value,key:'msg_'+crypto.createHash('sha256').update(value).digest('hex').slice(0,12),file:file.replaceAll('\\','/'),line:s.slice(0,q.start).split('\n').length,context:s.slice(Math.max(0,q.start-45),q.end+30).replaceAll('\n',' ')});
 }}
if(!process.argv.includes('--apply')){
 fs.writeFileSync('localization/source-candidates.json',JSON.stringify(found,null,2));
 console.log(found.map((q,i)=>`${i}\t${q.file.split('/').at(-1)}:${q.line}\t${q.value}`).join('\n'));
}else{
 const manifest=JSON.parse(fs.readFileSync('localization/source-selection.json'));
 const allowed=new Set(manifest.indices);const selected=found.filter((q,i)=>allowed.has(i));
 const catalog=Object.fromEntries([...selected,...found.filter((q,i)=>manifest.catalogOnly.includes(i))].map(q=>[q.key,q.value]));
 for(const file of new Set(selected.map(q=>q.file))){let s=fs.readFileSync(file,'utf8');for(const q of selected.filter(q=>q.file===file).sort((a,b)=>b.start-a.start)){
 const call=`com.readinghabitcoach.app.i18n.L10n.text(com.readinghabitcoach.app.R.string.${q.key}${q.args.length?', '+q.args.join(', '):''})`;
 s=s.slice(0,q.start)+call+s.slice(q.end);
 }fs.writeFileSync(file,s);}
 fs.writeFileSync('localization/messages.json',JSON.stringify(catalog,null,2));
 fs.writeFileSync('localization/extracted-sites.json',JSON.stringify(selected.map(({file,line,key,value})=>({file,line,key,value})),null,2));
 console.log(`Extracted ${selected.length} sites / ${Object.keys(catalog).length} messages`);
}

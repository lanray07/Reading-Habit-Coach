/** Extract safe static Kotlin UI literals. Default is a reviewable plan; --apply updates resources and call sites. */
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
function walk(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(dir,e.name)):[path.join(dir,e.name)]);}
const changes=[];const values=new Map();
for(const file of walk('app/src/main/java').filter(f=>f.endsWith('.kt'))){
 let text=fs.readFileSync(file,'utf8');let changed=false;
 text=text.replace(/\b(Text|Primary|PageTitle|SectionTitle|EmptyState)\(\s*("(?:\\.|[^"\\])*?")/g,(whole,call,literal)=>{
  if(literal.includes('$'))return whole; // Dynamic messages require explicit Android format placeholders and review.
  let value;try{value=JSON.parse(literal);}catch{return whole;}
  if(!value.trim())return whole;
  const key='ui_'+crypto.createHash('sha256').update(value).digest('hex').slice(0,12);values.set(key,value);
  changes.push({file,key,text:value});changed=true;
  return `${call}(androidx.compose.ui.res.stringResource(com.readinghabitcoach.app.R.string.${key})`;
 });
 if(changed&&process.argv.includes('--apply'))fs.writeFileSync(file,text);
}
fs.mkdirSync('localization',{recursive:true});fs.writeFileSync('localization/extraction-plan.json',JSON.stringify({status:process.argv.includes('--apply')?'applied':'review-plan-only',changes},null,2));
if(process.argv.includes('--apply')){
 const file='app/src/main/res/values/strings.xml';let xml=fs.readFileSync(file,'utf8');
 const escape=s=>s.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll("'","\\'").replaceAll('"','\\"').replaceAll('\n','\\n');
 const entries=[...values].filter(([key])=>!xml.includes(`name="${key}"`)).map(([key,value])=>`    <string name="${key}">${escape(value)}</string>`).join('\n');
 xml=xml.replace('</resources>',`\n${entries}\n</resources>`);fs.writeFileSync(file,xml);
}
console.log(`${changes.length} static call sites, ${values.size} distinct messages. ${process.argv.includes('--apply')?'Applied. Run Gradle and the locale report.':'Review localization/extraction-plan.json, then run with --apply.'}`);

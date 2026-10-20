/** Development-only LibreTranslate-compatible pipeline. No credentials enter the APK.
 * TRANSLATE_URL=https://your-reviewed-provider/translate TRANSLATE_KEY=... node tools/localize.mjs --translate
 */
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
const root = path.resolve('app/src/main/res');
const targets = {'en-rUS':'en','es':'es','fr':'fr','de':'de','it':'it','pt-rBR':'pt','pt-rPT':'pt','nl':'nl','pl':'pl','ar':'ar','hi':'hi'};
const source = fs.readFileSync(path.join(root,'values/strings.xml'),'utf8');
const strings = [...source.matchAll(/<string name="([^"]+)">([\s\S]*?)<\/string>/g)].map(m=>[m[1],m[2]]);
const cachePath = path.resolve('localization/cache.json');
fs.mkdirSync(path.dirname(cachePath),{recursive:true});
const cache = fs.existsSync(cachePath)?JSON.parse(fs.readFileSync(cachePath)):{};
const glossary = JSON.parse(fs.readFileSync('localization/glossary.json'));
const escape = s=>s.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll("'","\\'");
const placeholders = s=>s.match(/%\d+\$[sd]|%[sd]|\{\w+\}/g)||[];
const report={generatedAt:new Date().toISOString(),provider:'LibreTranslate-compatible HTTPS endpoint',humanReviewRequired:true,locales:{},hardcodedStrings:[]};
for(const [qualifier,locale] of Object.entries(targets)) {
  const output=[]; const missing=[];
  for(const [key,value] of strings) {
    const hash=crypto.createHash('sha256').update(`${locale}\0${value}\0${JSON.stringify(glossary)}`).digest('hex');
    let translated=cache[hash];
    if(locale==='en') translated=value;
    if(!translated && process.argv.includes('--translate')) {
      if(!process.env.TRANSLATE_URL?.startsWith('https://')) throw Error('Set a reviewed HTTPS TRANSLATE_URL. Credentials stay in the development environment.');
      let protectedText=value; const held=[];
      for(const token of [...placeholders(value), ...glossary.preserve]) {
        if(protectedText.includes(token)) { const marker=`ZXQ${held.length}QXZ`; protectedText=protectedText.replaceAll(token,marker); held.push([marker,token]); }
      }
      const response=await fetch(process.env.TRANSLATE_URL,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({q:protectedText,source:'en',target:locale,format:'text',api_key:process.env.TRANSLATE_KEY||undefined})});
      if(!response.ok) throw Error(`Translation provider returned ${response.status}`);
      translated=(await response.json()).translatedText;
      if(typeof translated!=='string') throw Error('Provider returned no translation');
      for(const [marker,token] of held) { if(!translated.includes(marker)) throw Error(`Provider damaged placeholder ${marker}`); translated=translated.replaceAll(marker,token); }
      if(JSON.stringify(placeholders(translated).sort())!==JSON.stringify(placeholders(value).sort())) throw Error(`Placeholder mismatch in ${key}`);
      cache[hash]=translated;
    }
    if(translated) output.push(`    <string name="${key}">${escape(translated)}</string>`); else missing.push(key);
  }
  if(output.length) {const dir=path.join(root,`values-${qualifier}`);fs.mkdirSync(dir,{recursive:true});fs.writeFileSync(path.join(dir,'strings.xml'),`<resources>\n${output.join('\n')}\n</resources>\n`);}
  report.locales[qualifier]={missing,translated:output.length,status:missing.length?'incomplete':'needs-human-review'};
}
function walk(dir) { return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(dir,e.name)):[path.join(dir,e.name)]); }
for(const file of walk('app/src/main/java').filter(f=>f.endsWith('.kt'))) {
  const text=fs.readFileSync(file,'utf8');
  for(const match of text.matchAll(/\b(?:Text|PageTitle|Field|Primary|SectionTitle)\(\s*"([^"\n]+)"/g)) report.hardcodedStrings.push({file,line:text.slice(0,match.index).split('\n').length,text:match[1]});
}
fs.writeFileSync(cachePath,JSON.stringify(cache,null,2));
fs.writeFileSync('localization/report.json',JSON.stringify(report,null,2));
console.log(`${strings.length} resource strings; ${report.hardcodedStrings.length} inline UI strings need extraction. See localization/report.json.`);
if(process.argv.includes('--strict') && (report.hardcodedStrings.length || Object.values(report.locales).some(l=>l.missing.length))) process.exitCode=1;

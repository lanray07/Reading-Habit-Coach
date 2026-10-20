/** Rasterise original vector icon sources and compose ACTUAL app captures. Never generates fake UI. */
import fs from 'node:fs';
import path from 'node:path';
import {pathToFileURL} from 'node:url';
const {chromium}=await import('playwright');
const browser=await chromium.launch({headless:true,...(process.env.CHROMIUM_PATH?{executablePath:process.env.CHROMIUM_PATH}:{})});
const escape=s=>s.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('"','&quot;');
fs.mkdirSync('branding',{recursive:true});fs.mkdirSync('screenshots/compositions',{recursive:true});
const xml=fs.readFileSync('app/src/main/res/drawable/ic_book.xml','utf8');
const paths=[...xml.matchAll(/<path\s+([^>]+)\/>/g)].map(m=>{
 const a=Object.fromEntries([...m[1].matchAll(/android:([\w]+)="([^"]*)"/g)].map(m=>[m[1],m[2]]));
 return `<path d="${a.pathData}" fill="${a.fillColor||'none'}" ${a.strokeColor?`stroke="${a.strokeColor}" stroke-width="${a.strokeWidth||1}" stroke-linecap="${a.strokeLineCap||'butt'}"`:''}/>`;
}).join('');
const svg=`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">${paths}</svg>`;
fs.writeFileSync('branding/icon-master.svg',svg);
const page=await browser.newPage({viewport:{width:1920,height:1200},deviceScaleFactor:1});
try {
 for(const size of [114,512,1024]) {
  await page.setViewportSize({width:size,height:size});await page.setContent(`<html><style>*{margin:0}svg{width:100vw;height:100vh}</style>${svg}</html>`);
  await page.screenshot({path:`branding/icon-${size}.png`,omitBackground:true});
 }
 const manifest=JSON.parse(fs.readFileSync('screenshots/manifest.json'));
 for(const item of manifest.captures) {
  const dir='screenshots/captures'; const capture=fs.existsSync(dir)?fs.readdirSync(dir).find(f=>f.startsWith(item.id+'-')&&f.endsWith('.png')):null;
  if(!capture){item.productionStatus='missing actual capture';continue;}
  const captureUrl='../captures/'+capture;
  const html=`<!doctype html><html lang="en-GB"><meta charset="utf-8"><style>*{box-sizing:border-box}body{margin:0;background:#f8f5ef;color:#292c29;font-family:system-ui,sans-serif;width:1920px;height:1200px;overflow:hidden}header{padding:35px 64px 28px;display:flex;align-items:center;gap:32px;border-bottom:1px solid #dedbd1}h1{font:42px/1.1 Georgia,serif;margin:0;color:#35574a}p{font-size:21px;max-width:800px;margin:12px 0 0;color:#62665e}.brand{margin-left:auto;font:22px Georgia,serif;white-space:nowrap;color:#35574a}main{display:flex;align-items:center;justify-content:center;height:1000px;padding:28px 70px 12px}img{max-height:930px;max-width:100%;object-fit:contain;border:1px solid #d8d8ca;box-shadow:0 15px 45px #35574a14}footer{position:absolute;bottom:12px;left:64px;font-size:13px;color:#62665e}</style><header><div><h1>${escape(item.headline)}</h1><p>${escape(item.support)}</p></div><div class="brand">Reading Habit Coach</div></header><main><img src="${captureUrl}" alt="Actual early-access application screen"></main><footer>Early-access 0.1.0 · Original QA sample content${item.caveat?' · '+escape(item.caveat):''}</footer></html>`;
  const filename=`screenshots/compositions/${item.id}-${item.screen.toLowerCase()}.html`;fs.writeFileSync(filename,html);
  await page.setViewportSize({width:1920,height:1200});await page.goto(pathToFileURL(path.resolve(filename)).href);await page.locator('img').evaluate(img=>img.decode());
  await page.screenshot({path:filename.replace('.html','.png')});item.productionStatus='composed from actual capture';item.sourceCapture=`captures/${capture}`;item.composition=filename.replace('screenshots/','').replace('.html','.png');
 }
 fs.writeFileSync('screenshots/manifest.json',JSON.stringify(manifest,null,2));
 console.log('Rendered original vector icons and available real-screen compositions.');
}finally{await browser.close();}

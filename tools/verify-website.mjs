import fs from 'node:fs';
import path from 'node:path';
const {chromium}=await import('playwright');
const base=process.env.WEBSITE_URL||'http://127.0.0.1:5183';
const browser=await chromium.launch({headless:true,...(process.env.CHROMIUM_PATH?{executablePath:process.env.CHROMIUM_PATH}:{})});
const report=[];
try {
 for(const size of [{width:1440,height:1000},{width:390,height:844}]) {
  const page=await browser.newPage({viewport:size}); const errors=[]; page.on('pageerror',e=>errors.push(e.message));
  await page.goto(base,{waitUntil:'networkidle'});
  const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth);
  if(overflow) throw Error(`Home overflows at ${size.width}px`);
  if(await page.locator('h1').count()!==1) throw Error('Home must have one main heading');
  await page.screenshot({path:`screenshots/website-${size.width===390?'mobile':'desktop'}-full.png`,fullPage:true});
  await page.getByRole('link',{name:'Reader',exact:true}).click();
  await page.getByRole('heading',{name:'A practical PDF view'}).waitFor();
  if(!(await page.locator('main').innerText()).includes('PDF text search, text selection, highlighting and read-aloud are unavailable')) throw Error('PDF limitation disclosure is missing');
  const readerOverflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth);
  if(readerOverflow) throw Error(`Reader page overflows at ${size.width}px`);
  report.push({viewport:size,homeOverflow:overflow,readerOverflow,errors}); await page.close();
 }
 const files=fs.readdirSync('website/dist',{withFileTypes:true}).filter(x=>x.isDirectory()&&x.name!=='assets');
 for(const f of files) {
  const response=await fetch(`${base}/${f.name}/`);if(response.status!==200)throw Error(`Route ${f.name}: ${response.status}`);
  const html=await response.text();if(!html.includes('rel="canonical"')||!html.includes('<h1>'))throw Error(`Missing page metadata: ${f.name}`);
 }
 fs.writeFileSync('screenshots/website-verification.json',JSON.stringify({method:'Playwright Chromium fallback: IAB viewport screenshots were clipped and click dispatch timed out',checkedAt:new Date().toISOString(),routes:files.length+1,checks:report},null,2));
 console.log(JSON.stringify({routes:files.length+1,checks:report}));
} finally {await browser.close();}

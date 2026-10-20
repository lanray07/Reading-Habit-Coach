/** Compatibility entry point: checked-in translation catalogs, no runtime translation. */
import {spawnSync} from 'node:child_process';
if(process.argv.includes('--translate')) {
  const result=spawnSync(process.platform==='win32'?'python':'python3',['tools/translate-locales.py','--fetch'],{stdio:'inherit'});
  if(result.status)process.exit(result.status);
}
const result=spawnSync(process.execPath,['tools/build-locales.mjs'],{stdio:'inherit'});
process.exit(result.status??1);

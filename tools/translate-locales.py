"""Development-only draft translation of public UI copy; no user content or APK network code.
Google's public web translation endpoint is best-effort, not a contracted Cloud API.
Cached JSON is checked in; normal builds never contact a translation service.
Run with --fetch to refresh missing entries, then review translations/*.json.
"""
import concurrent.futures, hashlib, json, pathlib, re, sys, time, urllib.error, urllib.parse, urllib.request
ROOT=pathlib.Path(__file__).resolve().parents[1]
store='--store' in sys.argv
messages=json.loads((ROOT/('localization/store-source.json' if store else 'localization/messages.json')).read_text(encoding='utf-8'))
directory=ROOT/('localization/store-translations' if store else 'localization/translations'); directory.mkdir(exist_ok=True)
languages=['de','es','fr','it','ja','pt-BR','pt-PT','nl','pl','sv']+([] if store else ['zh-CN','ar','hi'])
def translate(batch, language):
    if len(batch)!=1:raise ValueError('Translate one source message per request to preserve label boundaries.')
    protected={}; items=[]
    for i,(key,text) in enumerate(batch):
        held=[]
        def hold(m):
            token=f'ZXQ{len(held)}QXZ';held.append((token,m.group()));return token
        value=re.sub(r'%\d+\$s|%\.1f|Reading Habit Coach|Open Library|Readium|DELETE|YYYY-MM-DD|AES-256-GCM|PBKDF2-HMAC-SHA256',hold,text)
        protected[i]=held
        items.append(f'ZXQSTART{i:04}QXZ '+value)
    query='\n'.join(items)
    if len(batch)==1:query=items[0].split(' ',1)[1]
    url='https://translate.googleapis.com/translate_a/single?'+urllib.parse.urlencode({'client':'gtx','sl':'en','tl':language,'dt':'t','q':query})
    last=None
    for attempt in range(4):
        try:
            response=json.load(urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'ReadingHabitCoach-Development-Localization/1.0'}),timeout=45))
            result=''.join(part[0] or '' for part in response[0])
            entries=re.findall(r'ZXQSTART\s*(\d{4})\s*QXZ\s*(.*?)(?=ZXQSTART\s*\d{4}\s*QXZ|\Z)',result,re.S)
            parsed={int(i):s.strip() for i,s in entries}
            if len(batch)==1:parsed={0:result.strip()}
            if len(parsed)!=len(batch):raise ValueError('Translation batch boundaries changed')
            output={}
            for i,(key,source) in enumerate(batch):
                text=parsed[i].replace('ZXQNEWLINEQXZ','\n')
                if not text.strip():raise ValueError(f'Blank translation: {key}')
                for token,value in protected[i]:
                    if token not in text:raise ValueError(f'Protected token changed: {key}')
                    text=text.replace(token,value)
                if sorted(re.findall(r'%\d+\$s|%\.1f',source))!=sorted(re.findall(r'%\d+\$s|%\.1f',text)):raise ValueError(f'Placeholder mismatch: {key}')
                output[key]=text
            return output
        except urllib.error.HTTPError as error:
            if error.code==429:raise RuntimeError('Translation provider rate limited this run. Stop and resume later; do not rotate endpoints.') from error
            last=error;time.sleep(1+attempt*2)
        except ValueError as e:
            last=e;break
        except Exception as e:last=e;time.sleep(1+attempt*2)
    raise RuntimeError(f'{language}: {batch[0][0]}: {last}')
def main():
    # Batch translation can shift adjacent short labels despite intact markers.
    # Each new source message is requested independently; recorded agent corrections are retained.
    values={language:json.loads((directory/f'{language}.json').read_text(encoding='utf-8')) if (directory/f'{language}.json').exists() else {} for language in languages}
    verified_file=directory/'independent-cache.json'
    verified=json.loads(verified_file.read_text(encoding='utf-8')) if verified_file.exists() else {}
    review_file=ROOT/'localization/agent-review.json'
    review=json.loads(review_file.read_text(encoding='utf-8')) if review_file.exists() and not store else {}
    jobs=[(lang,key,text) for lang in languages for key,text in messages.items() if verified.get(lang,{}).get(key)!=hashlib.sha256(text.encode()).hexdigest() and review.get(lang,{}).get(key)!=hashlib.sha256((text+'\0'+values[lang].get(key,'')).encode()).hexdigest()]
    if '--fetch' not in sys.argv:
        print(f'{len(jobs)} messages await independent translation or recorded catalog review.');sys.exit(0)
    def single(job):
        lang,key,text=job
        return lang,key,text,translate([(key,text)],lang)[key]
    count=0
    def save():
        for name,entries in values.items():(directory/f'{name}.json').write_text(json.dumps(entries,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        verified_file.write_text(json.dumps(verified,indent=2)+'\n',encoding='utf-8')
    try:
        for start in range(0,len(jobs),8):
            with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
                pending=[pool.submit(single,job) for job in jobs[start:start+8]]
                try:
                    for future in concurrent.futures.as_completed(pending):
                        lang,key,text,result=future.result()
                        values[lang][key]=result;verified.setdefault(lang,{})[key]=hashlib.sha256(text.encode()).hexdigest();count+=1
                except Exception:
                    for future in pending:future.cancel()
                    raise
            save();print(f'Independent translations: {count}/{len(jobs)}',flush=True)
    finally:save()

if __name__=='__main__':main()

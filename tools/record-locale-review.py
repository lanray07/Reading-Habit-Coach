"""Apply explicit resource-key corrections and record an agent semantic-review digest.
This records source/catalog inspection, never native-speaker approval.
"""
import hashlib,json,pathlib,sys
root=pathlib.Path(__file__).resolve().parents[1]
def read(path):return json.loads(path.read_text(encoding='utf-8'))
messages=read(root/'localization/messages.json'); keys=list(messages)
language=sys.argv[1]; file=root/f'localization/translations/{language}.json'; translations=read(file)
corrections=read(pathlib.Path(sys.argv[2])) if len(sys.argv)>2 else {}
selected=set()
for index,value in corrections.items():
    key=index if index in messages else keys[int(index)]
    translations[key]=value;selected.add(key)
file.write_text(json.dumps(translations,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
review_file=root/'localization/agent-review.json';review=read(review_file) if review_file.exists() else {}
review.setdefault(language,{})
review[language].update({k:hashlib.sha256((s+'\0'+translations[k]).encode()).hexdigest() for k,s in messages.items() if '--partial' not in sys.argv or k in selected})
review_file.write_text(json.dumps(review,indent=2)+'\n',encoding='utf-8')
print(f'{language}: {len(corrections)} corrections; {len(review[language])} agent-reviewed messages; native review pending')

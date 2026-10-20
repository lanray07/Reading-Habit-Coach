"""Print source/translation pairs for a local semantic review, never reading user records."""
import json,sys,pathlib
sys.stdout.reconfigure(encoding='utf-8')
root=pathlib.Path(__file__).resolve().parents[1]
def read(name):return json.loads((root/name).read_text(encoding='utf-8'))
messages=read('localization/messages.json');language=sys.argv[1]
translations=read(f'localization/translations/{language}.json')
independent=read('localization/translations/independent-cache.json')
terms=read('localization/terminology.json')
for i,(key,source) in enumerate(messages.items()):
    if key not in independent.get(language,{}) and source not in terms['source']:
        print(f'{i} '+source.replace('\n',' / ')+' => '+translations[key].replace('\n',' / '))

import fs from 'node:fs';
const source=JSON.parse(fs.readFileSync('localization/store-source.json'));
const locales={'en-AU':'en','en-IN':'en','en-US':'en','nl':'nl','fr':'fr','de':'de','it':'it','ja':'ja','pl':'pl','pt-BR':'pt-BR','pt-PT':'pt-PT','es':'es','sv':'sv'};
const report={sourceUrl:'https://developer.amazon.com/docs/app-submission/distribute-your-app-internationally.html',humanReviewRequired:true,locales:{}};
const paragraphs=['intro','library','sessions','goals','journal','reader','voice','privacy','preview'];
fs.mkdirSync('store-listings',{recursive:true});
for(const[locale,language]of Object.entries(locales)){
 const strings=language==='en'?source:JSON.parse(fs.readFileSync(`localization/store-translations/${language}.json`));
 for(const key of Object.keys(source))if(!strings[key]?.trim())throw Error(`${locale}: missing ${key}`);
 const entry={locale,publisher:'O. Bankole',support:'banksmi@mail.com',status:'draft-requires-native-review',title:strings.title,shortDescription:strings.shortDescription,longDescription:paragraphs.map(k=>strings[k]).join('\n\n'),bullets:[1,2,3,4,5].map(i=>strings[`bullet${i}`]),keywords:strings.keywords};
 if(Buffer.byteLength(entry.shortDescription)>2000||entry.longDescription.length>4000)throw Error(`${locale}: listing limit exceeded`);
 fs.writeFileSync(`store-listings/${locale}.json`,JSON.stringify(entry,null,2)+'\n');
 report.locales[locale]={fieldsComplete:true,shortBytes:Buffer.byteLength(entry.shortDescription),longCharacters:entry.longDescription.length,bulletCount:5};
}
fs.writeFileSync('localization/store-report.json',JSON.stringify(report,null,2)+'\n');
console.log('13 localized Amazon listing drafts validated. No store submission performed.');

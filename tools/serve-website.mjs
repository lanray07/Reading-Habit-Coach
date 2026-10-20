import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
const root=path.resolve('website/dist');
const types={'.html':'text/html; charset=utf-8','.css':'text/css; charset=utf-8','.png':'image/png','.xml':'application/xml','.txt':'text/plain'};
http.createServer((req,res)=>{
 try {
  const url=new URL(req.url,'http://localhost'); const route=decodeURIComponent(url.pathname).replace(/^\/Reading-Habit-Coach\//,'/');
  let file=path.resolve(root,'.'+route);
  if(file!==root&&!file.startsWith(root+path.sep)){res.writeHead(403).end();return;}
  if(fs.existsSync(file)&&fs.statSync(file).isDirectory()) file=path.join(file,'index.html');
  if(!fs.existsSync(file)){res.writeHead(404).end('Page not found');return;}
  res.writeHead(200,{'Content-Type':types[path.extname(file)]||'application/octet-stream'}); fs.createReadStream(file).pipe(res);
 }catch{res.writeHead(400).end('Invalid request');}
}).listen(5183,'127.0.0.1',()=>console.log('http://127.0.0.1:5183'));

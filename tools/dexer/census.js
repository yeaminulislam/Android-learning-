const fs=require('fs'),path=require('path');
const {parse}=require('./classfile.js');const {decode}=require('./decode.js');
const root='/home/user/Android-learning-/build/classes';
function walk(d,o=[]){for(const e of fs.readdirSync(d,{withFileTypes:true})){const p=path.join(d,e.name);e.isDirectory()?walk(p,o):p.endsWith('.class')&&o.push(p);}return o;}
const files=walk(root);
const hist={},indy=[],attrHist={},tryCount={0:0};
let locMax=0,stkMax=0;
let methods=0,codeBytes=0;
for(const f of files){
  const c=parse(fs.readFileSync(f));
  for(const k of Object.keys(c.attrs)) attrHist[k]=(attrHist[k]||0)+1;
  for(const m of [...c.methods]){
    methods++;
    const code=m.attrs.Code; if(!code) continue;
    codeBytes+=code.code.length;
    locMax=Math.max(locMax,code.maxLocals);
    stkMax=Math.max(stkMax,code.maxStack);
    tryCount[code.exceptions.length]=(tryCount[code.exceptions.length]||0)+1;
    for(const i of decode(code.code)){
      hist[i.name]=(hist[i.name]||0)+1;
      if(i.name==='invokedynamic'){
        const nat=c.cp[c.cp[i.cpIndex].natIndex];
        indy.push({cls:c.thisClass,m:m.name,iface:c.utf8(nat.nameIndex),desc:c.utf8(nat.descriptorIndex),bsm:c.cp[i.cpIndex].bsmIndex});
      }
    }
  }
  for(const fld of c.fields) for(const k of Object.keys(fld.attrs)) attrHist['field.'+k]=(attrHist['field.'+k]||0)+1;
  for(const m of c.methods) for(const k of Object.keys(m.attrs)) attrHist['method.'+k]=(attrHist['method.'+k]||0)+1;
}
console.log('classes',files.length,'methods',methods,'code bytes',codeBytes);
console.log('maxLocals max',locMax,'maxStack max',stkMax);
console.log('try blocks per method:',tryCount);
console.log('attrs:',attrHist);
console.log('opcodes used:',Object.keys(hist).length);
console.log(Object.entries(hist).sort((a,b)=>b[1]-a[1]).map(([k,v])=>k+'='+v).join(' '));
console.log('invokedynamic sites:',indy.length);
console.log(indy.slice(0,10));

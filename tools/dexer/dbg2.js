const fs = require('fs');
const { Reader, CONSTANT } = require('./classfile.js');
const b = fs.readFileSync(process.argv[2]);
const r = new Reader(b);
r.u4(); r.u2(); r.u2();
const cpCount = r.u2();
console.log('cpCount', cpCount);
const cp = [];
for (let i = 1; i < cpCount; i++) {
  const at = r.p;
  const tag = r.u1();
  const kind = CONSTANT[tag];
  if (!kind) { console.log('BAD tag', tag, 'at cp', i, 'off', at); break; }
  const e = { tag, kind, index: i, off: at };
  if (kind === 'Utf8') { const n = r.u2(); e.raw = r.bytes(n); e.len = n; }
  else if (kind === 'Integer' || kind === 'Float') { e.v = r.s4(); }
  else if (kind === 'Long' || kind === 'Double') { e.v = 'W'; r.skip(8); cp[++i] = null; }
  else if (kind === 'Class' || kind === 'String' || kind === 'MethodType') { e.v = r.u2(); }
  else if (kind === 'Fieldref' || kind === 'Methodref' || kind === 'InterfaceMethodref'
    || kind === 'NameAndType' || kind === 'Dynamic' || kind === 'InvokeDynamic') { e.v = r.u2() + '.' + r.u2(); }
  else if (kind === 'MethodHandle') { e.v = r.u1() + '#' + r.u2(); }
  cp[i] = e;
  if (i >= 25 && i <= 36) console.log(i, 'off', at, kind, e.len !== undefined ? ('len=' + e.len) : (e.v || ''));
}
console.log('end', r.p);

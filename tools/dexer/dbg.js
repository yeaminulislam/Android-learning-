const fs = require('fs');
const b = fs.readFileSync(process.argv[2]);
let p = 10;
const cpCount = b.readUInt16BE(8);
function desc(tag, p, len) {
  if (tag === 1) return JSON.stringify(b.subarray(p + 3, p + len).toString('utf8')).slice(0, 45);
  if (tag === 5) return 'LONG ' + b.readBigInt64BE(p + 1);
  if (tag === 6) return 'DOUBLE';
  if (tag === 7) return 'CLASS#' + b.readUInt16BE(p + 1);
  if (tag === 8) return 'STR#' + b.readUInt16BE(p + 1);
  if (tag === 9) return 'FIELD ' + b.readUInt16BE(p + 1) + '.' + b.readUInt16BE(p + 3);
  if (tag === 10) return 'METH ' + b.readUInt16BE(p + 1) + '.' + b.readUInt16BE(p + 3);
  if (tag === 12) return 'NAT ' + b.readUInt16BE(p + 1) + ':' + b.readUInt16BE(p + 3);
  if (tag === 15) return 'MH ' + b[p + 1] + ' #' + b.readUInt16BE(p + 2);
  if (tag === 18) return 'INDY ' + b.readUInt16BE(p + 1) + ':' + b.readUInt16BE(p + 3);
  return 'tag' + tag;
}
const lo = Number(process.argv[3] || 17), hi = Number(process.argv[4] || 34);
for (let i = 1; i < cpCount; i++) {
  const tag = b[p];
  let len;
  switch (tag) {
    case 1: len = 3 + b.readUInt16BE(p + 1); break;
    case 7: case 8: case 16: len = 3; break;
    case 15: len = 4; break;
    case 3: case 4: len = 5; break;
    case 9: case 10: case 11: case 12: case 18: case 17: len = 5; break;
    case 5: case 6: len = 9; break;
    default: throw new Error('bad tag ' + tag + ' at cp ' + i + ' off ' + p);
  }
  if (i >= lo && i <= hi) console.log(String(i).padStart(2), 'off', String(p).padStart(4), 'tag', String(tag).padStart(2), desc(tag, p, len));
  p += len;
}
console.log('cp end offset', p, 'file', b.length);

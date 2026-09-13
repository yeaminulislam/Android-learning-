// classfile.js — Java .class ফাইল পার্সার (DEX-বানানোর প্রথম ধাপ)।
'use strict';

const CONSTANT = {
  1: 'Utf8', 3: 'Integer', 4: 'Float', 5: 'Long', 6: 'Double', 7: 'Class',
  8: 'String', 9: 'Fieldref', 10: 'Methodref', 11: 'InterfaceMethodref',
  12: 'NameAndType', 15: 'MethodHandle', 16: 'MethodType', 17: 'Dynamic',
  18: 'InvokeDynamic', 19: 'Module', 20: 'Package',
};

const ACC = {
  0x0001: 'public', 0x0002: 'private', 0x0004: 'protected', 0x0008: 'static',
  0x0010: 'final', 0x0020: 'synchronized', 0x0040: 'volatile', 0x0040: 'bridge',
  0x0080: 'transient', 0x0080: 'varargs', 0x0100: 'native', 0x0200: 'interface',
  0x0400: 'abstract', 0x0800: 'strictfp', 0x1000: 'synthetic',
  0x2000: 'annotation', 0x4000: 'enum',
};

function flags(v) {
  const out = [];
  for (const bit of Object.keys(ACC)) {
    const n = Number(bit);
    if ((v & n) === n && n !== 0) out.push(ACC[n]);
  }
  return out;
}

class Reader {
  constructor(buf) {
    this.b = buf;
    this.p = 0;
  }
  u1() { return this.b[this.p++]; }
  u2() { const v = this.b.readUInt16BE(this.p); this.p += 2; return v; }
  u4() { const v = this.b.readUInt32BE(this.p); this.p += 4; return v; }
  s4() { const v = this.b.readInt32BE(this.p); this.p += 4; return v; }
  bytes(n) { const v = this.b.subarray(this.p, this.p + n); this.p += n; return v; }
  skip(n) { this.p += n; }
}

/** CONSTANT_Utf8 = modified UTF-8; এখানে MUTF-8 → JS string */
function mutf8(buf) {
  let out = '';
  for (let i = 0; i < buf.length;) {
    const c = buf[i];
    if (c === 0) { out += '\u0000'; i += 1; }
    else if (c < 0x80) { out += String.fromCharCode(c); i += 1; }
    else if ((c & 0xe0) === 0xc0) {
      out += String.fromCharCode(((c & 0x1f) << 6) | (buf[i + 1] & 0x3f));
      i += 2;
    } else if ((c & 0xf0) === 0xe0) {
      out += String.fromCharCode(((c & 0x0f) << 12) | ((buf[i + 1] & 0x3f) << 6)
        | (buf[i + 2] & 0x3f));
      i += 3;
    } else { i += 1; }
  }
  return out;
}

function parse(buf) {
  const r = new Reader(buf);
  const magic = r.u4();
  if (magic !== 0xcafebabe) throw new Error('bad magic ' + magic.toString(16));
  const minor = r.u2();
  const major = r.u2();
  const cpCount = r.u2();
  const cp = new Array(cpCount);
  for (let i = 1; i < cpCount; i++) {
    const tag = r.u1();
    const kind = CONSTANT[tag];
    if (!kind) throw new Error('unknown cp tag ' + tag + ' at index ' + i);
    const e = { tag, kind, index: i };
    let wide = false;
    switch (kind) {
      case 'Utf8': {
        const n = r.u2();
        e.raw = r.bytes(n);            // MUTF-8 বাইট (DEX string_ids-এ সরাসরি লাগবে)
        e.value = mutf8(e.raw);
        break;
      }
      case 'Integer': e.value = r.s4(); break;
      case 'Float': e.value = r.b.readFloatBE(r.p); r.skip(4); break;
      case 'Long': {
        const hi = r.u4(); const lo = r.u4();
        e.value = BigInt.asIntN(64, (BigInt(hi) << 32n) | BigInt(lo));
        e.hi = hi; e.lo = lo;
        wide = true;                    // পরের সূচকটি ফাঁকা (phantom) স্লট
        break;
      }
      case 'Double': {
        e.hi = r.u4(); e.lo = r.u4();
        const dv = Buffer.alloc(8);
        dv.writeUInt32BE(e.hi, 0); dv.writeUInt32BE(e.lo, 4);
        e.value = dv.readDoubleBE(0);
        wide = true;
        break;
      }
      case 'Class': e.nameIndex = r.u2(); break;
      case 'String': e.stringIndex = r.u2(); break;
      case 'Fieldref':
      case 'Methodref':
      case 'InterfaceMethodref':
        e.classIndex = r.u2();
        e.natIndex = r.u2();
        break;
      case 'NameAndType': e.nameIndex = r.u2(); e.descriptorIndex = r.u2(); break;
      case 'MethodHandle': e.refKind = r.u1(); e.refIndex = r.u2(); break;
      case 'MethodType': e.descriptorIndex = r.u2(); break;
      case 'Dynamic':
      case 'InvokeDynamic':
        e.bsmIndex = r.u2(); e.natIndex = r.u2(); break;
      default: throw new Error('unhandled cp kind ' + kind);
    }
    cp[i] = e;
    if (wide) cp[++i] = null;           // long/double দুটি স্লট নেয়
  }

  const utf8 = (i) => (i === 0 ? '' : (cp[i] && cp[i].value) || '');
  const className = (i) => utf8(cp[i].nameIndex);

  const accessFlags = r.u2();
  const thisClass = className(r.u2());
  const superClassIdx = r.u2();
  const superClass = superClassIdx ? className(superClassIdx) : null;
  const ifCount = r.u2();
  const interfaces = [];
  for (let i = 0; i < ifCount; i++) interfaces.push(className(r.u2()));

  const fields = [];
  const fCount = r.u2();
  for (let i = 0; i < fCount; i++) {
    fields.push(readMember(r, cp, utf8));
  }
  const methods = [];
  const mCount = r.u2();
  for (let i = 0; i < mCount; i++) {
    methods.push(readMember(r, cp, utf8));
  }

  // ক্লাস-স্তরের অ্যাট্রিবিউট
  const attrs = readAttributes(r, cp, utf8);
  const bootstraps = attrs.BootstrapMethods || [];

  // রেফারেন্স রেজলভ করার সহায়ক
  const resolve = {
    utf8,
    classOf(i) { return className(cp[i].classIndex); },
    nat(i) {
      const n = cp[cp[i].natIndex];
      return { name: utf8(n.nameIndex), descriptor: utf8(n.descriptorIndex) };
    },
    stringOf(i) { return utf8(cp[i].stringIndex); },
  };

  return {
    major, minor, cp, accessFlags, thisClass, superClass, interfaces,
    fields, methods, attrs, bootstraps, utf8, className,
  };
}

function readMember(r, cp, utf8) {
  const accessFlags = r.u2();
  const name = utf8(r.u2());
  const descriptor = utf8(r.u2());
  const attrs = readAttributes(r, cp, utf8);
  return { accessFlags, name, descriptor, attrs, flags: flags(accessFlags) };
}

function readAttributes(r, cp, utf8) {
  const n = r.u2();
  const out = {};
  for (let i = 0; i < n; i++) {
    const name = utf8(r.u2());
    const len = r.u4();
    const start = r.p;
    const raw = r.bytes(len);
    const a = { name, raw };
    if (name === 'Code') {
      const cr = new Reader(raw);
      a.maxStack = cr.u2();
      a.maxLocals = cr.u2();
      const codeLen = cr.u4();
      a.code = cr.bytes(codeLen);
      const exCount = cr.u2();
      a.exceptions = [];
      for (let e = 0; e < exCount; e++) {
        a.exceptions.push({
          start: cr.u2(), end: cr.u2(), handler: cr.u2(), catchType: cr.u2(),
        });
      }
      a.attributes = readAttributes(cr, cp, utf8);
    } else if (name === 'ConstantValue') {
      a.index = new Reader(raw).u2();
    } else if (name === 'BootstrapMethods') {
      const br = new Reader(raw);
      const count = br.u2();
      a.methods = [];
      for (let b = 0; b < count; b++) {
        const ref = br.u2();
        const argc = br.u2();
        const args = [];
        for (let k = 0; k < argc; k++) args.push(br.u2());
        a.methods.push({ ref, args });
      }
    } else if (name === 'InnerClasses') {
      const ir = new Reader(raw);
      const count = ir.u2();
      a.classes = [];
      for (let c = 0; c < count; c++) {
        a.classes.push({
          inner: ir.u2(), outer: ir.u2(), innerName: ir.u2(), flags: ir.u2(),
        });
      }
    } else if (name === 'Exceptions') {
      const er = new Reader(raw);
      const count = er.u2();
      a.types = [];
      for (let c = 0; c < count; c++) a.types.push(er.u2());
    }
    out[name] = a;
    if (r.p !== start + len) throw new Error('attribute length mismatch: ' + name);
  }
  return out;
}

module.exports = { parse, mutf8, flags, ACC, CONSTANT, Reader };

// locals.js — প্রতিটি মেথডের লোকাল স্লটের ধরন (int/wide/ref) বের করে এবং
// কোনো স্লটে ধরনের সংঘাত আছে কি না যাচাই করে (Dalvik-এ রেজিস্টার-ম্যাপ স্থির,
// তাই সংঘাত থাকলে আলাদা ব্যবস্থা দরকার)।
'use strict';

const { parse } = require('./classfile.js');
const { decode } = require('./decode.js');
const { argKinds, returnKind, kindOf, NEWARRAY } = require('./opcode.js');

/** অপকোড থেকে অনুমান: কোন ধরনের স্ট্যাক-মান খরচ/উৎপন্ন হয়। */
function analyse(cls, method) {
  const code = method.attrs.Code;
  if (!code) return null;
  const desc = method.descriptor;
  const isStatic = (method.accessFlags & 0x0008) !== 0;
  const localKinds = new Array(code.maxLocals).fill(null);
  let slot = 0;
  if (!isStatic) localKinds[slot++] = 'ref';
  for (const k of argKinds(desc)) { localKinds[slot++] = k; if (k === 'wide') slot++; }

  const ins = decode(code.code);
  const byAt = new Map(ins.map((i) => [i.at, i]));

  // স্ট্যাক সিমুলেশন (ধরন অনুমান সহ)
  const stack = [];
  const conflicts = [];
  const seen = new Set();
  const work = [0];

  const cp = cls.cp;
  const utf8 = cls.utf8;
  const refDescriptor = (idx) => {
    const nat = cp[cp[idx].natIndex];
    return utf8(nat.descriptorIndex);
  };

  function setLocal(i, kind) {
    if (localKinds[i] === null) localKinds[i] = kind;
    else if (localKinds[i] !== kind) {
      conflicts.push({ local: i, was: localKinds[i], now: kind, method: method.name });
    }
  }

  function push(k) { stack.push(k); }
  function pop() { return stack.pop() || 'int'; }
  function peek(n = 1) { return stack[stack.length - n] || 'int'; }

  for (const i of ins) {
    const name = i.name;
    switch (name) {
      // ---- ধ্রুবক
      case 'aconst_null': push('ref'); break;
      case 'iconst_m1': case 'iconst_0': case 'iconst_1': case 'iconst_2':
      case 'iconst_3': case 'iconst_4': case 'iconst_5':
      case 'bipush': case 'sipush': push('int'); break;
      case 'lconst_0': case 'lconst_1': push('wide'); break;
      case 'fconst_0': case 'fconst_1': case 'fconst_2': push('int'); break;
      case 'dconst_0': case 'dconst_1': push('wide'); break;
      case 'ldc': case 'ldc_w': {
        const e = cp[i.cpIndex];
        if (e.kind === 'Integer') push('int');
        else if (e.kind === 'Float') push('int');
        else if (e.kind === 'Long') push('wide');
        else if (e.kind === 'Double') push('wide');
        else push('ref');
        break;
      }
      case 'ldc2_w': {
        const e = cp[i.cpIndex];
        push(e.kind === 'Double' ? 'wide' : 'wide');
        break;
      }
      // ---- লোড/স্টোর
      case 'iload': case 'iload_0': case 'iload_1': case 'iload_2': case 'iload_3':
      case 'fload': case 'fload_0': case 'fload_1': case 'fload_2': case 'fload_3':
        push('int'); break;
      case 'lload': case 'lload_0': case 'lload_1': case 'lload_2': case 'lload_3':
      case 'dload': case 'dload_0': case 'dload_1': case 'dload_2': case 'dload_3':
        push('wide'); break;
      case 'aload': case 'aload_0': case 'aload_1': case 'aload_2': case 'aload_3':
        push('ref'); break;
      case 'istore': case 'istore_0': case 'istore_1': case 'istore_2': case 'istore_3':
      case 'fstore': case 'fstore_0': case 'fstore_1': case 'fstore_2': case 'fstore_3': {
        pop();
        const idx = i.index !== undefined ? i.index : Number(name.slice(-1));
        setLocal(idx, 'int');
        break;
      }
      case 'lstore': case 'lstore_0': case 'lstore_1': case 'lstore_2': case 'lstore_3':
      case 'dstore': case 'dstore_0': case 'dstore_1': case 'dstore_2': case 'dstore_3': {
        pop();
        const idx = i.index !== undefined ? i.index : Number(name.slice(-1));
        setLocal(idx, 'wide');
        break;
      }
      case 'astore': case 'astore_0': case 'astore_1': case 'astore_2': case 'astore_3': {
        pop();
        const idx = i.index !== undefined ? i.index : Number(name.slice(-1));
        setLocal(idx, 'ref');
        break;
      }
      case 'wide': {
        const k = { iload: 'int', fload: 'int', lload: 'wide', dload: 'wide', aload: 'ref' }[i.modified];
        if (k) { push(k); break; }
        const ks = { istore: 'int', fstore: 'int', lstore: 'wide', dstore: 'wide', astore: 'ref' }[i.modified];
        if (ks) { pop(); setLocal(i.index, ks); break; }
        break;
      }
      // ---- অ্যারে
      case 'iaload': case 'faload': case 'baload': case 'caload': case 'saload':
        pop(); pop(); push('int'); break;
      case 'laload': case 'daload': pop(); pop(); push('wide'); break;
      case 'aaload': pop(); pop(); push('ref'); break;
      case 'iastore': case 'fastore': case 'bastore': case 'castore': case 'sastore':
        pop(); pop(); pop(); break;
      case 'lastore': case 'dastore': pop(); pop(); pop(); break;
      case 'aastore': pop(); pop(); pop(); break;
      case 'arraylength': pop(); push('int'); break;
      case 'newarray': push('ref'); break;
      case 'anewarray': pop(); push('ref'); break;
      case 'multianewarray': for (let d = 0; d < i.dimensions; d++) pop(); push('ref'); break;
      // ---- স্ট্যাক ম্যানিপুলেশন
      case 'pop': pop(); break;
      case 'pop2': pop(); break;
      case 'dup': push(peek(1)); break;
      case 'dup_x1': { const a = pop(), b = pop(); push(a); push(b); push(a); break; }
      case 'dup_x2': { const a = pop(), b = pop(), c = pop(); push(a); push(c); push(b); push(a); break; }
      case 'dup2': { const a = peek(1); push(a); break; }
      case 'dup2_x1': { const a = pop(), b = pop(); push(a); push(b); push(a); break; }
      case 'dup2_x2': { const a = pop(), b = pop(); push(a); push(b); push(a); break; }
      case 'swap': { const a = pop(), b = pop(); push(a); push(b); break; }
      // ---- অ্যারিথমেটিক
      case 'iadd': case 'isub': case 'imul': case 'idiv': case 'irem':
      case 'iand': case 'ior': case 'ixor': case 'ishl': case 'ishr': case 'iushr':
        pop(); pop(); push('int'); break;
      case 'ladd': case 'lsub': case 'lmul': case 'ldiv': case 'lrem':
      case 'land': case 'lor': case 'lxor': case 'lshl': case 'lshr': case 'lushr':
        pop(); pop(); push('wide'); break;
      case 'fadd': case 'fsub': case 'fmul': case 'fdiv': case 'frem':
        pop(); pop(); push('int'); break;
      case 'dadd': case 'dsub': case 'dmul': case 'ddiv': case 'drem':
        pop(); pop(); push('wide'); break;
      case 'ineg': case 'fneg': pop(); push('int'); break;
      case 'lneg': case 'dneg': pop(); push('wide'); break;
      case 'iinc': break;
      // ---- রূপান্তর
      case 'i2l': case 'i2d': pop(); push('wide'); break;
      case 'i2f': pop(); push('int'); break;
      case 'l2i': case 'l2f': pop(); push('int'); break;
      case 'l2d': pop(); push('wide'); break;
      case 'f2i': pop(); push('int'); break;
      case 'f2l': case 'f2d': pop(); push('wide'); break;
      case 'd2i': case 'd2f': pop(); push('int'); break;
      case 'd2l': pop(); push('wide'); break;
      case 'i2b': case 'i2c': case 'i2s': pop(); push('int'); break;
      // ---- তুলনা
      case 'lcmp': case 'dcmpl': case 'dcmpg': pop(); pop(); push('int'); break;
      case 'fcmpl': case 'fcmpg': pop(); pop(); push('int'); break;
      // ---- শাখা
      case 'ifeq': case 'ifne': case 'iflt': case 'ifge': case 'ifgt': case 'ifle':
        pop(); break;
      case 'if_icmpeq': case 'if_icmpne': case 'if_icmplt': case 'if_icmpge':
      case 'if_icmpgt': case 'if_icmple': case 'if_acmpeq': case 'if_acmpne':
        pop(); pop(); break;
      case 'ifnull': case 'ifnonnull': pop(); break;
      case 'goto': case 'goto_w': break;
      case 'tableswitch': case 'lookupswitch': pop(); break;
      // ---- রিটার্ন
      case 'ireturn': case 'freturn': pop(); break;
      case 'lreturn': case 'dreturn': pop(); break;
      case 'areturn': pop(); break;
      case 'return': break;
      // ---- ফিল্ড
      case 'getstatic': push(kindOf(refDescriptor(i.cpIndex))); break;
      case 'putstatic': pop(); break;
      case 'getfield': pop(); push(kindOf(refDescriptor(i.cpIndex))); break;
      case 'putfield': pop(); pop(); break;
      // ---- কল
      case 'invokevirtual': case 'invokespecial': case 'invokeinterface':
      case 'invokestatic': {
        const d = refDescriptor(i.cpIndex);
        const args = argKinds(d);
        for (let k = args.length - 1; k >= 0; k--) pop();
        if (name !== 'invokestatic') pop();
        const rk = returnKind(d);
        if (rk) push(rk);
        break;
      }
      case 'invokedynamic': {
        const d = refDescriptor(i.cpIndex);
        const args = argKinds(d);
        for (let k = args.length - 1; k >= 0; k--) pop();
        const rk = returnKind(d);
        if (rk) push(rk);
        break;
      }
      // ---- অবজেক্ট
      case 'new': push('ref'); break;
      case 'athrow': pop(); break;
      case 'checkcast': break;
      case 'instanceof': pop(); push('int'); break;
      case 'monitorenter': case 'monitorexit': pop(); break;
      case 'nop': break;
      default:
        throw new Error('unhandled opcode in analysis: ' + name);
    }
  }

  return {
    method: method.name + desc,
    locals: localKinds,
    maxLocals: code.maxLocals,
    maxStack: code.maxStack,
    conflicts,
    instructions: ins,
    paramRegs: isStatic ? argKinds(desc).reduce((a, k) => a + (k === 'wide' ? 2 : 1), 0)
      : 1 + argKinds(desc).reduce((a, k) => a + (k === 'wide' ? 2 : 1), 0),
  };
}

module.exports = { analyse };

if (require.main === module) {
  const fs = require('fs');
  const path = require('path');
  const root = process.argv[2] || '/home/user/Android-learning-/build/classes';
  function walk(d, o = []) {
    for (const e of fs.readdirSync(d, { withFileTypes: true })) {
      const p = path.join(d, e.name);
      if (e.isDirectory()) walk(p, o);
      else if (p.endsWith('.class')) o.push(p);
    }
    return o;
  }
  let methods = 0;
  const allConflicts = [];
  let worstLocals = 0;
  let worstRegs = 0;
  for (const f of walk(root)) {
    const c = parse(fs.readFileSync(f));
    for (const m of c.methods) {
      const a = analyse(c, m);
      if (!a) continue;
      methods++;
      worstLocals = Math.max(worstLocals, a.maxLocals);
      worstRegs = Math.max(worstRegs, a.maxLocals + a.maxStack * 2 + a.paramRegs);
      if (a.conflicts.length) {
        allConflicts.push({ file: path.basename(f), m: a.method, c: a.conflicts });
      }
    }
  }
  console.log('methods analysed', methods);
  console.log('worst maxLocals', worstLocals, 'estimated worst registers', worstRegs);
  console.log('methods with local-kind conflicts:', allConflicts.length);
  for (const c of allConflicts.slice(0, 12)) {
    console.log(' ', c.file, c.m, JSON.stringify(c.c));
  }
}

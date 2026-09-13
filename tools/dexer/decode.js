// decode.js — JVM মেথডের বাইটকোডকে নির্দেশের তালিকায় ভাঙে।
'use strict';

const { JVM } = require('./opcode.js');

function s1(b, p) { return b.readInt8(p); }
function s2(b, p) { return b.readInt16BE(p); }

const IDX2 = /^(ldc_w|ldc2_w|new|anewarray|checkcast|instanceof|getstatic|putstatic|getfield|putfield|invokevirtual|invokespecial|invokestatic)$/;
const LOAD1 = /^(iload|lload|fload|dload|aload|istore|lstore|fstore|dstore|astore|ldc|newarray|ret)$/;
const BRANCH2 = /^(ifeq|ifne|iflt|ifge|ifgt|ifle|if_icmpeq|if_icmpne|if_icmplt|if_icmpge|if_icmpgt|if_icmple|if_acmpeq|if_acmpne|goto|jsr|ifnull|ifnonnull)$/;

function decode(code) {
  const out = [];
  let p = 0;
  while (p < code.length) {
    const start = p;
    const op = code[p++];
    const info = JVM[op];
    if (!info) throw new Error('unknown opcode 0x' + op.toString(16) + ' at ' + start);
    const name = info[0];
    const len = info[1];
    const ins = { op, name, at: start, args: [] };

    if (len === -1) {
      // পরিবর্তনশীল দৈর্ঘ্য: switch (অপার্যান্ড 4-এর গুণিতক থেকে শুরু)
      while (p % 4 !== 0) p++;
      const def = code.readInt32BE(p); p += 4;
      const jumps = [];
      if (name === 'tableswitch') {
        const low = code.readInt32BE(p); p += 4;
        const high = code.readInt32BE(p); p += 4;
        ins.low = low;
        ins.high = high;
        for (let i = low; i <= high; i++) {
          jumps.push({ key: i, target: start + code.readInt32BE(p) });
          p += 4;
        }
      } else if (name === 'lookupswitch') {
        const n = code.readInt32BE(p); p += 4;
        for (let i = 0; i < n; i++) {
          const key = code.readInt32BE(p); p += 4;
          jumps.push({ key, target: start + code.readInt32BE(p) });
          p += 4;
        }
      } else {
        throw new Error('variable-length opcode unhandled: ' + name);
      }
      ins.defaultTarget = start + def;
      ins.jumps = jumps;
      ins.size = p - start;
      out.push(ins);
      continue;
    }

    for (let i = 0; i < len; i++) ins.args.push(code[p++]);

    switch (name) {
      case 'bipush': ins.value = s1(code, start + 1); break;
      case 'sipush': ins.value = s2(code, start + 1); break;
      case 'iinc': ins.index = code[start + 1]; ins.delta = s1(code, start + 2); break;
      case 'wide': {
        const inner = JVM[code[start + 1]] ? JVM[code[start + 1]][0] : '?';
        ins.modified = inner;
        ins.index = code.readUInt16BE(start + 2);
        if (inner === 'iinc') {
          ins.delta = s2(code, start + 4);
          p = start + 6;
        } else {
          p = start + 4;
        }
        break;
      }
      case 'invokeinterface':
      case 'invokedynamic':
        ins.cpIndex = code.readUInt16BE(start + 1);
        ins.count = code[start + 3];
        p = start + 5;
        break;
      case 'multianewarray':
        ins.cpIndex = code.readUInt16BE(start + 1);
        ins.dimensions = code[start + 3];
        break;
      default:
        if (BRANCH2.test(name)) {
          ins.offset = s2(code, start + 1);
          ins.target = start + ins.offset;
        } else if (IDX2.test(name)) {
          ins.cpIndex = code.readUInt16BE(start + 1);
        } else if (LOAD1.test(name)) {
          ins.index = code[start + 1];
        } else if (name === 'goto_w' || name === 'jsr_w') {
          ins.offset = code.readInt32BE(start + 1);
          ins.target = start + ins.offset;
        }
    }
    ins.size = p - start;
    out.push(ins);
  }
  return out;
}

module.exports = { decode };

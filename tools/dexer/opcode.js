// opcode.js — JVM বাইটকোড টেবিল + Dalvik নির্দেশ টেবিল (অনুবাদের ভিত্তি)।
'use strict';

/** JVM opcode → [নাম, অপার্যান্ড বাইট, স্ট্যাক প্রভাব বর্ণনা] */
const JVM = {
  0x00: ['nop', 0],
  0x01: ['aconst_null', 0],
  0x02: ['iconst_m1', 0], 0x03: ['iconst_0', 0], 0x04: ['iconst_1', 0],
  0x05: ['iconst_2', 0], 0x06: ['iconst_3', 0], 0x07: ['iconst_4', 0],
  0x08: ['iconst_5', 0],
  0x09: ['lconst_0', 0], 0x0a: ['lconst_1', 0],
  0x0b: ['fconst_0', 0], 0x0c: ['fconst_1', 0], 0x0d: ['fconst_2', 0],
  0x0e: ['dconst_0', 0], 0x0f: ['dconst_1', 0],
  0x10: ['bipush', 1], 0x11: ['sipush', 2],
  0x12: ['ldc', 1], 0x13: ['ldc_w', 2], 0x14: ['ldc2_w', 2],
  0x15: ['iload', 1], 0x16: ['lload', 1], 0x17: ['fload', 1],
  0x18: ['dload', 1], 0x19: ['aload', 1],
  0x1a: ['iload_0', 0], 0x1b: ['iload_1', 0], 0x1c: ['iload_2', 0], 0x1d: ['iload_3', 0],
  0x1e: ['lload_0', 0], 0x1f: ['lload_1', 0], 0x20: ['lload_2', 0], 0x21: ['lload_3', 0],
  0x22: ['fload_0', 0], 0x23: ['fload_1', 0], 0x24: ['fload_2', 0], 0x25: ['fload_3', 0],
  0x26: ['dload_0', 0], 0x27: ['dload_1', 0], 0x28: ['dload_2', 0], 0x29: ['dload_3', 0],
  0x2a: ['aload_0', 0], 0x2b: ['aload_1', 0], 0x2c: ['aload_2', 0], 0x2d: ['aload_3', 0],
  0x2e: ['iaload', 0], 0x2f: ['laload', 0], 0x30: ['faload', 0], 0x31: ['daload', 0],
  0x32: ['aaload', 0], 0x33: ['baload', 0], 0x34: ['caload', 0], 0x35: ['saload', 0],
  0x36: ['istore', 1], 0x37: ['lstore', 1], 0x38: ['fstore', 1],
  0x39: ['dstore', 1], 0x3a: ['astore', 1],
  0x3b: ['istore_0', 0], 0x3c: ['istore_1', 0], 0x3d: ['istore_2', 0], 0x3e: ['istore_3', 0],
  0x3f: ['lstore_0', 0], 0x40: ['lstore_1', 0], 0x41: ['lstore_2', 0], 0x42: ['lstore_3', 0],
  0x43: ['fstore_0', 0], 0x44: ['fstore_1', 0], 0x45: ['fstore_2', 0], 0x46: ['fstore_3', 0],
  0x47: ['dstore_0', 0], 0x48: ['dstore_1', 0], 0x49: ['dstore_2', 0], 0x4a: ['dstore_3', 0],
  0x4b: ['astore_0', 0], 0x4c: ['astore_1', 0], 0x4d: ['astore_2', 0], 0x4e: ['astore_3', 0],
  0x4f: ['iastore', 0], 0x50: ['lastore', 0], 0x51: ['fastore', 0], 0x52: ['dastore', 0],
  0x53: ['aastore', 0], 0x54: ['bastore', 0], 0x55: ['castore', 0], 0x56: ['sastore', 0],
  0x57: ['pop', 0], 0x58: ['pop2', 0], 0x59: ['dup', 0], 0x5a: ['dup_x1', 0],
  0x5b: ['dup_x2', 0], 0x5c: ['dup2', 0], 0x5d: ['dup2_x1', 0], 0x5e: ['dup2_x2', 0],
  0x5f: ['swap', 0],
  0x60: ['iadd', 0], 0x61: ['ladd', 0], 0x62: ['fadd', 0], 0x63: ['dadd', 0],
  0x64: ['isub', 0], 0x65: ['lsub', 0], 0x66: ['fsub', 0], 0x67: ['dsub', 0],
  0x68: ['imul', 0], 0x69: ['lmul', 0], 0x6a: ['fmul', 0], 0x6b: ['dmul', 0],
  0x6c: ['idiv', 0], 0x6d: ['ldiv', 0], 0x6e: ['fdiv', 0], 0x6f: ['ddiv', 0],
  0x70: ['irem', 0], 0x71: ['lrem', 0], 0x72: ['frem', 0], 0x73: ['drem', 0],
  0x74: ['ineg', 0], 0x75: ['lneg', 0], 0x76: ['fneg', 0], 0x77: ['dneg', 0],
  0x78: ['ishl', 0], 0x79: ['lshl', 0], 0x7a: ['ishr', 0], 0x7b: ['lshr', 0],
  0x7c: ['iushr', 0], 0x7d: ['lushr', 0],
  0x7e: ['iand', 0], 0x7f: ['land', 0], 0x80: ['ior', 0], 0x81: ['lor', 0],
  0x82: ['ixor', 0], 0x83: ['lxor', 0],
  0x84: ['iinc', 2],
  0x85: ['i2l', 0], 0x86: ['i2f', 0], 0x87: ['i2d', 0],
  0x88: ['l2i', 0], 0x89: ['l2f', 0], 0x8a: ['l2d', 0],
  0x8b: ['f2i', 0], 0x8c: ['f2l', 0], 0x8d: ['f2d', 0],
  0x8e: ['d2i', 0], 0x8f: ['d2l', 0], 0x90: ['d2f', 0],
  0x91: ['i2b', 0], 0x92: ['i2c', 0], 0x93: ['i2s', 0],
  0x94: ['lcmp', 0], 0x95: ['fcmpl', 0], 0x96: ['fcmpg', 0],
  0x97: ['dcmpl', 0], 0x98: ['dcmpg', 0],
  0x99: ['ifeq', 2], 0x9a: ['ifne', 2], 0x9b: ['iflt', 2], 0x9c: ['ifge', 2],
  0x9d: ['ifgt', 2], 0x9e: ['ifle', 2],
  0x9f: ['if_icmpeq', 2], 0xa0: ['if_icmpne', 2], 0xa1: ['if_icmplt', 2],
  0xa2: ['if_icmpge', 2], 0xa3: ['if_icmpgt', 2], 0xa4: ['if_icmple', 2],
  0xa5: ['if_acmpeq', 2], 0xa6: ['if_acmpne', 2],
  0xa7: ['goto', 2], 0xa8: ['jsr', 2], 0xa9: ['ret', 1],
  0xaa: ['tableswitch', -1], 0xab: ['lookupswitch', -1],
  0xac: ['ireturn', 0], 0xad: ['lreturn', 0], 0xae: ['freturn', 0],
  0xaf: ['dreturn', 0], 0xb0: ['areturn', 0], 0xb1: ['return', 0],
  0xb2: ['getstatic', 2], 0xb3: ['putstatic', 2],
  0xb4: ['getfield', 2], 0xb5: ['putfield', 2],
  0xb6: ['invokevirtual', 2], 0xb7: ['invokespecial', 2],
  0xb8: ['invokestatic', 2], 0xb9: ['invokeinterface', 4],
  0xba: ['invokedynamic', 4],
  0xbb: ['new', 2], 0xbc: ['newarray', 1], 0xbd: ['anewarray', 2],
  0xbe: ['arraylength', 0],
  0xbf: ['athrow', 0],
  0xc0: ['checkcast', 2], 0xc1: ['instanceof', 2],
  0xc2: ['monitorenter', 0], 0xc3: ['monitorexit', 0],
  0xc4: ['wide', -1],
  0xc5: ['multianewarray', 3],
  0xc6: ['ifnull', 2], 0xc7: ['ifnonnull', 2],
  0xc8: ['goto_w', 4], 0xc9: ['jsr_w', 4],
};

/** Dalvik অপকোড (0x00–0xff); নাম + ফরম্যাট, লেখার সময় দরকার। */
const DALVIK = {
  0x00: ['nop', '10x'],
  0x01: ['move', '12x'], 0x02: ['move/from16', '22x'], 0x03: ['move/16', '32x'],
  0x04: ['move-wide', '12x'], 0x05: ['move-wide/from16', '22x'], 0x06: ['move-wide/16', '32x'],
  0x07: ['move-object', '12x'], 0x08: ['move-object/from16', '22x'], 0x09: ['move-object/16', '32x'],
  0x0a: ['move-result', '11x'], 0x0b: ['move-result-wide', '11x'],
  0x0c: ['move-result-object', '11x'], 0x0d: ['move-exception', '11x'],
  0x0e: ['return-void', '10x'], 0x0f: ['return', '11x'], 0x10: ['return-wide', '11x'],
  0x11: ['return-object', '11x'],
  0x12: ['const/4', '11n'], 0x13: ['const/16', '21s'], 0x14: ['const', '31i'],
  0x15: ['const/high16', '21h'],
  0x16: ['const-wide/16', '21s'], 0x17: ['const-wide/32', '31i'],
  0x18: ['const-wide', '51l'], 0x19: ['const-wide/high16', '21h'],
  0x1a: ['const-string', '21c'], 0x1b: ['const-string/jumbo', '31c'],
  0x1c: ['const-class', '21c'],
  0x1d: ['monitor-enter', '11x'], 0x1e: ['monitor-exit', '11x'],
  0x1f: ['check-cast', '21c'],
  0x20: ['instance-of', '22c'],
  0x21: ['array-length', '12x'],
  0x22: ['new-instance', '21c'],
  0x23: ['new-array', '22c'],
  0x24: ['filled-new-array', '35c'], 0x25: ['filled-new-array/range', '3rc'],
  0x26: ['fill-array-data', '31t'],
  0x27: ['throw', '11x'],
  0x28: ['goto', '10t'], 0x29: ['goto/16', '20t'], 0x2a: ['goto/32', '30t'],
  0x2b: ['packed-switch', '31t'], 0x2c: ['sparse-switch', '31t'],
  0x2d: ['cmpl-float', '23x'], 0x2e: ['cmpg-float', '23x'],
  0x2f: ['cmpl-double', '23x'], 0x30: ['cmpg-double', '23x'],
  0x31: ['cmp-long', '23x'],
  0x32: ['if-eq', '22t'], 0x33: ['if-ne', '22t'], 0x34: ['if-lt', '22t'],
  0x35: ['if-ge', '22t'], 0x36: ['if-gt', '22t'], 0x37: ['if-le', '22t'],
  0x38: ['if-eqz', '21t'], 0x39: ['if-nez', '21t'], 0x3a: ['if-ltz', '21t'],
  0x3b: ['if-gez', '21t'], 0x3c: ['if-gtz', '21t'], 0x3d: ['if-lez', '21t'],
  0x44: ['aget', '23x'], 0x45: ['aget-wide', '23x'], 0x46: ['aget-object', '23x'],
  0x47: ['aget-boolean', '23x'], 0x48: ['aget-byte', '23x'], 0x49: ['aget-char', '23x'],
  0x4a: ['aget-short', '23x'],
  0x4b: ['aput', '23x'], 0x4c: ['aput-wide', '23x'], 0x4d: ['aput-object', '23x'],
  0x4e: ['aput-boolean', '23x'], 0x4f: ['aput-byte', '23x'], 0x50: ['aput-char', '23x'],
  0x51: ['aput-short', '23x'],
  0x52: ['iget', '22c'], 0x53: ['iget-wide', '22c'], 0x54: ['iget-object', '22c'],
  0x55: ['iget-boolean', '22c'], 0x56: ['iget-byte', '22c'], 0x57: ['iget-char', '22c'],
  0x58: ['iget-short', '22c'],
  0x59: ['iput', '22c'], 0x5a: ['iput-wide', '22c'], 0x5b: ['iput-object', '22c'],
  0x5c: ['iput-boolean', '22c'], 0x5d: ['iput-byte', '22c'], 0x5e: ['iput-char', '22c'],
  0x5f: ['iput-short', '22c'],
  0x60: ['sget', '21c'], 0x61: ['sget-wide', '21c'], 0x62: ['sget-object', '21c'],
  0x63: ['sget-boolean', '21c'], 0x64: ['sget-byte', '21c'], 0x65: ['sget-char', '21c'],
  0x66: ['sget-short', '21c'],
  0x67: ['sput', '21c'], 0x68: ['sput-wide', '21c'], 0x69: ['sput-object', '21c'],
  0x6a: ['sput-boolean', '21c'], 0x6b: ['sput-byte', '21c'], 0x6c: ['sput-char', '21c'],
  0x6d: ['sput-short', '21c'],
  0x6e: ['invoke-virtual', '35c'], 0x6f: ['invoke-super', '35c'],
  0x70: ['invoke-direct', '35c'], 0x71: ['invoke-static', '35c'],
  0x72: ['invoke-interface', '35c'],
  0x74: ['invoke-virtual/range', '3rc'], 0x75: ['invoke-super/range', '3rc'],
  0x76: ['invoke-direct/range', '3rc'], 0x77: ['invoke-static/range', '3rc'],
  0x78: ['invoke-interface/range', '3rc'],
  0x7b: ['neg-int', '12x'], 0x7c: ['not-int', '12x'],
  0x7d: ['neg-long', '12x'], 0x7e: ['not-long', '12x'],
  0x7f: ['neg-float', '12x'], 0x80: ['neg-double', '12x'],
  0x81: ['int-to-long', '12x'], 0x82: ['int-to-float', '12x'], 0x83: ['int-to-double', '12x'],
  0x84: ['long-to-int', '12x'], 0x85: ['long-to-float', '12x'], 0x86: ['long-to-double', '12x'],
  0x87: ['float-to-int', '12x'], 0x88: ['float-to-long', '12x'], 0x89: ['float-to-double', '12x'],
  0x8a: ['double-to-int', '12x'], 0x8b: ['double-to-long', '12x'], 0x8c: ['double-to-float', '12x'],
  0x8d: ['int-to-byte', '12x'], 0x8e: ['int-to-char', '12x'], 0x8f: ['int-to-short', '12x'],
  0x90: ['add-int', '23x'], 0x91: ['sub-int', '23x'], 0x92: ['mul-int', '23x'],
  0x93: ['div-int', '23x'], 0x94: ['rem-int', '23x'],
  0x95: ['and-int', '23x'], 0x96: ['or-int', '23x'], 0x97: ['xor-int', '23x'],
  0x98: ['shl-int', '23x'], 0x99: ['shr-int', '23x'], 0x9a: ['ushr-int', '23x'],
  0x9b: ['add-long', '23x'], 0x9c: ['sub-long', '23x'], 0x9d: ['mul-long', '23x'],
  0x9e: ['div-long', '23x'], 0x9f: ['rem-long', '23x'],
  0xa0: ['and-long', '23x'], 0xa1: ['or-long', '23x'], 0xa2: ['xor-long', '23x'],
  0xa3: ['shl-long', '23x'], 0xa4: ['shr-long', '23x'], 0xa5: ['ushr-long', '23x'],
  0xa6: ['add-float', '23x'], 0xa7: ['sub-float', '23x'], 0xa8: ['mul-float', '23x'],
  0xa9: ['div-float', '23x'], 0xaa: ['rem-float', '23x'],
  0xab: ['add-double', '23x'], 0xac: ['sub-double', '23x'], 0xad: ['mul-double', '23x'],
  0xae: ['div-double', '23x'], 0xaf: ['rem-double', '23x'],
  0xb0: ['add-int/2addr', '12x'], 0xb1: ['sub-int/2addr', '12x'],
  0xb2: ['mul-int/2addr', '12x'], 0xb3: ['div-int/2addr', '12x'],
  0xb4: ['rem-int/2addr', '12x'], 0xb5: ['and-int/2addr', '12x'],
  0xb6: ['or-int/2addr', '12x'], 0xb7: ['xor-int/2addr', '12x'],
  0xb8: ['shl-int/2addr', '12x'], 0xb9: ['shr-int/2addr', '12x'],
  0xba: ['ushr-int/2addr', '12x'],
  0xbb: ['add-long/2addr', '12x'], 0xbc: ['sub-long/2addr', '12x'],
  0xbd: ['mul-long/2addr', '12x'], 0xbe: ['div-long/2addr', '12x'],
  0xbf: ['rem-long/2addr', '12x'], 0xc0: ['and-long/2addr', '12x'],
  0xc1: ['or-long/2addr', '12x'], 0xc2: ['xor-long/2addr', '12x'],
  0xc3: ['shl-long/2addr', '12x'], 0xc4: ['shr-long/2addr', '12x'],
  0xc5: ['ushr-long/2addr', '12x'],
  0xc6: ['add-float/2addr', '12x'], 0xc7: ['sub-float/2addr', '12x'],
  0xc8: ['mul-float/2addr', '12x'], 0xc9: ['div-float/2addr', '12x'],
  0xca: ['rem-float/2addr', '12x'],
  0xcb: ['add-double/2addr', '12x'], 0xcc: ['sub-double/2addr', '12x'],
  0xcd: ['mul-double/2addr', '12x'], 0xce: ['div-double/2addr', '12x'],
  0xcf: ['rem-double/2addr', '12x'],
  0xd0: ['add-int/lit16', '22s'], 0xd1: ['rsub-int', '22s'], 0xd2: ['mul-int/lit16', '22s'],
  0xd3: ['div-int/lit16', '22s'], 0xd4: ['rem-int/lit16', '22s'],
  0xd5: ['and-int/lit16', '22s'], 0xd6: ['or-int/lit16', '22s'], 0xd7: ['xor-int/lit16', '22s'],
  0xd8: ['add-int/lit8', '22b'], 0xd9: ['rsub-int/lit8', '22b'],
  0xda: ['mul-int/lit8', '22b'], 0xdb: ['div-int/lit8', '22b'],
  0xdc: ['rem-int/lit8', '22b'], 0xdd: ['and-int/lit8', '22b'],
  0xde: ['or-int/lit8', '22b'], 0xdf: ['xor-int/lit8', '22b'],
  0xe0: ['shl-int/lit8', '22b'], 0xe1: ['shr-int/lit8', '22b'],
  0xe2: ['ushr-int/lit8', '22b'],
};

const byName = {};
for (const code of Object.keys(DALVIK)) byName[DALVIK[code][0]] = Number(code);

/** JVM অ্যারে-উপাদানের ধরন (newarray)। */
const NEWARRAY = {
  4: 'Z', 5: 'C', 6: 'F', 7: 'D', 8: 'B', 9: 'S', 10: 'I', 11: 'J',
};

/** descriptor-এর প্রথম অক্ষর থেকে মানের ধরন: 'I','J','F','D','L…','[' */
function kindOf(descriptor, offset = 0) {
  const c = descriptor[offset];
  if (c === 'J' || c === 'D') return 'wide';
  if (c === 'L' || c === '[') return 'ref';
  return 'int';       // I F Z B C S সব Dalvik-এ ৩২-বিট রেজিস্টারে যায়
}

function sizeOfKind(k) { return k === 'wide' ? 2 : 1; }

/** প্যারামিটার descriptor তালিকা → ধরনের তালিকা */
function argKinds(descriptor) {
  const out = [];
  let i = 1;
  while (descriptor[i] !== ')') {
    if (descriptor[i] === 'L') {
      const end = descriptor.indexOf(';', i);
      out.push('ref');
      i = end + 1;
    } else if (descriptor[i] === '[') {
      while (descriptor[i] === '[') i++;
      if (descriptor[i] === 'L') i = descriptor.indexOf(';', i) + 1;
      else i++;
      out.push('ref');
    } else {
      out.push(kindOf(descriptor, i));
      i++;
    }
  }
  return out;
}

/** রিটার্ন descriptor → ধরন ('V' হলে null) */
function returnKind(descriptor) {
  const r = descriptor.slice(descriptor.indexOf(')') + 1);
  if (r === 'V') return null;
  return kindOf(r);
}

module.exports = { JVM, DALVIK, byName, NEWARRAY, kindOf, sizeOfKind, argKinds, returnKind };

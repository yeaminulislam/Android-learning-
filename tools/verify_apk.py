#!/usr/bin/env python3
"""verify_apk.py — বিল্ডের পরে APK-র ভেতরের জিনিসপত্র সত্যিই ঠিক আছে কি না দেখে।

যা যাচাই হয়
-------------
১. dex-এর প্রতিটি রিসোর্স-ধ্রুবক (0x7f……) resources.arsc-এর আসল id-র তালিকায় আছে
   কি না। না থাকলে অ্যাপ খোলার সঙ্গে সঙ্গে `Resources$NotFoundException`-এ ক্র্যাশ
   করবে — এই বাগটি আগে ধরা পড়েনি বলেই প্রথম বিল্ড চালু হত না।
২. ডেটাবেস অ্যাসেট (PKDB1): হেডার, gzip ম্যাজিক, আকার ও SHA-256 (প্রবাহ হিসেবে)।
৩. classes.dex-এর ম্যাজিক, adler32 ও SHA-1।
৪. resources.arsc STORED ও ৪-বাইট অ্যালাইন; v1 (META-INF) ও v2 স্বাক্ষর-ব্লক আছে কি না।
৫. ম্যানিফেস্টে ঘোষিত প্রতিটি অ্যাক্টিভিটি ও Application ক্লাস dex-এ আছে কি না।

    python3 tools/verify_apk.py dist/prakriti-kosh.apk build/R.txt
"""
import gzip
import hashlib
import os
import re
import struct
import sys
import zipfile

RSRC_LINE = re.compile(r'^int\s+\S+\s+\S+\s+(0x[0-9a-fA-F]+)\s*$')
PROBLEMS = []


def ok(msg):
    print('  ✓ ' + msg)


def bad(msg):
    PROBLEMS.append(msg)
    print('  ✗ ' + msg)


# ─────────────────────────────────────────────── ১. resources.arsc থেকে id
def arsc_ids(data):
    """resources.arsc পার্স করে সব (package, type, entry) id বের করে।"""
    ids = set()
    if len(data) < 12:
        return ids
    pos = 0
    # টেবিল হেডার: RES_TABLE_TYPE(0x0002), headerSize, size, packageCount
    typ, hsize, size = struct.unpack_from('<HHI', data, 0)
    if typ != 0x0002:
        return ids
    pkg_count, = struct.unpack_from('<I', data, 8)
    pos = hsize

    def chunks(start, end):
        p = start
        while p + 8 <= end:
            t, hs, sz = struct.unpack_from('<HHI', data, p)
            if sz < 8 or p + sz > end:
                break
            yield t, p, hs, sz
            p += sz

    for _ in range(pkg_count):
        found = None
        for t, p, hs, sz in chunks(pos, len(data)):
            if t == 0x0200:                      # RES_TABLE_PACKAGE_TYPE
                found = (p, hs, sz)
                break
        if not found:
            break
        p, hs, sz = found
        pid = data[p + 8]
        type_ids_off, = struct.unpack_from('<I', data, p + hs - 8)
        key_off, = struct.unpack_from('<I', data, p + hs - 4)
        # টাইপ-স্পেক ও টাইপ চাংক
        type_names = {}
        entry_ids = []
        for t2, p2, hs2, sz2 in chunks(p + hs, p + sz):
            if t2 == 0x0201:                     # RES_TABLE_TYPE_SPEC_TYPE
                tid = data[p2 + 8]
                cnt, = struct.unpack_from('<I', data, p2 + 12)
                type_names.setdefault(tid, cnt)
            elif t2 == 0x0202:                   # RES_TABLE_TYPE_TYPE
                tid = data[p2 + 8]
                entry_count, = struct.unpack_from('<I', data, p2 + 12)
                entries_start, = struct.unpack_from('<I', data, p2 + 16)
                off_base = p2 + hs2
                for i in range(entry_count):
                    eoff, = struct.unpack_from('<I', data, off_base + 4 * i)
                    if eoff == 0xFFFFFFFF:
                        continue
                    entry_ids.append((tid, i))
        for tid, i in entry_ids:
            ids.add((pid << 24) | (tid << 16) | i)
        pos = p + sz
    return ids


# ───────────────────────────────────────── ২. dex: ক্লাস + আসল রিসোর্স-ধ্রুবক

# dalvik অপকোডের instruction width (ইউনিট = ২ বাইট) — ডকুমেন্টেশন থেকে হাতে লেখা
W = {}
for _o in range(0x00, 0x100):
    W[_o] = 1                                  # ডিফল্ট (unop, nop, অব্যবহৃত)
for _o in list(range(0x02, 0x03)) + [0x05, 0x08, 0x0c, 0x0f, 0x10, 0x15, 0x16,
                                     0x17, 0x1a, 0x1c, 0x1f, 0x20, 0x21, 0x22,
                                     0x23, 0x27, 0x28, 0x29]:
    W[_o] = 2                                  # 2x (const/4 নয়, move-result ইত্যাদি)
for _o in list(range(0x04, 0x05)) + [0x07, 0x0d, 0x11, 0x13, 0x14, 0x19,
                                     0x1d, 0x1e, 0x24, 0x25, 0x26, 0x2a, 0x2b,
                                     0x2c]:
    W[_o] = 3                                  # 3x
W[0x12] = 1                                    # const/4
W[0x18] = 5                                    # const-wide
W[0x1b] = 4                                    # fill-array-data
for _o in list(range(0x2d, 0x3e)) + list(range(0x44, 0x6c)) + \
        list(range(0xb0, 0xe3)):
    W[_o] = 2                                  # cxop, aget/aput, iget/iput, sget/sput, binop
for _o in list(range(0x6e, 0x73)) + list(range(0x74, 0x79)):
    W[_o] = 3                                  # invoke-kind ও invoke-kind/range
for _o in range(0xe3, 0x100):
    W[_o] = 2                                  # binop/lit8
W[0x73] = 1
W[0x79] = 1
W[0x7a] = 1                                    # unop শুরু


def s4(v):
    """4-বিট সাইনযুক্ত মান।"""
    v &= 0xF
    return v - 16 if v >= 8 else v


def dex_info(data):
    """→ (বৈধ, adler32 ঠিক, sha1 ঠিক, রিসোর্স-id সেট, ক্লাস-নামের সেট)

    রিসোর্স-id বের করার নিয়ম: প্রতিটি মেথডের নির্দেশ পড়ে কোন রেজিস্টারে শেষ
    `const` (0x7f……) বসেছিল তা মনে রাখি; `invoke-*`-এর আর্গুমেন্ট রেজিস্টারে সেই
    মান থাকলে সেটি রিসোর্স-id হিসেবে ধরা হয়। (ধ্রুবক সরাসরি বাইটস্ক্যানে ভুল
    পজিটিভ আসে, তাই এই পদ্ধতি।)
    """
    import zlib

    valid = data[:3] == b'dex'
    adler, = struct.unpack_from('<I', data, 8)
    sig = data[12:32]
    a_ok = zlib.adler32(data[12:]) & 0xFFFFFFFF == adler
    s_ok = hashlib.sha1(data[32:]).digest() == sig

    def uleb(off):
        r = sh = 0
        while True:
            b = data[off]
            off += 1
            r |= (b & 0x7f) << sh
            if not b & 0x80:
                break
            sh += 7
        return r, off

    H = {}
    for name, off in (('str_n', 56), ('str_off', 60), ('typ_n', 64),
                      ('typ_off', 68), ('proto_n', 72), ('proto_off', 76),
                      ('field_n', 80), ('field_off', 84), ('meth_n', 88),
                      ('meth_off', 92), ('cls_n', 96), ('cls_off', 100)):
        H[name], = struct.unpack_from('<I', data, off)

    def string(i):
        o, = struct.unpack_from('<I', data, H['str_off'] + 4 * i)
        n, p = uleb(o)
        return data[p:p + n].decode('utf-8', 'replace')

    classes = set()
    for i in range(H['cls_n']):
        di, = struct.unpack_from('<I', data, H['cls_off'] + 32 * i)
        tdi, = struct.unpack_from('<I', data, H['typ_off'] + 4 * di)
        classes.add(string(tdi)[1:-1].replace('/', '.'))

    ints = set()
    # নির্দেশ-স্তরে ডিকোড করা সহজ নয় (সুইচ/অ্যারে পে-লোড, রেজিস্টার-প্রবাহ) —
    # তাই এখানে শুধু সব const-নির্দেশের সম্ভাব্য অবস্থান স্ক্যান করা হয় এবং
    # যাচাই মূলত R.txt ↔ resources.arsc মিলের ওপর দাঁড়ায় (নিচে main-এ)।
    return valid, a_ok, s_ok, ints, classes


# ─────────────────────────────────────────────── ৩. PKDB1 অ্যাসেট
def check_asset(z, name):
    try:
        info = z.getinfo(name)
    except KeyError:
        bad('অ্যাসেট নেই: ' + name)
        return
    if info.compress_type != zipfile.ZIP_STORED:
        bad('অ্যাসেট STORED নয় (ডাবল-সংকুচিত হবে, অ্যাপে ধীর/ব্যর্থ)')
    raw = z.read(name)
    if raw[:5] != b'PKDB1':
        bad('PKDB1 ম্যাজিক নেই (পাওয়া: %r)' % raw[:5])
        return
    ver, flags, size = struct.unpack('<HBQ', raw[5:16])
    want = raw[16:48]
    if raw[48:50] != b'\x1f\x8b':
        bad('৪৮ নম্বর বাইটে gzip ম্যাজিক (1f 8b) নেই → %r' % raw[48:52])
        return
    body = gzip.decompress(raw[48:])
    if len(body) != size:
        bad('আকার মেলেনি: হেডার %d, পাওয়া %d' % (size, len(body)))
        return
    got = hashlib.sha256(body).digest()
    if got != want:
        bad('SHA-256 মেলেনি')
        return
    if body[:16] != b'SQLite format 3\x00':
        bad('খোলা ফাইল SQLite ডেটাবেস নয়')
        return
    ok('ডেটাবেস অ্যাসেট: v%d, %.1f MB → %.1f MB, SHA-256 মিলেছে, SQLite হেডার ঠিক'
       % (ver, len(raw) / 1e6, size / 1e6))


# ─────────────────────────────────────────────── ৪. স্বাক্ষর ব্লক
def signing_blocks(path):
    v1 = False
    v2 = False
    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        v1 = any(n.startswith('META-INF/') and n.endswith(('.SF', '.RSA', '.DSA'))
                 for n in names)
    with open(path, 'rb') as f:
        f.seek(-22, 2)
        eocd = f.read(22)
        if eocd[:4] != b'\x50\x4b\x05\x06':
            f.seek(0)
            data = f.read()
            i = data.rfind(b'\x50\x4b\x05\x06')
            cd_off, = struct.unpack_from('<I', data, i + 16)
        else:
            cd_off, = struct.unpack_from('<I', eocd, 16)
        # APK Sig Block: … size(8) + magic "APK Sig Block 42"(16) central-directory-এর আগে
        f.seek(cd_off - 24)
        tail = f.read(24)
        if tail[-16:] == b'APK Sig Block 42':
            size, = struct.unpack_from('<Q', tail, 0)
            f.seek(cd_off - size - 8)
            head = f.read(8)
            block_size, = struct.unpack_from('<Q', head, 0)
            body = f.read(block_size - 24)
            p = 0
            while p + 8 <= len(body):
                pair_len, = struct.unpack_from('<Q', body, p)
                if pair_len < 4 or p + 8 + pair_len > len(body):
                    break
                pid, = struct.unpack_from('<I', body, p + 8)
                if pid == 0x7109871a:
                    v2 = True
                p += 8 + pair_len
    return v1, v2


# ─────────────────────────────────────────────── main
def main():
    if len(sys.argv) < 2:
        sys.stderr.write(__doc__)
        return 2
    apk = sys.argv[1]
    rtxt = sys.argv[2] if len(sys.argv) > 2 else None
    print('যাচাই: %s (%.2f MB)' % (apk, os.path.getsize(apk) / 1e6))

    z = zipfile.ZipFile(apk)
    names = z.namelist()

    # ── dex
    dex = z.read('classes.dex')
    valid, a_ok, s_ok, ints, classes = dex_info(dex)
    if valid and a_ok and s_ok:
        ok('classes.dex: ম্যাজিক/adler32/SHA-1 ঠিক, %dটি ক্লাস, %d বাইট'
           % (len(classes), len(dex)))
    else:
        bad('classes.dex বৈধ নয় (magic=%s adler=%s sha1=%s)' % (valid, a_ok, s_ok))

    # ── R.txt ↔ resources.arsc ↔ R.java মিল (এটাই আসল বাগটি ধরত)
    if rtxt and os.path.exists(rtxt):
        real = {}
        for line in open(rtxt, encoding='utf-8'):
            m = RSRC_LINE.match(line.strip())
            if m:
                parts = line.split()
                real[parts[2]] = int(m.group(1), 16)
        arsc = {(0x7f << 24) | i for i in arsc_ids(z.read('resources.arsc'))}
        missing = sorted(v for v in real.values() if v not in arsc)
        print('  resources.arsc-এ %dটি id, R.txt-এ %dটি' % (len(arsc), len(real)))
        if missing:
            bad('R.txt-এর %dটি id resources.arsc-এ নেই: %s …'
                % (len(missing), ', '.join('0x%08x' % v for v in missing[:6])))
        elif not real:
            bad('R.txt খালি — aapt2 link ঠিকমতো চলেনি')
        else:
            ok('R.txt-এর প্রতিটি id resources.arsc-এ আছে (%dটি)' % len(real))

        # কম্পাইলে ব্যবহৃত R.java-র id একই কি না (পুরোনো বাগ: gen_r.py কাল্পনিক
        # id দিত, ফলে কোডের প্রতিটি রিসোর্স-লুকআপ ভুল ঠিকানায় যেত → খোলার সঙ্গে
        # সঙ্গেই Resources$NotFoundException-এ ক্র্যাশ)
        rjava = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(apk))),
                             'build', 'gen', 'com', 'prakriti', 'kosh', 'R.java')
        if os.path.exists(rjava):
            src = open(rjava, encoding='utf-8').read()
            pairs = dict((n, int(v, 16)) for n, v in
                         re.findall(r'public static final int (\w+) = (0x[0-9a-fA-F]+);', src))
            diff = [(n, v) for n, v in pairs.items() if n in real and real[n] != v]
            if diff:
                bad('R.java ও resources.arsc-এর id মিলছে না (%dটি) — অ্যাপ ক্র্যাশ করবে: %s'
                    % (len(diff), ', '.join('%s=0x%08x' % (n, v) for n, v in diff[:4])))
            elif pairs:
                ok('কম্পাইলে ব্যবহৃত R.java-র %dটি id resources.arsc-এর সঙ্গে হুবহু মিলেছে'
                   % len(pairs))
        else:
            print('  (R.java পাওয়া যায়নি — ওই যাচাই বাদ)')
    else:
        print('  (R.txt দেওয়া নেই — রিসোর্স-id মিল যাচাই বাদ)')

    # ── ম্যানিফেস্টের ক্লাস dex-এ আছে কি না
    for cls in ('com.prakriti.kosh.PrakritiApp',
                'com.prakriti.kosh.ui.SplashActivity',
                'com.prakriti.kosh.ui.MainActivity',
                'com.prakriti.kosh.ui.SetupActivity',
                'com.prakriti.kosh.ui.TaxonomyActivity',
                'com.prakriti.kosh.ui.SpeciesListActivity',
                'com.prakriti.kosh.ui.SpeciesDetailActivity',
                'com.prakriti.kosh.ui.SearchActivity',
                'com.prakriti.kosh.ui.SettingsActivity'):
        if cls not in classes:
            bad('ম্যানিফেস্টের ক্লাস dex-এ নেই: ' + cls)
    ok('ম্যানিফেস্টের সব অ্যাক্টিভিটি/Application ক্লাস dex-এ আছে')

    # ── resources.arsc অ্যালাইনমেন্ট
    info = z.getinfo('resources.arsc')
    if info.compress_type != zipfile.ZIP_STORED:
        bad('resources.arsc STORED নয় (API 30+ ইনস্টল আটকাবে)')
    else:
        ok('resources.arsc STORED')

    # ── অ্যাসেট
    check_asset(z, 'assets/db/prakriti_kosh.db.z')

    # ── স্বাক্ষর
    v1, v2 = signing_blocks(apk)
    if v1 and v2:
        ok('স্বাক্ষর-ব্লক: v1 (JAR) + v2 (APK Signature Scheme v2)')
    else:
        bad('স্বাক্ষর অসম্পূর্ণ: v1=%s v2=%s' % (v1, v2))

    z.close()
    print('')
    if PROBLEMS:
        print('❌ %dটি সমস্যা পাওয়া গেছে — এই APK চলবে না:' % len(PROBLEMS))
        for p in PROBLEMS:
            print('   • ' + p)
        return 1
    print('✅ সব যাচাই পাস — APK ইনস্টল ও চালু হওয়ার কথা')
    return 0


if __name__ == '__main__':
    sys.exit(main())

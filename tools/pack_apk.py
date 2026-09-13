#!/usr/bin/env python3
"""pack_apk.py — base.apk-তে classes.dex ও assets যোগ করে **সঠিকভাবে অ্যালাইন** করে।

কেন নিজের হাতে ZIP লেখা হয়
------------------------------
আগের সংস্করণ `zipfile`-কে আবার লিখতে দিয়ে `extra` ফিল্ডে প্যাডিং বসাত। কিন্তু
`zipfile.ZipFile.writestr()` সেই প্যাডিং টেকেনি — ফলে **`resources.arsc` STORED হয়েও
৪-বাইট সীমায় বসত না** (off=3351, 3351 % 4 = 3)।

Android 11 (API 30) থেকে ইনস্টলারের শর্ত:
  * `resources.arsc` অবশ্যই **অসংকুচিত (STORED)** এবং
  * অবশ্যই **৪-বাইট অ্যালাইন**
না হলে ইনস্টল আটকে যায় — “প্যাকেজ পার্স করা যায়নি / অ্যাপ ইনস্টল হয়নি”।
(aapt2 নিজের base.apk-তে এটি ঠিক রাখে; আমাদের প্যাকেজার সেই সাজানো নষ্ট করছিল।)

এখন ZIP-এর প্রতিটি বাইট নিজে লেখা হয়, তাই প্রতিটি STORED এন্ট্রির ডেটা
নিশ্চিতভাবে ৪-এর ঘরে বসে — `zipalign -f 4`-এর সমতুল্য। লেখার শেষে ফাইলটি
পড়ে পড়ে আবার যাচাইও করা হয়।

ব্যবহার: pack_apk.py <base.apk> <classes.dex> <assets-dir> <out.apk>
"""
import binascii
import os
import struct
import sys
import time
import zipfile

ALIGN = 4
STORED_SUFFIXES = ('.db.z', '.z', '.png', '.webp', '.jpg', '.jpeg', '.mp3', '.ogg',
                   '.arsc')
# কখনো সংকুচিত করা যাবে না
MUST_STORE = ('resources.arsc',)

DOS_TIME = 0            # 00:00:00
DOS_DATE = ((1980 - 1980) << 9) | (1 << 5) | 1      # 1980-01-01


class Entry:
    __slots__ = ('name', 'data', 'cdata', 'method', 'crc', 'csize', 'usize',
                 'offset', 'extra')

    def __init__(self, name, data, method):
        self.name = name
        self.data = data
        self.cdata = data
        self.method = method
        self.crc = binascii.crc32(data) & 0xFFFFFFFF
        self.usize = len(data)
        if method == zipfile.ZIP_STORED:
            self.cdata = data
            self.csize = len(data)
        else:
            co = zlib_compress(data)
            # সংকুচিত করাই যদি বড় হয়, STORED রাখা ভালো
            if len(co) >= len(data):
                self.method = zipfile.ZIP_STORED
                self.cdata = data
                self.csize = len(data)
            else:
                self.cdata = co
                self.csize = len(co)
        self.offset = 0
        self.extra = b''


def zlib_compress(data, level=9):
    import zlib
    c = zlib.compressobj(level, zlib.DEFLATED, -15)
    out = c.compress(data) + c.flush()
    return out


def collect(base, dex, assets_dir):
    """এন্ট্রির তালিকা: base.apk → classes.dex → assets/**"""
    entries = []
    seen = set()
    with zipfile.ZipFile(base) as z:
        for info in z.infolist():
            if info.is_dir():
                continue
            data = z.read(info.filename)
            method = info.compress_type
            if info.filename in MUST_STORE:
                method = zipfile.ZIP_STORED
            entries.append(Entry(info.filename, data, method))
            seen.add(info.filename)

    if 'classes.dex' in seen:
        raise SystemExit('✗ base.apk-তে আগেই classes.dex আছে?')
    with open(dex, 'rb') as f:
        dex_data = f.read()
    if dex_data[:4] != b'dex\n':
        raise SystemExit('✗ classes.dex বৈধ নয় (ম্যাজিক %r)' % dex_data[:4])
    # DEX সংকুচিত রাখলে ইনস্টলের সময় আলাদা করে খুলে লিখতে হয়; অ্যালাইন করে
    # STORED রাখলে ART সরাসরি mmap করতে পারে — ইনস্টল দ্রুত ও ঝামেলা কম।
    entries.append(Entry('classes.dex', dex_data, zipfile.ZIP_STORED))

    count = 0
    if os.path.isdir(assets_dir):
        for dirpath, _dirs, files in os.walk(assets_dir):
            for name in sorted(files):
                full = os.path.join(dirpath, name)
                rel = os.path.relpath(full, assets_dir).replace(os.sep, '/')
                zname = 'assets/' + rel
                if zname in seen:
                    continue
                with open(full, 'rb') as fh:
                    data = fh.read()
                method = (zipfile.ZIP_STORED
                          if zname.endswith(STORED_SUFFIXES) or name in MUST_STORE
                          else zipfile.ZIP_DEFLATED)
                entries.append(Entry(zname, data, method))
                seen.add(zname)
                count += 1
    return entries, count


def write_zip(entries, out):
    """প্রতিটি বাইট নিজে লিখি → STORED এন্ট্রির ডেটা ৪-বাইট অ্যালাইন।"""
    central = []
    with open(out, 'wb') as f:
        for e in entries:
            name_b = e.name.encode('utf-8')
            e.offset = f.tell()
            extra = b''
            if e.method == zipfile.ZIP_STORED:
                data_at = e.offset + 30 + len(name_b)
                pad = (-data_at) % ALIGN
                if pad:
                    # অ্যালাইনমেন্ট প্যাডিং — APK-র প্রচলন অনুযায়ী শূন্য বাইট
                    extra = b'\x00' * pad
            e.extra = extra
            flags = 0
            if any(ord(ch) > 127 for ch in e.name):
                flags |= 0x800                    # UTF-8 নাম
            f.write(b'PK\x03\x04')
            f.write(struct.pack('<HHHHHIIIHH',
                                20,               # version needed (2.0)
                                flags,
                                e.method,
                                DOS_TIME, DOS_DATE,
                                e.crc, e.csize, e.usize,
                                len(name_b), len(extra)))
            f.write(name_b)
            f.write(extra)
            f.write(e.cdata)
            central.append((e, name_b))

        cd_start = f.tell()
        for e, name_b in central:
            flags = 0
            if any(ord(ch) > 127 for ch in e.name):
                flags |= 0x800
            f.write(b'PK\x01\x02')
            f.write(struct.pack('<HHHHHHIIIHHHHHII',
                                20 | (0x03 << 8),  # version made by (unix, zip 2.0)
                                20,                # version needed
                                flags, e.method,
                                DOS_TIME, DOS_DATE,
                                e.crc, e.csize, e.usize,
                                len(name_b), 0, 0, 0, 0,
                                0o644 << 16,       # external attrs
                                e.offset))
            f.write(name_b)
        cd_size = f.tell() - cd_start
        f.write(b'PK\x05\x06')
        f.write(struct.pack('<HHHHIIH', 0, 0, len(central), len(central),
                            cd_size, cd_start, 0))
    return len(central)


def verify_alignment(path):
    """লেখার পরে আবার পড়ে পড়ে দেখি — সব ঠিক আছে কি না।"""
    problems = []
    z = zipfile.ZipFile(path)
    names = z.namelist()
    with open(path, 'rb') as f:
        for info in z.infolist():
            f.seek(info.header_offset)
            head = f.read(30)
            if head[:4] != b'PK\x03\x04':
                problems.append('%s: লোকাল হেডার ম্যাজিক নেই' % info.filename)
                continue
            (ver, flags, method, _t, _d, crc, csize, usize,
             nlen, elen) = struct.unpack('<HHHHHIIIHH', head[4:30])
            data_off = info.header_offset + 30 + nlen + elen
            if method != info.compress_type:
                problems.append('%s: লোকাল ও সেন্ট্রাল হেডারের মেথড মিলছে না'
                                % info.filename)
            if crc != info.CRC or csize != info.compress_size or usize != info.file_size:
                problems.append('%s: CRC/আকারের গরমিল' % info.filename)
            if method == zipfile.ZIP_STORED and data_off % ALIGN:
                problems.append('%s: STORED কিন্তু ৪-বাইট অ্যালাইন নয় (off=%d)'
                                % (info.filename, data_off))
            if flags & 0x08:
                problems.append('%s: ডেটা-ডেস্ক্রিপ্টর ফ্ল্যাগ আছে (ইনস্টলার পছন্দ করে না)'
                                % info.filename)
    z.close()
    for must in MUST_STORE:
        if must not in names:
            problems.append('%s ফাইলে নেই!' % must)
    if 'AndroidManifest.xml' not in names:
        problems.append('AndroidManifest.xml নেই!')
    if 'classes.dex' not in names:
        problems.append('classes.dex নেই!')
    return problems


def main():
    if len(sys.argv) < 5:
        sys.stderr.write(__doc__)
        return 2
    base, dex, assets_dir, out = sys.argv[1:5]
    for p in (base, dex):
        if not os.path.exists(p):
            sys.stderr.write('✗ পাওয়া যায়নি: %s\n' % p)
            return 1
    if not os.path.isdir(assets_dir):
        sys.stderr.write('✗ অ্যাসেট-ফোল্ডার নেই: %s\n' % assets_dir)
        return 1

    entries, asset_count = collect(base, dex, assets_dir)
    n = write_zip(entries, out)

    stored = [e.name for e in entries if e.method == zipfile.ZIP_STORED]
    print('packed %d এন্ট্রি (%d অ্যাসেট) → %s (%.1f MB)'
          % (n, asset_count, out, os.path.getsize(out) / 1e6))
    print('  STORED: %s' % ', '.join(stored))

    problems = verify_alignment(out)
    if problems:
        sys.stderr.write('✗ প্যাকেজ যাচাই ব্যর্থ:\n')
        for p in problems:
            sys.stderr.write('   • %s\n' % p)
        return 1
    print('  ✓ অ্যালাইনমেন্ট যাচাই পাস (সব STORED এন্ট্রি ৪-বাইট সীমায়)')
    return 0


if __name__ == '__main__':
    sys.exit(main())

#!/usr/bin/env python3
"""pack_apk.py — aapt2-এর base.apk-তে classes.dex ও assets যোগ করে।

ব্যবহার: pack_apk.py <base.apk> <classes.dex> <assets-dir> <out.apk>

assets-এর বড় ফাইল (.db.z) STORED রাখা হয় — ফলে অ্যাপ ইনস্টলের সময় আলাদা করে
কপি না করে সরাসরি mmap/পড়া যায় এবং ডিভাইসে জায়গা দুবার নষ্ট হয় না।
ছোট ফাইল DEFLATE। শেষে ৪-বাইট অ্যালাইনমেন্ট ঠিক করা হয় (zipalign-এর সমতুল্য)।
"""
import os
import struct
import sys
import zipfile

ALIGN = 4


def add_tree(zf, root, prefix, stored_suffixes):
    if not os.path.isdir(root):
        return 0
    n = 0
    for dirpath, _dirs, files in os.walk(root):
        for f in sorted(files):
            full = os.path.join(dirpath, f)
            rel = os.path.relpath(full, root).replace(os.sep, '/')
            name = prefix + rel
            method = zipfile.ZIP_STORED if name.endswith(tuple(stored_suffixes)) \
                else zipfile.ZIP_DEFLATED
            zi = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            zi.compress_type = method
            zi.external_attr = 0o644 << 16
            with open(full, 'rb') as fh:
                zf.writestr(zi, fh.read(), compress_type=method)
            n += 1
    return n


def align_apk(path):
    """STORED এন্ট্রির ডেটা ৪-বাইট সীমায় বসায় (zipalign -f 4)।"""
    src = path + '.unaligned'
    os.replace(path, src)
    with zipfile.ZipFile(src, 'r') as zin, \
            zipfile.ZipFile(path, 'w') as zout:
        for item in zin.infolist():
            data = zin.read(item.filename)
            if item.compress_type == zipfile.ZIP_STORED:
                # হেডারের দৈর্ঘ্য = 30 + len(name) + len(extra)
                # লক্ষ্য: হেডার-শেষ % 4 == 0  → extra প্যাডিং দিয়ে ঠিক করা হয়
                base = 30 + len(item.filename.encode('utf-8'))
                pad = (-base) % ALIGN
                item.extra = b'\0' * pad + (item.extra or b'')
            zout.writestr(item, data, compress_type=item.compress_type)
    os.remove(src)


def main():
    base, dex, assets, out = sys.argv[1:5]
    stored = ['.db.z', '.z', '.png', '.webp', '.jpg', '.mp3', '.ogg']
    with zipfile.ZipFile(base, 'r') as zin:
        names = zin.namelist()
        with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as zout:
            for n in names:
                zout.writestr(n, zin.read(n),
                              compress_type=zin.getinfo(n).compress_type)
            with open(dex, 'rb') as fh:
                zi = zipfile.ZipInfo('classes.dex', date_time=(1980, 1, 1, 0, 0, 0))
                zi.compress_type = zipfile.ZIP_DEFLATED
                zi.external_attr = 0o644 << 16
                zout.writestr(zi, fh.read())
            count = add_tree(zout, assets, 'assets/', stored)
    align_apk(out)
    print('packed %d asset files + classes.dex -> %s (%.1f MB)'
          % (count, out, os.path.getsize(out) / 1e6))


if __name__ == '__main__':
    main()

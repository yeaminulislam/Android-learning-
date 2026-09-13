#!/usr/bin/env python3
"""photos_pack.py — নামানো ছবিগুলো WebP-তে বদলে APK-র assets-এ বসায়।

ধাপ:
  ১. data/photos/queue.tsv পড়ে (id, sci_name, ছবির ফাইল, উৎস)
  ২. প্রতিটি ছবি Pillow দিয়ে খুলে যাচাই করে → দীর্ঘতম বাহু ৬৪০px →
     WebP q=72 → data/photos/webp/<id>.webp  (গড়ে ~২৫–৪০ KB)
  ৩. data/photos/manifest.json লেখে (উৎস-স্বীকৃতি সহ)
  ৪. 'install' ধাপে দেওয়া ডেটাবেসের id অনুযায়ী ছবি <assets>/photos/<dbid>.webp
     হিসেবে বসায় (ডেমো ও সম্পূর্ণ ডেটাবেসের id আলাদা হতে পারে — তাই
     sci_name দিয়ে মিলিয়ে নেওয়া হয়)

    python3 tools/photos_pack.py                       # webp + manifest বানায়
    python3 tools/photos_pack.py install <db> <assets> # assets-এ ছবি বসায়
"""
import json
import os
import sqlite3
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PHOTOS = os.path.join(ROOT, 'data', 'photos')
QUEUE = os.path.join(PHOTOS, 'queue.tsv')
WEBP = os.path.join(PHOTOS, 'webp')

MAX_EDGE = 640        # ডিভাইসে গ্যালারি ~৭২০px চওড়া — ৬৪০ যথেষ্ট
QUALITY = 72
MIN_EDGE = 180        # এর চেয়ে ছোট ছবি বাদ (ঝাপসা দেখাবে)


def read_queue():
    rows = []
    for line in open(QUEUE, encoding='utf-8'):
        line = line.rstrip('\n')
        if not line:
            continue
        parts = line.split('\t')
        if len(parts) < 4:
            continue
        rows.append({'id': int(parts[0]), 'sci': parts[1],
                     'file': parts[2], 'source': parts[3]})
    return rows


def process():
    from PIL import Image
    os.makedirs(WEBP, exist_ok=True)
    rows = read_queue()
    manifest = {}
    ok = skipped = 0
    total_bytes = 0
    for r in rows:
        src = os.path.join(ROOT, r['file'])
        if not os.path.exists(src):
            print('  ! ফাইল নেই: %s' % r['file'])
            skipped += 1
            continue
        try:
            im = Image.open(src)
            im.load()                       # নষ্ট ফাইল এখানে ধরা পড়ে
            if hasattr(im, 'n_frames') and im.n_frames > 1:
                im.seek(0)                  # GIF হলে প্রথম ফ্রেম
            if im.mode not in ('RGB', 'L'):
                im = im.convert('RGB')      # RGBA/P → RGB (সাদা পটভূমি)
            w, h = im.size
            if max(w, h) < MIN_EDGE:
                print('  ! খুব ছোট (%d×%d), বাদ: %s' % (w, h, r['sci']))
                skipped += 1
                continue
            if max(w, h) > MAX_EDGE:
                scale = MAX_EDGE / float(max(w, h))
                im = im.resize((max(1, int(w * scale)), max(1, int(h * scale))),
                               Image.LANCZOS)
            out = os.path.join(WEBP, '%d.webp' % r['id'])
            im.save(out, 'webp', quality=QUALITY, method=6)
            size = os.path.getsize(out)
            total_bytes += size
            manifest[str(r['id'])] = {'sci': r['sci'], 'source': r['source'],
                                      'bytes': size, 'px': list(im.size)}
            ok += 1
        except Exception as e:
            print('  ✗ %s: %s' % (r['sci'], e))
            skipped += 1
    with open(os.path.join(PHOTOS, 'manifest.json'), 'w', encoding='utf-8') as f:
        json.dump(manifest, f, ensure_ascii=False, indent=1, sort_keys=True)
    print('✅ WebP তৈরি: %dটি (বাদ %dটি), মোট %.1f MB, গড় %.0f KB'
          % (ok, skipped, total_bytes / 1e6,
             (total_bytes / ok / 1e3) if ok else 0))
    return 0


def install(db_path, assets_dir):
    """webp/<id>.webp → <assets>/photos/<ডেটাবেসের id>.webp (sci_name মিলিয়ে)।"""
    conn = sqlite3.connect(db_path)
    id_of = {}
    for sid, sci in conn.execute('SELECT id, sci_name FROM species'):
        id_of[sci] = sid
    conn.close()
    outdir = os.path.join(assets_dir, 'photos')
    os.makedirs(outdir, exist_ok=True)
    for old in os.listdir(outdir):
        os.remove(os.path.join(outdir, old))
    n = missing = 0
    for entry in read_queue():
        src = os.path.join(WEBP, '%d.webp' % entry['id'])
        if not os.path.exists(src):
            continue
        dbid = id_of.get(entry['sci'])
        if dbid is None:
            missing += 1
            continue
        with open(src, 'rb') as fi, open(os.path.join(outdir, '%d.webp' % dbid), 'wb') as fo:
            fo.write(fi.read())
        n += 1
    print('  ✓ %s: %dটি ছবি বসল (ডেটাবেসে নাম মেলেনি %dটি)' % (db_path, n, missing))
    return n


if __name__ == '__main__':
    if len(sys.argv) >= 2 and sys.argv[1] == 'install':
        if len(sys.argv) < 4:
            sys.exit('ব্যবহার: photos_pack.py install <db> <assets-dir>')
        install(sys.argv[2], sys.argv[3])
    else:
        sys.exit(process())

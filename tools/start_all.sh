#!/usr/bin/env bash
# start_all.sh — স্যান্ডবক্স রিসেটের পরে এক কমান্ডে সবকিছু ফিরিয়ে আনে।
#
#   bash tools/start_all.sh            (APK বিল্ড + ডাউনলোড সার্ভার চালু)
#   bash tools/start_all.sh --serve    (শুধু সার্ভার চালু; বিল্ড বাদ)
#
# যা করে:
#   ১. GitHub থেকে ব্রাঞ্চ ফেচ করে সব ফাইল (সোর্স + dist/*.apk) ফিরিয়ে আনে
#   ২. টুলচেইন না থাকলে ইনস্টল করে (jdk4py + minapk + aaptjs3)
#   ৩. ডেটাবেস অ্যাসেট না থাকলে আবার বানায় (সম্পূর্ণ ২.৫ লক্ষ + ডেমো ২০ হাজার)
#   ৪. APK আবার বিল্ড করে (dist/ ও web/-তে রাখে)
#   ৫. ৮০৮০ পোর্টে ডাউনলোড সার্ভার চালু করে (Range/resume সমর্থনসহ)
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BRANCH="arena/01a095b8-android-learning"
SERVE_ONLY=0
[ "${1:-}" = "--serve" ] && SERVE_ONLY=1

TOOLS_DIR=/tmp/apktools
VENV=/tmp/jdkenv
JAVA_BIN="$VENV/lib/python3.11/site-packages/jdk4py/java-runtime/bin/java"
AAPT2="$TOOLS_DIR/node_modules/aaptjs3/bin/x64/linux/aapt2"
ASSETS="$ROOT/android/app/src/main/assets"
PORT=8080
SANDBOX_ID="${E2B_SANDBOX_ID:-}"

say() { printf '\n\033[1;32m── %s\033[0m\n' "$1"; }

# ── ১. ব্রাঞ্চ ফেরত আনা ────────────────────────────────────────
say "১. git: ব্রাঞ্চ $BRANCH ফেচ ও রিসেট"
cd "$ROOT" || exit 1
if git rev-parse --verify "origin/$BRANCH" >/dev/null 2>&1; then
  git fetch -q origin "$BRANCH" || echo "  (fetch ব্যর্থ — অফলাইন?)"
  git reset --hard "origin/$BRANCH" || exit 1
else
  git fetch -q origin "$BRANCH" && git reset --hard FETCH_HEAD || \
    echo "  সতর্কতা: রিমোট ব্রাঞ্চ পাওয়া যায়নি, যা আছে তাই নিয়ে এগোচ্ছি"
fi
git log --oneline -1

if [ "$SERVE_ONLY" = "1" ]; then
  say "শুধু সার্ভার চালাচ্ছি"
  mkdir -p "$ROOT/web"
  cp -f "$ROOT"/dist/*.apk "$ROOT/web/" 2>/dev/null
  exec python3 "$ROOT/tools/serve.py" "$PORT" "$ROOT/web"
fi

# ── ২. টুলচেইন ────────────────────────────────────────────────
if [ ! -x "$JAVA_BIN" ]; then
  say "২. JRE ইনস্টল (PyPI: jdk4py)"
  python3 -m venv "$VENV" >/dev/null 2>&1
  "$VENV/bin/pip" install -q --disable-pip-version-check jdk4py || {
    echo "  ✗ jdk4py ইনস্টল ব্যর্থ"; exit 1; }
fi
"$JAVA_BIN" -version 2>&1 | head -1

if [ ! -f "$TOOLS_DIR/node_modules/@drxiaozhi/minapk/tools/d8.jar" ]; then
  say "২. বিল্ড টুল (npm: @drxiaozhi/minapk, aaptjs3)"
  mkdir -p "$TOOLS_DIR" && cd "$TOOLS_DIR"
  npm install --silent --no-audit --no-fund @drxiaozhi/minapk aaptjs3 || {
    echo "  ✗ npm ইনস্টল ব্যর্থ"; exit 1; }
fi
chmod +x "$AAPT2" 2>/dev/null
ls -la "$TOOLS_DIR/node_modules/@drxiaozhi/minapk/tools/" | head -7

# ── ৩. ডেটাবেস অ্যাসেট ────────────────────────────────────────
mkdir -p "$ASSETS/db"
if [ ! -s "$ASSETS/db/prakriti_kosh.db.z" ]; then
  say "৩. সম্পূর্ণ ডেটাবেস (২,৫০,০০০ প্রজাতি) বানাচ্ছি — ~২ মিনিট"
  python3 "$ROOT/tools/build_dataset.py" --total 250000 --demo 500 \
    --out "$ROOT/data/dist" --web "$ROOT/web/data" || exit 1
  cp -f "$ROOT/data/dist/prakriti_kosh.db.z" "$ASSETS/db/"
fi
python3 - "$ASSETS/db/prakriti_kosh.db.z" <<'PY'
import hashlib, json, os, sqlite3, struct, sys, tempfile
p = sys.argv[1]
raw = open(p, 'rb').read()
assert raw[:5] == b'PKDB1', 'magic মেলেনি'
ver, flags, size = struct.unpack('<HBQ', raw[5:16])
sha = raw[16:48]
assert raw[48:50] == b'\x1f\x8b', 'gzip ম্যাজিক ৪৮ নম্বর বাইটে নেই'
with tempfile.NamedTemporaryFile(suffix='.db', delete=False) as t:
    t.write(__import__('gzip').decompress(raw[48:])); db = t.name
assert os.path.getsize(db) == size, 'আকার মেলেনি'
assert hashlib.sha256(open(db, 'rb').read()).digest() == sha, 'SHA-256 মেলেনি'
c = sqlite3.connect(db).cursor()
n = c.execute('select count(*) from species').fetchone()[0]
print(f'  ✓ অ্যাসেট ঠিক আছে: version={ver} raw={size/1e6:.1f}MB প্রজাতি={n}')
os.remove(db)
PY

if [ ! -s /tmp/assets-demo/db/prakriti_kosh.db.z ]; then
  say "৩খ. ডেমো ডেটাবেস (২০,০০০ প্রজাতি) — ~১০ সেকেন্ড"
  mkdir -p /tmp/assets-demo/db /tmp/demoweb
  python3 "$ROOT/tools/build_dataset.py" --total 20000 --demo 400 \
    --out "$ROOT/data/dist-demo" --web /tmp/demoweb || exit 1
  cp -f "$ROOT/data/dist-demo/prakriti_kosh.db.z" /tmp/assets-demo/db/
  python3 - "$ROOT/data/dist-demo/prakriti_kosh.db" /tmp/assets-demo/db/prakriti_kosh.db.z <<'PY'
import json, os, sqlite3, sys, time
db, z = sys.argv[1], sys.argv[2]
c = sqlite3.connect(db).cursor()
q = lambda s: c.execute(s).fetchone()[0]
st = {'schema_version': '3', 'variant': 'demo',
      'built_at': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
      'species_total': q('select count(*) from species'),
      'groups': q('select count(*) from category_group'),
      'classes': q('select count(*) from taxon_class'),
      'orders': q('select count(*) from taxon_order'),
      'families': q('select count(*) from taxon_family'),
      'db_bytes': os.path.getsize(db), 'asset': 'db/prakriti_kosh.db.z',
      'asset_bytes': os.path.getsize(z)}
open(os.path.join(os.path.dirname(z), 'build_stats.json'), 'w',
     encoding='utf-8').write(json.dumps(st, ensure_ascii=False, indent=1))
print(f"  ✓ ডেমো: {st['species_total']} প্রজাতি, {st['asset_bytes']/1e6:.1f} MB")
PY
fi

# ── ৩গ. SQL পরীক্ষা (ডিভাইসে যাওয়ার আগেই) ────────────────────
say "৩গ. Repository.java-র সব SQL আসল ডেটাবেসে কম্পাইল হচ্ছে কি না"
python3 "$ROOT/tools/test_sql.py" "$ROOT/data/dist/prakriti_kosh.db" | tail -1 || exit 1
python3 "$ROOT/tools/test_sql.py" "$ROOT/data/dist-demo/prakriti_kosh.db" | tail -1 || exit 1

# ── ৪. APK বিল্ড ─────────────────────────────────────────────
say "৪. APK বিল্ড (ডেমো → সম্পূর্ণ)"
cd "$ROOT"
JAVA_BIN="$JAVA_BIN" TOOLS_DIR="$TOOLS_DIR/node_modules/@drxiaozhi/minapk/tools" \
  AAPT2_BIN="$AAPT2" ASSETS_DIR=/tmp/assets-demo \
  APK_NAME=prakriti-kosh-demo.apk bash tools/build_apk.sh || exit 1
JAVA_BIN="$JAVA_BIN" TOOLS_DIR="$TOOLS_DIR/node_modules/@drxiaozhi/minapk/tools" \
  AAPT2_BIN="$AAPT2" ASSETS_DIR="$ASSETS" \
  APK_NAME=prakriti-kosh.apk bash tools/build_apk.sh || exit 1

say "৪খ. ডায়াগনস্টিক APK (install-test.apk — ইনস্টল পরীক্ষার জন্য)"
JAVA_BIN="$JAVA_BIN" TOOLS_DIR="$TOOLS_DIR/node_modules/@drxiaozhi/minapk/tools" \
  AAPT2_BIN="$AAPT2" bash tools/build_diag.sh || exit 1

# ── ৫. স্বাক্ষর যাচাই + সার্ভার ────────────────────────────────
say "৫. স্বাক্ষর যাচাই"
for f in dist/install-test.apk dist/prakriti-kosh-demo.apk dist/prakriti-kosh.apk; do
  "$JAVA_BIN" -jar "$TOOLS_DIR/node_modules/@drxiaozhi/minapk/tools/apksigner.jar" \
    verify --print-certs "$f" 2>/dev/null | head -1 | sed "s|^|  $f: |"
  sha256sum "$f" | cut -c1-46
done

cp -f dist/*.apk web/
say "৬. সার্ভার চালু (পোর্ট $PORT)"
if [ -n "$SANDBOX_ID" ]; then
  echo "  📲 ছোট  APK: https://${PORT}-${SANDBOX_ID}.e2b.app/prakriti-kosh-demo.apk"
  echo "  📦 সম্পূর্ণ APK: https://${PORT}-${SANDBOX_ID}.e2b.app/prakriti-kosh.apk"
  echo "  🎨 UI + ডাউনলোড পেজ: https://${PORT}-${SANDBOX_ID}.e2b.app/"
fi
exec python3 "$ROOT/tools/serve.py" "$PORT" "$ROOT/web"

#!/usr/bin/env bash
# প্রকৃতি কোষ — সম্পূর্ণ হাতে-গড়া APK বিল্ড পাইপলাইন (Gradle/Android Studio ছাড়া)।
#
# ধাপগুলোর ক্রম গুরুত্বপূর্ণ:
#   ১. aapt2 compile : রিসোর্স → flat → res.zip
#   ২. aapt2 link    : manifest + res.zip → base.apk  (+ R.txt = আসল রিসোর্স-id)
#   ৩. R.java        : tools/r_from_txt.py দিয়ে R.txt থেকে (আসল id!)
#   ৪. javac (ECJ)   : java -jar ecj.jar -bootclasspath android.jar
#   ৫. dex (D8/R8)   : java -cp d8.jar com.android.tools.r8.D8 --min-api 21
#   ৬. প্যাকেজ       : classes.dex + assets (STORED, ৪-বাইট অ্যালাইন)
#   ৭. apksigner     : v1+v2+v3, debug keystore
#   ৮. যাচাই         : স্বাক্ষর + dex-এর রিসোর্স-id মিল + ভেতরের ডেটাবেস
#
# ⚠ আগে ধাপ ৩ শেষে ছিল এবং tools/gen_r.py কাল্পনিক id (0x7f010001…) বসাত।
#   ফলে resources.arsc-এর আসল id-র (0x7f040005 string, 0x7f050005 style…) সঙ্গে
#   মিলত না → অ্যাপ খোলার সঙ্গে সঙ্গে Resources$NotFoundException-এ ক্র্যাশ।
#   এখন আগে aapt2 link চলে, তারপর R.txt থেকে আসল id দিয়ে R.java হয়।
#
# এনভায়রনমেন্ট ভেরিয়েবল:
#   JAVA_BIN, TOOLS_DIR, AAPT2_BIN, API_LEVEL, ASSETS_DIR, APK_NAME, SKIP_VERIFY
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MAIN="$ROOT/android/app/src/main"
BUILD="$ROOT/build"
OUT="$ROOT/dist"
ASSETS="${ASSETS_DIR:-$MAIN/assets}"          # ছোট/বড় ডেটাসেট বেছে নেওয়ার জন্য
APK_NAME="${APK_NAME:-prakriti-kosh.apk}"

JAVA="${JAVA_BIN:-/tmp/jdkenv/lib/python3.11/site-packages/jdk4py/java-runtime/bin/java}"
TOOLS="${TOOLS_DIR:-/tmp/apktools/node_modules/@drxiaozhi/minapk/tools}"
AAPT2="${AAPT2_BIN:-/tmp/apktools/node_modules/aaptjs3/bin/x64/linux/aapt2}"
ANDROID_JAR="$TOOLS/android.jar"
KEYSTORE="$TOOLS/debug.keystore"
API="${API_LEVEL:-34}"

[ -s "$ASSETS/db/prakriti_kosh.db.z" ] || {
  echo "✗ ডেটাবেস অ্যাসেট নেই: $ASSETS/db/prakriti_kosh.db.z" >&2
  echo "  আগে চালান: python3 tools/build_dataset.py --total 250000 --out data/dist" >&2
  echo "  তারপর:    cp data/dist/prakriti_kosh.db.z $ASSETS/db/" >&2
  exit 1; }

mkdir -p "$BUILD" "$OUT"
rm -rf "$BUILD/gen" "$BUILD/classes" "$BUILD/dex" "$BUILD/flat" "$BUILD/res.zip" \
       "$BUILD/base.apk" "$BUILD/R.txt"
mkdir -p "$BUILD/gen/com/prakriti/kosh" "$BUILD/dex"

echo "── ১. aapt2 compile"
"$AAPT2" compile --dir "$MAIN/res" -o "$BUILD/res.zip"

echo "── ২. aapt2 link (+ R.txt)"
"$AAPT2" link -o "$BUILD/base.apk" -I "$ANDROID_JAR" \
  --manifest "$MAIN/AndroidManifest.xml" \
  --min-sdk-version 21 --target-sdk-version "$API" \
  --output-text-symbols "$BUILD/R.txt" \
  --auto-add-overlay "$BUILD/res.zip"
[ -s "$BUILD/R.txt" ] || { echo "✗ R.txt তৈরি হয়নি" >&2; exit 1; }
echo "  R.txt: $(grep -c '^int ' "$BUILD/R.txt") টি রিসোর্স-id"

echo "── ৩. R.java (আসল id)"
python3 "$ROOT/tools/r_from_txt.py" "$BUILD/R.txt" \
  "$BUILD/gen/com/prakriti/kosh/R.java" com.prakriti.kosh

echo "── ৪. javac (ECJ)"
mkdir -p "$BUILD/classes"
"$JAVA" -Xmx1400m -jar "$TOOLS/ecj-3.45.0.jar" \
  -source 1.8 -target 1.8 -nowarn -encoding UTF-8 \
  -bootclasspath "$ANDROID_JAR" -d "$BUILD/classes" \
  $(find "$MAIN/java" "$BUILD/gen" -name '*.java')
echo "  ক্লাস: $(find "$BUILD/classes" -name '*.class' | wc -l) টি"

echo "── ৫. D8 → classes.dex"
"$JAVA" -Xmx1400m -cp "$TOOLS/d8.jar" com.android.tools.r8.D8 \
  --lib "$ANDROID_JAR" --min-api 21 --output "$BUILD/dex" \
  $(find "$BUILD/classes" -name '*.class')
ls -la "$BUILD/dex"

echo "── ৬. প্যাকেজ (classes.dex + assets, STORED)"
python3 "$ROOT/tools/pack_apk.py" "$BUILD/base.apk" "$BUILD/dex/classes.dex" \
  "$ASSETS" "$BUILD/unsigned.apk"

echo "── ৭. apksigner"
cp "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
"$JAVA" -Xmx900m -jar "$TOOLS/apksigner.jar" sign \
  --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android \
  --ks-key-alias androiddebugkey --v1-signing-enabled true \
  --v2-signing-enabled true --v3-signing-enabled true \
  --out "$OUT/$APK_NAME" "$BUILD/aligned.apk"

if [ "${SKIP_VERIFY:-0}" != "1" ]; then
  echo "── ৮. যাচাই"
  # (grep মিল না পেলে exit 1 হত — set -e এর জন্য পুরো বিল্ড ব্যর্থ দেখাত)
  "$JAVA" -jar "$TOOLS/apksigner.jar" verify --print-certs "$OUT/$APK_NAME" 2>/dev/null \
    | { grep -E "Verifies|scheme" | head -4 || true; }
  python3 "$ROOT/tools/verify_apk.py" "$OUT/$APK_NAME" "$BUILD/R.txt" || exit 1
fi

ls -la "$OUT/$APK_NAME"
echo "✅ APK তৈরি: $OUT/$APK_NAME"

#!/usr/bin/env bash
# build_diag.sh — ছোট্ট “ইনস্টল পরীক্ষা” APK (কয়েক কিলোবাইট) বানায়।
#
# দরকার: ব্যবহারকারীর ফোনে আমাদের স্বাক্ষরিত APK ইনস্টল হয় কি না তা নিশ্চিত হওয়া।
# এই অ্যাপটিতে কোনো ডেটাবেস নেই — খুললেই ডিভাইসের তথ্য দেখায়। এটি ইনস্টল হলে
# বুঝতে হবে পাইপলাইন ও স্বাক্ষর ঠিক আছে; তখন বড় অ্যাপ না খোলার কারণ অন্যত্র।
#
#   bash tools/build_diag.sh        → dist/install-test.apk
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
D="$ROOT/diag"
B="$ROOT/build/diag"
OUT="$ROOT/dist"
NAME="${APK_NAME:-install-test.apk}"

JAVA="${JAVA_BIN:-/tmp/jdkenv/lib/python3.11/site-packages/jdk4py/java-runtime/bin/java}"
TOOLS="${TOOLS_DIR:-/tmp/apktools/node_modules/@drxiaozhi/minapk/tools}"
AAPT2="${AAPT2_BIN:-/tmp/apktools/node_modules/aaptjs3/bin/x64/linux/aapt2}"
ANDROID_JAR="$TOOLS/android.jar"
KEYSTORE="$TOOLS/debug.keystore"

rm -rf "$B"; mkdir -p "$B/gen/com/prakriti/diag" "$B/classes" "$B/dex" "$OUT"

echo "── ১. aapt2 compile + link"
"$AAPT2" compile --dir "$D/res" -o "$B/res.zip"
"$AAPT2" link -o "$B/base.apk" -I "$ANDROID_JAR" \
  --manifest "$D/AndroidManifest.xml" \
  --min-sdk-version 21 --target-sdk-version 34 \
  --output-text-symbols "$B/R.txt" \
  --auto-add-overlay "$B/res.zip"

echo "── ২. R.java (আসল id)"
python3 "$ROOT/tools/r_from_txt.py" "$B/R.txt" "$B/gen/com/prakriti/diag/R.java" com.prakriti.diag

echo "── ৩. ECJ"
"$JAVA" -Xmx900m -jar "$TOOLS/ecj-3.45.0.jar" \
  -source 1.8 -target 1.8 -nowarn -encoding UTF-8 \
  -bootclasspath "$ANDROID_JAR" -d "$B/classes" \
  $(find "$D/src" "$B/gen" -name '*.java')

echo "── ৪. D8"
"$JAVA" -Xmx900m -cp "$TOOLS/d8.jar" com.android.tools.r8.D8 \
  --lib "$ANDROID_JAR" --min-api 21 --output "$B/dex" \
  $(find "$B/classes" -name '*.class')

echo "── ৫. প্যাকেজ"
mkdir -p "$B/empty-assets"
python3 "$ROOT/tools/pack_apk.py" "$B/base.apk" "$B/dex/classes.dex" \
  "$B/empty-assets" "$B/unsigned.apk"

echo "── ৬. স্বাক্ষর"
"$JAVA" -Xmx600m -jar "$TOOLS/apksigner.jar" sign \
  --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android \
  --ks-key-alias androiddebugkey --v1-signing-enabled true \
  --v2-signing-enabled true --v3-signing-enabled true \
  --out "$OUT/$NAME" "$B/unsigned.apk"

echo "── ৭. যাচাই"
"$JAVA" -jar "$TOOLS/apksigner.jar" verify --print-certs "$OUT/$NAME" 2>/dev/null \
  | { grep -E "Verifies|scheme" | head -4 || true; }
python3 "$ROOT/tools/verify_apk.py" "$OUT/$NAME" "$B/R.txt" || true
"$AAPT2" dump badging "$OUT/$NAME" 2>/dev/null | head -3
ls -la "$OUT/$NAME"
echo "✅ তৈরি: $OUT/$NAME"

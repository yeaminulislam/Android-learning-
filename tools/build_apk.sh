#!/usr/bin/env bash
# প্রকৃতি কোষ — সম্পূর্ণ হাতে-গড়া APK বিল্ড পাইপলাইন (Gradle/Android Studio ছাড়া)।
#
#   ১. R.java          : tools/gen_r.py
#   ২. javac (ECJ)     : java -jar ecj.jar -bootclasspath android.jar
#   ৩. dex (D8/R8)     : java -cp d8.jar com.android.tools.r8.D8
#   ৪. aapt2 compile   : রিসোর্স → flat → res.zip
#   ৫. aapt2 link      : manifest + res.zip → base APK (+ AndroidManifest binary)
#   ৬. প্যাকেজ          : classes.dex + assets (STORED) যোগ
#   ৭. align + sign    : apksigner (v1+v2), debug keystore
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MAIN="$ROOT/android/app/src/main"
BUILD="$ROOT/build"
OUT="$ROOT/dist"

JAVA="${JAVA_BIN:-/tmp/jdkenv/lib/python3.11/site-packages/jdk4py/java-runtime/bin/java}"
TOOLS="${TOOLS_DIR:-/tmp/apktools/node_modules/@drxiaozhi/minapk/tools}"
AAPT2="${AAPT2_BIN:-/tmp/apktools/node_modules/aaptjs3/bin/x64/linux/aapt2}"
ANDROID_JAR="$TOOLS/android.jar"
KEYSTORE="$TOOLS/debug.keystore"
API="${API_LEVEL:-34}"

mkdir -p "$BUILD" "$OUT"
echo "── ১. R.java"
python3 "$ROOT/tools/gen_r.py" "$MAIN/res" "$BUILD/gen/com/prakriti/kosh/R.java"

echo "── ২. javac (ECJ)"
rm -rf "$BUILD/classes"; mkdir -p "$BUILD/classes"
"$JAVA" -Xmx1400m -jar "$TOOLS/ecj-3.45.0.jar" \
  -source 1.8 -target 1.8 -nowarn -encoding UTF-8 \
  -bootclasspath "$ANDROID_JAR" -d "$BUILD/classes" \
  $(find "$MAIN/java" "$BUILD/gen" -name '*.java')

echo "── ৩. D8 → classes.dex"
rm -rf "$BUILD/dex"; mkdir -p "$BUILD/dex"
"$JAVA" -Xmx1400m -cp "$TOOLS/d8.jar" com.android.tools.r8.D8 \
  --lib "$ANDROID_JAR" --min-api 21 --output "$BUILD/dex" \
  $(find "$BUILD/classes" -name '*.class')
ls -la "$BUILD/dex"

echo "── ৪. aapt2 compile"
rm -rf "$BUILD/flat" "$BUILD/res.zip"; mkdir -p "$BUILD/flat"
"$AAPT2" compile --dir "$MAIN/res" -o "$BUILD/res.zip"

echo "── ৫. aapt2 link"
rm -f "$BUILD/base.apk"
"$AAPT2" link -o "$BUILD/base.apk" -I "$ANDROID_JAR" \
  --manifest "$MAIN/AndroidManifest.xml" \
  --min-sdk-version 21 --target-sdk-version "$API" \
  --version-code 1 --version-name 1.0 \
  --auto-add-overlay "$BUILD/res.zip"

echo "── ৬. প্যাকেজ (classes.dex + assets, STORED)"
python3 "$ROOT/tools/pack_apk.py" "$BUILD/base.apk" "$BUILD/dex/classes.dex" \
  "$MAIN/assets" "$BUILD/unsigned.apk"

echo "── ৭. apksigner"
cp "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
"$JAVA" -Xmx900m -jar "$TOOLS/apksigner.jar" sign \
  --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android \
  --ks-key-alias androiddebugkey --v1-signing-enabled true \
  --v2-signing-enabled true --out "$OUT/prakriti-kosh.apk" "$BUILD/aligned.apk"
"$JAVA" -jar "$TOOLS/apksigner.jar" verify --print-certs "$OUT/prakriti-kosh.apk" | head -5
ls -la "$OUT/prakriti-kosh.apk"
echo "✅ APK তৈরি: $OUT/prakriti-kosh.apk"

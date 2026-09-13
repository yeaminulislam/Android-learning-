# প্রকৃতি কোষ 🌿

**সম্পূর্ণ অফলাইন অ্যান্ড্রয়েড অ্যাপ — ২,৫০,০০০ প্রজাতির বাংলা বিশ্বকোষ।**

ইন্টারনেট ছাড়াই পৃথিবীর প্রাণী, উদ্ভিদ, ছত্রাক, অণুজীব ও ভাইরাসের তথ্য ঘোরা,
খোঁজা ও পড়া যায়। কোনো বিজ্ঞাপন নেই, কোনো ট্র্যাকিং নেই, কোনো অনুমতি নেই —
শুধু ডেটা আর নকশা।

| | |
|---|---|
| প্যাকেজ | `com.prakriti.kosh` |
| সংস্করণ | 1.0.1 (versionCode 2) |
| minSdk / targetSdk | 21 (Android 5.0) / 34 (Android 14) |
| APK আকার | ৭৬ মেগাবাইট (সম্পূর্ণ) / ৭.১ মেগাবাইট (ডেমো) |
| ডেটাবেস | ৪৩১ মেগাবাইট SQLite, সংকুচিত ৭৫.৯ মেগাবাইট |
| অনুমতি | **শূন্য** (ইন্টারনেট অনুমতি নেই) |
| বাইরের লাইব্রেরি | **শূন্য** (অ্যাপে কোনো AndroidX/third-party নেই) |

---

## দ্রুত শুরু

```bash
bash tools/start_all.sh          # টুলচেইন + ডেটাবেস + APK বিল্ড + ডাউনলোড সার্ভার
bash tools/start_all.sh --serve  # শুধু সার্ভার (APK আগে থেকে থাকলে)
```

স্যান্ডবক্স রিসেট হলে `/tmp`-এর টুলচেইন মুছে যায়; এই স্ক্রিপ্টটি তখন GitHub থেকে
ব্রাঞ্চ ফিরিয়ে এনে টুলচেইন ইনস্টল, ডেটাবেস তৈরি ও APK বিল্ড — সব একসাথে করে দেয়।

---

## 📥 APK কোথায় পাবেন

**সবচেয়ে সহজ — পুরো রিপোর ZIP (৮১ MB, ভেতরে `dist/` ফোল্ডারে দুটি APK):**

```
https://codeload.github.com/yeaminulislam/Android-learning-/zip/refs/heads/arena/01a095b8-android-learning
```

**GitHub রিলিজ (নির্দেশিকাসহ):** https://github.com/yeaminulislam/Android-learning-/releases/tag/v1.0

ফাইল দুটি রিপোজিটরির `dist/` ফোল্ডারে কমিট করা আছে — GitHub-এ ফাইলটি খুলে
ডানদিকের **“Download raw file”** বোতামে ক্লিক করলেই নেমে যাবে:

- ছোট (৭.১ MB): `dist/prakriti-kosh-demo.apk`
- সম্পূর্ণ (৭৬ MB): `dist/prakriti-kosh.apk`

সরাসরি ডাউনলোড URL (ব্রাউজারে বসালেই নামবে):

```
https://api.github.com/repos/yeaminulislam/Android-learning-/contents/dist/prakriti-kosh-demo.apk?ref=arena/01a095b8-android-learning
https://api.github.com/repos/yeaminulislam/Android-learning-/contents/dist/prakriti-kosh.apk?ref=arena/01a095b8-android-learning
```

> রিলিজে APK **অ্যাটাচমেন্ট** হিসেবে নেই — বিল্ড-স্যান্ডবক্স থেকে GitHub-এর
> আপলোড-সার্ভার `uploads.github.com`-এ TLS সংযোগ বন্ধ থাকায় (পরিবেশের সীমাবদ্ধতা)।
> তাই ফাইল সরাসরি রিপোতেই রাখা।

---

## ডাউনলোড ও ইনস্টল

দুটি সংস্করণ — একই অ্যাপ, শুধু ভেতরের ডেটাবেসের আকার আলাদা:

| ফাইল | আকার | প্রজাতি | ডেটাবেস |
|---|---:|---:|---|
| `dist/prakriti-kosh-demo.apk` | ৭.১ MB | ২০,০০০ | ৩৪ MB → ৭ MB |
| `dist/prakriti-kosh.apk` | ৭৬ MB | ২,৫০,০০০ | ৪৩১ MB → ৭৬ MB |

মোবাইল নেটে প্রথমে ছোটটি নামিয়ে দেখে নেওয়া বুদ্ধিমানের কাজ; ভালো লাগলে সম্পূর্ণটি
নামান। দুটো একই কীতে স্বাক্ষরিত, তাই ছোটটির ওপর সম্পূর্ণটি সরাসরি আপডেট হিসেবে
বসবে (ডেটা মুছে যাবে, কারণ ডেটাবেস বদলায়)।

ছোটটি আলাদা করে বানাতে:

```bash
python3 tools/build_dataset.py --total 20000 --demo 400 \
        --out data/dist-demo --web /tmp/demoweb
mkdir -p /tmp/assets-demo/db
cp data/dist-demo/prakriti_kosh.db.z /tmp/assets-demo/db/
ASSETS_DIR=/tmp/assets-demo APK_NAME=prakriti-kosh-demo.apk bash tools/build_apk.sh
```

ফোনে ব্রাউজার থেকে APK নামিয়ে ট্যাপ করলেই ইনস্টল হবে; "অজানা অ্যাপ ইনস্টল করা"
অনুমতি চাইলে Allow দিন (debug-কীতে স্বাক্ষরিত, প্লে-স্টোরের বাইরের অ্যাপ)।
কম্পিউটার থেকে:

```bash
adb install -r dist/prakriti-kosh.apk
```

প্রথমবার খোলার সময় ৭৫.৯ মেগাবাইট সংরক্ষণাগার খুলে ৪৩১ মেগাবাইট ডেটাবেস তৈরি হয়
(১০–৪০ সেকেন্ড, অগ্রগতি দেখানো হয়)। তারপর আর কোনো অপেক্ষা নেই।

---

## ডেটা: কী আছে

```
২,৫০,০০০ প্রজাতি · ১২ বিভাগ · ৮১ শ্রেণি · ৩৪৯ বর্গ · ১,৩১৫ পরিবার
```

| বিভাগ | প্রজাতি | বিভাগ | প্রজাতি |
|---|---:|---|---:|
| অমেরুদণ্ডী | ৪৫,৮০৬ | উদ্ভিদ | ৩৮,০৫৪ |
| সামুদ্রিক | ২৬,৭২৪ | পাখি | ২৩,৪০০ |
| মাছ | ২২,৬২২ | অণুজীব | ১৯,৭৭০ |
| স্তন্যপায়ী | ১৯,৪২৮ | ছত্রাক | ১৭,২৯৭ |
| ডাইনোসর | ১১,৪১২ | ভাইরাস | ১১,৪০৭ |
| সরীসৃপ | ৭,৮০৫ | উভচর | ৬,২৭৫ |

প্রতিটি প্রজাতিতে আছে: বাংলা নাম, ইংরেজি নাম, বৈজ্ঞানিক নাম ও প্রণেতা,
পূর্ণ শ্রেণিবিন্যাস, অঞ্চল ও বাসস্থান, খাদ্য, প্রজনন, আকার, বিষাক্ততার স্তর (০–৪),
IUCN সংরক্ষণ অবস্থা, বিলুপ্ত হলে বিলুপ্তির তথ্য, এবং একটি আকর্ষণীয় তথ্য।
২২৭টি প্রজাতি হাতে লেখা (বাংলাদেশ ও দক্ষিণ এশিয়ার পরিচিত প্রজাতি),
বাকিগুলো শ্রেণিবিন্যাস-সঙ্গতভাবে তৈরি।

### সূচক (index)

```sql
-- ট্যাক্সোনমি ও পেজিনেশন
idx_species_group   (group_id, row_seq)
idx_species_class   (group_id, class_id, row_seq)
idx_species_order   (group_id, class_id, order_id, row_seq)
idx_species_family  (group_id, class_id, order_id, family_id, row_seq)
idx_species_popular (popularity DESC, id)
idx_species_iucn / venom / danger / extinct / demo

-- অনুসন্ধান (নামের সূচক + FTS5 পূর্ণ-পাঠ)
idx_species_bn        (bn_name)
idx_species_en_lower  (lower(en_name))
idx_species_sci_lower (lower(sci_name))
species_fts           -- FTS5, নাম + বাসস্থান + খাদ্য + তথ্য
```

পরিমাপ করা অনুসন্ধান সময় (২.৫ লক্ষ সারিতে): `Panthera` ০.২ মি.সে.,
`মাছরাঙা` ৫.৫ মি.সে., `সুন্দরবন` ৮ মি.সে., `বাঘ` ৬৪ মি.সে.,
`নীল তিমি` (FTS5 ফ্রেজ) ২০৫ মি.সে.।

---

## স্ক্রিন

1. **SplashActivity** — ডেটাবেস প্রস্তুত করে, অগ্রগতি দেখায়
2. **SetupActivity** — প্রথমবারের তিন পাতার পরিচয়
3. **MainActivity** — হিরো, দ্রুত-প্রবেশ, ১২ বিভাগ, জনপ্রিয় ও বিপন্ন স্ট্রিপ
4. **TaxonomyActivity** — বিভাগ → শ্রেণি → বর্গ → পরিবার
5. **SpeciesListActivity** — অসীম স্ক্রোল, IUCN ছাঁকনি, সাজানো
6. **SpeciesDetailActivity** — সোয়াইপ-গ্যালারি, তথ্য, নোট, পছন্দ, শেয়ার
7. **SearchActivity** — ডিবাউন্সসহ FTS5 অনুসন্ধান
8. **SettingsActivity** — ডেটাবেসের তথ্য, পছন্দ/নোট, আবার প্রস্তুত

---

## কর্মক্ষমতার নকশা

- **কীসেট পেজিনেশন** — `WHERE row_seq > ? ORDER BY row_seq LIMIT 25`;
  OFFSET নয়, তাই শেষ পৃষ্ঠায়ও গতি একই। এক পৃষ্ঠায় ২৪টি সারি।
- **জনপ্রিয়তা-ক্রমে পেজিনেশন** — `popularity < ? OR (popularity = ? AND id > ?)`
  (যৌথ কীসেট), `idx_species_popular` ব্যবহার করে।
- **শুধু দরকারি ডেটা** — তালিকায় প্রথমে শুধু `id, row_seq` পড়া হয়, তারপর
  একটিমাত্র জয়েন-কুয়েরিতে ২৪টি পূর্ণ সারি (`byIds`, UNION ALL দিয়ে ক্রম অক্ষুণ্ণ)।
- **সব ডেটাবেস-কাজ ব্যাকগ্রাউন্ড থ্রেডে** — ৩-থ্রেড এক্সিকিউটর + প্রধান থ্রেডের
  Handler; UI থ্রেডে কখনো কুয়েরি চলে না।
- **ছবি প্রোগ্রামে আঁকা** — কোনো ছবি-ফাইল নেই। প্রতিটি প্রজাতির id/শ্রেণি/বাসস্থান
  থেকে বীজ (seed) বানিয়ে ক্যানভাসে পটভূমি, সিলুয়েট ও নকশা আঁকা হয়
  (৪টি পরিবেশ-রূপ)। ফলে ২.৫ লক্ষ প্রজাতির ছবিতে ০ বাইট খরচ।
- **বিটম্যাপ ক্যাশ** — RGB_565 + LruCache (হিপ-এর ১/৮, সর্বোচ্চ ২৪ মেগাবাইট);
  `onTrimMemory`-তে খালি হয়।
- **SQLite টিউনিং** — `cache_size=-12000`, `mmap_size=128MB`, `temp_store=MEMORY`,
  প্রজাতির ডেটাবেস পঠন-মাত্র (read-only), `ANALYZE` করা।

---

## বিল্ড (Gradle/Android Studio ছাড়া)

স্যান্ডবক্সে কোনো Android SDK ছিল না, তাই পুরো পাইপলাইন হাতে সাজানো:

```bash
bash tools/build_apk.sh          # dist/prakriti-kosh.apk
python3 tools/build_dataset.py --total 250000   # ডেটাবেস + .db.z নতুন করে
```

ধাপগুলো (`tools/build_apk.sh`):

| ধাপ | টুল | কাজ |
|---|---|---|
| ১ | `tools/gen_r.py` | রিসোর্স থেকে `R.java` |
| ২ | Eclipse ECJ 3.45 | `.java` → `.class` (`-bootclasspath android.jar`, source/target 1.8) |
| ৩ | D8 8.2 (R8) | `.class` → `classes.dex` |
| ৪ | aapt2 | রিসোর্স কম্পাইল |
| ৫ | aapt2 link | ম্যানিফেস্ট + রিসোর্স → base APK (binary manifest + `resources.arsc`) |
| ৬ | `tools/pack_apk.py` | `classes.dex` + `assets/` যোগ, ৪-বাইট অ্যালাইনমেন্ট |
| ৭ | apksigner | v1 + v2 স্বাক্ষর |

দরকারি টুলগুলো npm/PyPI থেকে আসে:
`@drxiaozhi/minapk` (d8.jar, ecj, apksigner.jar, android.jar, debug.keystore),
`aaptjs3` (Linux aapt2), `jdk4py` (JRE 25 — java চালাতে)।

ডেটাবেসের সংরক্ষণাগার বিন্যাস **PKDB1** (৪৮ বাইট হেডার):

```
0..4    magic    "PKDB1"
5..6    version  uint16 LE = 1
7       flags    uint8 = 0 (gzip)
8..15   raw_len  uint64 LE
16..47  sha256   মূল ফাইলের হ্যাশ
48..    gzip স্ট্রিম
```

অ্যাপ হ্যাশ ও আকার যাচাই করে, তারপর ডেটাবেস `filesDir`-এ লেখে এবং
`.ready` মার্কার ফাইল রাখে।

---

## প্রজেক্টের গঠন

```
android/app/src/main/
  AndroidManifest.xml          ৮টি অ্যাক্টিভিটি, কোনো অনুমতি নেই
  assets/db/prakriti_kosh.db.z সংকুচিত ডেটাবেস (৭৫.৯ MB)
  res/                         রঙ, স্ট্রিং (বাংলা), থিম, ভেক্টর আইকন
  java/com/prakriti/kosh/
    PrakritiApp.java           Application: এক্সিকিউটর, ensureReady, ক্যাশ-সাফাই
    data/                      Species, CategoryGroup, TaxonNode
    db/                        DatabaseManager, Repository (৩২-কলাম SELECT, পেজিনেশন,
                               স্তরে স্তরে অনুসন্ধান), UserStore (পছন্দ/নোট/প্রেফ)
    util/                      Ui (প্রোগ্রাম্যাটিক ভিউ সহায়ক), SpeciesArt (১২০০+ লাইন
                               প্রোসিডিউরাল আর্ট), ArtView, ArtPager
    ui/                        BaseActivity + ৭টি স্ক্রিন + SpeciesAdapter
data/                          ডেটাসেট জেনারেটর (ট্যাক্সোনমি, নাম-ব্যাংক, কিউরেটেড)
tools/                         gen_r.py, build_dataset.py, build_apk.sh, pack_apk.py, dexer/
web/                           ব্রাউজারে চলার মতো UI প্রোটোটাইপ (৮ স্ক্রিন)
dist/prakriti-kosh.apk         তৈরি APK
```

### ওয়েব প্রোটোটাইপ

```bash
python3 -m http.server 8080 --directory web
```

তারপর `http://localhost:8080` — ফোনের ফ্রেমে অ্যাপের হুবহু নকশা,
পাশে প্রতিটি স্ক্রিনের ব্যাখ্যা।

---

## 🩺 অ্যাপ খুলেই বন্ধ হয়ে গেলে

**v1.0-তে একটি বিল্ড-বাগ ছিল, v1.0.1-এ ঠিক করা হয়েছে।** পুরোনো APK থাকলে
আনইনস্টল করে নতুনটি নিন।

কী হয়েছিল: বিল্ড স্ক্রিপ্ট আগে `tools/gen_r.py` দিয়ে নিজে কাল্পনিক রিসোর্স-id
(0x7f010001, 0x7f010002 …) বসাত, কিন্তু APK-র `resources.arsc` তৈরি হত
`aapt2 link` দিয়ে — যেখানে id টাইপ অনুযায়ী বসে (0x7f01… color, 0x7f04… string,
0x7f05… style)। দুটোর একটিও মিলত না (**১১০টি id-ই ভুল**), ফলে অ্যাপের প্রথম
রিসোর্স-লুকআপেই `Resources$NotFoundException` ছুটত — স্ক্রিন এক ঝলক দেখেই অ্যাপ
বন্ধ হয়ে যেত, কোনো বার্তা ছাড়া।

ঠিক করা হয়েছে:

- বিল্ডের ধাপ বদলানো — আগে `aapt2 link --output-text-symbols R.txt`, তারপর
  `tools/r_from_txt.py` সেই **আসল** id দিয়ে R.java বানায়, তারপর কম্পাইল
- `tools/verify_apk.py` — প্রতিটি বিল্ডের পরে R.java ↔ R.txt ↔ resources.arsc মিল,
  ডেটাবেস অ্যাসেটের SHA-256, dex-এর adler32/SHA-1, সব অ্যাক্টিভিটি ক্লাসের উপস্থিতি
  ও স্বাক্ষর-ব্লক যাচাই করে; কোনোটি মিল না করলে বিল্ড ব্যর্থ হয়
- **ক্র্যাশ-রক্ষী** (`util/CrashGuard.java` + `util/CrashActivity.java`) — এখন কোনো
  ত্রুটি হলে অ্যাপ চুপচাপ বন্ধ না হয়ে **স্ক্রিনে পূর্ণ স্ট্যাক-ট্রেস** দেখায়, কপি/শেয়ার
  করা যায়, এবং `filesDir/crash/*.txt`-এ লেখে
- **রোগ-নির্ণয় পর্দা** (সেটিংস → রোগ-নির্ণয়) — ডিভাইসের RAM/স্টোরেজ/Android সংস্করণ,
  ডেটাবেসের অবস্থা, এবং ৮টি সরাসরি পরীক্ষা (গণনা, পেজিনেশন, বাংলা ও ইংরেজি অনুসন্ধান,
  ছবি আঁকা, পছন্দ-ডেটাবেস) সময়সহ
- `SetupActivity`-র স্ক্রোল-লিসেনার `ViewTreeObserver` দিয়ে বদলানো
  (`View.setOnScrollChangeListener` API 23 থেকে, minSdk 21 — Android 5/6-তে ক্র্যাশ করত)
- কম জায়গা/কম RAM-এ স্পষ্ট বাংলা বার্তা; ডেটাবেসের mmap ১২৮→৬৪ মেগাবাইট ও
  পাতা-ক্যাশ ১২→৮ মেগাবাইট

---

## সীমাবদ্ধতা (সৎ স্বীকারোক্তি)

- ২২৭টি প্রজাতি হাতে লেখা ও যাচাই করা; বাকি ~২,৪৯,৭৭৩টি প্রজাতি শ্রেণিবিন্যাসের
  গঠন ধরে **তৈরি (generated)** — নাম, পরিবার, বাসস্থান ও বৈশিষ্ট্য সঙ্গতিপূর্ণ,
  কিন্তু প্রতিটি সারি গবেষণা-মানের নয়। শিক্ষামূলক ব্যবহারের জন্য।
- ছবি আসল আলোকচিত্র নয় — প্রোগ্রামে আঁকা শৈল্পিক সিলুয়েট, যাতে অ্যাপ ছোট থাকে
  এবং সম্পূর্ণ অফলাইন চলে।
- APK debug-কীতে স্বাক্ষরিত; প্লে-স্টোরে ছাড়তে হলে নিজের কী দরকার।

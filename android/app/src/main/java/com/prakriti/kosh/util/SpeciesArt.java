package com.prakriti.kosh.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * প্রজাতির জন্য নিয়তি-নির্ধারিত (deterministic) ভেক্টর চিত্র।
 *
 * ছবি হিসেবে কোনো বিটম্যাপ অ্যাসেটে নেই — প্রতিটি প্রজাতির id, শ্রেণি, বিভাগ,
 * বাসস্থান ও বিষমাত্রা থেকে একটি অনন্য ছবি আঁকা হয়। ফলে ২.৫ লক্ষ প্রজাতির
 * "ছবি" মাত্র কয়েক কিলোবাইট কোডে ধরে, অ্যাপ সম্পূর্ণ অফলাইন থাকে।
 *
 * সব এলোমেলো মান FNV-1a হ্যাশ থেকে আসে → একই প্রজাতিতে সবসময় একই ছবি।
 */
public final class SpeciesArt {

    private SpeciesArt() {
    }

    /** গ্যালারিতে দেখানোর ভিন্ন ভিন্ন "পরিবেশ"। */
    public static final int VARIANTS = 4;

    private static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint S = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path PATH = new Path();
    private static final RectF R = new RectF();

    static {
        P.setStyle(Paint.Style.FILL);
        S.setStyle(Paint.Style.STROKE);
        S.setStrokeCap(Paint.Cap.ROUND);
        S.setStrokeJoin(Paint.Join.ROUND);
    }

    // ------------------------------------------------------------ বীজ

    /** FNV-1a হ্যাশ। */
    public static int fnv(String s, int salt) {
        int h = 0x811c9dc5 ^ salt;
        if (s != null) {
            for (int i = 0; i < s.length(); i++) {
                h ^= s.charAt(i);
                h *= 16777619L;
            }
        }
        h ^= salt * 2654435761L;
        h *= 16777619L;
        return h & 0x7fffffff;
    }

    public static long seedOf(long speciesId, String classId, String groupId, int variant) {
        int a = fnv(classId, (int) (speciesId & 0xffff));
        int b = fnv(groupId, (int) ((speciesId >> 16) & 0xffff));
        long s = ((long) a << 20) ^ (speciesId * 2654435761L) ^ ((long) b << 7) ^ (variant * 40503L);
        return s == 0 ? 1 : (s < 0 ? -s : s);
    }

    /** বীজ থেকে পুনরুৎপাদনযোগ্য "এলোমেলো" মান [0,1)। */
    private static float r(long seed, int salt) {
        long h = seed ^ (salt * 0x9E3779B97F4A7C15L);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 31;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 29;
        return (h & 0xFFFFFFL) / 16777216f;
    }

    private static float rf(long seed, int salt, float lo, float hi) {
        return lo + (hi - lo) * r(seed, salt);
    }

    private static int ri(long seed, int salt, int n) {
        return (int) (r(seed, salt) * n) % n;
    }

    // ------------------------------------------------------------ রঙ

    /** বাসস্থানের ধরন → আকাশ/জলের রঙ। */
    private static int[] palette(String habitat, String groupId, int variant, long seed) {
        String h = habitat == null ? "" : habitat;
        boolean water = groupId.equals("marine") || groupId.equals("fishes")
                || h.contains("সমুদ্র") || h.contains("নদী") || h.contains("পুকুর")
                || h.contains("জল") || h.contains("প্রবাল") || h.contains("হ্রদ")
                || h.contains("নালা") || h.contains("খাল");
        boolean desert = h.contains("মরু") || h.contains("শুষ্ক") || h.contains("পাথুরে");
        boolean snow = h.contains("তুষার") || h.contains("হিম") || h.contains("আর্কটিক")
                || h.contains("তুন্দ্রা");
        boolean cave = h.contains("গুহা") || h.contains("অন্ধকার");
        boolean night = variant == 3 || (variant == 1 && r(seed, 77) < 0.35f);

        int top, bottom, ground, accent;
        if (water) {
            top = night ? 0xFF04313B : 0xFF1E6E8C;
            bottom = night ? 0xFF01222B : 0xFF0B4E5E;
            ground = night ? 0xFF05202A : 0xFF134E4A;
            accent = 0xFF7FD4E3;
        } else if (snow) {
            top = night ? 0xFF22304A : 0xFFBFD9E8;
            bottom = night ? 0xFF101A2C : 0xFFEAF2F6;
            ground = night ? 0xFF2C3B55 : 0xFFF4F8FA;
            accent = 0xFFDCE9F2;
        } else if (desert) {
            top = night ? 0xFF2C2114 : 0xFFE7B96B;
            bottom = night ? 0xFF181208 : 0xFFF6E3B4;
            ground = night ? 0xFF3A2C18 : 0xFFC99B58;
            accent = 0xFFFFD9A0;
        } else if (cave) {
            top = 0xFF2A2622;
            bottom = 0xFF15120F;
            ground = 0xFF3B342C;
            accent = 0xFF8D7B62;
        } else {
            top = night ? 0xFF101F17 : 0xFF8FC7A0;
            bottom = night ? 0xFF07120C : 0xFFDDF0DC;
            ground = night ? 0xFF16301E : 0xFF5E8C4A;
            accent = night ? 0xFF7FB08A : 0xFFF2F9E9;
        }
        if (variant == 2) {              // সোনালি আলো
            top = Ui.mix(top, 0xFFF6C56B, 0.35f);
            bottom = Ui.mix(bottom, 0xFFFFE6B0, 0.30f);
            accent = 0xFFFFD98A;
        }
        return new int[]{top, bottom, ground, accent, night ? 1 : 0};
    }

    /** দেহের রঙ — বিভাগ ও বিষমাত্রা বিবেচনায়। */
    private static int bodyColor(long seed, String groupId, int venomLevel, boolean extinct) {
        int base;
        switch (groupId) {
            case "birds":     base = pick(seed, 3, new int[]{0xFFB4472E, 0xFF2E6DA4, 0xFF3D8B37, 0xFFC9A227, 0xFF6A4C93, 0xFF1F7A6C}); break;
            case "fishes":    base = pick(seed, 3, new int[]{0xFF3C8DA8, 0xFF7FA650, 0xFFC48A2E, 0xFF5A6E9C, 0xFF2F8F7E}); break;
            case "marine":    base = pick(seed, 3, new int[]{0xFF9B5DE5, 0xFFF15BB5, 0xFF00BBF9, 0xFF00F5D4, 0xFFFEE440}); break;
            case "reptiles":  base = pick(seed, 3, new int[]{0xFF6B7A3A, 0xFF8A6A3B, 0xFF4A6B4E, 0xFF7D6B4F}); break;
            case "amphibians":base = pick(seed, 3, new int[]{0xFF5D9B3A, 0xFFB08A2E, 0xFF3E7D6E, 0xFF8B5E3C}); break;
            case "inverts":   base = pick(seed, 3, new int[]{0xFF8B5E3C, 0xFFA63D2F, 0xFF3D6B8B, 0xFF6B8B3D, 0xFF4B3D8B}); break;
            case "fungi":     base = pick(seed, 3, new int[]{0xFFC2703D, 0xFFD9CBA0, 0xFF9E5A5A, 0xFFB9A15E}); break;
            case "plants":    base = pick(seed, 3, new int[]{0xFF4F7A2E, 0xFF6E9B3C, 0xFF3E6B4A, 0xFF8B9B3C}); break;
            case "dinosaurs": base = pick(seed, 3, new int[]{0xFF7A6A4F, 0xFF5A6B4A, 0xFF8B6A4A, 0xFF4A5B6B}); break;
            case "microbes":  base = pick(seed, 3, new int[]{0xFF7FB069, 0xFF5FA8D3, 0xFFD3A15F, 0xFFA17FB0}); break;
            case "viruses":   base = pick(seed, 3, new int[]{0xFF9B5DE5, 0xFF00BBF9, 0xFFF15BB5, 0xFF8AC926}); break;
            default:          base = pick(seed, 3, new int[]{0xFF8B6A4A, 0xFF6B5A4A, 0xFF9B7B5A, 0xFF5A4A3A}); break;
        }
        if (venomLevel >= 3) base = Ui.mix(base, 0xFFC62828, 0.18f + 0.12f * venomLevel);
        else if (venomLevel == 2) base = Ui.mix(base, 0xFFFDD835, 0.22f);
        if (extinct) base = Ui.mix(base, 0xFF6D6D6D, 0.45f);
        return base;
    }

    private static int pick(long seed, int salt, int[] arr) {
        return arr[ri(seed, salt, arr.length)];
    }

    // ------------------------------------------------------------ প্রধান এন্ট্রি

    /** পূর্ণ চিত্র (গ্যালারি)। */
    public static void draw(Canvas cv, int w, int h, long speciesId, String classId,
                            String groupId, String habitat, int venomLevel, boolean extinct,
                            int variant) {
        long seed = seedOf(speciesId, classId, groupId, variant);
        drawInternal(cv, w, h, seed, classId, groupId, habitat, venomLevel, extinct,
                variant, true);
    }

    /** ছোট থাম্বনেইল — কম বিস্তারিত, দ্রুত। */
    public static void drawThumb(Canvas cv, int w, int h, long speciesId, String classId,
                                 String groupId, String habitat, int venomLevel,
                                 boolean extinct) {
        long seed = seedOf(speciesId, classId, groupId, 0);
        drawInternal(cv, w, h, seed, classId, groupId, habitat, venomLevel, extinct, 0, false);
    }

    private static void drawInternal(Canvas cv, int w, int h, long seed, String classId,
                                     String groupId, String habitat, int venomLevel,
                                     boolean extinct, int variant, boolean detail) {
        if (w <= 0 || h <= 0) return;
        int[] pal = palette(habitat, groupId, variant, seed);
        boolean night = pal[4] == 1;
        float u = Math.min(w, h);

        // পটভূমি
        P.setShader(new LinearGradient(0, 0, 0, h, pal[0], pal[1], Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, P);
        P.setShader(null);

        drawBackdrop(cv, w, h, u, seed, pal, night, detail, habitat, groupId);

        int body = bodyColor(seed, groupId, venomLevel, extinct);
        int dark = Ui.shade(body, 0.35f);
        int light = Ui.tint(body, 0.30f);

        String cls = classId == null ? "" : classId;
        if (groupId.equals("plants")) {
            drawPlant(cv, w, h, u, seed, body, dark, light, detail);
        } else if (groupId.equals("fungi")) {
            drawFungus(cv, w, h, u, seed, body, dark, light, detail);
        } else if (groupId.equals("microbes") || groupId.equals("viruses")) {
            drawMicrobe(cv, w, h, u, seed, body, dark, light, detail, groupId);
        } else if (cls.startsWith("Aves")) {
            drawBird(cv, w, h, u, seed, body, dark, light, detail);
        } else if (cls.startsWith("Actinopterygii") || cls.startsWith("Chondrichthyes")
                || groupId.equals("fishes")) {
            drawFish(cv, w, h, u, seed, body, dark, light, detail);
        } else if (cls.contains("Reptilia") || cls.startsWith("Sauropsida")
                || cls.startsWith("Amphibia") || groupId.equals("dinosaurs")) {
            drawReptile(cv, w, h, u, seed, body, dark, light, detail, groupId);
        } else if (cls.startsWith("Insecta") || cls.startsWith("Arachnida")
                || cls.startsWith("Crustacea") || groupId.equals("inverts")) {
            drawBug(cv, w, h, u, seed, body, dark, light, detail, cls);
        } else if (cls.startsWith("Cnidaria") || cls.startsWith("Echinodermata")
                || cls.startsWith("Mollusca") || cls.startsWith("Porifera")) {
            drawMarine(cv, w, h, u, seed, body, dark, light, detail, cls);
        } else if (cls.startsWith("Mammalia")) {
            drawMammal(cv, w, h, u, seed, body, dark, light, detail);
        } else {
            drawBlob(cv, w, h, u, seed, body, dark, light, detail);
        }

        // বিষাক্ত হলে সতর্ক-আভা
        if (venomLevel >= 3) {
            S.setColor(Ui.withAlpha(0xFFC62828, 90));
            S.setStrokeWidth(u * 0.02f);
            S.setStyle(Paint.Style.STROKE);
            R.set(u * 0.03f, u * 0.03f, w - u * 0.03f, h - u * 0.03f);
            cv.drawRoundRect(R, u * 0.06f, u * 0.06f, S);
            S.setStyle(Paint.Style.FILL);
        }
        if (extinct) {
            P.setColor(Ui.withAlpha(0xFF000000, 40));
            cv.drawRect(0, 0, w, h, P);
        }
    }

    // ------------------------------------------------------------ পটভূমি

    private static void drawBackdrop(Canvas cv, int w, int h, float u, long seed, int[] pal,
                                     boolean night, boolean detail, String habitat,
                                     String groupId) {
        String hb = habitat == null ? "" : habitat;
        boolean water = groupId.equals("marine") || groupId.equals("fishes")
                || hb.contains("সমুদ্র") || hb.contains("নদী") || hb.contains("জল")
                || hb.contains("পুকুর") || hb.contains("প্রবাল");

        if (night) {
            // তারা
            int stars = detail ? 46 : 14;
            for (int i = 0; i < stars; i++) {
                float x = rf(seed, 100 + i, 0, w);
                float y = rf(seed, 200 + i, 0, h * 0.55f);
                float rr = rf(seed, 300 + i, 0.6f, 2.1f) * (u / 300f);
                P.setColor(Ui.withAlpha(0xFFFFFFFF, (int) rf(seed, 400 + i, 90, 230)));
                cv.drawCircle(x, y, rr, P);
            }
            // চাঁদ
            P.setColor(Ui.withAlpha(0xFFF6F1DE, 235));
            cv.drawCircle(w * rf(seed, 501, 0.15f, 0.85f), h * rf(seed, 502, 0.1f, 0.28f),
                    u * 0.055f, P);
        } else {
            // সূর্য / আলোর বলয়
            float sx = w * rf(seed, 600, 0.12f, 0.88f);
            float sy = h * rf(seed, 601, 0.10f, 0.26f);
            P.setColor(Ui.withAlpha(pal[3], 120));
            cv.drawCircle(sx, sy, u * 0.10f, P);
            P.setColor(Ui.withAlpha(0xFFFFFFFF, 150));
            cv.drawCircle(sx, sy, u * 0.05f, P);
        }

        if (water) {
            // জলের রেখা
            S.setColor(Ui.withAlpha(0xFFFFFFFF, night ? 36 : 60));
            S.setStrokeWidth(Math.max(1.2f, u * 0.006f));
            int lines = detail ? 9 : 4;
            for (int i = 0; i < lines; i++) {
                float y = h * rf(seed, 700 + i, 0.15f, 0.92f);
                float x0 = w * rf(seed, 750 + i, 0f, 0.5f);
                float len = w * rf(seed, 800 + i, 0.15f, 0.5f);
                PATH.reset();
                PATH.moveTo(x0, y);
                PATH.quadTo(x0 + len * 0.5f, y - u * 0.012f, x0 + len, y);
                cv.drawPath(PATH, S);
            }
            // বুদবুদ
            if (detail) {
                for (int i = 0; i < 14; i++) {
                    P.setColor(Ui.withAlpha(0xFFFFFFFF, (int) rf(seed, 850 + i, 30, 110)));
                    cv.drawCircle(w * rf(seed, 900 + i, 0, 1), h * rf(seed, 950 + i, 0, 1),
                            u * rf(seed, 1000 + i, 0.004f, 0.022f), P);
                }
            }
        }

        // মাটি / তল
        float gy = h * rf(seed, 1100, 0.70f, 0.80f);
        P.setColor(pal[2]);
        PATH.reset();
        PATH.moveTo(0, h);
        PATH.lineTo(0, gy);
        PATH.quadTo(w * 0.3f, gy - u * rf(seed, 1101, 0.01f, 0.05f), w * 0.55f, gy + u * 0.01f);
        PATH.quadTo(w * 0.8f, gy + u * 0.03f, w, gy - u * 0.01f);
        PATH.lineTo(w, h);
        PATH.close();
        cv.drawPath(PATH, P);

        if (detail) {
            // পেছনের গাছ/পাথর
            int n = ri(seed, 1200, 3) + 2;
            for (int i = 0; i < n; i++) {
                float x = w * rf(seed, 1210 + i, 0.02f, 0.98f);
                float hh = u * rf(seed, 1240 + i, 0.10f, 0.30f);
                P.setColor(Ui.withAlpha(night ? 0xFF0C1A10 : Ui.shade(pal[2], 0.28f),
                        water ? 90 : 170));
                if (ri(seed, 1270 + i, 2) == 0 && !water) {
                    cv.drawRect(x - u * 0.008f, gy - hh, x + u * 0.008f, gy + u * 0.02f, P);
                    P.setColor(Ui.withAlpha(night ? 0xFF132A1A : Ui.shade(pal[2], 0.42f),
                            190));
                    cv.drawCircle(x, gy - hh, u * rf(seed, 1300 + i, 0.03f, 0.07f), P);
                } else {
                    R.set(x - u * 0.05f, gy - hh * 0.5f, x + u * 0.05f, gy + u * 0.02f);
                    cv.drawOval(R, P);
                }
            }
            // সামনের ঘাস
            S.setColor(Ui.withAlpha(night ? 0xFF1D3A22 : Ui.shade(pal[2], 0.5f), 220));
            S.setStrokeWidth(Math.max(1f, u * 0.005f));
            for (int i = 0; i < 26; i++) {
                float x = w * rf(seed, 1400 + i, 0, 1);
                float base = h * rf(seed, 1450 + i, 0.86f, 0.99f);
                float tip = base - u * rf(seed, 1500 + i, 0.02f, 0.09f);
                PATH.reset();
                PATH.moveTo(x, base);
                PATH.quadTo(x + u * 0.01f, (base + tip) / 2f, x + u * rf(seed, 1550 + i, -0.03f, 0.03f), tip);
                cv.drawPath(PATH, S);
            }
        }
    }

    // ------------------------------------------------------------ স্তন্যপায়ী

    private static void drawMammal(Canvas cv, int w, int h, float u, long seed,
                                   int body, int dark, int light, boolean detail) {
        float cx = w * 0.5f, base = h * rf(seed, 2000, 0.72f, 0.80f);
        float bl = u * rf(seed, 2001, 0.24f, 0.36f);   // দেহের দৈর্ঘ্য
        float bh = bl * rf(seed, 2002, 0.52f, 0.80f);  // দেহের উচ্চতা
        float legH = u * rf(seed, 2003, 0.06f, 0.17f);
        float neckT = rf(seed, 2004, 0.3f, 1f);        // লম্বা গলা?
        float headR = u * rf(seed, 2005, 0.055f, 0.095f);
        boolean tail = r(seed, 2006) < 0.9f;
        boolean ears = r(seed, 2007) < 0.85f;
        float hump = rf(seed, 2008, 0f, 0.35f);

        float bodyTop = base - bh - legH;
        // পা
        P.setColor(dark);
        int legs = 4;
        for (int i = 0; i < legs; i++) {
            float lx = cx - bl * 0.42f + bl * 0.84f * (i / (float) (legs - 1));
            float lw = u * rf(seed, 2010 + i, 0.018f, 0.032f);
            R.set(lx - lw, base - legH, lx + lw, base + u * 0.01f);
            cv.drawRoundRect(R, lw, lw, P);
        }
        // দেহ
        P.setColor(body);
        R.set(cx - bl / 2f, bodyTop, cx + bl / 2f, bodyTop + bh);
        cv.drawOval(R, P);
        if (hump > 0.05f) {
            R.set(cx - bl * 0.30f, bodyTop - bh * hump * 0.6f, cx + bl * 0.16f,
                    bodyTop + bh * 0.5f);
            cv.drawOval(R, P);
        }
        // লেজ
        if (tail) {
            S.setColor(dark);
            S.setStrokeWidth(u * rf(seed, 2020, 0.010f, 0.026f));
            PATH.reset();
            float tx = cx + bl * 0.48f, ty = bodyTop + bh * 0.35f;
            PATH.moveTo(tx, ty);
            PATH.quadTo(tx + u * 0.10f, ty - u * 0.06f * neckT,
                    tx + u * rf(seed, 2021, 0.05f, 0.16f), ty + u * rf(seed, 2022, -0.10f, 0.06f));
            cv.drawPath(PATH, S);
        }
        // গলা + মাথা
        float hx = cx - bl * 0.52f - headR * 0.5f;
        float hy = bodyTop + bh * 0.18f - neckT * u * 0.07f;
        P.setColor(body);
        S.setColor(body);
        S.setStrokeWidth(headR * 0.9f);
        cv.drawLine(cx - bl * 0.30f, bodyTop + bh * 0.35f, hx, hy, S);
        cv.drawCircle(hx, hy, headR, P);
        // থুতনি
        R.set(hx - headR * 1.6f, hy - headR * 0.35f, hx + headR * 0.3f, hy + headR * 0.85f);
        cv.drawOval(R, P);
        // কান
        if (ears) {
            P.setColor(dark);
            float er = headR * rf(seed, 2030, 0.35f, 0.85f);
            cv.drawCircle(hx + headR * 0.35f, hy - headR * 0.85f, er, P);
            cv.drawCircle(hx - headR * 0.15f, hy - headR * 0.9f, er * 0.8f, P);
        }
        // চোখ ও নাক
        P.setColor(0xFF14230F);
        cv.drawCircle(hx - headR * 0.30f, hy - headR * 0.12f, headR * 0.16f, P);
        P.setColor(dark);
        cv.drawCircle(hx - headR * 1.45f, hy + headR * 0.18f, headR * 0.16f, P);

        if (detail) {
            // গায়ের ছোপ/ডোরা
            int pattern = ri(seed, 2040, 4);
            if (pattern == 1) {
                P.setColor(Ui.withAlpha(dark, 150));
                for (int i = 0; i < 7; i++) {
                    float sx = cx - bl * 0.42f + bl * 0.84f * rf(seed, 2041 + i, 0, 1);
                    float sy = bodyTop + bh * rf(seed, 2060 + i, 0.15f, 0.85f);
                    cv.drawCircle(sx, sy, u * rf(seed, 2080 + i, 0.008f, 0.02f), P);
                }
            } else if (pattern == 2) {
                S.setColor(Ui.withAlpha(dark, 140));
                S.setStrokeWidth(u * 0.008f);
                for (int i = 0; i < 6; i++) {
                    float sx = cx - bl * 0.40f + bl * 0.80f * (i / 5f);
                    PATH.reset();
                    PATH.moveTo(sx, bodyTop + bh * 0.10f);
                    PATH.quadTo(sx + u * 0.01f, bodyTop + bh * 0.5f, sx - u * 0.008f,
                            bodyTop + bh * 0.92f);
                    cv.drawPath(PATH, S);
                }
            } else if (pattern == 3) {
                P.setColor(Ui.withAlpha(light, 190));
                R.set(cx - bl * 0.42f, bodyTop + bh * 0.55f, cx + bl * 0.42f,
                        bodyTop + bh * 1.02f);
                cv.drawOval(R, P);
            }
        }
    }

    // ------------------------------------------------------------ পাখি

    private static void drawBird(Canvas cv, int w, int h, float u, long seed,
                                 int body, int dark, int light, boolean detail) {
        float flying = r(seed, 3000) < 0.55f ? 1f : 0f;
        float cx = w * 0.5f;
        float base = h * rf(seed, 3001, 0.70f, 0.80f);
        float cy = flying == 1f ? h * rf(seed, 3002, 0.34f, 0.52f) : base - u * 0.13f;
        float bl = u * rf(seed, 3003, 0.16f, 0.26f);
        float bh = bl * rf(seed, 3004, 0.75f, 1.15f);
        float headR = u * rf(seed, 3005, 0.045f, 0.070f);
        int beak = 0xFFF0A73B;
        if (r(seed, 3006) < 0.3f) beak = 0xFFD84315;

        // লেজ
        P.setColor(dark);
        PATH.reset();
        PATH.moveTo(cx + bl * 0.35f, cy);
        PATH.lineTo(cx + bl * 1.15f, cy - u * rf(seed, 3010, 0.01f, 0.06f));
        PATH.lineTo(cx + bl * 1.10f, cy + u * rf(seed, 3011, 0.02f, 0.07f));
        PATH.close();
        cv.drawPath(PATH, P);

        // ডানা (পেছেরটি)
        P.setColor(Ui.shade(body, 0.18f));
        drawWing(cv, cx, cy, bl, bh, seed, 3020, -1f);

        // দেহ
        P.setColor(body);
        R.set(cx - bl / 2f, cy - bh / 2f, cx + bl / 2f, cy + bh / 2f);
        cv.save();
        cv.rotate(rf(seed, 3030, -12f, 12f), cx, cy);
        cv.drawOval(R, P);
        cv.restore();

        // ডানা (সামনেরটি)
        P.setColor(Ui.tint(body, 0.12f));
        drawWing(cv, cx, cy, bl, bh, seed, 3040, 1f);

        // মাথা
        float hx = cx - bl * 0.45f - headR * 0.3f;
        float hy = cy - bh * 0.42f;
        P.setColor(r(seed, 3050) < 0.45f ? dark : body);
        cv.drawCircle(hx, hy, headR, P);
        // ঠোঁট
        P.setColor(beak);
        PATH.reset();
        PATH.moveTo(hx - headR * 0.7f, hy - headR * 0.15f);
        PATH.lineTo(hx - headR * (1.5f + rf(seed, 3051, 0f, 1.3f)), hy + headR * 0.1f);
        PATH.lineTo(hx - headR * 0.7f, hy + headR * 0.55f);
        PATH.close();
        cv.drawPath(PATH, P);
        // চোখ
        P.setColor(0xFF14230F);
        cv.drawCircle(hx - headR * 0.22f, hy - headR * 0.15f, headR * 0.17f, P);
        P.setColor(0xFFFFFFFF);
        cv.drawCircle(hx - headR * 0.28f, hy - headR * 0.22f, headR * 0.06f, P);

        if (flying == 0f) {
            // পা
            S.setColor(0xFFE0A030);
            S.setStrokeWidth(Math.max(1.5f, u * 0.008f));
            for (int i = 0; i < 2; i++) {
                float lx = cx - bl * 0.18f + i * bl * 0.30f;
                cv.drawLine(lx, cy + bh * 0.42f, lx, base, S);
                cv.drawLine(lx, base, lx - u * 0.018f, base + u * 0.008f, S);
                cv.drawLine(lx, base, lx + u * 0.018f, base + u * 0.008f, S);
            }
        }
        if (detail) {
            // বুকের হালকা অংশ
            P.setColor(Ui.withAlpha(light, 170));
            R.set(cx - bl * 0.30f, cy - bh * 0.05f, cx + bl * 0.16f, cy + bh * 0.48f);
            cv.drawOval(R, P);
            // পালকের রেখা
            S.setColor(Ui.withAlpha(dark, 110));
            S.setStrokeWidth(Math.max(1f, u * 0.004f));
            for (int i = 0; i < 5; i++) {
                float y = cy - bh * 0.2f + bh * 0.12f * i;
                PATH.reset();
                PATH.moveTo(cx - bl * 0.30f, y);
                PATH.quadTo(cx, y + u * 0.012f, cx + bl * 0.30f, y);
                cv.drawPath(PATH, S);
            }
        }
    }

    private static void drawWing(Canvas cv, float cx, float cy, float bl, float bh,
                                 long seed, int salt, float dir) {
        float span = bl * rf(seed, salt, 0.9f, 1.7f) * dir;
        float lift = bh * rf(seed, salt + 1, 0.4f, 1.5f) * dir;
        PATH.reset();
        PATH.moveTo(cx - bl * 0.1f, cy - bh * 0.1f);
        PATH.quadTo(cx + span * 0.55f, cy - lift * 1.15f, cx + span, cy - lift * 0.25f);
        PATH.quadTo(cx + span * 0.45f, cy + bh * 0.18f, cx - bl * 0.12f, cy + bh * 0.22f);
        PATH.close();
        cv.drawPath(PATH, P);
    }

    // ------------------------------------------------------------ সরীসৃপ / ডাইনোসর

    private static void drawReptile(Canvas cv, int w, int h, float u, long seed,
                                    int body, int dark, int light, boolean detail,
                                    String groupId) {
        boolean dino = "dinosaurs".equals(groupId);
        float cx = w * 0.5f;
        float base = h * rf(seed, 4000, 0.72f, 0.82f);
        float bl = u * rf(seed, 4001, dino ? 0.30f : 0.26f, dino ? 0.46f : 0.40f);
        float bh = bl * rf(seed, 4002, dino ? 0.42f : 0.28f, dino ? 0.66f : 0.46f);
        float bodyTop = base - bh;
        float neck = rf(seed, 4003, dino ? 0.6f : 0.1f, dino ? 1.5f : 0.5f);
        float headL = u * rf(seed, 4004, 0.05f, dino ? 0.11f : 0.08f);

        // লেজ
        P.setColor(dark);
        PATH.reset();
        PATH.moveTo(cx + bl * 0.35f, bodyTop + bh * 0.35f);
        PATH.quadTo(cx + bl * 0.9f, bodyTop + bh * 0.3f,
                cx + bl * rf(seed, 4010, 0.95f, 1.35f), base - u * rf(seed, 4011, 0.01f, 0.09f));
        PATH.quadTo(cx + bl * 0.8f, bodyTop + bh * 0.85f, cx + bl * 0.32f, bodyTop + bh * 0.8f);
        PATH.close();
        cv.drawPath(PATH, P);

        // পা
        P.setColor(dark);
        float legH = u * rf(seed, 4020, dino ? 0.05f : 0.03f, dino ? 0.13f : 0.07f);
        for (int i = 0; i < 4; i++) {
            float lx = cx - bl * 0.36f + bl * 0.72f * (i / 3f);
            float lw = u * rf(seed, 4024 + i, 0.014f, 0.030f);
            PATH.reset();
            PATH.moveTo(lx - lw, bodyTop + bh * 0.75f);
            PATH.lineTo(lx + lw, bodyTop + bh * 0.75f);
            PATH.lineTo(lx + lw * 1.5f, base);
            PATH.lineTo(lx - lw * 1.5f, base);
            PATH.close();
            cv.drawPath(PATH, P);
        }
        // দেহ
        P.setColor(body);
        R.set(cx - bl / 2f, bodyTop, cx + bl / 2f, bodyTop + bh);
        cv.drawOval(R, P);
        // পিঠের কাঁটা / খোলস
        if (dino || r(seed, 4030) < 0.5f) {
            P.setColor(Ui.shade(body, 0.25f));
            int spikes = detail ? 9 : 5;
            for (int i = 0; i < spikes; i++) {
                float sx = cx - bl * 0.42f + bl * 0.84f * (i / (float) (spikes - 1));
                float sh = u * rf(seed, 4031 + i, 0.012f, 0.045f);
                PATH.reset();
                PATH.moveTo(sx - u * 0.012f, bodyTop + bh * 0.16f);
                PATH.lineTo(sx, bodyTop - sh);
                PATH.lineTo(sx + u * 0.012f, bodyTop + bh * 0.16f);
                PATH.close();
                cv.drawPath(PATH, P);
            }
        }
        // গলা ও মাথা
        float hx = cx - bl * 0.42f - headL * 0.4f;
        float hy = bodyTop - neck * u * 0.10f;
        S.setColor(body);
        S.setStrokeWidth(headL * rf(seed, 4050, 0.5f, 0.95f));
        cv.drawLine(cx - bl * 0.28f, bodyTop + bh * 0.30f, hx, hy, S);
        P.setColor(body);
        R.set(hx - headL, hy - headL * 0.42f, hx + headL * 0.4f, hy + headL * 0.42f);
        cv.drawOval(R, P);
        // চোখ
        P.setColor(0xFFF6C244);
        cv.drawCircle(hx - headL * 0.25f, hy - headL * 0.10f, headL * 0.14f, P);
        P.setColor(0xFF14230F);
        cv.drawCircle(hx - headL * 0.25f, hy - headL * 0.10f, headL * 0.06f, P);

        if (detail) {
            // আঁশ
            P.setColor(Ui.withAlpha(dark, 120));
            for (int i = 0; i < 22; i++) {
                float sx = cx - bl * 0.44f + bl * 0.88f * rf(seed, 4060 + i, 0, 1);
                float sy = bodyTop + bh * rf(seed, 4090 + i, 0.12f, 0.9f);
                R.set(sx - u * 0.012f, sy - u * 0.008f, sx + u * 0.012f, sy + u * 0.008f);
                cv.drawArc(R, 0, 180, true, P);
            }
            // পেটের হালকা রেখা
            P.setColor(Ui.withAlpha(light, 180));
            R.set(cx - bl * 0.40f, bodyTop + bh * 0.62f, cx + bl * 0.40f, bodyTop + bh);
            cv.drawOval(R, P);
        }
    }

    // ------------------------------------------------------------ মাছ

    private static void drawFish(Canvas cv, int w, int h, float u, long seed,
                                 int body, int dark, int light, boolean detail) {
        float cx = w * rf(seed, 5000, 0.42f, 0.58f);
        float cy = h * rf(seed, 5001, 0.40f, 0.58f);
        float bl = u * rf(seed, 5002, 0.26f, 0.40f);
        float bh = bl * rf(seed, 5003, 0.30f, 0.62f);
        boolean flat = r(seed, 5004) < 0.18f;

        // লেজ
        P.setColor(Ui.shade(body, 0.15f));
        PATH.reset();
        PATH.moveTo(cx + bl * 0.44f, cy);
        PATH.lineTo(cx + bl * rf(seed, 5010, 0.70f, 0.92f), cy - bh * rf(seed, 5011, 0.6f, 1.3f));
        PATH.lineTo(cx + bl * 0.62f, cy);
        PATH.lineTo(cx + bl * rf(seed, 5012, 0.70f, 0.92f), cy + bh * rf(seed, 5013, 0.6f, 1.3f));
        PATH.close();
        cv.drawPath(PATH, P);
        // পিঠের পাখনা
        PATH.reset();
        PATH.moveTo(cx - bl * 0.18f, cy - bh * 0.44f);
        PATH.quadTo(cx, cy - bh * rf(seed, 5020, 0.9f, 1.7f), cx + bl * 0.22f, cy - bh * 0.40f);
        PATH.close();
        cv.drawPath(PATH, P);
        // পেটের পাখনা
        PATH.reset();
        PATH.moveTo(cx - bl * 0.06f, cy + bh * 0.40f);
        PATH.quadTo(cx - bl * 0.02f, cy + bh * 1.1f, cx + bl * 0.18f, cy + bh * 0.44f);
        PATH.close();
        cv.drawPath(PATH, P);
        // দেহ
        P.setColor(body);
        R.set(cx - bl / 2f, cy - bh / 2f, cx + bl / 2f, cy + bh / 2f);
        cv.drawOval(R, P);
        // মাথা
        P.setColor(Ui.tint(body, 0.10f));
        R.set(cx - bl * 0.55f, cy - bh * 0.42f, cx - bl * 0.12f, cy + bh * 0.42f);
        cv.drawOval(R, P);
        // চোখ
        P.setColor(0xFFFFFFFF);
        cv.drawCircle(cx - bl * 0.40f, cy - bh * 0.10f, u * 0.016f, P);
        P.setColor(0xFF14230F);
        cv.drawCircle(cx - bl * 0.41f, cy - bh * 0.10f, u * 0.008f, P);
        // ফুলকা
        S.setColor(Ui.withAlpha(dark, 150));
        S.setStrokeWidth(Math.max(1.2f, u * 0.006f));
        PATH.reset();
        PATH.moveTo(cx - bl * 0.20f, cy - bh * 0.34f);
        PATH.quadTo(cx - bl * 0.13f, cy, cx - bl * 0.20f, cy + bh * 0.34f);
        cv.drawPath(PATH, S);

        if (detail) {
            if (flat) {
                P.setColor(Ui.withAlpha(dark, 130));
                for (int i = 0; i < 9; i++) {
                    cv.drawCircle(cx - bl * 0.3f + bl * 0.7f * rf(seed, 5030 + i, 0, 1),
                            cy - bh * 0.3f + bh * 0.6f * rf(seed, 5050 + i, 0, 1),
                            u * rf(seed, 5070 + i, 0.006f, 0.02f), P);
                }
            } else {
                S.setColor(Ui.withAlpha(light, 160));
                S.setStrokeWidth(Math.max(1f, u * 0.005f));
                for (int i = 0; i < 5; i++) {
                    float y = cy - bh * 0.30f + bh * 0.15f * i;
                    PATH.reset();
                    PATH.moveTo(cx - bl * 0.44f, y);
                    PATH.quadTo(cx, y + u * 0.01f, cx + bl * 0.44f, y);
                    cv.drawPath(PATH, S);
                }
            }
            // আঁশের ছায়া
            P.setColor(Ui.withAlpha(dark, 60));
            R.set(cx - bl * 0.1f, cy, cx + bl * 0.48f, cy + bh * 0.5f);
            cv.drawOval(R, P);
        }
    }

    // ------------------------------------------------------------ পোকা / মাকড়সা

    private static void drawBug(Canvas cv, int w, int h, float u, long seed,
                                int body, int dark, int light, boolean detail, String cls) {
        float cx = w * 0.5f;
        float cy = h * rf(seed, 6000, 0.42f, 0.60f);
        boolean spider = cls.startsWith("Arachnida") || r(seed, 6001) < 0.2f;
        boolean winged = !spider && r(seed, 6002) < 0.7f;
        float bl = u * rf(seed, 6003, 0.16f, 0.30f);

        if (spider) {
            int legs = 8;
            S.setColor(dark);
            S.setStrokeWidth(Math.max(1.4f, u * 0.009f));
            for (int i = 0; i < legs; i++) {
                float side = i < legs / 2 ? -1f : 1f;
                int k = i % (legs / 2);
                float ang = -0.9f + k * 0.62f;
                float lx = cx + side * bl * 0.30f;
                float kneeX = lx + side * bl * rf(seed, 6010 + i, 0.55f, 0.95f);
                float kneeY = cy - u * 0.10f + k * u * 0.045f + ang * u * 0.02f;
                float footX = kneeX + side * bl * rf(seed, 6030 + i, 0.15f, 0.45f);
                float footY = cy + u * rf(seed, 6050 + i, 0.06f, 0.17f);
                PATH.reset();
                PATH.moveTo(lx, cy);
                PATH.quadTo(kneeX, kneeY, footX, footY);
                cv.drawPath(PATH, S);
            }
            P.setColor(body);
            R.set(cx - bl * 0.42f, cy - bl * 0.30f, cx + bl * 0.42f, cy + bl * 0.36f);
            cv.drawOval(R, P);
            P.setColor(Ui.shade(body, 0.2f));
            cv.drawCircle(cx - bl * 0.52f, cy - bl * 0.16f, bl * 0.22f, P);
            P.setColor(0xFF14230F);
            for (int i = 0; i < 4; i++) {
                cv.drawCircle(cx - bl * 0.60f + i * bl * 0.06f, cy - bl * 0.22f,
                        u * 0.0045f, P);
            }
            if (detail) {
                P.setColor(Ui.withAlpha(light, 170));
                for (int i = 0; i < 6; i++) {
                    cv.drawCircle(cx - bl * 0.26f + bl * 0.52f * rf(seed, 6070 + i, 0, 1),
                            cy - bl * 0.16f + bl * 0.32f * rf(seed, 6080 + i, 0, 1),
                            u * rf(seed, 6090 + i, 0.005f, 0.017f), P);
                }
            }
            return;
        }

        // ডানা
        if (winged) {
            P.setColor(Ui.withAlpha(light, 200));
            for (int s = -1; s <= 1; s += 2) {
                for (int i = 0; i < 2; i++) {
                    float span = bl * rf(seed, 6100 + i, 0.9f, 1.6f);
                    float up = (i == 0 ? -1f : 0.35f) * bl * rf(seed, 6110 + i, 0.7f, 1.4f);
                    PATH.reset();
                    PATH.moveTo(cx, cy - bl * 0.1f);
                    PATH.quadTo(cx + s * span * 0.6f, cy + up * 1.3f, cx + s * span, cy + up * 0.3f);
                    PATH.quadTo(cx + s * span * 0.5f, cy + bl * 0.25f, cx, cy + bl * 0.05f);
                    PATH.close();
                    cv.drawPath(PATH, P);
                    if (detail) {
                        S.setColor(Ui.withAlpha(dark, 90));
                        S.setStrokeWidth(Math.max(0.8f, u * 0.003f));
                        cv.drawPath(PATH, S);
                    }
                }
            }
        }
        // পা
        S.setColor(dark);
        S.setStrokeWidth(Math.max(1.2f, u * 0.007f));
        int legs = 6;
        for (int i = 0; i < legs; i++) {
            float side = i < 3 ? -1f : 1f;
            int k = i % 3;
            float lx = cx - bl * 0.2f + bl * 0.2f * k;
            PATH.reset();
            PATH.moveTo(lx, cy + bl * 0.1f);
            PATH.lineTo(lx + side * bl * 0.42f, cy + bl * 0.42f + k * u * 0.01f);
            PATH.lineTo(lx + side * bl * 0.55f, cy + bl * 0.75f);
            cv.drawPath(PATH, S);
        }
        // দেহ: মাথা + বক্ষ + উদর
        P.setColor(dark);
        cv.drawCircle(cx - bl * 0.42f, cy, bl * 0.20f, P);
        P.setColor(Ui.shade(body, 0.12f));
        R.set(cx - bl * 0.32f, cy - bl * 0.20f, cx + bl * 0.02f, cy + bl * 0.20f);
        cv.drawOval(R, P);
        P.setColor(body);
        R.set(cx - bl * 0.06f, cy - bl * 0.28f, cx + bl * 0.56f, cy + bl * 0.28f);
        cv.drawOval(R, P);
        // শুঁয়া
        S.setColor(dark);
        S.setStrokeWidth(Math.max(1f, u * 0.005f));
        for (int i = 0; i < 2; i++) {
            PATH.reset();
            PATH.moveTo(cx - bl * 0.52f, cy - bl * 0.12f);
            PATH.quadTo(cx - bl * 0.85f, cy - bl * (0.5f + i * 0.3f),
                    cx - bl * rf(seed, 6120 + i, 0.9f, 1.3f), cy - bl * (0.35f + i * 0.45f));
            cv.drawPath(PATH, S);
        }
        // চোখ
        P.setColor(0xFFFFFFFF);
        cv.drawCircle(cx - bl * 0.46f, cy - bl * 0.05f, bl * 0.07f, P);
        P.setColor(0xFF14230F);
        cv.drawCircle(cx - bl * 0.47f, cy - bl * 0.05f, bl * 0.035f, P);
        if (detail) {
            // ডানা/উদরের ডোরা
            P.setColor(Ui.withAlpha(dark, 140));
            for (int i = 0; i < 5; i++) {
                float x = cx + bl * 0.02f + i * bl * 0.10f;
                cv.drawCircle(x, cy, u * rf(seed, 6130 + i, 0.005f, 0.016f), P);
            }
        }
    }

    // ------------------------------------------------------------ সামুদ্রিক অমেরুদণ্ডী

    private static void drawMarine(Canvas cv, int w, int h, float u, long seed,
                                   int body, int dark, int light, boolean detail, String cls) {
        float cx = w * 0.5f;
        float cy = h * rf(seed, 7000, 0.42f, 0.56f);
        int kind;
        if (cls.startsWith("Cnidaria")) kind = 0;          // জেলি / প্রবাল
        else if (cls.startsWith("Echinodermata")) kind = 1; // তারা / আর্চিন
        else if (cls.startsWith("Mollusca")) kind = 2;      // অক্টোপাস / শামুক
        else kind = 3;                                      // স্পঞ্জ / অন্যান্য

        if (kind == 0) {
            float br = u * rf(seed, 7010, 0.16f, 0.26f);
            P.setColor(Ui.withAlpha(body, 210));
            R.set(cx - br, cy - br, cx + br, cy + br * 0.7f);
            cv.drawArc(R, 180, 180, true, P);
            P.setColor(Ui.withAlpha(light, 150));
            R.set(cx - br * 0.7f, cy - br * 0.7f, cx + br * 0.7f, cy + br * 0.2f);
            cv.drawArc(R, 180, 180, true, P);
            S.setColor(Ui.withAlpha(body, 220));
            S.setStrokeWidth(Math.max(1.2f, u * 0.007f));
            int t = detail ? 14 : 7;
            for (int i = 0; i < t; i++) {
                float x = cx - br * 0.9f + (br * 1.8f) * (i / (float) (t - 1));
                float len = u * rf(seed, 7020 + i, 0.10f, 0.30f);
                PATH.reset();
                PATH.moveTo(x, cy);
                PATH.quadTo(x + u * rf(seed, 7040 + i, -0.05f, 0.05f), cy + len * 0.5f,
                        x + u * rf(seed, 7060 + i, -0.08f, 0.08f), cy + len);
                cv.drawPath(PATH, S);
            }
            return;
        }
        if (kind == 1) {
            float rr = u * rf(seed, 7100, 0.18f, 0.28f);
            int arms = 5 + ri(seed, 7101, 6);
            P.setColor(body);
            PATH.reset();
            for (int i = 0; i < arms * 2; i++) {
                float ang = (float) (i * Math.PI / arms) - (float) Math.PI / 2f;
                float rad = (i % 2 == 0) ? rr : rr * rf(seed, 7102 + i, 0.28f, 0.48f);
                float x = cx + (float) Math.cos(ang) * rad;
                float y = cy + (float) Math.sin(ang) * rad;
                if (i == 0) PATH.moveTo(x, y); else PATH.lineTo(x, y);
            }
            PATH.close();
            cv.drawPath(PATH, P);
            P.setColor(Ui.withAlpha(light, 190));
            cv.drawCircle(cx, cy, rr * 0.24f, P);
            if (detail) {
                P.setColor(Ui.withAlpha(dark, 160));
                for (int i = 0; i < arms; i++) {
                    float ang = (float) (i * 2 * Math.PI / arms) - (float) Math.PI / 2f;
                    for (int k = 1; k <= 3; k++) {
                        float rad = rr * (0.35f + 0.18f * k);
                        cv.drawCircle(cx + (float) Math.cos(ang) * rad,
                                cy + (float) Math.sin(ang) * rad, u * 0.006f, P);
                    }
                }
            }
            return;
        }
        if (kind == 2) {
            float hr = u * rf(seed, 7200, 0.12f, 0.20f);
            P.setColor(body);
            R.set(cx - hr, cy - hr * 1.15f, cx + hr, cy + hr * 0.7f);
            cv.drawOval(R, P);
            S.setColor(Ui.shade(body, 0.15f));
            S.setStrokeWidth(hr * 0.22f);
            int t = detail ? 8 : 5;
            for (int i = 0; i < t; i++) {
                float x = cx - hr * 0.75f + (hr * 1.5f) * (i / (float) (t - 1));
                PATH.reset();
                PATH.moveTo(x, cy + hr * 0.35f);
                PATH.quadTo(x + hr * rf(seed, 7210 + i, -0.7f, 0.7f), cy + hr * 1.5f,
                        x + hr * rf(seed, 7230 + i, -1.1f, 1.1f), cy + hr * rf(seed, 7250 + i, 1.6f, 2.8f));
                cv.drawPath(PATH, S);
            }
            P.setColor(0xFFFFFFFF);
            cv.drawCircle(cx - hr * 0.38f, cy - hr * 0.25f, hr * 0.22f, P);
            cv.drawCircle(cx + hr * 0.38f, cy - hr * 0.25f, hr * 0.22f, P);
            P.setColor(0xFF14230F);
            cv.drawCircle(cx - hr * 0.38f, cy - hr * 0.25f, hr * 0.10f, P);
            cv.drawCircle(cx + hr * 0.38f, cy - hr * 0.25f, hr * 0.10f, P);
            return;
        }
        // স্পঞ্জ / অন্যান্য
        P.setColor(Ui.withAlpha(body, 230));
        int lobes = 4 + ri(seed, 7300, 5);
        for (int i = 0; i < lobes; i++) {
            float rr = u * rf(seed, 7301 + i, 0.06f, 0.16f);
            float x = cx + u * rf(seed, 7320 + i, -0.16f, 0.16f);
            float y = cy + u * rf(seed, 7340 + i, -0.12f, 0.14f);
            cv.drawCircle(x, y, rr, P);
        }
        if (detail) {
            P.setColor(Ui.withAlpha(dark, 120));
            for (int i = 0; i < 20; i++) {
                cv.drawCircle(cx + u * rf(seed, 7360 + i, -0.18f, 0.18f),
                        cy + u * rf(seed, 7400 + i, -0.14f, 0.16f), u * 0.005f, P);
            }
        }
    }

    // ------------------------------------------------------------ উদ্ভিদ

    private static void drawPlant(Canvas cv, int w, int h, float u, long seed,
                                  int body, int dark, int light, boolean detail) {
        float cx = w * rf(seed, 8000, 0.40f, 0.60f);
        float base = h * rf(seed, 8001, 0.80f, 0.88f);
        int kind = ri(seed, 8002, 5);   // 0 গাছ, 1 ঘাস, 2 ফুল, 3 ফার্ন, 4 খেজুর
        float th = u * rf(seed, 8003, 0.28f, 0.52f);

        if (kind == 1) {
            S.setColor(body);
            int n = detail ? 26 : 12;
            for (int i = 0; i < n; i++) {
                S.setStrokeWidth(Math.max(1.5f, u * rf(seed, 8010 + i, 0.005f, 0.014f)));
                float x = w * rf(seed, 8030 + i, 0.05f, 0.95f);
                float hh = th * rf(seed, 8050 + i, 0.35f, 1f);
                PATH.reset();
                PATH.moveTo(x, base + u * 0.05f);
                PATH.quadTo(x + u * rf(seed, 8070 + i, -0.06f, 0.06f), base - hh * 0.6f,
                        x + u * rf(seed, 8090 + i, -0.12f, 0.12f), base - hh);
                cv.drawPath(PATH, S);
            }
            return;
        }

        // কান্ড
        S.setColor(dark);
        S.setStrokeWidth(u * (kind == 0 ? 0.030f : 0.012f));
        PATH.reset();
        PATH.moveTo(cx, base + u * 0.03f);
        PATH.quadTo(cx + u * rf(seed, 8100, -0.03f, 0.03f), base - th * 0.5f, cx, base - th);
        cv.drawPath(PATH, S);

        float topY = base - th;
        if (kind == 0) {
            // গাছের মুকুট
            int lobes = detail ? 9 : 5;
            for (int i = 0; i < lobes; i++) {
                float rr = u * rf(seed, 8110 + i, 0.06f, 0.13f);
                float x = cx + u * rf(seed, 8130 + i, -0.14f, 0.14f);
                float y = topY + u * rf(seed, 8150 + i, -0.10f, 0.06f);
                P.setColor(i % 2 == 0 ? body : Ui.tint(body, 0.10f));
                cv.drawCircle(x, y, rr, P);
            }
        } else if (kind == 4) {
            // খেজুর গাছের পাতা
            int fronds = detail ? 9 : 6;
            S.setColor(body);
            S.setStrokeWidth(u * 0.014f);
            for (int i = 0; i < fronds; i++) {
                float ang = (float) (i * 2 * Math.PI / fronds);
                float len = u * rf(seed, 8170 + i, 0.14f, 0.24f);
                PATH.reset();
                PATH.moveTo(cx, topY);
                PATH.quadTo(cx + (float) Math.cos(ang) * len * 0.7f,
                        topY + (float) Math.sin(ang) * len * 0.4f - u * 0.03f,
                        cx + (float) Math.cos(ang) * len, topY + (float) Math.sin(ang) * len * 0.55f);
                cv.drawPath(PATH, S);
            }
            P.setColor(0xFF8D6E63);
            cv.drawCircle(cx, topY, u * 0.018f, P);
        } else if (kind == 2) {
            // ফুল
            int petals = 5 + ri(seed, 8200, 4);
            float pr = u * rf(seed, 8201, 0.045f, 0.080f);
            int flowerColor = pick(seed, 8202, new int[]{0xFFE53935, 0xFFFFB300, 0xFFD81B60,
                    0xFF8E24AA, 0xFFFFFFFF, 0xFFFB8C00});
            for (int i = 0; i < petals; i++) {
                float ang = (float) (i * 2 * Math.PI / petals);
                P.setColor(flowerColor);
                cv.drawCircle(cx + (float) Math.cos(ang) * pr, topY + (float) Math.sin(ang) * pr,
                        pr * 0.85f, P);
            }
            P.setColor(0xFFFDD835);
            cv.drawCircle(cx, topY, pr * 0.6f, P);
            // পাতা
            P.setColor(body);
            for (int i = 0; i < 2; i++) {
                float dir = i == 0 ? -1f : 1f;
                float ly = base - th * rf(seed, 8210 + i, 0.25f, 0.55f);
                R.set(cx, ly - u * 0.02f, cx + dir * u * 0.10f, ly + u * 0.02f);
                cv.save();
                cv.rotate(dir * 25f, cx, ly);
                cv.drawOval(R, P);
                cv.restore();
            }
        } else {
            // ফার্ন
            S.setColor(body);
            S.setStrokeWidth(Math.max(1.5f, u * 0.008f));
            int fronds = detail ? 7 : 4;
            for (int i = 0; i < fronds; i++) {
                float dir = (i % 2 == 0) ? -1f : 1f;
                float fy = base - th * (0.25f + 0.16f * i);
                PATH.reset();
                PATH.moveTo(cx, fy);
                PATH.quadTo(cx + dir * u * 0.10f, fy - u * 0.03f, cx + dir * u * 0.16f, fy + u * 0.02f);
                cv.drawPath(PATH, S);
                if (detail) {
                    S.setStrokeWidth(Math.max(1f, u * 0.004f));
                    for (int k = 1; k <= 5; k++) {
                        float t = k / 6f;
                        float px = cx + dir * u * 0.16f * t;
                        cv.drawLine(px, fy - u * 0.012f * t, px, fy + u * 0.022f * t, S);
                    }
                    S.setStrokeWidth(Math.max(1.5f, u * 0.008f));
                }
            }
        }
    }

    // ------------------------------------------------------------ ছত্রাক

    private static void drawFungus(Canvas cv, int w, int h, float u, long seed,
                                   int body, int dark, int light, boolean detail) {
        float cx = w * rf(seed, 9000, 0.38f, 0.62f);
        float base = h * rf(seed, 9001, 0.78f, 0.88f);
        float stemH = u * rf(seed, 9002, 0.12f, 0.26f);
        float capW = u * rf(seed, 9003, 0.16f, 0.30f);
        float capH = capW * rf(seed, 9004, 0.38f, 0.80f);
        boolean bracket = r(seed, 9005) < 0.25f;

        if (!bracket) {
            P.setColor(0xFFF1E8D6);
            R.set(cx - capW * 0.14f, base - stemH, cx + capW * 0.14f, base);
            cv.drawRoundRect(R, capW * 0.10f, capW * 0.10f, P);
        }
        P.setColor(body);
        if (bracket) {
            for (int i = 0; i < 3; i++) {
                R.set(cx - capW * (0.9f - i * 0.18f), base - stemH - i * capH * 0.45f,
                        cx + capW * (0.9f - i * 0.18f), base - stemH + capH - i * capH * 0.45f);
                P.setColor(Ui.mix(body, dark, i * 0.22f));
                cv.drawArc(R, 180, 180, true, P);
            }
        } else {
            R.set(cx - capW, base - stemH - capH, cx + capW, base - stemH + capH * 0.35f);
            cv.drawArc(R, 180, 180, true, P);
        }
        if (detail) {
            P.setColor(Ui.withAlpha(light, 200));
            int spots = ri(seed, 9010, 7) + 3;
            for (int i = 0; i < spots; i++) {
                float sx = cx - capW * 0.78f + capW * 1.56f * rf(seed, 9011 + i, 0, 1);
                float sy = base - stemH - capH * rf(seed, 9030 + i, 0.15f, 0.85f);
                cv.drawCircle(sx, sy, u * rf(seed, 9050 + i, 0.006f, 0.020f), P);
            }
            // নিচের ফুলকা
            S.setColor(Ui.withAlpha(dark, 140));
            S.setStrokeWidth(Math.max(1f, u * 0.004f));
            for (int i = 0; i < 12; i++) {
                float x = cx - capW * 0.85f + capW * 1.7f * (i / 11f);
                cv.drawLine(x, base - stemH + capH * 0.05f, x, base - stemH + capH * 0.30f, S);
            }
        }
    }

    // ------------------------------------------------------------ অণুজীব ও ভাইরাস

    private static void drawMicrobe(Canvas cv, int w, int h, float u, long seed,
                                    int body, int dark, int light, boolean detail,
                                    String groupId) {
        float cx = w * rf(seed, 10000, 0.42f, 0.58f);
        float cy = h * rf(seed, 10001, 0.42f, 0.58f);
        boolean virus = "viruses".equals(groupId);
        float rr = u * rf(seed, 10002, 0.14f, 0.22f);

        if (virus) {
            int shape = ri(seed, 10010, 3);
            P.setColor(Ui.withAlpha(body, 235));
            if (shape == 0) {
                cv.drawCircle(cx, cy, rr, P);
                S.setColor(dark);
                S.setStrokeWidth(Math.max(1.5f, u * 0.010f));
                int spikes = detail ? 16 : 9;
                for (int i = 0; i < spikes; i++) {
                    float ang = (float) (i * 2 * Math.PI / spikes);
                    float x1 = cx + (float) Math.cos(ang) * rr;
                    float y1 = cy + (float) Math.sin(ang) * rr;
                    float x2 = cx + (float) Math.cos(ang) * rr * 1.35f;
                    float y2 = cy + (float) Math.sin(ang) * rr * 1.35f;
                    cv.drawLine(x1, y1, x2, y2, S);
                    P.setColor(light);
                    cv.drawCircle(x2, y2, u * 0.009f, P);
                    P.setColor(Ui.withAlpha(body, 235));
                }
            } else if (shape == 1) {
                PATH.reset();
                int sides = 6;
                for (int i = 0; i < sides; i++) {
                    float ang = (float) (i * 2 * Math.PI / sides) - (float) Math.PI / 2f;
                    float x = cx + (float) Math.cos(ang) * rr;
                    float y = cy + (float) Math.sin(ang) * rr;
                    if (i == 0) PATH.moveTo(x, y); else PATH.lineTo(x, y);
                }
                PATH.close();
                cv.drawPath(PATH, P);
                S.setColor(Ui.withAlpha(light, 200));
                S.setStrokeWidth(Math.max(1f, u * 0.005f));
                cv.drawPath(PATH, S);
            } else {
                // ট্যাডপোল-আকৃতি ব্যাকটেরিওফাজ
                R.set(cx - rr * 0.55f, cy - rr, cx + rr * 0.55f, cy + rr * 0.1f);
                cv.drawOval(R, P);
                S.setColor(dark);
                S.setStrokeWidth(Math.max(1.5f, u * 0.008f));
                cv.drawLine(cx, cy + rr * 0.1f, cx, cy + rr * 1.1f, S);
                for (int i = 0; i < 5; i++) {
                    float ang = -0.7f + i * 0.35f;
                    cv.drawLine(cx, cy + rr * 1.1f,
                            cx + (float) Math.sin(ang) * rr * 0.8f,
                            cy + rr * 1.1f + (float) Math.cos(ang) * rr * 0.5f, S);
                }
            }
            if (detail) {
                P.setColor(Ui.withAlpha(light, 150));
                for (int i = 0; i < 8; i++) {
                    cv.drawCircle(cx + rr * rf(seed, 10020 + i, -0.6f, 0.6f),
                            cy + rr * rf(seed, 10040 + i, -0.6f, 0.6f), u * 0.006f, P);
                }
            }
            return;
        }

        // ব্যাকটেরিয়া / আর্কিয়া
        int kind = ri(seed, 10100, 3);
        P.setColor(Ui.withAlpha(body, 225));
        if (kind == 0) {
            R.set(cx - rr * 1.5f, cy - rr * 0.6f, cx + rr * 1.5f, cy + rr * 0.6f);
            cv.save();
            cv.rotate(rf(seed, 10101, -30f, 30f), cx, cy);
            cv.drawOval(R, P);
            cv.restore();
        } else if (kind == 1) {
            cv.drawCircle(cx, cy, rr * 0.9f, P);
            for (int i = 0; i < 4; i++) {
                cv.drawCircle(cx + rr * rf(seed, 10110 + i, -1.3f, 1.3f),
                        cy + rr * rf(seed, 10120 + i, -1.3f, 1.3f), rr * 0.35f, P);
            }
        } else {
            PATH.reset();
            PATH.moveTo(cx - rr * 1.6f, cy);
            for (int i = 0; i <= 16; i++) {
                float t = i / 16f;
                PATH.lineTo(cx - rr * 1.6f + rr * 3.2f * t,
                        cy + (float) Math.sin(t * Math.PI * rf(seed, 10130, 2f, 4f)) * rr * 0.45f);
            }
            S.setColor(Ui.withAlpha(body, 235));
            S.setStrokeWidth(rr * 0.55f);
            S.setStrokeCap(Paint.Cap.ROUND);
            cv.drawPath(PATH, S);
        }
        // ফ্ল্যাজেলা
        if (detail && r(seed, 10140) < 0.7f) {
            S.setColor(Ui.withAlpha(dark, 180));
            S.setStrokeWidth(Math.max(1f, u * 0.005f));
            PATH.reset();
            PATH.moveTo(cx + rr * 1.4f, cy);
            for (int i = 1; i <= 8; i++) {
                PATH.lineTo(cx + rr * 1.4f + i * u * 0.018f,
                        cy + (float) Math.sin(i * 1.1f) * u * 0.016f);
            }
            cv.drawPath(PATH, S);
        }
        P.setColor(Ui.withAlpha(light, 170));
        cv.drawCircle(cx - rr * 0.3f, cy - rr * 0.25f, rr * 0.20f, P);
    }

    // ------------------------------------------------------------ অন্যান্য

    private static void drawBlob(Canvas cv, int w, int h, float u, long seed,
                                 int body, int dark, int light, boolean detail) {
        float cx = w * 0.5f;
        float cy = h * rf(seed, 11000, 0.45f, 0.60f);
        float rr = u * rf(seed, 11001, 0.16f, 0.26f);
        int lobes = 6 + ri(seed, 11002, 5);
        P.setColor(body);
        PATH.reset();
        for (int i = 0; i <= lobes; i++) {
            float ang = (float) (i * 2 * Math.PI / lobes);
            float rad = rr * rf(seed, 11010 + i, 0.72f, 1.18f);
            float x = cx + (float) Math.cos(ang) * rad;
            float y = cy + (float) Math.sin(ang) * rad;
            if (i == 0) PATH.moveTo(x, y); else PATH.lineTo(x, y);
        }
        PATH.close();
        cv.drawPath(PATH, P);
        P.setColor(Ui.withAlpha(light, 180));
        cv.drawCircle(cx - rr * 0.3f, cy - rr * 0.3f, rr * 0.22f, P);
        if (detail) {
            P.setColor(Ui.withAlpha(dark, 130));
            for (int i = 0; i < 10; i++) {
                cv.drawCircle(cx + rr * rf(seed, 11030 + i, -0.7f, 0.7f),
                        cy + rr * rf(seed, 11050 + i, -0.7f, 0.7f),
                        u * rf(seed, 11070 + i, 0.004f, 0.014f), P);
            }
        }
    }
}

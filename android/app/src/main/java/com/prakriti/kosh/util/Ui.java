package com.prakriti.kosh.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.prakriti.kosh.R;

/**
 * ছোট UI হেল্পার — সব স্ক্রিন প্রোগ্রাম্যাটিকভাবে তৈরি হয় (কোনো layout XML নেই),
 * তাই মাপ, রঙ ও ড্রয়েবল বানানোর সাধারণ কাজ এখানে এক জায়গায়।
 */
public final class Ui {

    private Ui() {
    }

    // ---------------------------------------------------------------- মাপ

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    public static int sp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v,
                c.getResources().getDisplayMetrics()));
    }

    // ---------------------------------------------------------------- রঙ

    public static int color(Context c, int res) {
        return c.getResources().getColor(res);
    }

    /** হালকা/গাঢ় মিশ্রণ (alpha নয়) — কন্টেইনার রঙ বানাতে। */
    public static int mix(int a, int b, float t) {
        if (t <= 0f) return a;
        if (t >= 1f) return b;
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return Color.rgb((int) (ar + (br - ar) * t), (int) (ag + (bg - ag) * t),
                (int) (ab + (bb - ab) * t));
    }

    public static int tint(int color, float amount) {
        return mix(color, Color.WHITE, amount);
    }

    public static int shade(int color, float amount) {
        return mix(color, Color.BLACK, amount);
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    /** রঙের ওপর কালো না সাদা লেখা পড়া যাবে। */
    public static int readableOn(int bg) {
        double lum = (0.299 * ((bg >> 16) & 0xFF) + 0.587 * ((bg >> 8) & 0xFF)
                + 0.114 * (bg & 0xFF)) / 255.0;
        return lum > 0.62 ? Color.parseColor("#14230F") : Color.WHITE;
    }

    public static int iucnColor(Context c, String iucn) {
        if (iucn == null) iucn = "";
        switch (iucn) {
            case "LC": return color(c, R.color.iucn_lc);
            case "NT": return color(c, R.color.iucn_nt);
            case "VU": return color(c, R.color.iucn_vu);
            case "EN": return color(c, R.color.iucn_en);
            case "CR": return color(c, R.color.iucn_cr);
            case "EW": return color(c, R.color.iucn_ew);
            case "EX": return color(c, R.color.iucn_ex);
            default:   return color(c, R.color.iucn_dd);
        }
    }

    public static String iucnLabel(String iucn) {
        if (iucn == null) return "তথ্য নেই";
        switch (iucn) {
            case "LC": return "ন্যূনতম ঝুঁকি";
            case "NT": return "প্রায়-বিপন্ন";
            case "VU": return "বিপন্ন";
            case "EN": return "সংকটাপন্ন";
            case "CR": return "মারাত্মক সংকটাপন্ন";
            case "EW": return "বন্যে বিলুপ্ত";
            case "EX": return "বিলুপ্ত";
            default:   return "তথ্য অপর্যাপ্ত";
        }
    }

    public static int venomColor(Context c, int level) {
        switch (level) {
            case 1: return color(c, R.color.venom_1);
            case 2: return color(c, R.color.venom_2);
            case 3: return color(c, R.color.venom_3);
            case 4: return color(c, R.color.venom_4);
            default: return color(c, R.color.venom_0);
        }
    }

    public static String venomLabel(int level) {
        switch (level) {
            case 1: return "সামান্য বিষাক্ত";
            case 2: return "মাঝারি বিষাক্ত";
            case 3: return "বিপজ্জনক";
            case 4: return "অতি বিপজ্জনক";
            default: return "নিরীহ";
        }
    }

    /** "#RRGGBB" → int; ব্যর্থ হলে fallback। */
    public static int parseColor(String s, int fallback) {
        if (TextUtils.isEmpty(s)) return fallback;
        try {
            return Color.parseColor(s);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    // ---------------------------------------------------------------- ড্রয়েবল

    public static GradientDrawable rounded(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static GradientDrawable roundedStroke(int fillColor, int strokeColor,
                                                 float radiusDp, float strokeDp, Context c) {
        GradientDrawable d = rounded(fillColor, radiusDp, c);
        d.setStroke(dp(c, strokeDp), strokeColor);
        return d;
    }

    public static GradientDrawable pill(int color, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, 100));
        return d;
    }

    public static GradientDrawable oval(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    public static GradientDrawable vertical(int top, int bottom) {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{top, bottom});
    }

    /** হালকা ছায়াসহ কার্ডের পটভূমি। */
    public static Drawable card(Context c) {
        return rounded(Color.WHITE, 16, c);
    }

    /** ripple (API 21+) — ক্লিকেবল কার্ডের জন্য। */
    public static Drawable ripple(Context c, Drawable content) {
        if (Build.VERSION.SDK_INT >= 21) {
            TypedValue tv = new TypedValue();
            c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
            Drawable mask = c.getResources().getDrawable(tv.resourceId);
            return new android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(withAlpha(0x1B5E20, 46)),
                    content, mask);
        }
        return content;
    }

    public static void elevate(View v, float dp) {
        if (Build.VERSION.SDK_INT >= 21) v.setElevation(dp);
    }

    // ---------------------------------------------------------------- টেক্সট

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    public static TextView label(Context c, String s) {
        TextView t = text(c, s.toUpperCase(java.util.Locale.ROOT), 11.5f,
                color(c, R.color.text_secondary), true);
        t.setLetterSpacing(0.06f);
        return t;
    }

    /** ছোট রঙিন ব্যাজ (চিপ)। */
    public static TextView chip(Context c, String s, int bg) {
        TextView t = text(c, s, 12f, readableOn(bg), true);
        t.setPadding(dp(c, 10), dp(c, 4), dp(c, 10), dp(c, 4));
        t.setBackground(pill(bg, c));
        t.setGravity(Gravity.CENTER);
        return t;
    }

    // ---------------------------------------------------------------- লেআউট

    public static LinearLayout vbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout hbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams lpw(int w, int h, float weight) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h, weight);
        return p;
    }

    public static FrameLayout.LayoutParams flp(int w, int h, int gravity) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(w, h);
        p.gravity = gravity;
        return p;
    }

    /** LayoutParams না থাকলেও মার্জিন বসায় (যেকোনো প্যারেন্টে)। */
    public static void margins2(View v, int l, int t, int r, int b) {
        ViewGroup.LayoutParams p = v.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        if (!(p instanceof ViewGroup.MarginLayoutParams)) {
            p = new ViewGroup.MarginLayoutParams(p);
        }
        ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) p;
        m.setMargins(l, t, r, b);
        v.setLayoutParams(m);
    }

    public static void margins(View v, int l, int t, int r, int b) {
        ViewGroup.LayoutParams p = v.getLayoutParams();
        if (p instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) p;
            m.setMargins(l, t, r, b);
            v.setLayoutParams(m);
        }
    }

    public static void pad(View v, int l, int t, int r, int b) {
        v.setPadding(l, t, r, b);
    }

    // ---------------------------------------------------------------- সংখ্যা

    private static final char[] BN = {'০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'};

    /** ইংরেজি সংখ্যা → বাংলা সংখ্যা। */
    public static String bnDigits(String s) {
        if (s == null) return "";
        char[] out = s.toCharArray();
        for (int i = 0; i < out.length; i++) {
            if (out[i] >= '0' && out[i] <= '9') out[i] = BN[out[i] - '0'];
        }
        return new String(out);
    }

    public static String bnDigits(long n) {
        return bnDigits(Long.toString(n));
    }

    /** "১২,৩৪৫টি প্রজাতি" ধরনের গণনা। */
    public static String bnCount(long n) {
        return bnDigits(n) + "টি";
    }

    /** "১.২ লক্ষ" / "৯,৪৯৭" ধরনের সংক্ষিপ্ত সংখ্যা। */
    public static String humanCount(long n) {
        if (n >= 10000000) return bnDigits(String.format(java.util.Locale.US, "%.2f", n / 1e7)) + " কোটি";
        if (n >= 100000) return bnDigits(String.format(java.util.Locale.US, "%.2f", n / 1e5)) + " লক্ষ";
        if (n >= 1000) return bnDigits(String.format(java.util.Locale.US, "%.1f", n / 1000f)) + " হাজার";
        return bnDigits(n);
    }

    public static String humanSize(long bytes) {
        if (bytes >= 1L << 30) return bnDigits(String.format(java.util.Locale.US, "%.2f", bytes / (double) (1L << 30))) + " গিগাবাইট";
        if (bytes >= 1L << 20) return bnDigits(String.format(java.util.Locale.US, "%.1f", bytes / (double) (1L << 20))) + " মেগাবাইট";
        if (bytes >= 1L << 10) return bnDigits(String.format(java.util.Locale.US, "%.0f", bytes / (double) (1L << 10))) + " কিলোবাইট";
        return bnDigits(bytes) + " বাইট";
    }

    /** লেবেল + মান — বিস্তারিত পর্বে তথ্য-সারি। */
    public static LinearLayout labeled(Context c, String label, String value, int accent) {
        LinearLayout box = vbox(c);
        box.setBackground(rounded(color(c, R.color.surface_alt), 12, c));
        pad(box, dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));

        LinearLayout head = hbox(c);
        View dot = new View(c);
        dot.setBackground(oval(accent));
        head.addView(dot, lp(dp(c, 8), dp(c, 8)));
        TextView l = label(c, label);
        margins(l, dp(c, 8), 0, 0, 0);
        head.addView(l, lpw(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(head, lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView v = text(c, value, 14.5f, color(c, R.color.text_primary), false);
        v.setLineSpacing(dp(c, 3), 1f);
        margins(v, 0, dp(c, 4), 0, 0);
        box.addView(v, lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return box;
    }
}

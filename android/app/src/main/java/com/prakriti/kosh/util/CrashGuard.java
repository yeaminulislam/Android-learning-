package com.prakriti.kosh.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.prakriti.kosh.PrakritiApp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ক্র্যাশ-রক্ষী।
 *
 * সমস্যা: অ্যাপ কোথায় থামছে ব্যবহারকারী বুঝতে পারছিলেন না — শুধু দেখা যাচ্ছিল
 * অ্যাপ খুলেই বন্ধ হয়ে যাচ্ছে, কোনো বার্তা নেই। Android-এর ডিফল্ট আচরণ হলো
 * প্রক্রিয়াটি মেরে ফেলা, তাই স্ক্রিন এক ঝলক দেখেই মিলিয়ে যায়।
 *
 * সমাধান: অ্যাপের যেকোনো অধরা ছাড়া পাওয়া exception এখানে ধরা পড়ে এবং
 * **স্ক্রিনে পূর্ণ স্ট্যাক-ট্রেসসহ** একটি প্রতিবেদন দেখানো হয় — কপি/শেয়ার করা যায়,
 * ফাইলেও লেখা হয় (filesDir/crash-*.txt)। ফলে সমস্যাটি চোখে দেখা যায় ও জানানো যায়।
 *
 * কী কী ধরা পড়ে:
 *   • যেকোনো থ্রেডের অধরা exception (setDefaultUncaughtExceptionHandler)
 *   • অ্যাক্টিভিটির onCreate/onStart/onResume/onPause/onStop/onDestroy (BaseActivity)
 *   • Application-এর নিজের onCreate (সেখানেও try/catch)
 */
public final class CrashGuard {

    private CrashGuard() {
    }

    private static Thread.UncaughtExceptionHandler system;
    private static volatile String lastReport = "";
    private static volatile Activity shownIn;

    /** অ্যাপ শুরুর সঙ্গে সঙ্গেই বসাতে হয় (Application.onCreate-এর প্রথম লাইন)। */
    public static void install(final Context appContext) {
        system = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                String report = report(appContext, t, e);
                try {
                    save(appContext, report);
                } catch (Throwable ignored) {
                    // প্রতিবেদন লিখতে ব্যর্থ হলেও স্ক্রিনে দেখানোর চেষ্টা চলবে
                }
                try {
                    Intent i = new Intent(appContext, CrashActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    i.putExtra("report", report);
                    appContext.startActivity(i);
                } catch (Throwable ignored) {
                    // স্ক্রিন দেখানো গেল না — সিস্টেমের ডিফল্ট ডায়ালগই দেখাবে
                }
                try {
                    Thread.sleep(400);          // অ্যাক্টিভিটিটি উঠতে সময় পাক
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                if (system != null) {
                    system.uncaughtException(t, e);
                } else {
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(10);
                }
            }
        });
    }

    /** অ্যাক্টিভিটির লাইফসাইকেল-পদ্ধতিতে try/catch — স্ক্রিনে সরাসরি প্রতিবেদন। */
    public static boolean guard(Activity a, String phase, Throwable e) {
        if (e == null) return true;
        String report = report(a, Thread.currentThread(), e)
                + "\n[পর্দা] " + a.getClass().getName() + "." + phase + "\n";
        lastReport = report;
        try {
            save(a, report);
        } catch (Throwable ignored) {
        }
        try {
            show(a, report);
        } catch (Throwable ignored) {
            if (system != null) system.uncaughtException(Thread.currentThread(), e);
        }
        return false;
    }

    /** শেষ ধরা পড়া প্রতিবেদন (সেটিংস পর্দায় দেখানোর জন্য)। */
    public static String lastReport() {
        return lastReport;
    }

    public static File[] savedReports(Context c) {
        File d = dir(c);
        File[] f = d.listFiles();
        return f == null ? new File[0] : f;
    }

    // ------------------------------------------------------------ প্রতিবেদন

    public static String report(Context c, Thread t, Throwable e) {
        StringBuilder b = new StringBuilder();
        b.append("প্রকৃতি কোষ — ক্র্যাশ প্রতিবেদন\n");
        b.append("সময়: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                Locale.US).format(new Date())).append('\n');
        b.append("থ্রেড: ").append(t == null ? "?" : t.getName()).append('\n');
        b.append("────────────────────────────\n");
        b.append(deviceInfo(c));
        b.append("────────────────────────────\n");
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        b.append(sw.toString());
        b.append("────────────────────────────\n");
        b.append(appInfo(c));
        return b.toString();
    }

    public static String deviceInfo(Context c) {
        Runtime r = Runtime.getRuntime();
        long mb = 1024L * 1024L;
        StringBuilder b = new StringBuilder();
        b.append("[ডিভাইস]\n");
        b.append("  Android: ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        b.append("  মডেল: ").append(Build.MANUFACTURER).append(' ')
                .append(Build.MODEL).append(" / ").append(Build.DEVICE).append('\n');
        b.append("  ভাষা: ").append(Locale.getDefault()).append('\n');
        try {
            android.util.DisplayMetrics dm = c.getResources().getDisplayMetrics();
            b.append("  স্ক্রিন: ").append(dm.widthPixels).append('x').append(dm.heightPixels)
                    .append(" @ ").append(Math.round(dm.density * 160)).append("dpi\n");
        } catch (Throwable ignored) {
        }
        b.append("  হিপ: সর্বোচ্চ ").append(r.maxMemory() / mb)
                .append(" MB, মুক্ত ").append(r.freeMemory() / mb).append(" MB\n");
        try {
            ActivityManagerCompat am = new ActivityManagerCompat(c);
            if (am.line != null) b.append("  ").append(am.line).append('\n');
        } catch (Throwable ignored) {
        }
        try {
            File d = c.getFilesDir();
            b.append("  স্টোরেজ: মুক্ত ").append(d.getUsableSpace() / mb).append(" MB\n");
        } catch (Throwable ignored) {
        }
        return b.toString();
    }

    /** অ্যাপের নিজের অবস্থা — ডেটাবেস প্রস্তুত হয়েছে কি না ইত্যাদি। */
    public static String appInfo(Context c) {
        StringBuilder b = new StringBuilder();
        b.append("[অ্যাপ]\n");
        try {
            b.append("  সংস্করণ: ").append(c.getPackageManager()
                    .getPackageInfo(c.getPackageName(), 0).versionName).append('\n');
        } catch (Throwable ignored) {
        }
        try {
            PrakritiApp app = PrakritiApp.get();
            b.append("  Application: ").append(app == null ? "null (!)" : "ঠিক আছে").append('\n');
            if (app != null) {
                b.append("  ডেটাবেস প্রস্তুত: ").append(app.isReady()).append('\n');
                String err = app.lastError();
                if (err != null && err.length() > 0) b.append("  শেষ ত্রুটি: ").append(err).append('\n');
            }
        } catch (Throwable t) {
            b.append("  অ্যাপের তথ্য পড়া যায়নি: ").append(t).append('\n');
        }
        try {
            File db = com.prakriti.kosh.db.DatabaseManager.dbFile(c);
            b.append("  ডেটাবেস ফাইল: ").append(db.exists()
                    ? (db.length() / (1024 * 1024)) + " MB" : "নেই").append('\n');
            File marker = new File(c.getFilesDir(), "prakriti_kosh.db.ready");
            b.append("  .ready মার্কার: ").append(marker.exists() ? "আছে" : "নেই").append('\n');
            File asset = new File(c.getPackageCodePath());
            b.append("  APK: ").append(asset.length() / (1024 * 1024)).append(" MB\n");
        } catch (Throwable t) {
            b.append("  ফাইলের তথ্য পড়া যায়নি: ").append(t).append('\n');
        }
        return b.toString();
    }

    // ------------------------------------------------------------ সংরক্ষণ

    private static File dir(Context c) {
        File d = new File(c.getFilesDir(), "crash");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private static void save(Context c, String report) {
        String name = "crash-" + new SimpleDateFormat("yyyyMMdd-HHmmss",
                Locale.US).format(new Date()) + ".txt";
        File f = new File(dir(c), name);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(f);
            out.write(report.getBytes("UTF-8"));
        } catch (Exception ignored) {
        } finally {
            if (out != null) try { out.close(); } catch (Exception ignored) { }
        }
        File[] all = dir(c).listFiles();
        if (all != null && all.length > 20) {          // পুরোনো ২০টি রেখে বাকি মুছে ফেলি
            java.util.Arrays.sort(all);
            for (int i = 0; i < all.length - 20; i++) all[i].delete();
        }
    }

    // ------------------------------------------------------------ স্ক্রিনে দেখানো

    private static void show(final Activity a, final String report) {
        lastReport = report;
        if (a != null && a.isFinishing()) {
            return;
        }
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#12200F"));
        int pad = Math.round(a.getResources().getDisplayMetrics().density * 16);
        root.setPadding(pad, pad, pad, pad);

        TextView head = new TextView(a);
        head.setText("⚠ অ্যাপটি থেমে গেছে");
        head.setTextColor(Color.WHITE);
        head.setTextSize(19f);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(head);

        TextView sub = new TextView(a);
        sub.setText("নিচের লেখাটি কপি বা শেয়ার করে পাঠিয়ে দিন — এটি দেখে সমস্যাটি "
                + "ঠিক করা যাবে। এটি স্ক্রিনশট নিলেও হবে।");
        sub.setTextColor(Color.parseColor("#B9D6BE"));
        sub.setTextSize(13f);
        sub.setPadding(0, Math.round(pad / 2f), 0, pad);
        root.addView(sub);

        final TextView body = new TextView(a);
        body.setText(report);
        body.setTextColor(Color.parseColor("#E8F5E9"));
        body.setTextSize(10.5f);
        body.setTypeface(Typeface.MONOSPACE);
        body.setMovementMethod(new ScrollingMovementMethod());
        body.setBackgroundColor(Color.parseColor("#0B1609"));
        body.setPadding(pad, pad, pad, pad);
        ScrollView sc = new ScrollView(a);
        sc.addView(body, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(sc, sp);

        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, pad, 0, 0);
        row.addView(button(a, "📋 কপি", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.ClipboardManager cm = (android.content.ClipboardManager)
                        a.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(android.content.ClipData.newPlainText(
                            "crash", report));
                }
            }
        }));
        row.addView(button(a, "⤴ শেয়ার", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, report);
                try {
                    a.startActivity(Intent.createChooser(i, "ক্র্যাশ প্রতিবেদন"));
                } catch (Exception ignored) {
                }
            }
        }));
        row.addView(button(a, "↻ আবার খুলুন", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = a.getPackageManager().getLaunchIntentForPackage(
                        a.getPackageName());
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK);
                    a.startActivity(i);
                }
                a.finish();
            }
        }));
        root.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (a instanceof CrashActivity) {
            shownIn = a;
        }
        a.setContentView(root);
    }

    private static Button button(Context c, String label, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(label);
        b.setTextSize(12.5f);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.parseColor("#2E7D32"));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        int m = Math.round(c.getResources().getDisplayMetrics().density * 4);
        p.setMargins(m, 0, m, 0);
        b.setLayoutParams(p);
        return b;
    }

    /** ActivityManager থেকে মোট RAM (প্রতিফলন ছাড়াই, সাধারণ কল)। */
    private static final class ActivityManagerCompat {
        final String line;

        ActivityManagerCompat(Context c) {
            String s = null;
            try {
                android.app.ActivityManager am = (android.app.ActivityManager)
                        c.getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    android.app.ActivityManager.MemoryInfo mi =
                            new android.app.ActivityManager.MemoryInfo();
                    am.getMemoryInfo(mi);
                    long mb = 1024L * 1024L;
                    s = "ডিভাইস RAM: মোট " + (mi.totalMem / mb) + " MB, মুক্ত "
                            + (mi.availMem / mb) + " MB"
                            + (mi.lowMemory ? " (কম!)" : "");
                }
            } catch (Throwable ignored) {
            }
            line = s;
        }
    }
}

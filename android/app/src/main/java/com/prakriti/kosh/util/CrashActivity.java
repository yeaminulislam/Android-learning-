package com.prakriti.kosh.util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * ক্র্যাশের পর যে পর্দা দেখা যায় — পূর্ণ স্ট্যাক-ট্রেস, ডিভাইসের তথ্য ও
 * কপি/শেয়ার/আবার-খোলার বোতাম।
 *
 * আলাদা অ্যাক্টিভিটি হিসেবে রাখার কারণ: যে প্রক্রিয়ায় ক্র্যাশ হয়েছে সেটি মারা যাওয়ার
 * আগেই এই পর্দাটি নতুন টাস্ক হিসেবে উঠে যায়, ফলে ব্যবহারকারী এক ঝলক দেখেই অ্যাপ
 * বন্ধ হয়ে যাওয়ার বদলে **কেন বন্ধ হলো** পড়তে ও পাঠাতে পারেন।
 */
public class CrashActivity extends Activity {

    public static final String EXTRA_REPORT = "report";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Intent i = getIntent();
        String report = i == null ? null : i.getStringExtra(EXTRA_REPORT);
        if (report == null || report.length() == 0) {
            String last = CrashGuard.lastReport();
            report = (last == null || last.length() == 0)
                    ? CrashGuard.report(this, Thread.currentThread(),
                            new RuntimeException("প্রতিবেদন পাওয়া যায়নি"))
                    : last;
        }
        setContentView(build(report));
    }

    private View build(final String report) {
        final float d = getResources().getDisplayMetrics().density;
        int pad = Math.round(16 * d);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#12200F"));
        root.setPadding(pad, pad, pad, pad);

        TextView head = new TextView(this);
        head.setText("⚠ অ্যাপটি থেমে গেছে");
        head.setTextColor(Color.WHITE);
        head.setTextSize(19f);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(head);

        TextView sub = new TextView(this);
        sub.setText("নিচের লেখাটি কপি বা শেয়ার করে পাঠিয়ে দিন — এটি দেখে সমস্যাটি "
                + "ঠিক করা যাবে। স্ক্রিনশট নিলেও হবে।\n\n");
        sub.setTextColor(Color.parseColor("#B9D6BE"));
        sub.setTextSize(13f);
        sub.setPadding(0, Math.round(pad / 2f), 0, pad / 2);
        root.addView(sub);

        TextView body = new TextView(this);
        body.setText(report);
        body.setTextColor(Color.parseColor("#E8F5E9"));
        body.setTextSize(10f);
        body.setTypeface(Typeface.MONOSPACE);
        body.setMovementMethod(new ScrollingMovementMethod());
        body.setBackgroundColor(Color.parseColor("#0B1609"));
        body.setPadding(pad, pad, pad, pad);

        ScrollView sc = new ScrollView(this);
        sc.addView(body, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(sc, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, pad, 0, 0);
        row.addView(btn("📋 কপি", d, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.ClipboardManager cm = (android.content.ClipboardManager)
                        getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(android.content.ClipData.newPlainText(
                            "crash", report));
                    toast("কপি হয়েছে");
                }
            }
        }));
        row.addView(btn("⤴ শেয়ার", d, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent s = new Intent(Intent.ACTION_SEND);
                s.setType("text/plain");
                s.putExtra(Intent.EXTRA_TEXT, report);
                try {
                    startActivity(Intent.createChooser(s, "ক্র্যাশ প্রতিবেদন"));
                } catch (Exception e) {
                    toast("শেয়ার করা যায়নি");
                }
            }
        }));
        row.addView(btn("↻ আবার খুলুন", d, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent l = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (l != null) {
                    l.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(l);
                }
                finish();
            }
        }));
        row.addView(btn("✕ বন্ধ", d, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAffinityCompat();
            }
        }));
        root.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView foot = new TextView(this);
        foot.setText("প্রতিবেদনটি filesDir/crash/ ফোল্ডারেও লেখা হয়েছে।");
        foot.setTextColor(Color.parseColor("#7FA586"));
        foot.setTextSize(10.5f);
        foot.setPadding(0, pad / 2, 0, 0);
        root.addView(foot);
        return root;
    }

    private Button btn(String label, float d, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(11.5f);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.parseColor("#2E7D32"));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        int m = Math.round(3 * d);
        p.setMargins(m, 0, m, 0);
        b.setLayoutParams(p);
        return b;
    }

    private void finishAffinityCompat() {
        try {
            finishAffinity();
        } catch (Throwable t) {
            finish();
        }
    }

    private void toast(String m) {
        android.widget.Toast.makeText(this, m, android.widget.Toast.LENGTH_SHORT).show();
    }
}

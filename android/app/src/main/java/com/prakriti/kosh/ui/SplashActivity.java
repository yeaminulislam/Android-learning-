package com.prakriti.kosh.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.prakriti.kosh.PrakritiApp;
import com.prakriti.kosh.R;
import com.prakriti.kosh.db.DatabaseManager;
import com.prakriti.kosh.util.Ui;

/**
 * স্প্ল্যাশ — অ্যাপ খোলার সঙ্গে সঙ্গে ডেটাবেস প্রস্তুত করে, তারপর পরের পর্দায় যায়।
 *
 * প্রথমবার ৭৬ মেগাবাইট সংরক্ষণাগার খুলতে কয়েক সেকেন্ড লাগে; তখন অগ্রগতি দেখানো
 * হয়। পরেরবার সঙ্গে সঙ্গেই খোলে।
 */
public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        build();
        start();
    }

    private TextView label;
    private android.widget.ProgressBar bar;

    private void build() {
        LinearLayout root = Ui.vbox(this);
        root.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Ui.color(this, R.color.green_dark), Ui.color(this, R.color.green_accent)});
        root.setBackground(bg);

        // বৃত্তাকার লোগো
        TextView logo = new TextView(this);
        logo.setText("\ud83c\udf3f");
        logo.setTextSize(52f);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(Ui.oval(Ui.withAlpha(Color.WHITE, 40)));
        root.addView(logo, Ui.lp(Ui.dp(this, 124), Ui.dp(this, 124)));

        TextView name = new TextView(this);
        name.setText(getString(R.string.app_name));
        name.setTextColor(Color.WHITE);
        name.setTextSize(27f);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setGravity(Gravity.CENTER);
        Ui.margins2(name, 0, Ui.dp(this, 18), 0, 0);
        root.addView(name);

        TextView tag = new TextView(this);
        tag.setText(getString(R.string.app_tagline));
        tag.setTextColor(Ui.withAlpha(Color.WHITE, 215));
        tag.setTextSize(14f);
        tag.setGravity(Gravity.CENTER);
        Ui.pad(tag, Ui.dp(this, 40), Ui.dp(this, 8), Ui.dp(this, 40), 0);
        root.addView(tag);

        label = new TextView(this);
        label.setText(getString(R.string.prep_first_run));
        label.setTextColor(Ui.withAlpha(Color.WHITE, 200));
        label.setTextSize(13f);
        label.setGravity(Gravity.CENTER);
        Ui.margins2(label, Ui.dp(this, 40), Ui.dp(this, 40), Ui.dp(this, 40), 0);
        root.addView(label);

        bar = new android.widget.ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        Ui.margins2(bar, Ui.dp(this, 56), Ui.dp(this, 10), Ui.dp(this, 56), 0);
        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Ui.dp(this, 5)));

        TextView ver = new TextView(this);
        ver.setText("সম্পূর্ণ অফলাইন · " + Ui.humanCount(250000) + " প্রজাতি");
        ver.setTextColor(Ui.withAlpha(Color.WHITE, 150));
        ver.setTextSize(11.5f);
        ver.setGravity(Gravity.CENTER);
        Ui.margins2(ver, 0, Ui.dp(this, 26), 0, 0);
        root.addView(ver);

        setContentView(root);
    }

    private void start() {
        final PrakritiApp app = PrakritiApp.get();
        final boolean firstRun = !DatabaseManager.isReady(this);
        if (!firstRun) {
            label.setText("খোলা হচ্ছে…");
            bar.setProgress(90);
        }
        app.ensureReady(new DatabaseManager.Progress() {
            @Override
            public void onProgress(final int percent, final String stage) {
                app.post(new Runnable() {
                    @Override
                    public void run() {
                        bar.setProgress(percent);
                        label.setText(stage + " (" + Ui.bnDigits(percent) + "%)");
                    }
                });
            }
        }, new PrakritiApp.Ready() {
            @Override
            public void onReady(boolean ok, String error) {
                if (!ok) {
                    label.setText(getString(R.string.prep_failed) + "\n" + error);
                    bar.setProgress(0);
                    return;
                }
                boolean seen = app.prefs().prefBool("intro_seen", false);
                Intent next = seen
                        ? new Intent(SplashActivity.this, MainActivity.class)
                        : new Intent(SplashActivity.this, SetupActivity.class);
                startActivity(next);
                finish();
            }
        });
    }
}

package com.prakriti.kosh.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.prakriti.kosh.PrakritiApp;
import com.prakriti.kosh.R;
import com.prakriti.kosh.util.Ui;

/**
 * প্রথমবারের পরিচয় — তিনটি পাতা পাশে সরিয়ে দেখা যায়, তারপর মূল পর্দা।
 *
 * বাইরের কোনো লাইব্রেরি নেই, তাই ViewPager-এর বদলে HorizontalScrollView আর
 * ছোট স্ন্যাপ-লজিক ব্যবহার করা হয়েছে।
 */
public class SetupActivity extends Activity {

    private static final String[] EMOJI = {
            "\ud83c\udf3f", "\ud83d\uddc2", "\ud83d\udd0e",
    };
    private static final String[] TITLE = {
            "ইন্টারনেট ছাড়াই পুরো পৃথিবী",
            "১২টি বিভাগ, ধাপে ধাপে",
            "নাম লিখলেই খুঁজে পাবেন",
    };
    private static final String[] BODY = {
            "প্রায় দুই লক্ষ পঞ্চাশ হাজার প্রজাতির তথ্য অ্যাপের ভেতরেই সংরক্ষিত।"
                    + " একবার প্রস্তুত হওয়ার পর কখনো নেট বা মোবাইল ডেটা লাগবে না।",
            "প্রধান বিভাগ থেকে শ্রেণি, বর্গ, পরিবার — তারপর প্রজাতির তালিকা। প্রতিটি"
                    + " প্রজাতির বাসস্থান, খাদ্য, প্রজনন, আকার ও বিষাক্ততা আলাদা করে লেখা।",
            "বাংলা, ইংরেজি বা বৈজ্ঞানিক — যে নামেই লিখুন, পূর্ণ-পাঠ সূচক (FTS5) মিলিয়ে"
                    + " দেবে। বিপন্ন ও বিলুপ্ত প্রজাতি আলাদা করেও ঘোরা যাবে।",
    };

    private HorizontalScrollView pager;
    private LinearLayout track;
    private LinearLayout dots;
    private TextView next;
    private int page;
    private int pageWidth = 1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable snap = new Runnable() {
        @Override
        public void run() {
            int target = Math.round(pager.getScrollX() / (float) pageWidth);
            target = Math.max(0, Math.min(TITLE.length - 1, target));
            if (target != page) {
                page = target;
                updateDots();
                updateNext();
            }
            pager.smoothScrollTo(target * pageWidth, 0);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = Ui.vbox(this);
        root.setBackgroundColor(Ui.color(this, R.color.bg));

        TextView skip = Ui.text(this, getString(R.string.skip), 13f,
                Ui.color(this, R.color.text_muted), false);
        skip.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        Ui.pad(skip, Ui.dp(this, 22), Ui.dp(this, 18), Ui.dp(this, 22), Ui.dp(this, 8));
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go();
            }
        });
        root.addView(skip, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        pager = new HorizontalScrollView(this);
        pager.setHorizontalScrollBarEnabled(false);
        pager.setSmoothScrollingEnabled(true);
        track = Ui.hbox(this);
        pager.addView(track, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(pager, Ui.lpw(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        pager.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int x, int y, int oldX, int oldY) {
                handler.removeCallbacks(snap);
                handler.postDelayed(snap, 160);
            }
        });

        dots = Ui.hbox(this);
        dots.setGravity(Gravity.CENTER);
        Ui.pad(dots, 0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        root.addView(dots, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        next = Ui.text(this, getString(R.string.next), 15f, Color.WHITE, true);
        next.setGravity(Gravity.CENTER);
        next.setBackground(Ui.pill(Ui.color(this, R.color.green_primary), this));
        Ui.margins2(next, Ui.dp(this, 28), Ui.dp(this, 6), Ui.dp(this, 28), Ui.dp(this, 26));
        Ui.pad(next, Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 20), Ui.dp(this, 14));
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (page >= TITLE.length - 1) {
                    go();
                } else {
                    page++;
                    pager.smoothScrollTo(page * pageWidth, 0);
                    updateDots();
                    updateNext();
                }
            }
        });
        root.addView(next, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
        buildPages();
        updateDots();
        updateNext();
    }

    private void buildPages() {
        track.removeAllViews();
        pageWidth = Math.max(1, getResources().getDisplayMetrics().widthPixels);
        for (int i = 0; i < TITLE.length; i++) {
            track.addView(pageView(i), Ui.lp(pageWidth, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    private View pageView(int index) {
        LinearLayout box = Ui.vbox(this);
        box.setGravity(Gravity.CENTER);
        Ui.pad(box, Ui.dp(this, 30), Ui.dp(this, 24), Ui.dp(this, 30), Ui.dp(this, 24));

        TextView emoji = new TextView(this);
        emoji.setText(EMOJI[index]);
        emoji.setTextSize(56f);
        emoji.setGravity(Gravity.CENTER);
        GradientDrawable circle = Ui.oval(Ui.color(this, R.color.surface_alt));
        emoji.setBackground(circle);
        box.addView(emoji, Ui.lp(Ui.dp(this, 134), Ui.dp(this, 134)));

        TextView t = Ui.text(this, TITLE[index], 22f,
                Ui.color(this, R.color.text_primary), true);
        t.setGravity(Gravity.CENTER);
        t.setLineSpacing(Ui.dp(this, 3), 1f);
        Ui.margins2(t, 0, Ui.dp(this, 26), 0, 0);
        box.addView(t);

        TextView body = Ui.text(this, BODY[index], 14.5f,
                Ui.color(this, R.color.text_secondary), false);
        body.setGravity(Gravity.CENTER);
        body.setLineSpacing(Ui.dp(this, 5), 1.15f);
        Ui.margins2(body, 0, Ui.dp(this, 14), 0, 0);
        box.addView(body);
        return box;
    }

    private void updateDots() {
        dots.removeAllViews();
        for (int i = 0; i < TITLE.length; i++) {
            View d = new View(this);
            d.setBackground(Ui.oval(i == page
                    ? Ui.color(this, R.color.green_primary)
                    : Ui.color(this, R.color.outline)));
            int size = i == page ? Ui.dp(this, 9) : Ui.dp(this, 7);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(size, size);
            p.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
            dots.addView(d, p);
        }
    }

    private void updateNext() {
        next.setText(page >= TITLE.length - 1
                ? getString(R.string.get_started) : getString(R.string.next));
    }

    private void go() {
        PrakritiApp app = PrakritiApp.get();
        if (app != null) app.prefs().setPrefBool("intro_seen", true);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}

package com.prakriti.kosh.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.prakriti.kosh.PrakritiApp;
import com.prakriti.kosh.R;
import com.prakriti.kosh.db.DatabaseManager;
import com.prakriti.kosh.db.Repository;
import com.prakriti.kosh.db.UserStore;
import com.prakriti.kosh.util.Ui;

/**
 * সব স্ক্রিনের ভিত্তি: টুলবার, প্রথমবারের ডেটাবেস-প্রস্তুতির পর্দা,
 * ব্যাকগ্রাউন্ড কাজ ও চিপ-বার।
 *
 * কোনো layout XML নেই — পুরো UI কোড থেকে তৈরি হয় (অ্যাপে কোনো লাইব্রেরি নেই)।
 */
public abstract class BaseActivity extends Activity {

    /** ব্যাকগ্রাউন্ড কাজ। */
    public interface Work {
        void run() throws Exception;
    }

    /** কাজ শেষে প্রধান থ্রেডে কলব্যাক। */
    public interface Done {
        void onResult(boolean ok, String error);
    }

    protected final Repository repo = Repository.get();
    protected final UserStore user = UserStore.get();
    protected PrakritiApp app;

    private LinearLayout root;
    private LinearLayout toolbar;
    private LinearLayout actions;
    private TextView titleView;
    private TextView subtitleView;
    private ImageView backButton;
    private FrameLayout content;
    private View progressOverlay;
    private TextView progressLabel;
    private ProgressBar progressBar;
    private HorizontalScrollView chipScroll;
    private LinearLayout chipBar;

    private int toolbarColor = -1;
    private boolean dbReady;
    /** onCreate ব্যর্থ হলে true — পরের ধাপগুলো আর ভাঙা ভিউ নিয়ে চলে না। */
    protected boolean uiBroken = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    try {
            app = PrakritiApp.get();
            if (app == null) app = (PrakritiApp) getApplication();

            root = Ui.vbox(this);
            root.setBackgroundColor(Ui.color(this, R.color.bg));

            toolbar = Ui.hbox(this);
            toolbar.setBackgroundColor(Ui.color(this, R.color.green_primary));
            Ui.pad(toolbar, Ui.dp(this, 6), statusBarPad() + Ui.dp(this, 6),
                    Ui.dp(this, 10), Ui.dp(this, 10));
            root.addView(toolbar, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            backButton = new ImageView(this);
            backButton.setImageDrawable(arrowDrawable(Color.WHITE));
            Ui.pad(backButton, Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10));
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
            toolbar.addView(backButton, Ui.lp(Ui.dp(this, 40), Ui.dp(this, 40)));

            LinearLayout titles = Ui.vbox(this);
            toolbar.addView(titles, Ui.lpw(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            titleView = new TextView(this);
            titleView.setTextColor(Color.WHITE);
            titleView.setTextSize(18.5f);
            titleView.setTypeface(Typeface.DEFAULT_BOLD);
            titleView.setSingleLine(true);
            titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            titles.addView(titleView);

            subtitleView = new TextView(this);
            subtitleView.setTextColor(Ui.withAlpha(Color.WHITE, 205));
            subtitleView.setTextSize(12f);
            subtitleView.setSingleLine(true);
            subtitleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            subtitleView.setVisibility(View.GONE);
            titles.addView(subtitleView);

            actions = Ui.hbox(this);
            toolbar.addView(actions, Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            chipScroll = new HorizontalScrollView(this);
            chipScroll.setHorizontalScrollBarEnabled(false);
            chipScroll.setBackgroundColor(Ui.color(this, R.color.surface));
            chipBar = Ui.hbox(this);
            Ui.pad(chipBar, Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
            chipScroll.addView(chipBar);
            chipScroll.setVisibility(View.GONE);
            root.addView(chipScroll, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            content = new FrameLayout(this);
            root.addView(content, Ui.lpw(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            progressOverlay = Ui.vbox(this);
            progressOverlay.setBackgroundColor(Ui.color(this, R.color.green_dark));
            progressOverlay.setClickable(true);
            progressOverlay.setVisibility(View.GONE);
            ((LinearLayout) progressOverlay).setGravity(Gravity.CENTER);

            TextView logo = new TextView(this);
            logo.setText("\ud83c\udf3f");
            logo.setTextSize(44f);
            logo.setGravity(Gravity.CENTER);
            ((LinearLayout) progressOverlay).addView(logo);

            TextView appName = new TextView(this);
            appName.setText(getString(R.string.app_name));
            appName.setTextColor(Color.WHITE);
            appName.setTextSize(24f);
            appName.setTypeface(Typeface.DEFAULT_BOLD);
            appName.setGravity(Gravity.CENTER);
            Ui.margins2(appName, 0, Ui.dp(this, 6), 0, 0);
            ((LinearLayout) progressOverlay).addView(appName);

            progressLabel = new TextView(this);
            progressLabel.setText(getString(R.string.prep_first_run));
            progressLabel.setTextColor(Ui.withAlpha(Color.WHITE, 210));
            progressLabel.setTextSize(13.5f);
            progressLabel.setGravity(Gravity.CENTER);
            Ui.margins2(progressLabel, Ui.dp(this, 28), Ui.dp(this, 18), Ui.dp(this, 28), 0);
            ((LinearLayout) progressOverlay).addView(progressLabel);

            progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setMax(100);
            Ui.margins2(progressBar, Ui.dp(this, 40), Ui.dp(this, 10), Ui.dp(this, 40), 0);
            ((LinearLayout) progressOverlay).addView(progressBar,
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            Ui.dp(this, 6)));

            TextView sizeHint = new TextView(this);
            sizeHint.setTextColor(Ui.withAlpha(Color.WHITE, 165));
            sizeHint.setTextSize(11.5f);
            sizeHint.setGravity(Gravity.CENTER);
            Ui.margins2(sizeHint, Ui.dp(this, 28), Ui.dp(this, 8), Ui.dp(this, 28), 0);
            ((LinearLayout) progressOverlay).addView(sizeHint);

            root.addView(progressOverlay, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            setContentView(root);
            long assetMb = DatabaseManager.assetSize(this) / (1024L * 1024L);
            sizeHint.setText("প্রথমবার শুধু একবার — " + Ui.bnDigits(assetMb)
                    + " মেগাবাইট সংরক্ষণাগার খোলা হবে। ইন্টারনেট লাগবে না।");
    } catch (Throwable t) {
        uiBroken = true;
        com.prakriti.kosh.util.CrashGuard.guard(this, "onCreate", t);
    }
    }

    private int statusBarPad() {
        return 0;   // status bar থিমের রঙে আঁকা হয়
    }

    /** বাম তীরের আইকন — সরাসরি Canvas-এ আঁকা (ড্রয়েবল রিসোর্স লাগে না)। */
    protected static Drawable arrowDrawable(int color) {
        return new BackArrow(color);
    }

    /** সহজ Canvas-আঁকা বাম তীর। */
    private static final class BackArrow extends android.graphics.drawable.Drawable {
        private final android.graphics.Paint p = new android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG);

        BackArrow(int color) {
            p.setColor(color);
            p.setStyle(android.graphics.Paint.Style.STROKE);
            p.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            p.setStrokeJoin(android.graphics.Paint.Join.ROUND);
        }

        @Override
        public void draw(android.graphics.Canvas c) {
            android.graphics.Rect b = getBounds();
            float cx = b.centerX(), cy = b.centerY();
            float u = Math.min(b.width(), b.height()) * 0.5f;
            p.setStrokeWidth(u * 0.18f);
            c.drawLine(cx - u * 0.34f, cy, cx + u * 0.34f, cy, p);
            c.drawLine(cx - u * 0.34f, cy, cx - u * 0.02f, cy - u * 0.32f, p);
            c.drawLine(cx - u * 0.34f, cy, cx - u * 0.02f, cy + u * 0.32f, p);
        }

        @Override
        public void setAlpha(int a) {
            p.setAlpha(a);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter f) {
            p.setColorFilter(f);
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }

    // ------------------------------------------------------------ টুলবার

    public void setToolbar(String title) {
        setToolbar(title, null);
    }

    public void setToolbar(String title, String subtitle) {
        if (uiBroken || titleView == null) return;
        titleView.setText(title == null ? "" : title);
        if (subtitle == null || subtitle.length() == 0) {
            subtitleView.setVisibility(View.GONE);
        } else {
            subtitleView.setText(subtitle);
            subtitleView.setVisibility(View.VISIBLE);
        }
    }

    public void setToolbarColor(int color) {
        if (uiBroken || toolbar == null) return;
        toolbarColor = color;
        toolbar.setBackgroundColor(color);
        int on = Ui.readableOn(color);
        titleView.setTextColor(on);
        subtitleView.setTextColor(Ui.withAlpha(on, 205));
        backButton.setImageDrawable(new BackArrow(on));
        for (int i = 0; i < actions.getChildCount(); i++) {
            View v = actions.getChildAt(i);
            if (v instanceof ImageView) ((ImageView) v).setImageTintList(
                    android.content.res.ColorStateList.valueOf(on));
        }
    }

    public void showBack(boolean show) {
        if (uiBroken || backButton == null) return;
        backButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /** টুলবারের সব অ্যাকশন মুছে দেয় (স্ক্রিন নতুন করে সাজাতে)। */
    public void clearToolbarActions() {
        if (uiBroken || actions == null) return;
        actions.removeAllViews();
    }

    /** টুলবারে টেক্সট-অ্যাকশন যোগ করে। */
    public TextView addToolbarAction(String label, View.OnClickListener l) {
        if (uiBroken || actions == null) return null;
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(toolbarColor == -1 ? Color.WHITE : Ui.readableOn(toolbarColor));
        t.setTextSize(13.5f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        Ui.pad(t, Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        t.setBackground(Ui.pill(Ui.withAlpha(Color.WHITE, 46), this));
        t.setOnClickListener(l);
        actions.addView(t, Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return t;
    }

    /** টুলবারে খোঁজার আইকন (\ud83d\udd0e) যোগ করে। */
    public void addSearchAction() {
        if (uiBroken || actions == null) return;
        TextView t = new TextView(this);
        t.setText("\ud83d\udd0e");
        t.setTextSize(16f);
        t.setGravity(Gravity.CENTER);
        Ui.pad(t, Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6));
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(BaseActivity.this, SearchActivity.class));
            }
        });
        actions.addView(t, Ui.lp(Ui.dp(this, 40), Ui.dp(this, 40)));
    }

    /** টুলবারে সেটিংস আইকন যোগ করে। */
    public void addSettingsAction() {
        if (uiBroken || actions == null) return;
        TextView t = new TextView(this);
        t.setText("⚙");
        t.setTextSize(17f);
        t.setGravity(Gravity.CENTER);
        Ui.pad(t, Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6));
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(BaseActivity.this, SettingsActivity.class));
            }
        });
        actions.addView(t, Ui.lp(Ui.dp(this, 40), Ui.dp(this, 40)));
    }

    /** স্ক্রিনের মূল কন্টেন্ট বসায়। */
    public void setContent(View v) {
        if (uiBroken || content == null) return;
        content.removeAllViews();
        if (v != null) {
            content.addView(v, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    protected FrameLayout contentFrame() {
        return content;
    }

    // ------------------------------------------------------------ চিপ বার

    public void clearChips() {
        if (uiBroken || chipBar == null) return;
        chipBar.removeAllViews();
        chipScroll.setVisibility(chipBar.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    /** একটি চিপ যোগ করে; শেষে চিপ-বার দেখায়। */
    public TextView addChip(String label, boolean selected, View.OnClickListener l) {
        if (uiBroken || chipBar == null) return null;
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(12.5f);
        t.setSingleLine(true);
        int bg = selected ? Ui.color(this, R.color.green_primary)
                : Ui.color(this, R.color.surface_alt);
        int fg = selected ? Ui.readableOn(bg) : Ui.color(this, R.color.text_secondary);
        t.setTextColor(fg);
        t.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        t.setBackground(Ui.pill(selected ? bg : Color.WHITE, this));
        if (!selected) {
            ((GradientDrawable) t.getBackground())
                    .setStroke(Ui.dp(this, 1), Ui.color(this, R.color.outline));
        }
        Ui.pad(t, Ui.dp(this, 14), Ui.dp(this, 7), Ui.dp(this, 14), Ui.dp(this, 7));
        Ui.margins2(t, 0, 0, Ui.dp(this, 8), 0);
        t.setOnClickListener(l);
        chipBar.addView(t);
        chipScroll.setVisibility(View.VISIBLE);
        return t;
    }

    // ------------------------------------------------------------ ডেটাবেস

    /** ডেটাবেস প্রস্তুত হলে onDbReady ডাকা হয় (প্রয়োজনে প্রগ্রেস দেখিয়ে)। */
    protected void onDbReady() {
    }

    protected final void requireDb() {
        if (uiBroken) return;
        if (app == null) {
            app = PrakritiApp.get();
            if (app == null) app = (PrakritiApp) getApplication();
        }
        if (app.isReady() || DatabaseManager.isReady(this)) {
            dbReady = true;
            onDbReady();
            return;
        }
        showPreparing(true);
        app.ensureReady(new DatabaseManager.Progress() {
            @Override
            public void onProgress(final int percent, final String stage) {
                app.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(percent);
                        progressLabel.setText(stage + " (" + Ui.bnDigits(percent) + "%)");
                    }
                });
            }
        }, new PrakritiApp.Ready() {
            @Override
            public void onReady(boolean ok, String error) {
                showPreparing(false);
                if (ok) {
                    dbReady = true;
                    onDbReady();
                } else {
                    toast(getString(R.string.prep_failed) + "\n" + error);
                    finish();
                }
            }
        });
    }

    private void showPreparing(boolean show) {
        progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /** প্রগ্রেস-ওভারলে ব্যবহার করা প্রগ্রেস কলব্যাক (যেমন ডেটাবেস আবার বানাতে)। */
    protected DatabaseManager.Progress progressOverlay() {
        showPreparing(true);
        return new DatabaseManager.Progress() {
            @Override
            public void onProgress(final int percent, final String stage) {
                app.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(percent);
                        progressLabel.setText(stage + " (" + Ui.bnDigits(percent) + "%)");
                        if (percent >= 100) showPreparing(false);
                    }
                });
            }
        };
    }

    protected boolean isDbReady() {
        return dbReady;
    }

    // ------------------------------------------------------------ সহায়ক

    /** ব্যাকগ্রাউন্ডে কাজ, শেষে প্রধান থ্রেডে ফলাফল। */
    protected void async(final Work work, final Done done) {
        app.async(new Runnable() {
            @Override
            public void run() {
                boolean ok = true;
                String err = "";
                try {
                    work.run();
                } catch (Throwable t) {
                    ok = false;
                    err = t.getClass().getSimpleName() + ": "
                            + (t.getMessage() == null ? "" : t.getMessage());
                }
                final boolean okF = ok;
                final String errF = err;
                app.post(new Runnable() {
                    @Override
                    public void run() {
                        if (done != null) done.onResult(okF, errF);
                    }
                });
            }
        });
    }

    public void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /** শূন্য-তালিকার বার্তা দেখানোর সাধারণ ভিউ। */
    protected View emptyView(String message) {
        LinearLayout box = Ui.vbox(this);
        box.setGravity(Gravity.CENTER);
        TextView emoji = new TextView(this);
        emoji.setText("\ud83c\udf43");
        emoji.setTextSize(40f);
        emoji.setGravity(Gravity.CENTER);
        box.addView(emoji);
        TextView t = Ui.text(this, message, 15f, Ui.color(this, R.color.text_secondary), false);
        t.setGravity(Gravity.CENTER);
        Ui.pad(t, Ui.dp(this, 32), Ui.dp(this, 12), Ui.dp(this, 32), 0);
        box.addView(t);
        return box;
    }

    /** কার্ডের মতো পটভূমি + ছায়া। */
    protected View card(View child, int padDp) {
        FrameLayout f = new FrameLayout(this);
        f.setBackground(Ui.card(this));
        Ui.elevate(f, Ui.dp(this, 1.5f));
        Ui.pad(f, Ui.dp(this, padDp), Ui.dp(this, padDp), Ui.dp(this, padDp),
                Ui.dp(this, padDp));
        f.addView(child, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return f;
    }

    protected void copyToClipboard(String text) {
        android.content.ClipboardManager cm =
                (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(android.content.ClipData.newPlainText("species", text));
            toast(getString(R.string.copied));
        }
    }

    protected void share(String text) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, text);
        try {
            startActivity(Intent.createChooser(i, getString(R.string.app_name)));
        } catch (Exception e) {
            toast(getString(R.string.empty_list));
        }
    }
}

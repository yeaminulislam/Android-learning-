package com.prakriti.kosh.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.prakriti.kosh.PrakritiApp;
import com.prakriti.kosh.R;
import com.prakriti.kosh.db.DatabaseManager;
import com.prakriti.kosh.util.Ui;

/**
 * সেটিংস ও অ্যাপ-সম্পর্কে পর্দা — ডেটাবেসের তথ্য, পছন্দ/নোট পরিষ্কার,
 * আবার প্রস্তুত করা।
 */
public class SettingsActivity extends BaseActivity {

    private LinearLayout body;
    private int speciesCount;
    private int groupCount;
    private long fileSize;
    private int favCount;
    private int noteCount;

    @Override
    protected void onDbReady() {
        setToolbar(getString(R.string.title_settings), null);
        setToolbarColor(Ui.color(this, R.color.green_dark));
        build();
        loadInfo();
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.color(this, R.color.bg));
        body = Ui.vbox(this);
        Ui.pad(body, Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 28));
        scroll.addView(body, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setContent(scroll);
    }

    private void loadInfo() {
        async(new Work() {
            @Override
            public void run() {
                speciesCount = repo.countAll();
                groupCount = repo.groups().size();
                favCount = user.favoriteCount();
                noteCount = user.noteCount();
                fileSize = DatabaseManager.dbFile(SettingsActivity.this).length();
            }
        }, new Done() {
            @Override
            public void onResult(boolean ok, String error) {
                render();
            }
        });
    }

    private void render() {
        body.removeAllViews();

        // ডেটাবেস
        body.addView(section(getString(R.string.settings_database)));
        body.addView(row("\ud83d\udcda", getString(R.string.settings_total_species),
                Ui.humanCount(speciesCount), null));
        body.addView(row("\ud83d\uddc2", "প্রধান বিভাগ", Ui.bnDigits(groupCount), null));
        body.addView(row("\ud83d\udcbe", "ডেটাবেসের আকার", humanSize(fileSize), null));
        body.addView(row("\ud83d\udd0e", getString(R.string.settings_fts),
                getString(R.string.settings_fts_sub), null));
        body.addView(row("\ud83d\udd01", getString(R.string.settings_rebuild),
                getString(R.string.settings_rebuild_sub), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmRebuild();
                    }
                }));
        body.addView(row("\ud83e\ude7a", "রোগ-নির্ণয়",
                "ডিভাইস, ডেটাবেস ও অনুসন্ধান পরীক্ষা — সমস্যা হলে এখান থেকে প্রতিবেদন পাঠান",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new android.content.Intent(SettingsActivity.this,
                                DiagnosticsActivity.class));
                    }
                }));

        // আমার ডেটা
        body.addView(section(getString(R.string.settings_my_data)));
        body.addView(row("❤", getString(R.string.title_favorites),
                Ui.bnDigits(favCount) + "টি প্রজাতি", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SpeciesListActivity.startFavorites(v.getContext(),
                                getString(R.string.title_favorites),
                                Ui.color(SettingsActivity.this, R.color.green_accent));
                    }
                }));
        body.addView(row("\ud83d\udcdd", "আমার নোট", Ui.bnDigits(noteCount) + "টি নোট",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showNotes();
                    }
                }));
        body.addView(row("\ud83e\uddf9", getString(R.string.settings_reset_favorites), null,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirm(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                async(new Work() {
                                    @Override
                                    public void run() {
                                        user.clearFavorites();
                                    }
                                }, new Done() {
                                    @Override
                                    public void onResult(boolean ok, String error) {
                                        toast(ok ? "পছন্দের তালিকা খালি করা হয়েছে" : error);
                                        loadInfo();
                                    }
                                });
                            }
                        }, getString(R.string.settings_confirm_reset), "এই কাজ ফেরানো যাবে না।");
                    }
                }));
        body.addView(row("\ud83d\uddd1", getString(R.string.settings_reset_notes), null,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirm(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                async(new Work() {
                                    @Override
                                    public void run() {
                                        user.clearNotes();
                                    }
                                }, new Done() {
                                    @Override
                                    public void onResult(boolean ok, String error) {
                                        toast(ok ? "সব নোট মুছে ফেলা হয়েছে" : error);
                                        loadInfo();
                                    }
                                });
                            }
                        }, getString(R.string.settings_confirm_reset), "এই কাজ ফেরানো যাবে না।");
                    }
                }));

        // অ্যাপ সম্পর্কে
        body.addView(section(getString(R.string.settings_about)));
        body.addView(row("\ud83c\udf3f", getString(R.string.app_name),
                "সংস্করণ " + versionName(), null));
        body.addView(aboutCard());
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0";
        }
    }

    private View section(String title) {
        TextView t = Ui.text(this, title, 12.5f, Ui.color(this, R.color.green_accent), true);
        t.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.NORMAL));
        t.setLetterSpacing(0.02f);
        Ui.pad(t, Ui.dp(this, 6), Ui.dp(this, 20), Ui.dp(this, 6), Ui.dp(this, 8));
        return t;
    }

    private View row(String emoji, String title, String subtitle, View.OnClickListener l) {
        LinearLayout box = Ui.hbox(this);
        box.setBackground(Ui.card(this));
        box.setGravity(Gravity.CENTER_VERTICAL);
        Ui.elevate(box, Ui.dp(this, 0.8f));
        Ui.pad(box, Ui.dp(this, 14), Ui.dp(this, 13), Ui.dp(this, 14), Ui.dp(this, 13));
        Ui.margins(box, 0, 0, 0, Ui.dp(this, 8));

        TextView e = Ui.text(this, emoji, 19f, Ui.color(this, R.color.text_primary), false);
        box.addView(e);

        LinearLayout txt = Ui.vbox(this);
        Ui.margins(txt, Ui.dp(this, 12), 0, 0, 0);
        box.addView(txt, Ui.lpw(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView t = Ui.text(this, title, 14.5f, Ui.color(this, R.color.text_primary), true);
        txt.addView(t);
        if (subtitle != null) {
            TextView s = Ui.text(this, subtitle, 12f, Ui.color(this, R.color.text_muted), false);
            s.setLineSpacing(Ui.dp(this, 2), 1f);
            Ui.margins(s, 0, Ui.dp(this, 2), 0, 0);
            txt.addView(s);
        }
        if (l != null) {
            box.setOnClickListener(l);
            TextView arrow = Ui.text(this, "›", 22f, Ui.color(this, R.color.text_muted), false);
            Ui.margins(arrow, Ui.dp(this, 6), 0, 0, 0);
            box.addView(arrow);
        }
        return box;
    }

    private View aboutCard() {
        LinearLayout box = Ui.vbox(this);
        box.setBackground(Ui.roundedStroke(Ui.color(this, R.color.surface_alt),
                Ui.color(this, R.color.outline), 16, 1, this));
        Ui.pad(box, Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));

        TextView t = Ui.text(this, getString(R.string.settings_offline), 13f,
                Ui.color(this, R.color.text_secondary), false);
        t.setLineSpacing(Ui.dp(this, 4), 1f);
        box.addView(t);

        TextView t2 = Ui.text(this, "প্রযুক্তি: SQLite (FTS5 পূর্ণ-পাঠ সূচক) · "
                + "প্রোগ্রামে আঁকা ভেক্টর ছবি (প্রতিটি ৫–১০ কিলোবাইটের বদলে শূন্য বাইট "
                + "নেটওয়ার্ক খরচ) · কীসেট পেজিনেশন · ব্যাকগ্রাউন্ড থ্রেড।",
                12f, Ui.color(this, R.color.text_muted), false);
        t2.setLineSpacing(Ui.dp(this, 3), 1f);
        Ui.margins(t2, 0, Ui.dp(this, 10), 0, 0);
        box.addView(t2);
        return box;
    }

    private void showNotes() {
        async(new Work() {
            @Override
            public void run() {
                noteDump = user.allNotes();
            }
        }, new Done() {
            @Override
            public void onResult(boolean ok, String error) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("আমার নোট")
                        .setMessage(notesText())
                        .setPositiveButton("বন্ধ", null)
                        .setNeutralButton("কপি", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                copyToClipboard(notesText());
                            }
                        })
                        .show();
            }
        });
    }

    private java.util.Map<Long, String> noteDump;

    private String notesText() {
        StringBuilder sb = new StringBuilder();
        if (noteDump == null || noteDump.isEmpty()) return "কোনো নোট নেই।";
        for (java.util.Map.Entry<Long, String> e : noteDump.entrySet()) {
            sb.append("প্রজাতি #").append(Ui.bnDigits(e.getKey().intValue()))
                    .append(": ").append(e.getValue()).append("\n\n");
        }
        return sb.toString();
    }

    private void confirm(DialogInterface.OnClickListener yes, String title, String msg) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setNegativeButton("বাতিল", null)
                .setPositiveButton("হ্যাঁ", yes)
                .show();
    }

    private void confirmRebuild() {
        confirm(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) {
                PrakritiApp.get().resetDatabase();
                toast(getString(R.string.prep_start));
                PrakritiApp.get().ensureReady(progressOverlay(), null);
                recreate();
            }
        }, getString(R.string.settings_rebuild), getString(R.string.settings_rebuild_sub));
    }

    static String humanSize(long bytes) {
        if (bytes <= 0) return "—";
        double mb = bytes / (1024.0 * 1024.0);
        if (mb >= 1024) return String.format(java.util.Locale.US, "%.2f গিগাবাইট",
                mb / 1024.0);
        return String.format(java.util.Locale.US, "%.1f মেগাবাইট", mb);
    }
}

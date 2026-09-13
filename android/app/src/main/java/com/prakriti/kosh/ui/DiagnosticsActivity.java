package com.prakriti.kosh.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.prakriti.kosh.PrakritiApp;
import com.prakriti.kosh.R;
import com.prakriti.kosh.db.DatabaseManager;
import com.prakriti.kosh.util.CrashGuard;
import com.prakriti.kosh.util.Ui;

import java.io.File;
import java.util.List;

/**
 * রোগ-নির্ণয় পর্দা।
 *
 * কোনো সমস্যা দেখা দিলে ব্যবহারকারী যাতে নিজেই দেখতে ও পাঠাতে পারেন তার জন্য —
 * ডিভাইসের তথ্য, অ্যাপের অবস্থা, ডেটাবেস প্রস্তুত হয়েছে কি না, এবং কয়েকটি সরাসরি
 * পরীক্ষা (গণনা, পেজিনেশন, অনুসন্ধান, ছবি আঁকা)।
 */
public class DiagnosticsActivity extends BaseActivity {

    private TextView out;
    private final StringBuilder log = new StringBuilder();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (uiBroken) return;
        setToolbar("রোগ-নির্ণয়", "ডিভাইস ও অ্যাপের অবস্থা");
        showBack(true);
        clearToolbarActions();
        addToolbarAction("⤴ শেয়ার", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share(toStringReport());
            }
        });

        LinearLayout body = Ui.vbox(DiagnosticsActivity.this);
        Ui.pad(body, Ui.dp(DiagnosticsActivity.this, 14), Ui.dp(DiagnosticsActivity.this, 14),
                Ui.dp(DiagnosticsActivity.this, 14), Ui.dp(DiagnosticsActivity.this, 24));

        out = new TextView(DiagnosticsActivity.this);
        out.setTextSize(11f);
        out.setTypeface(android.graphics.Typeface.MONOSPACE);
        out.setTextColor(Ui.color(DiagnosticsActivity.this, R.color.text_primary));
        out.setBackgroundColor(Color.WHITE);
        Ui.pad(out, Ui.dp(DiagnosticsActivity.this, 12), Ui.dp(DiagnosticsActivity.this, 12),
                Ui.dp(DiagnosticsActivity.this, 12), Ui.dp(DiagnosticsActivity.this, 12));
        body.addView(out, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView sc = new ScrollView(DiagnosticsActivity.this);
        sc.addView(body, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setContent(sc);

        line(CrashGuard.deviceInfo(this));
        line(CrashGuard.appInfo(this));

        String boot = app == null ? "" : app.bootError();
        if (boot != null && boot.length() > 0) {
            line("[!] Application.onCreate-এ ত্রুটি হয়েছিল:\n" + boot + "\n"
                    + (app.bootTrace() == null ? "" : app.bootTrace()));
        }
        String last = CrashGuard.lastReport();
        if (last != null && last.length() > 0) {
            line("[!] শেষ ক্র্যাশ:\n" + last);
        }
        File[] saved = CrashGuard.savedReports(this);
        if (saved.length > 0) {
            line("[!] সংরক্ষিত ক্র্যাশ-ফাইল: " + saved.length + "টি — " + saved[0].getName());
        }

        if (DatabaseManager.isReady(this)) {
            async(new Work() {
                @Override
                public void run() {
                    runTests();
                }
            }, new Done() {
                @Override
                public void onResult(boolean ok, String error) {
                    if (!ok) line("[!] পরীক্ষা ব্যর্থ: " + error);
                    show();
                }
            });
        } else {
            line("ডেটাবেস এখনো প্রস্তুত নয় — নিচের বোতামে চেপে প্রস্তুত করুন।");
            show();
        }

        TextView run = Ui.text(this, "↻ আবার পরীক্ষা করুন", 14f, Color.WHITE, true);
        run.setGravity(android.view.Gravity.CENTER);
        run.setBackground(Ui.pill(Ui.color(this, R.color.green_primary), this));
        Ui.pad(run, Ui.dp(this, 18), Ui.dp(this, 13), Ui.dp(this, 18), Ui.dp(this, 13));
        Ui.margins2(run, 0, Ui.dp(this, 12), 0, 0);
        run.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log.setLength(0);
                out.setText("পরীক্ষা চলছে…");
                async(new Work() {
                    @Override
                    public void run() {
                        try {
                    app.ensureReady(null, null);
                } catch (Throwable ignored) {
                }
                        line(CrashGuard.deviceInfo(DiagnosticsActivity.this));
                        line(CrashGuard.appInfo(DiagnosticsActivity.this));
                        if (DatabaseManager.isReady(DiagnosticsActivity.this)) {
                            runTests();
                        } else {
                            line("[!] ডেটাবেস এখনো প্রস্তুত নয়");
                        }
                    }
                }, new Done() {
                    @Override
                    public void onResult(boolean ok, String error) {
                        if (!ok) line("[!] " + error);
                        show();
                    }
                });
            }
        });
        body.addView(run, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void show() {
        out.setText(log.toString());
    }

    private void line(String s) {
        log.append(s == null ? "" : s);
        if (!s.endsWith("\n")) log.append('\n');
    }

    private String toStringReport() {
        return "প্রকৃতি কোষ — রোগ-নির্ণয়\n" + log.toString();
    }

    // ------------------------------------------------------------ পরীক্ষাসমূহ

    private void runTests() {
        line("──────── পরীক্ষা ────────");
        test("ডেটাবেস খোলা", new T() {
            @Override
            public String run() {
                File f = DatabaseManager.dbFile(DiagnosticsActivity.this);
                repo.groups();
                return "ফাইল " + Ui.humanSize(f.length()) + ", বিভাগ "
                        + repo.groups().size() + "টি";
            }
        });
        test("মোট প্রজাতি গণনা", new T() {
            @Override
            public String run() {
                return Ui.bnDigits(repo.countOf(com.prakriti.kosh.db.Repository.MODE_ALL,
                        null, null, null, null)) + "টি";
            }
        });
        test("প্রথম পৃষ্ঠা (জনপ্রিয়)", new T() {
            @Override
            public String run() {
                List<com.prakriti.kosh.data.Species> items = repo.page(
                        com.prakriti.kosh.db.Repository.MODE_POPULAR,
                        null, null, null, null).items;
                if (items.isEmpty()) return "[!] খালি!";
                return items.size() + "টি — প্রথমটি: " + items.get(0).bnName;
            }
        });
        test("শ্রেণিবিন্যাস তালিকা", new T() {
            @Override
            public String run() {
                List<com.prakriti.kosh.data.Species> items = repo.page(
                        com.prakriti.kosh.db.Repository.MODE_GROUP,
                        "mammals", null, null, null).items;
                return items.size() + "টি (মোট স্তন্যপায়ী "
                        + Ui.bnDigits(repo.countOf(
                                com.prakriti.kosh.db.Repository.MODE_GROUP,
                                "mammals", null, null, null)) + ")";
            }
        });
        test("অনুসন্ধান (বাংলা)", new T() {
            @Override
            public String run() {
                List<com.prakriti.kosh.data.Species> r = repo.search("বাঘ", 5).items;
                return r.isEmpty() ? "[!] কিছু পাওয়া যায়নি"
                        : r.size() + "টি — " + r.get(0).bnName;
            }
        });
        test("অনুসন্ধান (ইংরেজি)", new T() {
            @Override
            public String run() {
                List<com.prakriti.kosh.data.Species> r = repo.search("Panthera", 5).items;
                return r.isEmpty() ? "[!] কিছু পাওয়া যায়নি"
                        : r.size() + "টি — " + r.get(0).sciName;
            }
        });
        test("ছবি আঁকা (৪৮০x৩২০)", new T() {
            @Override
            public String run() {
                List<com.prakriti.kosh.data.Species> items = repo.page(
                        com.prakriti.kosh.db.Repository.MODE_POPULAR,
                        null, null, null, null).items;
                if (items.isEmpty()) return "[!] প্রজাতি নেই";
                android.graphics.Bitmap b = com.prakriti.kosh.util.ArtView.render(
                        480, 320, items.get(0), 0, false);
                return b == null ? "[!] আঁকা যায়নি"
                        : b.getWidth() + "x" + b.getHeight() + " "
                                + Ui.humanSize(b.getByteCount());
            }
        });
        test("পছন্দ/নোট ডেটাবেস", new T() {
            @Override
            public String run() {
                return "পছন্দ " + user.favoriteCount() + "টি";
            }
        });
        line("──────── শেষ ────────");
    }

    private interface T {
        String run() throws Exception;
    }

    private void test(String name, T t) {
        long t0 = System.nanoTime();
        try {
            String r = t.run();
            line("✓ " + name + ": " + r + "  ("
                    + ((System.nanoTime() - t0) / 1000000) + " ms)");
        } catch (Throwable e) {
            line("✗ " + name + ": " + e.getClass().getSimpleName() + " "
                    + e.getMessage());
        }
    }
}

package com.prakriti.kosh.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.prakriti.kosh.R;
import com.prakriti.kosh.data.CategoryGroup;
import com.prakriti.kosh.db.Repository;
import com.prakriti.kosh.data.Species;
import com.prakriti.kosh.util.ArtView;
import com.prakriti.kosh.util.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * প্রথম পর্দা — ১২টি প্রধান বিভাগ, জনপ্রিয় প্রজাতি, বিপন্ন ও বিলুপ্ত তালিকা।
 */
public class MainActivity extends BaseActivity {

    private List<CategoryGroup> groups = new ArrayList<CategoryGroup>();
    private List<Species> popular = new ArrayList<Species>();
    private List<Species> threatened = new ArrayList<Species>();
    private int totalCount;
    private int extinctCount;
    private int venomCount;
    private int favoriteCount;

    @Override
    protected void onDbReady() {
        setToolbar(getString(R.string.app_name), getString(R.string.app_tagline));
        setToolbarColor(Ui.color(this, R.color.green_primary));
        showBack(false);
        // onResume-এ requireDb() আবার চলে — আইকন যেন ডুপ্লিকেট না হয়
        clearToolbarActions();
        addSearchAction();
        addSettingsAction();
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        requireDb();
    }

    private void load() {
        async(new Work() {
            @Override
            public void run() {
                groups = repo.groups();
                popular = repo.page(Repository.MODE_POPULAR, null, null, null, null).items;
                threatened = repo.page(Repository.MODE_THREATENED, null, null, null, null).items;
                totalCount = repo.countOf(Repository.MODE_ALL, null, null, null, null);
                extinctCount = repo.countOf(Repository.MODE_EXTINCT, null, null, null, null);
                venomCount = repo.countOf(Repository.MODE_VENOMOUS, null, null, null, null);
                favoriteCount = user.favoriteCount();
            }
        }, new Done() {
            @Override
            public void onResult(boolean ok, String error) {
                if (!ok) {
                    setContent(emptyView(getString(R.string.prep_failed) + "\n" + error));
                    return;
                }
                build();
            }
        });
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.color(this, R.color.bg));
        LinearLayout body = Ui.vbox(this);
        Ui.pad(body, Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 28));
        scroll.addView(body, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        body.addView(hero());
        body.addView(sectionTitle("দ্রুত প্রবেশ", null));
        body.addView(quickRow());
        body.addView(sectionTitle("১২টি প্রধান বিভাগ",
                Ui.humanCount(totalCount) + " প্রজাতি"));
        body.addView(groupGrid());
        body.addView(sectionTitle("সবচেয়ে পরিচিত", "জনপ্রিয়তার ক্রমে"));
        body.addView(strip(popular));
        body.addView(sectionTitle("বিপন্ন প্রজাতি", "সংরক্ষণ প্রয়োজন"));
        body.addView(strip(threatened));
        body.addView(footerNote());

        setContent(scroll);
    }

    // ------------------------------------------------------------ হিরো কার্ড

    private View hero() {
        FrameLayout card = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Ui.color(this, R.color.green_dark), Ui.color(this, R.color.green_accent)});
        bg.setCornerRadius(Ui.dp(this, 22));
        card.setBackground(bg);
        Ui.elevate(card, Ui.dp(this, 3f));
        Ui.pad(card, Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));

        // উপরের সারি: শিরোনাম + ডানে একটি নমুনা ছবি
        LinearLayout top = Ui.hbox(this);
        card.addView(top, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout text = Ui.vbox(this);
        top.addView(text, Ui.lpw(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t1 = Ui.text(this, "পৃথিবীর জীবজগৎ", 15f, Ui.withAlpha(Color.WHITE, 210), false);
        text.addView(t1);

        TextView t2 = Ui.text(this, Ui.humanCount(totalCount) + " প্রজাতি", 30f, Color.WHITE, true);
        Ui.margins(t2, 0, Ui.dp(this, 2), 0, 0);
        text.addView(t2);

        if (!popular.isEmpty()) {
            FrameLayout artBox = new FrameLayout(this);
            artBox.setBackground(Ui.rounded(Ui.withAlpha(Color.WHITE, 40), 16, this));
            ArtView art = new ArtView(this);
            art.setSpecies(popular.get(0), 1, false);
            artBox.addView(art, new FrameLayout.LayoutParams(Ui.dp(this, 92), Ui.dp(this, 92)));
            Ui.margins(artBox, Ui.dp(this, 12), 0, 0, 0);
            top.addView(artBox, Ui.lp(Ui.dp(this, 92), Ui.dp(this, 92)));
        }

        TextView t3 = Ui.text(this, "বাংলা নাম, ইংরেজি নাম, বৈজ্ঞানিক নাম, বাসস্থান, খাদ্য,"
                + " প্রজনন, আকার ও বিষাক্ততা — সব কিছু এক জায়গায়, সম্পূর্ণ অফলাইনে।",
                13f, Ui.withAlpha(Color.WHITE, 205), false);
        t3.setLineSpacing(Ui.dp(this, 3), 1f);
        Ui.margins(t3, 0, Ui.dp(this, 8), 0, 0);
        text.addView(t3);

        // খোঁজার বাক্স
        TextView search = Ui.text(this, "\ud83d\udd0e  " + getString(R.string.search_hint), 13.5f,
                Ui.color(this, R.color.text_muted), false);
        search.setBackground(Ui.rounded(Color.WHITE, 14, this));
        Ui.pad(search, Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        Ui.margins(search, 0, Ui.dp(this, 16), 0, 0);
        search.setSingleLine(true);
        search.setEllipsize(android.text.TextUtils.TruncateAt.END);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SearchActivity.class));
            }
        });
        text.addView(search);

        return card;
    }

    // ------------------------------------------------------------ দ্রুত প্রবেশ

    private View quickRow() {
        HorizontalScrollView sv = new HorizontalScrollView(this);
        sv.setHorizontalScrollBarEnabled(false);
        LinearLayout row = Ui.hbox(this);
        Ui.margins(row, 0, Ui.dp(this, 2), 0, 0);
        sv.addView(row);

        addQuick(row, "\ud83c\udf1f", "জনপ্রিয়", Ui.humanCount(Math.min(totalCount, 99999)),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SpeciesListActivity.start(v.getContext(), Repository.MODE_POPULAR,
                                getString(R.string.title_browse_popular),
                                Ui.color(MainActivity.this, R.color.green_primary));
                    }
                });
        addQuick(row, "⚠️", "বিপন্ন", Ui.humanCount(threatened.size()) + "+",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SpeciesListActivity.start(v.getContext(), Repository.MODE_THREATENED,
                                getString(R.string.title_threatened),
                                Ui.color(MainActivity.this, R.color.iucn_en));
                    }
                });
        addQuick(row, "\ud83d\udd6f", "বিলুপ্ত", Ui.humanCount(extinctCount),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SpeciesListActivity.start(v.getContext(), Repository.MODE_EXTINCT,
                                getString(R.string.title_extinct),
                                Ui.color(MainActivity.this, R.color.iucn_ex));
                    }
                });
        addQuick(row, "☠", "বিষাক্ত", Ui.humanCount(venomCount),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SpeciesListActivity.start(v.getContext(), Repository.MODE_VENOMOUS,
                                getString(R.string.title_venomous),
                                Ui.color(MainActivity.this, R.color.venom_4));
                    }
                });
        addQuick(row, "❤", "পছন্দ", Ui.humanCount(favoriteCount),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SpeciesListActivity.startFavorites(v.getContext(),
                                getString(R.string.title_favorites),
                                Ui.color(MainActivity.this, R.color.green_accent));
                    }
                });
        addQuick(row, "\ud83d\uddc2", "সব প্রজাতি", Ui.humanCount(totalCount),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SpeciesListActivity.start(v.getContext(), Repository.MODE_ALL,
                                getString(R.string.title_all),
                                Ui.color(MainActivity.this, R.color.green_dark));
                    }
                });
        return sv;
    }

    private void addQuick(LinearLayout row, String emoji, String label, String count,
                          View.OnClickListener l) {
        LinearLayout box = Ui.vbox(this);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setBackground(Ui.card(this));
        Ui.elevate(box, Ui.dp(this, 1.2f));
        Ui.pad(box, Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        box.setOnClickListener(l);

        TextView e = Ui.text(this, emoji, 22f, Ui.color(this, R.color.text_primary), false);
        e.setGravity(Gravity.CENTER);
        box.addView(e);
        TextView t = Ui.text(this, label, 13.5f, Ui.color(this, R.color.text_primary), true);
        t.setGravity(Gravity.CENTER);
        Ui.margins(t, 0, Ui.dp(this, 6), 0, 0);
        box.addView(t);
        TextView c = Ui.text(this, count, 11.5f, Ui.color(this, R.color.text_muted), false);
        c.setGravity(Gravity.CENTER);
        Ui.margins(c, 0, Ui.dp(this, 1), 0, 0);
        box.addView(c);

        LinearLayout.LayoutParams p = Ui.lp(Ui.dp(this, 108),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, Ui.dp(this, 10), 0);
        row.addView(box, p);
    }

    // ------------------------------------------------------------ বিভাগ গ্রিড

    private View groupGrid() {
        LinearLayout grid = Ui.vbox(this);
        boolean wide = getResources().getDisplayMetrics().widthPixels > Ui.dp(this, 520);
        LinearLayout row = null;
        for (int i = 0; i < groups.size(); i++) {
            final CategoryGroup g = groups.get(i);
            View cardView = groupCard(g);
            if (wide) {
                if (row == null) {
                    row = Ui.hbox(this);
                    grid.addView(row, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                }
                LinearLayout.LayoutParams p = Ui.lpw(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                p.setMargins(0, Ui.dp(this, 5), Ui.dp(this, 5), Ui.dp(this, 5));
                row.addView(cardView, p);
                if (i % 2 == 1) row = null;
            } else {
                LinearLayout.LayoutParams p = Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                p.setMargins(0, Ui.dp(this, 5), 0, Ui.dp(this, 5));
                grid.addView(cardView, p);
            }
        }
        if (row != null && row.getChildCount() == 1) {
            View spacer = new View(this);
            row.addView(spacer, Ui.lpw(0, 1, 1f));
        }
        Ui.margins(grid, 0, Ui.dp(this, 4), 0, 0);
        return grid;
    }

    private View groupCard(final CategoryGroup g) {
        int color = Ui.parseColor(g.color, Ui.color(this, R.color.green_primary));
        FrameLayout card = new FrameLayout(this);
        GradientDrawable bg = Ui.vertical(Ui.tint(color, 0.90f), Ui.tint(color, 0.76f));
        bg.setCornerRadius(Ui.dp(this, 18));
        bg.setStroke(Ui.dp(this, 1), Ui.tint(color, 0.55f));
        card.setBackground(bg);
        Ui.elevate(card, Ui.dp(this, 1.5f));
        Ui.pad(card, Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));

        LinearLayout box = Ui.vbox(this);
        card.addView(box, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView emoji = Ui.text(this, g.emoji, 30f, color, false);
        box.addView(emoji);

        TextView name = Ui.text(this, g.bnName, 16.5f, Ui.color(this, R.color.text_primary), true);
        Ui.margins(name, 0, Ui.dp(this, 6), 0, 0);
        box.addView(name);

        TextView en = Ui.text(this, g.enName, 11.5f, Ui.color(this, R.color.text_muted), false);
        box.addView(en);

        TextView count = Ui.chip(this, Ui.humanCount(g.speciesCount) + " প্রজাতি", color);
        Ui.margins(count, 0, Ui.dp(this, 10), 0, 0);
        box.addView(count, Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView sub = Ui.text(this, g.familyCount + " পরিবার · " + g.orderCount + " বর্গ",
                11f, Ui.color(this, R.color.text_muted), false);
        sub.setText(Ui.bnDigits(g.familyCount) + " পরিবার · " + Ui.bnDigits(g.orderCount) + " বর্গ");
        Ui.margins(sub, 0, Ui.dp(this, 6), 0, 0);
        box.addView(sub);

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaxonomyActivity.start(v.getContext(), g.groupId, g.bnName,
                        Ui.parseColor(g.color, Ui.color(MainActivity.this, R.color.green_primary)));
            }
        });
        return card;
    }

    // ------------------------------------------------------------ আনুভূমিক স্ট্রিপ

    private View strip(List<Species> list) {
        HorizontalScrollView sv = new HorizontalScrollView(this);
        sv.setHorizontalScrollBarEnabled(false);
        LinearLayout row = Ui.hbox(this);
        Ui.margins(row, 0, Ui.dp(this, 6), 0, Ui.dp(this, 4));
        sv.addView(row);
        for (final Species s : list) {
            View cardView = stripCard(s);
            LinearLayout.LayoutParams p = Ui.lp(Ui.dp(this, 148),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, 0, Ui.dp(this, 10), 0);
            row.addView(cardView, p);
        }
        return sv;
    }

    private View stripCard(final Species s) {
        LinearLayout box = Ui.vbox(this);
        box.setBackground(Ui.card(this));
        Ui.elevate(box, Ui.dp(this, 1.2f));
        box.setClipChildren(false);

        ArtView art = new ArtView(this);
        art.setSpecies(s, 0, true);
        box.addView(art, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 92)));

        LinearLayout text = Ui.vbox(this);
        Ui.pad(text, Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 10));
        box.addView(text, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView bn = Ui.text(this, s.bnName, 13.5f, Ui.color(this, R.color.text_primary), true);
        bn.setMaxLines(2);
        bn.setEllipsize(android.text.TextUtils.TruncateAt.END);
        bn.setMinLines(2);
        text.addView(bn);

        TextView en = Ui.text(this, s.enName, 11f, Ui.color(this, R.color.text_muted), false);
        en.setSingleLine(true);
        en.setEllipsize(android.text.TextUtils.TruncateAt.END);
        Ui.margins(en, 0, Ui.dp(this, 2), 0, 0);
        text.addView(en);

        TextView chip = Ui.chip(this, s.iucn, Ui.iucnColor(this, s.iucn));
        Ui.margins(chip, 0, Ui.dp(this, 7), 0, 0);
        text.addView(chip, Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpeciesDetailActivity.start(v.getContext(), s.id);
            }
        });
        return box;
    }

    // ------------------------------------------------------------ সাধারণ

    private View sectionTitle(String title, String subtitle) {
        LinearLayout box = Ui.hbox(this);
        box.setGravity(Gravity.CENTER_VERTICAL | Gravity.BOTTOM);
        Ui.margins(box, Ui.dp(this, 2), Ui.dp(this, 22), Ui.dp(this, 2), Ui.dp(this, 6));
        TextView t = Ui.text(this, title, 17f, Ui.color(this, R.color.text_primary), true);
        box.addView(t, Ui.lpw(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (subtitle != null) {
            TextView s = Ui.text(this, subtitle, 11.5f, Ui.color(this, R.color.text_muted), false);
            box.addView(s);
        }
        return box;
    }

    private View footerNote() {
        LinearLayout box = Ui.vbox(this);
        box.setBackground(Ui.roundedStroke(Ui.color(this, R.color.surface_alt),
                Ui.color(this, R.color.outline), 16, 1, this));
        Ui.pad(box, Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        Ui.margins(box, Ui.dp(this, 2), Ui.dp(this, 24), Ui.dp(this, 2), 0);

        TextView t = Ui.text(this, getString(R.string.settings_offline), 12.5f,
                Ui.color(this, R.color.text_secondary), false);
        t.setLineSpacing(Ui.dp(this, 3), 1f);
        box.addView(t);
        TextView t2 = Ui.text(this, "ডেটা: " + Ui.humanCount(totalCount) + " প্রজাতি · "
                        + Ui.bnDigits(groups.size()) + " বিভাগ · "
                        + Ui.bnDigits(extinctCount) + " বিলুপ্ত ও বিলুপ্তপ্রায়",
                11.5f, Ui.color(this, R.color.text_muted), false);
        Ui.margins(t2, 0, Ui.dp(this, 6), 0, 0);
        box.addView(t2);
        return box;
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}

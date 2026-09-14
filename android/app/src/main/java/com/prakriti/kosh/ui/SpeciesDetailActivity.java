package com.prakriti.kosh.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.prakriti.kosh.R;
import com.prakriti.kosh.data.Species;
import com.prakriti.kosh.db.Repository;
import com.prakriti.kosh.util.ArtPager;
import com.prakriti.kosh.util.ArtView;
import com.prakriti.kosh.util.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * প্রজাতির বিস্তারিত পর্দা।
 *
 * উপরে সোয়াইপ-যোগ্য ছবি-গ্যালারি (৪টি ভিন্ন পরিবেশ), তারপর নাম, শ্রেণিবিন্যাস,
 * বাসস্থান, খাদ্য, প্রজনন, আকার, বিষাক্ততা, সংরক্ষণ অবস্থা ও ব্যবহারকারীর নোট।
 */
public class SpeciesDetailActivity extends BaseActivity {

    public static final String EXTRA_ID = "species_id";

    private long speciesId;
    private Species species;
    private List<Species> related = new ArrayList<Species>();
    private ArtPager pager;
    private TextView favAction;
    private boolean favorite;

    public static void start(Context c, long id) {
        Intent i = new Intent(c, SpeciesDetailActivity.class);
        i.putExtra(EXTRA_ID, id);
        c.startActivity(i);
    }

    @Override
    protected void onCreate(android.os.Bundle b) {
        super.onCreate(b);
        speciesId = getIntent().getLongExtra(EXTRA_ID, 0);
        if (b != null) speciesId = b.getLong(EXTRA_ID, speciesId);
        requireDb();
    }

    @Override
    protected void onSaveInstanceState(android.os.Bundle out) {
        super.onSaveInstanceState(out);
        out.putLong(EXTRA_ID, speciesId);
    }

    @Override
    protected void onDbReady() {
        setToolbar("…", null);
        addSearchAction();
        load();
    }

    private void load() {
        async(new Work() {
            @Override
            public void run() {
                species = repo.byId(speciesId);
                if (species != null) related = repo.related(species, 12);
                favorite = species != null && user.isFavorite(species.id);
            }
        }, new Done() {
            @Override
            public void onResult(boolean ok, String error) {
                if (!ok || species == null) {
                    setContent(emptyView(getString(R.string.empty_list)));
                    return;
                }
                build();
            }
        });
    }

    private void build() {
        int color = Ui.parseColor(species.color, Ui.color(this, R.color.green_primary));
        setToolbar(species.bnName, species.groupBn + " · " + species.familyBn);
        setToolbarColor(color);
        clearToolbarActions();
        favAction = addToolbarAction(favorite ? "❤ পছন্দের" : "♡ পছন্দ",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleFavorite();
                    }
                });
        addToolbarAction("⤴ শেয়ার", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share(species.shareText());
            }
        });
        addSearchAction();

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.color(this, R.color.bg));
        LinearLayout body = Ui.vbox(this);
        Ui.pad(body, 0, 0, 0, Ui.dp(this, 28));
        scroll.addView(body, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        body.addView(gallery(color));
        if (!repo.isCurated(species.id)) body.addView(sampleBanner());
        body.addView(nameBlock());
        body.addView(taxonomyBlock(color));
        body.addView(factsBlock(color));
        body.addView(noteBlock(color));
        if (!related.isEmpty()) body.addView(relatedBlock(color));

        setContent(scroll);
    }

    /** জেনারেট-করা এন্ট্রির জন্য স্পষ্ট সতর্কবার্তা — যাতে কেউ ভুল না বোঝে। */
    private View sampleBanner() {
        TextView t = Ui.text(this,
                "⚠ নমুনা এন্ট্রি: এটি বাস্তব প্রজাতি নয় — নাম, তথ্য ও ছবি সবই "
                + "প্রোগ্রামে তৈরি ডেমো কনটেন্ট। বাস্তব প্রজাতি চেনার জন্য "
                + "কিউরেটেড (আসল ছবিসহ) এন্ট্রিগুলো দেখুন।",
                12.5f, Ui.color(this, R.color.text_secondary), false);
        t.setLineSpacing(Ui.dp(this, 3), 1f);
        t.setBackground(Ui.rounded(Ui.color(this, R.color.surface_alt), 12, this));
        Ui.pad(t, Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        Ui.margins(t, Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), 0);
        return t;
    }

    // ------------------------------------------------------------ গ্যালারি

    private View gallery(int color) {
        FrameLayout box = new FrameLayout(this);
        int h = Math.round(getResources().getDisplayMetrics().widthPixels * 0.62f);
        int w = getResources().getDisplayMetrics().widthPixels;
        pager = new ArtPager(this);
        box.addView(pager, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // IUCN ব্যাজ
        TextView badge = Ui.chip(this, species.iucn + " · " + Ui.iucnLabel(species.iucn),
                Ui.iucnColor(this, species.iucn));
        FrameLayout.LayoutParams bp = Ui.flp(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        bp.setMargins(Ui.dp(this, 12), Ui.dp(this, 12), 0, 0);
        box.addView(badge, bp);

        if (species.extinct) {
            TextView ex = Ui.chip(this, "বিলুপ্ত", Ui.color(this, R.color.iucn_ex));
            FrameLayout.LayoutParams ep = Ui.flp(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
            ep.setMargins(0, Ui.dp(this, 12), Ui.dp(this, 12), 0);
            box.addView(ex, ep);
        }
        LinearLayout.LayoutParams p = Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, h);
        box.setLayoutParams(p);
        pager.post(new Runnable() {
            @Override
            public void run() {
                pager.setSpecies(species, Math.max(480, w()), Math.max(320, h()));
            }

            private int w() {
                return getResources().getDisplayMetrics().widthPixels;
            }

            private int h() {
                return Math.round(w() * 0.62f);
            }
        });
        return box;
    }

    // ------------------------------------------------------------ নাম

    private View nameBlock() {
        LinearLayout box = Ui.vbox(this);
        box.setBackground(Ui.card(this));
        Ui.elevate(box, Ui.dp(this, 1.5f));
        Ui.pad(box, Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 16));
        Ui.margins(box, Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), 0);

        TextView bn = Ui.text(this, species.bnName, 24f,
                Ui.color(this, R.color.text_primary), true);
        bn.setLineSpacing(Ui.dp(this, 2), 1f);
        box.addView(bn);

        if (species.enName.length() > 0) {
            TextView en = Ui.text(this, species.enName, 15f,
                    Ui.color(this, R.color.text_secondary), false);
            Ui.margins(en, 0, Ui.dp(this, 4), 0, 0);
            box.addView(en);
        }
        TextView sci = Ui.text(this, species.sciName, 14.5f,
                Ui.color(this, R.color.green_accent), false);
        sci.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        Ui.margins(sci, 0, Ui.dp(this, 4), 0, 0);
        box.addView(sci);

        if (species.authority.length() > 0) {
            TextView auth = Ui.text(this, getString(R.string.label_authority) + ": "
                    + species.authority, 12f, Ui.color(this, R.color.text_muted), false);
            Ui.margins(auth, 0, Ui.dp(this, 3), 0, 0);
            box.addView(auth);
        }

        // ট্যাক্সন-চিপ সারি
        LinearLayout chips = Ui.hbox(this);
        Ui.margins(chips, 0, Ui.dp(this, 12), 0, 0);
        box.addView(chips, Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        addChipTo(chips, species.groupBn, Ui.parseColor(species.color,
                Ui.color(this, R.color.green_primary)), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaxonomyActivity.start(v.getContext(), species.groupId, species.groupBn,
                        Ui.parseColor(species.color, Ui.color(SpeciesDetailActivity.this,
                                R.color.green_primary)));
            }
        });
        addChipTo(chips, species.classBn, Ui.color(this, R.color.surface_alt), null);
        addChipTo(chips, species.orderBn, Ui.color(this, R.color.surface_alt), null);
        addChipTo(chips, species.familyBn, Ui.color(this, R.color.surface_alt),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SpeciesListActivity.startFamily(v.getContext(),
                                Repository.MODE_FAMILY, species.groupId, species.classId,
                                species.orderId, species.familyId, species.familyBn,
                                Ui.color(SpeciesDetailActivity.this, R.color.green_accent));
                    }
                });

        // নাম কপি
        TextView copy = Ui.text(this, "নাম ও তথ্য কপি করুন", 12.5f,
                Ui.color(this, R.color.text_muted), false);
        copy.setPaintFlags(copy.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        Ui.margins(copy, 0, Ui.dp(this, 10), 0, 0);
        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(species.shareText());
            }
        });
        box.addView(copy);
        return box;
    }

    private void addChipTo(LinearLayout row, String label, int bg, View.OnClickListener l) {
        TextView t = Ui.chip(this, label, bg);
        if (bg == Ui.color(this, R.color.surface_alt)) {
            t.setTextColor(Ui.color(this, R.color.text_secondary));
            ((android.graphics.drawable.GradientDrawable) t.getBackground())
                    .setStroke(Ui.dp(this, 1), Ui.color(this, R.color.outline));
        }
        if (l != null) t.setOnClickListener(l);
        row.addView(t);
        Ui.margins(t, 0, 0, Ui.dp(this, 6), Ui.dp(this, 6));
    }

    // ------------------------------------------------------------ শ্রেণিবিন্যাস

    private View taxonomyBlock(int color) {
        LinearLayout box = sectionCard(getString(R.string.label_taxonomy), color);
        addRow(box, "বিভাগ", species.groupBn + (species.groupEn.length() > 0
                ? " (" + species.groupEn + ")" : ""));
        addRow(box, "শ্রেণি", species.classBn + " · " + species.classId);
        addRow(box, "বর্গ", species.orderBn + " · " + species.orderId);
        addRow(box, "পরিবার", species.familyBn + " · " + species.familyId);
        return wrap(box);
    }

    // ------------------------------------------------------------ তথ্য

    private View factsBlock(int color) {
        LinearLayout box = Ui.vbox(this);
        addFactCard(box, getString(R.string.label_iucn),
                Ui.iucnLabel(species.iucn) + " (" + species.iucn + ")",
                Ui.iucnColor(this, species.iucn));
        if (species.habitatBn.length() > 0) {
            String where = species.regionBn.length() > 0
                    ? species.habitatBn + " · " + species.regionBn : species.habitatBn;
            addFactCard(box, getString(R.string.label_habitat), where,
                    Ui.color(this, R.color.moss));
        }
        if (species.dietBn.length() > 0) {
            addFactCard(box, getString(R.string.label_diet), species.dietBn,
                    Ui.color(this, R.color.earth_primary));
        }
        if (species.reproBn.length() > 0) {
            addFactCard(box, getString(R.string.label_repro), species.reproBn,
                    Ui.color(this, R.color.green_accent));
        }
        if (species.sizeBn.length() > 0) {
            addFactCard(box, getString(R.string.label_size), species.sizeBn,
                    Ui.color(this, R.color.sky));
        }
        addFactCard(box, getString(R.string.label_venom),
                Ui.venomLabel(species.venomLevel) + " · " + species.venomBn,
                Ui.venomColor(this, species.venomLevel));
        if (species.extinctBn.length() > 0) {
            addFactCard(box, getString(R.string.label_extinct), species.extinctBn,
                    Ui.color(this, R.color.iucn_ex));
        }
        if (species.factBn.length() > 0) {
            addFactCard(box, getString(R.string.label_fact), species.factBn,
                    Ui.color(this, R.color.leaf));
        }
        if (species.notesBn.length() > 0 && !species.notesBn.equals(species.factBn)) {
            addFactCard(box, getString(R.string.label_notes), species.notesBn,
                    Ui.color(this, R.color.clay));
        }
        return box;
    }

    private void addFactCard(LinearLayout parent, String label, String value, int accent) {
        View card = Ui.labeled(this, label, value, accent);
        Ui.margins(card, Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), 0);
        parent.addView(card, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    // ------------------------------------------------------------ নোট

    private View noteBlock(int color) {
        LinearLayout box = sectionCard("আমার নোট", color);
        final EditText input = new EditText(this);
        input.setHint("এই প্রজাতি সম্পর্কে নিজের নোট লিখুন…");
        input.setTextSize(14f);
        input.setMinLines(2);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setBackground(Ui.rounded(Ui.color(this, R.color.surface_alt), 12, this));
        Ui.pad(input, Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        input.setTextColor(Ui.color(this, R.color.text_primary));
        input.setHintTextColor(Ui.color(this, R.color.text_muted));
        box.addView(input, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout btns = Ui.hbox(this);
        Ui.margins(btns, 0, Ui.dp(this, 10), 0, 0);
        box.addView(btns);

        final TextView save = Ui.text(this, "সংরক্ষণ", 13.5f, Color.WHITE, true);
        save.setBackground(Ui.pill(color, this));
        save.setGravity(Gravity.CENTER);
        Ui.pad(save, Ui.dp(this, 20), Ui.dp(this, 10), Ui.dp(this, 20), Ui.dp(this, 10));
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String text = input.getText().toString();
                async(new Work() {
                    @Override
                    public void run() {
                        user.setNote(species.id, text);
                    }
                }, new Done() {
                    @Override
                    public void onResult(boolean ok, String error) {
                        toast(ok ? "নোট সংরক্ষিত হয়েছে" : error);
                    }
                });
            }
        });
        btns.addView(save);

        TextView clear = Ui.text(this, "মুছুন", 13.5f, Ui.color(this, R.color.text_secondary),
                false);
        clear.setGravity(Gravity.CENTER);
        Ui.pad(clear, Ui.dp(this, 18), Ui.dp(this, 10), Ui.dp(this, 18), Ui.dp(this, 10));
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                input.setText("");
                async(new Work() {
                    @Override
                    public void run() {
                        user.setNote(species.id, "");
                    }
                }, null);
            }
        });
        Ui.margins(clear, Ui.dp(this, 8), 0, 0, 0);
        btns.addView(clear);

        async(new Work() {
            @Override
            public void run() {
                noteText = user.note(species.id);
            }
        }, new Done() {
            @Override
            public void onResult(boolean ok, String error) {
                if (noteText != null && noteText.length() > 0) input.setText(noteText);
            }
        });
        return wrap(box);
    }

    private String noteText;

    // ------------------------------------------------------------ সম্পর্কিত

    private View relatedBlock(int color) {
        LinearLayout box = Ui.vbox(this);
        TextView title = Ui.text(this, getString(R.string.title_related), 16f,
                Ui.color(this, R.color.text_primary), true);
        Ui.margins(title, Ui.dp(this, 14), Ui.dp(this, 22), Ui.dp(this, 14), Ui.dp(this, 6));
        box.addView(title);

        HorizontalScrollView sv = new HorizontalScrollView(this);
        sv.setHorizontalScrollBarEnabled(false);
        LinearLayout row = Ui.hbox(this);
        Ui.pad(row, Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 4));
        sv.addView(row);
        for (final Species s : related) {
            View cardView = relatedCard(s);
            LinearLayout.LayoutParams p = Ui.lp(Ui.dp(this, 140),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, 0, Ui.dp(this, 10), 0);
            row.addView(cardView, p);
        }
        box.addView(sv);

        TextView more = Ui.text(this, species.familyBn + "-এর সব প্রজাতি দেখুন  ›",
                13f, color, true);
        Ui.pad(more, Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 4));
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpeciesListActivity.startFamily(v.getContext(), Repository.MODE_FAMILY,
                        species.groupId, species.classId, species.orderId, species.familyId,
                        species.familyBn, color);
            }
        });
        box.addView(more);
        return box;
    }

    private View relatedCard(final Species s) {
        LinearLayout box = Ui.vbox(this);
        box.setBackground(Ui.card(this));
        Ui.elevate(box, Ui.dp(this, 1f));
        ArtView art = new ArtView(this);
        art.setSpecies(s, 0, true);
        box.addView(art, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 88)));
        TextView t = Ui.text(this, s.bnName, 12.5f, Ui.color(this, R.color.text_primary), true);
        t.setMaxLines(2);
        t.setMinLines(2);
        t.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout tt = Ui.vbox(this);
        Ui.pad(tt, Ui.dp(this, 9), Ui.dp(this, 7), Ui.dp(this, 9), Ui.dp(this, 9));
        tt.addView(t);
        box.addView(tt);
        box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start(v.getContext(), s.id);
            }
        });
        return box;
    }

    // ------------------------------------------------------------ সাধারণ

    private LinearLayout sectionCard(String title, int color) {
        LinearLayout box = Ui.vbox(this);
        box.setBackground(Ui.card(this));
        Ui.elevate(box, Ui.dp(this, 1.5f));
        Ui.pad(box, Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));

        LinearLayout head = Ui.hbox(this);
        View dot = new View(this);
        dot.setBackground(Ui.oval(color));
        head.addView(dot, Ui.lp(Ui.dp(this, 9), Ui.dp(this, 9)));
        TextView t = Ui.label(this, title);
        t.setTextColor(Ui.color(this, R.color.text_primary));
        t.setTextSize(14f);
        Ui.margins(t, Ui.dp(this, 9), 0, 0, 0);
        head.addView(t, Ui.lpw(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(head);
        return box;
    }

    private void addRow(LinearLayout box, String label, String value) {
        LinearLayout row = Ui.hbox(this);
        row.setGravity(Gravity.TOP);
        Ui.margins(row, 0, Ui.dp(this, 9), 0, 0);
        TextView l = Ui.text(this, label, 13f, Ui.color(this, R.color.text_muted), false);
        row.addView(l, Ui.lp(Ui.dp(this, 78), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView v = Ui.text(this, value, 13.5f, Ui.color(this, R.color.text_primary), false);
        row.addView(v, Ui.lpw(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(row);
    }

    private View wrap(View v) {
        LinearLayout box = Ui.vbox(this);
        Ui.margins(v, Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), 0);
        box.addView(v, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return box;
    }

    private void toggleFavorite() {
        async(new Work() {
            @Override
            public void run() {
                favorite = user.toggleFavorite(species.id);
            }
        }, new Done() {
            @Override
            public void onResult(boolean ok, String error) {
                if (favAction != null) {
                    favAction.setText(favorite ? "❤ পছন্দের" : "♡ পছন্দ");
                }
                toast(favorite ? "পছন্দের তালিকায় যোগ হয়েছে" : "পছন্দের তালিকা থেকে সরানো হয়েছে");
            }
        });
    }
}

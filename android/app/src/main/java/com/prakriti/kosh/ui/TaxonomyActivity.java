package com.prakriti.kosh.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.prakriti.kosh.R;
import com.prakriti.kosh.data.CategoryGroup;
import com.prakriti.kosh.db.Repository;
import com.prakriti.kosh.data.TaxonNode;
import com.prakriti.kosh.util.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * শ্রেণিবিন্যাসের ধাপে ধাপে নামা: বিভাগ → শ্রেণি → বর্গ → পরিবার → প্রজাতির তালিকা।
 *
 * একই স্ক্রিন তিন স্তরের জন্য ব্যবহৃত হয়; শুধু ডেটা বদলায়। প্রতিটি সারিতে
 * সেই ধাপে কত প্রজাতি আছে তা দেখানো হয়, ফলে ব্যবহারকারী আগেই ধারণা পান।
 */
public class TaxonomyActivity extends BaseActivity {

    public static final String EXTRA_GROUP_ID = "group_id";
    public static final String EXTRA_GROUP_BN = "group_bn";
    public static final String EXTRA_COLOR = "color";

    private String groupId;
    private String groupBn;
    private int color;
    private CategoryGroup group;

    private String classId;
    private String classBn;
    private String orderId;
    private String orderBn;

    private final List<TaxonNode> nodes = new ArrayList<TaxonNode>();
    private TaxonAdapter adapter;

    public static void start(Context c, String groupId, String groupBn, int color) {
        Intent i = new Intent(c, TaxonomyActivity.class);
        i.putExtra(EXTRA_GROUP_ID, groupId);
        i.putExtra(EXTRA_GROUP_BN, groupBn);
        i.putExtra(EXTRA_COLOR, color);
        c.startActivity(i);
    }

    @Override
    protected void onDbReady() {
        Intent in = getIntent();
        groupId = in.getStringExtra(EXTRA_GROUP_ID);
        groupBn = in.getStringExtra(EXTRA_GROUP_BN);
        color = in.getIntExtra(EXTRA_COLOR, Ui.color(this, R.color.green_primary));
        setToolbarColor(color);
        buildList();
    }

    @Override
    protected void onCreate(android.os.Bundle b) {
        super.onCreate(b);
        requireDb();
    }

    private void buildList() {
        ListView list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setCacheColorHint(Color.TRANSPARENT);
        list.setBackgroundColor(Ui.color(this, R.color.bg));
        Ui.pad(list, Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 24));
        list.setClipToPadding(false);

        // হেডার: এই বিভাগের সংক্ষিপ্ত পরিচয়
        View header = header();
        list.addHeaderView(header, null, false);

        adapter = new TaxonAdapter();
        list.setAdapter(adapter);

        View footer = new View(this);
        footer.setMinimumHeight(Ui.dp(this, 40));
        list.addFooterView(footer, null, false);

        setContent(list);
        loadLevel();
    }

    private View header() {
        LinearLayout box = Ui.vbox(this);
        box.setBackground(Ui.card(this));
        Ui.elevate(box, Ui.dp(this, 1.5f));
        Ui.pad(box, Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        Ui.margins(box, 0, Ui.dp(this, 6), 0, Ui.dp(this, 10));

        LinearLayout top = Ui.hbox(this);
        box.addView(top, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView emoji = Ui.text(this, group == null ? "\ud83c\udf3f" : group.emoji, 30f, color, false);
        top.addView(emoji);
        LinearLayout tt = Ui.vbox(this);
        Ui.margins(tt, Ui.dp(this, 12), 0, 0, 0);
        top.addView(tt, Ui.lpw(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView name = Ui.text(this, groupBn == null ? "" : groupBn, 18f,
                Ui.color(this, R.color.text_primary), true);
        tt.addView(name);
        TextView en = Ui.text(this, group == null ? "" : group.enName, 12f,
                Ui.color(this, R.color.text_muted), false);
        tt.addView(en);

        TextView blurb = Ui.text(this, group == null ? "" : group.blurb, 13f,
                Ui.color(this, R.color.text_secondary), false);
        blurb.setLineSpacing(Ui.dp(this, 3), 1f);
        Ui.margins(blurb, 0, Ui.dp(this, 10), 0, 0);
        box.addView(blurb);

        // সংখ্যা
        LinearLayout stats = Ui.hbox(this);
        Ui.margins(stats, 0, Ui.dp(this, 12), 0, 0);
        box.addView(stats);
        if (group != null) {
            stats.addView(statCell(Ui.humanCount(group.speciesCount), "প্রজাতি"));
            stats.addView(statCell(Ui.bnDigits(group.classCount), "শ্রেণি"));
            stats.addView(statCell(Ui.bnDigits(group.orderCount), "বর্গ"));
            stats.addView(statCell(Ui.bnDigits(group.familyCount), "পরিবার"));
        }

        // সরাসরি এই বিভাগের সব প্রজাতি
        TextView all = Ui.text(this, "এই বিভাগের সব প্রজাতি দেখুন  ›", 13.5f, color, true);
        all.setBackground(Ui.roundedStroke(Ui.tint(color, 0.90f), Ui.tint(color, 0.55f),
                12, 1, this));
        all.setGravity(Gravity.CENTER);
        Ui.pad(all, Ui.dp(this, 12), Ui.dp(this, 11), Ui.dp(this, 12), Ui.dp(this, 11));
        Ui.margins(all, 0, Ui.dp(this, 14), 0, 0);
        all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpeciesListActivity.start(v.getContext(), Repository.MODE_GROUP, groupId,
                        groupBn == null ? "" : groupBn, color);
            }
        });
        box.addView(all);
        return box;
    }

    private View statCell(String value, String label) {
        LinearLayout cell = Ui.vbox(this);
        cell.setGravity(Gravity.CENTER);
        TextView v = Ui.text(this, value, 15f, Ui.color(this, R.color.text_primary), true);
        v.setGravity(Gravity.CENTER);
        cell.addView(v);
        TextView l = Ui.text(this, label, 11f, Ui.color(this, R.color.text_muted), false);
        l.setGravity(Gravity.CENTER);
        cell.addView(l);
        LinearLayout.LayoutParams p = Ui.lpw(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cell.setLayoutParams(p);
        return cell;
    }

    // ------------------------------------------------------------ স্তর

    private int level() {
        if (classId == null) return TaxonNode.LEVEL_CLASS;
        if (orderId == null) return TaxonNode.LEVEL_ORDER;
        return TaxonNode.LEVEL_FAMILY;
    }

    private void loadLevel() {
        final int lv = level();
        async(new Work() {
            @Override
            public void run() {
                if (group == null) group = repo.group(groupId);
                List<TaxonNode> out;
                if (lv == TaxonNode.LEVEL_CLASS) {
                    out = repo.classes(groupId);
                } else if (lv == TaxonNode.LEVEL_ORDER) {
                    out = repo.orders(groupId, classId);
                } else {
                    out = repo.families(groupId, classId, orderId);
                }
                nodes.clear();
                nodes.addAll(out);
            }
        }, new Done() {
            @Override
            public void onResult(boolean ok, String error) {
                if (!ok) {
                    toast(error);
                    return;
                }
                updateChrome();
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void updateChrome() {
        String subtitle;
        int lv = level();
        if (lv == TaxonNode.LEVEL_CLASS) {
            subtitle = Ui.bnDigits(nodes.size()) + "টি শ্রেণি";
        } else if (lv == TaxonNode.LEVEL_ORDER) {
            subtitle = classBn + " · " + Ui.bnDigits(nodes.size()) + "টি বর্গ";
        } else {
            subtitle = orderBn + " · " + Ui.bnDigits(nodes.size()) + "টি পরিবার";
        }
        setToolbar(groupBn == null ? "" : groupBn, subtitle);
    }

    private void open(TaxonNode n) {
        if (n.level == TaxonNode.LEVEL_CLASS) {
            classId = n.id;
            classBn = n.bnName;
            orderId = null;
            orderBn = null;
            loadLevel();
        } else if (n.level == TaxonNode.LEVEL_ORDER) {
            orderId = n.id;
            orderBn = n.bnName;
            loadLevel();
        } else {
            SpeciesListActivity.startFamily(this, Repository.MODE_FAMILY, groupId, classId,
                    orderId, n.id, n.bnName, color);
        }
    }

    @Override
    public void onBackPressed() {
        if (level() == TaxonNode.LEVEL_FAMILY) {
            orderId = null;
            orderBn = null;
            loadLevel();
        } else if (level() == TaxonNode.LEVEL_ORDER) {
            classId = null;
            classBn = null;
            loadLevel();
        } else {
            super.onBackPressed();
        }
    }

    // ------------------------------------------------------------ অ্যাডাপ্টার

    private final class TaxonAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return nodes.size();
        }

        @Override
        public Object getItem(int position) {
            return nodes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Context c = TaxonomyActivity.this;
            final TaxonNode n = nodes.get(position);
            LinearLayout card;
            if (convertView == null) {
                card = Ui.vbox(c);
                card.setBackground(Ui.card(c));
                Ui.elevate(card, Ui.dp(c, 1f));
                Ui.pad(card, Ui.dp(c, 14), Ui.dp(c, 12), Ui.dp(c, 14), Ui.dp(c, 12));

                LinearLayout top = Ui.hbox(c);
                card.addView(top, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                TextView name = new TextView(c);
                name.setTag("name");
                name.setTextSize(15f);
                name.setTypeface(Typeface.DEFAULT_BOLD);
                name.setTextColor(Ui.color(c, R.color.text_primary));
                top.addView(name, Ui.lpw(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView arrow = Ui.text(c, "›", 22f, Ui.color(c, R.color.text_muted), false);
                top.addView(arrow);

                TextView sci = new TextView(c);
                sci.setTag("sci");
                sci.setTextSize(11.5f);
                sci.setTypeface(Typeface.create("serif", Typeface.ITALIC));
                sci.setTextColor(Ui.color(c, R.color.text_muted));
                Ui.margins(sci, 0, Ui.dp(c, 1), 0, 0);
                card.addView(sci);

                LinearLayout meta = Ui.hbox(c);
                meta.setTag("meta");
                Ui.margins(meta, 0, Ui.dp(c, 8), 0, 0);
                card.addView(meta);
            } else {
                card = (LinearLayout) convertView;
            }

            ((TextView) card.findViewWithTag("name")).setText(n.bnName);
            ((TextView) card.findViewWithTag("sci")).setText(n.sciName.replace("_ord", "")
                    .replace("_fam", ""));
            LinearLayout meta = (LinearLayout) card.findViewWithTag("meta");
            meta.removeAllViews();
            int childLabel = n.childCount;
            meta.addView(Ui.chip(c, Ui.humanCount(n.speciesCount) + " প্রজাতি", color));
            Ui.margins(meta.getChildAt(0), 0, 0, Ui.dp(c, 6), 0);
            if (n.level != TaxonNode.LEVEL_FAMILY) {
                String sub = n.level == TaxonNode.LEVEL_CLASS
                        ? Ui.bnDigits(childLabel) + " বর্গ"
                        : Ui.bnDigits(childLabel) + " পরিবার";
                meta.addView(Ui.chip(c, sub, Ui.color(c, R.color.surface_alt)));
                ((TextView) meta.getChildAt(1))
                        .setTextColor(Ui.color(c, R.color.text_secondary));
            }
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    open(n);
                }
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, Ui.dp(c, 4), 0, Ui.dp(c, 4));
            card.setLayoutParams(p);
            return card;
        }
    }
}

package com.prakriti.kosh.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.prakriti.kosh.R;
import com.prakriti.kosh.data.Species;
import com.prakriti.kosh.util.ArtView;
import com.prakriti.kosh.util.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * প্রজাতির তালিকার অ্যাডাপ্টার (ListView)।
 *
 * ListView প্রতিটি দৃশ্যমান সারি তৈরি/পুনর্ব্যবহার করে, তাই ২.৫ লক্ষ প্রজাতির
 * ডেটাবেসেও শুধু স্ক্রিনের ২৪–৩০টি সারিই মেমরিতে থাকে। ছবিগুলো RGB_565
 * LruCache-এ থাকে, ফলে স্ক্রোলের সময় আবার আঁকতে হয় না।
 */
public class SpeciesAdapter extends BaseAdapter {

    private final List<Species> items = new ArrayList<Species>();
    private final Context context;

    public SpeciesAdapter(Context c) {
        this.context = c;
    }

    public void addAll(List<Species> more) {
        if (more == null || more.isEmpty()) return;
        items.addAll(more);
        notifyDataSetChanged();
    }

    public void reset() {
        items.clear();
        notifyDataSetChanged();
    }

    public Species at(int position) {
        return position >= 0 && position < items.size() ? items.get(position) : null;
    }

    public List<Species> all() {
        return items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return at(position);
    }

    @Override
    public long getItemId(int position) {
        Species s = at(position);
        return s == null ? 0 : s.id;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder h;
        if (convertView == null) {
            convertView = buildItem(context);
            h = new Holder(convertView);
            convertView.setTag(h);
        } else {
            h = (Holder) convertView.getTag();
        }
        h.bind(items.get(position));
        return convertView;
    }

    private View buildItem(Context c) {
        int pad = Ui.dp(c, 10);
        FrameLayout card = new FrameLayout(c);
        card.setBackground(Ui.card(c));
        Ui.elevate(card, Ui.dp(c, 1.2f));

        LinearLayout row = Ui.hbox(c);
        Ui.pad(row, pad, pad, pad, pad);
        card.addView(row, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ছবি (থাম্বনেইল)
        FrameLayout artBox = new FrameLayout(c);
        artBox.setBackground(Ui.rounded(Ui.color(c, R.color.surface_alt), 12, c));
        ArtView art = new ArtView(c);
        art.setTag("art");
        artBox.addView(art, new FrameLayout.LayoutParams(Ui.dp(c, 88), Ui.dp(c, 88)));
        row.addView(artBox, Ui.lp(Ui.dp(c, 88), Ui.dp(c, 88)));

        // লেখা
        LinearLayout text = Ui.vbox(c);
        Ui.margins(text, Ui.dp(c, 12), 0, 0, 0);
        row.addView(text, Ui.lpw(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView bn = new TextView(c);
        bn.setTag("bn");
        bn.setTextSize(15.5f);
        bn.setTypeface(Typeface.DEFAULT_BOLD);
        bn.setTextColor(Ui.color(c, R.color.text_primary));
        bn.setMaxLines(2);
        bn.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(bn);

        TextView en = new TextView(c);
        en.setTag("en");
        en.setTextSize(12.5f);
        en.setTextColor(Ui.color(c, R.color.text_secondary));
        en.setSingleLine(true);
        en.setEllipsize(TextUtils.TruncateAt.END);
        Ui.margins(en, 0, Ui.dp(c, 1), 0, 0);
        text.addView(en);

        TextView sci = new TextView(c);
        sci.setTag("sci");
        sci.setTextSize(12f);
        sci.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        sci.setTextColor(Ui.color(c, R.color.text_muted));
        sci.setSingleLine(true);
        sci.setEllipsize(TextUtils.TruncateAt.END);
        Ui.margins(sci, 0, Ui.dp(c, 1), 0, 0);
        text.addView(sci);

        LinearLayout badges = Ui.hbox(c);
        badges.setTag("badges");
        Ui.margins(badges, 0, Ui.dp(c, 7), 0, 0);
        text.addView(badges, Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // ডান পাশের তীর
        TextView arrow = new TextView(c);
        arrow.setText("›");
        arrow.setTextSize(22f);
        arrow.setTextColor(Ui.color(c, R.color.text_muted));
        arrow.setGravity(Gravity.CENTER_VERTICAL);
        Ui.margins(arrow, Ui.dp(c, 4), 0, 0, 0);
        row.addView(arrow, Ui.lp(Ui.dp(c, 20), ViewGroup.LayoutParams.MATCH_PARENT));

        return card;
    }

    /** সারির ভিউ-হোল্ডার। */
    private final class Holder {
        private final View root;
        private final ArtView art;
        private final TextView bn, en, sci;
        private final LinearLayout badges;

        Holder(View v) {
            root = v;
            art = (ArtView) v.findViewWithTag("art");
            bn = (TextView) v.findViewWithTag("bn");
            en = (TextView) v.findViewWithTag("en");
            sci = (TextView) v.findViewWithTag("sci");
            badges = (LinearLayout) v.findViewWithTag("badges");
        }

        void bind(final Species s) {
            bn.setText(s.bnName);
            en.setText(s.enName.length() > 0 ? s.enName : "—");
            sci.setText(s.sciName);
            art.setSpecies(s, 0, true);
            badges.removeAllViews();
            Context c = badges.getContext();
            int gap = Ui.dp(c, 6);

            badges.addView(Ui.chip(c, s.iucn, Ui.iucnColor(c, s.iucn)),
                    Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
            Ui.margins(badges.getChildAt(0), 0, 0, gap, 0);

            int n = 1;
            if (s.venomLevel >= 2) {
                badges.addView(Ui.chip(c, "বিষ " + Ui.bnDigits(s.venomLevel),
                                Ui.venomColor(c, s.venomLevel)),
                        Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
                Ui.margins(badges.getChildAt(n++), 0, 0, gap, 0);
            }
            if (s.extinct) {
                badges.addView(Ui.chip(c, "বিলুপ্ত", Ui.color(c, R.color.iucn_ex)),
                        Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
                Ui.margins(badges.getChildAt(n++), 0, 0, gap, 0);
            }
            TextView fam = Ui.text(c, s.familyBn, 11.5f,
                    Ui.color(c, R.color.text_muted), false);
            fam.setSingleLine(true);
            fam.setEllipsize(TextUtils.TruncateAt.END);
            badges.addView(fam, Ui.lpw(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SpeciesDetailActivity.start(v.getContext(), s.id);
                }
            });
        }
    }
}

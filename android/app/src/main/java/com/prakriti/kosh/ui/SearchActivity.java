package com.prakriti.kosh.ui;

import android.graphics.Color;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.prakriti.kosh.R;
import com.prakriti.kosh.data.Species;
import com.prakriti.kosh.db.Repository;
import com.prakriti.kosh.util.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * অনুসন্ধান — FTS5 পূর্ণ-পাঠ সূচক + নামের আলাদা ইনডেক্স।
 *
 * লিখতেই ২২০ মি.সে. পর খোঁজে (ডিবাউন্স), ফলে প্রতিটি অক্ষরে ডেটাবেসে
 * চাপ পড়ে না। প্রথমে হুবহু মিল, তারপর উপসর্গ, শেষে FTS5।
 */
public class SearchActivity extends BaseActivity {

    private static final int DEBOUNCE_MS = 220;

    private EditText input;
    private ListView list;
    private SpeciesAdapter adapter;
    private View footer;
    private ProgressBar footerProgress;
    private TextView footerText;
    private TextView suggestionBox;

    private String query = "";
    private int offset;
    private boolean loading;
    private boolean hasMore;
    private int foundTotal;
    private final Runnable searchTask = new Runnable() {
        @Override
        public void run() {
            runSearch(query, 0);
        }
    };

    private boolean built;

    /**
     * বাগ-ফিক্স (v1.0.4): আগে এই স্ক্রিনে requireDb() কখনো ডাকা হত না —
     * ফলে onDbReady() চলত না, খোঁজার বাক্স ও ফলের তালিকা কিছুই বসত না
     * (স্ক্রিন ফাঁকা দেখাত)। এখন onCreate-এ requireDb() ডাকা হয়।
     */
    @Override
    protected void onCreate(android.os.Bundle b) {
        super.onCreate(b);
        if (uiBroken) return;
        requireDb();
    }

    @Override
    protected void onDbReady() {
        if (built) return;
        built = true;
        setToolbar(getString(R.string.title_search), "বাংলা, ইংরেজি বা বৈজ্ঞানিক নামে খুঁজুন");
        setToolbarColor(Ui.color(this, R.color.green_dark));
        build();
        input.requestFocus();
    }

    private void build() {
        LinearLayout body = Ui.vbox(this);
        body.setBackgroundColor(Ui.color(this, R.color.bg));

        // খোঁজার বাক্স
        LinearLayout searchBox = Ui.hbox(this);
        searchBox.setBackground(Ui.card(this));
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        Ui.pad(searchBox, Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        Ui.margins(searchBox, Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 4));

        TextView icon = Ui.text(this, "\ud83d\udd0e", 16f, Ui.color(this, R.color.green_accent), false);
        searchBox.addView(icon);

        input = new EditText(this);
        input.setHint(getString(R.string.search_hint));
        input.setTextSize(15f);
        input.setSingleLine(true);
        input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setBackground(null);
        input.setTextColor(Ui.color(this, R.color.text_primary));
        input.setHintTextColor(Ui.color(this, R.color.text_muted));
        Ui.margins(input, Ui.dp(this, 8), 0, 0, 0);
        searchBox.addView(input, Ui.lpw(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                schedule(s.toString());
            }
        });

        TextView clear = Ui.text(this, "✕", 16f, Ui.color(this, R.color.text_muted), false);
        clear.setGravity(Gravity.CENTER);
        Ui.pad(clear, Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 4), Ui.dp(this, 6));
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                input.setText("");
            }
        });
        searchBox.addView(clear);
        body.addView(searchBox);

        // পরামর্শ চিপ
        suggestionBox = Ui.text(this, "চেষ্টা করুন: বাঘ · ইলিশ · মাছরাঙা · সুন্দরবন · Panthera"
                + " · তিমি · সাপ", 12.5f, Ui.color(this, R.color.text_muted), false);
        Ui.pad(suggestionBox, Ui.dp(this, 16), Ui.dp(this, 6), Ui.dp(this, 16), Ui.dp(this, 6));
        suggestionBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickRandomSuggestion();
            }
        });
        body.addView(suggestionBox);

        list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setCacheColorHint(Color.TRANSPARENT);
        list.setBackgroundColor(Ui.color(this, R.color.bg));
        Ui.pad(list, Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 16));
        list.setClipToPadding(false);

        TextView head = new TextView(this);
        head.setTag("head");
        head.setTextSize(12.5f);
        head.setTextColor(Ui.color(this, R.color.text_muted));
        Ui.pad(head, Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 8));
        list.addHeaderView(head, null, false);

        footer = Ui.vbox(this);
        ((LinearLayout) footer).setGravity(Gravity.CENTER);
        Ui.pad(footer, Ui.dp(this, 20), Ui.dp(this, 18), Ui.dp(this, 20), Ui.dp(this, 26));
        footerProgress = new ProgressBar(this);
        ((LinearLayout) footer).addView(footerProgress,
                new LinearLayout.LayoutParams(Ui.dp(this, 26), Ui.dp(this, 26)));
        footerText = Ui.text(this, getString(R.string.search_hint), 13.5f,
                Ui.color(this, R.color.text_secondary), false);
        footerText.setGravity(Gravity.CENTER);
        ((LinearLayout) footer).addView(footerText);
        list.addFooterView(footer, null, false);

        adapter = new SpeciesAdapter(this);
        list.setAdapter(adapter);
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int first, int visible, int total) {
                if (total > 0 && first + visible >= total - 5) loadMore();
            }
        });
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Species s = adapter.at(position - list.getHeaderViewsCount());
                if (s != null) SpeciesDetailActivity.start(SearchActivity.this, s.id);
            }
        });
        body.addView(list, Ui.lpw(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContent(body);
        updateFooter();
    }

    private void pickRandomSuggestion() {
        String[] opts = {"বাঘ", "ইলিশ", "মাছরাঙা", "সুন্দরবন", "তিমি", "হাতি", "সাপ",
                "Panthera", "ডাইনোসর", "ময়ূর"};
        input.setText(opts[(int) (Math.random() * opts.length)]);
    }

    private void schedule(String q) {
        query = q == null ? "" : q.trim();
        input.removeCallbacks(searchTask);
        if (query.length() == 0) {
            adapter.reset();
            hasMore = false;
            foundTotal = 0;
            footerText.setText(getString(R.string.search_hint));
            footerProgress.setVisibility(View.GONE);
            updateHead();
            return;
        }
        input.postDelayed(searchTask, DEBOUNCE_MS);
    }

    private void loadMore() {
        if (loading || !hasMore || query.length() == 0) return;
        runSearch(query, offset);
    }

    private void runSearch(final String q, final int from) {
        loading = true;
        updateFooter();
        async(new Work() {
            @Override
            public void run() {
                Repository.Page page = repo.searchPage(q, from);
                lastItems = page.items;
                lastHasMore = page.hasMore;
                lastCount = page.items.size();
            }
        }, new Done() {
            @Override
            public void onResult(boolean ok, String error) {
                loading = false;
                if (!ok) {
                    footerText.setText(error);
                    footerProgress.setVisibility(View.GONE);
                    return;
                }
                if (from == 0) {
                    adapter.reset();
                    foundTotal = 0;
                }
                adapter.addAll(lastItems);
                offset = from + lastCount;
                hasMore = lastHasMore;
                foundTotal += lastCount;
                updateFooter();
                updateHead();
            }
        });
    }

    private List<Species> lastItems = new ArrayList<Species>();
    private boolean lastHasMore;
    private int lastCount;

    private void updateFooter() {
        if (loading) {
            footerProgress.setVisibility(View.VISIBLE);
            footerText.setText("খোঁজা হচ্ছে…");
        } else {
            footerProgress.setVisibility(View.GONE);
            if (query.length() == 0) footerText.setText(getString(R.string.search_hint));
            else if (foundTotal == 0) footerText.setText(getString(R.string.empty_search));
            else if (!hasMore) footerText.setText("— " + Ui.bnDigits(foundTotal)
                    + "টি ফল —");
            else footerText.setText("");
        }
    }

    private void updateHead() {
        TextView head = (TextView) list.findViewWithTag("head");
        if (head == null) return;
        if (query.length() == 0) {
            head.setText("");
            return;
        }
        head.setText("“" + query + "” — " + Ui.bnDigits(foundTotal) + "টি ফল দেখানো হয়েছে");
    }
}

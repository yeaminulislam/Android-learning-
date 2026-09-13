package com.prakriti.kosh.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
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
 * প্রজাতির তালিকা — কীসেট পেজিনেশন সহ অসীম স্ক্রোল।
 *
 * প্রতিবার মাত্র ২৪টি সারি পড়া হয় (`row_seq > last`), ফলে ২.৫ লক্ষ প্রজাতির
 * ডেটাবেসেও স্ক্রোল মসৃণ থাকে এবং মেমরিতে শুধু দরকারি সারিই থাকে।
 */
public class SpeciesListActivity extends BaseActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_GROUP = "group";
    public static final String EXTRA_CLASS = "class";
    public static final String EXTRA_ORDER = "order";
    public static final String EXTRA_FAMILY = "family";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_COLOR = "color";
    public static final String EXTRA_REGION = "region";
    public static final String EXTRA_HABITAT = "habitat";
    public static final String EXTRA_REGION_LAB = "region_lab";
    public static final String EXTRA_HABITAT_LAB = "habitat_lab";

    /** এই স্ক্রিনের নিজস্ব মোড (Repository.MODE_* ছাড়াও)। */
    public static final int MODE_ALL = Repository.MODE_ALL;
    public static final int MODE_GROUP = Repository.MODE_GROUP;
    public static final int MODE_FAMILY = Repository.MODE_FAMILY;
    public static final int MODE_VENOMOUS = Repository.MODE_VENOMOUS;
    public static final int MODE_THREATENED = Repository.MODE_THREATENED;
    public static final int MODE_EXTINCT = Repository.MODE_EXTINCT;
    public static final int MODE_FAVORITES = 100;
    public static final int MODE_RELATED = 101;

    private int mode;
    private String groupId, classId, orderId, familyId, title;
    private String regionKey, habitatKey, regionLab, habitatLab;
    private int color;

    private ListView list;
    private SpeciesAdapter adapter;
    private View footer;
    private ProgressBar footerProgress;
    private TextView footerText;

    private boolean loading;
    private boolean hasMore = true;
    private long lastSeq;
    private long lastId;
    private boolean byPopular;
    private String iucnFilter;
    private final List<Long> favoriteIds = new ArrayList<Long>();
    private final List<Long> relatedIds = new ArrayList<Long>();
    private int idOffset;
    private int totalCount;

    public static void start(Context c, int mode, String title, int color) {
        start(c, mode, null, null, null, null, title, color);
    }

    public static void start(Context c, int mode, String groupId, String title, int color) {
        start(c, mode, groupId, null, null, null, title, color);
    }

    public static void startFamily(Context c, int mode, String groupId, String classId,
                                   String orderId, String familyId, String title, int color) {
        start(c, mode, groupId, classId, orderId, familyId, title, color);
    }

    public static void startFavorites(Context c, String title, int color) {
        start(c, MODE_FAVORITES, null, null, null, null, title, color);
    }

    public static void startRelated(Context c, Species s, String title, int color) {
        Intent i = new Intent(c, SpeciesListActivity.class);
        i.putExtra(EXTRA_MODE, MODE_RELATED);
        i.putExtra(EXTRA_GROUP, s.groupId);
        i.putExtra(EXTRA_CLASS, s.classId);
        i.putExtra(EXTRA_ORDER, s.orderId);
        i.putExtra(EXTRA_FAMILY, s.familyId);
        i.putExtra(EXTRA_TITLE, title);
        i.putExtra(EXTRA_COLOR, color);
        c.startActivity(i);
    }

    public static void start(Context c, int mode, String groupId, String classId,
                             String orderId, String familyId, String title, int color) {
        start(c, mode, groupId, classId, orderId, familyId, title, color, null, null);
    }

    /** অঞ্চল/পরিবেশ ফিল্টারসহ তালিকা (ব্লুপ্রিন্ট লেভেল ২ → ৩)। */
    public static void start(Context c, int mode, String groupId, String classId,
                             String orderId, String familyId, String title, int color,
                             String regionKey, String habitatKey) {
        start(c, mode, groupId, classId, orderId, familyId, title, color,
                regionKey, habitatKey, null, null);
    }

    /** ফিল্টারের বাংলা লেবেলসহ। */
    public static void start(Context c, int mode, String groupId, String classId,
                             String orderId, String familyId, String title, int color,
                             String regionKey, String habitatKey,
                             String regionLab, String habitatLab) {
        Intent i = new Intent(c, SpeciesListActivity.class);
        i.putExtra(EXTRA_MODE, mode);
        if (groupId != null) i.putExtra(EXTRA_GROUP, groupId);
        if (classId != null) i.putExtra(EXTRA_CLASS, classId);
        if (orderId != null) i.putExtra(EXTRA_ORDER, orderId);
        if (familyId != null) i.putExtra(EXTRA_FAMILY, familyId);
        if (regionKey != null) i.putExtra(EXTRA_REGION, regionKey);
        if (habitatKey != null) i.putExtra(EXTRA_HABITAT, habitatKey);
        if (regionLab != null) i.putExtra(EXTRA_REGION_LAB, regionLab);
        if (habitatLab != null) i.putExtra(EXTRA_HABITAT_LAB, habitatLab);
        i.putExtra(EXTRA_TITLE, title == null ? "" : title);
        i.putExtra(EXTRA_COLOR, color);
        c.startActivity(i);
    }

    @Override
    protected void onCreate(android.os.Bundle b) {
        super.onCreate(b);
        Intent in = getIntent();
        mode = in.getIntExtra(EXTRA_MODE, MODE_ALL);
        groupId = in.getStringExtra(EXTRA_GROUP);
        classId = in.getStringExtra(EXTRA_CLASS);
        orderId = in.getStringExtra(EXTRA_ORDER);
        familyId = in.getStringExtra(EXTRA_FAMILY);
        title = in.getStringExtra(EXTRA_TITLE);
        regionKey = in.getStringExtra(EXTRA_REGION);
        habitatKey = in.getStringExtra(EXTRA_HABITAT);
        regionLab = in.getStringExtra(EXTRA_REGION_LAB);
        habitatLab = in.getStringExtra(EXTRA_HABITAT_LAB);
        color = in.getIntExtra(EXTRA_COLOR, Ui.color(this, R.color.green_primary));
        requireDb();
    }

    @Override
    protected void onDbReady() {
        if (title == null || title.length() == 0) title = getString(R.string.title_all);
        setToolbar(title, getString(R.string.app_tagline));
        setToolbarColor(color);
        addSearchAction();
        build();
        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isDbReady() && mode == MODE_FAVORITES) reload();
    }

    private void build() {
        LinearLayout body = Ui.vbox(this);

        // ছাঁকনি ও সাজানোর চিপ
        clearChips();
        addChip(byPopular || mode == Repository.MODE_POPULAR ? "\ud83d\udd25 জনপ্রিয়" : "↕ তালিকা-ক্রম",
                true, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        togglePopularSort();
                    }
                });
        addChip("সব", iucnFilter == null, chipClick(null));
        addChip("ন্যূনতম ঝুঁকি", "LC".equals(iucnFilter), chipClick("LC"));
        addChip("বিপন্ন", "VU".equals(iucnFilter), chipClick("VU"));
        addChip("সংকটাপন্ন", "EN".equals(iucnFilter), chipClick("EN"));
        addChip("মারাত্মক", "CR".equals(iucnFilter), chipClick("CR"));
        addChip("বিলুপ্ত", "EX".equals(iucnFilter), chipClick("EX"));
        if (regionKey != null && regionKey.length() > 0) {
            addChip("🌍 " + (regionLab == null ? regionKey : regionLab) + " ✕",
                    true, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    start(v.getContext(), mode, groupId, classId, orderId, familyId,
                            title, color, null, habitatKey, null, habitatLab);
                    finish();
                }
            });
        }
        if (habitatKey != null && habitatKey.length() > 0) {
            addChip("🏞 " + (habitatLab == null ? habitatKey : habitatLab) + " ✕",
                    true, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    start(v.getContext(), mode, groupId, classId, orderId, familyId,
                            title, color, regionKey, null, regionLab, null);
                    finish();
                }
            });
        }

        list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setCacheColorHint(Color.TRANSPARENT);
        list.setBackgroundColor(Ui.color(this, R.color.bg));
        Ui.pad(list, Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 16));
        list.setClipToPadding(false);

        // হেডার: মোট সংখ্যা
        TextView head = new TextView(this);
        head.setTag("head");
        head.setTextSize(12.5f);
        head.setTextColor(Ui.color(this, R.color.text_muted));
        Ui.pad(head, Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 8));
        list.addHeaderView(head, null, false);

        footer = Ui.vbox(this);
        ((LinearLayout) footer).setGravity(android.view.Gravity.CENTER);
        Ui.pad(footer, Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 28));
        footerProgress = new ProgressBar(this);
        ((LinearLayout) footer).addView(footerProgress,
                new LinearLayout.LayoutParams(Ui.dp(this, 28), Ui.dp(this, 28)));
        footerText = Ui.text(this, "", 13.5f, Ui.color(this, R.color.text_secondary), false);
        footerText.setGravity(android.view.Gravity.CENTER);
        Ui.margins(footerText, 0, Ui.dp(this, 8), 0, 0);
        ((LinearLayout) footer).addView(footerText);
        list.addFooterView(footer, null, false);

        adapter = new SpeciesAdapter(this);
        list.setAdapter(adapter);
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisible, int visibleCount,
                                 int total) {
                if (total > 0 && firstVisible + visibleCount >= total - 5) loadMore();
            }
        });
        list.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View v,
                                    int position, long id) {
                Species s = adapter.at(position - list.getHeaderViewsCount());
                if (s != null) SpeciesDetailActivity.start(SpeciesListActivity.this, s.id);
            }
        });

        body.addView(list, Ui.lpw(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContent(body);
        updateFooter();
    }

    private View.OnClickListener chipClick(final String iucn) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (iucn == null ? iucnFilter == null : iucn.equals(iucnFilter)) return;
                iucnFilter = iucn;
                build();       // চিপ ও তালিকা নতুন করে
                reload();
            }
        };
    }

    private void reload() {
        adapter.reset();
        lastSeq = 0;
        lastId = 0;
        idOffset = 0;
        hasMore = true;
        favoriteIds.clear();
        relatedIds.clear();
        loading = true;
        updateFooter();
        async(new Work() {
            @Override
            public void run() {
                if (mode == MODE_FAVORITES) favoriteIds.addAll(user.favoriteIds());
                else if (mode == MODE_RELATED) {
                    relatedIds.addAll(repo.familyIds(groupId, classId, orderId, familyId, 500));
                }
                totalCount = countNow();
            }
        }, new Done() {
            @Override
            public void onResult(boolean ok, String error) {
                if (!ok) {
                    toast(error);
                    loading = false;
                    updateFooter();
                    return;
                }
                loadMore();
            }
        });
    }

    private int countNow() {
        if (mode == MODE_FAVORITES) return favoriteIds.size();
        if (mode == MODE_RELATED) return relatedIds.size();
        return repo.countOf(mode, groupId, classId, orderId, familyId, iucnFilter,
                regionKey, habitatKey);
    }

    private void loadMore() {
        if (loading || !hasMore) return;
        loading = true;
        updateFooter();
        final long fromSeq = lastSeq;
        final long fromId = lastId;
        final int fromOffset = idOffset;
        final boolean popular = byPopular || mode == Repository.MODE_POPULAR;
        async(new Work() {
            @Override
            public void run() {
                Repository.Page page;
                if (mode == MODE_FAVORITES) {
                    page = repo.pageOfIds(favoriteIds, fromOffset);
                } else if (mode == MODE_RELATED) {
                    page = repo.pageOfIds(relatedIds, fromOffset);
                } else {
                    page = repo.pageAfter(mode, groupId, classId, orderId, familyId,
                            fromSeq, fromId, popular, iucnFilter, regionKey, habitatKey);
                }
                lastPage = page;
                lastPopularUsed = popular;
                if (mode == MODE_FAVORITES || mode == MODE_RELATED) {
                    idOffset = fromOffset + Repository.PAGE_SIZE;
                }
            }
        }, new Done() {
            @Override
            public void onResult(boolean ok, String error) {
                loading = false;
                if (!ok) {
                    toast(error);
                    updateFooter();
                    return;
                }
                applyPage();
            }
        });
    }

    private Repository.Page lastPage;
    private boolean lastPopularUsed;

    private void applyPage() {
        Repository.Page page = lastPage;
        if (page == null) return;
        if (!page.items.isEmpty()) {
            adapter.addAll(page.items);
            if (lastPopularUsed) {
                lastSeq = page.lastPop;
                lastId = page.lastId;
            } else {
                lastSeq = page.lastSeq;
            }
        }
        hasMore = page.hasMore;
        updateFooter();
        updateHead();
    }

    private void updateFooter() {
        if (footer == null) return;
        if (loading) {
            footerProgress.setVisibility(View.VISIBLE);
            footerText.setText("আরও দেখানো হচ্ছে…");
        } else if (!hasMore && adapter.getCount() == 0) {
            footerProgress.setVisibility(View.GONE);
            footerText.setText(mode == MODE_FAVORITES
                    ? getString(R.string.empty_favorites) : getString(R.string.empty_list));
        } else if (!hasMore) {
            footerProgress.setVisibility(View.GONE);
            footerText.setText("— তালিকার শেষ —");
        } else {
            footerProgress.setVisibility(View.GONE);
            footerText.setText("");
        }
    }

    private void updateHead() {
        TextView head = (TextView) list.findViewWithTag("head");
        if (head == null) return;
        String shown = Ui.bnDigits(adapter.getCount());
        String total = totalCount > 0 ? Ui.humanCount(totalCount) : "";
        StringBuilder sb = new StringBuilder();
        sb.append(shown).append(" / ").append(total).append(" প্রজাতি দেখানো হচ্ছে");
        if (iucnFilter != null) sb.append(" · ছাঁকনি: ").append(iucnFilter);
        head.setText(sb.toString());
        setToolbar(title, sb.toString());
    }

    /** জনপ্রিয়তার ক্রমে সাজানো (টুলবার অ্যাকশন থেকে)। */
    public void togglePopularSort() {
        byPopular = !byPopular;
        reload();
    }
}

package com.prakriti.kosh.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.prakriti.kosh.data.CategoryGroup;
import com.prakriti.kosh.data.Species;
import com.prakriti.kosh.data.TaxonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * সব ডেটা-পড়ার একমাত্র দরজা।
 *
 * তালিকা সবসময় কীসেট পেজিনেশন করে (LIMIT/OFFSET নয়): প্রতিটি পৃষ্ঠা
 * `row_seq > lastSeq ORDER BY row_seq LIMIT PAGE_SIZE` — ফলে ২.৫ লক্ষ সারির
 * ডেটাবেসেও পৃষ্ঠা বদলানো প্রায় শূন্য সময়ে হয়, শুধু দরকারি সারিই পড়া হয়।
 */
public final class Repository {

    public static final int PAGE_SIZE = 24;

    /** তালিকার ধরন। */
    public static final int MODE_ALL = 0;
    public static final int MODE_GROUP = 1;
    public static final int MODE_CLASS = 2;
    public static final int MODE_ORDER = 3;
    public static final int MODE_FAMILY = 4;
    public static final int MODE_THREATENED = 5;
    public static final int MODE_EXTINCT = 6;
    public static final int MODE_VENOMOUS = 7;
    public static final int MODE_POPULAR = 8;
    public static final int MODE_DEMO = 9;

    /** species + ট্যাক্সোনমি join (SELECT_FULL = SELECT_BASE + JOIN_TAXONOMY)। */
    public static final String SELECT_BASE =
            "SELECT s.id, s.bn_name, s.en_name, s.sci_name, s.authority,"
            + " s.class_id, s.order_id, s.family_id, s.group_id,"
            + " s.region_bn, s.habitat_bn, s.diet_bn, s.iucn, s.size_bn,"
            + " s.venom_level, s.venom_bn, s.repro_bn, s.fact_bn, s.extinct_bn,"
            + " s.notes_bn, s.is_demo, s.popularity, s.row_seq, s.extinct";

    public static final String JOIN_TAXONOMY =
            " LEFT JOIN taxon_class tc ON tc.group_id = s.group_id"
            + "   AND tc.class_id = s.class_id"
            + " LEFT JOIN taxon_order tord ON tord.group_id = s.group_id"
            + "   AND tord.class_id = s.class_id AND tord.order_id = s.order_id"
            + " LEFT JOIN taxon_family tf ON tf.group_id = s.group_id"
            + "   AND tf.class_id = s.class_id AND tf.order_id = s.order_id"
            + "   AND tf.family_id = s.family_id"
            + " LEFT JOIN category_group cg ON cg.group_id = s.group_id";

    public static final String SELECT_EXTRA =
            " IFNULL(tc.bn_name, s.class_id) AS class_bn,"
            + " IFNULL(tord.bn_name, s.order_id) AS order_bn,"
            + " IFNULL(tf.bn_name, s.family_id) AS family_bn,"
            + " IFNULL(cg.bn_name, s.group_id) AS group_bn,"
            + " IFNULL(cg.en_name, '') AS group_en,"
            + " IFNULL(cg.emoji, '') AS emoji,"
            + " IFNULL(cg.color, '#2E7D32') AS color,"
            + " s.dangerous";

    public static final String SELECT_FULL = SELECT_BASE + SELECT_EXTRA + " FROM species s"
            + JOIN_TAXONOMY;

    /** এক পৃষ্ঠার ফলাফল। */
    public static final class Page {
        public final List<Species> items;
        public final long lastSeq;         // পরের পৃষ্ঠার শুরু-বিন্দু
        public final long lastPop;         // জনপ্রিয়তা-ক্রমে পেজিনেশনের জন্য
        public final long lastId;
        public boolean hasMore;

        public Page(List<Species> items, boolean hasMore) {
            this.items = items;
            this.hasMore = hasMore;
            if (items.isEmpty()) {
                lastSeq = 0;
                lastPop = Long.MAX_VALUE;
                lastId = 0;
            } else {
                Species last = items.get(items.size() - 1);
                lastSeq = last.rowSeq;
                lastPop = last.popularity;
                lastId = last.id;
            }
        }
    }

    private static final Repository INSTANCE = new Repository();
    private Context appContext;
    private SQLiteDatabase db;

    private Repository() {
    }

    public static Repository get() {
        return INSTANCE;
    }

    /** ডেটাবেস প্রস্তুত হওয়ার পর একবার ডাকতে হয়। */
    public void init(Context c) {
        this.appContext = c.getApplicationContext();
        this.db = DatabaseManager.get().species(appContext);
    }

    public SQLiteDatabase db() {
        return db;
    }

    public boolean isReady() {
        return db != null && db.isOpen();
    }

    // ------------------------------------------------------------ বিভাগ ও শ্রেণিবিন্যাস

    public List<CategoryGroup> groups() {
        List<CategoryGroup> out = new ArrayList<CategoryGroup>();
        Cursor c = db.rawQuery(
                "SELECT group_id, bn_name, en_name, emoji, color, blurb_bn,"
                + " species_count, class_count, order_count, family_count, sort_order"
                + " FROM category_group ORDER BY sort_order", null);
        try {
            while (c.moveToNext()) out.add(CategoryGroup.from(c));
        } finally {
            c.close();
        }
        return out;
    }

    public CategoryGroup group(String groupId) {
        Cursor c = db.rawQuery(
                "SELECT group_id, bn_name, en_name, emoji, color, blurb_bn,"
                + " species_count, class_count, order_count, family_count, sort_order"
                + " FROM category_group WHERE group_id=?", new String[]{groupId});
        try {
            return c.moveToFirst() ? CategoryGroup.from(c) : null;
        } finally {
            c.close();
        }
    }

    public List<TaxonNode> classes(String groupId) {
        List<TaxonNode> out = new ArrayList<TaxonNode>();
        Cursor c = db.rawQuery(
                "SELECT class_id, group_id, sci_name, bn_name, species_count, order_count,"
                + " sort_order FROM taxon_class WHERE group_id=? ORDER BY sort_order",
                new String[]{groupId});
        try {
            while (c.moveToNext()) out.add(TaxonNode.classFrom(c));
        } finally {
            c.close();
        }
        return out;
    }

    public List<TaxonNode> orders(String groupId, String classId) {
        List<TaxonNode> out = new ArrayList<TaxonNode>();
        Cursor c = db.rawQuery(
                "SELECT order_id, group_id, class_id, sci_name, bn_name, species_count,"
                + " family_count, sort_order FROM taxon_order WHERE group_id=? AND class_id=?"
                + " ORDER BY sort_order", new String[]{groupId, classId});
        try {
            while (c.moveToNext()) out.add(TaxonNode.orderFrom(c));
        } finally {
            c.close();
        }
        return out;
    }

    public List<TaxonNode> families(String groupId, String classId, String orderId) {
        List<TaxonNode> out = new ArrayList<TaxonNode>();
        Cursor c = db.rawQuery(
                "SELECT family_id, group_id, class_id, order_id, sci_name, bn_name,"
                + " species_count, sort_order FROM taxon_family WHERE group_id=?"
                + " AND class_id=? AND order_id=? ORDER BY sort_order",
                new String[]{groupId, classId, orderId});
        try {
            while (c.moveToNext()) out.add(TaxonNode.familyFrom(c));
        } finally {
            c.close();
        }
        return out;
    }

    // ------------------------------------------------------------ একক প্রজাতি

    public Species byId(long id) {
        Cursor c = db.rawQuery(SELECT_FULL + " WHERE s.id=?",
                new String[]{Long.toString(id)});
        try {
            return c.moveToFirst() ? Species.from(c) : null;
        } finally {
            c.close();
        }
    }

    /** বিস্তারিত পর্দের গ্যালারিতে দেখানোর আশেপাশের প্রজাতি (সোয়াইপে পরবর্তী)। */
    public List<Long> neighbours(long id, int mode, String groupId, String classId,
                                 String orderId, String familyId, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT s.id FROM species s WHERE s.row_seq>?");
        List<String> args = new ArrayList<String>();
        args.add(Long.toString(id));
        appendScopeAliased(sql, args, mode, groupId, classId, orderId, familyId);
        sql.append(" ORDER BY s.row_seq LIMIT ").append(limit);
        List<Long> out = new ArrayList<Long>();
        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        try {
            while (c.moveToNext()) out.add(c.getLong(0));
        } finally {
            c.close();
        }
        return out;
    }

    // ------------------------------------------------------------ পৃষ্ঠা (কীসেট)

    /** প্রথম পৃষ্ঠা। */
    public Page page(int mode, String groupId, String classId, String orderId,
                     String familyId) {
        return pageAfter(mode, groupId, classId, orderId, familyId, 0, 0, false, null);
    }

    /**
     * পরের পৃষ্ঠা।
     *
     * @param lastSeq   আগের পৃষ্ঠার শেষ row_seq (জনপ্রিয়তা-ক্রমে lastId)
     * @param byPopular true হলে popularity DESC, id ক্রমে
     */
    public Page pageAfter(int mode, String groupId, String classId, String orderId,
                          String familyId, long lastSeq, long lastId, boolean byPopular,
                          String iucn) {
        StringBuilder sql = new StringBuilder();
        List<String> args = new ArrayList<String>();
        String order;
        if (byPopular) {
            long pop = lastSeq <= 0 ? Long.MAX_VALUE : lastSeq;
            sql.append("SELECT s.id, s.popularity FROM species s WHERE (s.popularity<?"
                    + " OR (s.popularity=? AND s.id>?))");
            args.add(Long.toString(pop));
            args.add(Long.toString(pop));
            args.add(Long.toString(lastId));
            appendScopeAliased(sql, args, mode, groupId, classId, orderId, familyId);
            order = " ORDER BY s.popularity DESC, s.id LIMIT " + (PAGE_SIZE + 1);
        } else {
            sql.append("SELECT s.id, s.row_seq FROM species s WHERE s.row_seq>?");
            args.add(Long.toString(lastSeq));
            appendScopeAliased(sql, args, mode, groupId, classId, orderId, familyId);
            order = " ORDER BY s.row_seq LIMIT " + (PAGE_SIZE + 1);
        }
        if (iucn != null && iucn.length() > 0) {
            sql.append(" AND s.iucn=?");
            args.add(iucn);
        }
        sql.append(order);

        List<Long> ids = new ArrayList<Long>();
        List<Long> seqs = new ArrayList<Long>();
        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        try {
            while (c.moveToNext()) {
                ids.add(c.getLong(0));
                seqs.add(c.getLong(1));
            }
        } finally {
            c.close();
        }
        boolean hasMore = ids.size() > PAGE_SIZE;
        if (hasMore) {
            ids.remove(ids.size() - 1);
            seqs.remove(seqs.size() - 1);
        }
        List<Species> items = byIds(ids);
        return new Page(items, hasMore);
    }

    /** id তালিকা থেকে প্রজাতি — তালিকার ক্রম হুবহু বজায় থাকে। */
    public List<Species> byIds(List<Long> ids) {
        List<Species> out = new ArrayList<Species>();
        if (ids.isEmpty()) return out;
        // (VALUES …) টেবিল সব Android-এ চলে না, তাই UNION ALL দিয়ে ক্রম-সহ তালিকা
        StringBuilder list = new StringBuilder();
        String[] args = new String[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) list.append(" UNION ALL ");
            list.append("SELECT ").append(i).append(" AS k, ").append(ids.get(i))
                    .append(" AS sid");
            args[i] = Long.toString(ids.get(i));
        }
        Cursor c = db.rawQuery(SELECT_BASE + SELECT_EXTRA + ", o.k"
                        + " FROM (" + list + ") AS o"
                        + " JOIN species s ON s.id = o.sid"
                        + JOIN_TAXONOMY
                        + " ORDER BY o.k",
                null);
        try {
            while (c.moveToNext()) out.add(Species.from(c));
        } finally {
            c.close();
        }
        return out;
    }

    private static void appendScopeAliased(StringBuilder sql, List<String> args, int mode,
                                           String groupId, String classId, String orderId,
                                           String familyId) {
        switch (mode) {
            case MODE_ALL:
                break;
            case MODE_GROUP:
                sql.append(" AND s.group_id=?");
                args.add(groupId);
                break;
            case MODE_CLASS:
                sql.append(" AND s.group_id=? AND s.class_id=?");
                args.add(groupId);
                args.add(classId);
                break;
            case MODE_ORDER:
                sql.append(" AND s.group_id=? AND s.class_id=? AND s.order_id=?");
                args.add(groupId);
                args.add(classId);
                args.add(orderId);
                break;
            case MODE_FAMILY:
                sql.append(" AND s.group_id=? AND s.class_id=? AND s.order_id=?"
                        + " AND s.family_id=?");
                args.add(groupId);
                args.add(classId);
                args.add(orderId);
                args.add(familyId);
                break;
            case MODE_THREATENED:
                sql.append(" AND s.iucn IN ('VU','EN','CR')");
                break;
            case MODE_EXTINCT:
                sql.append(" AND s.extinct=1");
                break;
            case MODE_VENOMOUS:
                sql.append(" AND s.dangerous=1");
                break;
            case MODE_POPULAR:
                break;
            case MODE_DEMO:
                sql.append(" AND s.is_demo=1");
                break;
            default:
                break;
        }
    }

    /** কোনো পরিসরে মোট কত প্রজাতি। */
    public int countOf(int mode, String groupId, String classId, String orderId,
                       String familyId) {
        return countOf(mode, groupId, classId, orderId, familyId, null);
    }

    /** মোট প্রজাতি সংখ্যা। */
    public int countAll() {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM species", null);
        try {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            c.close();
        }
    }

    /** IUCN ছাঁকনিসহ গণনা। */
    public int countOf(int mode, String groupId, String classId, String orderId,
                       String familyId, String iucn) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM species s WHERE 1=1");
        List<String> args = new ArrayList<String>();
        appendScopeAliased(sql, args, mode, groupId, classId, orderId, familyId);
        if (iucn != null && iucn.length() > 0) {
            sql.append(" AND s.iucn=?");
            args.add(iucn);
        }
        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        try {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            c.close();
        }
    }

    /** একটি নির্দিষ্ট id তালিকার পৃষ্ঠা (পছন্দ/সম্পর্কিত তালিকার জন্য)। */
    public Page pageOfIds(List<Long> all, int offset) {
        List<Species> out = new ArrayList<Species>();
        if (all == null || offset >= all.size()) return new Page(out, false);
        int end = Math.min(all.size(), offset + PAGE_SIZE);
        out.addAll(byIds(all.subList(offset, end)));
        return new Page(out, end < all.size());
    }

    /** একটি পরিবারের সব প্রজাতির id (জনপ্রিয়তার ক্রমে)। */
    public List<Long> familyIds(String groupId, String classId, String orderId,
                                String familyId, int limit) {
        List<Long> out = new ArrayList<Long>();
        Cursor c = db.rawQuery(
                "SELECT s.id FROM species s WHERE s.group_id=? AND s.class_id=?"
                + " AND s.order_id=? AND s.family_id=? ORDER BY s.popularity DESC, s.row_seq"
                + " LIMIT ?",
                new String[]{groupId, classId, orderId, familyId, Integer.toString(limit)});
        try {
            while (c.moveToNext()) out.add(c.getLong(0));
        } finally {
            c.close();
        }
        return out;
    }

    // ------------------------------------------------------------ অনুসন্ধান (FTS5)

    /**
     * FTS5 দিয়ে পূর্ণ-পাঠ অনুসন্ধান। ব্যবহারকারীর লেখা থেকে নিরাপদ MATCH
     * প্যাটার্ন বানানো হয় (উদ্ধৃতি ও বিশেষ চিহ্ন সরিয়ে, প্রতিটি শব্দে prefix '*')।
     */
    public Page search(String query, int limit) {
        if (TextUtils.isEmpty(query)) {
            return new Page(new ArrayList<Species>(), false);
        }
        String q = query.trim();
        String lower = q.toLowerCase(java.util.Locale.US);
        String prefixHi = q + "\uFFFF";
        String prefixLo = lower + "\uFFFF";
        java.util.LinkedHashSet<Long> seen = new java.util.LinkedHashSet<Long>();

        // ১) হুবহু নাম
        collect(seen, "SELECT id FROM species WHERE bn_name=? LIMIT ?", q, str(limit));
        collect(seen, "SELECT id FROM species WHERE LOWER(en_name)=? LIMIT ?", lower, str(limit));
        collect(seen, "SELECT id FROM species WHERE LOWER(sci_name)=? LIMIT ?", lower, str(limit));
        // ২) নামের শুরুতে মিল (ইনডেক্স ব্যবহার করে, খুব দ্রুত)
        collect(seen, "SELECT id FROM species WHERE bn_name>=? AND bn_name<?"
                + " ORDER BY popularity DESC LIMIT ?", q, prefixHi, str(limit));
        collect(seen, "SELECT id FROM species WHERE LOWER(en_name)>=? AND LOWER(en_name)<?"
                + " ORDER BY popularity DESC LIMIT ?", lower, prefixLo, str(limit));
        collect(seen, "SELECT id FROM species WHERE LOWER(sci_name)>=? AND LOWER(sci_name)<?"
                + " ORDER BY popularity DESC LIMIT ?", lower, prefixLo, str(limit));
        // ৩) বাকি সব — FTS5 (নামের অংশ, বাসস্থান, খাদ্য, তথ্য …)
        if (seen.size() < limit) {
            String match = buildMatch(q);
            if (match != null) {
                collect(seen, "SELECT s.id FROM species_fts f JOIN species s ON s.id=f.rowid"
                        + " WHERE species_fts MATCH ? ORDER BY rank LIMIT ?",
                        match, str(limit * 3));
            }
        }
        List<Long> ids = new ArrayList<Long>(seen);
        if (ids.size() > limit) ids = ids.subList(0, limit);
        return new Page(byIds(ids), false);
    }

    /**
     * পৃষ্ঠা-ভিত্তিক অনুসন্ধান (অসীম স্ক্রোলের জন্য)।
     *
     * প্রতিটি স্তরে `offset + PAGE_SIZE` পর্যন্ত মিল সংগ্রহ করে তারপর আগেরগুলো
     * বাদ দেওয়া হয় — ফলে নতুন করে পুরো ডেটাবেস ঘাঁটতে হয় না।
     */
    public Page searchPage(String query, int offset) {
        if (TextUtils.isEmpty(query)) return new Page(new ArrayList<Species>(), false);
        int want = offset + PAGE_SIZE;
        Page all = search(query, want);
        List<Species> items = new ArrayList<Species>();
        for (int i = offset; i < all.items.size(); i++) items.add(all.items.get(i));
        return new Page(items, all.items.size() >= want);
    }

    private static String str(int v) {
        return Integer.toString(v);
    }

    private void collect(java.util.LinkedHashSet<Long> into, String sql, String... args) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, args);
            while (c.moveToNext()) into.add(c.getLong(0));
        } catch (Exception e) {
            // কোনো একটি স্তর ব্যর্থ হলেও বাকিগুলো চলবে
        } finally {
            if (c != null) c.close();
        }
    }

    static String buildMatch(String query) {
        if (TextUtils.isEmpty(query)) return null;
        String[] words = query.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            String t = sanitize(w);
            if (t.length() == 0) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append('"').append(t).append('"').append('*');
        }
        if (sb.length() == 0) {
            String t = sanitize(query);
            if (t.length() == 0) return null;
            sb.append('"').append(t).append('"').append('*');
        }
        return sb.toString();
    }

    private static String sanitize(String w) {
        StringBuilder sb = new StringBuilder(w.length());
        for (int i = 0; i < w.length(); i++) {
            char ch = w.charAt(i);
            if (Character.isLetterOrDigit(ch)) sb.append(ch);
        }
        return sb.toString();
    }

    /** একই পরিবারের আরও প্রজাতি (বিস্তারিত পর্দের নিচে)। */
    public List<Species> related(Species s, int limit) {
        if (s == null) return new ArrayList<Species>();
        Cursor c = db.rawQuery(
                "SELECT id FROM species WHERE group_id=? AND class_id=? AND order_id=?"
                + " AND family_id=? AND id<>? ORDER BY popularity DESC, row_seq LIMIT ?",
                new String[]{s.groupId, s.classId, s.orderId, s.familyId,
                        Long.toString(s.id), Integer.toString(limit)});
        List<Long> ids = new ArrayList<Long>();
        try {
            while (c.moveToNext()) ids.add(c.getLong(0));
        } finally {
            c.close();
        }
        return byIds(ids);
    }

    // ------------------------------------------------------------ পছন্দের তালিকা

    public List<Species> favorites() {
        SQLiteDatabase u = DatabaseManager.get().user(appContext);
        Cursor c = u.rawQuery(
                "SELECT species_id FROM favorite ORDER BY added_at DESC LIMIT 2000", null);
        List<Long> ids = new ArrayList<Long>();
        try {
            while (c.moveToNext()) ids.add(c.getLong(0));
        } finally {
            c.close();
        }
        return byIds(ids);
    }

    // ------------------------------------------------------------ মেটা

    public String meta(String key) {
        Cursor c = db.rawQuery("SELECT value FROM meta WHERE key=?", new String[]{key});
        try {
            return c.moveToFirst() ? c.getString(0) : "";
        } finally {
            c.close();
        }
    }
}

package com.prakriti.kosh.data;

import android.database.Cursor;

/**
 * একটি প্রজাতির সম্পূর্ণ তথ্য (species টেবিল + ট্যাক্সোনমি join)।
 * সব ফিল্ড ইমিউটেবল — থ্রেড-নিরাপদ।
 */
public final class Species {

    public final long id;
    public final String bnName;
    public final String enName;
    public final String sciName;
    public final String authority;
    public final String classId;
    public final String classBn;
    public final String orderId;
    public final String orderBn;
    public final String familyId;
    public final String familyBn;
    public final String groupId;
    public final String groupBn;
    public final String groupEn;
    public final String emoji;
    public final String color;
    public final String regionBn;
    public final String habitatBn;
    public final String dietBn;
    public final String iucn;
    public final String sizeBn;
    public final int venomLevel;
    public final String venomBn;
    public final String reproBn;
    public final String factBn;
    public final String extinctBn;
    public final String notesBn;
    public final boolean isDemo;
    public final int popularity;
    public final long rowSeq;
    public final boolean extinct;
    public final boolean dangerous;

    public Species(long id, String bnName, String enName, String sciName, String authority,
                   String classId, String classBn, String orderId, String orderBn,
                   String familyId, String familyBn, String groupId, String groupBn,
                   String groupEn, String emoji, String color, String regionBn,
                   String habitatBn, String dietBn, String iucn, String sizeBn,
                   int venomLevel, String venomBn, String reproBn, String factBn,
                   String extinctBn, String notesBn, boolean isDemo, int popularity,
                   long rowSeq, boolean extinct, boolean dangerous) {
        this.id = id;
        this.bnName = bnName;
        this.enName = enName;
        this.sciName = sciName;
        this.authority = authority;
        this.classId = classId;
        this.classBn = classBn;
        this.orderId = orderId;
        this.orderBn = orderBn;
        this.familyId = familyId;
        this.familyBn = familyBn;
        this.groupId = groupId;
        this.groupBn = groupBn;
        this.groupEn = groupEn;
        this.emoji = emoji;
        this.color = color;
        this.regionBn = regionBn;
        this.habitatBn = habitatBn;
        this.dietBn = dietBn;
        this.iucn = iucn;
        this.sizeBn = sizeBn;
        this.venomLevel = venomLevel;
        this.venomBn = venomBn;
        this.reproBn = reproBn;
        this.factBn = factBn;
        this.extinctBn = extinctBn;
        this.notesBn = notesBn;
        this.isDemo = isDemo;
        this.popularity = popularity;
        this.rowSeq = rowSeq;
        this.extinct = extinct;
        this.dangerous = dangerous;
    }

    /**
     * কার্সরের বর্তমান সারি থেকে Species বানায়।
     * প্রজেকশন সবসময় {@link com.prakriti.kosh.db.Repository#SELECT_FULL} অনুযায়ী হতে হবে।
     */
    // SELECT_FULL প্রজেকশনের কলাম-ক্রম (একবারই গণনা হয়)
    private static int[] ix;

    private static int[] idx(Cursor c) {
        // কলাম-সূচি ক্যাশ করা হয় না: ভিন্ন প্রশ্নে ভিন্ন কলাম-সেট আসতে পারে,
        // আর Cursor.getColumnIndex খুব সস্তা।
        {
            String[] names = {
                    "id", "bn_name", "en_name", "sci_name", "authority",
                    "class_id", "order_id", "family_id", "group_id",
                    "region_bn", "habitat_bn", "diet_bn", "iucn", "size_bn",
                    "venom_level", "venom_bn", "repro_bn", "fact_bn", "extinct_bn",
                    "notes_bn", "is_demo", "popularity", "row_seq", "extinct",
                    "class_bn", "order_bn", "family_bn", "group_bn",
                    "group_en", "emoji", "color", "dangerous",
            };
            int[] ix = new int[names.length];
            for (int i = 0; i < names.length; i++) {
                ix[i] = c.getColumnIndex(names[i]);
                if (ix[i] < 0) ix[i] = 0;   // অনুপস্থিত কলাম → প্রথমটি (নিরাপদ)
            }
            return ix;
        }
    }

    /** কার্সরের বর্তমান সারি থেকে Species বানায় (SELECT_FULL প্রজেকশন)। */
    public static Species from(Cursor c) {
        int[] i = idx(c);
        return new Species(
                c.getLong(i[0]),
                s(c, i[1]), s(c, i[2]), s(c, i[3]), s(c, i[4]),
                s(c, i[5]), s(c, i[24]), s(c, i[6]), s(c, i[25]),
                s(c, i[7]), s(c, i[26]), s(c, i[8]), s(c, i[27]),
                s(c, i[28]), s(c, i[29]), s(c, i[30]),
                s(c, i[9]), s(c, i[10]), s(c, i[11]), s(c, i[12]), s(c, i[13]),
                c.getInt(i[14]), s(c, i[15]), s(c, i[16]), s(c, i[17]),
                s(c, i[18]), s(c, i[19]),
                c.getInt(i[20]) == 1, c.getInt(i[21]), c.getLong(i[22]),
                c.getInt(i[23]) == 1, c.getInt(i[31]) == 1);
    }

    private static String s(Cursor c, int i) {
        if (c.isNull(i)) return "";
        String v = c.getString(i);
        return v == null ? "" : v;
    }

    /** তালিকায় ছোট করে দেখানোর জন্য এক লাইনের পরিচয়। */
    public String subtitle() {
        StringBuilder sb = new StringBuilder();
        if (enName.length() > 0) sb.append(enName);
        if (sciName.length() > 0) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(sciName);
        }
        return sb.toString();
    }

    /** শেয়ার করার জন্য পাঠ্য। */
    public String shareText() {
        StringBuilder sb = new StringBuilder();
        sb.append(bnName).append('\n');
        if (enName.length() > 0) sb.append(enName).append('\n');
        if (sciName.length() > 0) sb.append(sciName);
        if (authority.length() > 0) sb.append(" ").append(authority);
        sb.append('\n').append(groupBn).append(" · ").append(familyBn);
        if (habitatBn.length() > 0) sb.append('\n').append(habitatBn);
        return sb.toString();
    }
}

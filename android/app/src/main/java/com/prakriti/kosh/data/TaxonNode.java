package com.prakriti.kosh.data;

import android.database.Cursor;

/** শ্রেণি / বর্গ / পরিবার — শ্রেণিবিন্যাসের একটি ধাপ। */
public final class TaxonNode {
    public static final int LEVEL_CLASS = 0;
    public static final int LEVEL_ORDER = 1;
    public static final int LEVEL_FAMILY = 2;

    public final int level;
    public final String groupId;
    public final String classId;
    public final String orderId;
    public final String id;
    public final String sciName;
    public final String bnName;
    public final int speciesCount;
    public final int childCount;
    public final int sortOrder;

    public TaxonNode(int level, String groupId, String classId, String orderId, String id,
                     String sciName, String bnName, int speciesCount, int childCount,
                     int sortOrder) {
        this.level = level;
        this.groupId = groupId;
        this.classId = classId;
        this.orderId = orderId;
        this.id = id;
        this.sciName = sciName;
        this.bnName = bnName;
        this.speciesCount = speciesCount;
        this.childCount = childCount;
        this.sortOrder = sortOrder;
    }

    /** taxon_class: class_id, group_id, sci_name, bn_name, species_count, order_count, sort_order */
    public static TaxonNode classFrom(Cursor c) {
        return new TaxonNode(LEVEL_CLASS, c.getString(1), c.getString(0), "", c.getString(0),
                c.getString(2), c.getString(3), c.getInt(4), c.getInt(5), c.getInt(6));
    }

    /** taxon_order: order_id, group_id, class_id, sci, bn, species_count, family_count, sort */
    public static TaxonNode orderFrom(Cursor c) {
        return new TaxonNode(LEVEL_ORDER, c.getString(1), c.getString(2), c.getString(0),
                c.getString(0), c.getString(3), c.getString(4), c.getInt(5), c.getInt(6),
                c.getInt(7));
    }

    /** taxon_family: family_id, group_id, class_id, order_id, sci, bn, count, sort */
    public static TaxonNode familyFrom(Cursor c) {
        return new TaxonNode(LEVEL_FAMILY, c.getString(1), c.getString(2), c.getString(3),
                c.getString(0), c.getString(4), c.getString(5), c.getInt(6), 0, c.getInt(7));
    }

    public boolean hasChildren() {
        return level != LEVEL_FAMILY;
    }
}

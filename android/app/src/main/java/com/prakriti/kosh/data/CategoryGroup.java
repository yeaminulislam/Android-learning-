package com.prakriti.kosh.data;

import android.database.Cursor;

/** প্রধান বিভাগ (category_group টেবিল)। */
public final class CategoryGroup {
    public final String groupId;
    public final String bnName;
    public final String enName;
    public final String emoji;
    public final String color;
    public final String blurb;
    public final int speciesCount;
    public final int classCount;
    public final int orderCount;
    public final int familyCount;
    public final int sortOrder;

    public CategoryGroup(String groupId, String bnName, String enName, String emoji,
                         String color, String blurb, int speciesCount, int classCount,
                         int orderCount, int familyCount, int sortOrder) {
        this.groupId = groupId;
        this.bnName = bnName;
        this.enName = enName;
        this.emoji = emoji;
        this.color = color;
        this.blurb = blurb;
        this.speciesCount = speciesCount;
        this.classCount = classCount;
        this.orderCount = orderCount;
        this.familyCount = familyCount;
        this.sortOrder = sortOrder;
    }

    public static CategoryGroup from(Cursor c) {
        return new CategoryGroup(c.getString(0), c.getString(1), c.getString(2),
                c.getString(3), c.getString(4), c.isNull(5) ? "" : c.getString(5),
                c.getInt(6), c.getInt(7), c.getInt(8), c.getInt(9), c.getInt(10));
    }
}

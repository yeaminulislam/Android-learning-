package com.prakriti.kosh.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ব্যবহারকারীর নিজের ডেটা — পছন্দের প্রজাতি, নোট ও সেটিংস।
 * আলাদা ছোট ডেটাবেসে থাকে, তাই মূল (পঠনমাত্র) ডেটাবেস কখনো বদলায় না।
 */
public final class UserStore {

    private static final UserStore INSTANCE = new UserStore();
    private Context appContext;
    private SQLiteDatabase db;

    private UserStore() {
    }

    public static UserStore get() {
        return INSTANCE;
    }

    public void init(Context c) {
        this.appContext = c.getApplicationContext();
        this.db = DatabaseManager.get().user(appContext);
    }

    private SQLiteDatabase db() {
        if (db == null && appContext != null) db = DatabaseManager.get().user(appContext);
        return db;
    }

    // ------------------------------------------------------------ পছন্দ

    public boolean isFavorite(long id) {
        Cursor c = db().rawQuery("SELECT 1 FROM favorite WHERE species_id=?",
                new String[]{Long.toString(id)});
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    public void setFavorite(long id, boolean on) {
        if (on) {
            ContentValues v = new ContentValues();
            v.put("species_id", id);
            v.put("added_at", System.currentTimeMillis());
            db().insertWithOnConflict("favorite", null, v, SQLiteDatabase.CONFLICT_REPLACE);
        } else {
            db().delete("favorite", "species_id=?", new String[]{Long.toString(id)});
        }
    }

    public boolean toggleFavorite(long id) {
        boolean now = !isFavorite(id);
        setFavorite(id, now);
        return now;
    }

    public int favoriteCount() {
        Cursor c = db().rawQuery("SELECT COUNT(*) FROM favorite", null);
        try {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            c.close();
        }
    }

    public List<Long> favoriteIds() {
        List<Long> out = new ArrayList<Long>();
        Cursor c = db().rawQuery("SELECT species_id FROM favorite ORDER BY added_at DESC", null);
        try {
            while (c.moveToNext()) out.add(c.getLong(0));
        } finally {
            c.close();
        }
        return out;
    }

    public void clearFavorites() {
        db().delete("favorite", null, null);
    }

    // ------------------------------------------------------------ নোট

    public String note(long id) {
        Cursor c = db().rawQuery("SELECT body FROM note WHERE species_id=?",
                new String[]{Long.toString(id)});
        try {
            return c.moveToFirst() ? c.getString(0) : "";
        } finally {
            c.close();
        }
    }

    public void setNote(long id, String body) {
        if (body == null) body = "";
        if (body.trim().length() == 0) {
            db().delete("note", "species_id=?", new String[]{Long.toString(id)});
            return;
        }
        ContentValues v = new ContentValues();
        v.put("species_id", id);
        v.put("body", body);
        v.put("updated_at", System.currentTimeMillis());
        db().insertWithOnConflict("note", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int noteCount() {
        Cursor c = db().rawQuery("SELECT COUNT(*) FROM note", null);
        try {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            c.close();
        }
    }

    public Map<Long, String> allNotes() {
        Map<Long, String> out = new HashMap<Long, String>();
        Cursor c = db().rawQuery("SELECT species_id, body FROM note", null);
        try {
            while (c.moveToNext()) out.put(c.getLong(0), c.getString(1));
        } finally {
            c.close();
        }
        return out;
    }

    public void clearNotes() {
        db().delete("note", null, null);
    }

    // ------------------------------------------------------------ সেটিংস

    public String pref(String key, String fallback) {
        Cursor c = db().rawQuery("SELECT value FROM pref WHERE key=?", new String[]{key});
        try {
            return c.moveToFirst() ? c.getString(0) : fallback;
        } finally {
            c.close();
        }
    }

    public void setPref(String key, String value) {
        ContentValues v = new ContentValues();
        v.put("key", key);
        v.put("value", value == null ? "" : value);
        db().insertWithOnConflict("pref", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public boolean prefBool(String key, boolean fallback) {
        String v = pref(key, fallback ? "1" : "0");
        return "1".equals(v) || "true".equalsIgnoreCase(v);
    }

    public void setPrefBool(String key, boolean value) {
        setPref(key, value ? "1" : "0");
    }
}

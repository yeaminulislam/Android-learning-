package com.prakriti.kosh.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;

/**
 * অফলাইন ডেটাবেস ব্যবস্থাপনা।
 *
 * ডেটাবেসটি assets-এ gzip-সংকুচিত অবস্থায় থাকে (PKDB1 হেডারসহ)। প্রথমবার
 * খোলার সময় এটি অ্যাপের নিজস্ব স্টোরেজে খুলে লেখা হয় — কোনো ইন্টারনেট লাগে না।
 *
 * PKDB1 হেডার (৪৮ বাইট):
 *   0..4   magic   "PKDB1"
 *   5..6   version (little-endian uint16)
 *   7      flags   (0 = gzip স্ট্রিম)
 *   8..15  মূল ফাইলের আকার (little-endian uint64)
 *   16..47 SHA-256 (মূল ফাইলের)
 *   48…    gzip স্ট্রিম
 */
public final class DatabaseManager {

    private static final String TAG = "PrakritiDb";
    public static final String DB_NAME = "prakriti_kosh.db";
    public static final String ASSET = "db/prakriti_kosh.db.z";
    public static final String VERSION_KEY = "schema_version";
    public static final String EXPECTED_VERSION = "3";

    private static final byte[] MAGIC = {'P', 'K', 'D', 'B', '1'};
    private static final int HEADER = 48;
    private static final int BUF = 256 * 1024;

    public interface Progress {
        /** percent 0..100, stage = বর্তমান ধাপের বাংলা বর্ণনা */
        void onProgress(int percent, String stage);
    }

    private static final DatabaseManager INSTANCE = new DatabaseManager();

    private final Object lock = new Object();
    private SQLiteDatabase speciesDb;
    private SQLiteDatabase userDb;

    private DatabaseManager() {
    }

    public static DatabaseManager get() {
        return INSTANCE;
    }

    public static File dbFile(Context c) {
        return new File(c.getFilesDir(), DB_NAME);
    }

    public static File userFile(Context c) {
        return new File(c.getFilesDir(), "prakriti_user.db");
    }

    private static File markerFile(Context c) {
        return new File(c.getFilesDir(), DB_NAME + ".ready");
    }

    /** ডেটাবেস আগে থেকে প্রস্তুত কি না। */
    public static boolean isReady(Context c) {
        File m = markerFile(c);
        if (!m.exists()) return false;
        File db = dbFile(c);
        return db.exists() && db.length() > 1024L * 1024L;
    }

    /** সংরক্ষণাগারের আকার (বাইট) — প্রগ্রেস বারের জন্য। */
    public static long assetSize(Context c) {
        InputStream in = null;
        try {
            in = c.getAssets().open(ASSET);
            return in.available();
        } catch (IOException e) {
            return 0;
        } finally {
            close(in);
        }
    }

    /**
     * প্রয়োজনে assets থেকে ডেটাবেস খুলে লেখে। ইতিমধ্যে প্রস্তুত থাকলে দ্রুত ফেরে।
     * দীর্ঘ কাজ — অবশ্যই ব্যাকগ্রাউন্ড থ্রেডে চালাতে হবে।
     */
    public void prepare(Context c, Progress progress) throws IOException {
        synchronized (lock) {
            if (isReady(c)) {
                if (progress != null) progress.onProgress(100, "প্রস্তুত");
                return;
            }
            File db = dbFile(c);
            File tmp = new File(c.getCacheDir(), DB_NAME + ".tmp");
            File par = db.getParentFile();
            if (par != null && !par.exists()) par.mkdirs();

            long total = assetSize(c);
            if (progress != null) progress.onProgress(0, "সংরক্ষণাগার খোলা হচ্ছে…");
            extract(c, tmp, total, progress);

            if (progress != null) progress.onProgress(97, "যাচাই করা হচ্ছে…");
            if (!tmp.renameTo(db)) {
                copy(tmp, db);
                tmp.delete();
            }
            writeMarker(c, db.length());
            if (progress != null) progress.onProgress(100, "প্রস্তুত!");
            Log.i(TAG, "database ready: " + db.length() + " bytes");
        }
    }

    private void extract(Context c, File out, long total, Progress progress)
            throws IOException {
        InputStream raw = null;
        GZIPInputStream gz = null;
        FileOutputStream fos = null;
        try {
            raw = c.getAssets().open(ASSET);
            // হেডার পড়া
            byte[] head = new byte[HEADER];
            int got = 0;
            while (got < HEADER) {
                int n = raw.read(head, got, HEADER - got);
                if (n < 0) break;
                got += n;
            }
            long expected = 0;
            byte[] wantHash = null;
            if (got == HEADER) {
                boolean ok = true;
                for (int i = 0; i < 5; i++) if (head[i] != MAGIC[i]) ok = false;
                if (ok) {
                    expected = readLong(head, 8);
                    wantHash = new byte[32];
                    System.arraycopy(head, 16, wantHash, 0, 32);
                    // নিরাপত্তা: gzip ম্যাজিক (1f 8b) না পাওয়া পর্যন্ত সামনের
                    // অতিরিক্ত বাইট বাদ দেওয়া হয় (ভবিষ্যতের হেডার-বদলের বিরুদ্ধে)।
                    byte[] two = new byte[2];
                    int guard = 0;
                    while (guard++ < 16) {
                        int read = 0;
                        while (read < 2) {
                            int n = raw.read(two, read, 2 - read);
                            if (n < 0) break;
                            read += n;
                        }
                        if (read < 2) break;
                        if ((two[0] & 0xFF) == 0x1f && (two[1] & 0xFF) == 0x8b) {
                            raw = new java.io.PushbackInputStream(raw, 2);
                            ((java.io.PushbackInputStream) raw).unread(two, 0, 2);
                            break;
                        }
                    }
                }
            } else {
                // হেডার নেই — পুরো ফাইলই gzip ধরে নিই
                raw.close();
                raw = c.getAssets().open(ASSET);
            }

            gz = new GZIPInputStream(raw, BUF);
            fos = new FileOutputStream(out);
            MessageDigest md = wantHash == null ? null : digest();
            byte[] buf = new byte[BUF];
            long written = 0;
            int lastPercent = -1;
            int n;
            while ((n = gz.read(buf)) > 0) {
                fos.write(buf, 0, n);
                written += n;
                if (md != null) md.update(buf, 0, n);
                if (progress != null && total > 0) {
                    int p = (int) Math.min(96, written * 96 / Math.max(total, written));
                    if (p != lastPercent) {
                        lastPercent = p;
                        progress.onProgress(p, "ডেটাবেস খোলা হচ্ছে…");
                    }
                }
            }
            fos.flush();
            fos.getFD().sync();
            if (md != null) {
                byte[] have = md.digest();
                if (!java.util.Arrays.equals(have, wantHash)) {
                    throw new IOException("checksum mismatch");
                }
            }
            if (expected > 0 && written != expected) {
                throw new IOException("size mismatch: " + written + " != " + expected);
            }
        } finally {
            close(fos);
            close(gz);
            close(raw);
        }
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            return null;
        }
    }

    private static long readLong(byte[] b, int off) {
        long v = 0;
        for (int i = 7; i >= 0; i--) v = (v << 8) | (b[off + i] & 0xFFL);
        return v;
    }

    private static void copy(File from, File to) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(from);
            out = new FileOutputStream(to);
            byte[] buf = new byte[BUF];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
        } finally {
            close(out);
            close(in);
        }
    }

    private static void writeMarker(Context c, long size) {
        RandomAccessFile f = null;
        try {
            f = new RandomAccessFile(markerFile(c), "rw");
            f.write((EXPECTED_VERSION + ":" + size).getBytes("UTF-8"));
        } catch (IOException e) {
            Log.w(TAG, "marker write failed", e);
        } finally {
            close(f);
        }
    }

    /** প্রজাতির (পঠনমাত্র) ডেটাবেস — প্রস্তুত থাকতে হবে। */
    public SQLiteDatabase species(Context c) {
        synchronized (lock) {
            if (speciesDb == null || !speciesDb.isOpen()) {
                speciesDb = open(dbFile(c), true);
            }
            return speciesDb;
        }
    }

    /** পছন্দ ও নোটের (লেখা যায় এমন) ছোট ডেটাবেস। */
    public SQLiteDatabase user(Context c) {
        synchronized (lock) {
            if (userDb == null || !userDb.isOpen()) {
                File f = userFile(c);
                userDb = open(f, false);
                userDb.execSQL("CREATE TABLE IF NOT EXISTS favorite ("
                        + "species_id INTEGER PRIMARY KEY, added_at INTEGER NOT NULL)");
                userDb.execSQL("CREATE TABLE IF NOT EXISTS note ("
                        + "species_id INTEGER PRIMARY KEY, body TEXT NOT NULL DEFAULT '', "
                        + "updated_at INTEGER NOT NULL)");
                userDb.execSQL("CREATE TABLE IF NOT EXISTS pref ("
                        + "key TEXT PRIMARY KEY, value TEXT NOT NULL)");
            }
            return userDb;
        }
    }

    private static SQLiteDatabase open(File f, boolean readOnly) {
        SQLiteDatabase db = readOnly
                ? SQLiteDatabase.openDatabase(f.getAbsolutePath(), null,
                        SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS)
                : SQLiteDatabase.openOrCreateDatabase(f, null);
        // কর্মক্ষমতা: বড় ক্যাশ, mmap
        try {
            db.execSQL("PRAGMA cache_size = -12000");
            db.execSQL("PRAGMA mmap_size = 134217728");
            db.execSQL("PRAGMA temp_store = MEMORY");
        } catch (Exception ignored) {
            // পুরোনো Android-এ সব PRAGMA নাও চলতে পারে
        }
        return db;
    }

    public void close() {
        synchronized (lock) {
            if (speciesDb != null && speciesDb.isOpen()) speciesDb.close();
            if (userDb != null && userDb.isOpen()) userDb.close();
            speciesDb = null;
            userDb = null;
        }
    }

    private static void close(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }
}

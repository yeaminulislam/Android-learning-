package com.prakriti.kosh.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;

/**
 * প্রজাতির **আসল ছবি** — APK-র ভেতরে `assets/photos/<id>.webp`।
 *
 * ছবি ইন্টারনেট থেকে নামিয়ে (উইকিমিডিয়া/ব্রিটানিকা ইত্যাদি উৎস), ছোট করে
 * WebP-তে বদলে বিল্ডের সময় assets-এ বসানো হয় (tools/photos_pack.py)।
 * যাদের ছবি জোটেনি তাদের জন্য null ফেরে — তখন প্রোগ্রামে আঁকা শিল্প-চিত্র
 * (SpeciesArt) দেখানো হয়।
 */
public final class PhotoStore {

    private PhotoStore() {
    }

    /** assets-এর ভেতরে ছবির পথ। */
    public static String path(long id) {
        return "photos/" + id + ".webp";
    }

    /** এই প্রজাতির আসল ছবি APK-তে আছে কি না। */
    public static boolean exists(Context c, long id) {
        InputStream in = null;
        try {
            in = c.getAssets().open(path(id));
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            close(in);
        }
    }

    /**
     * ছবি ডিকোড করে ফেরায় — দীর্ঘতম বাহু প্রায় maxPx রেখে (inSampleSize),
     * RGB_565-তে (অর্ধেক মেমোরি; ছবিতে স্বচ্ছতা লাগে না)। ব্যর্থ হলে null।
     */
    public static Bitmap load(Context c, long id, int maxPx) {
        InputStream in = null;
        try {
            in = c.getAssets().open(path(id));
            BitmapFactory.Options probe = new BitmapFactory.Options();
            probe.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, probe);
            close(in);
            in = null;
            if (probe.outWidth <= 0 || probe.outHeight <= 0) return null;

            int sample = 1;
            while (Math.max(probe.outWidth, probe.outHeight) / (sample * 2) >= maxPx) {
                sample *= 2;
            }
            in = c.getAssets().open(path(id));
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = sample;
            opt.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeStream(in, null, opt);
        } catch (Throwable t) {   // ছবি নেই/নষ্ট — আঁকা ছবিতেই চলবে
            return null;
        } finally {
            close(in);
        }
    }

    private static void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (Throwable ignored) {
            }
        }
    }
}

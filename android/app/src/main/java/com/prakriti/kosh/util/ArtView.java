package com.prakriti.kosh.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.LruCache;
import android.view.View;

import com.prakriti.kosh.data.Species;

/**
 * প্রজাতির ভেক্টর-চিত্র দেখানোর View।
 *
 * তালিকায় বারবার আঁকা এড়াতে ছবি RGB_565 বিটম্যাপে ক্যাশ করা হয়
 * (LruCache, অ্যাপের হিপের ~১/৮)। ছবি না থাকলে সরাসরি Canvas-এ আঁকা হয়।
 */
public class ArtView extends View {

    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>(cacheBytes()) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount();
                }
            };

    private static int cacheBytes() {
        long max = Runtime.getRuntime().maxMemory();
        int v = (int) (max / 8);
        return Math.max(2 * 1024 * 1024, Math.min(v, 24 * 1024 * 1024));
    }

    private Species species;
    private int variant;
    private boolean thumb;
    private Bitmap cached;
    private String key;

    public ArtView(Context c) {
        super(c);
        init();
    }

    public ArtView(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    public ArtView(Context c, AttributeSet a, int d) {
        super(c, a, d);
        init();
    }

    private void init() {
        setWillNotDraw(false);
    }

    public void setSpecies(Species s) {
        setSpecies(s, 0, false);
    }

    public void setSpecies(Species s, int variant, boolean thumb) {
        this.species = s;
        this.variant = variant;
        this.thumb = thumb;
        this.cached = null;
        this.key = null;
        if (s != null) {
            this.key = s.id + ":" + variant + ":" + (thumb ? "t" : "f")
                    + ":" + getWidth() + "x" + getHeight();
            Bitmap b = CACHE.get(s.id + ":" + variant + ":" + (thumb ? "t" : "f")
                    + ":" + getWidth() + "x" + getHeight());
            if (b != null && !b.isRecycled()) this.cached = b;
        }
        invalidate();
    }

    /** গ্যালারির জন্য ভিন্ন পরিবেশ। */
    public void setVariant(int v) {
        if (v == variant) return;
        setSpecies(species, v, thumb);
    }

    public static Bitmap render(int w, int h, Species s, int variant, boolean thumb) {
        if (w <= 0 || h <= 0 || s == null) return null;
        String k = s.id + ":" + variant + ":" + (thumb ? "t" : "f") + ":" + w + "x" + h;
        Bitmap b = CACHE.get(k);
        if (b != null && !b.isRecycled()) return b;
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        Canvas cv = new Canvas(out);
        if (thumb) {
            SpeciesArt.drawThumb(cv, w, h, s.id, s.classId, s.groupId, s.habitatBn,
                    s.venomLevel, s.extinct);
        } else {
            SpeciesArt.draw(cv, w, h, s.id, s.classId, s.groupId, s.habitatBn,
                    s.venomLevel, s.extinct, variant);
        }
        CACHE.put(k, out);
        return out;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (species == null) return;
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        Bitmap b = render(w, h, species, variant, thumb);
        if (b != null) {
            canvas.drawBitmap(b, 0, 0, null);
        } else if (thumb) {
            SpeciesArt.drawThumb(canvas, w, h, species.id, species.classId, species.groupId,
                    species.habitatBn, species.venomLevel, species.extinct);
        } else {
            SpeciesArt.draw(canvas, w, h, species.id, species.classId, species.groupId,
                    species.habitatBn, species.venomLevel, species.extinct, variant);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cached = null;
        if (species != null) {
            Bitmap b = CACHE.get(species.id + ":" + variant + ":" + (thumb ? "t" : "f")
                    + ":" + w + "x" + h);
            if (b != null && !b.isRecycled()) cached = b;
        }
    }

    public static void clearCache() {
        CACHE.evictAll();
    }
}

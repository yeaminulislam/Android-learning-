package com.prakriti.kosh.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.prakriti.kosh.R;
import com.prakriti.kosh.data.Species;

import java.util.ArrayList;
import java.util.List;

/**
 * বিস্তারিত পর্দের ছবি-গ্যালারি: বাম/ডান সোয়াইপে ভিন্ন পরিবেশ, নিচে বিন্দু-নির্দেশক।
 * ViewPager ছাড়াই (কোনো লাইব্রেরি নেই) সাধারণ FrameLayout + স্পর্শ-শনাক্তকরণ।
 */
public class ArtPager extends FrameLayout {

    public interface OnVariantChange {
        void onVariant(int variant);
    }

    private final ImageView image;
    private final LinearLayout dots;
    private final TextView caption;
    private Species species;
    private int variant;
    private int size = 600;
    private float downX, downY;
    private boolean swiping;
    private OnVariantChange listener;
    private int artW = 1080, artH = 720;

    private static final String[] CAPTIONS = {
            "স্বাভাবিক পরিবেশে", "মুক্ত আকাশে", "সোনালি আলোয়", "রাতের আঁধারে"
    };

    public ArtPager(Context c) {
        super(c);
        image = new ImageView(c);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setBackgroundColor(Color.TRANSPARENT);
        addView(image, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout bottom = new FrameLayout(c);
        android.graphics.drawable.GradientDrawable scrim = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, Ui.withAlpha(0xFF000000, 130)});
        bottom.setBackground(scrim);
        addView(bottom, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Ui.dp(c, 54), Gravity.BOTTOM));

        LinearLayout inner = Ui.vbox(c);
        inner.setGravity(Gravity.CENTER_HORIZONTAL);
        bottom.addView(inner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        caption = new TextView(c);
        caption.setTextColor(Color.WHITE);
        caption.setTextSize(12f);
        caption.setGravity(Gravity.CENTER);
        inner.addView(caption);

        dots = Ui.hbox(c);
        dots.setGravity(Gravity.CENTER);
        Ui.margins2(dots, 0, Ui.dp(c, 3), 0, Ui.dp(c, 5));
        inner.addView(dots, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < SpeciesArt.VARIANTS; i++) {
            View d = new View(c);
            d.setBackground(Ui.oval(Ui.withAlpha(0xFFFFFFFF, 110)));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(Ui.dp(c, 6), Ui.dp(c, 6));
            p.setMargins(Ui.dp(c, 3), 0, Ui.dp(c, 3), 0);
            d.setTag(i);
            d.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setVariant((Integer) v.getTag());
                }
            });
            dots.addView(d, p);
        }
        setWillNotDraw(false);
    }

    public void setOnVariantChange(OnVariantChange l) {
        this.listener = l;
    }

    public void setSpecies(Species s, int w, int h) {
        this.species = s;
        this.artW = Math.max(320, w);
        this.artH = Math.max(240, h);
        this.variant = 0;
        render();
    }

    public void setVariant(int v) {
        if (v < 0) v = SpeciesArt.VARIANTS - 1;
        if (v >= SpeciesArt.VARIANTS) v = 0;
        if (v == variant) return;
        variant = v;
        render();
        if (listener != null) listener.onVariant(v);
    }

    public int getVariant() {
        return variant;
    }

    private void render() {
        if (species == null) return;
        Bitmap b = ArtView.render(artW, artH, species, variant, false);
        if (b != null) image.setImageBitmap(b);
        caption.setText(CAPTIONS[variant % CAPTIONS.length]);
        for (int i = 0; i < dots.getChildCount(); i++) {
            View d = dots.getChildAt(i);
            d.setBackground(Ui.oval(i == variant ? Color.WHITE : Ui.withAlpha(0xFFFFFFFF, 100)));
            d.setScaleX(i == variant ? 1.5f : 1f);
            d.setScaleY(i == variant ? 1.5f : 1f);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(android.view.MotionEvent e) {
        switch (e.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                swiping = false;
                break;
            case android.view.MotionEvent.ACTION_MOVE:
                float dx = e.getX() - downX;
                float dy = e.getY() - downY;
                if (Math.abs(dx) > Ui.dp(getContext(), 22) && Math.abs(dx) > Math.abs(dy) * 1.6f) {
                    swiping = true;
                    return true;
                }
                break;
            default:
                break;
        }
        return swiping;
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent e) {
        switch (e.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                return true;
            case android.view.MotionEvent.ACTION_UP:
                float dx = e.getX() - downX;
                float dy = e.getY() - downY;
                if (Math.abs(dx) > Ui.dp(getContext(), 40) && Math.abs(dx) > Math.abs(dy)) {
                    setVariant(dx < 0 ? variant + 1 : variant - 1);
                } else if (Math.abs(dx) < Ui.dp(getContext(), 6) && Math.abs(dy) < Ui.dp(getContext(), 6)) {
                    setVariant(variant + 1);   // ট্যাপ করলেও পরের ছবি
                }
                return true;
            default:
                return true;
        }
    }
}

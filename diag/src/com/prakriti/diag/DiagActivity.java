package com.prakriti.diag;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * ইনস্টল-পরীক্ষা অ্যাপ (কয়েক কিলোবাইট)।
 *
 * উদ্দেশ্য: ব্যবহারকারীর ফোনে আমাদের স্বাক্ষরিত APK ইনস্টল ও চালু হয় কি না তা
 * নিশ্চিত হওয়া। এটি খুললেই ডিভাইসের তথ্য দেখা যায় — যদি এটিও ইনস্টল না হয়,
 * সমস্যাটি প্রকৃতি কোষ অ্যাপে নয়, ডিভাইস/সেটিংসে।
 */
public class DiagActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#E8F5E9"));
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView tick = new TextView(this);
        tick.setText("\u2705");
        tick.setTextSize(56f);
        tick.setGravity(Gravity.CENTER);
        root.addView(tick);

        TextView head = new TextView(this);
        head.setText("ইনস্টল ঠিকঠাক কাজ করছে");
        head.setTextSize(22f);
        head.setTextColor(Color.parseColor("#1B5E20"));
        head.setTypeface(Typeface.DEFAULT_BOLD);
        head.setGravity(Gravity.CENTER);
        root.addView(head);

        TextView sub = new TextView(this);
        sub.setText("এই ছোট অ্যাপটি খোলার মানে — আপনার ফোনে আমাদের বিল্ড করা,\n"
                + "debug-কীতে স্বাক্ষরিত APK ইনস্টল ও চালু হতে পারে।\n\n"
                + "এখন “প্রকৃতি কোষ” অ্যাপটি ইনস্টল করুন। সেটিও একইভাবে\n"
                + "ইনস্টল হওয়ার কথা। না হলে নিচের তথ্যসহ জানান।");
        sub.setTextSize(14f);
        sub.setTextColor(Color.parseColor("#2E4B33"));
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(10), 0, dp(18));
        sub.setLineSpacing(dp(3), 1f);
        root.addView(sub);

        TextView info = new TextView(this);
        info.setText(deviceInfo());
        info.setTextSize(12f);
        info.setTypeface(Typeface.MONOSPACE);
        info.setTextColor(Color.parseColor("#14230F"));
        info.setBackgroundColor(Color.WHITE);
        info.setPadding(pad, pad, pad, pad);
        root.addView(info, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        ScrollView sc = new ScrollView(this);
        sc.addView(root, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        setContentView(sc);
    }

    private String deviceInfo() {
        StringBuilder s = new StringBuilder();
        s.append("[ ডিভাইসের তথ্য ]\n");
        s.append("Android : ").append(Build.VERSION.RELEASE)
                .append("  (API ").append(Build.VERSION.SDK_INT).append(")\n");
        s.append("মডেল    : ").append(Build.MANUFACTURER).append(' ')
                .append(Build.MODEL).append('\n');
        s.append("ডিভাইস  : ").append(Build.DEVICE).append('\n');
        s.append("ABI     : ").append(Build.CPU_ABI);
        if (Build.VERSION.SDK_INT >= 21) {
            String[] abis = Build.SUPPORTED_ABIS;
            if (abis != null && abis.length > 0) {
                s.setLength(s.length() - Build.CPU_ABI.length());
                for (int i = 0; i < abis.length; i++) {
                    s.append(i == 0 ? "" : ", ").append(abis[i]);
                }
            }
        }
        s.append('\n');
        s.append("স্ক্রিন   : ").append(getResources().getDisplayMetrics().widthPixels)
                .append('x').append(getResources().getDisplayMetrics().heightPixels)
                .append(" @ ").append(Math.round(getResources().getDisplayMetrics().density * 160))
                .append(" dpi\n");
        try {
            StatFs st = new StatFs(getFilesDir().getAbsolutePath());
            long mb = 1024 * 1024;
            long free = (st.getAvailableBlocksLong() * st.getBlockSizeLong()) / mb;
            long total = (st.getBlockCountLong() * st.getBlockSizeLong()) / mb;
            s.append("স্টোরেজ : মুক্ত ").append(free).append(" MB / মোট ")
                    .append(total).append(" MB\n");
        } catch (Throwable t) {
            s.append("স্টোরেজ : মাপা যায়নি (").append(t.getClass().getSimpleName()).append(")\n");
        }
        try {
            android.app.ActivityManager am = (android.app.ActivityManager)
                    getSystemService(ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo mi =
                    new android.app.ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            s.append("RAM     : মুক্ত ").append(mi.availMem / (1024 * 1024))
                    .append(" MB / মোট ").append(mi.totalMem / (1024 * 1024))
                    .append(mi.lowMemory ? " MB  (কম!)" : " MB").append('\n');
        } catch (Throwable t) {
            s.append("RAM     : মাপা যায়নি\n");
        }
        s.append("ভাষা    : ").append(java.util.Locale.getDefault()).append('\n');
        s.append("সময়     : ").append(new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                .format(new java.util.Date()));
        s.append("\n\nপ্রকৃতি কোষ ইনস্টল করতে প্রায় ৫১০ মেগাবাইট মুক্ত জায়গা দরকার\n"
                + "(৭৬ MB APK + ৪৩১ MB ডেটাবেস)। ছোট সংস্করণে লাগে প্রায় ৪১ MB।");
        return s.toString();
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}

package com.prakriti.kosh;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.prakriti.kosh.db.DatabaseManager;
import com.prakriti.kosh.db.Repository;
import com.prakriti.kosh.db.UserStore;
import com.prakriti.kosh.util.ArtView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * অ্যাপের এন্ট্রি পয়েন্ট।
 *
 * একটিমাত্র ব্যাকগ্রাউন্ড এক্সিকিউটর ও প্রধান থ্রেডের Handler সব স্ক্রিন ভাগ করে
 * নেয় — ডেটাবেসের কাজ কখনো UI থ্রেডে হয় না।
 */
public class PrakritiApp extends Application {

    public interface Ready {
        void onReady(boolean ok, String error);
    }

    private static PrakritiApp instance;

    private ExecutorService executor;
    private Handler main;
    private final AtomicBoolean preparing = new AtomicBoolean(false);
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private volatile String lastError = "";

    public static PrakritiApp get() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        executor = Executors.newFixedThreadPool(3);
        main = new Handler(Looper.getMainLooper());
        if (DatabaseManager.isReady(this)) {
            Repository.get().init(this);
            UserStore.get().init(this);
            ready.set(true);
        }
    }

    /** ব্যাকগ্রাউন্ড কাজ (ডেটাবেস পড়া, প্রস্তুত করা)। */
    public void async(Runnable work) {
        executor.execute(work);
    }

    /** ফলাফল প্রধান থ্রেডে পাঠানো। */
    public void post(Runnable work) {
        main.post(work);
    }

    public boolean isReady() {
        return ready.get();
    }

    public String lastError() {
        return lastError;
    }

    /** অ্যাপের সামান্য পছন্দসমূহ (intro দেখা হয়েছে কি না ইত্যাদি)। */
    public UserStore prefs() {
        return UserStore.get();
    }

    /**
     * প্রস্তুত করা ডেটাবেস মুছে আবার শুরু থেকে প্রস্তুত করে (ব্যবহারকারীর পছন্দ
     * ও নোট অক্ষত থাকে)।
     */
    public void resetDatabase() {
        DatabaseManager.get().close();
        ready.set(false);
        preparing.set(false);
        java.io.File f = DatabaseManager.dbFile(this);
        if (f.exists()) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }

    /**
     * ডেটাবেস প্রস্তুত করে Repository/UserStore চালু করে। ইতিমধ্যে প্রস্তুত থাকলে
     * সঙ্গে সঙ্গেই onReady ডাকে।
     *
     * @param progress প্রগ্রেস কলব্যাক (ব্যাকগ্রাউন্ড থ্রেডে ডাকা হয়)
     */
    public void ensureReady(final DatabaseManager.Progress progress, final Ready onReady) {
        if (ready.get()) {
            if (onReady != null) onReady.onReady(true, "");
            return;
        }
        if (!preparing.compareAndSet(false, true)) {
            // আরেকটি থ্রেড ইতিমধ্যে প্রস্তুত করছে — অপেক্ষা করি
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    while (!ready.get() && preparing.get()) {
                        try {
                            Thread.sleep(80);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    if (onReady != null) {
                        main.post(new Runnable() {
                            @Override
                            public void run() {
                                onReady.onReady(ready.get(), lastError);
                            }
                        });
                    }
                }
            });
            return;
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                boolean ok = false;
                try {
                    DatabaseManager.get().prepare(PrakritiApp.this, progress);
                    Repository.get().init(PrakritiApp.this);
                    UserStore.get().init(PrakritiApp.this);
                    ok = true;
                    lastError = "";
                } catch (final Throwable t) {
                    lastError = t.getClass().getSimpleName() + ": "
                            + (t.getMessage() == null ? "" : t.getMessage());
                } finally {
                    ready.set(ok);
                    preparing.set(false);
                    final boolean done = ok;
                    if (onReady != null) {
                        main.post(new Runnable() {
                            @Override
                            public void run() {
                                onReady.onReady(done, lastError);
                            }
                        });
                    }
                }
            }
        });
    }

    /** হিপ চাপ কমাতে ছবির ক্যাশ খালি করা। */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_BACKGROUND) ArtView.clearCache();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ArtView.clearCache();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        DatabaseManager.get().close();
        if (executor != null) executor.shutdownNow();
    }
}

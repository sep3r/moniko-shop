package com.monikoshop.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * محدودیت تلاش ناموفق به‌ازای «کلید» (ایمیل) تا حمله‌ی توزیع‌شده از چند IP هم جلوی یک حساب را نگیرد.
 * ۱۰ تلاش ناموفق در ۱۵ دقیقه ⇒ همان کلید تا پایان پنجره مسدود می‌شود.
 * در حافظه‌ی همین نمونه‌ی سرور نگه داشته می‌شود (برای چند نمونه باید Redis شود).
 * کلید برای ایمیل‌های ناموجود هم ساخته می‌شود، پس وجود/نبود حساب لو نمی‌رود.
 */
@Component
public class LoginAttemptService {

    static final long WINDOW_MS = 15 * 60 * 1000L;
    static final int MAX_FAILURES = 10;
    private static final int MAX_KEYS = 100_000;

    // value = {failures, windowStart}
    private final ConcurrentHashMap<String, long[]> failures = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        return isBlocked(key, System.currentTimeMillis());
    }

    boolean isBlocked(String key, long now) {
        long[] e = failures.get(normalize(key));
        if (e == null) {
            return false;
        }
        synchronized (e) {
            return now - e[1] <= WINDOW_MS && e[0] >= MAX_FAILURES;
        }
    }

    public void recordFailure(String key) {
        recordFailure(key, System.currentTimeMillis());
    }

    void recordFailure(String key, long now) {
        if (failures.size() > MAX_KEYS) {
            failures.entrySet().removeIf(en -> now - en.getValue()[1] > WINDOW_MS);
        }
        long[] e = failures.computeIfAbsent(normalize(key), k -> new long[]{0, now});
        synchronized (e) {
            if (now - e[1] > WINDOW_MS) {
                e[0] = 0;
                e[1] = now;
            }
            e[0]++;
        }
    }

    public void clear(String key) {
        failures.remove(normalize(key));
    }

    private static String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }
}

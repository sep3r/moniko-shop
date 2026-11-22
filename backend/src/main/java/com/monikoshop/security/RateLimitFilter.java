package com.monikoshop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory brute-force protection (single backend instance):
 *  - login: too many failed attempts from one IP are temporarily blocked
 *  - register: limited number of sign-ups per IP per hour
 * Limits are intentionally generous because many users can share one IP (mobile carriers / NAT).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long LOGIN_WINDOW_MS = 15 * 60 * 1000L;
    private static final long LOGIN_MAX_FAILURES = 20;
    private static final long REGISTER_WINDOW_MS = 60 * 60 * 1000L;
    private static final long REGISTER_MAX = 20;
    private static final long FORGOT_MAX = 10;
    private static final long RESET_MAX = 30;
    private static final long CHANGE_PW_MAX_FAILURES = 10;
    private static final long CATALOG_MAX_PER_MIN = 30;
    private static final long SEARCH_MAX_PER_MIN = 120;
    private static final long ORDER_WINDOW_MS = 60 * 60 * 1000L;
    private static final long ORDER_MAX_PER_HOUR = 20;
    private static final int MAX_KEYS = 100_000;

    // value = {count, windowStart}
    private final ConcurrentHashMap<String, long[]> loginFailures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, long[]> registrations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, long[]> changePasswordFailures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, long[]> readHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, long[]> orderHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, long[]> trackHits = new ConcurrentHashMap<>();

    private static final long TRACK_WINDOW_MS = 60 * 1000L;
    /** heartbeat هر ~۲۰ ثانیه است؛ سقف سخاوتمندانه چون چند نفر ممکن است پشت یک IP باشند. */
    private static final long TRACK_MAX_PER_MIN = 300;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // لیست کامل کاتالوگ و جستجو: سقف سخاوتمندانه (چند نفر ممکن است پشت یک IP باشند) تا scraping/فشار الکی جلوگیری شود
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            long max = "/api/products".equals(path) ? CATALOG_MAX_PER_MIN
                    : "/api/products/search".equals(path) ? SEARCH_MAX_PER_MIN : 0;
            if (max > 0) {
                String key = path + "|" + clientIp(request);
                if (isLimited(readHits, key, TRACK_WINDOW_MS, max)) {
                    response.setStatus(429);
                    response.setHeader("Retry-After", "60");
                    return;
                }
                hit(readHits, key, TRACK_WINDOW_MS);
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("/api/track/heartbeat".equals(path)) {
            String ip = clientIp(request);
            if (isLimited(trackHits, ip, TRACK_WINDOW_MS, TRACK_MAX_PER_MIN)) {
                response.setStatus(429);
                return;
            }
            hit(trackHits, ip, TRACK_WINDOW_MS);
            filterChain.doFilter(request, response);
            return;
        }

        if ("/api/auth/login".equals(path)) {
            String ip = clientIp(request);
            if (isLimited(loginFailures, ip, LOGIN_WINDOW_MS, LOGIN_MAX_FAILURES)) {
                reject(response);
                return;
            }
            filterChain.doFilter(request, response);
            int status = response.getStatus();
            if (status == 200) {
                loginFailures.remove(ip);
            } else if (status == 400 || status == 401) {
                hit(loginFailures, ip, LOGIN_WINDOW_MS);
            }
            return;
        }

        if ("/api/orders".equals(path)) {
            String key = clientIp(request);
            if (isLimited(orderHits, key, ORDER_WINDOW_MS, ORDER_MAX_PER_HOUR)) {
                reject(response);
                return;
            }
            hit(orderHits, key, ORDER_WINDOW_MS);
            filterChain.doFilter(request, response);
            return;
        }

        if ("/api/auth/change-password".equals(path)) {
            // جلوگیری از حدس زدن رمز فعلی با توکن دزدیده‌شده: ۱۰ تلاش ناموفق در ۱۵ دقیقه
            String ip = clientIp(request);
            if (isLimited(changePasswordFailures, ip, LOGIN_WINDOW_MS, CHANGE_PW_MAX_FAILURES)) {
                reject(response);
                return;
            }
            filterChain.doFilter(request, response);
            int status = response.getStatus();
            if (status == 200) {
                changePasswordFailures.remove(ip);
            } else if (status == 400) {
                hit(changePasswordFailures, ip, LOGIN_WINDOW_MS);
            }
            return;
        }

        boolean register = "/api/auth/register".equals(path);
        boolean forgot = "/api/auth/forgot-password".equals(path);
        boolean reset = "/api/auth/reset-password".equals(path);
        if (register || forgot || reset) {
            String key = path + "|" + clientIp(request);
            long max = register ? REGISTER_MAX : (forgot ? FORGOT_MAX : RESET_MAX);
            if (isLimited(registrations, key, REGISTER_WINDOW_MS, max)) {
                reject(response);
                return;
            }
            hit(registrations, key, REGISTER_WINDOW_MS);
        }

        filterChain.doFilter(request, response);
    }

    private static String clientIp(HttpServletRequest request) {
        return ClientIp.of(request);
    }

    private static boolean isLimited(ConcurrentHashMap<String, long[]> map, String key, long windowMs, long max) {
        long[] entry = map.get(key);
        if (entry == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        synchronized (entry) {
            if (now - entry[1] > windowMs) {
                return false;
            }
            return entry[0] >= max;
        }
    }

    private static void hit(ConcurrentHashMap<String, long[]> map, String key, long windowMs) {
        long now = System.currentTimeMillis();
        if (map.size() > MAX_KEYS) {
            map.entrySet().removeIf(e -> now - e.getValue()[1] > windowMs);
        }
        long[] entry = map.computeIfAbsent(key, k -> new long[]{0, now});
        synchronized (entry) {
            if (now - entry[1] > windowMs) {
                entry[0] = 0;
                entry[1] = now;
            }
            entry[0]++;
        }
    }

    private static void reject(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", "900");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"تعداد تلاش‌ها بیش از حد مجاز است. چند دقیقه بعد دوباره تلاش کنید\"}");
    }
}

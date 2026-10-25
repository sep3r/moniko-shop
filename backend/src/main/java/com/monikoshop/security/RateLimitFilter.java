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
    private static final int MAX_KEYS = 100_000;

    // value = {count, windowStart}
    private final ConcurrentHashMap<String, long[]> loginFailures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, long[]> registrations = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

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
        // The platform proxy appends the real client address; the last entry cannot be forged by the client.
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            String last = parts[parts.length - 1].trim();
            if (!last.isEmpty() && last.length() <= 64) {
                return last;
            }
        }
        return request.getRemoteAddr();
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

package com.monikoshop.security;

import jakarta.servlet.http.HttpServletRequest;

/** آدرس IP و User-Agent درخواست؛ هم برای محدودیت تعداد درخواست و هم برای لاگ استفاده می‌شود. */
public final class ClientIp {

    private ClientIp() { }

    /**
     * پروکسی پلتفرم آدرس واقعی کلاینت را به انتهای X-Forwarded-For اضافه می‌کند؛
     * آخرین مقدار را کلاینت نمی‌تواند جعل کند (به شرط اینکه پروکسی واقعاً اضافه کند).
     */
    public static String of(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            String last = parts[parts.length - 1].trim();
            if (!last.isEmpty() && last.length() <= 64) {
                return last;
            }
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }

    public static String userAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null) {
            return null;
        }
        ua = ua.replaceAll("[\\r\\n]", " ").trim();
        return ua.length() > 255 ? ua.substring(0, 255) : ua;
    }
}

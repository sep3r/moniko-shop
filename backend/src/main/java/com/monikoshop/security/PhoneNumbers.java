package com.monikoshop.security;

/**
 * نرمال‌سازی شماره موبایل ایران به شکل استاندارد 09123456789.
 * ورودی می‌تواند ارقام فارسی/عربی، فاصله، خط‌تیره و پیشوند +98 / 0098 / 98 داشته باشد.
 */
public final class PhoneNumbers {

    private PhoneNumbers() { }

    /** @return شماره‌ی نرمال‌شده (09xxxxxxxxx) یا null اگر موبایل معتبر ایران نباشد */
    public static String normalize(String raw) {
        if (raw == null || raw.length() > 25) {
            return null;
        }
        String d = digitsOnly(raw);
        if (d.startsWith("0098") && d.length() == 14) {
            d = "0" + d.substring(4);
        } else if (d.startsWith("98") && d.length() == 12) {
            d = "0" + d.substring(2);
        } else if (d.startsWith("9") && d.length() == 10) {
            d = "0" + d;
        }
        return d.matches("^09\\d{9}$") ? d : null;
    }

    /** ارقام فارسی و عربی را انگلیسی می‌کند و هر چیز غیر از رقم را حذف می‌کند. */
    public static String digitsOnly(String raw) {
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '\u06F0' && c <= '\u06F9') {
                sb.append((char) ('0' + (c - '\u06F0')));
            } else if (c >= '\u0660' && c <= '\u0669') {
                sb.append((char) ('0' + (c - '\u0660')));
            } else if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

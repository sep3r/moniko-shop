package com.monikoshop.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PhoneNumbersTest {

    @Test
    void normalizesCommonFormats() {
        assertEquals("09123456789", PhoneNumbers.normalize("09123456789"));
        assertEquals("09123456789", PhoneNumbers.normalize("+98 912 345 6789"));
        assertEquals("09123456789", PhoneNumbers.normalize("0098-912-345-6789"));
        assertEquals("09123456789", PhoneNumbers.normalize("989123456789"));
        assertEquals("09123456789", PhoneNumbers.normalize("9123456789"));
    }

    @Test
    void acceptsPersianAndArabicDigits() {
        assertEquals("09123456789", PhoneNumbers.normalize("\u06F0\u06F9\u06F1\u06F2\u06F3\u06F4\u06F5\u06F6\u06F7\u06F8\u06F9"));
        assertEquals("09123456789", PhoneNumbers.normalize("\u0660\u0669\u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669"));
    }

    @Test
    void rejectsInvalidNumbers() {
        assertNull(PhoneNumbers.normalize(null));
        assertNull(PhoneNumbers.normalize(""));
        assertNull(PhoneNumbers.normalize("admin@monikoshop.ir"));
        assertNull(PhoneNumbers.normalize("02112345678"));
        assertNull(PhoneNumbers.normalize("0912345678"));
        assertNull(PhoneNumbers.normalize("091234567890"));
    }
}

package com.monikoshop.dto;

/** پاسخ درخواست کد: مدت اعتبار کد و فاصله‌ی مجاز تا درخواست بعدی (ثانیه). */
public record OtpSentResponse(String message, long expiresInSeconds, long resendAfterSeconds) { }

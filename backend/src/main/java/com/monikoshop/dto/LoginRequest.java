package com.monikoshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    /** شماره موبایل (برای حساب‌های قدیمی/مدیر، ایمیل هم پذیرفته می‌شود). */
    @NotBlank(message = "شماره موبایل الزامی است")
    @Size(max = 100)
    private String username;

    @NotBlank(message = "رمز عبور الزامی است")
    @Size(max = 128)
    private String password;

    /** «مرا به خاطر بسپار»: توکن ۳ روزه به‌جای توکن کوتاه‌مدت. اختیاری. */
    private Boolean rememberMe;
}

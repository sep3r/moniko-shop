package com.monikoshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "رمز عبور فعلی الزامی است")
    @Size(max = 128, message = "رمز عبور فعلی نامعتبر است")
    private String currentPassword;

    @NotBlank(message = "رمز عبور جدید الزامی است")
    @Size(min = 8, max = 64, message = "رمز عبور باید ۸ تا ۶۴ کاراکتر باشد")
    @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).+$", message = "رمز عبور باید شامل حداقل یک حرف و یک عدد باشد")
    private String newPassword;

    /** کد پیامکی (برای حساب‌هایی که شماره‌ی تأییدشده دارند الزامی است). */
    @Pattern(regexp = "^([0-9\\u06F0-\\u06F9\\u0660-\\u0669]{6})?$", message = "کد تأیید ۶ رقمی را وارد کنید")
    private String code;

    /** توکن تازه هم‌مدت با نشست فعلی صادر شود (اگر کاربر «مرا به خاطر بسپار» را زده بود). */
    private Boolean rememberMe;
}

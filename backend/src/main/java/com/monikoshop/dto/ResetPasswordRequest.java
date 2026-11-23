package com.monikoshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "شماره موبایل الزامی است")
    @Size(max = 25, message = "شماره موبایل نامعتبر است")
    private String phone;

    @NotBlank(message = "کد تأیید الزامی است")
    @Pattern(regexp = "^[0-9\\\\u06F0-\\\\u06F9\\\\u0660-\\\\u0669]{6}$", message = "کد تأیید ۶ رقمی را وارد کنید")
    private String code;

    @NotBlank(message = "رمز عبور جدید الزامی است")
    @Size(min = 8, max = 64, message = "رمز عبور باید ۸ تا ۶۴ کاراکتر باشد")
    @Pattern(regexp = "^(?=.*\\\\p{L})(?=.*\\\\d).+$", message = "رمز عبور باید شامل حداقل یک حرف و یک عدد باشد")
    private String newPassword;
}

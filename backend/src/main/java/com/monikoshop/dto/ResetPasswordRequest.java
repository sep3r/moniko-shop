package com.monikoshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "توکن بازیابی الزامی است")
    @Size(max = 200, message = "توکن نامعتبر است")
    private String token;

    @NotBlank(message = "رمز عبور جدید الزامی است")
    @Size(min = 8, max = 64, message = "رمز عبور باید ۸ تا ۶۴ کاراکتر باشد")
    @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).+$", message = "رمز عبور باید شامل حداقل یک حرف و یک عدد باشد")
    private String newPassword;
}

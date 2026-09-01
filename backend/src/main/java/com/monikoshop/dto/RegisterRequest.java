package com.monikoshop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "نام کامل الزامی است")
    @Size(min = 2, max = 100)
    private String fullName;

    @NotBlank(message = "ایمیل الزامی است")
    @Email(message = "فرمت ایمیل معتبر نیست")
    private String email;

    @NotBlank(message = "رمز عبور الزامی است")
    @Size(min = 6, max = 100, message = "رمز عبور باید حداقل ۶ کاراکتر باشد")
    private String password;

    private String phone;
}

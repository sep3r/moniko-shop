package com.monikoshop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "ایمیل الزامی است")
    @Email
    private String email;

    @NotBlank(message = "رمز عبور الزامی است")
    private String password;
}

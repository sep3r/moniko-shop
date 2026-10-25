package com.monikoshop.dto;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "ایمیل الزامی است")
    @Email
    @Size(max = 100)
    private String email;

    @NotBlank(message = "رمز عبور الزامی است")
    @Size(max = 128)
    private String password;
}

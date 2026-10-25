package com.monikoshop.dto;


import jakarta.validation.constraints.Pattern;
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
    @Size(max = 100, message = "ایمیل بیش از حد طولانی است")
    private String email;

    @NotBlank(message = "رمز عبور الزامی است")
    @Size(min = 8, max = 64, message = "رمز عبور باید ۸ تا ۶۴ کاراکتر باشد")
    @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).+$", message = "رمز عبور باید شامل حداقل یک حرف و یک عدد باشد")
    private String password;

    @Size(max = 20, message = "شماره تماس نامعتبر است")
    @Pattern(regexp = "^[0-9\\u06F0-\\u06F9\\u0660-\\u0669+\\- ]*$", message = "شماره تماس نامعتبر است")
    private String phone;
}

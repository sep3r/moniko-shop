package com.monikoshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendOtpRequest {

    @NotBlank(message = "شماره موبایل الزامی است")
    @Size(max = 25, message = "شماره موبایل نامعتبر است")
    private String phone;
}

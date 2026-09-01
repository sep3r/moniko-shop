package com.monikoshop.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {

    @NotEmpty(message = "سبد خرید نمی‌تواند خالی باشد")
    @Valid
    private List<OrderItemRequest> items;

    @NotBlank(message = "آدرس ارسال الزامی است")
    private String shippingAddress;

    @NotBlank(message = "شماره تماس الزامی است")
    private String phone;
}

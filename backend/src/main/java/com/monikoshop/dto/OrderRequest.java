package com.monikoshop.dto;


import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {

    @Size(max = 50, message = "تعداد اقلام سفارش بیش از حد مجاز است")
    @NotEmpty(message = "سبد خرید نمی‌تواند خالی باشد")
    @Valid
    private List<OrderItemRequest> items;

    @NotBlank(message = "آدرس ارسال الزامی است")
    @Size(max = 255, message = "آدرس بیش از حد طولانی است (حداکثر ۲۵۵ کاراکتر)")
    private String shippingAddress;

    @NotBlank(message = "شماره تماس الزامی است")
    @Size(max = 20, message = "شماره تماس نامعتبر است")
    @Pattern(regexp = "^[0-9\\u06F0-\\u06F9\\u0660-\\u0669+\\- ]+$", message = "شماره تماس نامعتبر است")
    private String phone;
}

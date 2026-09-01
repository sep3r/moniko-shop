package com.monikoshop.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemRequest {

    @NotNull(message = "شناسه محصول الزامی است")
    private Long productId;

    @NotNull(message = "تعداد الزامی است")
    @Min(value = 1, message = "تعداد باید حداقل ۱ باشد")
    private Integer quantity;
}

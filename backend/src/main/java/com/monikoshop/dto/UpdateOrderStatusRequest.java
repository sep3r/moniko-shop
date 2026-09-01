package com.monikoshop.dto;

import com.monikoshop.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "وضعیت الزامی است")
    private OrderStatus status;
}

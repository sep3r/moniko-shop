package com.monikoshop.dto;

import com.monikoshop.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String phone;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
    // Populated only in the admin listing, so the admin panel can show who placed the order.
    private String customerName;
    private String customerEmail;
}

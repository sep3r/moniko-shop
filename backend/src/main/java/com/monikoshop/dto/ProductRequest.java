package com.monikoshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "نام محصول الزامی است")
    private String name;

    private String description;

    @NotNull(message = "قیمت الزامی است")
    @PositiveOrZero
    private BigDecimal price;

    private BigDecimal discountPrice;

    private String imageUrl;

    private String brand;

    @NotNull(message = "دسته‌بندی الزامی است")
    private Long categoryId;

    @NotNull(message = "موجودی الزامی است")
    @PositiveOrZero
    private Integer stock;

    private Boolean active;
}

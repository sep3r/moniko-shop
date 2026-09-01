package com.monikoshop.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "نام دسته‌بندی الزامی است")
    private String name;

    @NotBlank(message = "اسلاگ الزامی است")
    private String slug;

    private String imageUrl;

    private Integer sortOrder;
}

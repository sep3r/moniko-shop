package com.monikoshop.dto;


import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "نام دسته‌بندی الزامی است")
    @Size(max = 100, message = "نام دسته‌بندی بیش از حد طولانی است")
    private String name;

    @NotBlank(message = "اسلاگ الزامی است")
    @Size(max = 100, message = "اسلاگ بیش از حد طولانی است")
    @Pattern(regexp = "^[\\p{L}0-9]+(?:-[\\p{L}0-9]+)*$", message = "اسلاگ فقط می‌تواند شامل حروف، عدد و خط تیره باشد")
    private String slug;

    private String imageUrl;

    private Integer sortOrder;

    /** شناسه دسته والد (null = دسته اصلی سطح اول) */
    private Long parentId;
}

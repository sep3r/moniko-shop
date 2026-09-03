package com.monikoshop.dto;

import com.monikoshop.entity.Category;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO درخت دسته‌بندی برای مگامنو و ادمین.
 * از حلقه بی‌نهایت JSON جلوگیری می‌کند.
 */
@Data
@Builder
public class CategoryTreeResponse {

    private Long id;
    private String name;
    private String slug;
    private String imageUrl;
    private Integer sortOrder;
    private Long parentId;
    private List<CategoryTreeResponse> children;

    public static CategoryTreeResponse from(Category c) {
        List<CategoryTreeResponse> kids = new ArrayList<>();
        if (c.getChildren() != null) {
            kids = c.getChildren().stream()
                    .map(CategoryTreeResponse::from)
                    .collect(Collectors.toList());
        }
        return CategoryTreeResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .imageUrl(c.getImageUrl())
                .sortOrder(c.getSortOrder())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .children(kids)
                .build();
    }

    /** نسخه تخت (بدون children) برای لیست ساده */
    public static CategoryTreeResponse flat(Category c) {
        return CategoryTreeResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .imageUrl(c.getImageUrl())
                .sortOrder(c.getSortOrder())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .children(List.of())
                .build();
    }
}

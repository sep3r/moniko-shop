package com.monikoshop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    private String imageUrl;

    private Integer sortOrder;

    /** والد این دسته (null = دسته اصلی سطح اول) */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    @JsonIgnoreProperties({"parent", "children", "hibernateLazyInitializer", "handler"})
    private Category parent;

    /** زیر‌دسته‌ها */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = false)
    @OrderBy("sortOrder ASC, id ASC")
    @Builder.Default
    @JsonIgnoreProperties({"parent", "hibernateLazyInitializer", "handler"})
    private List<Category> children = new ArrayList<>();

    /** فقط برای جلوگیری از حلقه در JSON وقتی parent لود شده */
    @Transient
    public Long getParentId() {
        return parent != null ? parent.getId() : null;
    }
}

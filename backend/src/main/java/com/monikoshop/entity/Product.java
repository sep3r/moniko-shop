package com.monikoshop.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal price;

    private BigDecimal discountPrice;

    /**
     * Legacy/external image URL. For uploaded product images this contains the
     * public API URL that serves the bytes stored in PostgreSQL.
     */
    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    /** Raw image bytes stored directly in PostgreSQL BYTEA. */
    @JsonIgnore
    @Column(name = "image_data", columnDefinition = "BYTEA")
    private byte[] imageData;

    @JsonIgnore
    @Column(name = "image_content_type", length = 100)
    private String imageContentType;

    private String brand;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

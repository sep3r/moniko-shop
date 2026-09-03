package com.monikoshop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
     * Kept for backwards compatibility with existing products that use an external URL.
     * For newly uploaded images this contains /api/products/{id}/image.
     */
    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    /** Actual uploaded image bytes stored in PostgreSQL BYTEA. */
    @JsonIgnore
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "image_data", columnDefinition = "BYTEA")
    private byte[] imageData;

    /** MIME type needed when the image is served back to the browser. */
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

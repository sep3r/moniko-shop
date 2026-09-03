package com.monikoshop.controller;

import com.monikoshop.entity.Category;
import com.monikoshop.entity.Product;
import com.monikoshop.repository.CategoryRepository;
import com.monikoshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findByActiveTrue());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/products/{id}/image")
    public ResponseEntity<byte[]> getProductImage(@PathVariable Long id) {
        return productRepository.findById(id)
                .filter(product -> product.getImageData() != null && product.getImageContentType() != null)
                .map(product -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(product.getImageContentType()))
                        .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                        .body(product.getImageData()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @GetMapping("/categories/{slug}/products")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String slug) {
        return categoryRepository.findBySlug(slug)
                .map(cat -> ResponseEntity.ok(productRepository.findByCategoryIdAndActiveTrue(cat.getId())))
                .orElse(ResponseEntity.notFound().build());
    }
}

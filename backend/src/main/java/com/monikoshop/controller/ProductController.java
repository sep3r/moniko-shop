package com.monikoshop.controller;

import com.monikoshop.dto.CategoryTreeResponse;
import com.monikoshop.entity.Category;
import com.monikoshop.entity.Product;
import com.monikoshop.repository.CategoryRepository;
import com.monikoshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin
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

    /** لیست تخت همه دسته‌ها (سازگاری با قبل) */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryTreeResponse>> getAllCategories() {
        List<CategoryTreeResponse> list = categoryRepository.findAll().stream()
                .map(CategoryTreeResponse::flat)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /** درخت کامل دسته‌ها برای مگامنو (ریشه‌ها + children) */
    @GetMapping("/categories/tree")
    public ResponseEntity<List<CategoryTreeResponse>> getCategoryTree() {
        List<Category> roots = categoryRepository.findRootCategoriesWithChildren();
        // اگر سطح سوم هم لازم شد می‌توان دوباره fetch کرد؛ فعلاً دو سطح کافی است
        List<CategoryTreeResponse> tree = roots.stream()
                .map(CategoryTreeResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tree);
    }

    @GetMapping("/categories/{slug}/products")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String slug) {
        return categoryRepository.findBySlug(slug)
                .map(cat -> ResponseEntity.ok(productRepository.findByCategoryIdAndActiveTrue(cat.getId())))
                .orElse(ResponseEntity.notFound().build());
    }
}

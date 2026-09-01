package com.monikoshop.controller;

import com.monikoshop.dto.*;
import com.monikoshop.entity.Category;
import com.monikoshop.entity.Product;
import com.monikoshop.entity.User;
import com.monikoshop.repository.CategoryRepository;
import com.monikoshop.repository.ProductRepository;
import com.monikoshop.repository.UserRepository;
import com.monikoshop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// Admin panel: product/category/order/user management.
// Every endpoint here is already locked to ROLE_ADMIN in SecurityConfig
// via the "/api/admin/**" matcher.
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin
public class AdminController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    // ---- Products ----

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductRequest request) {
        Product product = applyToProduct(new Product(), request);
        return ResponseEntity.ok(productRepository.save(product));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("محصول یافت نشد"));
        product = applyToProduct(product, request);
        return ResponseEntity.ok(productRepository.save(product));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<MessageResponse> deleteProduct(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("محصول یافت نشد");
        }
        productRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("محصول حذف شد"));
    }

    private Product applyToProduct(Product product, ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("دسته‌بندی یافت نشد"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setImageUrl(request.getImageUrl());
        product.setBrand(request.getBrand());
        product.setCategory(category);
        product.setStock(request.getStock());
        product.setActive(request.getActive() == null || request.getActive());
        return product;
    }

    // ---- Categories ----

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @PostMapping("/categories")
    public ResponseEntity<Category> createCategory(@Valid @RequestBody CategoryRequest request) {
        Category category = applyToCategory(new Category(), request);
        return ResponseEntity.ok(categoryRepository.save(category));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("دسته‌بندی یافت نشد"));
        category = applyToCategory(category, request);
        return ResponseEntity.ok(categoryRepository.save(category));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<MessageResponse> deleteCategory(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("دسته‌بندی یافت نشد");
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("دسته‌بندی حذف شد"));
    }

    private Category applyToCategory(Category category, CategoryRequest request) {
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setImageUrl(request.getImageUrl());
        category.setSortOrder(request.getSortOrder());
        return category;
    }

    // ---- Orders ----

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrdersForAdmin());
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request.getStatus()));
    }

    // ---- Users ----

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        List<AdminUserResponse> users = userRepository.findAll().stream()
                .map(this::toAdminUserResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<AdminUserResponse> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("کاربر یافت نشد"));
        user.setEnabled(request.getEnabled());
        return ResponseEntity.ok(toAdminUserResponse(userRepository.save(user)));
    }

    @PatchMapping("/users/{id}/roles")
    public ResponseEntity<AdminUserResponse> updateUserRoles(@PathVariable Long id, @Valid @RequestBody UpdateUserRolesRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("کاربر یافت نشد"));
        var roles = request.getRoles().stream()
                .map(r -> com.monikoshop.entity.Role.valueOf(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                .collect(Collectors.toSet());
        user.setRoles(roles);
        return ResponseEntity.ok(toAdminUserResponse(userRepository.save(user)));
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .roles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

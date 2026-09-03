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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin
public class AdminController {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp"
    );
    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024; // 5MB

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    // ---- Products ----

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> createProduct(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) BigDecimal discountPrice,
            @RequestParam(required = false) String brand,
            @RequestParam Long categoryId,
            @RequestParam Integer stock,
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) MultipartFile image
    ) {
        Product product = new Product();
        applyFields(product, name, description, price, discountPrice, brand, categoryId, stock, active);
        applyImage(product, image, imageUrl, null);
        Product saved = productRepository.save(product);
        if (saved.getImageData() != null) {
            saved.setImageUrl("/api/products/" + saved.getId() + "/image");
            saved = productRepository.save(saved);
        }
        return ResponseEntity.ok(saved);
    }

    @PutMapping(value = "/products/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) BigDecimal discountPrice,
            @RequestParam(required = false) String brand,
            @RequestParam Long categoryId,
            @RequestParam Integer stock,
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) MultipartFile image
    ) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("محصول یافت نشد"));
        applyFields(product, name, description, price, discountPrice, brand, categoryId, stock, active);
        applyImage(product, image, imageUrl, product.getImageUrl());
        Product saved = productRepository.save(product);
        if (saved.getImageData() != null) {
            saved.setImageUrl("/api/products/" + saved.getId() + "/image");
            saved = productRepository.save(saved);
        }
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<MessageResponse> deleteProduct(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("محصول یافت نشد");
        }
        productRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("محصول حذف شد"));
    }

    /**
     * Uploaded images are stored as raw bytes in PostgreSQL BYTEA.
     * Existing external image URLs remain supported for backwards compatibility.
     */
    private void applyImage(Product product, MultipartFile image, String imageUrl, String existingImageUrl) {
        if (image != null && !image.isEmpty()) {
            validateImage(image);
            try {
                product.setImageData(image.getBytes());
                product.setImageContentType(image.getContentType());
                product.setImageUrl(null);
            } catch (Exception e) {
                throw new RuntimeException("خطا در خواندن تصویر: " + e.getMessage());
            }
            return;
        }

        // On edit, the frontend sends the current /api/products/{id}/image URL.
        // In that case keep the existing BYTEA instead of clearing it.
        if (imageUrl != null && imageUrl.equals(existingImageUrl)) {
            return;
        }

        if (imageUrl != null) {
            product.setImageData(null);
            product.setImageContentType(null);
            product.setImageUrl(imageUrl.isBlank() ? null : imageUrl);
        } else {
            product.setImageUrl(existingImageUrl);
        }
    }

    private void validateImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("فقط فایل‌های تصویری مجاز هستند (jpg, png, gif, webp, bmp)");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new RuntimeException("حجم تصویر نباید بیشتر از ۵ مگابایت باشد");
        }
    }

    private void applyFields(Product product, String name, String description, BigDecimal price,
                             BigDecimal discountPrice, String brand, Long categoryId,
                             Integer stock, Boolean active) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("دسته‌بندی یافت نشد"));
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setDiscountPrice(discountPrice);
        product.setBrand(brand);
        product.setCategory(category);
        product.setStock(stock);
        product.setActive(active == null || active);
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

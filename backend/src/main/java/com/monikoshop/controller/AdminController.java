package com.monikoshop.controller;

import com.monikoshop.dto.*;
import com.monikoshop.dto.CategoryTreeResponse;
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
import java.util.Base64;
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
            @RequestParam(required = false) MultipartFile image,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        Product product = new Product();
        applyFields(product, name, description, price, discountPrice, brand, categoryId, stock, active);
        applyImage(product, image, imageUrl, httpRequest);
        Product saved = productRepository.save(product);
        if (saved.getImageData() != null) {
            saved.setImageUrl(buildImageUrl(httpRequest, saved.getId()));
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
            @RequestParam(required = false) MultipartFile image,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("محصول یافت نشد"));
        applyFields(product, name, description, price, discountPrice, brand, categoryId, stock, active);
        applyImage(product, image, imageUrl, httpRequest);
        Product saved = productRepository.save(product);
        if (saved.getImageData() != null) {
            saved.setImageUrl(buildImageUrl(httpRequest, saved.getId()));
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

    private void applyFields(
            Product product,
            String name,
            String description,
            BigDecimal price,
            BigDecimal discountPrice,
            String brand,
            Long categoryId,
            Integer stock,
            Boolean active
    ) {
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setDiscountPrice(discountPrice);
        product.setBrand(brand);
        product.setStock(stock);
        product.setActive(active != null ? active : true);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("دسته‌بندی یافت نشد"));
        product.setCategory(category);
    }

    private void applyImage(Product product, MultipartFile image, String imageUrl,
                            jakarta.servlet.http.HttpServletRequest request) {
        if (image != null && !image.isEmpty()) {
            validateImage(image);
            try {
                product.setImageData(image.getBytes());
                product.setImageContentType(image.getContentType().toLowerCase());
                product.setImageUrl(buildImageUrl(request, product.getId()));
            } catch (Exception e) {
                throw new RuntimeException("خطا در ذخیره تصویر: " + e.getMessage());
            }
            return;
        }

        if (imageUrl != null) {
            product.setImageUrl(imageUrl.isBlank() ? null : imageUrl);
            product.setImageData(null);
            product.setImageContentType(null);
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

    private String buildImageUrl(jakarta.servlet.http.HttpServletRequest request, Long productId) {
        String base = request.getRequestURL().toString();
        int adminIndex = base.indexOf("/api/admin/products");
        if (adminIndex >= 0) {
            base = base.substring(0, adminIndex);
        }
        // New product has no id until flush; save will assign it. The response
        // is normalized by ProductImageController/client after persistence.
        return base + "/api/products/" + (productId == null ? "" : productId) + "/image";
    }

    // ---- Categories (hierarchical) ----

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryTreeResponse>> getAllCategories() {
        List<CategoryTreeResponse> list = categoryRepository.findAll().stream()
                .map(CategoryTreeResponse::flat)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /** درخت کامل برای ادمین */
    @GetMapping("/categories/tree")
    public ResponseEntity<List<CategoryTreeResponse>> getCategoryTree() {
        List<Category> roots = categoryRepository.findRootCategoriesWithChildren();
        return ResponseEntity.ok(roots.stream()
                .map(CategoryTreeResponse::from)
                .collect(Collectors.toList()));
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryTreeResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new RuntimeException("اسلاگ تکراری است");
        }
        Category category = applyToCategory(new Category(), request, null);
        Category saved = categoryRepository.save(category);
        return ResponseEntity.ok(CategoryTreeResponse.flat(saved));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryTreeResponse> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("دسته‌بندی یافت نشد"));
        if (categoryRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
            throw new RuntimeException("اسلاگ تکراری است");
        }
        // جلوگیری از حلقه: نمی‌تواند والد خودش یا فرزندش باشد
        if (request.getParentId() != null && request.getParentId().equals(id)) {
            throw new RuntimeException("دسته نمی‌تواند والد خودش باشد");
        }
        category = applyToCategory(category, request, id);
        Category saved = categoryRepository.save(category);
        return ResponseEntity.ok(CategoryTreeResponse.flat(saved));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<MessageResponse> deleteCategory(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("دسته‌بندی یافت نشد"));
        // اگر زیر‌دسته دارد، حذف نشود
        List<Category> children = categoryRepository.findByParent_IdOrderBySortOrderAscIdAsc(id);
        if (!children.isEmpty()) {
            throw new RuntimeException("ابتدا زیر‌دسته‌ها را حذف کنید (" + children.size() + " مورد)");
        }
        categoryRepository.delete(category);
        return ResponseEntity.ok(new MessageResponse("دسته‌بندی حذف شد"));
    }

    private Category applyToCategory(Category category, CategoryRequest request, Long currentId) {
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setImageUrl(request.getImageUrl());
        category.setSortOrder(request.getSortOrder());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("دسته والد یافت نشد"));
            // فقط یک سطح زیر‌دسته مجاز است (والد خودش نباید والد داشته باشد) — اختیاری
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
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

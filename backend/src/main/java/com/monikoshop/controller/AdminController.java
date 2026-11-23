package com.monikoshop.controller;


import com.monikoshop.audit.AuditEvent;
import com.monikoshop.audit.AuditService;
import com.monikoshop.exception.ApiException;
import com.monikoshop.dto.*;
import com.monikoshop.dto.CategoryTreeResponse;
import com.monikoshop.entity.Category;
import com.monikoshop.entity.User;
import com.monikoshop.repository.CategoryRepository;
import com.monikoshop.repository.UserRepository;
import com.monikoshop.service.OrderService;
import com.monikoshop.service.ProductService;
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
public class AdminController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final AuditService auditService;

    // ---- Products ----

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.listAll());
    }

    /**
     * Create a product. Rules: 1..5 images are required (multipart field "images", repeated),
     * "imageOrder" (optional) is the final order, e.g. "n1,n0".
     * "specs" is a JSON array of {key, value} rows for the "مشخصات" table.
     */
    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> createProduct(
            @RequestParam String name,
            @RequestParam(required = false) String subtitle,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) BigDecimal discountPrice,
            @RequestParam(required = false) String brand,
            @RequestParam Long categoryId,
            @RequestParam Integer stock,
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            @RequestParam(required = false) String specs,
            @RequestParam(required = false) String imageOrder,
            @RequestParam(required = false) List<MultipartFile> images
    ) {
        ProductForm form = new ProductForm(name, subtitle, description, price, discountPrice, brand,
                categoryId, stock, active, specs);
        ProductResponse created = productService.create(form, images, imageOrder);
        auditService.log(AuditEvent.ADMIN_PRODUCT_CREATE, "productId=" + created.id());
        return ResponseEntity.ok(created);
    }

    /**
     * Update a product. "imageOrder" lists the final images: "e<id>" keeps an existing image,
     * "n<i>" is the i-th newly uploaded file; existing images that are not listed are deleted.
     */
    @PutMapping(value = "/products/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String subtitle,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) BigDecimal discountPrice,
            @RequestParam(required = false) String brand,
            @RequestParam Long categoryId,
            @RequestParam Integer stock,
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            @RequestParam(required = false) String specs,
            @RequestParam(required = false) String imageOrder,
            @RequestParam(required = false) List<MultipartFile> images
    ) {
        ProductForm form = new ProductForm(name, subtitle, description, price, discountPrice, brand,
                categoryId, stock, active, specs);
        ProductResponse updated = productService.update(id, form, images, imageOrder);
        auditService.log(AuditEvent.ADMIN_PRODUCT_UPDATE, "productId=" + id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<MessageResponse> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        auditService.log(AuditEvent.ADMIN_PRODUCT_DELETE, "productId=" + id);
        return ResponseEntity.ok(new MessageResponse("محصول حذف شد"));
    }

    /** Only http(s) URLs or site-relative paths are accepted (blocks javascript:, data:, file: ...). */
    private static String sanitizeImageUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String u = url.trim();
        if (u.length() > 1000) {
            throw new ApiException("آدرس تصویر بیش از حد طولانی است");
        }
        String lower = u.toLowerCase();
        if (lower.startsWith("https://") || lower.startsWith("http://") || (u.startsWith("/") && !u.startsWith("//"))) {
            return u;
        }
        throw new ApiException("آدرس تصویر باید با http:// یا https:// شروع شود");
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
            throw new ApiException("اسلاگ تکراری است");
        }
        Category category = applyToCategory(new Category(), request, null);
        Category saved = categoryRepository.save(category);
        auditService.log(AuditEvent.ADMIN_CATEGORY_CREATE, "categoryId=" + saved.getId());
        return ResponseEntity.ok(CategoryTreeResponse.flat(saved));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryTreeResponse> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException("دسته‌بندی یافت نشد"));
        if (categoryRepository.existsBySlugAndIdNot(request.getSlug(), id)) {
            throw new ApiException("اسلاگ تکراری است");
        }
        // جلوگیری از حلقه: نمی‌تواند والد خودش یا فرزندش باشد
        if (request.getParentId() != null && request.getParentId().equals(id)) {
            throw new ApiException("دسته نمی‌تواند والد خودش باشد");
        }
        category = applyToCategory(category, request, id);
        Category saved = categoryRepository.save(category);
        // نام/والد دسته در متن جستجوی محصولات هم هست؛ ایندکس دوباره ساخته شود
        productService.reindexAllSearchText();
        auditService.log(AuditEvent.ADMIN_CATEGORY_UPDATE, "categoryId=" + saved.getId());
        return ResponseEntity.ok(CategoryTreeResponse.flat(saved));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<MessageResponse> deleteCategory(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException("دسته‌بندی یافت نشد"));
        // اگر زیر‌دسته دارد، حذف نشود
        List<Category> children = categoryRepository.findByParent_IdOrderBySortOrderAscIdAsc(id);
        if (!children.isEmpty()) {
            throw new ApiException("ابتدا زیر‌دسته‌ها را حذف کنید (" + children.size() + " مورد)");
        }
        categoryRepository.delete(category);
        auditService.log(AuditEvent.ADMIN_CATEGORY_DELETE, "categoryId=" + id);
        return ResponseEntity.ok(new MessageResponse("دسته‌بندی حذف شد"));
    }

    private Category applyToCategory(Category category, CategoryRequest request, Long currentId) {
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setImageUrl(sanitizeImageUrl(request.getImageUrl()));
        category.setSortOrder(request.getSortOrder());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ApiException("دسته والد یافت نشد"));
            // جلوگیری از حلقه: والد جدید نباید خود دسته یا یکی از فرزندان آن باشد
            if (currentId != null) {
                Category ancestor = parent;
                int guard = 0;
                while (ancestor != null && guard++ < 100) {
                    if (currentId.equals(ancestor.getId())) {
                        throw new ApiException("دسته نمی‌تواند زیرمجموعه‌ی خودش باشد");
                    }
                    ancestor = ancestor.getParent();
                }
            }
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
                .orElseThrow(() -> new ApiException("کاربر یافت نشد"));
        if (Boolean.FALSE.equals(request.getEnabled()) && user.getId().equals(currentUserId())) {
            throw new ApiException("نمی‌توانید حساب کاربری خودتان را غیرفعال کنید");
        }
        user.setEnabled(request.getEnabled());
        User savedUser = userRepository.save(user);
        auditService.log(AuditEvent.ADMIN_USER_STATUS, "userId=" + id + ", enabled=" + request.getEnabled());
        return ResponseEntity.ok(toAdminUserResponse(savedUser));
    }

    @PatchMapping("/users/{id}/roles")
    public ResponseEntity<AdminUserResponse> updateUserRoles(@PathVariable Long id, @Valid @RequestBody UpdateUserRolesRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException("کاربر یافت نشد"));
        java.util.Set<com.monikoshop.entity.Role> roles = new java.util.HashSet<>();
        for (String r : request.getRoles()) {
            String roleName = r.startsWith("ROLE_") ? r : "ROLE_" + r;
            try {
                roles.add(com.monikoshop.entity.Role.valueOf(roleName));
            } catch (IllegalArgumentException e) {
                throw new ApiException("نقش نامعتبر است");
            }
        }
        if (!roles.contains(com.monikoshop.entity.Role.ROLE_ADMIN) && user.getId().equals(currentUserId())) {
            throw new ApiException("نمی‌توانید دسترسی ادمین خودتان را حذف کنید");
        }
        user.setRoles(roles);
        User savedUser = userRepository.save(user);
        auditService.log(AuditEvent.ADMIN_USER_ROLES, "userId=" + id + ", roles=" + roles);
        return ResponseEntity.ok(toAdminUserResponse(savedUser));
    }

    private static Long currentUserId() {
        return com.monikoshop.security.CurrentUser.id();
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
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}

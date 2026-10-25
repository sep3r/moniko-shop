package com.monikoshop.controller;


import com.monikoshop.exception.ApiException;
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
public class AdminController {

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
                .orElseThrow(() -> new ApiException("محصول یافت نشد"));
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
            throw new ApiException("محصول یافت نشد");
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
        if (name == null || name.isBlank() || name.length() > 255) {
            throw new ApiException("نام محصول الزامی است و نباید بیش از ۲۵۵ کاراکتر باشد");
        }
        if (description != null && description.length() > 10000) {
            throw new ApiException("توضیحات بیش از حد طولانی است");
        }
        if (brand != null && brand.length() > 255) {
            throw new ApiException("نام برند بیش از حد طولانی است");
        }
        if (price == null || price.signum() <= 0 || price.compareTo(new BigDecimal("999999999999")) > 0) {
            throw new ApiException("قیمت نامعتبر است");
        }
        if (discountPrice != null && (discountPrice.signum() < 0 || discountPrice.compareTo(price) > 0)) {
            throw new ApiException("قیمت با تخفیف نامعتبر است");
        }
        if (stock == null || stock < 0 || stock > 1_000_000) {
            throw new ApiException("موجودی نامعتبر است");
        }
        product.setName(name.trim());
        product.setDescription(description);
        product.setPrice(price);
        product.setDiscountPrice(discountPrice);
        product.setBrand(brand);
        product.setStock(stock);
        product.setActive(active != null ? active : true);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException("دسته‌بندی یافت نشد"));
        product.setCategory(category);
    }

    private void applyImage(Product product, MultipartFile image, String imageUrl,
                            jakarta.servlet.http.HttpServletRequest request) {
        if (image != null && !image.isEmpty()) {
            if (image.getSize() > MAX_IMAGE_BYTES) {
                throw new ApiException("حجم تصویر نباید بیشتر از ۵ مگابایت باشد");
            }
            byte[] bytes;
            try {
                bytes = image.getBytes();
            } catch (java.io.IOException e) {
                throw new ApiException("خطا در خواندن تصویر");
            }
            // The real file content decides the type; the client-supplied Content-Type is never trusted.
            String detectedType = detectImageType(bytes);
            if (detectedType == null) {
                throw new ApiException("فقط فایل‌های تصویری معتبر مجاز هستند (jpg, png, gif, webp, bmp)");
            }
            product.setImageData(bytes);
            product.setImageContentType(detectedType);
            product.setImageUrl(buildImageUrl(request, product.getId()));
            return;
        }

        if (imageUrl != null) {
            product.setImageUrl(sanitizeImageUrl(imageUrl));
            product.setImageData(null);
            product.setImageContentType(null);
        }
    }

    private static String detectImageType(byte[] b) {
        if (b == null || b.length < 12) {
            return null;
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return "image/gif";
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        if (b[0] == 'B' && b[1] == 'M') {
            return "image/bmp";
        }
        return null;
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

    private String buildImageUrl(jakarta.servlet.http.HttpServletRequest request, Long productId) {
        // مسیر نسبی ذخیره می‌شود؛ فرانت آدرس بک‌اند را (از VITE_API_URL) به آن اضافه می‌کند
        return "/api/products/" + (productId == null ? "" : productId) + "/image";
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
        if (Boolean.FALSE.equals(request.getEnabled()) && user.getEmail().equalsIgnoreCase(currentEmail())) {
            throw new ApiException("نمی‌توانید حساب کاربری خودتان را غیرفعال کنید");
        }
        user.setEnabled(request.getEnabled());
        return ResponseEntity.ok(toAdminUserResponse(userRepository.save(user)));
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
        if (!roles.contains(com.monikoshop.entity.Role.ROLE_ADMIN) && user.getEmail().equalsIgnoreCase(currentEmail())) {
            throw new ApiException("نمی‌توانید دسترسی ادمین خودتان را حذف کنید");
        }
        user.setRoles(roles);
        return ResponseEntity.ok(toAdminUserResponse(userRepository.save(user)));
    }

    private static String currentEmail() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "" : auth.getName();
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

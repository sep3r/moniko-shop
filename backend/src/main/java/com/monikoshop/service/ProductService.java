package com.monikoshop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monikoshop.dto.PagedProducts;
import com.monikoshop.dto.AdminPage;
import com.monikoshop.dto.ProductForm;
import com.monikoshop.dto.ProductResponse;
import com.monikoshop.dto.ProductSpecDto;
import com.monikoshop.entity.Category;
import com.monikoshop.entity.Product;
import com.monikoshop.entity.ProductImage;
import com.monikoshop.entity.ProductSpec;
import com.monikoshop.exception.ApiException;
import com.monikoshop.repository.CategoryRepository;
import com.monikoshop.repository.ProductImageRepository;
import com.monikoshop.repository.ProductRepository;
import com.monikoshop.repository.ProductSearchRepository;
import com.monikoshop.search.SearchTextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductService {

    public static final int MIN_IMAGES = 1;
    public static final int MAX_IMAGES = 5;
    public static final int MAX_SPECS = 30;
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024; // 5MB per image
    private static final Pattern ORDER_TOKEN = Pattern.compile("^([en])(\\d{1,18})$");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository imageRepository;
    private final ProductSearchRepository searchRepository;
    private final ObjectMapper objectMapper;

    // ------------------------------------------------------------------ reads

    @Transactional(readOnly = true)
    public List<ProductResponse> listActive() {
        return toResponses(productRepository.findByActiveTrue());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listAll() {
        return toResponses(productRepository.findAll(Sort.by(Sort.Direction.DESC, "id")));
    }

    @Transactional(readOnly = true)
    public AdminPage<ProductResponse> listAll(Pageable pageable) {
        Pageable sorted = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        Page<Product> result = productRepository.findAll(sorted);
        return new AdminPage<>(toResponses(result.getContent()), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listActiveByCategory(Long categoryId) {
        return toResponses(productRepository.findByCategoryIdAndActiveTrue(categoryId));
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> findActive(Long id) {
        return productRepository.findById(id)
                .filter(Product::isActive)
                .map(p -> toResponses(List.of(p)).get(0));
    }

    private List<ProductResponse> toResponses(List<Product> products) {
        Map<Long, List<Long>> imageIds = new HashMap<>();
        if (!products.isEmpty()) {
            List<Long> ids = products.stream().map(Product::getId).toList();
            for (Object[] row : imageRepository.findIdsByProductIds(ids)) {
                imageIds.computeIfAbsent(((Number) row[0]).longValue(), k -> new ArrayList<>())
                        .add(((Number) row[1]).longValue());
            }
        }
        List<ProductResponse> result = new ArrayList<>(products.size());
        for (Product p : products) {
            result.add(toResponse(p, imageIds.getOrDefault(p.getId(), List.of())));
        }
        return result;
    }

    private ProductResponse toResponse(Product p, List<Long> imageIds) {
        List<ProductResponse.Image> images = imageIds.stream()
                .map(id -> new ProductResponse.Image(id, imageUrl(p.getId(), id)))
                .toList();
        String mainUrl = images.isEmpty() ? p.getImageUrl() : images.get(0).url();
        Category c = p.getCategory();
        ProductResponse.CategoryRef category = c == null ? null
                : new ProductResponse.CategoryRef(c.getId(), c.getName(), c.getSlug(), c.getParentId());
        List<ProductSpecDto> specs = p.getSpecs().stream()
                .map(s -> new ProductSpecDto(s.getKey(), s.getValue()))
                .toList();
        return new ProductResponse(p.getId(), p.getName(), p.getSubtitle(), p.getDescription(), p.getPrice(),
                p.getDiscountPrice(), p.getBrand(), category, p.getStock(), p.isActive(), p.getCreatedAt(),
                mainUrl, images, specs);
    }

    private static String imageUrl(Long productId, Long imageId) {
        // relative path; the frontend prepends the backend address (VITE_API_URL)
        return "/api/products/" + productId + "/images/" + imageId;
    }

    // ----------------------------------------------------------------- search

    public static final int DEFAULT_PAGE_SIZE = 24;
    public static final int MAX_PAGE_SIZE = 60;
    private static final int MAX_QUERY_CHARS = 100;
    private static final int MAX_QUERY_TERMS = 8;
    private static final Set<String> SORTS = Set.of("relevance", "new", "cheap", "expensive", "discount");

    /**
     * جستجو و صفحه‌بندی سمت سرور.
     *
     * @param q            عبارت جستجو (اختیاری)؛ همه‌ی کلمات باید در نام/برند/دسته/توضیحات/مشخصات باشند
     * @param categorySlug فقط محصولات این دسته و زیردسته‌هایش (اختیاری)
     * @param sort         relevance | new | cheap | expensive | discount
     * @param page         شماره‌ی صفحه از ۰
     */
    @Transactional(readOnly = true)
    public PagedProducts search(String q, String categorySlug, boolean inStockOnly, String sort, int page, int size) {
        String query = q == null ? "" : (q.length() > MAX_QUERY_CHARS ? q.substring(0, MAX_QUERY_CHARS) : q);
        List<String> terms = SearchTextNormalizer.terms(query, MAX_QUERY_TERMS);
        int safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, 100_000));
        String slug = (categorySlug == null || categorySlug.isBlank()) ? null : categorySlug.trim();
        String effectiveSort = (sort != null && SORTS.contains(sort)) ? sort : (terms.isEmpty() ? "new" : "relevance");

        ProductSearchRepository.Result r = searchRepository.search(terms, slug, inStockOnly, effectiveSort, safePage, safeSize);

        List<ProductResponse> items = List.of();
        if (!r.ids().isEmpty()) {
            Map<Long, Product> byId = new HashMap<>();
            for (Product p : productRepository.findAllById(r.ids())) {
                byId.put(p.getId(), p);
            }
            List<Product> ordered = new ArrayList<>(r.ids().size());
            for (Long id : r.ids()) {
                Product p = byId.get(id);
                if (p != null) {
                    ordered.add(p);
                }
            }
            items = toResponses(ordered);
        }
        int totalPages = (int) ((r.total() + safeSize - 1) / safeSize);
        return new PagedProducts(items, safePage, safeSize, r.total(), totalPages, null);
    }

    /** متن ایندکس جستجوی یک محصول (نام، برند، دسته و دسته‌ی والد، توضیحات، مشخصات). */
    private String buildSearchText(Product p) {
        Category c = p.getCategory();
        String parentName = (c != null && c.getParent() != null) ? c.getParent().getName() : null;
        List<String> specTexts = new ArrayList<>();
        for (ProductSpec s : p.getSpecs()) {
            specTexts.add(s.getKey() + " " + s.getValue());
        }
        return SearchTextNormalizer.buildIndexText(p.getName(), p.getSubtitle(), p.getBrand(),
                c == null ? null : c.getName(), parentName, p.getDescription(), specTexts);
    }

    /** ایندکس همه‌ی محصولات را بازسازی می‌کند (بعد از تغییر نام/والد دسته‌ها و در شروع برای محصولات قدیمی). */
    @Transactional
    public int reindexAllSearchText() {
        List<Product> all = productRepository.findAll();
        for (Product p : all) {
            p.setSearchText(buildSearchText(p));
        }
        productRepository.saveAll(all);
        return all.size();
    }

    // ----------------------------------------------------------------- writes

    /**
     * @param files      newly uploaded images
     * @param imageOrder final order of the images, comma separated: "e12" = existing image id 12,
     *                   "n0" = first uploaded file. Existing images that are not listed are deleted.
     */
    @Transactional
    public ProductResponse create(ProductForm form, List<MultipartFile> files, String imageOrder) {
        Product product = new Product();
        applyFields(product, form);
        List<NewImage> uploads = validateUploads(files);
        List<Slot> slots = resolveSlots(imageOrder, uploads.size(), List.of());
        Product saved = productRepository.save(product);
        applyImages(saved, uploads, slots);
        return toResponses(List.of(saved)).get(0);
    }

    @Transactional
    public ProductResponse update(Long id, ProductForm form, List<MultipartFile> files, String imageOrder) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("محصول یافت نشد"));
        applyFields(product, form);
        List<NewImage> uploads = validateUploads(files);
        List<Long> existing = imageRepository.findIdsByProductId(id);
        List<Slot> slots = resolveSlots(imageOrder, uploads.size(), existing);
        Product saved = productRepository.save(product);
        applyImages(saved, uploads, slots);
        return toResponses(List.of(saved)).get(0);
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("محصول یافت نشد"));
        imageRepository.deleteAllByProductId(id);
        productRepository.delete(product);
        // flush now so a foreign-key violation (product used in orders) is reported and rolls everything back
        productRepository.flush();
    }

    // ---------------------------------------------------------------- helpers

    private record NewImage(byte[] bytes, String contentType) { }

    /** existingId != null -> keep that image; otherwise newIndex points into the uploads list. */
    private record Slot(Long existingId, Integer newIndex) { }

    private List<NewImage> validateUploads(List<MultipartFile> files) {
        List<NewImage> result = new ArrayList<>();
        if (files == null) {
            return result;
        }
        if (files.size() > MAX_IMAGES) {
            throw new ApiException("حداکثر " + MAX_IMAGES + " تصویر برای هر محصول مجاز است");
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new ApiException("فایل تصویر خالی است");
            }
            if (file.getSize() > MAX_IMAGE_BYTES) {
                throw new ApiException("حجم هر تصویر نباید بیشتر از ۵ مگابایت باشد");
            }
            byte[] bytes;
            try {
                bytes = file.getBytes();
            } catch (IOException e) {
                throw new ApiException("خطا در خواندن تصویر");
            }
            // The real file content decides the type; the client-supplied Content-Type is never trusted.
            String type = detectImageType(bytes);
            if (type == null) {
                throw new ApiException("فقط فایل‌های تصویری معتبر مجاز هستند (jpg, png, webp)");
            }
            result.add(new NewImage(bytes, type));
        }
        return result;
    }

    /** Builds the final ordered list of images and enforces 1..5 images. */
    private List<Slot> resolveSlots(String imageOrder, int newCount, List<Long> existingIds) {
        List<Slot> slots = new ArrayList<>();
        Set<Long> existingSet = new HashSet<>(existingIds);

        if (imageOrder == null || imageOrder.isBlank()) {
            // default: keep all current images, then append the new uploads
            for (Long id : existingIds) {
                slots.add(new Slot(id, null));
            }
            for (int i = 0; i < newCount; i++) {
                slots.add(new Slot(null, i));
            }
        } else {
            Set<Long> usedExisting = new HashSet<>();
            Set<Integer> usedNew = new HashSet<>();
            for (String raw : imageOrder.split(",")) {
                Matcher m = ORDER_TOKEN.matcher(raw.trim());
                if (!m.matches()) {
                    throw new ApiException("ترتیب تصاویر نامعتبر است");
                }
                long number = Long.parseLong(m.group(2));
                if ("e".equals(m.group(1))) {
                    if (!existingSet.contains(number) || !usedExisting.add(number)) {
                        throw new ApiException("ترتیب تصاویر نامعتبر است");
                    }
                    slots.add(new Slot(number, null));
                } else {
                    if (number >= newCount || !usedNew.add((int) number)) {
                        throw new ApiException("ترتیب تصاویر نامعتبر است");
                    }
                    slots.add(new Slot(null, (int) number));
                }
            }
            if (usedNew.size() != newCount) {
                throw new ApiException("ترتیب تصاویر نامعتبر است");
            }
        }

        if (slots.size() < MIN_IMAGES) {
            throw new ApiException("حداقل یک تصویر برای ثبت محصول الزامی است");
        }
        if (slots.size() > MAX_IMAGES) {
            throw new ApiException("حداکثر " + MAX_IMAGES + " تصویر برای هر محصول مجاز است");
        }
        return slots;
    }

    private void applyImages(Product product, List<NewImage> uploads, List<Slot> slots) {
        List<Long> keep = slots.stream().map(Slot::existingId).filter(Objects::nonNull).toList();
        if (keep.isEmpty()) {
            imageRepository.deleteAllByProductId(product.getId());
        } else {
            imageRepository.deleteByProductIdExcept(product.getId(), keep);
        }
        for (int position = 0; position < slots.size(); position++) {
            Slot slot = slots.get(position);
            if (slot.existingId() != null) {
                imageRepository.updateSortOrder(slot.existingId(), position);
            } else {
                NewImage upload = uploads.get(slot.newIndex());
                imageRepository.save(ProductImage.builder()
                        .product(product)
                        .data(upload.bytes())
                        .contentType(upload.contentType())
                        .sortOrder(position)
                        .build());
            }
        }
        // the old external URL is no longer needed once real images exist
        if (product.getImageUrl() != null) {
            product.setImageUrl(null);
            productRepository.save(product);
        }
    }

    private void applyFields(Product product, ProductForm f) {
        if (f.name() == null || f.name().isBlank() || f.name().length() > 255) {
            throw new ApiException("نام محصول الزامی است و نباید بیش از ۲۵۵ کاراکتر باشد");
        }
        if (f.subtitle() != null && f.subtitle().length() > 255) {
            throw new ApiException("عنوان دوم محصول بیش از حد طولانی است");
        }
        if (f.description() != null && f.description().length() > 10000) {
            throw new ApiException("توضیحات بیش از حد طولانی است");
        }
        if (f.brand() != null && f.brand().length() > 255) {
            throw new ApiException("نام برند بیش از حد طولانی است");
        }
        BigDecimal price = f.price();
        if (price == null || price.signum() <= 0 || price.compareTo(new BigDecimal("999999999999")) > 0) {
            throw new ApiException("قیمت نامعتبر است");
        }
        BigDecimal discount = f.discountPrice();
        if (discount != null && (discount.signum() < 0 || discount.compareTo(price) > 0)) {
            throw new ApiException("قیمت با تخفیف نامعتبر است");
        }
        Integer stock = f.stock();
        if (stock == null || stock < 0 || stock > 1_000_000) {
            throw new ApiException("موجودی نامعتبر است");
        }
        Category category = categoryRepository.findById(f.categoryId() == null ? -1L : f.categoryId())
                .orElseThrow(() -> new ApiException("دسته‌بندی یافت نشد"));

        product.setName(f.name().trim());
        product.setSubtitle(blankToNull(f.subtitle()));
        product.setDescription(blankToNull(f.description()));
        product.setPrice(price);
        product.setDiscountPrice(discount);
        product.setBrand(blankToNull(f.brand()));
        product.setStock(stock);
        product.setActive(f.active() != null ? f.active() : true);
        product.setCategory(category);

        // replace the contents of the managed collection (do not swap the list instance)
        List<ProductSpec> specs = parseSpecs(f.specsJson());
        product.getSpecs().clear();
        product.getSpecs().addAll(specs);

        product.setSearchText(buildSearchText(product));
    }

    private List<ProductSpec> parseSpecs(String json) {
        List<ProductSpec> result = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return result;
        }
        if (json.length() > 60_000) {
            throw new ApiException("مشخصات محصول بیش از حد طولانی است");
        }
        List<ProductSpecDto> rows;
        try {
            rows = objectMapper.readValue(json, new TypeReference<List<ProductSpecDto>>() { });
        } catch (Exception e) {
            throw new ApiException("فرمت مشخصات محصول نامعتبر است");
        }
        for (ProductSpecDto row : rows) {
            if (row == null) {
                continue;
            }
            String key = row.key() == null ? "" : row.key().trim();
            String value = row.value() == null ? "" : row.value().trim();
            if (key.isEmpty() && value.isEmpty()) {
                continue; // empty row
            }
            if (key.isEmpty() || value.isEmpty()) {
                throw new ApiException("در هر ردیف مشخصات، هم عنوان و هم مقدار باید پر شود");
            }
            if (key.length() > 100 || value.length() > 500) {
                throw new ApiException("عنوان مشخصات حداکثر ۱۰۰ و مقدار آن حداکثر ۵۰۰ کاراکتر است");
            }
            result.add(new ProductSpec(key, value));
        }
        if (result.size() > MAX_SPECS) {
            throw new ApiException("حداکثر " + MAX_SPECS + " ردیف مشخصات مجاز است");
        }
        return result;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    static String detectImageType(byte[] b) {
        if (b == null || b.length < 12) {
            return null;
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}

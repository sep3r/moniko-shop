package com.monikoshop.service;

import com.monikoshop.dto.ProductResponse;
import com.monikoshop.dto.StorefrontDtos.*;
import com.monikoshop.entity.*;
import com.monikoshop.exception.ApiException;
import com.monikoshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** منطق مدیریت و نمایش محتوای صفحه اصلی: بنرها، برندها، تنظیمات و محصولات منتخب/حراج. */
@Service
@RequiredArgsConstructor
public class StorefrontService {

    /** کلیدهای مجاز تنظیمات؛ کلیدهایی که با link_ شروع می‌شوند باید آدرس https باشند. */
    public static final List<String> SETTING_KEYS = List.of(
            "topbar_text", "phone1", "phone2", "support_hours", "email", "address",
            "instagram", "telegram", "whatsapp", "linkedin",
            "about_title", "about_text", "copyright");
    private static final Set<String> LINK_KEYS = Set.of("instagram", "telegram", "whatsapp", "linkedin");

    private final BannerRepository bannerRepository;
    private final BrandRepository brandRepository;
    private final SiteSettingRepository settingRepository;
    private final HomeProductRepository homeProductRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    // ---------- public ----------

    @Transactional(readOnly = true)
    public HomeResponse home() {
        List<BannerDto> banners = bannerRepository.findByActiveTrueOrderByPositionAscSortOrderAscIdAsc()
                .stream().map(this::toDto).toList();
        List<BrandDto> brands = brandRepository.findByActiveTrueOrderBySortOrderAscIdAsc()
                .stream().map(this::toDto).toList();

        Map<Long, ProductResponse> active = productService.listActive().stream()
                .collect(Collectors.toMap(ProductResponse::id, p -> p, (a, b) -> a));
        Instant now = Instant.now();

        List<HomeProduct> deals = homeProductRepository.findByTypeWithProduct(HomeProduct.Type.DAILY_DEAL).stream()
                .filter(h -> h.getEndsAt() == null || h.getEndsAt().isAfter(now))
                .toList();
        Instant endsAt = deals.stream().map(HomeProduct::getEndsAt).filter(Objects::nonNull)
                .min(Comparator.naturalOrder()).orElse(null);
        List<ProductResponse> dealProducts = deals.stream()
                .map(h -> active.get(h.getProduct().getId())).filter(Objects::nonNull).toList();

        List<ProductResponse> featured = homeProductRepository.findByTypeWithProduct(HomeProduct.Type.FEATURED).stream()
                .map(h -> active.get(h.getProduct().getId())).filter(Objects::nonNull).toList();

        return new HomeResponse(banners, brands, settings(), new Deal(endsAt, dealProducts), featured);
    }

    public Map<String, String> settings() {
        Map<String, String> map = new LinkedHashMap<>();
        settingRepository.findAll().forEach(s -> map.put(s.getKey(), s.getValue() == null ? "" : s.getValue()));
        return map;
    }

    public Banner getBanner(Long id) {
        return bannerRepository.findById(id).filter(Banner::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "یافت نشد"));
    }

    public Brand getBrand(Long id) {
        return brandRepository.findById(id).filter(Brand::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "یافت نشد"));
    }

    // ---------- admin: banners ----------

    public List<BannerDto> listBanners() {
        return bannerRepository.findAllByOrderByPositionAscSortOrderAscIdAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public BannerDto saveBanner(Long id, String position, String title, String linkUrl, Integer sortOrder,
                                Boolean active, MultipartFile image) {
        Banner b;
        if (id == null) {
            if (image == null || image.isEmpty()) throw bad("تصویر بنر الزامی است");
            b = new Banner();
        } else {
            b = bannerRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "بنر یافت نشد"));
        }
        try {
            b.setPosition(Banner.Position.valueOf(position));
        } catch (Exception e) {
            throw bad("جایگاه بنر نامعتبر است");
        }
        b.setTitle(clip(title, 200));
        b.setLinkUrl(cleanLink(linkUrl));
        b.setSortOrder(sortOrder == null ? 0 : sortOrder);
        b.setActive(active == null || active);
        if (image != null && !image.isEmpty()) {
            b.setData(readImage(image));
            b.setContentType(sniffType(b.getData()));
        }
        b.setUpdatedAt(Instant.now());
        return toDto(bannerRepository.save(b));
    }

    public void deleteBanner(Long id) {
        bannerRepository.deleteById(id);
    }

    // ---------- admin: brands ----------

    public List<BrandDto> listBrands() {
        return brandRepository.findAllByOrderBySortOrderAscIdAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public BrandDto saveBrand(Long id, String name, String linkUrl, Integer sortOrder, Boolean active,
                              MultipartFile image) {
        if (name == null || name.isBlank()) throw bad("نام برند الزامی است");
        Brand b;
        if (id == null) {
            if (image == null || image.isEmpty()) throw bad("لوگوی برند الزامی است");
            b = new Brand();
        } else {
            b = brandRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "برند یافت نشد"));
        }
        b.setName(clip(name.trim(), 100));
        b.setLinkUrl(cleanLink(linkUrl));
        b.setSortOrder(sortOrder == null ? 0 : sortOrder);
        b.setActive(active == null || active);
        if (image != null && !image.isEmpty()) {
            b.setData(readImage(image));
            b.setContentType(sniffType(b.getData()));
        }
        b.setUpdatedAt(Instant.now());
        return toDto(brandRepository.save(b));
    }

    public void deleteBrand(Long id) {
        brandRepository.deleteById(id);
    }

    // ---------- admin: settings ----------

    @Transactional
    public Map<String, String> saveSettings(Map<String, String> input) {
        for (String key : SETTING_KEYS) {
            if (!input.containsKey(key)) continue;
            String v = input.get(key) == null ? "" : input.get(key).trim();
            if (v.length() > 2000) throw bad("متن «" + key + "» خیلی طولانی است");
            if (LINK_KEYS.contains(key) && !v.isEmpty() && !v.matches("^https://\\S+$")) {
                throw bad("آدرس «" + key + "» باید با https:// شروع شود");
            }
            settingRepository.save(new SiteSetting(key, v));
        }
        return settings();
    }

    // ---------- admin: home products ----------

    @Transactional(readOnly = true)
    public List<HomeProductItem> homeProducts(HomeProduct.Type type) {
        return homeProductRepository.findByTypeWithProduct(type).stream()
                .map(h -> new HomeProductItem(h.getProduct().getId(), h.getProduct().getName(), h.getEndsAt()))
                .toList();
    }

    @Transactional
    public List<HomeProductItem> saveHomeProducts(HomeProduct.Type type, HomeProductsRequest req) {
        List<HomeProductsRequest.Item> items = req == null || req.items() == null ? List.of() : req.items();
        if (items.size() > 30) throw bad("حداکثر ۳۰ محصول");
        homeProductRepository.deleteByType(type);
        int order = 0;
        Set<Long> seen = new HashSet<>();
        for (HomeProductsRequest.Item it : items) {
            if (it.productId() == null || !seen.add(it.productId())) continue;
            Product p = productRepository.findById(it.productId())
                    .orElseThrow(() -> bad("محصول با شناسه " + it.productId() + " یافت نشد"));
            homeProductRepository.save(HomeProduct.builder()
                    .type(type).product(p).sortOrder(order++)
                    .endsAt(type == HomeProduct.Type.DAILY_DEAL ? it.endsAt() : null)
                    .build());
        }
        return homeProducts(type);
    }

    // ---------- helpers ----------

    private BannerDto toDto(Banner b) {
        return new BannerDto(b.getId(), b.getPosition().name(), b.getTitle(), b.getLinkUrl(),
                "/api/banners/" + b.getId() + "/image?v=" + b.getUpdatedAt().toEpochMilli(),
                b.getSortOrder(), b.isActive());
    }

    private BrandDto toDto(Brand b) {
        return new BrandDto(b.getId(), b.getName(), b.getLinkUrl(),
                "/api/brands/" + b.getId() + "/logo?v=" + b.getUpdatedAt().toEpochMilli(),
                b.getSortOrder(), b.isActive());
    }

    /** نوع فایل از «محتوای واقعی» تشخیص داده می‌شود؛ Content-Type ارسالی کلاینت قابل اعتماد نیست. */
    private static byte[] readImage(MultipartFile f) {
        byte[] bytes;
        try {
            bytes = f.getBytes();
        } catch (IOException e) {
            throw bad("خواندن فایل ممکن نشد");
        }
        if (sniffType(bytes) == null) {
            throw bad("فقط تصویر JPG، PNG، WEBP یا GIF مجاز است");
        }
        return bytes;
    }

    static String sniffType(byte[] b) {
        String type = ProductService.detectImageType(b);
        if (type != null) {
            return type;
        }
        if (b != null && b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return "image/gif";
        }
        return null;
    }

    private static String cleanLink(String link) {
        if (link == null || link.isBlank()) return null;
        String v = link.trim();
        if (v.length() > 500) throw bad("لینک خیلی طولانی است");
        boolean internal = v.startsWith("/") && !v.startsWith("//");
        if (!internal && !v.matches("^https?://\\S+$")) {
            throw bad("لینک باید با / (مسیر داخلی) یا https:// شروع شود");
        }
        return v;
    }

    private static String clip(String s, int max) {
        if (s == null) return null;
        String v = s.trim();
        return v.length() > max ? v.substring(0, max) : v;
    }

    private static ApiException bad(String msg) {
        return new ApiException(HttpStatus.BAD_REQUEST, msg);
    }
}

package com.monikoshop.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monikoshop.dto.PagedProducts;
import com.monikoshop.dto.ProductResponse;
import com.monikoshop.entity.Category;
import com.monikoshop.repository.CategoryRepository;
import com.monikoshop.repository.ProductSearchRepository;
import com.monikoshop.seo.SeoHtml;
import com.monikoshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * صفحات HTML سبک برای خزنده‌ها و پیش‌نمایش لینک‌ها + sitemap.xml.
 * فرانت‌اند (nginx / vercel.json) فقط درخواست‌های خزنده را به این آدرس‌ها می‌فرستد.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SeoController {

    private static final MediaType HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);
    private static final MediaType XML_UTF8 = new MediaType("application", "xml", StandardCharsets.UTF_8);
    private static final int LIST_SIZE = 48;

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final ProductSearchRepository searchRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.site-name:مونا بیوتی}")
    private String siteName;
    @Value("${app.site-description:فروشگاه اینترنتی لوازم آرایشی و بهداشتی}")
    private String siteDescription;
    @Value("${app.site-url:https://moniko.ir}")
    private String siteUrl;
    @Value("${app.api-url:https://api.moniko.ir}")
    private String apiUrl;
    @Value("${app.price-to-rial:10}")
    private long priceToRial;

    /** تصویر پیش‌فرض اشتراک‌گذاری (تلگرام، واتس‌اپ، ...) برای صفحاتی که تصویر خودشان را ندارند. */
    private String defaultShareImage() {
        return siteUrl + "/og-default.png";
    }

    // ------------------------------------------------------------------ pages

    @GetMapping("/seo/home")
    public ResponseEntity<String> home() {
        List<SeoHtml.Link> links = new ArrayList<>();
        StringBuilder body = new StringBuilder("<p>").append(SeoHtml.esc(siteDescription)).append("</p>\n");
        body.append("<h2>دسته‌بندی‌ها</h2>\n");
        for (Category root : categoryRepository.findRootCategoriesWithChildren()) {
            links.add(new SeoHtml.Link(siteUrl + "/categories/" + seg(root.getSlug()), root.getName()));
            for (Category child : root.getChildren()) {
                links.add(new SeoHtml.Link(siteUrl + "/categories/" + seg(child.getSlug()), root.getName() + " - " + child.getName()));
            }
        }
        body.append(SeoHtml.linkList(links));
        body.append("\n<h2>جدیدترین محصولات</h2>\n").append(productLinks(productService.search(null, null, false, "new", 0, LIST_SIZE)));
        String html = SeoHtml.page(siteName, siteName + " | " + shorten(siteDescription, 60), siteDescription,
                siteUrl + "/", defaultShareImage(), "website", homeJsonLd(), siteName, body.toString());
        return ok(html, HTML_UTF8);
    }

    @GetMapping("/seo/categories/{slug}")
    public ResponseEntity<String> category(@PathVariable String slug) {
        return categoryRepository.findBySlug(slug).map(cat -> {
            PagedProducts page = productService.search(null, cat.getSlug(), false, "new", 0, LIST_SIZE);
            String title = cat.getName() + " | خرید آنلاین " + cat.getName() + " | " + siteName;
            String description = "خرید اینترنتی " + cat.getName() + " با بهترین قیمت و ضمانت اصالت کالا از " + siteName
                    + " (" + page.totalItems() + " کالا)";
            String body = productLinks(page);
            String canonical = siteUrl + "/categories/" + seg(cat.getSlug());
            Map<String, Object> collection = new LinkedHashMap<>();
            collection.put("@type", "CollectionPage");
            collection.put("name", cat.getName());
            collection.put("url", canonical);
            String jsonLd = graph(List.of(collection,
                    breadcrumbList(new String[]{siteName, siteUrl + "/"}, new String[]{cat.getName(), canonical})));
            String html = SeoHtml.page(siteName, title, description, canonical,
                    defaultShareImage(), "website", jsonLd, cat.getName(), body);
            return ok(html, HTML_UTF8);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/seo/products/{id}")
    public ResponseEntity<String> product(@PathVariable Long id) {
        return productService.findActive(id).map(p -> {
            String canonical = siteUrl + "/products/" + p.id();
            String image = absoluteImage(p.imageUrl());
            String shareImage = image != null ? image : defaultShareImage();
            String catName = p.category() == null ? "" : p.category().name();
            String title = p.name() + (catName.isEmpty() ? "" : " | " + catName) + " | " + siteName;
            String description = descriptionOf(p);

            StringBuilder body = new StringBuilder();
            if (image != null) {
                // تصویر اصلی با alt توصیفی (برای ایندکس تصاویر)
                body.append("<p><img src=\"").append(SeoHtml.esc(image)).append("\" alt=\"")
                        .append(SeoHtml.esc(p.name())).append("\"></p>\n");
            }
            if (p.subtitle() != null && !p.subtitle().isBlank()) {
                body.append("<p>").append(SeoHtml.esc(p.subtitle())).append("</p>\n");
            }
            if (p.brand() != null && !p.brand().isBlank()) {
                body.append("<p>برند: ").append(SeoHtml.esc(p.brand())).append("</p>\n");
            }
            body.append("<p>قیمت: ").append(effectivePrice(p).toPlainString()).append(" تومان</p>\n");
            body.append("<p>").append(p.stock() != null && p.stock() > 0 ? "موجود" : "ناموجود").append("</p>\n");
            if (p.description() != null && !p.description().isBlank()) {
                body.append("<p>").append(SeoHtml.esc(p.description())).append("</p>\n");
            }
            if (p.specs() != null && !p.specs().isEmpty()) {
                body.append("<h2>مشخصات</h2>\n<ul>");
                p.specs().forEach(s -> body.append("<li>").append(SeoHtml.esc(s.key())).append(": ")
                        .append(SeoHtml.esc(s.value())).append("</li>"));
                body.append("</ul>\n");
            }
            if (p.category() != null) {
                body.append("<p><a href=\"").append(SeoHtml.esc(siteUrl + "/categories/" + seg(p.category().slug())))
                        .append("\">").append(SeoHtml.esc(catName)).append("</a></p>");
            }
            List<String[]> crumbs = new ArrayList<>();
            crumbs.add(new String[]{siteName, siteUrl + "/"});
            if (p.category() != null) {
                crumbs.add(new String[]{catName, siteUrl + "/categories/" + seg(p.category().slug())});
            }
            crumbs.add(new String[]{p.name(), canonical});
            String jsonLd = graph(List.of(productNode(p, canonical), breadcrumbList(crumbs.toArray(new String[0][]))));
            String html = SeoHtml.page(siteName, title, description, canonical, shareImage, "product",
                    jsonLd, p.name(), body.toString());
            return ok(html, HTML_UTF8);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ---------------------------------------------------------------- sitemap

    @GetMapping("/sitemap.xml")
    public ResponseEntity<String> sitemap() {
        List<String> urls = new ArrayList<>();
        urls.add(siteUrl + "/");
        for (Category c : categoryRepository.findAll()) {
            urls.add(siteUrl + "/categories/" + seg(c.getSlug()));
        }
        for (Long id : searchRepository.activeProductIds()) {
            if (urls.size() >= 50_000) { // سقف استاندارد هر فایل sitemap
                break;
            }
            urls.add(siteUrl + "/products/" + id);
        }
        return ok(SeoHtml.sitemap(urls), XML_UTF8);
    }

    // ---------------------------------------------------------------- helpers

    private ResponseEntity<String> ok(String body, MediaType type) {
        return ResponseEntity.ok()
                .contentType(type)
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(body);
    }

    private String productLinks(PagedProducts page) {
        List<SeoHtml.Link> links = new ArrayList<>();
        for (ProductResponse p : page.items()) {
            links.add(new SeoHtml.Link(siteUrl + "/products/" + p.id(), p.name()));
        }
        return SeoHtml.linkList(links);
    }

    private static String seg(String slug) {
        return UriUtils.encodePathSegment(slug == null ? "" : slug, StandardCharsets.UTF_8);
    }

    private static BigDecimal effectivePrice(ProductResponse p) {
        BigDecimal d = p.discountPrice();
        return (d != null && d.signum() > 0) ? d : p.price();
    }

    private String absoluteImage(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        return url.startsWith("http://") || url.startsWith("https://") ? url : apiUrl + url;
    }

    private String descriptionOf(ProductResponse p) {
        String base = (p.description() != null && !p.description().isBlank())
                ? p.description().replaceAll("\\s+", " ").trim()
                : "خرید آنلاین " + p.name() + " با بهترین قیمت و ضمانت اصالت کالا از " + siteName;
        return shorten(base, 160);
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1).trim() + "…";
    }

    private String homeJsonLd() {
        Map<String, Object> site = new LinkedHashMap<>();
        site.put("@type", "WebSite");
        site.put("name", siteName);
        site.put("url", siteUrl + "/");
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("@type", "SearchAction");
        action.put("target", siteUrl + "/search?q={search_term_string}");
        action.put("query-input", "required name=search_term_string");
        site.put("potentialAction", action);

        Map<String, Object> org = new LinkedHashMap<>();
        org.put("@type", "Organization");
        org.put("name", siteName);
        org.put("url", siteUrl + "/");
        org.put("logo", siteUrl + "/og-default.png");
        return graph(List.of(site, org));
    }

    /** نقش «مسیر صفحه» (BreadcrumbList) برای نمایش در نتایج گوگل. هر عنصر: {نام، آدرس}. */
    private static Map<String, Object> breadcrumbList(String[]... crumbs) {
        List<Map<String, Object>> items = new ArrayList<>();
        int position = 1;
        for (String[] c : crumbs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("@type", "ListItem");
            item.put("position", position++);
            item.put("name", c[0]);
            item.put("item", c[1]);
            items.add(item);
        }
        Map<String, Object> list = new LinkedHashMap<>();
        list.put("@type", "BreadcrumbList");
        list.put("itemListElement", items);
        return list;
    }

    /** چند گره‌ی schema.org را در یک JSON-LD با @graph می‌گذارد. */
    private String graph(List<Map<String, Object>> nodes) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("@context", "https://schema.org");
        root.put("@graph", nodes);
        return json(root);
    }

    private Map<String, Object> productNode(ProductResponse p, String canonical) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("@type", "Product");
        m.put("name", p.name());
        m.put("sku", String.valueOf(p.id()));
        m.put("description", descriptionOf(p));
        List<String> images = new ArrayList<>();
        if (p.images() != null) {
            for (ProductResponse.Image i : p.images()) {
                images.add(absoluteImage(i.url()));
            }
        }
        if (images.isEmpty() && absoluteImage(p.imageUrl()) != null) {
            images.add(absoluteImage(p.imageUrl()));
        }
        if (!images.isEmpty()) {
            m.put("image", images);
        }
        if (p.brand() != null && !p.brand().isBlank()) {
            Map<String, Object> brand = new LinkedHashMap<>();
            brand.put("@type", "Brand");
            brand.put("name", p.brand());
            m.put("brand", brand);
        }
        Map<String, Object> offer = new LinkedHashMap<>();
        offer.put("@type", "Offer");
        offer.put("url", canonical);
        offer.put("priceCurrency", "IRR");
        offer.put("price", effectivePrice(p).multiply(BigDecimal.valueOf(priceToRial)).toPlainString());
        offer.put("availability", p.stock() != null && p.stock() > 0
                ? "https://schema.org/InStock" : "https://schema.org/OutOfStock");
        offer.put("itemCondition", "https://schema.org/NewCondition");
        Map<String, Object> seller = new LinkedHashMap<>();
        seller.put("@type", "Organization");
        seller.put("name", siteName);
        offer.put("seller", seller);
        m.put("offers", offer);
        return m;
    }

    private String json(Map<String, Object> m) {
        try {
            return objectMapper.writeValueAsString(m);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}

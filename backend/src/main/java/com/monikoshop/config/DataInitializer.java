package com.monikoshop.config;

import com.monikoshop.entity.*;
import com.monikoshop.repository.CategoryRepository;
import com.monikoshop.repository.ProductRepository;
import com.monikoshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create admin user if not exists
        if (!userRepository.existsByEmail("admin@monikoshop.ir")) {
            User admin = User.builder()
                    .fullName("مدیر سیستم")
                    .email("admin@monikoshop.ir")
                    .password(passwordEncoder.encode("admin123"))
                    .phone("09120000000")
                    .roles(Set.of(Role.ROLE_ADMIN, Role.ROLE_USER))
                    .enabled(true)
                    .build();
            userRepository.save(admin);
        }

        // Seed hierarchical categories
        if (categoryRepository.count() == 0) {
            // ===== دسته‌های اصلی =====
            Category makeup = saveRoot("آرایشی", "makeup", 1);
            Category skincare = saveRoot("پوست", "skincare", 2);
            Category hair = saveRoot("مو", "hair", 3);
            Category body = saveRoot("بدن", "body", 4);
            Category fragrance = saveRoot("عطر و ادکلن", "fragrance", 5);
            Category health = saveRoot("سلامت", "health", 6);

            // ===== زیر‌دسته‌های آرایشی =====
            saveChild(makeup, "آرایش صورت", "face-makeup", 1);
            saveChild(makeup, "آرایش چشم", "eye-makeup", 2);
            saveChild(makeup, "آرایش لب", "lip-makeup", 3);
            saveChild(makeup, "آرایش ابرو", "brow-makeup", 4);
            saveChild(makeup, "ابزار آرایشی", "makeup-tools", 5);

            // ===== زیر‌دسته‌های پوست =====
            saveChild(skincare, "پاک‌کننده صورت", "face-cleanser", 1);
            saveChild(skincare, "مرطوب‌کننده و آبرسان", "moisturizer", 2);
            saveChild(skincare, "ضد آفتاب", "sunscreen", 3);
            saveChild(skincare, "سرم پوست صورت", "face-serum", 4);
            saveChild(skincare, "ماسک صورت و بدن", "face-mask", 5);
            saveChild(skincare, "ضد چروک", "anti-aging", 6);

            // ===== زیر‌دسته‌های مو =====
            saveChild(hair, "شامپو مو", "shampoo", 1);
            saveChild(hair, "نرم‌کننده مو", "conditioner", 2);
            saveChild(hair, "ماسک مو", "hair-mask", 3);
            saveChild(hair, "روغن مو", "hair-oil", 4);
            saveChild(hair, "حالت‌دهنده مو", "hair-styling", 5);
            saveChild(hair, "ضد ریزش و تقویت‌کننده", "hair-growth", 6);

            // ===== زیر‌دسته‌های بدن =====
            saveChild(body, "شامپو بدن", "body-wash", 1);
            saveChild(body, "لوسیون و کرم بدن", "body-lotion", 2);
            saveChild(body, "اسپری و بادی اسپلش", "body-splash", 3);
            saveChild(body, "ضد تعریق", "deodorant", 4);

            // ===== زیر‌دسته‌های عطر =====
            saveChild(fragrance, "عطر زنانه", "women-perfume", 1);
            saveChild(fragrance, "عطر مردانه", "men-perfume", 2);
            saveChild(fragrance, "عطر یونیسکس", "unisex-perfume", 3);

            // ===== زیر‌دسته‌های سلامت =====
            saveChild(health, "مکمل‌ها", "supplements", 1);
            saveChild(health, "ویتامین‌ها", "vitamins", 2);

            // ===== محصولات نمونه =====
            productRepository.save(Product.builder()
                    .name("کرم پودر مات برند X")
                    .description("کرم پودر با پوشش کامل و ماندگاری بالا، مناسب پوست چرب")
                    .price(new BigDecimal("450000"))
                    .discountPrice(new BigDecimal("389000"))
                    .brand("BrandX")
                    .category(makeup)
                    .stock(50)
                    .imageUrl("https://via.placeholder.com/300x300?text=Foundation")
                    .active(true)
                    .build());

            productRepository.save(Product.builder()
                    .name("سرم ویتامین C")
                    .description("سرم روشن‌کننده و ضد لک با ویتامین C خالص ۲۰٪")
                    .price(new BigDecimal("780000"))
                    .discountPrice(new BigDecimal("650000"))
                    .brand("SkinLab")
                    .category(skincare)
                    .stock(30)
                    .imageUrl("https://via.placeholder.com/300x300?text=Serum")
                    .active(true)
                    .build());

            productRepository.save(Product.builder()
                    .name("شامپو ترمیم‌کننده مو")
                    .description("شامپو حاوی کراتین و روغن آرگان برای موهای آسیب‌دیده")
                    .price(new BigDecimal("320000"))
                    .brand("HairCare")
                    .category(hair)
                    .stock(100)
                    .imageUrl("https://via.placeholder.com/300x300?text=Shampoo")
                    .active(true)
                    .build());

            productRepository.save(Product.builder()
                    .name("عطر زنانه بلوم")
                    .description("عطر گرم و شیرین با نت‌های گل رز و وانیل")
                    .price(new BigDecimal("1250000"))
                    .discountPrice(new BigDecimal("1100000"))
                    .brand("Parfum")
                    .category(fragrance)
                    .stock(20)
                    .imageUrl("https://via.placeholder.com/300x300?text=Perfume")
                    .active(true)
                    .build());

            productRepository.save(Product.builder()
                    .name("کپسول کلاژن")
                    .description("مکمل کلاژن هیدرولیز شده برای پوست و مفاصل")
                    .price(new BigDecimal("560000"))
                    .brand("HealthPlus")
                    .category(health)
                    .stock(80)
                    .imageUrl("https://via.placeholder.com/300x300?text=Collagen")
                    .active(true)
                    .build());

            productRepository.save(Product.builder()
                    .name("ریمل حجم‌دهنده")
                    .description("ریمل ضد آب با فرچه سیلیکونی برای حجم و بلندی")
                    .price(new BigDecimal("280000"))
                    .discountPrice(new BigDecimal("245000"))
                    .brand("BrandX")
                    .category(makeup)
                    .stock(60)
                    .imageUrl("https://via.placeholder.com/300x300?text=Mascara")
                    .active(true)
                    .build());
        }
    }

    private Category saveRoot(String name, String slug, int order) {
        return categoryRepository.save(Category.builder()
                .name(name)
                .slug(slug)
                .imageUrl("https://via.placeholder.com/200x200?text=" + slug)
                .sortOrder(order)
                .parent(null)
                .build());
    }

    private Category saveChild(Category parent, String name, String slug, int order) {
        return categoryRepository.save(Category.builder()
                .name(name)
                .slug(slug)
                .sortOrder(order)
                .parent(parent)
                .build());
    }
}

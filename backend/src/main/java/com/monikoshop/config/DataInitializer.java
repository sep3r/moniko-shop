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

        // Seed categories
        if (categoryRepository.count() == 0) {
            Category makeup = categoryRepository.save(Category.builder()
                    .name("آرایشی")
                    .slug("makeup")
                    .imageUrl("https://via.placeholder.com/200x200?text=Makeup")
                    .sortOrder(1)
                    .build());

            Category skincare = categoryRepository.save(Category.builder()
                    .name("مراقبت پوست")
                    .slug("skincare")
                    .imageUrl("https://via.placeholder.com/200x200?text=Skincare")
                    .sortOrder(2)
                    .build());

            Category hair = categoryRepository.save(Category.builder()
                    .name("مراقبت مو")
                    .slug("hair")
                    .imageUrl("https://via.placeholder.com/200x200?text=Hair")
                    .sortOrder(3)
                    .build());

            Category fragrance = categoryRepository.save(Category.builder()
                    .name("عطر و ادکلن")
                    .slug("fragrance")
                    .imageUrl("https://via.placeholder.com/200x200?text=Fragrance")
                    .sortOrder(4)
                    .build());

            Category health = categoryRepository.save(Category.builder()
                    .name("سلامت و مکمل")
                    .slug("health")
                    .imageUrl("https://via.placeholder.com/200x200?text=Health")
                    .sortOrder(5)
                    .build());

            // Seed products
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
}

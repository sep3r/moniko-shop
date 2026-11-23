# مونا بیوتی (Mona Beauty) - فروشگاه آنلاین

پروژه فروشگاه آنلاین لوازم آرایشی و بهداشتی.

## فناوری‌ها

- **Backend**: Java 21 + Spring Boot 3.5 + Spring Security + JWT + Spring Data JPA + PostgreSQL
- **Frontend**: React 18 + TypeScript + Vite + React Router
- **Infrastructure**: Docker Compose + PostgreSQL
- **پیام‌رسانی**: این نسخه به Kafka وابستگی ندارد.

## ویژگی‌ها

- ثبت‌نام و ورود با JWT
- نقش‌های USER و ADMIN
- صفحه اصلی با دسته‌بندی و محصولات نمونه
- سبد خرید در مرورگر و ثبت سفارش
- پنل کاربری و تاریخچه سفارش‌ها
- پنل مدیریت محصولات، دسته‌بندی‌ها، سفارش‌ها و کاربران
- صفحه محصول شبیه دیجی‌کالا: گالری ۱ تا ۵ تصویر با بزرگ‌نمایی و ذره‌بین (موس/لمس)، جدول مشخصات و معرفی محصول
- رابط کاربری فارسی و RTL

## اجرا با Docker

```bash
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- PostgreSQL: localhost:5432

### حساب ادمین پیش‌فرض
- ایمیل: `admin@monikoshop.ir`
- رمز: `admin123`

> برای محیط واقعی، رمز ادمین پیش‌فرض و مقادیر محرمانه را تغییر بده.

## اجرای محلی بدون Docker برای بک‌اند

ابتدا PostgreSQL را اجرا کن، سپس:

```bash
cd backend
./mvnw spring-boot:run
```

در ویندوز می‌توانی از `mvnw.cmd spring-boot:run` استفاده کنی.

## APIهای احراز هویت

- `POST /api/auth/register` - ثبت‌نام
- `POST /api/auth/login` - ورود
- `GET /api/auth/me` - اطلاعات کاربر فعلی (نیاز به توکن)

---
> تغییرات اخیر (جستجوی سمت سرور، Flyway، بکاپ، سئو، تست و CI) و نکات استقرار: [IMPROVEMENTS.md](IMPROVEMENTS.md)


## پالت رنگ (قفل‌شده)

سایت فقط از این ۴ رنگ + مشکی (برای فونت‌ها) ساخته شده است:

| رنگ | کد | نقش |
|---|---|---|
| مرجانی | `#F58F83` | رنگ اصلی (دکمه‌ها، نوار بالا، نشان‌ها، هاور) |
| سبز نعنایی | `#D8F0E5` | زمینه‌های ملایم، نوار منو، سرجدول‌ها |
| سبز مریم‌گلی | `#A8BFAE` | حاشیه‌ها، فوتر |
| سفید شیری | `#FFFCF8` | پس‌زمینه‌ی سایت و کارت‌ها |

رنگ‌ها در `frontend/src/index.css` (`:root`) تعریف شده‌اند. برای اضافه نشدن رنگ دیگر: `npm run check:palette` (در CI هم اجرا می‌شود).

## سئو

- خزنده‌ها (گوگل، تلگرام، واتس‌اپ، ...) HTML سروری `/api/seo/*` را می‌گیرند؛ کاربران عادی اپ React را می‌بینند.
- هر صفحه: عنوان، توضیحات، canonical، Open Graph، کارت توییتر، و داده‌ی ساختاریافته (Product، BreadcrumbList، CollectionPage، WebSite، Organization).
- تصویر پیش‌فرض اشتراک‌گذاری: `frontend/public/og-default.png` (۱۲۰۰×۶۳۰).
- فونت وزیرمتن داخل خود سایت است (بدون Google Fonts)؛ صفحه‌های پنل کاربر و ادمین lazy بارگذاری می‌شوند.
- صفحه‌ی ناموجود (محصول، دسته، ۴۰۴) `noindex` می‌گیرد؛ سبد، حساب و ادمین هم `noindex` + `Disallow` هستند.

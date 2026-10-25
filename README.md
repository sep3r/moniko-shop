# MonikoShop - فروشگاه آنلاین

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

## امنیت

پس از به‌روزرسانی‌های امنیتی، فایل `SECURITY.md` را مطالعه کنید.

- وابستگی‌های frontend به نسخه‌های پچ‌شده ارتقا یافته‌اند.
- هدرهای امنیتی به nginx اضافه شده است.
- رمز ادمین پیش‌فرض از متغیر محیطی `ADMIN_PASSWORD` خوانده می‌شود (پیش‌فرض قوی‌تر).

قبل از استقرار production حتماً `JWT_SECRET` و رمز دیتابیس را تغییر دهید.

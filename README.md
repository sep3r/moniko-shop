# MonikoShop - فروشگاه آنلاین

پروژه اسکلت فروشگاه آنلاین لوازم آرایشی و بهداشتی با تکنولوژی‌های:

- **Backend**: Java 25 + Spring Boot 3.5 + Spring Security + JWT + Spring Kafka + PostgreSQL
- **Frontend**: React 18 + TypeScript + Vite + React Router
- **Infrastructure**: Docker Compose + Kafka (KRaft) + Postgres

## ویژگی‌های فعلی

- ثبت‌نام و ورود با JWT
- نقش‌های USER و ADMIN
- ارسال رویداد به Kafka هنگام ثبت‌نام، لاگین و ثبت سفارش
- صفحه اصلی با دسته‌بندی و محصولات نمونه
- **سبد خرید واقعی** (ذخیره در مرورگر) + تکمیل خرید و ثبت سفارش
- **پنل کاربری**: مشاهده پروفایل و تاریخچه سفارش‌ها (`/profile`, `/orders`)
- **پنل مدیریت** (`/admin`, فقط برای ADMIN): مدیریت محصولات، دسته‌بندی‌ها، سفارش‌ها (تغییر وضعیت) و کاربران (فعال/غیرفعال، ارتقا به ادمین)
- RTL و رابط کاربری فارسی
- Docker کامل

## اجرا با Docker

```bash
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Postgres: localhost:5432
- Kafka: localhost:9092

### حساب ادمین پیش‌فرض
- ایمیل: `admin@monikoshop.ir`
- رمز: `admin123`

## اجرای محلی (بدون Docker)

### Backend
```bash
cd backend
# نیاز به Postgres و Kafka در حال اجرا
./mvnw spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

## APIهای احراز هویت

- `POST /api/auth/register` - ثبت‌نام
- `POST /api/auth/login` - ورود
- `GET /api/auth/me` - اطلاعات کاربر فعلی (نیاز به توکن)

## مرحله بعدی (کنترل پنل)

در مرحله بعد می‌توان کنترل پنل ادمین، مدیریت محصولات، سفارشات، سبد خرید و ... را اضافه کرد.

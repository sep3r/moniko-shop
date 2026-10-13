# استقرار MonikoShop: بک‌اند روی رانفلر + فرانت روی Vercel

## ۱) دیتابیس (Postgres)
1. در پنل رانفلر یک سرویس **PostgreSQL** بساز (بخش دیتابیس‌ها) یا یک سرویس Docker با ایمیج `postgres:16-alpine`.
2. نام دیتابیس، یوزر، پسورد و آدرس/پورت اتصال داخلی را یادداشت کن.

## ۲) بک‌اند
1. ریپوی گیت‌هاب را وصل کن. در ریشه‌ی ریپو یک `Dockerfile` هست که فقط بک‌اند را build می‌کند، پس نیازی به تنظیم زیرپوشه نیست. (نوع build را Dockerfile انتخاب کن.)
2. در تنظیمات سرویس، این **متغیرهای محیطی** را بگذار:

| متغیر | مقدار |
|---|---|
| `PORT` | `8000` (همان پورتی که در پنل Expose کرده‌ای) |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST:5432/DBNAME` |
| `SPRING_DATASOURCE_USERNAME` | یوزر دیتابیس |
| `SPRING_DATASOURCE_PASSWORD` | پسورد دیتابیس |
| `JWT_SECRET` | یک رشته‌ی تصادفی طولانی (حداقل ۴۰ کاراکتر) |
| `CORS_ALLOWED_ORIGINS` | (اختیاری) دامنه‌ی اختصاصی فرانت، مثلاً `https://moniko.ir` |

3. در بخش دامنه، «ایجاد دامنه موقت» را بزن و روی پورت `8000` وصل کن.
4. تست: `https://DOMAIN/api/products` باید JSON برگرداند.

## ۳) فرانت (Vercel)
1. Vercel → Project → Settings → Environment Variables:
   `VITE_API_URL` = `https://دامنه-رانفلر/api`  (حتماً با `/api` در انتها)
2. Redeploy کن (متغیرهای Vite موقع build خوانده می‌شوند).

## ۴) بعد از بالا آمدن
- با `admin@monikoshop.ir` / `admin123` وارد شو و **فوراً رمز ادمین را عوض کن**.
- دیتابیس جدید خالی است؛ محصولات قبلی (روی Railway) منتقل نمی‌شوند.

## عیب‌یابی
- خطای CORS: دامنه‌ی فرانت را در `CORS_ALLOWED_ORIGINS` اضافه کن.
- ۵۰۲/عدم پاسخ: مطمئن شو `PORT` با پورت Expose یکی است.
- خطای اتصال به دیتابیس: لاگ سرویس را ببین و `SPRING_DATASOURCE_URL` را چک کن.

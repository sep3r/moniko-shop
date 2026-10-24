# استقرار MonikoShop روی رانفلر

آدرس‌ها:
- بک‌اند: `https://api.moniko.ir` (سرویس `monikoshop-backend`، پورت Expose: 8000)
- فرانت: `https://moniko.ir`

## ۱) بک‌اند (سرویس Docker)
1. کد را در گیت‌هاب push کن (`git add . && git commit -m "api.moniko.ir" && git push`).
2. سرویس از ریپو با `Dockerfile` ریشه build می‌شود. بعد از push، در پنل سرویس **ری‌دیپلوی/بیلد مجدد** بزن.
3. متغیرهای محیطی سرویس:

| متغیر | مقدار |
|---|---|
| `PORT` | `8000` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST:5432/DBNAME` |
| `SPRING_DATASOURCE_USERNAME` | یوزر دیتابیس |
| `SPRING_DATASOURCE_PASSWORD` | پسورد دیتابیس |
| `JWT_SECRET` | رشته‌ی تصادفی بلند (۴۰+ کاراکتر) |
| `CORS_ALLOWED_ORIGINS` | `https://moniko.ir,https://www.moniko.ir` |

4. تست: `https://api.moniko.ir/api/products` باید JSON برگرداند.

## ۲) دیتابیس
یک سرویس PostgreSQL در رانفلر بساز و آدرس داخلی/پورت را در `SPRING_DATASOURCE_URL` بگذار.

## ۳) فرانت (سرویس Docker جدید)
1. در پنل پروژه → «سرویس‌ها» → «ایجاد» → Docker از گیت‌هاب.
2. **Dockerfile path**: `frontend/Dockerfile` و **context**: `frontend` (یا ریشه‌ی پوشه‌ی frontend).
3. در Build Args (یا متغیر محیطی زمان build): `VITE_API_URL` = `https://api.moniko.ir/api`
   (اگر ندادی، مقدار فایل `frontend/.env.production` استفاده می‌شود و همان درست است.)
4. پورت Expose: `80`.
5. دامنه: `moniko.ir` (و `www.moniko.ir`) را به این سرویس وصل کن و در بخش «تنظیم دامنه» رکوردها را مطابق راهنمای رانفلر (ns1/ns2.runflare-dns.com) بگذار؛ SSL را هم از «تنظیم SSL» فعال کن.

## ۴) بعد از بالا آمدن
- با `admin@monikoshop.ir` / `admin123` وارد شو و **فوراً رمز را عوض کن**.
- `JWT_SECRET` پیش‌فرض را حتماً عوض کن.

## عیب‌یابی
- خطای CORS در کنسول مرورگر: دامنه‌ی دقیق فرانت را در `CORS_ALLOWED_ORIGINS` بگذار و بک‌اند را ری‌استارت کن.
- ۵۰۲: `PORT` باید با پورت Expose (۸۰۰۰) یکی باشد.
- تصاویر محصول لود نمی‌شوند: مطمئن شو `VITE_API_URL` موقع build ست شده (متغیرهای Vite فقط زمان build خوانده می‌شوند).
- ۴۰۰ در آپلود تصویر: حجم تصویر زیر ۱۰MB باشد.

# اجرای مونا بیوتی روی سیستم خودت با Docker

## پیش‌نیاز
Docker Desktop (ویندوز/مک) یا Docker Engine + Compose (لینوکس) نصب و روشن باشد.
بررسی: `docker --version` و `docker compose version`

## ۱) ساخت فایل .env (یک بار)
در ریشه‌ی پروژه (کنار docker-compose.yml):

    cp .env.example .env        # ویندوز: copy .env.example .env

سپس .env را باز کن و مقدارها را پر کن (بدون فاصله دور `=`):

    POSTGRES_PASSWORD=MyLocalDbPass123
    JWT_SECRET=یک-رشته-تصادفی-حداقل-۴۰-کاراکتر-انگلیسی-و-عدد
    ADMIN_EMAIL=admin@monikoshop.ir
    ADMIN_PASSWORD=AdminPass12345        # حداقل ۱۰ کاراکتر، وگرنه ادمین ساخته نمی‌شود

## ۲) بالا آوردن
    docker compose up -d --build

بار اول چند دقیقه طول می‌کشد (دانلود ایمیج‌ها + build). بعد از آن:

- سایت:      http://localhost:3000
- API:       http://localhost:8080/api/products
- ورود ادمین: http://localhost:3000/login با ADMIN_EMAIL / ADMIN_PASSWORD

## ۳) دستورهای روزمره
    docker compose ps                    # وضعیت سرویس‌ها
    docker compose logs -f backend       # لاگ بک‌اند (خطاها اینجا هستند)
    docker compose restart backend
    docker compose up -d --build         # بعد از هر تغییر در کد
    docker compose down                  # خاموش (داده‌ها می‌مانند)
    docker compose down -v               # خاموش + پاک کردن کامل دیتابیس

## ۴) چک کردن اینکه عکس‌ها توی دیتابیس نشسته‌اند
    docker exec -it monikoshop-db psql -U monikoshop -d monikoshop \
      -c "SELECT id, product_id, content_type, sort_order, octet_length(data) AS bytes FROM product_images ORDER BY id DESC;"

اگر ردیف دیدی (bytes بزرگ‌تر از صفر) یعنی عکس در دیتابیس ذخیره شده.
مستقیم باز کردن عکس: http://localhost:8080/api/products/<productId>/images/<imageId>

## عیب‌یابی سریع
- پورت ۳۰۰۰ یا ۸۰۸۰ اشغال است → پورت چپِ `ports:` را در docker-compose.yml عوض کن.
- بک‌اند بالا نمی‌آید → `docker compose logs backend`
- "No admin user exists" در لاگ → ADMIN_PASSWORD کوتاه‌تر از ۱۰ کاراکتر یا خالی است؛ اصلاح و `docker compose up -d` بزن.
- بعد از تغییر ADMIN_PASSWORD ادمین عوض نمی‌شود (فقط وقتی هیچ ادمینی نیست ساخته می‌شود) → `docker compose down -v` و دوباره بالا بیاور.

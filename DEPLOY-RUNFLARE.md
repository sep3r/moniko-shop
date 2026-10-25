# استقرار MonikoShop (رانفلر + Vercel) — نسخه‌ی امن‌شده

## ۱) بک‌اند (سرویس Docker روی رانفلر، پورت 8000)

متغیرهای محیطی سرویس `monikoshop-backend`:

| متغیر | مقدار | توضیح |
|---|---|---|
| `PORT` | `8000` | با پورت Expose یکی باشد |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST:5432/DBNAME` | بدون یوزر/پسورد داخل آدرس |
| `SPRING_DATASOURCE_USERNAME` | یوزر دیتابیس | |
| `SPRING_DATASOURCE_PASSWORD` | پسورد دیتابیس | حتماً قوی؛ پیش‌فرض ندارد |
| `JWT_SECRET` | **الزامی** — رشته‌ی تصادفی ≥ ۴۰ کاراکتر | بدون آن کلید موقت ساخته می‌شود و با هر ری‌استارت همه logout می‌شوند |
| `CORS_ALLOWED_ORIGINS` | `https://moniko.ir,https://www.moniko.ir` | آدرس دقیق (بدون `*`) |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | ایمیل و رمز (≥ ۱۰ کاراکتر) | فقط وقتی هیچ ادمینی وجود ندارد استفاده می‌شود |
| `JWT_EXPIRATION` | اختیاری، میلی‌ثانیه (پیش‌فرض ۸ ساعت) | |

### ایمیل (برای «فراموشی رمز عبور»)
بدون این متغیرها سایت کار می‌کند ولی ایمیل بازیابی ارسال نمی‌شود (در لاگ هشدار می‌آید).

| متغیر | مثال |
|---|---|
| `SPRING_MAIL_HOST` | آدرس SMTP سرویس ایمیل شما |
| `SPRING_MAIL_PORT` | `587` |
| `SPRING_MAIL_USERNAME` | یوزر SMTP (معمولاً خود ایمیل) |
| `SPRING_MAIL_PASSWORD` | رمز SMTP (یا App Password) |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` | `true` |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | `true` |
| `MAIL_FROM` | اختیاری؛ آدرس فرستنده (پیش‌فرض: همان username) |
| `APP_BASE_URL` | `https://moniko.ir` (برای ساخت لینک بازیابی) |

نکته: سرور رانفلر باید بتواند به پورت SMTP سرویس ایمیل وصل شود؛ بعضی سرویس‌های خارجی از ایران بسته‌اند. بعد از تنظیم، یک بار از صفحه‌ی «فراموشی رمز» تست کن و لاگ بک‌اند را ببین.

ساخت رشته‌ی تصادفی برای `JWT_SECRET` (در ترمینال): `openssl rand -base64 48`

## ۲) فرانت (Vercel)
- متغیر `VITE_API_URL` = `https://api.moniko.ir/api`
- بعد از هر تغییر متغیر، Redeploy (بدون Build Cache).
- Node در Vercel باید ۲۰.۱۹ یا بالاتر باشد (در `package.json` تنظیم شده است).
- هدرهای امنیتی و CSP در `frontend/vercel.json` هستند. اگر دامنه‌ی API عوض شد، `connect-src` را هم عوض کن.

## ۳) بعد از هر استقرار
- لاگ بک‌اند: `Started MonikoShopApplication` و نبودن هشدار `JWT_SECRET is missing`.
- `https://api.moniko.ir/api/categories/tree` باید JSON بدهد.

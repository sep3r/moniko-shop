#!/bin/sh
# رمزهای تصادفی قوی برای فایل .env می‌سازد (چیزی روی دیسک نمی‌نویسد؛ خروجی را در .env کپی کنید).
# بعد از عوض‌کردن JWT_SECRET همه‌ی کاربران یک بار logout می‌شوند.
set -eu
rand() { openssl rand -base64 48 | tr -d '/+=\n' | cut -c1-"$1"; }
echo "POSTGRES_PASSWORD=$(rand 28)"
echo "JWT_SECRET=$(rand 64)"
echo "ADMIN_PASSWORD=$(rand 20)"
echo "OTP_HASH_SECRET=$(rand 32)"

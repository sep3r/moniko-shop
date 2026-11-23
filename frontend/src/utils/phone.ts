/** ارقام فارسی/عربی را انگلیسی می‌کند. */
export function toEnDigits(v: string): string {
  return v
    .replace(/[\u06F0-\u06F9]/g, (c) => String(c.charCodeAt(0) - 0x06f0))
    .replace(/[\u0660-\u0669]/g, (c) => String(c.charCodeAt(0) - 0x0660));
}

/** شماره‌ی موبایل ایران را به شکل 09123456789 درمی‌آورد؛ نامعتبر ⇒ null (همان منطق بک‌اند). */
export function normalizePhone(raw: string): string | null {
  let d = toEnDigits(raw).replace(/[^0-9]/g, '');
  if (d.startsWith('0098') && d.length === 14) d = '0' + d.slice(4);
  else if (d.startsWith('98') && d.length === 12) d = '0' + d.slice(2);
  else if (d.startsWith('9') && d.length === 10) d = '0' + d;
  return /^09\d{9}$/.test(d) ? d : null;
}

export const PHONE_ERROR = 'شماره موبایل معتبر نیست. نمونه: 09123456789';
export const PASSWORD_ERROR = 'رمز عبور باید ۸ تا ۶۴ کاراکتر و شامل حداقل یک حرف و یک عدد باشد';
export const isStrongPassword = (p: string) =>
  p.length >= 8 && p.length <= 64 && /\p{L}/u.test(p) && /\d/.test(p);

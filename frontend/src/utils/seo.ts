import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { Product } from '../types';

export const SITE_NAME = 'مونا بیوتی';
/** آدرس اصلی سایت (برای canonical)؛ در صورت نیاز با VITE_SITE_URL تغییر دهید. */
export const SITE_URL = ((import.meta.env.VITE_SITE_URL as string | undefined) || 'https://moniko.ir').replace(/\/$/, '');
export const DEFAULT_TITLE = `${SITE_NAME} | فروشگاه اینترنتی لوازم آرایشی و بهداشتی`;
/** تصویر پیش‌فرض اشتراک‌گذاری لینک (تلگرام، واتس‌اپ، ...) برای صفحاتی که تصویر خودشان را ندارند */
export const DEFAULT_IMAGE = `${SITE_URL}/og-default.png`;
export const DEFAULT_DESCRIPTION =
  'خرید آنلاین لوازم آرایشی، بهداشتی و مراقبتی پوست و مو با ضمانت اصالت کالا و ارسال سریع به سراسر کشور.';

/** قیمت‌ها «تومان» هستند ولی ISO 4217 فقط IRR (ریال) دارد؛ پس ×۱۰ */
const TOMAN_TO_RIAL = 10;

export function shorten(text: string, max: number): string {
  const t = text.replace(/\s+/g, ' ').trim();
  return t.length <= max ? t : t.slice(0, max - 1).trim() + '…';
}

export function productDescription(p: Product): string {
  if (p.description && p.description.trim()) return shorten(p.description, 160);
  return shorten(`خرید آنلاین ${p.name} با بهترین قیمت و ضمانت اصالت کالا از ${SITE_NAME}`, 160);
}

/** داده ساختاریافته‌ی schema.org/Product برای نمایش قیمت و موجودی در نتایج گوگل */
export function buildProductJsonLd(p: Product, toAbsoluteImage: (u?: string | null) => string): Record<string, unknown> {
  const images = (p.images?.length ? p.images.map((i) => i.url) : p.imageUrl ? [p.imageUrl] : [])
    .map((u) => toAbsoluteImage(u))
    .filter(Boolean);
  const price = p.discountPrice && p.discountPrice > 0 ? p.discountPrice : p.price;
  const ld: Record<string, unknown> = {
    '@type': 'Product',
    name: p.name,
    sku: String(p.id),
    description: productDescription(p),
    offers: {
      '@type': 'Offer',
      url: `${SITE_URL}/products/${p.id}`,
      priceCurrency: 'IRR',
      price: String(Math.round(price * TOMAN_TO_RIAL)),
      availability: p.stock > 0 ? 'https://schema.org/InStock' : 'https://schema.org/OutOfStock',
      itemCondition: 'https://schema.org/NewCondition',
      seller: { '@type': 'Organization', name: SITE_NAME },
    },
  };
  if (images.length) ld.image = images;
  if (p.brand) ld.brand = { '@type': 'Brand', name: p.brand };

  const crumbs = [{ name: SITE_NAME, url: `${SITE_URL}/` }];
  if (p.category?.slug) crumbs.push({ name: p.category.name, url: `${SITE_URL}/categories/${encodeURIComponent(p.category.slug)}` });
  crumbs.push({ name: p.name, url: `${SITE_URL}/products/${p.id}` });
  return { '@context': 'https://schema.org', '@graph': [ld, breadcrumbNode(crumbs)] };
}

/** «مسیر صفحه» (BreadcrumbList) برای نمایش در نتایج گوگل */
export function breadcrumbNode(items: { name: string; url: string }[]): Record<string, unknown> {
  return {
    '@type': 'BreadcrumbList',
    itemListElement: items.map((it, i) => ({ '@type': 'ListItem', position: i + 1, name: it.name, item: it.url })),
  };
}

/** صفحه‌ی دسته‌بندی: CollectionPage + مسیر صفحه */
export function buildCategoryJsonLd(name: string, slug: string): Record<string, unknown> {
  const url = `${SITE_URL}/categories/${encodeURIComponent(slug)}`;
  return {
    '@context': 'https://schema.org',
    '@graph': [
      { '@type': 'CollectionPage', name, url },
      breadcrumbNode([{ name: SITE_NAME, url: `${SITE_URL}/` }, { name, url }]),
    ],
  };
}

/** صفحه‌ی اصلی: WebSite (با جستجو) + Organization */
export function buildHomeJsonLd(): Record<string, unknown> {
  return {
    '@context': 'https://schema.org',
    '@graph': [
      {
        '@type': 'WebSite', name: SITE_NAME, url: `${SITE_URL}/`,
        potentialAction: { '@type': 'SearchAction', target: `${SITE_URL}/search?q={search_term_string}`, 'query-input': 'required name=search_term_string' },
      },
      { '@type': 'Organization', name: SITE_NAME, url: `${SITE_URL}/`, logo: DEFAULT_IMAGE },
    ],
  };
}

// ---------------------------------------------------------------- DOM helpers

const MARK = 'data-seo';

function upsertMeta(attr: 'name' | 'property', key: string, content: string) {
  let el = document.head.querySelector<HTMLMetaElement>(`meta[${attr}="${key}"]`);
  if (!el) {
    el = document.createElement('meta');
    el.setAttribute(attr, key);
    el.setAttribute(MARK, '1');
    document.head.appendChild(el);
  }
  el.setAttribute('content', content);
}

export interface SeoOptions {
  title?: string;
  description?: string;
  /** آدرس مطلق تصویر برای پیش‌نمایش لینک (تلگرام، واتس‌اپ، ...) */
  image?: string;
  /** مسیر canonical مثل /products/12 */
  path?: string;
  noindex?: boolean;
  type?: 'website' | 'product';
  jsonLd?: Record<string, unknown> | null;
}

function applySeo(o: SeoOptions) {
  const title = o.title || DEFAULT_TITLE;
  const description = o.description || DEFAULT_DESCRIPTION;
  document.title = title;
  upsertMeta('name', 'description', description);
  upsertMeta('property', 'og:site_name', SITE_NAME);
  upsertMeta('property', 'og:locale', 'fa_IR');
  upsertMeta('property', 'og:type', o.type || 'website');
  upsertMeta('property', 'og:title', title);
  upsertMeta('property', 'og:description', description);
  upsertMeta('property', 'og:image', o.image || DEFAULT_IMAGE);
  upsertMeta('name', 'twitter:card', 'summary_large_image');
  if (o.path) {
    const href = SITE_URL + o.path;
    upsertMeta('property', 'og:url', href);
    const link = document.createElement('link');
    link.rel = 'canonical';
    link.href = href;
    link.setAttribute(MARK, '1');
    document.head.appendChild(link);
  }
  if (o.noindex) upsertMeta('name', 'robots', 'noindex, nofollow');
  if (o.jsonLd) {
    const s = document.createElement('script');
    s.type = 'application/ld+json';
    s.setAttribute(MARK, '1');
    // «</» داخل JSON نباید تگ script را ببندد
    s.textContent = JSON.stringify(o.jsonLd).replace(/<\//g, '<\\/');
    document.head.appendChild(s);
  }
}

function resetSeo() {
  document.head.querySelectorAll(`[${MARK}]`).forEach((el) => el.remove());
  document.title = DEFAULT_TITLE;
  const d = document.head.querySelector('meta[name="description"]');
  if (d) d.setAttribute('content', DEFAULT_DESCRIPTION);
  // متاهای ثابت index.html هم به حالت پیش‌فرض برگردند
  document.head.querySelector('meta[property="og:type"]')?.setAttribute('content', 'website');
  document.head.querySelector('meta[property="og:image"]')?.setAttribute('content', DEFAULT_IMAGE);
}

/**
 * عنوان، توضیحات، canonical، Open Graph و JSON-LD صفحه را تنظیم می‌کند و هنگام ترک صفحه
 * به پیش‌فرض برمی‌گرداند. (گوگل جاوااسکریپت را اجرا می‌کند؛ ربات‌های پیش‌نمایش لینک
 * HTML سروری /api/seo/* را می‌گیرند.)
 */
export function useSeo(o: SeoOptions) {
  const key = JSON.stringify(o);
  useEffect(() => {
    applySeo(o);
    return resetSeo;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);
}

const PRIVATE_PREFIXES = ['/admin', '/cart', '/checkout', '/orders', '/profile', '/login', '/register', '/forgot-password', '/reset-password'];

export const isPrivatePath = (pathname: string) =>
  PRIVATE_PREFIXES.some((p) => pathname === p || pathname.startsWith(p + '/'));

/** صفحات حساب کاربری، سبد، پرداخت و ادمین نباید در گوگل ایندکس شوند. */
export function usePrivateRouteNoindex() {
  const { pathname } = useLocation();
  useEffect(() => {
    if (!isPrivatePath(pathname)) return;
    upsertMeta('name', 'robots', 'noindex, nofollow');
    return resetSeo;
  }, [pathname]);
}

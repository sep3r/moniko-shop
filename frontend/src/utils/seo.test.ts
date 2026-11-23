import { describe, expect, it } from 'vitest';
import { buildProductJsonLd as buildGraph, buildCategoryJsonLd, buildHomeJsonLd, isPrivatePath, productDescription, shorten, SITE_URL } from './seo';
import { Product } from '../types';

const base: Product = {
  id: 12, name: 'کرم مرطوب‌کننده', price: 100000, stock: 5, active: true,
  brand: 'NIVEA', imageUrl: '/api/products/12/images/3',
};
const abs = (u?: string | null) => (u ? `https://api.moniko.ir${u}` : '');
/** JSON-LD محصول حالا یک @graph است (Product + BreadcrumbList)؛ این تابع فقط گره‌ی Product را برمی‌گرداند. */
const buildProductJsonLd = (p: Product, f: typeof abs) => (buildGraph(p, f) as any)['@graph'][0];

describe('buildProductJsonLd', () => {
  it('قیمت را از تومان به ریال می‌برد و موجودی را درست می‌گذارد', () => {
    const ld = buildProductJsonLd(base, abs) as any;
    expect(ld['@type']).toBe('Product');
    expect(ld.offers.priceCurrency).toBe('IRR');
    expect(ld.offers.price).toBe('1000000');
    expect(ld.offers.availability).toBe('https://schema.org/InStock');
    expect(ld.offers.url).toBe(`${SITE_URL}/products/12`);
    expect(ld.brand.name).toBe('NIVEA');
    expect(ld.image).toEqual(['https://api.moniko.ir/api/products/12/images/3']);
  });

  it('قیمت با تخفیف معتبر را استفاده می‌کند ولی تخفیف ۰ را نادیده می‌گیرد', () => {
    expect((buildProductJsonLd({ ...base, discountPrice: 80000 }, abs) as any).offers.price).toBe('800000');
    expect((buildProductJsonLd({ ...base, discountPrice: 0 }, abs) as any).offers.price).toBe('1000000');
  });

  it('محصول ناموجود OutOfStock می‌شود', () => {
    expect((buildProductJsonLd({ ...base, stock: 0 }, abs) as any).offers.availability).toBe('https://schema.org/OutOfStock');
  });
});

describe('JSON-LD graph', () => {
  it('محصول: مسیر صفحه شامل خانه، دسته و محصول است', () => {
    const g = (buildGraph({ ...base, category: { id: 1, name: 'مراقبت پوست', slug: 'skin-care' } }, abs) as any)['@graph'];
    const crumbs = g[1].itemListElement;
    expect(g[1]['@type']).toBe('BreadcrumbList');
    expect(crumbs.map((c: any) => c.position)).toEqual([1, 2, 3]);
    expect(crumbs[1].item).toBe(`${SITE_URL}/categories/skin-care`);
    expect(crumbs[2].item).toBe(`${SITE_URL}/products/12`);
  });

  it('دسته و صفحه‌ی اصلی گره‌های درست دارند', () => {
    const cat = (buildCategoryJsonLd('مراقبت پوست', 'skin-care') as any)['@graph'];
    expect(cat[0]['@type']).toBe('CollectionPage');
    expect(cat[1].itemListElement).toHaveLength(2);
    const home = (buildHomeJsonLd() as any)['@graph'];
    expect(home.map((n: any) => n['@type'])).toEqual(['WebSite', 'Organization']);
  });
});

describe('productDescription / shorten', () => {
  it('توضیحات طولانی را کوتاه می‌کند', () => {
    const d = productDescription({ ...base, description: 'الف '.repeat(200) });
    expect(d.length).toBeLessThanOrEqual(160);
    expect(d.endsWith('…')).toBe(true);
  });

  it('بدون توضیحات، متن پیش‌فرض با نام محصول می‌سازد', () => {
    expect(productDescription(base)).toContain(base.name);
  });

  it('shorten متن کوتاه را دست نمی‌زند', () => {
    expect(shorten('سلام  دنیا', 50)).toBe('سلام دنیا');
  });
});

describe('isPrivatePath', () => {
  it('صفحات حساب، سبد و ادمین خصوصی‌اند', () => {
    ['/admin', '/admin/products', '/cart', '/checkout', '/orders/5', '/profile', '/login'].forEach((p) =>
      expect(isPrivatePath(p)).toBe(true));
  });

  it('صفحات عمومی خصوصی نیستند (و پیشوند شبیه‌به‌هم اشتباه نمی‌شود)', () => {
    ['/', '/products/1', '/categories/skin-care', '/search', '/administrator', '/cartoon'].forEach((p) =>
      expect(isPrivatePath(p)).toBe(false));
  });
});

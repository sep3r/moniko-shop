import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { productApi } from '../services/api';
import { Category, Product } from '../types';
import { useSetting, useSite } from '../context/SiteContext';
import { getProductImageUrl } from '../utils/imageUrl';
import { discountPercent } from '../utils/format';
import Carousel from '../components/Carousel';
import HeroSlider from '../components/HeroSlider';
import BannerRow from '../components/BannerRow';
import ProductCard from '../components/ProductCard';
import Countdown from '../components/Countdown';
import SmartLink from '../components/SmartLink';
import { DEFAULT_DESCRIPTION, DEFAULT_TITLE, buildHomeJsonLd, useSeo } from '../utils/seo';

/** شناسه‌ی یک دسته و همه‌ی زیرمجموعه‌هایش */
const collectIds = (c: Category): number[] => [c.id, ...(c.children || []).flatMap(collectIds)];

interface Tab { key: string; label: string; products: Product[] }

function TabbedProducts({ id, title, tabs }: { id?: string; title: string; tabs: Tab[] }) {
  const [active, setActive] = useState(0);
  const visible = tabs.filter((t) => t.products.length > 0);
  if (visible.length === 0) return null;
  const current = visible[Math.min(active, visible.length - 1)];

  return (
    <section className="kh-section kh-container" id={id}>
      <h2 className="kh-title">{title}</h2>
      <div className="kh-tabs" role="tablist">
        {visible.map((t, i) => (
          <button key={t.key} type="button" role="tab" aria-selected={t === current}
                  className={t === current ? 'active' : ''} onClick={() => setActive(i)}>
            {t.label}
          </button>
        ))}
      </div>
      <Carousel className="kh-products">
        {current.products.slice(0, 20).map((p) => <ProductCard key={p.id} product={p} />)}
      </Carousel>
    </section>
  );
}

export default function Home() {
  const { data } = useSite();
  const { hash } = useLocation();
  const [categories, setCategories] = useState<Category[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [dealOver, setDealOver] = useState(false);

  useSeo({
    title: DEFAULT_TITLE,
    description: DEFAULT_DESCRIPTION,
    path: '/',
    jsonLd: buildHomeJsonLd(),
  });

  const aboutTitle = useSetting('about_title', 'خرید لوازم آرایشی و بهداشتی از فروشگاه اینترنتی مونا بیوتی');
  const aboutText = useSetting(
    'about_text',
    'فروشگاه مونا بیوتی با عرضه‌ی محصولات اصل و اورجینال آرایشی، بهداشتی و مراقبتی پوست و مو، امکان خرید اینترنتی امن را برای شما فراهم کرده است. ' +
    'تمامی کالاها با تضمین اصالت و ارسال سریع به سراسر کشور عرضه می‌شوند.'
  );
  const [aboutOpen, setAboutOpen] = useState(false);
  const [aboutClipped, setAboutClipped] = useState(false);
  const aboutRef = useRef<HTMLParagraphElement>(null);

  // به‌جای دانلود کل کاتالوگ، فقط چند لیست کوچک از سرور گرفته می‌شود (جدیدترین، پرتخفیف، موجود، هر دسته)
  useEffect(() => {
    let cancelled = false;
    const items = (r: PromiseSettledResult<{ data: { items?: Product[] } }>) =>
      r.status === 'fulfilled' ? (r.value.data.items ?? []) : [];
    (async () => {
      try {
        const [catRes, ...lists] = await Promise.allSettled([
          productApi.getCategoryTree(),
          productApi.search({ sort: 'new', size: 20 }),
          productApi.search({ sort: 'discount', size: 20 }),
          productApi.search({ sort: 'new', inStock: true, size: 20 }),
        ]);
        const cats: Category[] = catRes.status === 'fulfilled' ? catRes.value.data : [];
        const perCategory = await Promise.allSettled(
          cats.slice(0, 6).map((c) => productApi.search({ category: c.slug, sort: 'new', size: 12 }))
        );
        if (cancelled) return;
        const byId = new Map<number, Product>();
        [...lists, ...perCategory].forEach((r) => items(r as PromiseSettledResult<{ data: { items?: Product[] } }>).forEach((p) => byId.set(p.id, p)));
        setCategories(cats);
        setProducts([...byId.values()]);
      } catch (error) {
        console.error('Failed to load home data:', error);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  // لینک‌هایی مثل /#deals بعد از بارگذاری اسکرول شوند
  useEffect(() => {
    if (!loading && hash) document.getElementById(hash.slice(1))?.scrollIntoView({ behavior: 'smooth' });
  }, [loading, hash]);

  const banners = data?.banners || [];
  const hero = banners.filter((b) => b.position === 'HERO');
  const mid1 = banners.filter((b) => b.position === 'MID1');
  const mid2 = banners.filter((b) => b.position === 'MID2');
  const deal = data?.dailyDeal;
  const showDeal = !!deal && deal.products.length > 0 && !dealOver;

  const featuredTabs: Tab[] = useMemo(() => {
    const tabs: Tab[] = [];
    if (data?.featured?.length) tabs.push({ key: 'admin', label: 'پیشنهاد مونا بیوتی', products: data.featured });
    categories.slice(0, 6).forEach((c) => {
      const ids = new Set(collectIds(c));
      tabs.push({ key: `c${c.id}`, label: c.name, products: products.filter((p) => p.category && ids.has(p.category.id)) });
    });
    return tabs;
  }, [data, categories, products]);

  const todayTabs: Tab[] = useMemo(() => {
    const newest = [...products].sort((a, b) => b.id - a.id);
    const discounted = products
      .filter((p) => p.discountPrice)
      .sort((a, b) => discountPercent(b.price, b.discountPrice) - discountPercent(a.price, a.discountPrice));
    const inStock = products.filter((p) => p.stock > 0).slice(0, 20);
    return [
      { key: 'new', label: 'جدیدترین‌ها', products: newest },
      { key: 'off', label: 'پرتخفیف‌ها', products: discounted },
      { key: 'stock', label: 'موجود در انبار', products: inStock },
    ];
  }, [products]);

  // دکمه‌ی «مشاهده بیشتر» فقط وقتی متن واقعاً بریده شده باشد لازم است
  useEffect(() => {
    const el = aboutRef.current;
    if (!el || aboutOpen) return;
    const check = () => setAboutClipped(el.scrollHeight > el.clientHeight + 2);
    check();
    window.addEventListener('resize', check);
    return () => window.removeEventListener('resize', check);
  }, [aboutText, loading, aboutOpen]);

  if (loading) return <div className="loading">در حال بارگذاری...</div>;

  return (
    <div className="kh-home">
      {/* اسلایدر اصلی؛ اگر مدیر بنری نگذاشته باشد یک بنر پیش‌فرض */}
      <section className="kh-container kh-section-tight">
        {hero.length > 0 ? (
          <HeroSlider banners={hero} />
        ) : (
          <div className="kh-hero-default">
            <h1>فروشگاه تخصصی زیبایی و سلامت</h1>
            <p>محصولات اصل از برندهای معتبر با تضمین اصالت کالا</p>
          </div>
        )}
      </section>

      {/* دایره‌های دسته‌بندی */}
      {categories.length > 0 && (
        <section className="kh-container kh-section-tight">
          <Carousel className="kh-cats">
            {categories.map((cat) => (
              <Link key={cat.id} to={`/categories/${cat.slug}`} className="kh-cat">
                <span className="kh-cat-circle">
                  {cat.imageUrl ? <img src={getProductImageUrl(cat.imageUrl)} alt="" loading="lazy" /> : cat.name.charAt(0)}
                </span>
                <span className="kh-cat-name">{cat.name}</span>
              </Link>
            ))}
          </Carousel>
        </section>
      )}

      {mid1.length > 0 && <section className="kh-container kh-section-tight"><BannerRow banners={mid1} /></section>}

      {/* حراج روزانه */}
      {showDeal && deal && (
        <section className="kh-container kh-section" id="deals">
          <div className="kh-deal">
            <div className="kh-deal-side">
              <h2>ترین حراج روزانه</h2>
              {deal.endsAt && <Countdown endsAt={deal.endsAt} onEnd={() => setDealOver(true)} />}
              <Link to="/search?q=" className="kh-deal-more">مشاهده همه ‹</Link>
            </div>
            <Carousel className="kh-deal-products">
              {deal.products.map((p) => <ProductCard key={p.id} product={p} />)}
            </Carousel>
          </div>
        </section>
      )}

      {mid2.length > 0 && <section className="kh-container kh-section-tight"><BannerRow banners={mid2} /></section>}

      <TabbedProducts id="featured" title="منتخب مونا بیوتی" tabs={featuredTabs} />
      <TabbedProducts title="امروز چی بخریم؟" tabs={todayTabs} />

      {/* برندها */}
      {data && data.brands.length > 0 && (
        <section className="kh-container kh-section" id="brands">
          <h2 className="kh-title">برندهای برتر</h2>
          <div className="kh-brands">
            {data.brands.map((b) => (
              <SmartLink key={b.id} to={b.linkUrl} className="kh-brand" title={b.name}>
                <img src={getProductImageUrl(b.imageUrl)} alt={b.name} loading="lazy" />
              </SmartLink>
            ))}
          </div>
        </section>
      )}

      {/* متن معرفی (سئو) */}
      <section className="kh-container kh-about">
        <h3>{aboutTitle}</h3>
        <p ref={aboutRef} className={aboutOpen ? 'open' : ''}>{aboutText}</p>
        {(aboutClipped || aboutOpen) && (
          <button type="button" onClick={() => setAboutOpen((v) => !v)}>{aboutOpen ? 'بستن' : 'مشاهده بیشتر'}</button>
        )}
      </section>
    </div>
  );
}

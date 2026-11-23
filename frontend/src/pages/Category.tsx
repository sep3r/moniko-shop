import { Link, useParams } from 'react-router-dom';
import ProductCard from '../components/ProductCard';
import Pager from '../components/Pager';
import { useListingParams, useProductList } from '../utils/useProductList';
import { SITE_NAME, buildCategoryJsonLd, useSeo } from '../utils/seo';

const PAGE_SIZE = 24;
const fa = (n: number) => new Intl.NumberFormat('fa-IR').format(n);

export default function Category() {
  const { slug } = useParams<{ slug: string }>();
  const { sort, inStock, page, update } = useListingParams();

  const { data, loading, error, notFound, retry } = useProductList({
    category: slug, sort: sort || undefined, inStock, page, size: PAGE_SIZE,
  });

  const name = data?.categoryName || '';
  useSeo(name ? {
    title: `${name} | خرید آنلاین ${name} | ${SITE_NAME}`,
    description: `خرید اینترنتی ${name} با بهترین قیمت و ضمانت اصالت کالا از ${SITE_NAME}.`,
    path: `/categories/${slug}`,
    jsonLd: buildCategoryJsonLd(name, slug || ''),
  } : { noindex: notFound });   // دسته‌ی ناموجود نباید به‌عنوان «soft 404» ایندکس شود

  if (loading && !data) return <div className="loading">در حال بارگذاری...</div>;

  if (notFound) {
    return (
      <div className="kh-container kh-section">
        <p>دسته‌بندی مورد نظر یافت نشد.</p>
        <Link to="/">بازگشت به صفحه اصلی</Link>
      </div>
    );
  }

  if (error) {
    return (
      <div className="kh-container kh-section">
        <p className="kh-muted">دریافت محصولات با مشکل روبه‌رو شد. دوباره تلاش کنید.</p>
        <button type="button" className="btn" onClick={retry}>تلاش دوباره</button>
      </div>
    );
  }

  const items = data?.items ?? [];

  return (
    <div className="kh-container kh-section">
      <nav className="kh-breadcrumb"><Link to="/">خانه</Link> ‹ <span>{name || 'محصولات دسته‌بندی'}</span></nav>
      <div className="kh-listing-bar">
        <h1 className="kh-title">{name ? `محصولات ${name}` : 'محصولات'}</h1>
        <div className="kh-listing-tools">
          <label className="kh-check">
            <input type="checkbox" checked={inStock} onChange={(e) => update({ inStock: e.target.checked ? '1' : null })} /> فقط کالاهای موجود
          </label>
          <select value={sort} onChange={(e) => update({ sort: e.target.value })} aria-label="مرتب‌سازی">
            <option value="">جدیدترین</option>
            <option value="discount">بیشترین تخفیف</option>
            <option value="cheap">ارزان‌ترین</option>
            <option value="expensive">گران‌ترین</option>
          </select>
        </div>
      </div>
      {data && data.totalItems > 0 && <p className="kh-muted">{fa(data.totalItems)} کالا</p>}
      {items.length === 0 ? (
        <p className="kh-muted">محصولی در این دسته‌بندی یافت نشد.</p>
      ) : (
        <div className={`kh-grid${loading ? ' kh-listing-busy' : ''}`}>
          {items.map((p) => <ProductCard key={p.id} product={p} />)}
        </div>
      )}
      <Pager page={page} totalPages={data?.totalPages ?? 0}
             onChange={(p) => { update({ page: String(p + 1) }); window.scrollTo({ top: 0, behavior: 'smooth' }); }} />
    </div>
  );
}

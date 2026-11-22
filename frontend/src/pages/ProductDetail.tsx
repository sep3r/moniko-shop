import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { productApi } from '../services/api';
import { Product } from '../types';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { getProductImageUrl } from '../utils/imageUrl';
import ProductGallery from '../components/ProductGallery';
import { SITE_NAME, SITE_URL, buildProductJsonLd, productDescription, useSeo } from '../utils/seo';

const formatPrice = (price: number) => new Intl.NumberFormat('fa-IR').format(price) + ' تومان';
const toFa = (n: number) => new Intl.NumberFormat('fa-IR').format(n);

type Tab = 'description' | 'specs';

export default function ProductDetail() {
  const { id } = useParams<{ id: string }>();
  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [added, setAdded] = useState(false);
  const [loginHint, setLoginHint] = useState(false);
  const [tab, setTab] = useState<Tab>('description');
  const tabsRef = useRef<HTMLDivElement>(null);
  const { addItem } = useCart();
  const { user } = useAuth();

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setNotFound(false);
    productApi.getById(Number(id))
      .then((res) => {
        const p: Product = res.data;
        setProduct(p);
        setTab(p.description ? 'description' : 'specs');
      })
      .catch((err) => {
        if (err.response?.status === 404) setNotFound(true);
      })
      .finally(() => setLoading(false));
  }, [id]);

  const absImage = (u?: string | null) => {
    const r = getProductImageUrl(u);
    return r ? new URL(r, SITE_URL).toString() : '';
  };
  useSeo(product ? {
    title: `${product.name}${product.category ? ' | ' + product.category.name : ''} | ${SITE_NAME}`,
    description: productDescription(product),
    image: absImage(product.imageUrl) || undefined,
    path: `/products/${product.id}`,
    type: 'product',
    jsonLd: buildProductJsonLd(product, absImage),
  } : { noindex: notFound });

  if (loading) return <div className="loading">در حال بارگذاری...</div>;

  if (notFound || !product) {
    return (
      <div className="container">
        <p>محصول مورد نظر یافت نشد.</p>
        <Link to="/">بازگشت به صفحه اصلی</Link>
      </div>
    );
  }

  const handleAdd = () => {
    if (!user) {
      setLoginHint(true);
      return;
    }
    const ok = addItem(product);
    if (ok) {
      setAdded(true);
      setTimeout(() => setAdded(false), 1500);
    }
  };

  const images = (product.images && product.images.length > 0
    ? product.images.map((i) => getProductImageUrl(i.url))
    : product.imageUrl ? [getProductImageUrl(product.imageUrl)] : []);

  const specs = product.specs ?? [];
  const hasDescription = !!product.description?.trim();
  const discountPercent = product.discountPrice && product.price > 0
    ? Math.round((1 - product.discountPrice / product.price) * 100)
    : 0;

  const showAllSpecs = () => {
    setTab('specs');
    tabsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  return (
    <div className="container">
      {product.category && (
        <nav className="pd-breadcrumb">
          <Link to="/">خانه</Link>
          <span>›</span>
          <Link to={`/categories/${product.category.slug}`}>{product.category.name}</Link>
        </nav>
      )}

      <div className="pd">
        <div className="pd-gallery">
          {images.length > 0
            ? <ProductGallery images={images} alt={product.name} />
            : <div className="gallery-empty">بدون تصویر</div>}
        </div>

        <div className="pd-info">
          {product.brand && <div className="pd-brand">برند: <b>{product.brand}</b></div>}
          <h1 className="pd-title">{product.name}</h1>
          {product.subtitle && <div className="pd-subtitle" dir="ltr">{product.subtitle}</div>}

          {specs.length > 0 && (
            <div className="pd-features">
              <h3>ویژگی‌ها</h3>
              <ul>
                {specs.slice(0, 5).map((s, i) => (
                  <li key={i}><span className="k">{s.key}:</span> <span className="v">{s.value}</span></li>
                ))}
              </ul>
              {specs.length > 5 && (
                <button type="button" className="link-btn" onClick={showAllSpecs}>
                  مشاهده همه مشخصات ({toFa(specs.length)} مورد)
                </button>
              )}
            </div>
          )}
        </div>

        <aside className="pd-buy">
          <div className="pd-buy-row">
            <span className="pd-buy-label">وضعیت</span>
            <span className={product.stock > 0 ? 'in-stock' : 'out-of-stock'}>
              {product.stock > 0 ? `موجود در انبار (${toFa(product.stock)} عدد)` : 'ناموجود'}
            </span>
          </div>
          {product.category && (
            <div className="pd-buy-row">
              <span className="pd-buy-label">دسته‌بندی</span>
              <span>{product.category.name}</span>
            </div>
          )}

          <div className="pd-price-box">
            {product.discountPrice ? (
              <>
                <div className="pd-old-row">
                  {discountPercent > 0 && <span className="pd-discount">{toFa(discountPercent)}٪</span>}
                  <span className="old-price">{formatPrice(product.price)}</span>
                </div>
                <div className="pd-price">{formatPrice(product.discountPrice)}</div>
              </>
            ) : (
              <div className="pd-price">{formatPrice(product.price)}</div>
            )}
          </div>

          {product.stock > 0 && (
            <>
              <button type="button" className="btn btn-primary pd-add" onClick={handleAdd}>
                {added ? 'به سبد خرید اضافه شد ✓' : 'افزودن به سبد خرید'}
              </button>
              {loginHint && !user && (
                <div className="error-msg" style={{ marginTop: 12 }}>
                  برای افزودن به سبد خرید ابتدا{' '}
                  <Link to="/login" style={{ color: '#B94536', fontWeight: 600 }}>وارد شوید</Link>
                  {' '}یا{' '}
                  <Link to="/register" style={{ color: '#B94536', fontWeight: 600 }}>ثبت‌نام کنید</Link>.
                </div>
              )}
            </>
          )}
        </aside>
      </div>

      {(hasDescription || specs.length > 0) && (
        <section className="pd-tabs" ref={tabsRef}>
          <div className="pd-tab-bar" role="tablist">
            {hasDescription && (
              <button type="button" role="tab" aria-selected={tab === 'description'}
                className={tab === 'description' ? 'active' : ''} onClick={() => setTab('description')}>
                معرفی محصول
              </button>
            )}
            {specs.length > 0 && (
              <button type="button" role="tab" aria-selected={tab === 'specs'}
                className={tab === 'specs' ? 'active' : ''} onClick={() => setTab('specs')}>
                مشخصات
              </button>
            )}
          </div>

          {tab === 'description' && hasDescription && (
            <div className="pd-description">{product.description}</div>
          )}

          {tab === 'specs' && specs.length > 0 && (
            <table className="pd-specs">
              <tbody>
                {specs.map((s, i) => (
                  <tr key={i}>
                    <th>{s.key}</th>
                    <td>{s.value}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      )}
    </div>
  );
}

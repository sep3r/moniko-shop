import { Link } from 'react-router-dom';
import { Product } from '../types';
import { getProductImageUrl, FALLBACK_IMAGE } from '../utils/imageUrl';
import { discountPercent, formatPrice } from '../utils/format';

export default function ProductCard({ product }: { product: Product }) {
  const pct = discountPercent(product.price, product.discountPrice);
  const soldOut = product.stock <= 0;

  return (
    <Link to={`/products/${product.id}`} className={`kh-card ${soldOut ? 'is-soldout' : ''}`}>
      <div className="kh-card-img">
        <img
          src={product.imageUrl ? getProductImageUrl(product.imageUrl) : FALLBACK_IMAGE}
          alt={product.name}
          loading="lazy"
          decoding="async"
          onError={(e) => { e.currentTarget.src = FALLBACK_IMAGE; }}
        />
        {soldOut && <span className="kh-soldout">ناموجود</span>}
      </div>
      <div className="kh-card-brand">{product.brand || '\u00a0'}</div>
      <div className="kh-card-name">{product.name}</div>
      <div className="kh-card-price">
        {product.discountPrice ? (
          <>
            <span className="kh-badge">{formatPrice(pct)}٪</span>
            <span className="kh-prices">
              <span className="kh-old">{formatPrice(product.price)}</span>
              <span className="kh-now">{formatPrice(product.discountPrice)} <small>تومان</small></span>
            </span>
          </>
        ) : (
          <span className="kh-prices">
            <span className="kh-old" />
            <span className="kh-now">{formatPrice(product.price)} <small>تومان</small></span>
          </span>
        )}
      </div>
    </Link>
  );
}

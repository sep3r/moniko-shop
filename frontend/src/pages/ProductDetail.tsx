import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { productApi } from '../services/api';
import { Product } from '../types';
import { useCart } from '../context/CartContext';

const formatPrice = (price: number) => new Intl.NumberFormat('fa-IR').format(price) + ' تومان';

export default function ProductDetail() {
  const { id } = useParams<{ id: string }>();
  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [added, setAdded] = useState(false);
  const { addItem } = useCart();

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setNotFound(false);
    productApi.getById(Number(id))
      .then((res) => setProduct(res.data))
      .catch((err) => {
        if (err.response?.status === 404) setNotFound(true);
      })
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <div className="loading">در حال بارگذاری...</div>;

  if (notFound || !product) {
    return (
      <div className="container">
        <p>محصول مورد نظر یافت نشد.</p>
        <Link to="/">بازگشت به صفحه اصلی</Link>
      </div>
    );
  }

  return (
    <div className="container">
      <div className="product-detail">
        <img src={product.imageUrl || 'https://via.placeholder.com/400'} alt={product.name} />
        <div>
          <div className="product-brand">{product.brand}</div>
          <h1>{product.name}</h1>
          <p>{product.description}</p>
          <div className="product-price">
            {product.discountPrice ? (
              <>
                <span className="price">{formatPrice(product.discountPrice)}</span>
                <span className="old-price">{formatPrice(product.price)}</span>
              </>
            ) : (
              <span className="price">{formatPrice(product.price)}</span>
            )}
          </div>
          <p>{product.stock > 0 ? `موجود در انبار (${product.stock} عدد)` : 'ناموجود'}</p>
          {product.stock > 0 && (
            <button
              className="btn btn-primary"
              style={{ width: 'auto', padding: '12px 32px' }}
              onClick={() => {
                addItem(product);
                setAdded(true);
                setTimeout(() => setAdded(false), 1500);
              }}
            >
              {added ? 'به سبد خرید اضافه شد ✓' : 'افزودن به سبد خرید'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

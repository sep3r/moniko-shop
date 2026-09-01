import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { productApi } from '../services/api';
import { Product } from '../types';

const formatPrice = (price: number) => new Intl.NumberFormat('fa-IR').format(price) + ' تومان';

export default function Category() {
  const { slug } = useParams<{ slug: string }>();
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (!slug) return;
    setLoading(true);
    setNotFound(false);
    productApi.getByCategory(slug)
      .then((res) => setProducts(res.data))
      .catch((err) => {
        if (err.response?.status === 404) setNotFound(true);
      })
      .finally(() => setLoading(false));
  }, [slug]);

  if (loading) return <div className="loading">در حال بارگذاری...</div>;

  if (notFound) {
    return (
      <div className="container">
        <p>دسته‌بندی مورد نظر یافت نشد.</p>
        <Link to="/">بازگشت به صفحه اصلی</Link>
      </div>
    );
  }

  return (
    <div className="container">
      <h2 className="section-title">محصولات</h2>
      {products.length === 0 ? (
        <p>محصولی در این دسته‌بندی یافت نشد.</p>
      ) : (
        <div className="products-grid">
          {products.map((product) => (
            <Link to={`/products/${product.id}`} key={product.id} className="product-card">
              <img src={product.imageUrl || 'https://via.placeholder.com/300'} alt={product.name} />
              <div className="product-info">
                <div className="product-brand">{product.brand}</div>
                <div className="product-name">{product.name}</div>
                <div className="product-price">
                  <span className="price">{formatPrice(product.discountPrice ?? product.price)}</span>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

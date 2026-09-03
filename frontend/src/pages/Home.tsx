import {useEffect, useState} from 'react';
import {Link} from 'react-router-dom';
import {productApi} from '../services/api';
import {Category, Product} from '../types';
import {getProductImageUrl, FALLBACK_IMAGE} from '../utils/imageUrl';

const formatPrice = (price: number) => {
    return new Intl.NumberFormat('fa-IR').format(price) + ' تومان';
};

export default function Home() {
    const [categories, setCategories] = useState<Category[]>([]);
    const [products, setProducts] = useState<Product[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        Promise.all([
            productApi.getCategoryTree(),  // فقط دسته‌های اصلی
            productApi.getAll(),
        ])
            .then(([catRes, prodRes]) => {
                setCategories(catRes.data);
                setProducts(prodRes.data);
            })
            .catch((error) => {
                console.error('Failed to load home data:', error);
            })
            .finally(() => {
                setLoading(false);
            });
    }, []);

    if (loading) {
        return <div className="loading">در حال بارگذاری...</div>;
    }

    return (
        <>
            <section className="hero">
                <div className="container">
                    <h1>فروشگاه تخصصی زیبایی و سلامت</h1>
                    <p>بیش از ۵۰ هزار محصول از برندهای معتبر با تضمین اصالت</p>
                </div>
            </section>

            <div className="container">

                {/* Categories */}
                <h2 className="section-title">دسته‌بندی‌ها</h2>

                <div className="categories">
                    {categories.map((cat: Category) => (
                        <Link
                            to={`/categories/${cat.slug}`}
                            key={cat.id}
                            className="category-card"
                        >
                            <img
                                src={cat.imageUrl || 'https://via.placeholder.com/80'}
                                alt={cat.name}
                                onError={(event) => {
                                    event.currentTarget.src =
                                        'https://via.placeholder.com/80';
                                }}
                            />

                            <h3>{cat.name}</h3>
                        </Link>
                    ))}
                </div>

                {/* Products */}
                <h2 className="section-title">محصولات منتخب</h2>

                <div className="products-grid">
                    {products.map((product: Product) => (
                        <Link
                            to={`/products/${product.id}`}
                            key={product.id}
                            className="product-card"
                        >
                            <img
                                src={
                                    product.imageUrl
                                        ? getProductImageUrl(product.imageUrl)
                                        : FALLBACK_IMAGE
                                }
                                alt={product.name}
                                onError={(event) => {
                                    event.currentTarget.src = FALLBACK_IMAGE;
                                }}
                            />

                            <div className="product-info">

                                <div className="product-brand">
                                    {product.brand}
                                </div>

                                <div className="product-name">
                                    {product.name}
                                </div>

                                <div className="product-price">

                                    {product.discountPrice ? (
                                        <>
                      <span className="price">
                        {formatPrice(product.discountPrice)}
                      </span>

                                            <span className="old-price">
                        {formatPrice(product.price)}
                      </span>

                                            <span className="discount-badge">
                        {Math.round(
                            (1 - product.discountPrice / product.price) * 100
                        )}
                                                ٪
                      </span>
                                        </>
                                    ) : (
                                        <span className="price">
                      {formatPrice(product.price)}
                    </span>
                                    )}

                                </div>

                            </div>
                        </Link>
                    ))}
                </div>

            </div>
        </>
    );
}
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { orderApi } from '../services/api';
import { Order } from '../types';

const formatPrice = (price: number) => new Intl.NumberFormat('fa-IR').format(price) + ' تومان';

const statusLabel: Record<string, string> = {
  PENDING: 'در انتظار بررسی',
  CONFIRMED: 'تایید شده',
  SHIPPED: 'ارسال شده',
  DELIVERED: 'تحویل داده شده',
  CANCELLED: 'لغو شده',
};

export default function OrderDetail() {
  const { id } = useParams<{ id: string }>();
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (!id) return;
    orderApi.getById(Number(id))
      .then((res) => setOrder(res.data))
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <div className="loading">در حال بارگذاری...</div>;
  if (notFound || !order) {
    return (
      <div className="container">
        <p>سفارش یافت نشد.</p>
        <Link to="/orders">بازگشت به سفارش‌ها</Link>
      </div>
    );
  }

  return (
    <div className="container">
      <h2 className="section-title">سفارش #{order.id}</h2>
      <p>وضعیت: <span className={`status-badge status-${order.status}`}>{statusLabel[order.status]}</span></p>
      <p>آدرس ارسال: {order.shippingAddress}</p>
      <p>شماره تماس: {order.phone}</p>
      <div className="cart-list">
        {order.items.map((item) => (
          <div className="cart-item" key={item.productId}>
            <div className="cart-item-info">
              <div className="product-name">{item.productName}</div>
              <div className="price">{formatPrice(item.unitPrice)} × {item.quantity}</div>
            </div>
            <div className="cart-item-subtotal">{formatPrice(item.subtotal)}</div>
          </div>
        ))}
      </div>
      <div className="cart-summary">
        <span>جمع کل:</span>
        <strong>{formatPrice(order.totalAmount)}</strong>
      </div>
      <Link to="/orders">بازگشت به سفارش‌ها</Link>
    </div>
  );
}

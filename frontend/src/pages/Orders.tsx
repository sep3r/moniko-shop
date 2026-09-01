import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
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

export default function Orders() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    orderApi.getMine().then((res) => setOrders(res.data)).finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="loading">در حال بارگذاری...</div>;

  return (
    <div className="container">
      <h2 className="section-title">سفارش‌های من</h2>
      {orders.length === 0 ? (
        <p>هنوز سفارشی ثبت نکرده‌اید. <Link to="/">مشاهده محصولات</Link></p>
      ) : (
        <div className="order-list">
          {orders.map((order) => (
            <Link to={`/orders/${order.id}`} key={order.id} className="order-row">
              <span>سفارش #{order.id}</span>
              <span>{new Date(order.createdAt).toLocaleDateString('fa-IR')}</span>
              <span className={`status-badge status-${order.status}`}>{statusLabel[order.status]}</span>
              <strong>{formatPrice(order.totalAmount)}</strong>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

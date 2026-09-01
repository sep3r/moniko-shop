import { useEffect, useState } from 'react';
import { adminApi } from '../../services/api';
import { Order, OrderStatus } from '../../types';

const formatPrice = (price: number) => new Intl.NumberFormat('fa-IR').format(price) + ' تومان';

const statusLabel: Record<string, string> = {
  PENDING: 'در انتظار بررسی',
  CONFIRMED: 'تایید شده',
  SHIPPED: 'ارسال شده',
  DELIVERED: 'تحویل داده شده',
  CANCELLED: 'لغو شده',
};

const statusOptions: OrderStatus[] = ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

export default function AdminOrders() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = () => {
    setLoading(true);
    adminApi.getOrders().then((res) => setOrders(res.data)).catch(() => setError('خطا در دریافت سفارش‌ها')).finally(() => setLoading(false));
  };

  useEffect(load, []);

  const changeStatus = async (id: number, status: string) => {
    try {
      await adminApi.updateOrderStatus(id, status);
      load();
    } catch {
      setError('به‌روزرسانی وضعیت با خطا مواجه شد');
    }
  };

  if (loading) return <div className="loading">در حال بارگذاری...</div>;

  return (
    <div>
      <h2 className="section-title">سفارش‌ها</h2>
      {error && <div className="error-msg">{error}</div>}
      <table className="admin-table">
        <thead><tr><th>شماره</th><th>مشتری</th><th>تاریخ</th><th>مبلغ</th><th>وضعیت</th></tr></thead>
        <tbody>
          {orders.map((o) => (
            <tr key={o.id}>
              <td>#{o.id}</td>
              <td>{o.customerName}<br /><small>{o.customerEmail}</small></td>
              <td>{new Date(o.createdAt).toLocaleDateString('fa-IR')}</td>
              <td>{formatPrice(o.totalAmount)}</td>
              <td>
                <select value={o.status} onChange={(e) => changeStatus(o.id, e.target.value)}>
                  {statusOptions.map((s) => <option key={s} value={s}>{statusLabel[s]}</option>)}
                </select>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

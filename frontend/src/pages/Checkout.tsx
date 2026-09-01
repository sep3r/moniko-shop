import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { orderApi } from '../services/api';

const formatPrice = (price: number) => new Intl.NumberFormat('fa-IR').format(price) + ' تومان';

export default function Checkout() {
  const { items, totalPrice, clearCart } = useCart();
  const navigate = useNavigate();
  const [shippingAddress, setShippingAddress] = useState('');
  const [phone, setPhone] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  if (items.length === 0) {
    return (
      <div className="container">
        <p>سبد خرید شما خالی است.</p>
        <Link to="/">بازگشت به صفحه اصلی</Link>
      </div>
    );
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await orderApi.create({
        items: items.map((i) => ({ productId: i.productId, quantity: i.quantity })),
        shippingAddress,
        phone,
      });
      clearCart();
      navigate(`/orders/${res.data.id}`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'ثبت سفارش با خطا مواجه شد');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container">
      <div className="auth-page" style={{ minHeight: 'auto', padding: '32px 0' }}>
        <div className="auth-card" style={{ maxWidth: 480 }}>
          <h2>تکمیل خرید</h2>
          {error && <div className="error-msg">{error}</div>}
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>آدرس ارسال</label>
              <input value={shippingAddress} onChange={(e) => setShippingAddress(e.target.value)} required />
            </div>
            <div className="form-group">
              <label>شماره تماس</label>
              <input value={phone} onChange={(e) => setPhone(e.target.value)} required />
            </div>
            <div className="cart-summary" style={{ marginBottom: 16 }}>
              <span>جمع کل:</span>
              <strong>{formatPrice(totalPrice)}</strong>
            </div>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'در حال ثبت سفارش...' : 'ثبت نهایی سفارش'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

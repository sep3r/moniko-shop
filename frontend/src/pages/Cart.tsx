import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';

const formatPrice = (price: number) => new Intl.NumberFormat('fa-IR').format(price) + ' تومان';

export default function Cart() {
  const { items, updateQuantity, removeItem, totalPrice } = useCart();
  const navigate = useNavigate();

  if (items.length === 0) {
    return (
      <div className="container">
        <h2 className="section-title">سبد خرید</h2>
        <p>سبد خرید شما خالی است.</p>
        <Link to="/">بازگشت به صفحه اصلی</Link>
      </div>
    );
  }

  return (
    <div className="container">
      <h2 className="section-title">سبد خرید</h2>
      <div className="cart-list">
        {items.map((item) => (
          <div className="cart-item" key={item.productId}>
            <img src={item.imageUrl || 'https://via.placeholder.com/80'} alt={item.name} />
            <div className="cart-item-info">
              <div className="product-name">{item.name}</div>
              <div className="price">{formatPrice(item.price)}</div>
            </div>
            <div className="cart-qty">
              <button onClick={() => updateQuantity(item.productId, item.quantity - 1)}>-</button>
              <span>{item.quantity}</span>
              <button onClick={() => updateQuantity(item.productId, item.quantity + 1)} disabled={item.quantity >= item.stock}>+</button>
            </div>
            <div className="cart-item-subtotal">{formatPrice(item.price * item.quantity)}</div>
            <button className="cart-remove" onClick={() => removeItem(item.productId)}>حذف</button>
          </div>
        ))}
      </div>
      <div className="cart-summary">
        <span>جمع کل:</span>
        <strong>{formatPrice(totalPrice)}</strong>
      </div>
      <button className="btn btn-primary" style={{ width: 'auto', padding: '12px 32px' }} onClick={() => navigate('/checkout')}>
        ادامه فرآیند خرید
      </button>
    </div>
  );
}

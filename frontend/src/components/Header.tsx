import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';

export default function Header() {
  const { user, logout } = useAuth();
  const { totalItems } = useCart();

  return (
    <header className="header">
      <div className="header-top">
        ارسال رایگان برای سفارش‌های بالای ۵۰۰ هزار تومان
      </div>
      <div className="container">
        <div className="header-main">
          <Link to="/" className="logo">مونیکو شاپ</Link>
          <div className="search-box">
            <input type="text" placeholder="جستجوی محصولات، برندها و ..." />
          </div>
          <div className="header-actions">
            {user ? (
              <>
                <Link to="/profile">سلام، {user.fullName}</Link>
                <Link to="/orders">سفارش‌های من</Link>
                {user.roles.includes('ROLE_ADMIN') && <Link to="/admin">پنل مدیریت</Link>}
                <button onClick={logout}>خروج</button>
              </>
            ) : (
              <>
                <Link to="/login">ورود</Link>
                <Link to="/register">ثبت‌نام</Link>
              </>
            )}
            <Link to="/cart">سبد خرید{totalItems > 0 ? ` (${totalItems})` : ''}</Link>
          </div>
        </div>
        <nav className="nav">
          <ul>
            <li><Link to="/">صفحه اصلی</Link></li>
            <li><Link to="/categories/makeup">آرایشی</Link></li>
            <li><Link to="/categories/skincare">مراقبت پوست</Link></li>
            <li><Link to="/categories/hair">مراقبت مو</Link></li>
            <li><Link to="/categories/fragrance">عطر</Link></li>
            <li><Link to="/categories/health">سلامت</Link></li>
          </ul>
        </nav>
      </div>
    </header>
  );
}

import { useEffect, useState, useRef } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { productApi } from '../services/api';
import { Category } from '../types';

export default function Header() {
  const { user, logout } = useAuth();
  const { totalItems } = useCart();
  const [categories, setCategories] = useState<Category[]>([]);
  const [loadingCats, setLoadingCats] = useState(true);
  const [openMegaId, setOpenMegaId] = useState<number | null>(null);
  const [mobileOpen, setMobileOpen] = useState(false);
  const closeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    productApi
      .getCategoryTree()
      .then((res) => {
        const sorted = [...res.data].sort(
          (a: Category, b: Category) => (a.sortOrder ?? 999) - (b.sortOrder ?? 999)
        );
        setCategories(sorted);
      })
      .catch((err) => console.error('خطا در دریافت دسته‌بندی‌ها:', err))
      .finally(() => setLoadingCats(false));
  }, []);

  const openMega = (id: number) => {
    if (closeTimer.current) clearTimeout(closeTimer.current);
    setOpenMegaId(id);
  };

  const scheduleClose = () => {
    closeTimer.current = setTimeout(() => setOpenMegaId(null), 150);
  };

  const cancelClose = () => {
    if (closeTimer.current) clearTimeout(closeTimer.current);
  };

  return (
    <header className="header">
      <div className="header-top">
        ارسال رایگان برای سفارش‌های بالای ۵۰۰ هزار تومان
      </div>
      <div className="container">
        <div className="header-main">
          <Link to="/" className="logo">
            مونیکو شاپ
          </Link>
          <div className="search-box">
            <input type="text" placeholder="جستجوی محصولات، برندها و ..." />
          </div>
          <div className="header-actions">
            {user ? (
              <>
                <Link to="/profile">سلام، {user.fullName}</Link>
                <Link to="/orders">سفارش‌های من</Link>
                {user.roles.includes('ROLE_ADMIN') && (
                  <Link to="/admin">پنل مدیریت</Link>
                )}
                <button onClick={logout}>خروج</button>
              </>
            ) : (
              <>
                <Link to="/login">ورود</Link>
                <Link to="/register">ثبت‌نام</Link>
              </>
            )}
            <Link to="/cart" className="cart-link">
              سبد خرید{totalItems > 0 ? ` (${totalItems})` : ''}
            </Link>
            <button
              className="mobile-menu-btn"
              onClick={() => setMobileOpen(!mobileOpen)}
              aria-label="منو"
            >
              ☰
            </button>
          </div>
        </div>

        {/* ===== Desktop Nav + Mega Menu ===== */}
        <nav className={`nav desktop-nav ${mobileOpen ? 'mobile-open' : ''}`}>
          <ul>
            <li>
              <Link to="/" onClick={() => setMobileOpen(false)}>
                صفحه اصلی
              </Link>
            </li>

            {loadingCats ? (
              <li>
                <span className="nav-loading">در حال بارگذاری...</span>
              </li>
            ) : (
              categories.map((cat) => {
                const hasChildren = cat.children && cat.children.length > 0;
                return (
                  <li
                    key={cat.id}
                    className={`nav-item ${hasChildren ? 'has-mega' : ''} ${
                      openMegaId === cat.id ? 'is-open' : ''
                    }`}
                    onMouseEnter={() => hasChildren && openMega(cat.id)}
                    onMouseLeave={() => hasChildren && scheduleClose()}
                  >
                    <Link
                      to={`/categories/${cat.slug}`}
                      onClick={() => setMobileOpen(false)}
                    >
                      {cat.name}
                      {hasChildren && <span className="mega-arrow">▾</span>}
                    </Link>

                    {/* Mega Panel */}
                    {hasChildren && openMegaId === cat.id && (
                      <div
                        className="mega-panel"
                        onMouseEnter={cancelClose}
                        onMouseLeave={scheduleClose}
                      >
                        <div className="mega-columns">
                          {cat.children!
                            .slice()
                            .sort(
                              (a, b) =>
                                (a.sortOrder ?? 999) - (b.sortOrder ?? 999)
                            )
                            .map((child) => (
                              <div className="mega-column" key={child.id}>
                                <Link
                                  to={`/categories/${child.slug}`}
                                  className="mega-column-title"
                                  onClick={() => {
                                    setOpenMegaId(null);
                                    setMobileOpen(false);
                                  }}
                                >
                                  {child.name}
                                </Link>
                                {/* سطح سوم اگر وجود داشته باشد */}
                                {child.children && child.children.length > 0 && (
                                  <ul className="mega-sublist">
                                    {child.children.map((sub) => (
                                      <li key={sub.id}>
                                        <Link
                                          to={`/categories/${sub.slug}`}
                                          onClick={() => {
                                            setOpenMegaId(null);
                                            setMobileOpen(false);
                                          }}
                                        >
                                          {sub.name}
                                        </Link>
                                      </li>
                                    ))}
                                  </ul>
                                )}
                              </div>
                            ))}
                        </div>
                        <div className="mega-footer">
                          <Link
                            to={`/categories/${cat.slug}`}
                            onClick={() => {
                              setOpenMegaId(null);
                              setMobileOpen(false);
                            }}
                          >
                            مشاهده همه محصولات {cat.name} ←
                          </Link>
                        </div>
                      </div>
                    )}

                    {/* Mobile accordion children */}
                    {hasChildren && mobileOpen && (
                      <ul className="mobile-sub">
                        {cat.children!.map((child) => (
                          <li key={child.id}>
                            <Link
                              to={`/categories/${child.slug}`}
                              onClick={() => setMobileOpen(false)}
                            >
                              {child.name}
                            </Link>
                          </li>
                        ))}
                      </ul>
                    )}
                  </li>
                );
              })
            )}
          </ul>
        </nav>
      </div>
    </header>
  );
}

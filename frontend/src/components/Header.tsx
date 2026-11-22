import { FormEvent, useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useSetting } from '../context/SiteContext';
import { productApi } from '../services/api';
import { Category } from '../types';

const sortCats = (list: Category[]) => [...list].sort((a, b) => (a.sortOrder ?? 999) - (b.sortOrder ?? 999));

export default function Header() {
  const { user, logout } = useAuth();
  const { totalItems } = useCart();
  const navigate = useNavigate();
  const location = useLocation();
  const topbar = useSetting('topbar_text', 'ارسال رایگان برای سفارش‌های بالای ۵۰۰ هزار تومان');

  const [categories, setCategories] = useState<Category[]>([]);
  const [megaOpen, setMegaOpen] = useState(false);
  const [activeCat, setActiveCat] = useState<number | null>(null);
  const [drawer, setDrawer] = useState(false);
  const [drawerCat, setDrawerCat] = useState<number | null>(null);
  const [userMenu, setUserMenu] = useState(false);
  const [q, setQ] = useState('');
  const closeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    productApi.getCategoryTree()
      .then((res) => {
        const sorted = sortCats(res.data);
        setCategories(sorted);
        if (sorted.length) setActiveCat(sorted[0].id);
      })
      .catch((err) => console.error('خطا در دریافت دسته‌بندی‌ها:', err));
  }, []);

  // کادر جستجو با عبارت جستجوشده در آدرس هم‌گام باشد (و بیرون از صفحه‌ی جستجو خالی شود)
  useEffect(() => {
    setQ(location.pathname === '/search' ? new URLSearchParams(location.search).get('q') || '' : '');
  }, [location.pathname, location.search]);

  // با تغییر صفحه همه منوها بسته شوند
  useEffect(() => {
    setMegaOpen(false); setDrawer(false); setUserMenu(false);
  }, [location.pathname]);

  const openMega = () => { if (closeTimer.current) clearTimeout(closeTimer.current); setMegaOpen(true); };
  const closeMega = () => { closeTimer.current = setTimeout(() => setMegaOpen(false), 150); };

  const onSearch = (e: FormEvent) => {
    e.preventDefault();
    const v = q.trim();
    navigate(v ? `/search?q=${encodeURIComponent(v)}` : '/search');
  };

  const current = categories.find((c) => c.id === activeCat) || categories[0];
  const isAdmin = !!user?.roles.includes('ROLE_ADMIN');

  return (
    <header className="kh-header">
      {topbar && <div className="kh-topbar">{topbar}</div>}

      <div className="kh-container kh-header-main">
        <button type="button" className="kh-burger" aria-label="منو" onClick={() => setDrawer(true)}>☰</button>

        <Link to="/" className="kh-logo" aria-label="مونا بیوتی">
          <span className="kh-logo-name">مونا بیوتی</span>
          <span className="kh-logo-sub">BEAUTY · CARE · CONFIDENCE</span>
        </Link>

        <form className="kh-search" onSubmit={onSearch} role="search">
          <input
            type="search"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="جستجو در کالاها، دسته‌بندی‌ها و برندها"
            aria-label="جستجو"
          />
          <button type="submit" aria-label="جستجو">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><circle cx="11" cy="11" r="7" /><path d="M20 20l-3.5-3.5" /></svg>
          </button>
        </form>

        <div className="kh-actions">
          {user ? (
            <div className="kh-user" onMouseLeave={() => setUserMenu(false)}>
              <button type="button" className="kh-login" onClick={() => setUserMenu((v) => !v)}>
                {user.fullName.split(' ')[0]} <span aria-hidden>▾</span>
              </button>
              {userMenu && (
                <div className="kh-user-menu">
                  <Link to="/profile">پروفایل من</Link>
                  <Link to="/orders">سفارش‌های من</Link>
                  {isAdmin && <Link to="/admin">پنل مدیریت</Link>}
                  <button type="button" onClick={logout}>خروج از حساب</button>
                </div>
              )}
            </div>
          ) : (
            <Link to="/login" className="kh-login">ورود | ثبت‌نام</Link>
          )}
          <Link to="/cart" className="kh-cart" aria-label="سبد خرید">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M3 4h2l2.2 11h10.6L20 7H6.2" /><circle cx="9" cy="19.5" r="1.3" /><circle cx="17" cy="19.5" r="1.3" /></svg>
            {totalItems > 0 && <span className="kh-cart-badge">{new Intl.NumberFormat('fa-IR').format(totalItems)}</span>}
          </Link>
        </div>
      </div>

      {/* نوار تیره + مگامنو (دسکتاپ) */}
      <nav className="kh-nav" onMouseLeave={closeMega}>
        <div className="kh-container kh-nav-inner">
          <button type="button" className={`kh-nav-cats ${megaOpen ? 'open' : ''}`}
                  onMouseEnter={openMega} onClick={() => setMegaOpen((v) => !v)}>
            دسته‌بندی محصولات <span aria-hidden>▾</span>
          </button>
          <Link to="/#deals">پیشنهاد ویژه</Link>
          <Link to="/#featured">منتخب مونا بیوتی</Link>
          <Link to="/#brands">برندها</Link>
        </div>

        {megaOpen && current && (
          <div className="kh-mega" onMouseEnter={openMega}>
            <div className="kh-container kh-mega-inner">
              <ul className="kh-mega-side">
                {categories.map((c) => (
                  <li key={c.id} className={c.id === current.id ? 'active' : ''} onMouseEnter={() => setActiveCat(c.id)}>
                    <Link to={`/categories/${c.slug}`}>{c.name}</Link>
                  </li>
                ))}
              </ul>
              <div className="kh-mega-main">
                <Link to={`/categories/${current.slug}`} className="kh-mega-all">مشاهده همه‌ی {current.name} ‹</Link>
                <div className="kh-mega-cols">
                  {sortCats(current.children || []).map((child) => (
                    <div key={child.id} className="kh-mega-col">
                      <Link to={`/categories/${child.slug}`} className="kh-mega-title">{child.name}</Link>
                      {sortCats(child.children || []).map((sub) => (
                        <Link key={sub.id} to={`/categories/${sub.slug}`}>{sub.name}</Link>
                      ))}
                    </div>
                  ))}
                  {(current.children || []).length === 0 && <p className="kh-muted">زیر‌دسته‌ای ثبت نشده است.</p>}
                </div>
              </div>
            </div>
          </div>
        )}
      </nav>

      {/* منوی کشویی موبایل */}
      {drawer && <div className="kh-overlay" onClick={() => setDrawer(false)} />}
      <aside className={`kh-drawer ${drawer ? 'open' : ''}`} aria-hidden={!drawer}>
        <div className="kh-drawer-head">
          <strong>دسته‌بندی محصولات</strong>
          <button type="button" aria-label="بستن" onClick={() => setDrawer(false)}>✕</button>
        </div>
        <ul>
          {categories.map((c) => (
            <li key={c.id}>
              <div className="kh-drawer-row">
                <Link to={`/categories/${c.slug}`}>{c.name}</Link>
                {(c.children || []).length > 0 && (
                  <button type="button" aria-label="زیرمجموعه‌ها" onClick={() => setDrawerCat(drawerCat === c.id ? null : c.id)}>
                    {drawerCat === c.id ? '−' : '+'}
                  </button>
                )}
              </div>
              {drawerCat === c.id && (
                <ul className="kh-drawer-sub">
                  {sortCats(c.children || []).map((ch) => (
                    <li key={ch.id}><Link to={`/categories/${ch.slug}`}>{ch.name}</Link></li>
                  ))}
                </ul>
              )}
            </li>
          ))}
        </ul>
        <div className="kh-drawer-links">
          {isAdmin && <Link to="/admin">پنل مدیریت</Link>}
          <Link to="/orders">سفارش‌های من</Link>
        </div>
      </aside>

      {/* نوار پایین موبایل */}
      <nav className="kh-bottom-nav" aria-label="ناوبری موبایل">
        <Link to="/" className={location.pathname === '/' ? 'active' : ''}>خانه</Link>
        <button type="button" onClick={() => setDrawer(true)}>دسته‌بندی</button>
        <Link to="/cart" className={location.pathname === '/cart' ? 'active' : ''}>
          سبد خرید{totalItems > 0 && <span className="kh-cart-badge">{new Intl.NumberFormat('fa-IR').format(totalItems)}</span>}
        </Link>
        <Link to={user ? '/profile' : '/login'} className={['/profile', '/login'].includes(location.pathname) ? 'active' : ''}>
          {user ? 'حساب من' : 'ورود'}
        </Link>
      </nav>
    </header>
  );
}

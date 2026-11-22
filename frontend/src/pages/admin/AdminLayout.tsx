import { NavLink, Outlet } from 'react-router-dom';

export default function AdminLayout() {
  return (
    <div className="container admin-layout">
      <aside className="admin-sidebar">
        <h3>پنل مدیریت</h3>
        <nav>
          <NavLink to="/admin/products" className={({ isActive }) => (isActive ? 'active' : '')}>محصولات</NavLink>
          <NavLink to="/admin/categories" className={({ isActive }) => (isActive ? 'active' : '')}>دسته‌بندی‌ها</NavLink>
          <NavLink to="/admin/orders" className={({ isActive }) => (isActive ? 'active' : '')}>سفارش‌ها</NavLink>
          <NavLink to="/admin/users" className={({ isActive }) => (isActive ? 'active' : '')}>کاربران</NavLink>
          <NavLink to="/admin/visits" className={({ isActive }) => (isActive ? 'active' : '')}>آمار بازدید</NavLink>
          <NavLink to="/admin/logs" className={({ isActive }) => (isActive ? 'active' : '')}>لاگ و IP ها</NavLink>
          <div className="admin-nav-sep">ظاهر سایت</div>
          <NavLink to="/admin/banners" className={({ isActive }) => (isActive ? 'active' : '')}>بنرها و اسلایدر</NavLink>
          <NavLink to="/admin/home-products" className={({ isActive }) => (isActive ? 'active' : '')}>حراج روزانه و منتخب</NavLink>
          <NavLink to="/admin/brands" className={({ isActive }) => (isActive ? 'active' : '')}>برندهای برتر</NavLink>
          <NavLink to="/admin/settings" className={({ isActive }) => (isActive ? 'active' : '')}>تنظیمات سایت</NavLink>
        </nav>
      </aside>
      <div className="admin-content">
        <Outlet />
      </div>
    </div>
  );
}

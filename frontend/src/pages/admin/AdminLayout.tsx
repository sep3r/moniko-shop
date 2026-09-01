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
        </nav>
      </aside>
      <div className="admin-content">
        <Outlet />
      </div>
    </div>
  );
}

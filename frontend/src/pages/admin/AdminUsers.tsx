import { useEffect, useState } from 'react';
import { adminApi } from '../../services/api';
import { AdminUser } from '../../types';

export default function AdminUsers() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = () => {
    setLoading(true);
    adminApi.getUsers().then((res) => setUsers(res.data)).catch(() => setError('خطا در دریافت کاربران')).finally(() => setLoading(false));
  };

  useEffect(load, []);

  const toggleEnabled = async (u: AdminUser) => {
    try {
      await adminApi.updateUserStatus(u.id, !u.enabled);
      load();
    } catch {
      setError('به‌روزرسانی کاربر با خطا مواجه شد');
    }
  };

  const toggleAdmin = async (u: AdminUser) => {
    const isAdmin = u.roles.includes('ROLE_ADMIN');
    const newRoles = isAdmin ? ['ROLE_USER'] : ['ROLE_USER', 'ROLE_ADMIN'];
    try {
      await adminApi.updateUserRoles(u.id, newRoles);
      load();
    } catch {
      setError('به‌روزرسانی نقش کاربر با خطا مواجه شد');
    }
  };

  if (loading) return <div className="loading">در حال بارگذاری...</div>;

  return (
    <div>
      <h2 className="section-title">کاربران</h2>
      {error && <div className="error-msg">{error}</div>}
      <table className="admin-table">
        <thead><tr><th>نام</th><th>ایمیل</th><th>نقش</th><th>وضعیت</th><th></th></tr></thead>
        <tbody>
          {users.map((u) => (
            <tr key={u.id}>
              <td>{u.fullName}</td>
              <td>{u.email}</td>
              <td>{u.roles.includes('ROLE_ADMIN') ? 'مدیر' : 'کاربر'}</td>
              <td>{u.enabled ? 'فعال' : 'غیرفعال'}</td>
              <td>
                <button className="link-btn" onClick={() => toggleAdmin(u)}>
                  {u.roles.includes('ROLE_ADMIN') ? 'حذف دسترسی مدیر' : 'ارتقا به مدیر'}
                </button>
                <button className="link-btn danger" onClick={() => toggleEnabled(u)}>
                  {u.enabled ? 'غیرفعال کردن' : 'فعال کردن'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

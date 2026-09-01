import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Profile() {
  const { user } = useAuth();
  if (!user) return null;

  return (
    <div className="container">
      <h2 className="section-title">پنل کاربری</h2>
      <div className="auth-card" style={{ maxWidth: 480, margin: 0 }}>
        <p><strong>نام:</strong> {user.fullName}</p>
        <p><strong>ایمیل:</strong> {user.email}</p>
        {user.phone && <p><strong>تلفن:</strong> {user.phone}</p>}
        <p><strong>نقش:</strong> {user.roles.includes('ROLE_ADMIN') ? 'مدیر' : 'کاربر'}</p>
      </div>
      <p style={{ marginTop: 20 }}><Link to="/orders">مشاهده سفارش‌های من</Link></p>
    </div>
  );
}

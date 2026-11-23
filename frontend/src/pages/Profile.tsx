import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ChangePasswordForm from '../components/ChangePasswordForm';

export default function Profile() {
  const { user } = useAuth();
  if (!user) return null;

  return (
    <div className="container">
      <h2 className="section-title">پنل کاربری</h2>
      <div className="auth-card" style={{ maxWidth: 480, margin: 0 }}>
        <p><strong>نام:</strong> {user.fullName}</p>
        {user.phone && <p><strong>شماره موبایل:</strong> <span dir="ltr">{user.phone}</span></p>}
        {user.email && <p><strong>ایمیل:</strong> {user.email}</p>}
        <p><strong>نقش:</strong> {user.roles.includes('ROLE_ADMIN') ? 'مدیر' : 'کاربر'}</p>
      </div>
      <ChangePasswordForm />
      <p style={{ marginTop: 20 }}><Link to="/orders">مشاهده سفارش‌های من</Link></p>
    </div>
  );
}

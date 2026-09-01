import { useState, FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Register() {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [phone, setPhone] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (password.length < 6) {
      setError('رمز عبور باید حداقل ۶ کاراکتر باشد');
      return;
    }
    setLoading(true);
    try {
      await register(fullName, email, password, phone || undefined);
      navigate('/');
    } catch (err: any) {
      setError(err.response?.data?.message || 'خطا در ثبت‌نام');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h2>ثبت‌نام در بیوتی‌شاپ</h2>
        {error && <div className="error-msg">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>نام و نام خانوادگی</label>
            <input type="text" value={fullName} onChange={(e) => setFullName(e.target.value)} required placeholder="نام کامل" />
          </div>
          <div className="form-group">
            <label>ایمیل</label>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required placeholder="example@email.com" />
          </div>
          <div className="form-group">
            <label>شماره موبایل (اختیاری)</label>
            <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="09123456789" />
          </div>
          <div className="form-group">
            <label>رمز عبور</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required placeholder="حداقل ۶ کاراکتر" />
          </div>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'در حال ثبت‌نام...' : 'ثبت‌نام'}
          </button>
        </form>
        <div className="auth-footer">
          قبلاً ثبت‌نام کرده‌اید؟ <Link to="/login">وارد شوید</Link>
        </div>
      </div>
    </div>
  );
}

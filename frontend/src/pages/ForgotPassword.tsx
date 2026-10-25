import { useState, FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../services/api';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);
    try {
      const res = await authApi.forgotPassword(email.trim());
      setMessage(
        res.data?.message ||
          'اگر این ایمیل در سیستم ثبت شده باشد، لینک بازیابی رمز عبور برای شما ارسال می‌شود'
      );
    } catch (err: any) {
      setError(err.response?.data?.message || 'خطا در ارسال درخواست. لطفاً بعداً دوباره تلاش کنید');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h2>فراموشی رمز عبور</h2>
        <p style={{ fontSize: 13, marginBottom: 16 }}>
          ایمیل حساب کاربری خود را وارد کنید تا لینک تعیین رمز جدید برای شما ارسال شود.
        </p>
        {error && <div className="error-msg">{error}</div>}
        {message && <div className="success-msg">{message}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>ایمیل</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              maxLength={100}
              placeholder="example@email.com"
              autoComplete="email"
            />
          </div>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'در حال ارسال...' : 'ارسال لینک بازیابی'}
          </button>
        </form>
        <div className="auth-footer">
          <Link to="/login">بازگشت به صفحه ورود</Link>
        </div>
      </div>
    </div>
  );
}

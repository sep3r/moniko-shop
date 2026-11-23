import { useEffect, useState, FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../services/api';
import OtpField from '../components/OtpField';
import { useCountdown } from '../utils/useCountdown';
import { normalizePhone, isStrongPassword, PHONE_ERROR, PASSWORD_ERROR } from '../utils/phone';

/** بازیابی رمز با پیامک: ۱) شماره  ۲) کد + رمز جدید. */
export default function ForgotPassword() {
  const [step, setStep] = useState<1 | 2>(1);
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);
  const { left, start } = useCountdown();
  const navigate = useNavigate();

  useEffect(() => {
    if (!success) return;
    const t = setTimeout(() => navigate('/login'), 3000);
    return () => clearTimeout(t);
  }, [success, navigate]);

  const sendCode = async (e?: FormEvent) => {
    e?.preventDefault();
    setError('');
    const normalized = normalizePhone(phone);
    if (!normalized) return setError(PHONE_ERROR);
    setLoading(true);
    try {
      const res = await authApi.forgotPassword(normalized);
      setPhone(normalized);
      setCode('');
      setMessage(res.data.message);
      setStep(2);
      start(res.data.resendAfterSeconds);
    } catch (err: any) {
      setError(err.response?.data?.message || 'خطا در ارسال درخواست. لطفاً بعداً دوباره تلاش کنید');
    } finally {
      setLoading(false);
    }
  };

  const reset = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (code.length !== 6) return setError('کد ۶ رقمی را کامل وارد کنید');
    if (!isStrongPassword(password)) return setError(PASSWORD_ERROR);
    if (password !== confirmPassword) return setError('رمز عبور و تکرار آن یکسان نیستند');
    setLoading(true);
    try {
      await authApi.resetPassword(phone, code, password);
      setSuccess(true);
    } catch (err: any) {
      setError(err.response?.data?.message || 'خطا در تغییر رمز عبور');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h2>فراموشی رمز عبور</h2>
        {error && <div className="error-msg">{error}</div>}

        {success ? (
          <div className="success-msg">رمز عبور با موفقیت تغییر کرد. در حال انتقال به صفحه ورود...</div>
        ) : step === 1 ? (
          <>
            <p style={{ fontSize: 13, marginBottom: 16 }}>
              شماره موبایل حساب خود را وارد کنید تا کد تأیید برای شما پیامک شود.
            </p>
            <form onSubmit={sendCode}>
              <div className="form-group">
                <label>شماره موبایل</label>
                <input type="tel" inputMode="numeric" dir="ltr" value={phone} onChange={(e) => setPhone(e.target.value)} required placeholder="09123456789" autoComplete="tel" style={{ textAlign: 'right' }} />
              </div>
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'در حال ارسال...' : 'ارسال کد تأیید'}
              </button>
            </form>
          </>
        ) : (
          <form onSubmit={reset}>
            {message && <div className="success-msg">{message}</div>}
            <OtpField value={code} onChange={setCode} resendIn={left} onResend={() => sendCode()} disabled={loading} />
            <div className="form-group">
              <label>رمز عبور جدید</label>
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required maxLength={64} placeholder="حداقل ۸ کاراکتر، شامل حرف و عدد" autoComplete="new-password" />
            </div>
            <div className="form-group">
              <label>تکرار رمز عبور جدید</label>
              <input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required maxLength={64} autoComplete="new-password" />
              {confirmPassword.length > 0 && password !== confirmPassword && (
                <small className="field-msg bad">رمز عبور و تکرار آن یکسان نیستند</small>
              )}
              {confirmPassword.length > 0 && password === confirmPassword && (
                <small className="field-msg good">رمزها یکسان هستند ✓</small>
              )}
            </div>
            <button type="submit" className="btn btn-primary" disabled={loading || code.length !== 6}>
              {loading ? 'در حال ذخیره...' : 'ذخیره رمز عبور جدید'}
            </button>
            <p style={{ fontSize: 13, marginTop: 10 }}>
              <button type="button" className="link-btn" onClick={() => { setStep(1); setError(''); }}>ویرایش شماره</button>
            </p>
          </form>
        )}
        <div className="auth-footer">
          <Link to="/login">بازگشت به صفحه ورود</Link>
        </div>
      </div>
    </div>
  );
}

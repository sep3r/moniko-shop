import { useState, FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../services/api';
import OtpField from '../components/OtpField';
import { useCountdown } from '../utils/useCountdown';
import { normalizePhone, isStrongPassword, PHONE_ERROR, PASSWORD_ERROR } from '../utils/phone';

/** ثبت‌نام با شماره موبایل: ۱) مشخصات و ارسال کد  ۲) وارد کردن کد پیامکی ⇒ ساخت حساب و ورود. */
export default function Register() {
  const [step, setStep] = useState<1 | 2>(1);
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [code, setCode] = useState('');
  const [remember, setRemember] = useState(false);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();
  const { left, start } = useCountdown();

  const sendCode = async (e?: FormEvent) => {
    e?.preventDefault();
    setError('');
    setInfo('');
    const normalized = normalizePhone(phone);
    if (!normalized) return setError(PHONE_ERROR);
    if (!isStrongPassword(password)) return setError(PASSWORD_ERROR);
    if (password !== confirmPassword) return setError('رمز عبور و تکرار آن یکسان نیستند');
    setLoading(true);
    try {
      const res = await authApi.registerOtp(normalized);
      setPhone(normalized);
      setCode('');
      setStep(2);
      setInfo(res.data.message);
      start(res.data.resendAfterSeconds);
    } catch (err: any) {
      const wait = err.response?.status === 429 ? err.response?.data?.message : null;
      setError(wait || err.response?.data?.message || 'ارسال کد ناموفق بود. دوباره تلاش کنید');
    } finally {
      setLoading(false);
    }
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (code.length !== 6) return setError('کد ۶ رقمی را کامل وارد کنید');
    setLoading(true);
    try {
      await register(fullName.trim(), phone, code, password, remember);
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
        <h2>ثبت‌نام در مونا بیوتی</h2>
        {error && <div className="error-msg">{error}</div>}
        {step === 2 && info && <div className="success-msg">{info}</div>}

        {step === 1 ? (
          <form onSubmit={sendCode}>
            <div className="form-group">
              <label>نام و نام خانوادگی</label>
              <input type="text" value={fullName} onChange={(e) => setFullName(e.target.value)} required minLength={2} maxLength={100} placeholder="نام کامل" />
            </div>
            <div className="form-group">
              <label>شماره موبایل</label>
              <input type="tel" inputMode="numeric" dir="ltr" value={phone} onChange={(e) => setPhone(e.target.value)} required placeholder="09123456789" autoComplete="tel" style={{ textAlign: 'right' }} />
            </div>
            <div className="form-group">
              <label>رمز عبور</label>
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required placeholder="حداقل ۸ کاراکتر، شامل حرف و عدد" maxLength={64} autoComplete="new-password" />
            </div>
            <div className="form-group">
              <label>تکرار رمز عبور</label>
              <input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required placeholder="رمز عبور را دوباره وارد کنید" maxLength={64} autoComplete="new-password" />
              {confirmPassword.length > 0 && password !== confirmPassword && (
                <small className="field-msg bad">رمز عبور و تکرار آن یکسان نیستند</small>
              )}
              {confirmPassword.length > 0 && password === confirmPassword && (
                <small className="field-msg good">رمزها یکسان هستند ✓</small>
              )}
            </div>
            <label className="remember-row">
              <input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} />
              <span>مرا به خاطر بسپار <small>(۳ روز نیازی به ورود دوباره نیست)</small></span>
            </label>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'در حال ارسال کد...' : 'دریافت کد تأیید'}
            </button>
          </form>
        ) : (
          <form onSubmit={submit}>
            <p style={{ fontSize: 13, marginBottom: 12 }}>
              کد ۶ رقمی به شماره <strong dir="ltr">{phone}</strong> پیامک شد.{' '}
              <button type="button" className="link-btn" onClick={() => { setStep(1); setError(''); setInfo(''); }}>
                ویرایش شماره
              </button>
            </p>
            <OtpField value={code} onChange={setCode} resendIn={left} onResend={() => sendCode()} disabled={loading} />
            <button type="submit" className="btn btn-primary" disabled={loading || code.length !== 6}>
              {loading ? 'در حال ثبت‌نام...' : 'تأیید و ثبت‌نام'}
            </button>
          </form>
        )}
        <div className="auth-footer">
          قبلاً ثبت‌نام کرده‌اید؟ <Link to="/login">وارد شوید</Link>
        </div>
      </div>
    </div>
  );
}

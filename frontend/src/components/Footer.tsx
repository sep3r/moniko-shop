import { Link } from 'react-router-dom';
import { useSetting, useSite } from '../context/SiteContext';

const Icon = ({ name }: { name: string }) => {
  const common = { width: 20, height: 20, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 1.8, strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const };
  switch (name) {
    case 'instagram':
      return <svg {...common}><rect x="3" y="3" width="18" height="18" rx="5" /><circle cx="12" cy="12" r="4" /><circle cx="17.2" cy="6.8" r=".6" fill="currentColor" /></svg>;
    case 'telegram':
      return <svg {...common}><path d="M21 4L3 11l6 2 2 6 3-4 5 3z" /><path d="M9 13l12-9" /></svg>;
    case 'whatsapp':
      return <svg {...common}><path d="M4 20l1.3-4.2A8 8 0 1 1 8.4 18.8z" /><path d="M9 9c0 3 3 6 6 6l1-1.5-2-1-1 .8c-.8-.4-1.6-1.2-2-2l.8-1-1-2z" /></svg>;
    default:
      return <svg {...common}><rect x="3" y="3" width="18" height="18" rx="3" /><path d="M8 10v6M8 7.5v.1M12 16v-3.5a2 2 0 0 1 4 0V16M12 10v6" /></svg>;
  }
};

export default function Footer() {
  const phone1 = useSetting('phone1', '+98 902 817 8300');
  const phone2 = useSetting('phone2', '+98 902 817 8400');
  const email = useSetting('email', 'info@mail.moniko.ir');
  const hours = useSetting('support_hours', 'روزهای شنبه تا پنجشنبه، از ساعت ۹ الی ۲۱ پاسخگوی سوالات شما هستیم.');
  const address = useSetting('address');
  const copyright = useSetting('copyright', '© ۱۴۰۴ مونا بیوتی - تمامی حقوق محفوظ است');
  const { data } = useSite();
  const socials = ['instagram', 'telegram', 'whatsapp', 'linkedin']
    .map((k) => ({ key: k, url: (data?.settings?.[k] || '').trim() }))
    .filter((s) => s.url);
  const tel = (p: string) => 'tel:' + p.replace(/[^\d+]/g, '');

  return (
    <footer className="kh-footer">
      <div className="kh-container">
        <div className="kh-footer-grid">
          <div>
            <h4>شرکت مونا بیوتی</h4>
            <ul>
              <li><Link to="/about">درباره ما</Link></li>
              <li><Link to="/contact">تماس با ما</Link></li>
              <li><Link to="/orders">پیگیری سفارش</Link></li>
            </ul>
          </div>
          <div>
            <h4>خدمات مشتریان</h4>
            <ul>
              <li><Link to="/faq">پرسش‌های متداول</Link></li>
              <li><Link to="/guide">راهنمای خرید و پرداخت</Link></li>
              <li><Link to="/cart">سبد خرید</Link></li>
            </ul>
          </div>
          <div className="kh-footer-contact">
            <p>{hours}</p>
            <div className="kh-phones">
              <a href={tel(phone1)} dir="ltr">{phone1}</a>
              {phone2 && <a href={tel(phone2)} dir="ltr">{phone2}</a>}
            </div>
            {email && <p dir="ltr" className="kh-email">{email}</p>}
            {address && <p>{address}</p>}
            {socials.length > 0 && (
              <>
                <div className="kh-socials">
                  {socials.map((s) => (
                    <a key={s.key} href={s.url} target="_blank" rel="noopener noreferrer" aria-label={s.key}><Icon name={s.key} /></a>
                  ))}
                </div>
                <p>ما را در شبکه‌های اجتماعی دنبال کنید!</p>
              </>
            )}
          </div>
        </div>

        <div className="kh-seals">
          <a referrerPolicy="origin" target="_blank" rel="noreferrer"
             href="https://trustseal.enamad.ir/?id=8365443&Code=ClMe5Nxa0kR1zge2QEWtMOjdrj4G4shS">
            <img
              referrerPolicy="origin"
              src="https://trustseal.enamad.ir/logo.aspx?id=8365443&Code=ClMe5Nxa0kR1zge2QEWtMOjdrj4G4shS"
              alt="نماد اعتماد الکترونیکی"
              style={{ cursor: 'pointer' }}
              {...({ code: 'ClMe5Nxa0kR1zge2QEWtMOjdrj4G4shS' } as Record<string, string>)}
            />
          </a>
        </div>
        <div className="kh-copyright">{copyright}</div>
      </div>
    </footer>
  );
}

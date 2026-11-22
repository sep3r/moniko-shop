import { Link } from 'react-router-dom';
import { useSeo } from '../utils/seo';

export default function NotFound() {
  useSeo({ title: 'صفحه یافت نشد | مونا بیوتی', noindex: true });
  return (
    <div className="container" style={{ textAlign: 'center', padding: '60px 0' }}>
      <h2>۴۰۴</h2>
      <p>صفحه مورد نظر یافت نشد.</p>
      <Link to="/">بازگشت به صفحه اصلی</Link>
    </div>
  );
}

import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="container" style={{ textAlign: 'center', padding: '60px 0' }}>
      <h2>۴۰۴</h2>
      <p>صفحه مورد نظر یافت نشد.</p>
      <Link to="/">بازگشت به صفحه اصلی</Link>
    </div>
  );
}

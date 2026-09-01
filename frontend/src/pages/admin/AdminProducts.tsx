import { useEffect, useState } from 'react';
import { adminApi } from '../../services/api';
import { Category, Product } from '../../types';

const formatPrice = (price: number) => new Intl.NumberFormat('fa-IR').format(price) + ' تومان';

const emptyForm = {
  id: 0,
  name: '',
  description: '',
  price: '',
  discountPrice: '',
  imageUrl: '',
  brand: '',
  categoryId: '',
  stock: '',
  active: true,
};

export default function AdminProducts() {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [form, setForm] = useState<any>(null);

  const load = () => {
    setLoading(true);
    Promise.all([adminApi.getProducts(), adminApi.getCategories()])
      .then(([p, c]) => {
        setProducts(p.data);
        setCategories(c.data);
      })
      .catch(() => setError('خطا در دریافت اطلاعات'))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const startCreate = () => setForm({ ...emptyForm, categoryId: categories[0]?.id ?? '' });
  const startEdit = (p: Product) => setForm({
    id: p.id,
    name: p.name,
    description: p.description || '',
    price: String(p.price),
    discountPrice: p.discountPrice ? String(p.discountPrice) : '',
    imageUrl: p.imageUrl || '',
    brand: p.brand || '',
    categoryId: p.category?.id ?? '',
    stock: String(p.stock),
    active: p.active,
  });

  const save = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    const payload = {
      name: form.name,
      description: form.description,
      price: Number(form.price),
      discountPrice: form.discountPrice ? Number(form.discountPrice) : null,
      imageUrl: form.imageUrl,
      brand: form.brand,
      categoryId: Number(form.categoryId),
      stock: Number(form.stock),
      active: form.active,
    };
    try {
      if (form.id) {
        await adminApi.updateProduct(form.id, payload);
      } else {
        await adminApi.createProduct(payload);
      }
      setForm(null);
      load();
    } catch (err: any) {
      setError(err.response?.data?.message || 'ذخیره محصول با خطا مواجه شد');
    }
  };

  const remove = async (id: number) => {
    if (!confirm('حذف این محصول قطعی است؟')) return;
    try {
      await adminApi.deleteProduct(id);
      load();
    } catch (err: any) {
      setError(err.response?.data?.message || 'حذف محصول با خطا مواجه شد');
    }
  };

  if (loading) return <div className="loading">در حال بارگذاری...</div>;

  return (
    <div>
      <div className="admin-header-row">
        <h2 className="section-title">محصولات</h2>
        <button className="btn btn-primary" style={{ width: 'auto' }} onClick={startCreate}>+ محصول جدید</button>
      </div>
      {error && <div className="error-msg">{error}</div>}

      {form && (
        <form className="admin-form" onSubmit={save}>
          <div className="form-group">
            <label>نام محصول</label>
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </div>
          <div className="form-group">
            <label>توضیحات</label>
            <input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </div>
          <div className="admin-form-row">
            <div className="form-group">
              <label>قیمت (تومان)</label>
              <input type="number" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>قیمت با تخفیف</label>
              <input type="number" value={form.discountPrice} onChange={(e) => setForm({ ...form, discountPrice: e.target.value })} />
            </div>
            <div className="form-group">
              <label>موجودی</label>
              <input type="number" value={form.stock} onChange={(e) => setForm({ ...form, stock: e.target.value })} required />
            </div>
          </div>
          <div className="admin-form-row">
            <div className="form-group">
              <label>برند</label>
              <input value={form.brand} onChange={(e) => setForm({ ...form, brand: e.target.value })} />
            </div>
            <div className="form-group">
              <label>دسته‌بندی</label>
              <select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })} required>
                {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
          </div>
          <div className="form-group">
            <label>آدرس تصویر</label>
            <input value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} />
          </div>
          <div className="form-group">
            <label><input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /> فعال (نمایش در فروشگاه)</label>
          </div>
          <div className="admin-form-actions">
            <button type="submit" className="btn btn-primary" style={{ width: 'auto' }}>ذخیره</button>
            <button type="button" className="btn" onClick={() => setForm(null)}>انصراف</button>
          </div>
        </form>
      )}

      <table className="admin-table">
        <thead>
          <tr><th>نام</th><th>دسته</th><th>قیمت</th><th>موجودی</th><th>وضعیت</th><th></th></tr>
        </thead>
        <tbody>
          {products.map((p) => (
            <tr key={p.id}>
              <td>{p.name}</td>
              <td>{p.category?.name}</td>
              <td>{formatPrice(p.discountPrice ?? p.price)}</td>
              <td>{p.stock}</td>
              <td>{p.active ? 'فعال' : 'غیرفعال'}</td>
              <td>
                <button className="link-btn" onClick={() => startEdit(p)}>ویرایش</button>
                <button className="link-btn danger" onClick={() => remove(p.id)}>حذف</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

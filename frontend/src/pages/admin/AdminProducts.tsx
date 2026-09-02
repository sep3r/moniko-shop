import { useEffect, useState, useRef } from 'react';
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
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

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

  const startCreate = () => {
    setForm({ ...emptyForm, categoryId: categories[0]?.id ?? '' });
    setImageFile(null);
    setPreviewUrl(null);
  };

  const startEdit = (p: Product) => {
    setForm({
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
    setImageFile(null);
    setPreviewUrl(p.imageUrl || null);
  };

  const onFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] || null;
    setImageFile(file);
    if (file) {
      const url = URL.createObjectURL(file);
      setPreviewUrl(url);
    }
  };

  const save = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    const fd = new FormData();
    fd.append('name', form.name);
    if (form.description) fd.append('description', form.description);
    fd.append('price', String(Number(form.price)));
    if (form.discountPrice) fd.append('discountPrice', String(Number(form.discountPrice)));
    if (form.brand) fd.append('brand', form.brand);
    fd.append('categoryId', String(Number(form.categoryId)));
    fd.append('stock', String(Number(form.stock)));
    fd.append('active', String(form.active));
    if (imageFile) {
      fd.append('image', imageFile);
    } else if (form.imageUrl) {
      fd.append('imageUrl', form.imageUrl);
    }
    try {
      if (form.id) {
        await adminApi.updateProduct(form.id, fd);
      } else {
        await adminApi.createProduct(fd);
      }
      setForm(null);
      setImageFile(null);
      setPreviewUrl(null);
      load();
    } catch (err: any) {
      setError(err.response?.data?.message || err.response?.data?.error || 'ذخیره محصول با خطا مواجه شد');
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
            <textarea
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              rows={3}
              style={{ width: '100%', padding: '12px 14px', border: '1px solid #ddd', borderRadius: 8, fontFamily: 'inherit' }}
            />
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
            <label>تصویر محصول</label>
            <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start', flexWrap: 'wrap' }}>
              <div>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/gif,image/webp"
                  onChange={onFileChange}
                  style={{ marginBottom: 8 }}
                />
                <p style={{ fontSize: 12, color: '#888', marginTop: 4 }}>یا آدرس تصویر خارجی:</p>
                <input
                  value={form.imageUrl}
                  onChange={(e) => {
                    setForm({ ...form, imageUrl: e.target.value });
                    if (!imageFile) setPreviewUrl(e.target.value || null);
                  }}
                  placeholder="https://..."
                  style={{ marginTop: 4 }}
                />
              </div>
              {previewUrl && (
                <img
                  src={previewUrl}
                  alt="پیش‌نمایش"
                  style={{ width: 120, height: 120, objectFit: 'cover', borderRadius: 10, border: '1px solid #eee', background: '#f5f5f5' }}
                  onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
                />
              )}
            </div>
          </div>

          <div className="form-group">
            <label><input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /> فعال (نمایش در فروشگاه)</label>
          </div>
          <div className="admin-form-actions">
            <button type="submit" className="btn btn-primary" style={{ width: 'auto' }}>ذخیره</button>
            <button type="button" className="btn" onClick={() => { setForm(null); setImageFile(null); setPreviewUrl(null); }}>انصراف</button>
          </div>
        </form>
      )}

      <table className="admin-table">
        <thead>
          <tr><th>تصویر</th><th>نام</th><th>دسته</th><th>قیمت</th><th>موجودی</th><th>وضعیت</th><th></th></tr>
        </thead>
        <tbody>
          {products.map((p) => (
            <tr key={p.id}>
              <td>
                <img
                  src={p.imageUrl || 'https://via.placeholder.com/48'}
                  alt=""
                  style={{ width: 48, height: 48, objectFit: 'cover', borderRadius: 6, background: '#f5f5f5' }}
                />
              </td>
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

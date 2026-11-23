import axios from 'axios';

// Relative by default: nginx (prod) and the Vite dev server (see vite.config.ts)
// both proxy /api/* to the backend, so this works regardless of host/domain.
// Set VITE_API_URL only if the API is served from a different origin.
const API_URL = import.meta.env.VITE_API_URL || '/api';

const api = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  // Let the browser set multipart/form-data with its boundary.
  if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
    delete config.headers['Content-Type'];
  }

  return config;
});

export const authApi = {
  register: (data: { fullName: string; email: string; password: string; phone?: string; rememberMe?: boolean }) =>
    api.post('/auth/register', data),
  login: (data: { email: string; password: string; rememberMe?: boolean }) =>
    api.post('/auth/login', data),
  me: () => api.get('/auth/me'),
  changePassword: (currentPassword: string, newPassword: string, rememberMe?: boolean) =>
    api.post('/auth/change-password', { currentPassword, newPassword, rememberMe }),
  forgotPassword: (email: string) => api.post('/auth/forgot-password', { email }),
  resetPassword: (token: string, newPassword: string) =>
    api.post('/auth/reset-password', { token, newPassword }),
};

export interface ProductSearchParams {
  q?: string;
  category?: string;
  sort?: string;
  inStock?: boolean;
  /** شماره‌ی صفحه از ۰ */
  page?: number;
  size?: number;
}

export const productApi = {
  /** همه‌ی محصولات فعال بدون صفحه‌بندی؛ برای لیست‌های بزرگ از search استفاده کنید */
  getAll: () => api.get('/products'),
  /** جستجو/لیست سمت سرور با صفحه‌بندی (PagedProducts) */
  search: (p: ProductSearchParams) =>
    api.get('/products/search', { params: { ...p, inStock: p.inStock ? true : undefined } }),
  getById: (id: number) => api.get(`/products/${id}`),
  getCategories: () => api.get('/categories'),
  /** درخت کامل دسته‌ها برای مگامنو */
  getCategoryTree: () => api.get('/categories/tree'),
  getByCategory: (slug: string) => api.get(`/categories/${slug}/products`),
};

export const homeApi = {
  get: () => api.get('/home'),
};

export const orderApi = {
  create: (data: { items: { productId: number; quantity: number }[]; shippingAddress: string; phone: string }) =>
    api.post('/orders', data),
  getMine: () => api.get('/orders'),
  getById: (id: number) => api.get(`/orders/${id}`),
};

export const adminApi = {
  // Products (multipart for image upload)
  getProducts: () => api.get('/admin/products'),
  createProduct: (formData: FormData) =>
    api.post('/admin/products', formData),
  updateProduct: (id: number, formData: FormData) =>
    api.put(`/admin/products/${id}`, formData),
  deleteProduct: (id: number) => api.delete(`/admin/products/${id}`),
  // Categories
  getCategories: () => api.get('/admin/categories'),
  getCategoryTree: () => api.get('/admin/categories/tree'),
  createCategory: (data: any) => api.post('/admin/categories', data),
  updateCategory: (id: number, data: any) => api.put(`/admin/categories/${id}`, data),
  deleteCategory: (id: number) => api.delete(`/admin/categories/${id}`),
  // Orders
  getOrders: () => api.get('/admin/orders'),
  updateOrderStatus: (id: number, status: string) => api.patch(`/admin/orders/${id}/status`, { status }),
  // Storefront (banners / brands / settings / home products)
  getBanners: () => api.get('/admin/banners'),
  saveBanner: (id: number | null, fd: FormData) =>
    id ? api.put(`/admin/banners/${id}`, fd) : api.post('/admin/banners', fd),
  deleteBanner: (id: number) => api.delete(`/admin/banners/${id}`),
  getBrands: () => api.get('/admin/brands'),
  saveBrand: (id: number | null, fd: FormData) =>
    id ? api.put(`/admin/brands/${id}`, fd) : api.post('/admin/brands', fd),
  deleteBrand: (id: number) => api.delete(`/admin/brands/${id}`),
  getSettings: () => api.get('/admin/settings'),
  saveSettings: (data: Record<string, string>) => api.put('/admin/settings', data),
  getHomeProducts: (type: 'daily-deal' | 'featured') => api.get(`/admin/home-products/${type}`),
  saveHomeProducts: (type: 'daily-deal' | 'featured', items: { productId: number; endsAt?: string | null }[]) =>
    api.put(`/admin/home-products/${type}`, { items }),
  // Visits
  getVisits: () => api.get('/admin/visits'),
  // Logs (IP بازدیدکنندگان و رویدادهای امنیتی)
  getVisitLogs: (params: { page?: number; size?: number; kind?: string; ip?: string; date?: string }) =>
    api.get('/admin/logs/visits', { params }),
  getSecurityLogs: (params: { page?: number; size?: number; type?: string; q?: string }) =>
    api.get('/admin/logs/security', { params }),
  // Users
  getUsers: () => api.get('/admin/users'),
  updateUserStatus: (id: number, enabled: boolean) => api.patch(`/admin/users/${id}/status`, { enabled }),
  updateUserRoles: (id: number, roles: string[]) => api.patch(`/admin/users/${id}/roles`, { roles }),
};

export default api;

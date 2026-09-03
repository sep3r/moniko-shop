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
  register: (data: { fullName: string; email: string; password: string; phone?: string }) =>
    api.post('/auth/register', data),
  login: (data: { email: string; password: string }) =>
    api.post('/auth/login', data),
  me: () => api.get('/auth/me'),
};

export const productApi = {
  getAll: () => api.get('/products'),
  getById: (id: number) => api.get(`/products/${id}`),
  getCategories: () => api.get('/categories'),
  /** درخت کامل دسته‌ها برای مگامنو */
  getCategoryTree: () => api.get('/categories/tree'),
  getByCategory: (slug: string) => api.get(`/categories/${slug}/products`),
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
  // Users
  getUsers: () => api.get('/admin/users'),
  updateUserStatus: (id: number, enabled: boolean) => api.patch(`/admin/users/${id}/status`, { enabled }),
  updateUserRoles: (id: number, roles: string[]) => api.patch(`/admin/users/${id}/roles`, { roles }),
};

export default api;

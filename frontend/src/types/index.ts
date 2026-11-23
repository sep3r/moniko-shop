export interface User {
  id: number;
  email?: string | null;
  fullName: string;
  phone?: string | null;
  /** true ⇒ تغییر رمز نیاز به کد پیامکی دارد */
  phoneVerified?: boolean;
  roles: string[];
}

export interface AuthResponse {
  token: string;
  type: string;
  id: number;
  email?: string | null;
  phone?: string | null;
  fullName: string;
  roles: string[];
}

export interface Category {
  id: number;
  name: string;
  slug: string;
  imageUrl?: string;
  sortOrder?: number;
  parentId?: number | null;
  children?: Category[];
}

export interface ProductImage {
  id: number;
  url: string;
}

export interface ProductSpec {
  key: string;
  value: string;
}

export interface Product {
  id: number;
  name: string;
  subtitle?: string;
  description?: string;
  images?: ProductImage[];
  specs?: ProductSpec[];
  price: number;
  discountPrice?: number;
  imageUrl?: string;
  brand?: string;
  category?: Category;
  stock: number;
  active: boolean;
}

/** یک صفحه از نتایج جستجو/لیست محصولات (page از ۰ شروع می‌شود) */
export interface PagedProducts {
  items: Product[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  /** فقط وقتی لیست روی یک دسته فیلتر شده */
  categoryName?: string | null;
}

export interface CartItem {
  productId: number;
  name: string;
  price: number;
  imageUrl?: string;
  quantity: number;
  stock: number;
}

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface OrderItem {
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface Order {
  id: number;
  status: OrderStatus;
  totalAmount: number;
  shippingAddress: string;
  phone: string;
  createdAt: string;
  items: OrderItem[];
  customerName?: string;
  customerEmail?: string;
  customerPhone?: string;
}

export interface AdminUser {
  id: number;
  email: string;
  fullName: string;
  phone?: string;
  roles: string[];
  enabled: boolean;
  createdAt: string;
  lastLoginAt?: string | null;
}

export interface VisitDay {
  date: string;
  total: number;
  users: number;
  guests: number;
}

export interface VisitStats {
  online: { total: number; users: number; guests: number };
  today: VisitDay;
  history: VisitDay[];
}

export interface Banner {
  id: number;
  position: 'HERO' | 'MID1' | 'MID2';
  title?: string;
  linkUrl?: string;
  imageUrl: string;
  sortOrder: number;
  active: boolean;
}

export interface Brand {
  id: number;
  name: string;
  linkUrl?: string;
  imageUrl: string;
  sortOrder: number;
  active: boolean;
}

export interface HomeData {
  banners: Banner[];
  brands: Brand[];
  settings: Record<string, string>;
  dailyDeal: { endsAt?: string | null; products: Product[] };
  featured: Product[];
}

export interface HomeProductItem {
  productId: number;
  productName: string;
  endsAt?: string | null;
}

export interface LogPage<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface VisitLogEntry {
  id: number;
  date: string;
  visitorId: string;
  userId: number | null;
  userEmail: string | null;
  ip: string;
  userAgent: string | null;
  firstSeen: string;
  lastSeen: string;
}

export interface SecurityEventEntry {
  id: number;
  createdAt: string;
  type: string;
  userId: number | null;
  email: string | null;
  ip: string | null;
  userAgent: string | null;
  details: string | null;
}

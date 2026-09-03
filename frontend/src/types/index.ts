export interface User {
  id: number;
  email: string;
  fullName: string;
  phone?: string;
  roles: string[];
}

export interface AuthResponse {
  token: string;
  type: string;
  id: number;
  email: string;
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

export interface Product {
  id: number;
  name: string;
  description?: string;
  price: number;
  discountPrice?: number;
  imageUrl?: string;
  brand?: string;
  category?: Category;
  stock: number;
  active: boolean;
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
}

export interface AdminUser {
  id: number;
  email: string;
  fullName: string;
  phone?: string;
  roles: string[];
  enabled: boolean;
  createdAt: string;
}

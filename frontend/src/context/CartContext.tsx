import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { CartItem, Product } from '../types';
import { useAuth } from './AuthContext';

interface CartContextType {
  items: CartItem[];
  addItem: (product: Product, quantity?: number) => void;
  removeItem: (productId: number) => void;
  updateQuantity: (productId: number, quantity: number) => void;
  clearCart: () => void;
  totalItems: number;
  totalPrice: number;
}

const CartContext = createContext<CartContextType | undefined>(undefined);
const STORAGE_PREFIX = 'moniko_cart_';
// Cart for a logged-out visitor is kept separate from any account's cart.
const guestKey = () => `${STORAGE_PREFIX}guest`;
const userKey = (userId: number | string) => `${STORAGE_PREFIX}user_${userId}`;

const loadCart = (key: string): CartItem[] => {
  try {
    const stored = localStorage.getItem(key);
    return stored ? JSON.parse(stored) : [];
  } catch {
    return [];
  }
};

export const CartProvider = ({ children }: { children: ReactNode }) => {
  const { user, loading } = useAuth();
  // While auth is still resolving (e.g. on refresh, before /me returns),
  // don't load/render a cart yet - loading the guest cart here and then
  // swapping to the account cart a moment later is what let one account's
  // cart flash into view under another account.
  const activeKey = loading ? null : (user ? userKey(user.id) : guestKey());

  const [storageKey, setStorageKey] = useState<string | null>(activeKey);
  const [items, setItems] = useState<CartItem[]>(() => (activeKey ? loadCart(activeKey) : []));

  // Whenever the logged-in account changes (login, logout, or switching
  // accounts), load that account's own cart instead of continuing to show
  // whatever was previously in state.
  useEffect(() => {
    if (activeKey === null) return;
    setStorageKey(activeKey);
    setItems(loadCart(activeKey));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeKey]);

  useEffect(() => {
    if (!storageKey) return;
    localStorage.setItem(storageKey, JSON.stringify(items));
  }, [items, storageKey]);

  const addItem = (product: Product, quantity = 1) => {
    setItems((prev) => {
      const existing = prev.find((i) => i.productId === product.id);
      const price = product.discountPrice ?? product.price;
      if (existing) {
        const newQty = Math.min(existing.quantity + quantity, product.stock);
        return prev.map((i) => (i.productId === product.id ? { ...i, quantity: newQty } : i));
      }
      return [
        ...prev,
        {
          productId: product.id,
          name: product.name,
          price,
          imageUrl: product.imageUrl,
          quantity: Math.min(quantity, product.stock),
          stock: product.stock,
        },
      ];
    });
  };

  const removeItem = (productId: number) => {
    setItems((prev) => prev.filter((i) => i.productId !== productId));
  };

  const updateQuantity = (productId: number, quantity: number) => {
    if (quantity < 1) {
      removeItem(productId);
      return;
    }
    setItems((prev) => prev.map((i) => (i.productId === productId ? { ...i, quantity: Math.min(quantity, i.stock) } : i)));
  };

  const clearCart = () => setItems([]);

  const totalItems = items.reduce((sum, i) => sum + i.quantity, 0);
  const totalPrice = items.reduce((sum, i) => sum + i.price * i.quantity, 0);

  return (
    <CartContext.Provider value={{ items, addItem, removeItem, updateQuantity, clearCart, totalItems, totalPrice }}>
      {children}
    </CartContext.Provider>
  );
};

export const useCart = () => {
  const context = useContext(CartContext);
  if (!context) throw new Error('useCart must be used within CartProvider');
  return context;
};

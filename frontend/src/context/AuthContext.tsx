import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User, AuthResponse } from '../types';
import { authApi } from '../services/api';

interface AuthContextType {
  user: User | null;
  token: string | null;
  /** rememberMe=true ⇒ ۳ روز بدون ورود دوباره؛ وگرنه نشست عادی (۸ ساعت). */
  login: (email: string, password: string, rememberMe?: boolean) => Promise<void>;
  register: (fullName: string, email: string, password: string, phone?: string, rememberMe?: boolean) => Promise<void>;
  changePassword: (currentPassword: string, newPassword: string) => Promise<void>;
  logout: () => void;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const TOKEN_KEY = 'token';
const REMEMBER_KEY = 'remember_me';

function saveSession(token: string, remember: boolean) {
  localStorage.setItem(TOKEN_KEY, token);
  if (remember) localStorage.setItem(REMEMBER_KEY, '1');
  else localStorage.removeItem(REMEMBER_KEY);
}

function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REMEMBER_KEY);
}

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'));
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (token) {
      authApi.me()
        .then((res) => setUser(res.data))
        .catch(() => {
          clearSession();   // توکن منقضی یا نامعتبر
          setToken(null);
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, [token]);

  const login = async (email: string, password: string, rememberMe = false) => {
    const res = await authApi.login({ email, password, rememberMe });
    const data: AuthResponse = res.data;
    saveSession(data.token, rememberMe);
    setToken(data.token);
    setUser({ id: data.id, email: data.email, fullName: data.fullName, roles: data.roles });
  };

  const register = async (fullName: string, email: string, password: string, phone?: string, rememberMe = false) => {
    const res = await authApi.register({ fullName, email, password, phone, rememberMe });
    const data: AuthResponse = res.data;
    saveSession(data.token, rememberMe);
    setToken(data.token);
    setUser({ id: data.id, email: data.email, fullName: data.fullName, roles: data.roles });
  };

  const changePassword = async (currentPassword: string, newPassword: string) => {
    const remembered = localStorage.getItem(REMEMBER_KEY) === '1';
    const res = await authApi.changePassword(currentPassword, newPassword, remembered);
    const data: AuthResponse = res.data;
    // توکن‌های قبلی باطل شده‌اند؛ توکن تازه (هم‌مدت با نشست فعلی) جایگزین می‌شود تا کاربر logout نشود
    saveSession(data.token, remembered);
    setToken(data.token);
  };

  const logout = () => {
    clearSession();
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, login, register, changePassword, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
};

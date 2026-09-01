import { Navigate } from 'react-router-dom';
import { ReactNode } from 'react';
import { useAuth } from '../context/AuthContext';

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="loading">در حال بارگذاری...</div>;
  if (!user) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export function AdminRoute({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="loading">در حال بارگذاری...</div>;
  if (!user) return <Navigate to="/login" replace />;
  if (!user.roles.includes('ROLE_ADMIN')) return <Navigate to="/" replace />;
  return <>{children}</>;
}

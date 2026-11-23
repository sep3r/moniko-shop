// @ts-ignore
import React, { Suspense, lazy } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Header from './components/Header';
import Footer from './components/Footer';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import Category from './pages/Category';
import ProductDetail from './pages/ProductDetail';
import Cart from './pages/Cart';
import NotFound from './pages/NotFound';
import Search from './pages/Search';
import { ProtectedRoute, AdminRoute } from './components/RouteGuards';
import { useVisitTracker } from './utils/visitTracker';
import { usePrivateRouteNoindex } from './utils/seo';

// صفحه‌های پنل کاربر و مدیریت فقط وقتی لازم شدند بارگذاری می‌شوند (بسته‌ی اولیه‌ی سایت سبک‌تر = سریع‌تر برای بازدیدکننده و گوگل)
const Checkout = lazy(() => import('./pages/Checkout'));
const Orders = lazy(() => import('./pages/Orders'));
const OrderDetail = lazy(() => import('./pages/OrderDetail'));
const Profile = lazy(() => import('./pages/Profile'));
const AdminLayout = lazy(() => import('./pages/admin/AdminLayout'));
const AdminProducts = lazy(() => import('./pages/admin/AdminProducts'));
const AdminCategories = lazy(() => import('./pages/admin/AdminCategories'));
const AdminOrders = lazy(() => import('./pages/admin/AdminOrders'));
const AdminUsers = lazy(() => import('./pages/admin/AdminUsers'));
const AdminVisits = lazy(() => import('./pages/admin/AdminVisits'));
const AdminLogs = lazy(() => import('./pages/admin/AdminLogs'));
const AdminBanners = lazy(() => import('./pages/admin/AdminBanners'));
const AdminBrands = lazy(() => import('./pages/admin/AdminBrands'));
const AdminHomeProducts = lazy(() => import('./pages/admin/AdminHomeProducts'));
const AdminSettings = lazy(() => import('./pages/admin/AdminSettings'));

export default function App() {
    useVisitTracker();
    usePrivateRouteNoindex();
    return (
        <>
            <Header />
            <main>
                <Suspense fallback={<div className="loading">در حال بارگذاری...</div>}>
                <Routes>
                    <Route path="/" element={<Home />} />
                    <Route path="/login" element={<Login />} />
                    <Route path="/register" element={<Register />} />
                    <Route path="/forgot-password" element={<ForgotPassword />} />
                    <Route path="/reset-password" element={<Navigate to="/forgot-password" replace />} />
                    <Route path="/categories/:slug" element={<Category />} />
                    <Route path="/products/:id" element={<ProductDetail />} />
                    <Route path="/cart" element={<Cart />} />
                    <Route path="/search" element={<Search />} />

                    {/* User panel */}
                    <Route path="/checkout" element={<ProtectedRoute><Checkout /></ProtectedRoute>} />
                    <Route path="/orders" element={<ProtectedRoute><Orders /></ProtectedRoute>} />
                    <Route path="/orders/:id" element={<ProtectedRoute><OrderDetail /></ProtectedRoute>} />
                    <Route path="/profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />

                    {/* Admin panel */}
                    <Route path="/admin" element={<AdminRoute><AdminLayout /></AdminRoute>}>
                        <Route index element={<AdminProducts />} />
                        <Route path="products" element={<AdminProducts />} />
                        <Route path="categories" element={<AdminCategories />} />
                        <Route path="orders" element={<AdminOrders />} />
                        <Route path="users" element={<AdminUsers />} />
                        <Route path="visits" element={<AdminVisits />} />
                        <Route path="logs" element={<AdminLogs />} />
                        <Route path="banners" element={<AdminBanners />} />
                        <Route path="brands" element={<AdminBrands />} />
                        <Route path="home-products" element={<AdminHomeProducts />} />
                        <Route path="settings" element={<AdminSettings />} />
                    </Route>

                    <Route path="*" element={<NotFound />} />
                </Routes>
                </Suspense>
            </main>
            <Footer />
        </>
    );
}

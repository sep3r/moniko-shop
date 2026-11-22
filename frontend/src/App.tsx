// @ts-ignore
import React from 'react';
import { Routes, Route } from 'react-router-dom';
import Header from './components/Header';
import Footer from './components/Footer';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import Category from './pages/Category';
import ProductDetail from './pages/ProductDetail';
import Cart from './pages/Cart';
import Checkout from './pages/Checkout';
import Orders from './pages/Orders';
import OrderDetail from './pages/OrderDetail';
import Profile from './pages/Profile';
import NotFound from './pages/NotFound';
import Search from './pages/Search';
import { ProtectedRoute, AdminRoute } from './components/RouteGuards';
import AdminLayout from './pages/admin/AdminLayout';
import AdminProducts from './pages/admin/AdminProducts';
import AdminCategories from './pages/admin/AdminCategories';
import AdminOrders from './pages/admin/AdminOrders';
import AdminUsers from './pages/admin/AdminUsers';
import AdminVisits from './pages/admin/AdminVisits';
import AdminLogs from './pages/admin/AdminLogs';
import AdminBanners from './pages/admin/AdminBanners';
import AdminBrands from './pages/admin/AdminBrands';
import AdminHomeProducts from './pages/admin/AdminHomeProducts';
import AdminSettings from './pages/admin/AdminSettings';
import { useVisitTracker } from './utils/visitTracker';
import { usePrivateRouteNoindex } from './utils/seo';

export default function App() {
    useVisitTracker();
    usePrivateRouteNoindex();
    return (
        <>
            <Header />
            <main>
                <Routes>
                    <Route path="/" element={<Home />} />
                    <Route path="/login" element={<Login />} />
                    <Route path="/register" element={<Register />} />
                    <Route path="/forgot-password" element={<ForgotPassword />} />
                    <Route path="/reset-password" element={<ResetPassword />} />
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
            </main>
            <Footer />
        </>
    );
}

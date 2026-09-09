import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { I18nProvider } from './i18n/i18n';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './hooks/useToast';
import { useAuth } from './context/AuthContext';
import AppErrorBoundary from './components/AppErrorBoundary';
import './index.css';

// Pages
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import DiscoveryPage from './pages/DiscoveryPage';
import AnalysisPage from './pages/AnalysisPage';
import SimulatorPage from './pages/SimulatorPage';
import MapPage from './pages/MapPage';
import GoalSeekPage from './pages/GoalSeekPage';
import ReportsPage from './pages/ReportsPage';
import PostLoanPage from './pages/PostLoanPage';
import ConnectNGOPage from './pages/ConnectNGOPage';

function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();
  
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="w-12 h-12 border-4 border-primary-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }
  
  return isAuthenticated ? children : <Navigate to="/login" replace />;
}

function PublicRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();
  
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="w-12 h-12 border-4 border-primary-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }
  
  return isAuthenticated ? <Navigate to="/dashboard" replace /> : children;
}

function AppRoutes() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={
        <PublicRoute>
          <LoginPage />
        </PublicRoute>
      } />
      
      {/* Protected routes */}
      <Route path="/dashboard" element={
        <ProtectedRoute>
          <DashboardPage />
        </ProtectedRoute>
      } />
      <Route path="/discovery" element={
        <ProtectedRoute>
          <DiscoveryPage />
        </ProtectedRoute>
      } />
      <Route path="/analysis" element={
        <ProtectedRoute>
          <AnalysisPage />
        </ProtectedRoute>
      } />
      <Route path="/simulator" element={
        <ProtectedRoute>
          <SimulatorPage />
        </ProtectedRoute>
      } />
      <Route path="/map" element={
        <ProtectedRoute>
          <MapPage />
        </ProtectedRoute>
      } />
      <Route path="/goal-seek" element={
        <ProtectedRoute>
          <GoalSeekPage />
        </ProtectedRoute>
      } />
      <Route path="/reports" element={
        <ProtectedRoute>
          <ReportsPage />
        </ProtectedRoute>
      } />
      <Route path="/post-loan-preview" element={
        <ProtectedRoute>
          <PostLoanPage />
        </ProtectedRoute>
      } />
      <Route path="/connect-ngo" element={
        <ProtectedRoute>
          <ConnectNGOPage />
        </ProtectedRoute>
      } />
      
      {/* Redirects */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <AppErrorBoundary>
      <I18nProvider>
        <AuthProvider>
          <ToastProvider>
            <BrowserRouter>
              <AppRoutes />
            </BrowserRouter>
          </ToastProvider>
        </AuthProvider>
      </I18nProvider>
    </AppErrorBoundary>
  );
}
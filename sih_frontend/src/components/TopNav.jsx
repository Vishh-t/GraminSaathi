import { NavLink, useNavigate } from 'react-router-dom';
import { useState, useRef, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useI18n } from '../i18n/i18n';
import LanguageToggle from './LanguageToggle';
import { Settings, HelpCircle, Bell, User, LogOut, FileText, ChevronDown } from 'lucide-react';

const navItems = [
  { to: '/dashboard', key: 'nav.dashboard', fallback: 'Dashboard' },
  { to: '/discovery', key: 'nav.discovery', fallback: 'Discovery' },
  { to: '/analysis', key: 'nav.analysis', fallback: 'Analysis' },
  { to: '/reports', key: 'nav.reports', fallback: 'Reports' },
  { to: '/map', key: 'nav.map', fallback: 'Map' },
];

export default function TopNav({ subtitle }) {
  const { user, logout, isAuthenticated } = useAuth();
  const { t } = useI18n();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    function onClickOutside(e) {
      if (menuRef.current && !menuRef.current.contains(e.target)) setMenuOpen(false);
    }
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-40">
      <div className="max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 gap-4">
          {/* Logo */}
          <button
            onClick={() => navigate('/dashboard')}
            className="flex items-center gap-2.5 shrink-0"
          >
            <div className="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-sm">GS</span>
            </div>
            <div className="text-left hidden sm:block">
              <p className="text-lg font-bold text-primary-700 leading-tight">{t('app.name')}</p>
              {subtitle && <p className="text-[11px] text-gray-500 leading-tight">{subtitle}</p>}
            </div>
          </button>

          {/* Nav links */}
          <nav className="hidden md:flex items-center gap-1 flex-1 justify-center">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) => (isActive ? 'nav-link-active' : 'nav-link')}
              >
                {t(item.key) !== item.key ? t(item.key) : item.fallback}
              </NavLink>
            ))}
          </nav>

          {/* Actions */}
          <div className="flex items-center gap-1.5 sm:gap-2 shrink-0">
            <button
              onClick={() => navigate('/reports')}
              className="hidden sm:inline-flex btn-primary !min-h-0 !py-2 text-sm items-center gap-1.5"
            >
              <FileText className="w-4 h-4" />
              <span>View Reports</span>
            </button>
            <div className="hidden lg:block">
              <LanguageToggle />
            </div>
            <button className="p-2 text-gray-500 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors" aria-label="Notifications">
              <Bell className="w-5 h-5" />
            </button>
            <button className="p-2 text-gray-500 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors" aria-label="Settings">
              <Settings className="w-5 h-5" />
            </button>
            <button className="hidden sm:inline-flex p-2 text-gray-500 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors" aria-label="Help">
              <HelpCircle className="w-5 h-5" />
            </button>

            {isAuthenticated && (
              <div className="relative" ref={menuRef}>
                <button
                  onClick={() => setMenuOpen((v) => !v)}
                  className="flex items-center gap-1.5 pl-1 pr-1.5 py-1 rounded-full hover:bg-gray-100 transition-colors"
                >
                  <div className="w-8 h-8 bg-primary-600 rounded-full flex items-center justify-center text-white font-semibold text-xs">
                    {(user?.fullName || 'U').charAt(0).toUpperCase()}
                  </div>
                  <ChevronDown className="w-3.5 h-3.5 text-gray-400 hidden sm:block" />
                </button>
                {menuOpen && (
                  <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-50">
                    <div className="px-3 py-2 border-b border-gray-100">
                      <p className="text-sm font-medium text-gray-900 truncate">{user?.fullName || 'User'}</p>
                      <p className="text-xs text-gray-500 truncate">{user?.email}</p>
                    </div>
                    <button
                      onClick={() => { setMenuOpen(false); logout(); }}
                      className="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
                    >
                      <LogOut className="w-4 h-4" />
                      {t('auth.logout')}
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Mobile nav */}
        <nav className="flex md:hidden items-center gap-1 overflow-x-auto pb-2 -mt-1 scrollbar-hide">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `whitespace-nowrap ${isActive ? 'nav-link-active' : 'nav-link'}`}
            >
              {t(item.key) !== item.key ? t(item.key) : item.fallback}
            </NavLink>
          ))}
        </nav>
      </div>
    </header>
  );
}

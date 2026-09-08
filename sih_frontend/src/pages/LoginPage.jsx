import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useI18n } from '../i18n/i18n';
import VoiceButton from '../components/VoiceButton';
import LanguageToggle from '../components/LanguageToggle';
import { Eye, EyeOff, Mail, Lock, User, Loader2 } from 'lucide-react';

export default function LoginPage() {
  const { t, language } = useI18n();
  const { login, signup } = useAuth();
  const navigate = useNavigate();
  const [isLogin, setIsLogin] = useState(true);
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [errors, setErrors] = useState({});
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const validate = () => {
    const newErrors = {};
    if (!formData.email) {
      newErrors.email = t('auth.enterEmail');
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = 'Invalid email format';
    }
    if (!formData.password) {
      newErrors.password = t('auth.enterPassword');
    } else if (formData.password.length < 6) {
      newErrors.password = t('auth.weakPassword');
    }
    if (!isLogin && !formData.fullName) {
      newErrors.fullName = t('auth.enterFullName');
    }
    if (!isLogin && formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = t('auth.passwordMismatch');
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    
    setIsLoading(true);
    try {
      if (isLogin) {
        await login(formData.email, formData.password);
      } else {
        await signup(formData.fullName, formData.email, formData.password);
      }
      navigate('/dashboard');
    } catch (err) {
      const message = err.response?.data?.message || (isLogin ? t('auth.invalidCredentials') : t('auth.emailExists'));
      setErrors({ submit: message });
    } finally {
      setIsLoading(false);
    }
  };

  const handleVoiceInput = (field) => (transcript) => {
    setFormData(prev => ({ ...prev, [field]: transcript }));
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="w-16 h-16 bg-primary-600 rounded-2xl flex items-center justify-center mx-auto mb-4">
            <span className="text-white font-bold text-2xl">GS</span>
          </div>
          <h1 className="text-3xl font-bold text-gray-900">{t('app.name')}</h1>
          <p className="text-gray-500 mt-2">{t('app.tagline')}</p>
        </div>

        {/* Language Toggle */}
        <div className="flex justify-end mb-6">
          <LanguageToggle />
        </div>

        {/* Form Card */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
          <div className="flex gap-4 mb-6 border-b border-gray-200">
            <button
              onClick={() => setIsLogin(true)}
              className={`flex-1 py-2 text-sm font-medium rounded-lg transition-colors ${
                isLogin ? 'bg-primary-600 text-white' : 'text-gray-500 hover:text-gray-900'
              }`}
            >
              {t('auth.signIn')}
            </button>
            <button
              onClick={() => setIsLogin(false)}
              className={`flex-1 py-2 text-sm font-medium rounded-lg transition-colors ${
                !isLogin ? 'bg-primary-600 text-white' : 'text-gray-500 hover:text-gray-900'
              }`}
            >
              {t('auth.signUp')}
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {!isLogin && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">{t('auth.fullName')}</label>
                <div className="relative">
                  <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    type="text"
                    name="fullName"
                    value={formData.fullName}
                    onChange={handleChange}
                    placeholder={t('auth.enterFullName')}
                    className={`input-field pl-10 ${errors.fullName ? 'border-red-500 focus:ring-red-500' : ''}`}
                    disabled={isLoading}
                  />
                </div>
                {errors.fullName && <p className="mt-1 text-sm text-red-600">{errors.fullName}</p>}
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('auth.email')}</label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  placeholder={t('auth.enterEmail')}
                  className={`input-field pl-10 ${errors.email ? 'border-red-500 focus:ring-red-500' : ''}`}
                  disabled={isLoading}
                />
                <VoiceButton
                  onTranscript={handleVoiceInput('email')}
                  language={language === 'hi' ? 'hi-IN' : 'en-IN'}
                  className="absolute right-3 top-1/2 -translate-y-1/2"
                />
              </div>
              {errors.email && <p className="mt-1 text-sm text-red-600">{errors.email}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('auth.password')}</label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder={t('auth.enterPassword')}
                  className={`input-field pl-10 pr-12 ${errors.password ? 'border-red-500 focus:ring-red-500' : ''}`}
                  disabled={isLoading}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
              {errors.password && <p className="mt-1 text-sm text-red-600">{errors.password}</p>}
            </div>

            {!isLogin && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">{t('auth.confirmPassword')}</label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="confirmPassword"
                    value={formData.confirmPassword}
                    onChange={handleChange}
                    placeholder={t('auth.confirmPassword')}
                    className={`input-field pl-10 ${errors.confirmPassword ? 'border-red-500 focus:ring-red-500' : ''}`}
                    disabled={isLoading}
                  />
                </div>
                {errors.confirmPassword && <p className="mt-1 text-sm text-red-600">{errors.confirmPassword}</p>}
              </div>
            )}

            {errors.submit && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
                {errors.submit}
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading}
              className="btn-primary w-full py-3"
            >
              {isLoading ? (
                <Loader2 className="w-5 h-5 animate-spin mx-auto" />
              ) : (
                isLogin ? t('auth.login') : t('auth.createAccount')
              )}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-600">
            {isLogin ? t('auth.dontHaveAccount') : t('auth.alreadyHaveAccount')}{' '}
            <button
              onClick={() => { setIsLogin(!isLogin); setErrors({}); }}
              className="text-primary-600 hover:underline font-medium"
            >
              {isLogin ? t('auth.signUp') : t('auth.signIn')}
            </button>
          </p>
        </div>

        <p className="mt-6 text-center text-xs text-gray-400">
          {t('app.name')} — SIH 2026 Prototype
        </p>
      </div>
    </div>
  );
}
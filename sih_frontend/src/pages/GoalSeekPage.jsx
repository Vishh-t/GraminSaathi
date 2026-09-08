import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { referenceAPI, analysisAPI } from '../services/api';
import { useI18n } from '../i18n/i18n';
import TopNav from '../components/TopNav';
import { formatCurrency } from '../utils/format';
import { getErrorMessage } from '../utils/errors';
import { Loader2, Target, TrendingUp, DollarSign, Calculator } from 'lucide-react';

export default function GoalSeekPage() {
  const { t, language } = useI18n();
  const navigate = useNavigate();
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('');
  const [desiredIncome, setDesiredIncome] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadCategories();
  }, []);

  const loadCategories = async () => {
    try {
      const res = await referenceAPI.getBusinessCategories();
      setCategories(res.data);
      if (res.data.length > 0) setSelectedCategory(res.data[0].categoryName);
    } catch (err) {
      console.error('Failed to load categories:', err);
    }
  };

  const handleCalculate = async () => {
    if (!selectedCategory || !desiredIncome) return;
    setLoading(true);
    setError(null);
    try {
      const res = await analysisAPI.goalSeek({
        businessCategory: selectedCategory,
        desiredMonthlyIncome: parseFloat(desiredIncome),
      });
      setResult(res.data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#f9fafb]">
      <TopNav subtitle="Business Advisory" />

      <main className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Title */}
        <div className="mb-8 text-center">
          <h2 className="text-2xl font-bold text-gray-900">{t('goalSeek.title')}</h2>
          <p className="text-gray-500 mt-1">{t('goalSeek.subtitle')}</p>
        </div>

        {/* Form */}
        <div className="card mb-8">
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('goalSeek.categoryLabel')}</label>
              <select
                value={selectedCategory}
                onChange={(e) => setSelectedCategory(e.target.value)}
                className="input-field"
                disabled={loading}
              >
                {categories.map(c => (
                  <option key={c.categoryName} value={c.categoryName}>{c.categoryName}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('goalSeek.incomeLabel')}</label>
              <input
                type="number"
                value={desiredIncome}
                onChange={(e) => setDesiredIncome(e.target.value)}
                placeholder={t('goalSeek.incomePlaceholder')}
                className="input-field"
                disabled={loading}
                min="1000"
              />
            </div>

            <button
              onClick={handleCalculate}
              disabled={loading || !selectedCategory || !desiredIncome}
              className="btn-primary w-full py-3 flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <Loader2 className="w-5 h-5 animate-spin" />
                  {t('goalSeek.loading')}
                </>
              ) : (
                <>
                  <Calculator className="w-5 h-5" />
                  {t('goalSeek.calculateBtn')}
                </>
              )}
            </button>

            {error && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
                {error}
              </div>
            )}
          </div>
        </div>

        {/* Results */}
        {result && (
          <div className="card animate-fade-in">
            <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <Target className="w-5 h-5 text-primary-600" />
              {t('goalSeek.results.title')}
            </h3>
            
            <div className="grid md:grid-cols-2 gap-4 mb-4">
              <div className="p-4 bg-primary-50 rounded-lg border border-primary-200">
                <p className="text-sm text-primary-700">{t('goalSeek.results.requiredRevenue')}</p>
                <p className="text-2xl font-bold text-primary-600 mt-1">{formatCurrency(result.requiredMonthlyRevenue)}</p>
              </div>
              <div className="p-4 bg-green-50 rounded-lg border border-green-200">
                <p className="text-sm text-green-700">{t('goalSeek.results.projectCost')}</p>
                <p className="text-2xl font-bold text-green-600 mt-1">{formatCurrency(result.estimatedProjectCost)}</p>
              </div>
              <div className="p-4 bg-blue-50 rounded-lg border border-blue-200">
                <p className="text-sm text-blue-700">{t('goalSeek.results.marginRequired')}</p>
                <p className="text-2xl font-bold text-blue-600 mt-1">{formatCurrency(result.estimatedMarginRequired)}</p>
              </div>
              <div className="p-4 bg-purple-50 rounded-lg border border-purple-200">
                <p className="text-sm text-purple-700">{t('goalSeek.results.loanRequired')}</p>
                <p className="text-2xl font-bold text-purple-600 mt-1">{formatCurrency(result.estimatedLoanRequired)}</p>
              </div>
            </div>

            <div className="p-4 bg-warning-50 rounded-lg border border-warning-200">
              <p className="text-sm text-warning-800 flex items-center gap-1.5">
                <TrendingUp className="w-4 h-4" />
                {t('goalSeek.results.note')}
              </p>
            </div>

            <div className="mt-4 p-4 bg-gray-50 rounded-lg border border-gray-200">
              <p className="text-sm text-gray-600">
                <strong>How this works:</strong> The calculation scales the reference project cost proportionally 
                to achieve your desired monthly income, based on the category's typical revenue and operating cost ratios.
              </p>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
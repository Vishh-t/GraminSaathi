import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { analysisAPI } from '../services/api';
import { useI18n } from '../i18n/i18n';
import { useAnalysisContext } from '../context/AnalysisContext';
import TopNav from '../components/TopNav';
import VillageAutocomplete from '../components/VillageAutocomplete';
import { formatCurrency } from '../utils/format';
import { getErrorMessage } from '../utils/errors';
import { Loader2, TrendingUp, ArrowRight, Target, Store } from 'lucide-react';

export default function DiscoveryPage() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { village: ctxVillage, capital: ctxCapital, updateContext } = useAnalysisContext();
  const [selectedVillage, setSelectedVillage] = useState('');
  const [capital, setCapital] = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    // URL params win (a real deep link); otherwise fall back to whatever was
    // last used elsewhere in the app.
    setSelectedVillage(searchParams.get('village') || ctxVillage || '');
    setCapital(searchParams.get('capital') || ctxCapital || '');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleDiscover = async () => {
    if (!selectedVillage || !capital) return;
    setLoading(true);
    setError(null);
    try {
      const res = await analysisAPI.discover({
        villageName: selectedVillage,
        availableMarginCapital: parseFloat(capital),
      });
      setResults(res.data.businesses);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleAnalyze = (business) => {
    updateContext({ village: selectedVillage, category: business.categoryName, capital });
    navigate(`/analysis?village=${selectedVillage}&category=${business.categoryName}&capital=${capital}`);
  };

  return (
    <div className="min-h-screen bg-[#f9fafb]">
      <TopNav subtitle="Business Advisory" />

      <main className="max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4 mb-8">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">{t('discovery.title')}</h2>
            <p className="text-gray-500 mt-1">{t('discovery.subtitle')}</p>
          </div>
        </div>

        <div className="grid lg:grid-cols-[280px_1fr] gap-6">
          {/* Filters sidebar */}
          <aside className="card h-fit lg:sticky lg:top-24">
            <h3 className="font-semibold text-gray-900 mb-4 pb-3 border-b border-gray-200">Filters</h3>

            <div className="mb-5">
              <label className="block text-sm font-medium text-gray-700 mb-1.5">{t('discovery.villageLabel')}</label>
              <VillageAutocomplete
                value={selectedVillage}
                onChange={(name) => setSelectedVillage(name)}
                disabled={loading}
                placeholder="Search for a village..."
              />
            </div>

            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-1.5">{t('discovery.capitalLabel')}</label>
              <input
                type="number"
                value={capital}
                onChange={(e) => setCapital(e.target.value)}
                placeholder={t('discovery.capitalPlaceholder')}
                className="input-field"
                disabled={loading}
                min="1000"
              />
            </div>

            <button
              onClick={handleDiscover}
              disabled={loading || !selectedVillage || !capital}
              className="btn-primary w-full"
            >
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <Loader2 className="w-5 h-5 animate-spin" />
                  {t('discovery.loading')}
                </span>
              ) : (
                <span className="flex items-center justify-center gap-2">
                  <Target className="w-5 h-5" />
                  {t('discovery.discoverBtn')}
                </span>
              )}
            </button>

            {error && (
              <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
                {error}
              </div>
            )}
          </aside>

          {/* Results grid */}
          <div>
            {results.length > 0 && (
              <>
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-semibold text-gray-900">{t('discovery.resultsTitle')}</h3>
                  <span className="pill bg-surface-container text-gray-600">Sort: Highest Score</span>
                </div>
                <div className="grid sm:grid-cols-2 xl:grid-cols-3 gap-5">
                  {results.map((business) => {
                    // marketScore (real demand-vs-competition data) is null until step 2b/HCES land for
                    // this village - fall back to personFitScore (always computable) so the card still
                    // has a meaningful color/score instead of always reading as "undefined"/red.
                    const displayScore = business.marketScore ?? business.personFitScore;
                    const fitPillColor = business.personFitLabel === 'Strong fit'
                      ? 'bg-green-100 text-green-700'
                      : business.personFitLabel === 'Moderate fit'
                        ? 'bg-yellow-100 text-yellow-700'
                        : 'bg-red-100 text-red-700';
                    return (
                    <div
                      key={business.categoryName}
                      className="bg-white rounded-lg border border-gray-200 border-l-4 shadow-sm overflow-hidden hover:shadow-md transition-shadow flex flex-col"
                      style={{ borderLeftColor: displayScore >= 75 ? '#16a34a' : displayScore >= 50 ? '#f59e0b' : '#dc2626' }}
                    >
                      <div className="h-28 bg-gradient-to-br from-primary-700 to-primary-500 flex items-center justify-center relative">
                        <Store className="w-10 h-10 text-white/70" />
                        <span className="absolute top-2 right-2 pill bg-black/40 text-white backdrop-blur-sm text-[11px]">
                          {business.marketScore != null ? `Market: ${business.marketScore}/100` : `Fit: ${business.personFitScore}/100`}
                        </span>
                      </div>
                      <div className="p-4 flex flex-col flex-1">
                        <div className="flex flex-wrap gap-1.5 mb-2">
                          <span className={`pill text-[11px] ${
                            business.affordabilityFlag === 'Within budget' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
                          }`}>
                            {business.affordabilityFlag}
                          </span>
                          <span className={`pill text-[11px] ${fitPillColor}`}>
                            {business.personFitLabel}
                          </span>
                        </div>
                        <h4 className="text-lg font-bold text-gray-900 mb-1">{business.categoryName}</h4>
                        <p className="text-sm text-gray-500 mb-3 flex-1">
                          {business.marketExplanation || 'Fit assessed on your profile; market demand data isn\'t available for this village yet.'}
                        </p>

                        <div className="grid grid-cols-2 gap-y-2 gap-x-3 text-xs text-gray-600 mb-4 pt-3 border-t border-gray-100">
                          <div>
                            <p className="text-gray-400">Monthly Revenue</p>
                            <p className="font-semibold text-gray-900">{formatCurrency(business.referenceMonthlyRevenue)}</p>
                          </div>
                          <div>
                            <p className="text-gray-400">Monthly Cost</p>
                            <p className="font-semibold text-gray-900">{formatCurrency(business.referenceMonthlyOperatingCost)}</p>
                          </div>
                        </div>

                        <button
                          onClick={() => handleAnalyze(business)}
                          className="btn-primary w-full flex items-center justify-center gap-2 !py-2 text-sm"
                        >
                          <ArrowRight className="w-4 h-4" />
                          <span>{t('discovery.analyzeBtn')}</span>
                        </button>
                      </div>
                    </div>
                    );
                  })}
                </div>
              </>
            )}

            {results.length === 0 && !loading && capital && (
              <div className="card text-center py-16">
                <Target className="w-16 h-16 mx-auto text-gray-300 mb-4" />
                <p className="text-gray-500">{t('discovery.noResults')}</p>
              </div>
            )}

            {results.length === 0 && !loading && !capital && (
              <div className="card text-center py-16">
                <TrendingUp className="w-16 h-16 mx-auto text-gray-300 mb-4" />
                <p className="text-gray-500">Set your village and available capital, then discover tailored business opportunities.</p>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}

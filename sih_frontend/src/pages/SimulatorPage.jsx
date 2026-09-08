import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { analysisAPI, referenceAPI } from '../services/api';
import { useI18n } from '../i18n/i18n';
import TopNav from '../components/TopNav';
import SurvivalChart from '../components/SurvivalChart';
import { formatCurrency } from '../utils/format';
import { getErrorMessage } from '../utils/errors';
import { Loader2, AlertTriangle, CheckCircle, Play, RotateCcw } from 'lucide-react';

const shockOptions = [
  { value: 0, label: '0%' },
  { value: -0.1, label: '-10%' },
  { value: -0.2, label: '-20%' },
  { value: -0.3, label: '-30%' },
];

const costShockOptions = [
  { value: 0, label: '0%' },
  { value: 0.1, label: '+10%' },
  { value: 0.2, label: '+20%' },
  { value: 0.3, label: '+30%' },
];

export default function SimulatorPage() {
  const { t } = useI18n();
  const [searchParams] = useSearchParams();
  const [village, setVillage] = useState('');
  const [category, setCategory] = useState('');
  const [capital, setCapital] = useState('');
  const [revenueShock, setRevenueShock] = useState(0);
  const [costShock, setCostShock] = useState(0);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [error, setError] = useState(null);
  const [villages, setVillages] = useState([]);
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    loadReferenceData();
    if (searchParams.get('village')) setVillage(searchParams.get('village'));
    if (searchParams.get('category')) setCategory(searchParams.get('category'));
    if (searchParams.get('capital')) setCapital(searchParams.get('capital'));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadReferenceData = async () => {
    try {
      const [vRes, cRes] = await Promise.all([
        referenceAPI.getVillages(),
        referenceAPI.getBusinessCategories(),
      ]);
      setVillages(vRes.data);
      setCategories(cRes.data);
      if (vRes.data.length > 0 && !village) setVillage(vRes.data[0].villageName);
      if (cRes.data.length > 0 && !category) setCategory(cRes.data[0].categoryName);
      setInitialLoading(false);
      runSimulation();
    } catch (err) {
      setInitialLoading(false);
    }
  };

  const runSimulation = async () => {
    if (!village || !category || !capital) return;
    setLoading(true);
    setError(null);
    try {
      const res = await analysisAPI.simulate({
        villageName: village,
        businessCategory: category,
        availableMarginCapital: parseFloat(capital),
        revenueShockPct: revenueShock,
        costShockPct: costShock,
      });
      setResult(res.data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleShockChange = () => {
    clearTimeout(window.simDebounce);
    window.simDebounce = setTimeout(runSimulation, 300);
  };

  const handleReset = () => {
    setRevenueShock(0);
    setCostShock(0);
    clearTimeout(window.simDebounce);
    window.simDebounce = setTimeout(runSimulation, 100);
  };

  if (initialLoading) {
    return (
      <div className="min-h-screen bg-[#f9fafb] flex items-center justify-center">
        <Loader2 className="w-12 h-12 animate-spin text-primary-600" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#f9fafb]">
      <TopNav subtitle="Business Advisory" />

      <main className="max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid lg:grid-cols-[300px_1fr] gap-6">
          {/* Left control panel */}
          <aside className="card h-fit lg:sticky lg:top-24">
            <h2 className="text-lg font-bold text-gray-900">{t('simulator.title')}</h2>
            <p className="text-sm text-gray-500 mb-5">{t('simulator.subtitle')}</p>

            <div className="space-y-4 mb-5 pb-5 border-b border-gray-100">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">{t('discovery.villageLabel')}</label>
                <select
                  value={village}
                  onChange={(e) => { setVillage(e.target.value); handleShockChange(); }}
                  className="input-field"
                  disabled={loading}
                >
                  {villages.map(v => (
                    <option key={v.villageName} value={v.villageName}>{v.villageName}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">{t('goalSeek.categoryLabel')}</label>
                <select
                  value={category}
                  onChange={(e) => { setCategory(e.target.value); handleShockChange(); }}
                  className="input-field"
                  disabled={loading}
                >
                  {categories.map(c => (
                    <option key={c.categoryName} value={c.categoryName}>{c.categoryName}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">{t('discovery.capitalLabel')}</label>
                <input
                  type="number"
                  value={capital}
                  onChange={(e) => { setCapital(e.target.value); handleShockChange(); }}
                  placeholder={t('discovery.capitalPlaceholder')}
                  className="input-field"
                  disabled={loading}
                  min="1000"
                />
              </div>
            </div>

            <div className="mb-5">
              <div className="flex items-center justify-between mb-1">
                <label className="text-sm font-medium text-gray-700">{t('simulator.revenueShock')}</label>
                <span className="text-sm font-semibold text-primary-700">{(revenueShock * 100).toFixed(0)}%</span>
              </div>
              <input
                type="range"
                min={-0.3}
                max={0}
                step={0.05}
                value={revenueShock}
                onChange={(e) => { setRevenueShock(parseFloat(e.target.value)); handleShockChange(); }}
                className="w-full accent-primary-600"
                disabled={loading}
              />
              <div className="flex justify-between text-[11px] text-gray-400">
                <span>Baseline</span>
                <span>-30% (Severe)</span>
              </div>
            </div>

            <div className="mb-6">
              <div className="flex items-center justify-between mb-1">
                <label className="text-sm font-medium text-gray-700">{t('simulator.costShock')}</label>
                <span className="text-sm font-semibold text-primary-700">+{(costShock * 100).toFixed(0)}%</span>
              </div>
              <input
                type="range"
                min={0}
                max={0.3}
                step={0.05}
                value={costShock}
                onChange={(e) => { setCostShock(parseFloat(e.target.value)); handleShockChange(); }}
                className="w-full accent-primary-600"
                disabled={loading}
              />
              <div className="flex justify-between text-[11px] text-gray-400">
                <span>Baseline</span>
                <span>+30% (Severe)</span>
              </div>
            </div>

            <button onClick={runSimulation} disabled={loading} className="btn-primary w-full flex items-center justify-center gap-2 mb-2">
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Play className="w-4 h-4" />}
              {t('simulator.applyBtn')}
            </button>
            <button onClick={handleReset} className="btn-secondary w-full flex items-center justify-center gap-2">
              <RotateCcw className="w-4 h-4" />
              Reset Baseline
            </button>
          </aside>

          {/* Right content */}
          <div className="space-y-6">
            {result && (
              <>
                {/* Verdict Banner */}
                <div className={`rounded-lg p-5 flex items-center justify-between gap-4 flex-wrap text-white ${
                  result.deficitMonth ? 'bg-gradient-to-r from-warning-600 to-warning-500' : 'bg-gradient-to-r from-primary-700 to-primary-600'
                }`}>
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-white/20 flex items-center justify-center flex-shrink-0">
                      {result.deficitMonth ? <AlertTriangle className="w-5 h-5" /> : <CheckCircle className="w-5 h-5" />}
                    </div>
                    <div>
                      <p className="font-semibold">
                        {result.deficitMonth ? 'Cash-Flow Deficit Ahead' : 'Business Survives'}
                      </p>
                      <p className="text-sm text-white/85 mt-0.5">
                        {result.deficitMonth
                          ? `${t('simulator.verdict.deficit')}${result.deficitMonth} under this scenario`
                          : 'Cash flow remains positive for 24 months.'}
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="text-[11px] uppercase tracking-wide text-white/70">Min Cash Reserve</p>
                    <p className="text-xl font-bold">
                      {formatCurrency(Math.min(...result.cashCurve.map(c => c.cumulativeCash)))}
                    </p>
                  </div>
                </div>

                <SurvivalChart cashCurve={result.cashCurve} deficitMonth={result.deficitMonth} />

                <div className="grid sm:grid-cols-3 gap-4">
                  <div className="card border-l-4 border-l-primary-600 text-center">
                    <p className="text-xs uppercase tracking-wide text-gray-400">Starting Capital</p>
                    <p className="text-xl font-bold text-gray-900 mt-1">{formatCurrency(result.cashCurve[0]?.cumulativeCash || 0)}</p>
                  </div>
                  <div className="card text-center">
                    <p className="text-xs uppercase tracking-wide text-gray-400">Month 12</p>
                    <p className={`text-xl font-bold mt-1 ${(result.cashCurve[11]?.cumulativeCash || 0) >= 0 ? 'text-primary-700' : 'text-red-600'}`}>
                      {formatCurrency(result.cashCurve[11]?.cumulativeCash || 0)}
                    </p>
                  </div>
                  <div className="card text-center">
                    <p className="text-xs uppercase tracking-wide text-gray-400">Deficit Month</p>
                    <p className={`text-xl font-bold mt-1 ${result.deficitMonth ? 'text-red-600' : 'text-primary-700'}`}>
                      {result.deficitMonth ? result.deficitMonth : 'None'}
                    </p>
                  </div>
                </div>
              </>
            )}

            {error && (
              <div className="card p-6 text-center text-red-600">
                {error}
                <button onClick={runSimulation} className="mt-4 btn-primary">{t('common.retry')}</button>
              </div>
            )}

            {!result && !error && (
              <div className="card text-center py-16 text-gray-500">
                Set your village, business, and capital on the left to run the survival simulation.
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}

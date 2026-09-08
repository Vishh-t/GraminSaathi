import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { referenceAPI } from '../services/api';
import { useI18n } from '../i18n/i18n';
import TopNav from '../components/TopNav';
import BusinessMap from '../components/BusinessMap';
import { Loader2, MapPin, ArrowLeft, Building2, BarChart2 } from 'lucide-react';
import { formatNumber } from '../utils/format';

export default function MapPage() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [villageData, setVillageData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const village = searchParams.get('village');
  const category = searchParams.get('category');

  useEffect(() => {
    if (village) {
      loadVillageData();
    } else {
      setError('No village specified');
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [village]);

  const loadVillageData = async () => {
    setLoading(true);
    try {
      const res = await referenceAPI.getVillages();
      const v = res.data.find(v => v.villageName === village);
      if (v) {
        setVillageData(v);
      } else {
        setError('Village not found');
      }
    } catch (err) {
      setError(t('errors.network'));
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#f9fafb] flex items-center justify-center">
        <Loader2 className="w-12 h-12 animate-spin text-primary-600" />
      </div>
    );
  }

  if (error || !villageData) {
    return (
      <div className="min-h-screen bg-[#f9fafb] flex items-center justify-center">
        <div className="text-center p-8">
          <p className="text-red-600">{error || 'Failed to load map'}</p>
          <button onClick={() => navigate(-1)} className="mt-4 btn-primary">
            <ArrowLeft className="w-4 h-4 mr-2" />
            Go Back
          </button>
        </div>
      </div>
    );
  }

  const businessData = villageData.businessData[category] || { competitorCount: 0, avgLocalPrice: null };

  return (
    <div className="min-h-screen bg-[#f9fafb]">
      <TopNav subtitle="Business Advisory" />

      <main className="max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center gap-3 mb-6">
          <button onClick={() => navigate(-1)} className="btn-ghost p-2">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h2 className="text-2xl font-bold text-gray-900">{t('map.title')}</h2>
            <p className="text-gray-500 mt-1">{t('map.subtitle')} &mdash; {villageData.villageName}{category ? ` \u00b7 ${category}` : ''}</p>
          </div>
        </div>

        <div className="grid lg:grid-cols-4 gap-6">
          {/* Map with floating insight card */}
          <div className="lg:col-span-3 relative rounded-lg overflow-hidden border border-gray-200">
            <BusinessMap
              village={villageData}
              competitorCount={businessData.competitorCount}
            />

            <div className="absolute top-4 right-4 w-72 max-w-[80vw] bg-white rounded-lg shadow-lg border border-gray-200 overflow-hidden hidden sm:block">
              <div className="bg-primary-700 text-white px-4 py-3 flex items-center gap-2">
                <Building2 className="w-4 h-4" />
                <h3 className="font-semibold text-sm">Market Insight</h3>
              </div>
              <div className="p-4 space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-500">Selected Village</span>
                  <span className="font-semibold">{villageData.villageName}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-gray-500 flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-red-500" /> Competitors {category ? `(${category})` : ''}
                  </span>
                  <span className="font-semibold text-red-600">{businessData.competitorCount}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">Est. Population</span>
                  <span className="font-semibold">{formatNumber(villageData.population5kmRadius)}</span>
                </div>
                <button
                  onClick={() => navigate(`/analysis?village=${village}&category=${category}`)}
                  className="btn-primary w-full !py-2 text-sm mt-1"
                >
                  View Detailed Report
                </button>
              </div>
            </div>

            <div className="absolute bottom-4 left-4 bg-white rounded-lg shadow border border-gray-200 px-3 py-2 text-xs text-gray-600 max-w-[280px]">
              <div className="flex items-center gap-3 mb-1">
                <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-blue-600 inline-block" /> {t('map.legend.village')}</span>
                <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-red-500 inline-block" /> {t('map.legend.competitor')}</span>
              </div>
              <p className="text-gray-400">{t('map.disclaimer')}. Radius: 5km.</p>
            </div>
          </div>

          {/* Info Panel */}
          <div className="space-y-4">
            <div className="card">
              <h3 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <MapPin className="w-5 h-5 text-primary-600" />
                Village Details
              </h3>
              <div className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-500">District</span>
                  <span className="font-medium">{villageData.district}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">State</span>
                  <span className="font-medium">{villageData.state}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">Population (5km)</span>
                  <span className="font-medium">{formatNumber(villageData.population5kmRadius)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">Households (5km)</span>
                  <span className="font-medium">{formatNumber(villageData.households5kmRadius)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">Coordinates</span>
                  <span className="font-medium font-mono text-xs">
                    {villageData.latitude.toFixed(4)}, {villageData.longitude.toFixed(4)}
                  </span>
                </div>
              </div>
            </div>

            <div className="card">
              <h3 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <Building2 className="w-5 h-5 text-primary-600" />
                Competitor Info
              </h3>
              <div className="space-y-3 text-sm">
                <div className="flex justify-between items-center">
                  <span className="text-gray-500">Competitors</span>
                  <span className="font-bold text-2xl text-gray-900">{businessData.competitorCount}</span>
                </div>
                {businessData.avgLocalPrice && (
                  <div className="flex justify-between">
                    <span className="text-gray-500">Avg Local Price</span>
                    <span className="font-medium">₹{businessData.avgLocalPrice}</span>
                  </div>
                )}
                <div className="p-3 bg-surface-low rounded-lg border border-gray-200">
                  <p className="text-xs text-gray-600">
                    {t('map.disclaimer')}
                  </p>
                </div>
              </div>
            </div>

            <div className="card">
              <h3 className="font-semibold text-gray-900 mb-4">Quick Actions</h3>
              <div className="space-y-2">
                <button
                  onClick={() => navigate(`/analysis?village=${village}&category=${category}`)}
                  className="btn-secondary w-full justify-start"
                >
                  <MapPin className="w-4 h-4 mr-2" />
                  View Full Analysis
                </button>
                <button
                  onClick={() => navigate(`/simulator?village=${village}&category=${category}`)}
                  className="btn-secondary w-full justify-start"
                >
                  <BarChart2 className="w-4 h-4 mr-2" />
                  Run Simulation
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

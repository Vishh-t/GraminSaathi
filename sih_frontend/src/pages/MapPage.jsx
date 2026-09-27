import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { referenceAPI, villageAPI } from '../services/api';
import { useI18n } from '../i18n/i18n';
import { useAnalysisContext } from '../context/AnalysisContext';
import TopNav from '../components/TopNav';
import Select from '../components/Select';
import VillageAutocomplete from '../components/VillageAutocomplete';
import BusinessMap from '../components/BusinessMap';
import { Loader2, MapPin, Building2, BarChart2, AlertTriangle, SlidersHorizontal, RotateCcw, Users } from 'lucide-react';
import { formatNumber } from '../utils/format';
import { getErrorMessage } from '../utils/errors';

// Rewired 2026-09-27 off the real ~633k-village Census/LGD search table (VillageController) - previously
// this page's village picker only ever showed the ~4 hardcoded demo villages (referenceAPI.getVillages()).
// See GraminSaathi_Backend/Project_Docs/RECOMMENDATION_ENGINE_BUILD_LOG.md ("part 7") for the full context.
//
// The real VillageSearchResponse shape (id, name, block, district, state, latitude, longitude,
// population2011, households2011, source) is different from the old demo VillageResponse shape
// (population5kmRadius, households5kmRadius, businessData with a per-category competitor count/avg
// price) - the info panels below show what the real endpoint actually has (Census 2011 figures) rather
// than pretending the 5km-radius/competitor numbers still exist. Real per-village, per-category
// competitor locations aren't exposed by any API yet (scoped as "Step 2b-extended" in the build log,
// not started) - the competitor panel and map say so honestly instead of showing a fabricated count.
export default function MapPage() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { village: ctxVillage, category: ctxCategory, capital: ctxCapital, updateContext } = useAnalysisContext();

  const [categories, setCategories] = useState([]);
  const [selectedVillage, setSelectedVillage] = useState(searchParams.get('village') || ctxVillage || '');
  const [selectedVillageData, setSelectedVillageData] = useState(null);
  const [selectedCategory, setSelectedCategory] = useState(searchParams.get('category') || ctxCategory || '');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadReferenceData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadReferenceData = async () => {
    setLoading(true);
    setError(null);
    try {
      const cRes = await referenceAPI.getBusinessCategories();
      setCategories(cRes.data);

      const initialCategory = searchParams.get('category') || ctxCategory || cRes.data[0]?.categoryName || '';
      setSelectedCategory(initialCategory);

      // A deep link/context only ever carries a village NAME, not the full real-village record this
      // page needs for its info panels - look it up by exact name against the real search table.
      const initialVillageName = searchParams.get('village') || ctxVillage || '';
      if (initialVillageName) {
        await loadVillageByName(initialVillageName);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const loadVillageByName = async (name) => {
    try {
      const res = await villageAPI.search(name, undefined, 5);
      const exact = res.data.find(v => v.name.toLowerCase() === name.toLowerCase()) || res.data[0] || null;
      setSelectedVillage(name);
      setSelectedVillageData(exact);
    } catch (err) {
      console.error('Failed to look up village:', err);
      setSelectedVillageData(null);
    }
  };

  const handleVillageChange = (name, villageObj) => {
    setSelectedVillage(name);
    setSelectedVillageData(villageObj);
    if (villageObj) {
      updateContext({ village: name });
      setSearchParams(prev => {
        const next = new URLSearchParams(prev);
        next.set('village', name);
        return next;
      });
    }
  };

  const handleCategoryChange = (c) => {
    setSelectedCategory(c);
    updateContext({ category: c });
    setSearchParams(prev => {
      const next = new URLSearchParams(prev);
      next.set('category', c);
      return next;
    });
  };

  const handleReset = () => {
    const firstCategory = categories[0]?.categoryName || '';
    setSelectedVillage('');
    setSelectedVillageData(null);
    setSelectedCategory(firstCategory);
    setSearchParams(prev => {
      const next = new URLSearchParams(prev);
      next.delete('village');
      next.set('category', firstCategory);
      return next;
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#f9fafb] flex items-center justify-center">
        <Loader2 className="w-12 h-12 animate-spin text-primary-600" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-[#f9fafb]">
        <TopNav subtitle="Business Advisory" />
        <div className="max-w-xl mx-auto px-4 py-20 text-center">
          <AlertTriangle className="w-14 h-14 mx-auto text-gray-300 mb-4" />
          <h2 className="text-lg font-semibold text-gray-900 mb-2">Couldn't load the map</h2>
          <p className="text-gray-500 mb-6">{error} — check the backend connection and try again.</p>
          <button onClick={loadReferenceData} className="btn-primary">{t('common.retry')}</button>
        </div>
      </div>
    );
  }

  const villageData = selectedVillageData;
  const hasCoordinates = villageData?.latitude != null && villageData?.longitude != null;

  return (
    <div className="min-h-screen bg-[#f9fafb]">
      <TopNav subtitle="Business Advisory" />

      <main className="max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-6">
          <h2 className="text-2xl font-bold text-gray-900">{t('map.title')}</h2>
          <p className="text-gray-500 mt-1">{t('map.subtitle')}</p>
        </div>

        <div className="grid lg:grid-cols-[260px_1fr] gap-6">
          {/* Filters sidebar — kept separate from the map/info-panel columns
              so an open dropdown only ever overlaps empty space in its own
              narrow column, never the cards on the right. */}
          <aside className="card h-fit lg:sticky lg:top-24 overflow-hidden !p-0">
            <div className="bg-gradient-to-br from-primary-700 to-primary-600 px-5 py-4 flex items-center justify-between">
              <div className="flex items-center gap-2 text-white">
                <SlidersHorizontal className="w-4 h-4" />
                <h3 className="font-semibold">Filters</h3>
              </div>
              <button
                onClick={handleReset}
                className="flex items-center gap-1 text-xs text-primary-100 hover:text-white transition-colors"
                title="Reset to defaults"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                Reset
              </button>
            </div>

            <div className="p-5">
              <div className="mb-5">
                <label className="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1.5">
                  <MapPin className="w-3.5 h-3.5 text-primary-600" />
                  {t('discovery.villageLabel')}
                </label>
                <VillageAutocomplete
                  value={selectedVillage}
                  onChange={handleVillageChange}
                  placeholder="Search for a village..."
                />
              </div>

              <div className="mb-5">
                <label className="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1.5">
                  <Building2 className="w-3.5 h-3.5 text-primary-600" />
                  {t('goalSeek.categoryLabel')}
                </label>
                <Select
                  value={selectedCategory}
                  onChange={handleCategoryChange}
                  placeholder="Select a business"
                  options={categories.map(c => ({ value: c.categoryName, label: c.categoryName }))}
                />
              </div>

              {villageData && (
                <div className="pt-4 border-t border-gray-100">
                  <p className="text-xs font-medium text-gray-400 uppercase tracking-wide mb-2">Currently viewing</p>
                  <div className="rounded-lg bg-primary-50 border border-primary-100 p-3 space-y-2">
                    <div className="flex items-center gap-2 text-sm font-semibold text-primary-800">
                      <MapPin className="w-4 h-4 shrink-0" />
                      <span className="truncate">{villageData.name}, {villageData.district}</span>
                    </div>
                    {selectedCategory && (
                      <div className="flex items-center gap-2 text-sm text-primary-700">
                        <Building2 className="w-4 h-4 shrink-0" />
                        <span className="truncate">{selectedCategory}</span>
                      </div>
                    )}
                    {villageData.population2011 != null && (
                      <div className="flex items-center gap-2 text-sm text-primary-700">
                        <Users className="w-4 h-4 shrink-0" />
                        <span>{formatNumber(villageData.population2011)} people (Census 2011)</span>
                      </div>
                    )}
                  </div>
                </div>
              )}

              <p className="text-xs text-gray-400 mt-4">
                Search across all Census villages · {categories.length} business types
              </p>
            </div>
          </aside>

          {!villageData ? (
            <div className="card text-center py-16">
              <MapPin className="w-16 h-16 mx-auto text-gray-300 mb-4" />
              <p className="text-gray-500">Search for and pick a village to see it on the map.</p>
            </div>
          ) : (
            <div className="grid lg:grid-cols-4 gap-6">
              {/* Map with floating insight card */}
              <div className="lg:col-span-3 relative rounded-lg overflow-hidden border border-gray-200 shadow-sm h-[420px] sm:h-[520px]">
                {hasCoordinates ? (
                  <>
                    <BusinessMap
                      village={villageData}
                      competitorCount={null}
                    />
                    <div className="absolute bottom-4 left-4 bg-white rounded-lg shadow border border-gray-200 px-3 py-2 text-xs text-gray-600 max-w-[280px] z-[1000]">
                      <div className="flex items-center gap-3">
                        <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-blue-600 inline-block" /> {t('map.legend.village')}</span>
                      </div>
                    </div>
                  </>
                ) : (
                  <div className="h-full flex items-center justify-center bg-gray-50">
                    <div className="text-center text-gray-500 p-4">
                      <MapPin className="w-12 h-12 mx-auto mb-2 text-gray-300" />
                      <p>Location data not available yet for this village</p>
                      <p className="text-sm mt-1">Coordinates haven't been matched for every real village yet.</p>
                    </div>
                  </div>
                )}
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
                      <span className="text-gray-500">Population (Census 2011)</span>
                      <span className="font-medium">{villageData.population2011 != null ? formatNumber(villageData.population2011) : 'Not available'}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-500">Households (Census 2011)</span>
                      <span className="font-medium">{villageData.households2011 != null ? formatNumber(villageData.households2011) : 'Not available'}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-500">Coordinates</span>
                      <span className="font-medium font-mono text-xs">
                        {hasCoordinates ? `${villageData.latitude.toFixed(4)}, ${villageData.longitude.toFixed(4)}` : 'Not available'}
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
                      <span className="text-sm text-gray-400 italic">Not available yet</span>
                    </div>
                    <p className="text-xs text-gray-400">
                      Per-category competitor locations for real villages aren't wired up yet — check the
                      Discovery page's market score for a data-backed estimate where it's available.
                    </p>
                  </div>
                </div>

                <div className="card">
                  <h3 className="font-semibold text-gray-900 mb-4">Quick Actions</h3>
                  <div className="space-y-2">
                    <button
                      onClick={() => navigate(`/analysis?village=${selectedVillage}&category=${selectedCategory}${ctxCapital ? `&capital=${ctxCapital}` : ''}`)}
                      className="btn-secondary w-full justify-start"
                    >
                      <MapPin className="w-4 h-4" />
                      View Full Analysis
                    </button>
                    <button
                      onClick={() => navigate(`/simulator?village=${selectedVillage}&category=${selectedCategory}${ctxCapital ? `&capital=${ctxCapital}` : ''}`)}
                      className="btn-secondary w-full justify-start"
                    >
                      <BarChart2 className="w-4 h-4" />
                      Run Simulation
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

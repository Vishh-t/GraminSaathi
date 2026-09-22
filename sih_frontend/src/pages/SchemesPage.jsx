import { useState, useEffect, useCallback } from 'react';
import { schemesAPI, profileAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useI18n } from '../i18n/i18n';
import TopNav from '../components/TopNav';
import Select from '../components/Select';
import SchemeMatchCard from '../components/SchemeMatchCard';
import SchemeDetailModal from '../components/SchemeDetailModal';
import ApplicantIntakeModal from '../components/intake/ApplicantIntakeModal';
import { SECTOR_OPTIONS, STATE_OPTIONS } from '../components/intake/constants';
import { getErrorMessage } from '../utils/errors';
import { Loader2, Landmark, ChevronDown, UserCog, SearchX } from 'lucide-react';

const LEVEL_OPTIONS = [
  { value: 'central', label: 'Central' },
  { value: 'state', label: 'State' },
];

const CALCULATION_TYPE_OPTIONS = [
  { value: 'flat_grant_or_cap', label: 'Flat grant' },
  { value: 'pct_of_cost_capped', label: '% of project cost' },
  { value: 'pct_of_loan_capped', label: '% of loan (capped)' },
  { value: 'pct_of_loan_uncapped', label: '% of loan (uncapped)' },
  { value: 'per_unit_or_in_kind', label: 'Per-unit / in-kind' },
  { value: 'interest_rate_reduction', label: 'Interest rate reduction' },
  { value: 'guarantee_cover_pct', label: 'Loan guarantee' },
  { value: 'equity_or_uncapped', label: 'Equity / funding' },
  { value: 'composite', label: 'Multiple benefits' },
];

const PREVIEW_COUNT = 6;

/**
 * Full-schemes-browse page (status doc section 6, Phase C): every scheme the applicant is
 * eligible for, across every benefit kind - not just the 1-2 loans /api/analyze shows. Uses the
 * saved applicant profile (same one the intake modal writes) so results improve the moment
 * someone fills that in, without asking again here.
 */
export default function SchemesPage() {
  const { t } = useI18n();
  const { isAuthenticated } = useAuth();

  const [applicant, setApplicant] = useState(null);
  const [profileLoaded, setProfileLoaded] = useState(!isAuthenticated);
  const [filters, setFilters] = useState({ level: '', sector: '', state: '', calculationType: '' });
  const [matches, setMatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showAll, setShowAll] = useState(false);
  const [selectedMatch, setSelectedMatch] = useState(null);
  const [intakeOpen, setIntakeOpen] = useState(false);

  // Load the saved applicant profile once (logged-in users only - matches the intake modal's own source).
  useEffect(() => {
    if (!isAuthenticated) return;
    let cancelled = false;
    (async () => {
      try {
        const res = await profileAPI.getApplicant();
        if (!cancelled) setApplicant(res.data);
      } catch (err) {
        console.error('Failed to load applicant profile:', err);
      } finally {
        if (!cancelled) setProfileLoaded(true);
      }
    })();
    return () => { cancelled = true; };
  }, [isAuthenticated]);

  const loadMatches = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await schemesAPI.match({
        applicant: applicant || undefined,
        level: filters.level || undefined,
        sector: filters.sector || undefined,
        state: filters.state || undefined,
        calculationType: filters.calculationType || undefined,
      });
      setMatches(res.data.matches || []);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [applicant, filters]);

  useEffect(() => {
    if (!profileLoaded) return;
    setShowAll(false);
    loadMatches();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [profileLoaded, filters, applicant]);

  const updateFilter = (key, value) => setFilters((f) => ({ ...f, [key]: value }));
  const clearFilters = () => setFilters({ level: '', sector: '', state: '', calculationType: '' });
  const activeFilterCount = Object.values(filters).filter(Boolean).length;

  const visibleMatches = showAll ? matches : matches.slice(0, PREVIEW_COUNT);
  const filledFieldCount = applicant ? Object.values(applicant).filter((v) => v !== null && v !== undefined && v !== '').length : 0;

  return (
    <div className="min-h-screen bg-[#f9fafb]">
      <TopNav subtitle="Government Schemes" />

      <main className="max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4 mb-6">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">Browse Government Schemes</h2>
            <p className="text-gray-500 mt-1">
              Every loan, grant, subsidy and guarantee you're eligible for — not just the top matches.
            </p>
          </div>
          {isAuthenticated && (
            <button onClick={() => setIntakeOpen(true)} className="btn-secondary flex items-center gap-2 !py-2 text-sm">
              <UserCog className="w-4 h-4" />
              {filledFieldCount > 0 ? 'Update your profile' : 'Answer a few questions to unlock more matches'}
            </button>
          )}
        </div>

        <div className="grid lg:grid-cols-[260px_1fr] gap-6">
          {/* Filters sidebar */}
          <aside className="card h-fit lg:sticky lg:top-24">
            <div className="flex items-center justify-between mb-4 pb-3 border-b border-gray-200">
              <h3 className="font-semibold text-gray-900">Filters</h3>
              {activeFilterCount > 0 && (
                <button onClick={clearFilters} className="text-xs text-primary-600 hover:underline">Clear</button>
              )}
            </div>

            <div className="mb-5">
              <label className="block text-sm font-medium text-gray-700 mb-1.5">Level</label>
              <Select
                value={filters.level}
                onChange={(v) => updateFilter('level', v)}
                placeholder="Central or state"
                options={LEVEL_OPTIONS}
              />
            </div>

            <div className="mb-5">
              <label className="block text-sm font-medium text-gray-700 mb-1.5">State</label>
              <Select
                value={filters.state}
                onChange={(v) => updateFilter('state', v)}
                placeholder="Any state"
                options={STATE_OPTIONS}
              />
            </div>

            <div className="mb-5">
              <label className="block text-sm font-medium text-gray-700 mb-1.5">Sector</label>
              <Select
                value={filters.sector}
                onChange={(v) => updateFilter('sector', v)}
                placeholder="Any sector"
                options={SECTOR_OPTIONS}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">Benefit type</label>
              <Select
                value={filters.calculationType}
                onChange={(v) => updateFilter('calculationType', v)}
                placeholder="Any type"
                options={CALCULATION_TYPE_OPTIONS}
              />
            </div>
          </aside>

          {/* Results */}
          <div>
            {loading && (
              <div className="card text-center py-16">
                <Loader2 className="w-8 h-8 mx-auto text-primary-600 animate-spin mb-3" />
                <p className="text-gray-500">Finding schemes you're eligible for...</p>
              </div>
            )}

            {!loading && error && (
              <div className="card text-center py-16">
                <p className="text-red-600 mb-4">{error}</p>
                <button onClick={loadMatches} className="btn-secondary">{t('common.retry')}</button>
              </div>
            )}

            {!loading && !error && matches.length === 0 && (
              <div className="card text-center py-16">
                <SearchX className="w-14 h-14 mx-auto text-gray-300 mb-4" />
                <p className="text-gray-700 font-medium mb-1">No matching schemes found</p>
                <p className="text-gray-500 text-sm">
                  {activeFilterCount > 0
                    ? 'Try clearing a filter, or widen your search.'
                    : "Answer a few questions about yourself and your business to unlock more matches."}
                </p>
              </div>
            )}

            {!loading && !error && matches.length > 0 && (
              <>
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
                    <Landmark className="w-5 h-5 text-primary-600" />
                    {matches.length} scheme{matches.length === 1 ? '' : 's'} you're eligible for
                  </h3>
                </div>

                <div className="grid sm:grid-cols-2 xl:grid-cols-3 gap-4">
                  {visibleMatches.map((m) => (
                    <SchemeMatchCard key={m.schemeId} match={m} onClick={() => setSelectedMatch(m)} />
                  ))}
                </div>

                {!showAll && matches.length > PREVIEW_COUNT && (
                  <div className="text-center mt-6">
                    <button onClick={() => setShowAll(true)} className="btn-secondary inline-flex items-center gap-1.5">
                      See {matches.length - PREVIEW_COUNT} more<ChevronDown className="w-4 h-4" />
                    </button>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </main>

      {selectedMatch && (
        <SchemeDetailModal match={selectedMatch} onClose={() => setSelectedMatch(null)} />
      )}

      {intakeOpen && (
        <ApplicantIntakeModal
          onClose={() => setIntakeOpen(false)}
          onSaved={async () => {
            try {
              const res = await profileAPI.getApplicant();
              setApplicant(res.data);
            } catch (err) {
              console.error('Failed to reload applicant profile:', err);
            }
          }}
        />
      )}
    </div>
  );
}

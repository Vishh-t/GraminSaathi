import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { analysisAPI, reportsAPI, referenceAPI } from '../services/api';
import { useI18n } from '../i18n/i18n';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../hooks/useToast';
import TopNav from '../components/TopNav';
import Select from '../components/Select';
import OpportunityScoreCard from '../components/OpportunityScoreCard';
import LoanComparisonCard from '../components/LoanComparisonCard';
import DSCRIndicator from '../components/DSCRIndicator';
import AgentCommitteePanel from '../components/AgentCommitteePanel';
import EvidencePanel from '../components/EvidencePanel';
import HealthScoreRadar from '../components/HealthScoreRadar';
import SupplyRiskCard from '../components/SupplyRiskCard';
import PeerBenchmarkCard from '../components/PeerBenchmarkCard';
import RoadmapTimeline from '../components/RoadmapTimeline';
import LocalPriceIntelligence from '../components/LocalPriceIntelligence';
import FailureBoundary from '../components/FailureBoundary';
import SchemeComparisonTable from '../components/SchemeComparisonTable';
import UnlockMatchesBanner from '../components/UnlockMatchesBanner';
import { formatCurrency } from '../utils/format';
import { getErrorMessage, getErrorMessageFromBlob } from '../utils/errors';
import { Loader2, FileText, MapPin, BarChart2, Download, Save, Lightbulb, Building2, Wallet, Search } from 'lucide-react';

const tabs = [
  { id: 'overview', label: 'analysis.tabs.overview' },
  { id: 'financials', label: 'analysis.tabs.financials' },
  { id: 'risks', label: 'analysis.tabs.risks' },
  { id: 'evidence', label: 'analysis.tabs.evidence' },
];

export default function AnalysisPage() {
  const { t } = useI18n();
  const { isAuthenticated } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [activeTab, setActiveTab] = useState('overview');
  const [analysis, setAnalysis] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const village = searchParams.get('village');
  const category = searchParams.get('category');
  const capital = searchParams.get('capital');
  const hasParams = Boolean(village && category && capital);

  // Reference data + a small inline form so this page can be a real starting
  // point on its own, not just a screen that only ever renders when Discovery
  // (or Map/Dashboard) has already handed it a fully-formed deep link.
  const [villages, setVillages] = useState([]);
  const [categories, setCategories] = useState([]);
  const [refLoading, setRefLoading] = useState(true);
  const [formVillage, setFormVillage] = useState(village || '');
  const [formCategory, setFormCategory] = useState(category || '');
  const [formCapital, setFormCapital] = useState(capital || '');

  useEffect(() => {
    loadReferenceData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (village) setFormVillage(village);
    if (category) setFormCategory(category);
    if (capital) setFormCapital(capital);
  }, [village, category, capital]);

  useEffect(() => {
    if (hasParams) {
      loadAnalysis();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [village, category, capital]);

  const loadReferenceData = async () => {
    setRefLoading(true);
    try {
      const [vRes, cRes] = await Promise.all([
        referenceAPI.getVillages(),
        referenceAPI.getBusinessCategories(),
      ]);
      setVillages(vRes.data);
      setCategories(cRes.data);
      setFormVillage(prev => prev || village || vRes.data[0]?.villageName || '');
      setFormCategory(prev => prev || category || cRes.data[0]?.categoryName || '');
    } catch (err) {
      console.error('Failed to load reference data:', err);
    } finally {
      setRefLoading(false);
    }
  };

  const loadAnalysis = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await analysisAPI.analyze({
        villageName: village,
        businessCategory: category,
        availableMarginCapital: parseFloat(capital),
      });
      setAnalysis(res.data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadPdf = async () => {
    try {
      const res = await analysisAPI.downloadPdf({
        villageName: village,
        businessCategory: category,
        availableMarginCapital: parseFloat(capital),
      });
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `GraminSaathi_Analysis_${village}_${category}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert(await getErrorMessageFromBlob(err));
    }
  };

  const handleSaveReport = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    setSaving(true);
    try {
      await reportsAPI.save({
        villageName: village,
        businessCategory: category,
        availableMarginCapital: parseFloat(capital),
        resultJson: JSON.stringify(analysis),
      });
      alert('Report saved successfully!');
    } catch (err) {
      alert(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const handleViewMap = () => {
    navigate(`/map?village=${village}&category=${category}&capital=${capital}`);
  };

  // Re-fetches without the full-page loader (that would yank the intake modal's
  // "you're all set" screen out from under the user). AnalysisController
  // already merges the just-saved applicant profile server-side for logged-in
  // users, so a plain re-analyze picks up the new matches automatically.
  const handleProfileSaved = async () => {
    try {
      const res = await analysisAPI.analyze({
        villageName: village,
        businessCategory: category,
        availableMarginCapital: parseFloat(capital),
      });
      setAnalysis(res.data);
      showToast('Updated your results using your new profile.', 'success');
    } catch (err) {
      showToast(getErrorMessage(err, "Couldn't refresh your results — try re-running the analysis."), 'error');
    }
  };

  const handleRunSimulation = () => {
    navigate(`/simulator?village=${village}&category=${category}&capital=${capital}`);
  };

  const handleRunAnalysis = (e) => {
    e.preventDefault();
    if (!formVillage || !formCategory || !formCapital) return;
    navigate(`/analysis?village=${formVillage}&category=${formCategory}&capital=${formCapital}`);
  };

  // No village/category/capital yet — this is now a real landing state with
  // its own working form, not a dead end that only points back to Discovery.
  if (!hasParams) {
    return (
      <div className="min-h-screen bg-[#f9fafb]">
        <TopNav subtitle="Business Advisory" />
        <main className="max-w-xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
          <div className="text-center mb-8">
            <div className="w-14 h-14 bg-primary-100 rounded-2xl flex items-center justify-center mx-auto mb-4">
              <FileText className="w-7 h-7 text-primary-600" />
            </div>
            <h2 className="text-2xl font-bold text-gray-900">{t('analysis.title')}</h2>
            <p className="text-gray-500 mt-1">{t('analysis.subtitle')}</p>
          </div>

          <form onSubmit={handleRunAnalysis} className="card space-y-5">
            <div>
              <label className="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1.5">
                <MapPin className="w-3.5 h-3.5 text-primary-600" />
                {t('discovery.villageLabel')}
              </label>
              <Select
                value={formVillage}
                onChange={setFormVillage}
                disabled={refLoading}
                placeholder="Select a village"
                options={villages.map(v => ({ value: v.villageName, label: v.villageName }))}
              />
            </div>

            <div>
              <label className="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1.5">
                <Building2 className="w-3.5 h-3.5 text-primary-600" />
                {t('goalSeek.categoryLabel')}
              </label>
              <Select
                value={formCategory}
                onChange={setFormCategory}
                disabled={refLoading}
                placeholder="Select a business"
                options={categories.map(c => ({ value: c.categoryName, label: c.categoryName }))}
              />
            </div>

            <div>
              <label className="flex items-center gap-1.5 text-sm font-medium text-gray-700 mb-1.5">
                <Wallet className="w-3.5 h-3.5 text-primary-600" />
                {t('discovery.capitalLabel')}
              </label>
              <input
                type="number"
                value={formCapital}
                onChange={(e) => setFormCapital(e.target.value)}
                placeholder={t('discovery.capitalPlaceholder')}
                className="input-field"
                min="1000"
              />
            </div>

            <button
              type="submit"
              disabled={refLoading || !formVillage || !formCategory || !formCapital}
              className="btn-primary w-full flex items-center justify-center gap-2"
            >
              <BarChart2 className="w-5 h-5" />
              Run Analysis
            </button>
          </form>

          <p className="text-center text-sm text-gray-400 mt-6">
            Not sure yet? <button onClick={() => navigate('/discovery')} className="text-primary-600 hover:underline font-medium inline-flex items-center gap-1">
              <Search className="w-3.5 h-3.5" />Browse business ideas in Discovery
            </button> instead.
          </p>
        </main>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-[#f9fafb] flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="w-12 h-12 animate-spin text-primary-600 mx-auto mb-4" />
          <p className="text-gray-600">{t('analysis.loading')}</p>
        </div>
      </div>
    );
  }

  if (error || !analysis) {
    return (
      <div className="min-h-screen bg-[#f9fafb]">
        <TopNav subtitle="Business Advisory" />
        <div className="max-w-xl mx-auto px-4 py-20 text-center">
          <FileText className="w-14 h-14 mx-auto text-gray-300 mb-4" />
          <h2 className="text-lg font-semibold text-gray-900 mb-2">{error || 'Failed to load analysis'}</h2>
          <p className="text-gray-500 mb-6">
            We couldn't load this analysis. The backend may be unreachable, or check the console for details.
          </p>
          <div className="flex items-center justify-center gap-3">
            <button onClick={loadAnalysis} className="btn-secondary">{t('common.retry')}</button>
            <button onClick={() => navigate('/discovery')} className="btn-primary">Go to Discovery</button>
          </div>
        </div>
      </div>
    );
  }

  const { financial, feasibility, dscr, combination, evidence, committee, healthScore, supplyRisk, peerBenchmark, roadmap } = analysis;

  return (
    <div className="min-h-screen bg-[#f9fafb]">
      <TopNav subtitle="Business Advisory" />

      <main className="max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Title row */}
        <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4 mb-6">
          <div>
            <p className="text-xs font-medium uppercase tracking-wide text-gray-400 mb-1">
              {village} &middot; {category} &middot; {formatCurrency(parseFloat(capital))}
            </p>
            <h2 className="text-2xl font-bold text-gray-900">{t('analysis.title')}</h2>
            <p className="text-gray-500 mt-1">{t('analysis.subtitle')}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button onClick={handleDownloadPdf} className="btn-secondary flex items-center gap-2 !py-2 text-sm">
              <Download className="w-4 h-4" />
              {t('analysis.actions.downloadPdf')}
            </button>
            {isAuthenticated && (
              <button onClick={handleSaveReport} disabled={saving} className="btn-primary flex items-center gap-2 !py-2 text-sm">
                <Save className="w-4 h-4" />
                {saving ? 'Saving...' : t('analysis.actions.saveReport')}
              </button>
            )}
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="border-b border-gray-200 mb-6">
          <nav className="flex gap-1 overflow-x-auto" aria-label="Analysis tabs">
            {tabs.map(tab => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`px-4 py-3 text-sm font-medium border-b-2 transition-colors whitespace-nowrap ${
                  activeTab === tab.id
                    ? 'border-primary-600 text-primary-700'
                    : 'border-transparent text-gray-500 hover:text-gray-700'
                }`}
              >
                {t(tab.label)}
              </button>
            ))}
          </nav>
        </div>

        {/* Tab Content */}
        {activeTab === 'overview' && (
          <div className="space-y-6">
            {isAuthenticated && <UnlockMatchesBanner onProfileSaved={handleProfileSaved} />}

            <div className="grid lg:grid-cols-2 gap-6">
              <OpportunityScoreCard
                score={feasibility.opportunityScore}
                competitorCount={feasibility.competitorCount}
                population={feasibility.population5kmRadius}
                demandRatio={feasibility.demandRatio}
              />
              <LoanComparisonCard financial={financial} />
            </div>

            <div className="grid md:grid-cols-3 gap-4">
              <DSCRIndicator
                dscr={dscr.dscr}
                monthlyNetOperatingIncome={dscr.monthlyNetOperatingIncome}
                emi={dscr.emi}
              />
              <div className="card bg-primary-700 text-white border-none flex flex-col justify-center md:col-span-2">
                <div className="flex items-start gap-3">
                  <div className="w-9 h-9 bg-white/15 rounded-lg flex items-center justify-center flex-shrink-0">
                    <Lightbulb className="w-5 h-5" />
                  </div>
                  <div>
                    <p className="font-semibold">{t('analysis.secondaryOpportunity')}</p>
                    <p className="text-primary-50 text-sm mt-1">{combination.secondaryOpportunity}</p>
                  </div>
                </div>
              </div>
            </div>

            <LocalPriceIntelligence financial={financial} />
            <FailureBoundary financial={financial} />

            <div className="flex flex-wrap gap-3">
              <button onClick={handleViewMap} className="btn-secondary flex items-center gap-2">
                <MapPin className="w-4 h-4" />
                {t('analysis.actions.viewMap')}
              </button>
              <button onClick={handleRunSimulation} className="btn-secondary flex items-center gap-2">
                <BarChart2 className="w-4 h-4" />
                {t('analysis.actions.runSimulation')}
              </button>
            </div>
          </div>
        )}

        {activeTab === 'financials' && (
          <div className="space-y-6">
            <SchemeComparisonTable schemeComparison={financial.schemeComparison} />

            <div className="card">
              <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
                <FileText className="w-5 h-5 text-primary-600" />
                Detailed Financial Breakdown
              </h3>
              <div className="grid md:grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-gray-500">Project Cost</p>
                  <p className="font-semibold">{formatCurrency(financial.projectCost)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Loan Amount (90%)</p>
                  <p className="font-semibold">{formatCurrency(financial.loanAmount)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Scheme</p>
                  <p className="font-semibold">{financial.schemeName}</p>
                </div>
                <div>
                  <p className="text-gray-500">Interest Rate</p>
                  <p className="font-semibold">{(financial.interestRateAnnual * 100).toFixed(1)}% p.a.</p>
                </div>
                <div>
                  <p className="text-gray-500">Tenure</p>
                  <p className="font-semibold">{financial.tenureYears} years</p>
                </div>
                <div>
                  <p className="text-gray-500">Moratorium</p>
                  <p className="font-semibold">{financial.moratoriumMonths} months</p>
                </div>
                <div>
                  <p className="text-gray-500">Repayment Months</p>
                  <p className="font-semibold">{financial.repaymentMonths}</p>
                </div>
                <div>
                  <p className="text-gray-500">Monthly EMI</p>
                  <p className="font-semibold text-primary-600">{formatCurrency(financial.emi)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Working Capital Estimate</p>
                  <p className="font-semibold">{formatCurrency(financial.workingCapitalEstimate)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Recommended Project Cost</p>
                  <p className="font-semibold text-green-600">{formatCurrency(financial.recommendedProjectCost)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Recommended Loan</p>
                  <p className="font-semibold text-green-600">{formatCurrency(financial.recommendedLoanAmount)}</p>
                </div>
                <div>
                  <p className="text-gray-500">Buffer Amount</p>
                  <p className="font-semibold text-green-600">{formatCurrency(financial.bufferAmount)}</p>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'risks' && (
          <div className="space-y-6">
            <AgentCommitteePanel committee={committee} />
            <div className="grid md:grid-cols-2 gap-6">
              <SupplyRiskCard supplyRisk={supplyRisk} />
              <PeerBenchmarkCard peerBenchmark={peerBenchmark} />
            </div>
            <RoadmapTimeline roadmap={roadmap} />
            <HealthScoreRadar healthScore={healthScore} />
          </div>
        )}

        {activeTab === 'evidence' && (
          <div className="space-y-6">
            <EvidencePanel evidence={evidence} />
          </div>
        )}
      </main>
    </div>
  );
}

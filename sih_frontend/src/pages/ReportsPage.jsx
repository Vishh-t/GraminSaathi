import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { reportsAPI, analysisAPI } from '../services/api';
import { useI18n } from '../i18n/i18n';
import { useAuth } from '../context/AuthContext';
import TopNav from '../components/TopNav';
import { formatCurrency } from '../utils/format';
import { getErrorMessage, getErrorMessageFromBlob } from '../utils/errors';
import { Loader2, FileText, Download, Eye, Calendar, MapPin, Briefcase, DollarSign, FileBarChart, TrendingUp } from 'lucide-react';

export default function ReportsPage() {
  const { t, language } = useI18n();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (isAuthenticated) {
      loadReports();
    }
  }, [isAuthenticated]);

  const loadReports = async () => {
    setLoading(true);
    try {
      const res = await reportsAPI.getAll();
      setReports(res.data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleView = (report) => {
    navigate(`/analysis?village=${report.villageName}&category=${report.businessCategory}&capital=${report.availableMarginCapital}`);
  };

  const handleDownload = async (report) => {
    try {
      const res = await analysisAPI.downloadPdf({
        villageName: report.villageName,
        businessCategory: report.businessCategory,
        availableMarginCapital: report.availableMarginCapital,
      });
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `GraminSaathi_Report_${report.villageName}_${report.businessCategory}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert(await getErrorMessageFromBlob(err));
    }
  };

  const formatDate = (dateStr) => {
    return new Date(dateStr).toLocaleDateString(language === 'hi' ? 'hi-IN' : 'en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const mostViable = (() => {
    if (!reports.length) return '—';
    const counts = {};
    reports.forEach(r => { counts[r.businessCategory] = (counts[r.businessCategory] || 0) + 1; });
    return Object.entries(counts).sort((a, b) => b[1] - a[1])[0][0];
  })();

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-[#f9fafb] flex items-center justify-center">
        <div className="text-center p-8">
          <FileText className="w-16 h-16 mx-auto text-gray-300 mb-4" />
          <h2 className="text-xl font-semibold text-gray-900 mb-2">Please log in to view reports</h2>
          <button onClick={() => navigate('/login')} className="mt-4 btn-primary">
            Go to Login
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#f9fafb]">
      <TopNav subtitle="Business Advisory" />

      <main className="max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-8">
          <h2 className="text-2xl font-bold text-gray-900">{t('reports.title')}</h2>
          <p className="text-gray-500 mt-1">{t('reports.subtitle')}</p>
        </div>

        {/* Stat cards */}
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
          <div className="card flex items-center gap-4">
            <div className="w-11 h-11 rounded-full bg-surface-container flex items-center justify-center flex-shrink-0">
              <FileBarChart className="w-5 h-5 text-primary-700" />
            </div>
            <div>
              <p className="text-sm text-gray-500">Total Reports Generated</p>
              <p className="text-2xl font-bold text-gray-900">{reports.length}</p>
            </div>
          </div>
          <div className="card flex items-center gap-4 border-l-4 border-l-primary-600">
            <div className="w-11 h-11 rounded-full bg-green-50 flex items-center justify-center flex-shrink-0">
              <TrendingUp className="w-5 h-5 text-primary-700" />
            </div>
            <div>
              <p className="text-sm text-gray-500">Most Viable Category</p>
              <p className="text-2xl font-bold text-gray-900">{mostViable}</p>
            </div>
          </div>
          <div className="hidden lg:block rounded-lg overflow-hidden bg-gradient-to-br from-primary-700 to-primary-500 h-full min-h-[76px]" />
        </div>

        <div className="card !p-0 overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100">
            <h3 className="font-semibold text-primary-700">Report History</h3>
          </div>

          {loading ? (
            <div className="text-center py-16">
              <Loader2 className="w-10 h-10 animate-spin text-primary-600 mx-auto mb-4" />
              <p className="text-gray-600">{t('reports.loading')}</p>
            </div>
          ) : error ? (
            <div className="text-center py-16 text-red-600">
              <p>{error}</p>
              <button onClick={loadReports} className="mt-4 btn-primary">{t('common.retry')}</button>
            </div>
          ) : reports.length === 0 ? (
            <div className="text-center py-16">
              <FileText className="w-16 h-16 mx-auto text-gray-300 mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">{t('reports.empty')}</h3>
              <p className="text-gray-500 mb-6">Save an analysis from the Analysis page to see it here</p>
              <button onClick={() => navigate('/discovery')} className="btn-primary">
                Start New Analysis
              </button>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-200 bg-surface-low">
                    <th className="text-left p-4 font-medium text-gray-600">{t('reports.date')}</th>
                    <th className="text-left p-4 font-medium text-gray-600">{t('reports.village')}</th>
                    <th className="text-left p-4 font-medium text-gray-600">{t('reports.business')}</th>
                    <th className="text-left p-4 font-medium text-gray-600">{t('reports.capital')}</th>
                    <th className="text-right p-4 font-medium text-gray-600">{t('common.actions') !== 'common.actions' ? t('common.actions') : 'Actions'}</th>
                  </tr>
                </thead>
                <tbody>
                  {reports.map((report) => (
                    <tr key={report.id} className="border-b border-gray-100 hover:bg-gray-50">
                      <td className="p-4">
                        <div className="flex items-center gap-2 text-gray-700">
                          <Calendar className="w-4 h-4 text-gray-400" />
                          <span>{formatDate(report.createdAt)}</span>
                        </div>
                      </td>
                      <td className="p-4">
                        <div className="flex items-center gap-2 text-gray-700">
                          <MapPin className="w-4 h-4 text-gray-400" />
                          <span>{report.villageName}</span>
                        </div>
                      </td>
                      <td className="p-4">
                        <span className="pill bg-primary-50 text-primary-700">
                          <Briefcase className="w-3.5 h-3.5 mr-1" />
                          {report.businessCategory}
                        </span>
                      </td>
                      <td className="p-4">
                        <div className="flex items-center gap-2 text-gray-700">
                          <DollarSign className="w-4 h-4 text-gray-400" />
                          <span>{formatCurrency(report.availableMarginCapital)}</span>
                        </div>
                      </td>
                      <td className="p-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => handleView(report)}
                            className="btn-ghost p-2"
                            title={t('reports.actions.view')}
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleDownload(report)}
                            className="btn-ghost p-2"
                            title={t('reports.actions.download')}
                          >
                            <Download className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

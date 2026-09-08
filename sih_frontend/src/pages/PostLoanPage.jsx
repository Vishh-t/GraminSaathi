import { useI18n } from '../i18n/i18n';
import TopNav from '../components/TopNav';
import { AlertTriangle, TrendingDown, DollarSign, Clock, Brain, Shield, Lightbulb } from 'lucide-react';

const nudges = [
  {
    icon: AlertTriangle,
    color: 'text-warning-600 bg-warning-100',
    title: 'Expense Alert',
    message: 'postLoan.nudges.expenses',
    action: 'Review input costs',
  },
  {
    icon: TrendingDown,
    color: 'text-red-600 bg-red-100',
    title: 'Margin Pressure',
    message: 'postLoan.nudges.margin',
    action: 'Consider pricing suggestions',
  },
  {
    icon: DollarSign,
    color: 'text-blue-600 bg-blue-100',
    title: 'EMI Reminder',
    message: 'postLoan.nudges.emi',
    action: 'Prepare payment',
    dynamic: true,
  },
];

export default function PostLoanPage() {
  const { t } = useI18n();

  return (
    <div className="min-h-screen bg-[#f9fafb]">
      <TopNav subtitle={t('postLoan.subtitle')} />

      <main className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Title */}
        <div className="mb-8 text-center">
          <div className="inline-flex items-center gap-2 px-4 py-2 bg-purple-100 text-purple-800 rounded-full text-sm font-medium mb-4">
            <Brain className="w-4 h-4" />
            {t('postLoan.subtitle')}
          </div>
          <h2 className="text-2xl font-bold text-gray-900">{t('postLoan.title')}</h2>
          <p className="text-gray-500 mt-1 max-w-xl mx-auto">{t('postLoan.disclaimer')}</p>
        </div>

        {/* Nudge Cards */}
        <div className="space-y-4 mb-8">
          {nudges.map((nudge, index) => (
            <div key={index} className="card border-l-4 border-purple-500">
              <div className="flex items-start gap-4">
                <div className={`w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 ${nudge.color}`}>
                  <nudge.icon className="w-6 h-6" />
                </div>
                <div className="flex-1">
                  <h3 className="font-semibold text-gray-900">{t(nudge.title)}</h3>
                  <p className="text-gray-600 mt-1">
                    {nudge.dynamic 
                      ? t(nudge.message, { emi: '21,400' }) 
                      : t(nudge.message)
                    }
                  </p>
                </div>
                <button className="btn-ghost text-sm px-3 py-1 whitespace-nowrap">
                  {t(nudge.action)}
                </button>
              </div>
            </div>
          ))}
        </div>

        {/* Feature Preview Sections */}
        <div className="space-y-6">
          <div className="card">
            <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <Shield className="w-5 h-5 text-primary-600" />
              Planned Features
            </h3>
            <div className="grid md:grid-cols-3 gap-4">
              <div className="p-4 bg-gray-50 rounded-lg">
                <Lightbulb className="w-6 h-6 text-primary-600 mb-2" />
                <h4 className="font-medium text-gray-900 mb-1">Smart Alerts</h4>
                <p className="text-sm text-gray-600">Real-time notifications for cash flow risks, margin pressure, and payment due dates</p>
              </div>
              <div className="p-4 bg-gray-50 rounded-lg">
                <Brain className="w-6 h-6 text-primary-600 mb-2" />
                <h4 className="font-medium text-gray-900 mb-1">AI Advisory</h4>
                <p className="text-sm text-gray-600">Personalized recommendations based on actual business performance vs. projections</p>
              </div>
              <div className="p-4 bg-gray-50 rounded-lg">
                <Clock className="w-6 h-6 text-primary-600 mb-2" />
                <h4 className="font-medium text-gray-900 mb-1">Auto-Reports</h4>
                <p className="text-sm text-gray-600">Monthly performance summaries sent to entrepreneur and bank/SCA</p>
              </div>
            </div>
          </div>

          <div className="card bg-primary-50 border-primary-200">
            <h3 className="text-lg font-semibold text-primary-800 mb-4 flex items-center gap-2">
              <Lightbulb className="w-5 h-5" />
              Coming in Next Version
            </h3>
            <ul className="space-y-2 text-sm text-primary-700 pl-4 list-disc">
              <li>Integration with bank APIs for real-time transaction monitoring</li>
              <li>SHG group-level portfolio dashboards for field officers</li>
              <li>Voice-based business diary for daily expense/income logging</li>
              <li>Market price feeds integration for dynamic pricing alerts</li>
              <li>Insurance product recommendations based on risk profile</li>
            </ul>
          </div>
        </div>

        <div className="mt-8 text-center text-sm text-gray-500">
          <p>GraminSaathi — SIH 2026 Prototype</p>
          <p className="mt-1">"We don't just tell you how much you can borrow — we tell you if your business will survive, before you do."</p>
        </div>
      </main>
    </div>
  );
}
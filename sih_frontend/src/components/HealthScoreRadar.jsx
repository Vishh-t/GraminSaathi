import { formatNumber, getHealthRecommendationColor } from '../utils/format';

const subScoreConfigs = [
  { key: 'marketDemand', label: 'Market Demand', weight: '25%' },
  { key: 'capitalAdequacy', label: 'Capital Adequacy', weight: '15%' },
  { key: 'profitability', label: 'Profitability', weight: '20%' },
  { key: 'cashFlow', label: 'Cash Flow', weight: '20%' },
  { key: 'supplyRisk', label: 'Supply Risk', weight: '10%' },
  { key: 'seasonality', label: 'Seasonality', weight: '10%' },
];

export default function HealthScoreRadar({ healthScore, className = '' }) {
  const { overallScore, recommendation, ...subScores } = healthScore;

  return (
    <div className={`card ${className}`}>
      <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg className="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
        Business Health Score
      </h3>

      <div className="grid md:grid-cols-2 gap-8 mb-6">
        {/* Overall Score */}
        <div className="text-center p-6 bg-gray-50 rounded-xl">
          <div className="relative w-40 h-40 mx-auto mb-4">
            <svg className="w-full h-full transform -rotate-90">
              <circle
                cx="80"
                cy="80"
                r="70"
                stroke="#e5e7eb"
                strokeWidth="10"
                fill="none"
              />
              <circle
                cx="80"
                cy="80"
                r="70"
                stroke={overallScore >= 75 ? '#16a34a' : overallScore >= 50 ? '#f59e0b' : '#dc2626'}
                strokeWidth="10"
                fill="none"
                strokeDasharray={440 * (overallScore / 100)}
                strokeDashoffset={440 * (1 - overallScore / 100)}
                strokeLinecap="round"
                className="transition-all duration-1000"
              />
            </svg>
            <div className="absolute inset-0 flex items-center justify-center">
              <span className="text-4xl font-bold text-gray-900">{overallScore}</span>
            </div>
          </div>
          <div className={`inline-flex items-center px-4 py-2 rounded-full text-sm font-medium ${getHealthRecommendationColor(recommendation)}`}>
            {recommendation}
          </div>
        </div>

        {/* Sub-scores breakdown */}
        <div className="space-y-4">
          {subScoreConfigs.map(({ key, label, weight }) => {
            const score = subScores[key];
            const colorClass = score.score >= 70 ? 'bg-green-500' : score.score >= 40 ? 'bg-yellow-500' : 'bg-red-500';
            return (
              <div key={key} className="space-y-1">
                <div className="flex items-center justify-between">
                  <span className="font-medium text-gray-900">{label}</span>
                  <span className="text-sm text-gray-500">{weight} weight</span>
                </div>
                <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-500 ${colorClass}`}
                    style={{ width: `${score.score}%` }}
                  />
                </div>
                <div className="flex justify-between text-xs">
                  <span className="text-gray-500">{score.description}</span>
                  <span className="font-semibold">{score.score}/100</span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Formula explanation */}
      <div className="p-4 bg-primary-50 rounded-lg border border-primary-100">
        <p className="text-sm text-primary-800 font-medium mb-2">Score Calculation:</p>
        <p className="text-xs text-primary-700">
          Market Demand (25%) + Capital Adequacy (15%) + Profitability (20%) + Cash Flow (20%) + Supply Risk (10%) + Seasonality (10%)
        </p>
      </div>
    </div>
  );
}
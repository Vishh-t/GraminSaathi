import { getScoreColor, getScoreLabel, formatNumber } from '../utils/format';

export default function OpportunityScoreCard({ score, competitorCount, population, demandRatio, className = '' }) {
  const colorClass = getScoreColor(score);
  const label = getScoreLabel(score);

  return (
    <div className={`card ${className}`}>
      <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg className="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
        Opportunity Score
      </h3>
      
      <div className="flex items-center gap-8">
        <div className="relative w-32 h-32 flex-shrink-0">
          <svg className="w-full h-full transform -rotate-90">
            <circle
              cx="64"
              cy="64"
              r="56"
              stroke="#e5e7eb"
              strokeWidth="8"
              fill="none"
            />
            <circle
              cx="64"
              cy="64"
              r="56"
              stroke={score >= 70 ? '#16a34a' : score >= 40 ? '#f59e0b' : '#dc2626'}
              strokeWidth="8"
              fill="none"
              strokeDasharray={352 * (score / 100)}
              strokeDashoffset={352 * (1 - score / 100)}
              strokeLinecap="round"
              className="transition-all duration-1000"
              style={{ filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.1))' }}
            />
          </svg>
          <div className="absolute inset-0 flex items-center justify-center">
            <span className="text-3xl font-bold text-gray-900">{score}</span>
          </div>
        </div>
        
        <div className="flex-1">
          <div className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${colorClass} mb-4`}>
            {label}
          </div>
          
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <p className="text-gray-500">Competitors</p>
              <p className="font-semibold">{formatNumber(competitorCount)}</p>
            </div>
            <div>
              <p className="text-gray-500">Population (5km)</p>
              <p className="font-semibold">{formatNumber(population)}</p>
            </div>
            <div>
              <p className="text-gray-500">Demand Ratio</p>
              <p className="font-semibold">{demandRatio.toFixed(1)}</p>
            </div>
            <div>
              <p className="text-gray-500">Reference High</p>
              <p className="font-semibold">4,000</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
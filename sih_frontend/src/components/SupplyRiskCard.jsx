import { getRiskColor, formatNumber } from '../utils/format';

export default function SupplyRiskCard({ supplyRisk, className = '' }) {
  const { riskLevel, note } = supplyRisk;
  const colorClass = getRiskColor(riskLevel);

  return (
    <div className={`card ${className}`}>
      <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg className="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
        </svg>
        Supply Chain Risk
      </h3>

      <div className="flex items-center gap-4">
        <div className={`inline-flex items-center px-4 py-2 rounded-lg text-lg font-bold ${colorClass}`}>
          {riskLevel}
        </div>
      </div>

      <div className="mt-4 p-4 bg-gray-50 rounded-lg border border-gray-200">
        <p className="text-gray-700">{note}</p>
      </div>

      <div className="mt-4 p-3 bg-primary-50 rounded-lg border border-primary-100">
        <p className="text-sm text-primary-800 font-medium mb-1">Risk Assessment:</p>
        <p className="text-sm text-primary-700">
          Based on category-specific supply chain dependencies. Diversify suppliers to mitigate risk.
        </p>
      </div>
    </div>
  );
}
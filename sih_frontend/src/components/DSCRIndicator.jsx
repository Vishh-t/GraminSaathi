import { getDSCRColor, getDSCRLabel, formatCurrency } from '../utils/format';

export default function DSCRIndicator({ dscr, monthlyNetOperatingIncome, emi, className = '' }) {
  const colorClass = getDSCRColor(dscr);
  const label = getDSCRLabel(dscr);
  const thresholdsText = '>=2.0 Healthy | >=1.0 Moderate | <1.0 Risky';

  return (
    <div className={`card ${className}`}>
      <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg className="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
        DSCR (Debt Service Coverage Ratio)
      </h3>

      <div className="flex items-center gap-8">
        <div className="flex-shrink-0">
          <div className={`inline-flex items-center px-4 py-2 rounded-lg text-lg font-bold ${colorClass}`}>
            {dscr.toFixed(2)}
          </div>
          <div className={`mt-2 inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${colorClass}`}>
            {label}
          </div>
        </div>

        <div className="flex-1 grid grid-cols-2 gap-4 text-sm">
          <div>
            <p className="text-gray-500">Net Monthly Income</p>
            <p className="font-semibold">{formatCurrency(monthlyNetOperatingIncome)}</p>
          </div>
          <div>
            <p className="text-gray-500">Monthly EMI</p>
            <p className="font-semibold">{formatCurrency(emi)}</p>
          </div>
          <div>
            <p className="text-gray-500">Formula</p>
            <p className="font-mono text-xs text-gray-600">Net Income / EMI</p>
          </div>
          <div>
            <p className="text-gray-500">Thresholds</p>
            <p className="font-mono text-xs text-gray-600">{thresholdsText}</p>
          </div>
        </div>
      </div>

      {/* Visual indicator bar */}
      <div className="mt-4">
        <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
          <div
            className={`h-full rounded-full transition-all duration-500 ${
              dscr >= 2 ? 'bg-green-500' : dscr >= 1 ? 'bg-yellow-500' : 'bg-red-500'
            }`}
            style={{ width: `${Math.min(dscr / 3 * 100, 100)}%` }}
          />
        </div>
        <div className="flex justify-between text-xs text-gray-500 mt-1">
          <span>0</span>
          <span>1.0</span>
          <span>2.0</span>
          <span>3.0+</span>
        </div>
      </div>
    </div>
  );
}
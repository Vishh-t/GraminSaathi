import { formatCurrency, formatNumber } from '../utils/format';

/** Long real scheme names (some run past 70 characters) would break the card's layout untruncated. */
function truncateSchemeName(name, maxLength = 42) {
  if (!name) return '';
  return name.length > maxLength ? `${name.slice(0, maxLength - 1)}\u2026` : name;
}

export default function LoanComparisonCard({ financial, className = '' }) {
  const {
    projectCost,
    loanAmount,
    schemeName,
    interestRateAnnual,
    tenureYears,
    moratoriumMonths,
    emi,
    recommendedProjectCost,
    recommendedLoanAmount,
    recommendedEmi,
    bufferAmount,
  } = financial;

  const primaryScheme = truncateSchemeName(schemeName);

  return (
    <div className={`card ${className}`}>
      <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg className="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        Loan Comparison
      </h3>

      <div className="grid md:grid-cols-2 gap-6">
        {/* Maximum Eligible */}
        <div className="border border-gray-200 rounded-lg p-5">
          <div className="flex items-center justify-between mb-4">
            <h4 className="font-medium text-gray-900">Maximum Eligible</h4>
            <span className="badge badge-info" title={schemeName}>{primaryScheme}</span>
          </div>
          
          <div className="space-y-3">
            <div className="flex justify-between">
              <span className="text-gray-600">Project Cost</span>
              <span className="font-semibold">{formatCurrency(projectCost)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Loan Amount (90%)</span>
              <span className="font-semibold">{formatCurrency(loanAmount)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Interest Rate</span>
              <span className="font-semibold">{(interestRateAnnual * 100).toFixed(1)}% p.a.</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Tenure</span>
              <span className="font-semibold">{tenureYears} years ({moratoriumMonths} mo moratorium)</span>
            </div>
            <div className="border-t border-gray-100 pt-3 flex justify-between">
              <span className="text-gray-600">Monthly EMI</span>
              <span className="font-bold text-lg">{formatCurrency(emi)}</span>
            </div>
          </div>
        </div>

        {/* Recommended (Optimal) */}
        <div className="border-2 border-primary-500 rounded-lg p-5 bg-primary-50 relative">
          <div className="absolute -top-3 left-4 bg-primary-600 text-white text-xs font-medium px-2 py-0.5 rounded">
            ✓ Recommended (Optimal)
          </div>
          <h4 className="font-medium text-gray-900 mb-4">Recommended (Optimal)</h4>
          
          <div className="space-y-3">
            <div className="flex justify-between">
              <span className="text-gray-600">Project Cost</span>
              <span className="font-semibold">{formatCurrency(recommendedProjectCost)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Loan Amount (90%)</span>
              <span className="font-semibold">{formatCurrency(recommendedLoanAmount)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Interest Rate</span>
              <span className="font-semibold">{(interestRateAnnual * 100).toFixed(1)}% p.a.</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Tenure</span>
              <span className="font-semibold">{tenureYears} years ({moratoriumMonths} mo moratorium)</span>
            </div>
            <div className="border-t border-primary-200 pt-3 flex justify-between">
              <span className="text-gray-600">Monthly EMI</span>
              <span className="font-bold text-lg text-primary-600">{formatCurrency(recommendedEmi || emi * (recommendedLoanAmount / loanAmount))}</span>
            </div>
          </div>
          
          <div className="mt-4 p-3 bg-primary-100 rounded-lg border border-primary-200">
            <div className="flex items-center justify-between">
              <span className="font-medium text-primary-800">Safety Buffer</span>
              <span className="font-bold text-primary-700">{formatCurrency(bufferAmount)}</span>
            </div>
            <p className="text-sm text-primary-700 mt-1">
              Your margin capital covers the 10% contribution with ₹{formatNumber(bufferAmount)} buffer for working capital
            </p>
          </div>
        </div>
      </div>

      {/* Working Capital Warning */}
      {financial.workingCapitalWarning && (
        <div className="mt-4 p-4 bg-warning-50 border border-warning-200 rounded-lg">
          <div className="flex items-start gap-3">
            <svg className="w-5 h-5 text-warning-600 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <p className="text-warning-800 text-sm">{financial.workingCapitalWarning}</p>
          </div>
        </div>
      )}
    </div>
  );
}
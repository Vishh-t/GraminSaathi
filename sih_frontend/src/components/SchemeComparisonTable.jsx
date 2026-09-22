import { formatCurrency, formatNumber } from '../utils/format';

export default function SchemeComparisonTable({ schemeComparison, className = '' }) {
  return (
    <div className={`card ${className}`}>
      <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg className="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
        </svg>
        Scheme Comparison
      </h3>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-gray-200 bg-gray-50">
              <th className="text-left p-3 font-medium text-gray-600">Scheme</th>
              <th className="text-right p-3 font-medium text-gray-600">Interest</th>
              <th className="text-right p-3 font-medium text-gray-600">Tenure</th>
              <th className="text-right p-3 font-medium text-gray-600">Moratorium</th>
              <th className="text-right p-3 font-medium text-gray-600">EMI</th>
              <th className="text-left p-3 font-medium text-gray-600">Agency</th>
              <th className="text-left p-3 font-medium text-gray-600">Notes</th>
            </tr>
          </thead>
          <tbody>
            {schemeComparison.map((scheme, index) => (
              <tr
                key={index}
                className={`border-b border-gray-100 ${scheme.isPrimary ? 'bg-primary-50' : ''}`}
              >
                <td className="p-3">
                  <div className="flex items-center gap-2">
                    {scheme.isPrimary && (
                      <span className="text-primary-600 font-bold">★</span>
                    )}
                    <span className={scheme.isPrimary ? 'font-semibold text-gray-900' : 'text-gray-700'}>
                      {scheme.schemeName}
                    </span>
                  </div>
                </td>
                <td className="p-3 text-right font-medium">
                  {scheme.interestRate.toFixed(1)}%
                  {scheme.rateEstimated && (
                    <span className="ml-1 text-xs font-normal text-gray-400" title="Estimated from the scheme's stated interest subvention, not fixed by the scheme itself">
                      (est.)
                    </span>
                  )}
                </td>
                <td className="p-3 text-right">
                  {scheme.tenureYears} years
                </td>
                <td className="p-3 text-right">
                  {scheme.moratoriumMonths} months
                </td>
                <td className="p-3 text-right font-medium">
                  {formatCurrency(scheme.emi)}
                </td>
                <td className="p-3 text-sm text-gray-600">{scheme.agency}</td>
                <td className="p-3 text-sm text-gray-500">
                  {scheme.subsidyNote || '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="mt-3 text-sm text-gray-500 text-center">
        ★ = Your matched scheme (the cheapest option you can afford). (est.) = interest rate estimated from the scheme's
        subsidy note, not fixed by the scheme itself — verify current terms with the implementing agency before applying.
      </p>
    </div>
  );
}
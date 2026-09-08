import { formatCurrency, formatNumber } from '../utils/format';

export default function PeerBenchmarkCard({ peerBenchmark, className = '' }) {
  const { sampleSize, avgMonthlyRevenueAfter6Months, pctStillOperatingAfter1Year, disclaimer } = peerBenchmark;

  return (
    <div className={`card ${className}`}>
      <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg className="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
        </svg>
        Peer Benchmarking
      </h3>

      <div className="grid md:grid-cols-3 gap-4 mb-4">
        <div className="p-4 bg-gray-50 rounded-lg border border-gray-200 text-center">
          <p className="text-sm text-gray-500">Sample Size</p>
          <p className="text-2xl font-bold text-gray-900 mt-1">{formatNumber(sampleSize)} businesses</p>
        </div>
        <div className="p-4 bg-green-50 rounded-lg border border-green-200 text-center">
          <p className="text-sm text-green-700">Avg Revenue (6 months)</p>
          <p className="text-2xl font-bold text-green-600 mt-1">{formatCurrency(avgMonthlyRevenueAfter6Months)}</p>
        </div>
        <div className="p-4 bg-blue-50 rounded-lg border border-blue-200 text-center">
          <p className="text-sm text-blue-700">Still Operating (1 year)</p>
          <p className="text-2xl font-bold text-blue-600 mt-1">{pctStillOperatingAfter1Year}%</p>
        </div>
      </div>

      <div className="p-4 bg-warning-50 rounded-lg border border-warning-200">
        <p className="text-sm text-warning-800 flex items-center gap-1.5">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          {disclaimer}
        </p>
      </div>

      <div className="mt-4 p-3 bg-primary-50 rounded-lg border border-primary-100">
        <p className="text-sm text-primary-800 font-medium mb-1">Interpretation:</p>
        <p className="text-sm text-primary-700">
          These figures represent illustrative cohort data from similar businesses in the region. 
          In production, this would reflect real anonymized outcomes from platform users over time.
        </p>
      </div>
    </div>
  );
}
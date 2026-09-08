import { formatCurrency } from '../utils/format';

export default function FailureBoundary({ financial, className = '' }) {
  const { breakevenPrice, breakevenNote, localAveragePrice } = financial;

  if (breakevenPrice === null || breakevenPrice === undefined) {
    return (
      <div className={`card ${className}`}>
        <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
          <svg className="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          Failure Boundary
        </h3>
        <div className="p-4 bg-gray-50 rounded-lg border border-gray-200">
          <p className="text-gray-600">Not applicable — mixed product pricing</p>
        </div>
      </div>
    );
  }

  const safetyMargin = localAveragePrice - breakevenPrice;
  const marginPercent = ((safetyMargin / localAveragePrice) * 100).toFixed(1);

  return (
    <div className={`card ${className}`}>
      <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg className="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
        Failure Boundary
      </h3>

      <div className="grid md:grid-cols-3 gap-4 mb-4">
        <div className="p-4 bg-red-50 rounded-lg border border-red-200 text-center">
          <p className="text-sm text-red-700">Minimum Viable Price</p>
          <p className="text-2xl font-bold text-red-600 mt-1">{formatCurrency(breakevenPrice)}</p>
          <p className="text-xs text-red-500 mt-1">Below this = unprofitable</p>
        </div>
        <div className="p-4 bg-green-50 rounded-lg border border-green-200 text-center">
          <p className="text-sm text-green-700">Your Planned Price</p>
          <p className="text-2xl font-bold text-green-600 mt-1">{formatCurrency(localAveragePrice)}</p>
        </div>
        <div className="p-4 bg-blue-50 rounded-lg border border-blue-200 text-center">
          <p className="text-sm text-blue-700">Per-Unit Safety Margin</p>
          <p className="text-2xl font-bold text-blue-600 mt-1">{formatCurrency(safetyMargin)}</p>
          <p className="text-xs text-blue-500 mt-1">({marginPercent}% buffer)</p>
        </div>
      </div>

      <div className="p-4 bg-gray-50 rounded-lg border border-gray-200">
        <p className="text-sm text-gray-700">{breakevenNote}</p>
      </div>
    </div>
  );
}
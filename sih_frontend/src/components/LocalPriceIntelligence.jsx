import { formatCurrency } from '../utils/format';

export default function LocalPriceIntelligence({ financial, className = '' }) {
  const {
    localAveragePrice,
    recommendedPriceLow,
    recommendedPriceHigh,
    recommendedLaunchPrice,
  } = financial;

  // Skip for Retail (null avg_local_price)
  if (localAveragePrice === null || localAveragePrice === undefined) {
    return (
      <div className={`card ${className}`}>
        <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
          <svg className="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          Local Price Intelligence
        </h3>
        <div className="p-4 bg-gray-50 rounded-lg border border-gray-200">
          <p className="text-gray-600">Pricing varies by product line — not applicable for general retail</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`card ${className}`}>
      <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <svg className="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        Local Price Intelligence
      </h3>

      <div className="grid md:grid-cols-4 gap-4">
        <div className="p-4 bg-gray-50 rounded-lg border border-gray-200 text-center">
          <p className="text-sm text-gray-500">Local Average Price</p>
          <p className="text-2xl font-bold text-gray-900 mt-1">{formatCurrency(localAveragePrice)}</p>
        </div>
        <div className="p-4 bg-primary-50 rounded-lg border border-primary-200 text-center">
          <p className="text-sm text-primary-700">Recommended Launch Price</p>
          <p className="text-2xl font-bold text-primary-600 mt-1">{formatCurrency(recommendedLaunchPrice)}</p>
          <p className="text-xs text-primary-500 mt-1">(97% of local avg)</p>
        </div>
        <div className="p-4 bg-green-50 rounded-lg border border-green-200 text-center">
          <p className="text-sm text-green-700">Price Band Low (95%)</p>
          <p className="text-2xl font-bold text-green-600 mt-1">{formatCurrency(recommendedPriceLow)}</p>
        </div>
        <div className="p-4 bg-blue-50 rounded-lg border border-blue-200 text-center">
          <p className="text-sm text-blue-700">Price Band High (105%)</p>
          <p className="text-2xl font-bold text-blue-600 mt-1">{formatCurrency(recommendedPriceHigh)}</p>
        </div>
      </div>

      <div className="mt-4 p-4 bg-primary-50 rounded-lg border border-primary-100">
        <p className="text-sm text-primary-800 font-medium mb-1">Pricing Strategy:</p>
        <p className="text-sm text-primary-700">
          Launch slightly below local average ({formatCurrency(recommendedLaunchPrice)}) to encourage early adoption, 
          then adjust within the {formatCurrency(recommendedPriceLow)} – {formatCurrency(recommendedPriceHigh)} band based on demand.
        </p>
      </div>
    </div>
  );
}
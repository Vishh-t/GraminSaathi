export function formatCurrency(amount, locale = 'en-IN') {
  if (amount === null || amount === undefined) return '—';
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(amount);
}

export function formatNumber(num, locale = 'en-IN') {
  if (num === null || num === undefined) return '—';
  return new Intl.NumberFormat(locale).format(num);
}

export function formatPercent(value, locale = 'en-IN') {
  if (value === null || value === undefined) return '—';
  return new Intl.NumberFormat(locale, {
    style: 'percent',
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  }).format(value / 100);
}

export function formatPercentDecimal(value, locale = 'en-IN') {
  if (value === null || value === undefined) return '—';
  return new Intl.NumberFormat(locale, {
    style: 'percent',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

export function getScoreColor(score) {
  if (score >= 70) return 'text-green-600 bg-green-100';
  if (score >= 40) return 'text-yellow-600 bg-yellow-100';
  return 'text-red-600 bg-red-100';
}

export function getScoreLabel(score) {
  if (score >= 70) return 'High opportunity';
  if (score >= 40) return 'Moderate opportunity';
  return 'Low opportunity (saturated)';
}

export function getDSCRColor(dscr) {
  if (dscr >= 2.0) return 'text-green-600 bg-green-100';
  if (dscr >= 1.0) return 'text-yellow-600 bg-yellow-100';
  return 'text-red-600 bg-red-100';
}

export function getDSCRLabel(dscr) {
  if (dscr >= 2.0) return 'Healthy';
  if (dscr >= 1.0) return 'Moderate';
  return 'Risky';
}

export function getVerdictColor(verdict) {
  switch (verdict) {
    case 'VIABLE':
      return 'text-green-600 bg-green-100 border-green-200';
    case 'VIABLE WITH MODIFICATIONS':
      return 'text-yellow-600 bg-yellow-100 border-yellow-200';
    case 'NOT RECOMMENDED AS STRUCTURED':
      return 'text-red-600 bg-red-100 border-red-200';
    default:
      return 'text-gray-600 bg-gray-100 border-gray-200';
  }
}

export function getRiskColor(risk) {
  switch (risk) {
    case 'High':
      return 'text-red-600 bg-red-100';
    case 'Medium':
      return 'text-yellow-600 bg-yellow-100';
    case 'Low':
      return 'text-green-600 bg-green-100';
    default:
      return 'text-gray-600 bg-gray-100';
  }
}

export function getHealthRecommendationColor(recommendation) {
  if (recommendation.includes('🟢') || recommendation.includes('Proceed')) {
    return 'text-green-600 bg-green-100';
  }
  if (recommendation.includes('🟡') || recommendation.includes('modifications')) {
    return 'text-yellow-600 bg-yellow-100';
  }
  return 'text-red-600 bg-red-100';
}

export function truncateText(text, maxLength = 100) {
  if (!text || text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
}

export function generateId() {
  return Math.random().toString(36).substring(2, 15);
}

export function debounce(fn, delay) {
  let timeoutId;
  return (...args) => {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => fn(...args), delay);
  };
}
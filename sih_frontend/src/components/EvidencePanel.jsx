import { ChevronDown } from 'lucide-react';
import { useState } from 'react';

export default function EvidencePanel({ evidence, className = '' }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className={`card ${className}`}>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
          <svg className="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          Evidence Trail
        </h3>
        <button
          onClick={() => setExpanded(!expanded)}
          className="btn-ghost p-2"
          aria-expanded={expanded}
        >
          <ChevronDown className={`w-5 h-5 transition-transform ${expanded ? 'rotate-180' : ''}`} />
        </button>
      </div>

      <div className={`transition-all duration-300 ${expanded ? 'max-h-96 overflow-y-auto' : 'max-h-0 overflow-hidden'}`}>
        <div className="space-y-3">
          {evidence.evidence.map((item, index) => (
            <div key={index} className="border border-gray-200 rounded-lg p-4 bg-gray-50">
              <div className="flex items-start gap-3">
                <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center flex-shrink-0">
                  <span className="text-primary-600 font-bold text-sm">{index + 1}</span>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-900">{item.claim}</p>
                  <p className="text-sm text-gray-600 mt-1">Source: {item.source}</p>
                  <p className="text-xs text-gray-500 mt-1 font-mono bg-white p-2 rounded">{item.details}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <p className="text-center text-sm text-gray-500 mt-3">
        {expanded ? 'Click to collapse' : 'Click to expand'} ({evidence.evidence.length} items)
      </p>
    </div>
  );
}
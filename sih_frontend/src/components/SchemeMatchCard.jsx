import { Landmark, Sparkles, ChevronRight } from 'lucide-react';
import { benefitBadge, benefitHeadline } from '../utils/schemeBenefit';

/**
 * One scheme match on the browse page. Deliberately compact (name/badge/headline
 * number, tap to expand) - status doc section 6, Phase C: "collapsed, tap to expand".
 */
export default function SchemeMatchCard({ match, onClick }) {
  const badge = benefitBadge(match.benefit);
  const headline = benefitHeadline(match.benefit);

  return (
    <button
      onClick={onClick}
      className="card text-left w-full hover:shadow-md hover:border-primary-300 transition-all flex flex-col gap-3"
    >
      <div className="flex items-start justify-between gap-2">
        <div className="flex flex-wrap gap-1.5">
          <span className="pill bg-surface-container text-gray-600 text-[11px] flex items-center gap-1">
            <Landmark className="w-3 h-3" />
            {match.level === 'state' ? match.state : 'Central'}
          </span>
          <span className="pill bg-primary-50 text-primary-700 text-[11px]">{badge}</span>
          {match.enhancementApplied && (
            <span className="pill bg-amber-50 text-amber-700 text-[11px] flex items-center gap-1">
              <Sparkles className="w-3 h-3" />
              Enhanced
            </span>
          )}
        </div>
        <ChevronRight className="w-4 h-4 text-gray-300 flex-shrink-0 mt-0.5" />
      </div>

      <h4 className="font-semibold text-gray-900 leading-snug">{match.name}</h4>

      {headline && (
        <p className="text-lg font-bold text-primary-700">{headline}</p>
      )}

      {match.eligibilityHuman && (
        <p className="text-xs text-gray-500 line-clamp-2">{match.eligibilityHuman}</p>
      )}
    </button>
  );
}

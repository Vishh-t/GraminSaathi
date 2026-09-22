import { X, ExternalLink, FileText, ListChecks, ThumbsUp, ThumbsDown, Sparkles, Landmark } from 'lucide-react';
import { benefitBadge, benefitHeadline } from '../utils/schemeBenefit';

function BenefitRow({ benefit }) {
  if (!benefit) return null;
  return (
    <div className="rounded-lg border border-gray-100 bg-gray-50 p-3">
      <p className="text-sm text-gray-800">{benefit.displayText}</p>
      {benefit.components?.length > 0 && (
        <div className="mt-2 pl-3 border-l-2 border-primary-200 space-y-2">
          {benefit.components.map((c, i) => <BenefitRow key={i} benefit={c} />)}
        </div>
      )}
    </div>
  );
}

/**
 * Detail view for one scheme match (status doc section 6, Phase C): eligibility explanation incl.
 * any special-category enhancement, the computed benefit (handoff §6 display rules), documents,
 * application steps, pros/cons, portal link — all sourced from a single SchemesController response
 * item, no extra call.
 */
export default function SchemeDetailModal({ match, onClose }) {
  if (!match) return null;
  const headline = benefitHeadline(match.benefit);

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/40 p-0 sm:p-4">
      <div className="bg-white w-full sm:max-w-2xl sm:rounded-2xl rounded-t-2xl shadow-xl max-h-[92vh] flex flex-col">
        {/* Header */}
        <div className="flex items-start justify-between gap-3 px-6 py-4 border-b border-gray-100">
          <div>
            <div className="flex flex-wrap gap-1.5 mb-1.5">
              <span className="pill bg-surface-container text-gray-600 text-[11px] flex items-center gap-1">
                <Landmark className="w-3 h-3" />
                {match.level === 'state' ? match.state : 'Central'}
              </span>
              <span className="pill bg-primary-50 text-primary-700 text-[11px]">{benefitBadge(match.benefit)}</span>
            </div>
            <h3 className="text-lg font-semibold text-gray-900 leading-snug">{match.name}</h3>
            {match.implementingAgency && (
              <p className="text-xs text-gray-500 mt-0.5">{match.implementingAgency}</p>
            )}
          </div>
          <button onClick={onClose} className="btn-ghost !p-2 flex-shrink-0" aria-label="Close">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="px-6 py-5 overflow-y-auto flex-1 space-y-5">
          {headline && (
            <div className="rounded-lg bg-primary-50 border border-primary-100 p-4">
              <p className="text-2xl font-bold text-primary-700">{headline}</p>
              {match.benefit?.amount != null && match.benefit?.displayText && (
                <p className="text-sm text-primary-800 mt-1">{match.benefit.displayText}</p>
              )}
            </div>
          )}

          {match.benefit?.components?.length > 0 && (
            <div>
              <p className="text-sm font-medium text-gray-700 mb-2">Benefit breakdown</p>
              <BenefitRow benefit={match.benefit} />
            </div>
          )}

          {match.enhancementApplied && (
            <div className="rounded-lg border border-amber-200 bg-amber-50 p-3 flex items-start gap-2">
              <Sparkles className="w-4 h-4 text-amber-600 flex-shrink-0 mt-0.5" />
              <div>
                <p className="text-sm font-medium text-amber-800">Special-category enhancement applied</p>
                {match.enhancementApplied.note && (
                  <p className="text-xs text-amber-700 mt-0.5">{match.enhancementApplied.note}</p>
                )}
              </div>
            </div>
          )}

          {match.eligibilityHuman && (
            <div>
              <p className="text-sm font-medium text-gray-700 mb-1">Who's eligible</p>
              <p className="text-sm text-gray-600">{match.eligibilityHuman}</p>
            </div>
          )}

          {(match.pros?.length > 0 || match.cons?.length > 0) && (
            <div className="grid sm:grid-cols-2 gap-4">
              {match.pros?.length > 0 && (
                <div>
                  <p className="text-sm font-medium text-gray-700 mb-1.5 flex items-center gap-1.5">
                    <ThumbsUp className="w-3.5 h-3.5 text-green-600" />Pros
                  </p>
                  <ul className="text-sm text-gray-600 space-y-1 list-disc list-inside">
                    {match.pros.map((p, i) => <li key={i}>{p}</li>)}
                  </ul>
                </div>
              )}
              {match.cons?.length > 0 && (
                <div>
                  <p className="text-sm font-medium text-gray-700 mb-1.5 flex items-center gap-1.5">
                    <ThumbsDown className="w-3.5 h-3.5 text-red-500" />Cons
                  </p>
                  <ul className="text-sm text-gray-600 space-y-1 list-disc list-inside">
                    {match.cons.map((c, i) => <li key={i}>{c}</li>)}
                  </ul>
                </div>
              )}
            </div>
          )}

          {match.documentsRequired?.length > 0 && (
            <div>
              <p className="text-sm font-medium text-gray-700 mb-1.5 flex items-center gap-1.5">
                <FileText className="w-3.5 h-3.5 text-primary-600" />Documents required
              </p>
              <ul className="text-sm text-gray-600 space-y-1 list-disc list-inside">
                {match.documentsRequired.map((d, i) => <li key={i}>{d}</li>)}
              </ul>
            </div>
          )}

          {match.applicationSteps?.length > 0 && (
            <div>
              <p className="text-sm font-medium text-gray-700 mb-1.5 flex items-center gap-1.5">
                <ListChecks className="w-3.5 h-3.5 text-primary-600" />How to apply
              </p>
              <ol className="text-sm text-gray-600 space-y-1 list-decimal list-inside">
                {match.applicationSteps.map((s, i) => <li key={i}>{s}</li>)}
              </ol>
            </div>
          )}

          {match.applicationChannel?.length > 0 && (
            <p className="text-xs text-gray-400">Apply via: {match.applicationChannel.join(', ')}</p>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between gap-3 px-6 py-4 border-t border-gray-100">
          <button onClick={onClose} className="btn-secondary">Close</button>
          {match.portalUrl && (
            <a
              href={match.portalUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="btn-primary flex items-center gap-1.5"
            >
              Visit portal<ExternalLink className="w-4 h-4" />
            </a>
          )}
        </div>
      </div>
    </div>
  );
}

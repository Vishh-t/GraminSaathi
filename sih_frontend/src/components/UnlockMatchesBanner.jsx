import { useState } from 'react';
import { Sparkles, X } from 'lucide-react';
import ApplicantIntakeModal from './intake/ApplicantIntakeModal';

const DISMISS_KEY = 'unlockMatchesBannerDismissed';

/**
 * Dismissible banner shown after first analyze results, prompting the
 * applicant intake form. Not shown at signup — only once there's already a
 * result on screen for it to promise "more of this" against.
 *
 * Dismissal is remembered locally (not sent to the backend) — it's just
 * "don't nag me again on this device", separate from whether the profile
 * itself is actually filled in.
 *
 * Completing the intake form (all 3 steps, reaching "you're all set") is
 * ALSO treated as a dismissal reason, not just the explicit X — otherwise the
 * banner would keep reappearing on every future analysis page even though
 * the person already answered it. The dismiss itself is deferred to the
 * modal's onClose (not the moment saving succeeds): the banner returns null
 * once dismissed, and since the modal renders as this component's own child,
 * dismissing immediately on save would unmount the modal mid-flow and skip
 * past the "you're all set" confirmation screen.
 */
export default function UnlockMatchesBanner({ className = '', onProfileSaved }) {
  const [dismissed, setDismissed] = useState(() => localStorage.getItem(DISMISS_KEY) === '1');
  const [modalOpen, setModalOpen] = useState(false);
  const [completed, setCompleted] = useState(false);

  if (dismissed) return null;

  const handleDismiss = () => {
    localStorage.setItem(DISMISS_KEY, '1');
    setDismissed(true);
  };

  const handleModalClose = () => {
    setModalOpen(false);
    if (completed) handleDismiss();
  };

  return (
    <>
      <div className={`card border-primary-200 bg-primary-50/60 flex items-start gap-3 ${className}`}>
        <div className="w-9 h-9 bg-primary-100 rounded-lg flex items-center justify-center flex-shrink-0">
          <Sparkles className="w-5 h-5 text-primary-600" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="font-semibold text-gray-900">Unlock more loan and scheme matches</p>
          <p className="text-sm text-gray-600 mt-0.5">
            A couple of quick questions about you (category, income, business details) can surface
            state-specific schemes you're not seeing yet — takes under a minute, answer only what applies.
          </p>
          <button onClick={() => setModalOpen(true)} className="btn-secondary !py-1.5 text-sm mt-3">
            Answer a few questions
          </button>
        </div>
        <button onClick={handleDismiss} className="btn-ghost !p-1.5 flex-shrink-0" aria-label="Dismiss">
          <X className="w-4 h-4" />
        </button>
      </div>

      {modalOpen && (
        <ApplicantIntakeModal
          onClose={handleModalClose}
          onSaved={() => { setCompleted(true); onProfileSaved?.(); }}
        />
      )}
    </>
  );
}

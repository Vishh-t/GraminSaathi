import { useState, useEffect } from 'react';
import { X, ChevronLeft, ChevronRight, Loader2, CheckCircle2 } from 'lucide-react';
import { profileAPI } from '../../services/api';
import { getErrorMessage } from '../../utils/errors';
import { EMPTY_APPLICANT_PROFILE } from './constants';
import IntakeStepAbout from './IntakeStepAbout';
import IntakeStepEligibility from './IntakeStepEligibility';
import IntakeStepBusiness from './IntakeStepBusiness';

const STEPS = [
  { title: 'About you', Component: IntakeStepAbout },
  { title: 'Does this apply to you?', Component: IntakeStepEligibility },
  { title: 'Income & business', Component: IntakeStepBusiness },
];

/**
 * 3-screen chip-based applicant intake flow. Loads whatever's already saved
 * (profileAPI.getApplicant), lets the person fill in more across the 3 steps,
 * and PUTs after each step (merge-safe on the backend — see profileAPI's own
 * comment) so nothing is lost if they close partway through.
 *
 * All fields are optional everywhere; this never blocks on validation. More
 * filled in just means more schemes become eligible once matching is wired up
 * (see Project_Docs/SCHEMES_STATUS_AND_NEXT_STEPS.md section 6, phases B/C).
 */
export default function ApplicantIntakeModal({ onClose, onSaved }) {
  const [step, setStep] = useState(0);
  const [profile, setProfile] = useState(EMPTY_APPLICANT_PROFILE);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [done, setDone] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await profileAPI.getApplicant();
        if (!cancelled) setProfile({ ...EMPTY_APPLICANT_PROFILE, ...res.data });
      } catch (err) {
        if (!cancelled) setError(getErrorMessage(err, "Couldn't load your saved details — starting fresh."));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  const persist = async () => {
    setSaving(true);
    setError(null);
    try {
      await profileAPI.saveApplicant(profile);
      return true;
    } catch (err) {
      setError(getErrorMessage(err));
      return false;
    } finally {
      setSaving(false);
    }
  };

  const handleNext = async () => {
    const ok = await persist();
    if (!ok) return;
    if (step < STEPS.length - 1) {
      setStep((s) => s + 1);
    } else {
      setDone(true);
      onSaved?.(profile);
    }
  };

  const handleBack = () => setStep((s) => Math.max(0, s - 1));

  const { title, Component } = STEPS[step];

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/40 p-0 sm:p-4">
      <div className="bg-white w-full sm:max-w-xl sm:rounded-2xl rounded-t-2xl shadow-xl max-h-[92vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <div>
            <p className="text-xs font-medium uppercase tracking-wide text-gray-400">
              Step {step + 1} of {STEPS.length}
            </p>
            <h3 className="text-lg font-semibold text-gray-900">{done ? "You're all set" : title}</h3>
          </div>
          <button onClick={onClose} className="btn-ghost !p-2" aria-label="Close">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Progress dots */}
        <div className="flex items-center gap-1.5 px-6 pt-4">
          {STEPS.map((_, i) => (
            <div
              key={i}
              className={`h-1.5 flex-1 rounded-full transition-colors ${
                i <= step ? 'bg-primary-600' : 'bg-gray-200'
              }`}
            />
          ))}
        </div>

        {/* Body */}
        <div className="px-6 py-5 overflow-y-auto flex-1">
          {loading ? (
            <div className="flex items-center justify-center py-16">
              <Loader2 className="w-6 h-6 animate-spin text-primary-600" />
            </div>
          ) : done ? (
            <div className="flex flex-col items-center text-center py-10">
              <CheckCircle2 className="w-14 h-14 text-primary-600 mb-4" />
              <p className="text-gray-700">
                Saved. Your scheme matches will use this from now on — you won't be asked again.
              </p>
            </div>
          ) : (
            <Component profile={profile} onChange={setProfile} />
          )}

          {error && (
            <p className="mt-4 text-sm text-danger-600 bg-danger-50 border border-danger-200 rounded-lg px-3 py-2">
              {error}
            </p>
          )}
        </div>

        {/* Footer */}
        {!loading && (
          <div className="flex items-center justify-between px-6 py-4 border-t border-gray-100">
            {done ? (
              <button onClick={onClose} className="btn-primary w-full">Done</button>
            ) : (
              <>
                <button
                  onClick={handleBack}
                  disabled={step === 0 || saving}
                  className="btn-ghost flex items-center gap-1"
                >
                  <ChevronLeft className="w-4 h-4" />
                  Back
                </button>
                <button onClick={onClose} className="text-sm text-gray-400 hover:text-gray-600">
                  Skip for now
                </button>
                <button
                  onClick={handleNext}
                  disabled={saving}
                  className="btn-primary flex items-center gap-1"
                >
                  {saving ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : step === STEPS.length - 1 ? (
                    'Save'
                  ) : (
                    <>Next<ChevronRight className="w-4 h-4" /></>
                  )}
                </button>
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

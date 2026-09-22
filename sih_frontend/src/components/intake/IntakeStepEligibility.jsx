import { ChipBooleanToggle } from './Chip';
import { BOOLEAN_FLAG_GROUPS } from './constants';

/**
 * Step 2 of 3 — "Does this apply to you?" Every flag here is one of the ~27
 * applicant.* booleans that special-category enhancements and some base
 * eligibility rules gate on. Tap to turn on; tap again to clear. Nothing here
 * is required — an unset flag is treated as "no"/"unknown" by the rules, which
 * is why chips default to off rather than needing an explicit "no" choice.
 */
export default function IntakeStepEligibility({ profile, onChange }) {
  const toggle = (key) => (value) => onChange({ ...profile, [key]: value });

  return (
    <div className="space-y-6">
      <p className="text-sm text-gray-500">
        Tap anything that applies to you. This is what unlocks state-specific and
        category-specific schemes beyond the general central ones — skip anything
        you're unsure about.
      </p>
      {BOOLEAN_FLAG_GROUPS.map((group) => (
        <div key={group.title}>
          <p className="text-xs font-semibold uppercase tracking-wide text-gray-400 mb-2">
            {group.title}
          </p>
          <div className="flex flex-wrap gap-2">
            {group.items.map((item) => (
              <ChipBooleanToggle
                key={item.key}
                label={item.label}
                checked={profile[item.key]}
                onToggle={toggle(item.key)}
              />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

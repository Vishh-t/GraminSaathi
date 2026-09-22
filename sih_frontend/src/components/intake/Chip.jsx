import { Check } from 'lucide-react';

/** A single toggleable pill. Purely presentational — parent owns selected state. */
export default function Chip({ label, selected, onClick, className = '' }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={selected}
      className={`pill border gap-1 transition-colors ${
        selected
          ? 'bg-primary-600 border-primary-600 text-white'
          : 'bg-white border-gray-300 text-gray-700 hover:border-primary-400 hover:text-primary-700'
      } ${className}`}
    >
      {selected && <Check className="w-3.5 h-3.5" />}
      {label}
    </button>
  );
}

/**
 * A wrapping grid of single-select chips for a small enum field (rural_urban,
 * business_stage, category, sector, ...). Clicking the already-selected chip
 * clears it back to null — every field here is optional, "don't know" is a
 * valid answer.
 */
export function ChipSingleSelect({ options, value, onChange, className = '' }) {
  return (
    <div className={`flex flex-wrap gap-2 ${className}`}>
      {options.map((opt) => (
        <Chip
          key={opt.value}
          label={opt.label}
          selected={value === opt.value}
          onClick={() => onChange(value === opt.value ? null : opt.value)}
        />
      ))}
    </div>
  );
}

/** A single boolean flag rendered as a toggle chip. */
export function ChipBooleanToggle({ label, checked, onToggle, className = '' }) {
  return (
    <Chip
      label={label}
      selected={checked === true}
      onClick={() => onToggle(checked === true ? null : true)}
      className={className}
    />
  );
}

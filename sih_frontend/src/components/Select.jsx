import { useState, useRef, useEffect } from 'react';
import { ChevronDown, Check } from 'lucide-react';

/**
 * Custom-styled dropdown to replace native <select>. Browsers render a native
 * <select>'s open option-list using OS UI that CSS cannot restyle (flat white
 * background, hard-coded blue selection highlight, system font) — which is
 * why a `select.input-field` still looked broken the moment you opened it,
 * even though the closed box matched the design system. This renders the
 * whole list ourselves so open and closed states both match.
 *
 * Props:
 *  - value: currently selected value
 *  - onChange: (value) => void
 *  - options: [{ value, label, sublabel? }] or [string]
 *  - placeholder, disabled, className
 */
export default function Select({ value, onChange, options = [], placeholder = 'Select...', disabled = false, className = '' }) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);

  const normalized = options.map(o => (typeof o === 'string' ? { value: o, label: o } : o));
  const selected = normalized.find(o => o.value === value);

  useEffect(() => {
    if (!open) return;
    const handleClickOutside = (e) => {
      if (rootRef.current && !rootRef.current.contains(e.target)) setOpen(false);
    };
    const handleEscape = (e) => { if (e.key === 'Escape') setOpen(false); };
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [open]);

  return (
    <div ref={rootRef} className={`relative ${className}`}>
      <button
        type="button"
        onClick={() => !disabled && setOpen(o => !o)}
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={open}
        className="input-field flex items-center justify-between text-left disabled:cursor-not-allowed"
      >
        <span className={`truncate ${selected ? 'text-gray-900' : 'text-gray-400'}`}>
          {selected ? selected.label : placeholder}
        </span>
        <ChevronDown className={`w-4 h-4 text-gray-500 shrink-0 ml-2 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && (
        <div
          role="listbox"
          className="absolute z-50 mt-1.5 w-full max-h-64 overflow-y-auto bg-white border border-gray-200 rounded-xl shadow-lg py-1.5 animate-fade-in"
        >
          {normalized.length === 0 && (
            <p className="px-4 py-2.5 text-sm text-gray-400">No options</p>
          )}
          {normalized.map((opt) => {
            const isSelected = opt.value === value;
            return (
              <button
                key={opt.value}
                type="button"
                role="option"
                aria-selected={isSelected}
                onClick={() => { onChange(opt.value); setOpen(false); }}
                className={`w-full flex items-center justify-between gap-2 px-4 py-2.5 text-sm text-left transition-colors ${
                  isSelected ? 'bg-primary-50 text-primary-700 font-medium' : 'text-gray-700 hover:bg-gray-50'
                }`}
              >
                <span className="truncate">
                  {opt.label}
                  {opt.sublabel && <span className="text-gray-400 font-normal"> {opt.sublabel}</span>}
                </span>
                {isSelected && <Check className="w-4 h-4 text-primary-600 shrink-0" />}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}

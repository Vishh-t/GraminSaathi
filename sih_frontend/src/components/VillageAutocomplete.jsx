import { useState, useRef, useEffect, useCallback } from 'react';
import { Search, MapPin, Loader2 } from 'lucide-react';
import { villageAPI } from '../services/api';

/**
 * Search-as-you-type village picker backed by the real ~633k-village Census/LGD table
 * (GET /api/villages/search), replacing the old fixed-list `Select` fed by the legacy
 * `/api/villages` demo endpoint (only ~4 hardcoded villages - see
 * GraminSaathi_Backend/Project_Docs/RECOMMENDATION_ENGINE_BUILD_LOG.md, "part 7"). A full village
 * list can't be preloaded into a dropdown at this scale, hence search-as-you-type instead of `Select`.
 *
 * Props:
 *  - value: currently selected/typed village name (string) - controlled from the parent
 *  - onChange: (name, villageObject | null) => void. villageObject is the full VillageSearchResponse
 *              row (id, name, block, district, state, latitude, longitude, population2011,
 *              households2011, source) when the user picked a suggestion, or null when they're still
 *              typing/haven't picked one (parent may want to gate a submit button on this).
 *  - placeholder, disabled, className
 */
export default function VillageAutocomplete({ value, onChange, placeholder = 'Search for a village...', disabled = false, className = '' }) {
  const [query, setQuery] = useState(value || '');
  const [open, setOpen] = useState(false);
  const [options, setOptions] = useState([]);
  const [loading, setLoading] = useState(false);
  const rootRef = useRef(null);
  const debounceRef = useRef(null);

  // Keep the input in sync when the parent changes `value` from outside (e.g. a deep-link query param,
  // or the "reset" button on MapPage) rather than via a suggestion pick here.
  useEffect(() => {
    setQuery(value || '');
  }, [value]);

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

  useEffect(() => () => { if (debounceRef.current) clearTimeout(debounceRef.current); }, []);

  const runSearch = useCallback((q) => {
    const trimmed = q.trim();
    if (trimmed.length < 2) {
      setOptions([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    villageAPI.search(trimmed)
      .then(res => setOptions(res.data))
      .catch(() => setOptions([]))
      .finally(() => setLoading(false));
  }, []);

  const handleInputChange = (e) => {
    const q = e.target.value;
    setQuery(q);
    setOpen(true);
    onChange(q, null); // typing invalidates any previously picked village object
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => runSearch(q), 250);
  };

  const handleSelect = (village) => {
    setQuery(village.name);
    setOpen(false);
    setOptions([]);
    onChange(village.name, village);
  };

  return (
    <div ref={rootRef} className={`relative ${className}`}>
      <div className="relative">
        <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
        <input
          type="text"
          value={query}
          onChange={handleInputChange}
          onFocus={() => setOpen(true)}
          disabled={disabled}
          placeholder={placeholder}
          className="input-field pl-9"
          autoComplete="off"
        />
        {loading && <Loader2 className="w-4 h-4 text-gray-400 animate-spin absolute right-3 top-1/2 -translate-y-1/2" />}
      </div>

      {open && query.trim().length >= 2 && (
        <div
          role="listbox"
          className="absolute z-50 mt-1.5 w-full max-h-64 overflow-y-auto bg-white border border-gray-200 rounded-xl shadow-lg py-1.5 animate-fade-in"
        >
          {!loading && options.length === 0 && (
            <p className="px-4 py-2.5 text-sm text-gray-400">No villages found</p>
          )}
          {options.map((v) => (
            <button
              key={v.id}
              type="button"
              role="option"
              onClick={() => handleSelect(v)}
              className="w-full flex items-start gap-2 px-4 py-2.5 text-sm text-left text-gray-700 hover:bg-gray-50 transition-colors"
            >
              <MapPin className="w-3.5 h-3.5 text-primary-600 shrink-0 mt-0.5" />
              <span className="truncate">
                <span className="font-medium text-gray-900">{v.name}</span>
                <span className="text-gray-400"> · {v.block ? `${v.block}, ` : ''}{v.district}, {v.state}</span>
              </span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

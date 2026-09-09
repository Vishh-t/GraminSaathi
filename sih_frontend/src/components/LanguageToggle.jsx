import { useState, useRef, useEffect } from 'react';
import { useI18n, SUPPORTED_LANGUAGES } from '../i18n/i18n';
import { Check, ChevronDown, Globe } from 'lucide-react';

export default function LanguageToggle() {
  const { language, setLanguage, t } = useI18n();
  const [open, setOpen] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    function onClickOutside(e) {
      if (menuRef.current && !menuRef.current.contains(e.target)) setOpen(false);
    }
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  const current = SUPPORTED_LANGUAGES.find((l) => l.code === language) ?? SUPPORTED_LANGUAGES[0];

  return (
    <div className="relative" ref={menuRef}>
      <button
        onClick={() => setOpen((v) => !v)}
        className="btn-ghost flex items-center gap-2"
        aria-label={t('language.toggle')}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        <Globe className="w-[18px] h-[18px]" />
        <span className="hidden sm:inline">{t(current.labelKey)}</span>
        <ChevronDown className="w-3.5 h-3.5 text-gray-400 hidden sm:block" />
      </button>

      {open && (
        <div
          role="listbox"
          className="absolute right-0 mt-2 w-40 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-50"
        >
          {SUPPORTED_LANGUAGES.map((lang) => (
            <button
              key={lang.code}
              role="option"
              aria-selected={lang.code === language}
              onClick={() => {
                setLanguage(lang.code);
                setOpen(false);
              }}
              className="w-full flex items-center justify-between gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
            >
              <span>{t(lang.labelKey)}</span>
              {lang.code === language && <Check className="w-4 h-4 text-primary-600" />}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
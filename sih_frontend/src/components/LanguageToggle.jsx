import { useI18n } from '../i18n/i18n';

export default function LanguageToggle() {
  const { language, toggleLanguage, t } = useI18n();

  return (
    <button
      onClick={toggleLanguage}
      className="btn-ghost flex items-center gap-2"
      aria-label={t('language.toggle')}
    >
      <span className="text-lg">🌐</span>
      <span className="hidden sm:inline">{language === 'en' ? t('language.english') : t('language.hindi')}</span>
    </button>
  );
}
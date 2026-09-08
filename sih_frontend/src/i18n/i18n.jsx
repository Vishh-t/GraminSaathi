import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import en from './en.json';
import hi from './hi.json';

const translations = { en, hi };

const I18nContext = createContext(null);

export function I18nProvider({ children }) {
  const [language, setLanguage] = useState(() => {
    const saved = localStorage.getItem('language');
    return saved || 'en';
  });

  useEffect(() => {
    localStorage.setItem('language', language);
  }, [language]);

  const t = useCallback((key, params = {}) => {
    let result = translations[language]?.[key];

    if (result === undefined) {
      // Fallback to English
      result = translations.en?.[key];
    }

    if (typeof result === 'string') {
      return Object.entries(params).reduce((str, [k, v]) => 
        str.replace(new RegExp(`{{${k}}}`, 'g'), v), result
      );
    }
    
    return key;
  }, [language]);

  const toggleLanguage = useCallback(() => {
    setLanguage(prev => prev === 'en' ? 'hi' : 'en');
  }, []);

  return (
    <I18nContext.Provider value={{ language, setLanguage, toggleLanguage, t }}>
      {children}
    </I18nContext.Provider>
  );
}

export function useI18n() {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error('useI18n must be used within an I18nProvider');
  }
  return context;
}
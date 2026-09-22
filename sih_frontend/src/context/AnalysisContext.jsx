import { createContext, useContext, useState } from 'react';

const STORAGE_KEY = 'gs_last_analysis_context';

const AnalysisContext = createContext(null);

function loadStored() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : {};
  } catch {
    return {};
  }
}

/**
 * Remembers the last village / business category / capital the person entered
 * anywhere in the app (Discovery, Analysis, Map, Simulator all read + write the
 * same values here) and persists it to localStorage so it survives a full page
 * reload too. Every page treats this as a FALLBACK only — an explicit URL query
 * param (a deep link from Discovery, a shared report link, etc.) always wins;
 * this just fills in when a page is reached with no params at all, e.g. a bare
 * TopNav click or a Dashboard quick action.
 */
export function AnalysisContextProvider({ children }) {
  const [context, setContext] = useState(loadStored);

  const updateContext = (patch) => {
    setContext((prev) => {
      const next = { ...prev, ...patch };
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
      } catch {
        // Private-mode / quota errors shouldn't break navigation - the
        // in-memory context still works for the rest of this session.
      }
      return next;
    });
  };

  return (
    <AnalysisContext.Provider value={{ ...context, updateContext }}>
      {children}
    </AnalysisContext.Provider>
  );
}

export function useAnalysisContext() {
  const ctx = useContext(AnalysisContext);
  if (!ctx) throw new Error('useAnalysisContext must be used within AnalysisContextProvider');
  return ctx;
}

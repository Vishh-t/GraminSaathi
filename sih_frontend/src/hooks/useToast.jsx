import { createContext, useContext, useState, useCallback } from 'react';

const ToastContext = createContext(null);

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const showToast = useCallback((message, type = 'info', duration = 4000) => {
    const id = Date.now();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, duration);
  }, []);

  const hideToast = useCallback((id) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  return (
    <ToastContext.Provider value={{ toasts, showToast, hideToast }}>
      <div>
        {children}
        <div className="fixed bottom-4 right-4 z-50 space-y-2 pointer-events-none">
          {toasts.map(toast => (
            <div
              key={toast.id}
              className={`px-4 py-3 rounded-lg shadow-lg flex items-center gap-3 min-w-[280px] max-w-md animate-slide-in pointer-events-auto ${
                toast.type === 'success' ? 'bg-green-600 text-white' :
                toast.type === 'error' ? 'bg-red-600 text-white' :
                toast.type === 'warning' ? 'bg-yellow-600 text-white' :
                'bg-blue-600 text-white'
              }`}
            >
              <span className="flex-1">{toast.message}</span>
              <button onClick={() => hideToast(toast.id)} className="text-white hover:text-gray-200">
                ✕
              </button>
            </div>
          ))}
        </div>
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
}
import React, { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';

const ToastContext = createContext(null);

function nextIdFactory() {
  let current = 1;
  return () => current++;
}

const nextId = nextIdFactory();

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const timersRef = useRef(new Map());

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
    const timers = timersRef.current;
    const timer = timers.get(id);
    if (timer) {
      clearTimeout(timer);
      timers.delete(id);
    }
  }, []);

  const pushToast = useCallback((toast) => {
    const id = nextId();
    const ttlMs = Number.isFinite(Number(toast?.ttlMs)) ? Number(toast.ttlMs) : 4000;
    const type = toast?.type || 'info';
    const message = toast?.message || '';

    setToasts((prev) => [{ id, type, message }, ...prev].slice(0, 5));

    const timer = setTimeout(() => removeToast(id), Math.max(1500, ttlMs));
    timersRef.current.set(id, timer);
    return id;
  }, [removeToast]);

  const value = useMemo(() => ({ pushToast, removeToast }), [pushToast, removeToast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="ems-toast-viewport" aria-live="polite" aria-relevant="additions removals">
        {toasts.map((t) => {
          const variant = t.type === 'success'
            ? 'success'
            : t.type === 'error'
              ? 'danger'
              : t.type === 'warning'
                ? 'warning'
                : 'secondary';

          return (
            <div
              key={t.id}
              className={`alert alert-${variant} d-flex align-items-start gap-2 shadow-sm mb-2`}
              role={t.type === 'error' ? 'alert' : 'status'}
            >
              <div className="flex-grow-1">{t.message}</div>
              <button
                type="button"
                className="btn-close"
                aria-label="Close notification"
                onClick={() => removeToast(t.id)}
              />
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToasts() {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    throw new Error('useToasts must be used within ToastProvider');
  }
  return ctx;
}

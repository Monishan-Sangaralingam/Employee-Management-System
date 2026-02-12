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

    const show = useCallback((toast) => {
        const id = nextId();
        const ttlMs = Number.isFinite(Number(toast?.ttlMs)) ? Number(toast.ttlMs) : 4000;
        const variant = toast?.variant || 'secondary';
        const title = toast?.title || '';
        const message = toast?.message || '';

        setToasts((prev) => [{ id, variant, title, message }, ...prev].slice(0, 5));

        const timer = setTimeout(() => removeToast(id), Math.max(1500, ttlMs));
        timersRef.current.set(id, timer);
        return id;
    }, [removeToast]);

    // Backwards-compatible API used across the app today.
    // Accepts { type: 'success'|'error'|'warning'|'info', message, ttlMs }
    const pushToast = useCallback((toast) => {
        const type = toast?.type || 'info';
        const variant = type === 'success'
            ? 'success'
            : type === 'error'
                ? 'danger'
                : type === 'warning'
                    ? 'warning'
                    : 'secondary';
        const title = toast?.title || (type === 'error' ? 'Error' : type === 'success' ? 'Success' : '');
        return show({
            ttlMs: toast?.ttlMs,
            variant,
            title,
            message: toast?.message,
        });
    }, [show]);

    const value = useMemo(() => ({ show, pushToast, removeToast }), [show, pushToast, removeToast]);

    return (
        <ToastContext.Provider value={value}>
            {children}
            <div className="ems-toast-viewport" aria-live="polite" aria-relevant="additions removals">
                {toasts.map((t) => {
                    return (
                        <div
                            key={t.id}
                            className={`alert alert-${t.variant} d-flex align-items-start gap-2 shadow-sm mb-2`}
                            role={t.variant === 'danger' ? 'alert' : 'status'}
                        >
                            <div className="flex-grow-1">
                                {t.title ? <div className="fw-semibold">{t.title}</div> : null}
                                <div>{t.message}</div>
                            </div>
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

export function useToast() {
    const ctx = useContext(ToastContext);
    if (!ctx) {
        throw new Error('useToast must be used within ToastProvider');
    }
    return {
        show: ctx.show,
        remove: ctx.removeToast,
    };
}

export function useToasts() {
    const ctx = useContext(ToastContext);
    if (!ctx) {
        throw new Error('useToasts must be used within ToastProvider');
    }
    return ctx;
}

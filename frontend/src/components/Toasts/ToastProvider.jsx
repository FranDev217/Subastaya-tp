import { useCallback, useEffect, useRef, useState } from "react";
import { ToastsContext } from "../../context/toastsContext";
import "./Toasts.css";

let contadorId = 0;

function iconoDe(tipo) {
  switch (tipo) {
    case "success":
      return "✓";
    case "error":
      return "!";
    case "warning":
      return "⏱";
    default:
      return "i";
  }
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const timers = useRef(new Map());

  const quitar = useCallback((id) => {
    setToasts((previos) => previos.filter((toast) => toast.id !== id));
    const timer = timers.current.get(id);
    if (timer) {
      clearTimeout(timer);
      timers.current.delete(id);
    }
  }, []);

  const mostrar = useCallback(
    ({ tipo = "info", mensaje, duracion = 4500 }) => {
      const id = ++contadorId;
      setToasts((previos) => [...previos, { id, tipo, mensaje }]);
      const timer = setTimeout(() => quitar(id), duracion);
      timers.current.set(id, timer);
      return id;
    },
    [quitar],
  );

  useEffect(
    () => () => {
      timers.current.forEach((timer) => clearTimeout(timer));
      timers.current.clear();
    },
    [],
  );

  return (
    <ToastsContext.Provider value={{ mostrar, quitar }}>
      {children}
      <div className="toasts" aria-live="polite">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={`toast toast--${toast.tipo}`}
            role="status"
          >
            <span className="toast__icono">{iconoDe(toast.tipo)}</span>
            <p className="toast__mensaje">{toast.mensaje}</p>
            <button
              type="button"
              className="toast__cerrar"
              onClick={() => quitar(toast.id)}
              aria-label="Cerrar aviso"
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastsContext.Provider>
  );
}

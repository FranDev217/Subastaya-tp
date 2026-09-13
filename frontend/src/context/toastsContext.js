import { createContext, useContext } from "react";

export const ToastsContext = createContext(null);

export function useToasts() {
  const contexto = useContext(ToastsContext);
  if (!contexto) {
    throw new Error("useToasts debe usarse dentro de un ToastProvider");
  }
  return contexto;
}

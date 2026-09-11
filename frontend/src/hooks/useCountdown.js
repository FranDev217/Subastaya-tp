import { useState, useEffect } from "react";

// El backend serializa LocalDateTime sin zona horaria (ej: "2026-09-09T21:38:04.878507"),
// pero esa hora está en UTC porque la JVM corre en UTC. Si no se lo indicamos,
// el navegador la interpreta como hora local y el cálculo queda desfasado.
function parsearComoUTC(fechaString) {
  const tieneZonaExplicita = /Z$|[+-]\d{2}:\d{2}$/.test(fechaString);
  return new Date(tieneZonaExplicita ? fechaString : `${fechaString}Z`);
}
export function useCountdown(fechaFin) {
  const calcularTiempoRestante = () => {
    const ahora = new Date().getTime();
    const fin = parsearComoUTC(fechaFin).getTime();
    const diferencia = fin - ahora;

    if (diferencia <= 0) {
      return { dias: 0, horas: 0, minutos: 0, segundos: 0, finalizada: true };
    }

    const dias = Math.floor(diferencia / (1000 * 60 * 60 * 24));
    const horas = Math.floor(
      (diferencia % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60),
    );
    const minutos = Math.floor((diferencia % (1000 * 60 * 60)) / (1000 * 60));
    const segundos = Math.floor((diferencia % (1000 * 60)) / 1000);

    return { dias, horas, minutos, segundos, finalizada: false };
  };

  const [tiempoRestante, setTiempoRestante] = useState(
    calcularTiempoRestante(),
  );

  useEffect(() => {
    if (tiempoRestante.finalizada) {
      return;
    }

    const intervalo = setInterval(() => {
      setTiempoRestante(calcularTiempoRestante());
    }, 1000);

    return () => clearInterval(intervalo);
  }, [fechaFin, tiempoRestante.finalizada]);

  return tiempoRestante;
}

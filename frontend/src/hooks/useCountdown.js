import { useState, useEffect } from 'react'

export function useCountdown(fechaFin) {
  const calcularTiempoRestante = () => {
    const ahora = new Date().getTime()
    const fin = new Date(fechaFin).getTime()
    const diferencia = fin - ahora

    if (diferencia <= 0) {
      return { dias: 0, horas: 0, minutos: 0, segundos: 0, finalizada: true }
    }

    const dias = Math.floor(diferencia / (1000 * 60 * 60 * 24))
    const horas = Math.floor((diferencia % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
    const minutos = Math.floor((diferencia % (1000 * 60 * 60)) / (1000 * 60))
    const segundos = Math.floor((diferencia % (1000 * 60)) / 1000)

    return { dias, horas, minutos, segundos, finalizada: false }
  }

  const [tiempoRestante, setTiempoRestante] = useState(calcularTiempoRestante())

  useEffect(() => {
    if (tiempoRestante.finalizada) {
      return
    }

    const intervalo = setInterval(() => {
      setTiempoRestante(calcularTiempoRestante())
    }, 1000)

    return () => clearInterval(intervalo)
  }, [fechaFin, tiempoRestante.finalizada])

  return tiempoRestante
}

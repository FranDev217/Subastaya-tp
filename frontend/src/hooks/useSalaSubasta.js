import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import {
  obtenerDetalleSubasta,
  obtenerHistorialPujas,
} from "../services/salaService";

function construirUrlWebSocket() {
  const protocolo = window.location.protocol === "https:" ? "wss" : "ws";
  return `${protocolo}://${window.location.host}/ws`;
}

function normalizarPuja(puja) {
  if (!puja) return null;
  return {
    id: puja.id,
    compradorId: puja.compradorId,
    alias: puja.compradorAlias,
    monto: puja.monto,
    fechaPuja: puja.fechaPuja,
  };
}

function parsearEvento(mensaje) {
  try {
    return JSON.parse(mensaje.body);
  } catch {
    return null;
  }
}

export function useSalaSubasta(subastaId, { usuarioId, onEvento } = {}) {
  const [detalle, setDetalle] = useState(null);
  const [pujas, setPujas] = useState([]);
  const [ofertaActual, setOfertaActual] = useState(0);
  const [incrementoMinimo, setIncrementoMinimo] = useState(0);
  const [liderId, setLiderId] = useState(null);
  const [fechaFin, setFechaFin] = useState(null);
  const [estado, setEstado] = useState(null);
  const [errorCarga, setErrorCarga] = useState(null);
  const [conectado, setConectado] = useState(false);

  const usuarioIdRef = useRef(usuarioId);
  const liderIdRef = useRef(null);
  const onEventoRef = useRef(onEvento);

  useEffect(() => {
    usuarioIdRef.current = usuarioId;
  }, [usuarioId]);

  useEffect(() => {
    onEventoRef.current = onEvento;
  }, [onEvento]);

  useEffect(() => {
    let activo = true;

    Promise.all([
      obtenerDetalleSubasta(subastaId),
      obtenerHistorialPujas(subastaId),
    ])
      .then(([detalleData, historialData]) => {
        if (!activo) return;
        setErrorCarga(null);
        setDetalle(detalleData);
        setPujas(historialData);
        setOfertaActual(detalleData.ofertaActual);
        setIncrementoMinimo(detalleData.incrementoMinimo);
        setFechaFin(detalleData.fechaFin);
        setEstado(detalleData.estado);
        liderIdRef.current = detalleData.liderId;
        setLiderId(detalleData.liderId);
      })
      .catch((error) => {
        if (activo) setErrorCarga(error.message);
      });

    return () => {
      activo = false;
    };
  }, [subastaId]);

  useEffect(() => {
    let activo = true;

    const aplicarSnapshot = (evento) => {
      if (evento.montoActual != null) setOfertaActual(evento.montoActual);
      if (evento.incrementoMinimo != null) {
        setIncrementoMinimo(evento.incrementoMinimo);
      }
      if (evento.fechaFin != null) setFechaFin(evento.fechaFin);
      if (evento.estado != null) setEstado(evento.estado);

      const nuevoLider = evento.ultimaPuja?.compradorId ?? null;
      liderIdRef.current = nuevoLider;
      setLiderId(nuevoLider);

      if (evento.ultimaPuja) {
        const puja = normalizarPuja(evento.ultimaPuja);
        setPujas((previas) =>
          previas.some((p) => p.id === puja.id) ? previas : [puja, ...previas],
        );
      }
    };

    const aplicarEvento = (evento) => {
      if (evento.tipo === "NUEVA_PUJA") {
        const puja = normalizarPuja(evento.ultimaPuja);
        const nuevoLider = puja?.compradorId ?? null;
        const usuarioActual = usuarioIdRef.current;
        const eraLider =
          usuarioActual != null && liderIdRef.current === usuarioActual;

        if (puja) {
          setOfertaActual(puja.monto);
          setPujas((previas) =>
            previas.some((p) => p.id === puja.id) ? previas : [puja, ...previas],
          );
        } else if (evento.montoActual != null) {
          setOfertaActual(evento.montoActual);
        }

        if (evento.incrementoMinimo != null) {
          setIncrementoMinimo(evento.incrementoMinimo);
        }
        if (evento.fechaFin != null) setFechaFin(evento.fechaFin);

        liderIdRef.current = nuevoLider;
        setLiderId(nuevoLider);

        onEventoRef.current?.({
          tipo: "NUEVA_PUJA",
          evento,
          puja,
          superado: eraLider && nuevoLider !== usuarioActual,
          extendidoPorAntiSniping: Boolean(puja?.extendidoPorAntiSniping),
        });
        return;
      }

      if (evento.tipo === "ESTADO_CAMBIADO") {
        aplicarSnapshot(evento);
        onEventoRef.current?.({ tipo: "ESTADO_CAMBIADO", evento });
        return;
      }

      if (evento.tipo === "FINALIZADA" || evento.tipo === "DESIERTA") {
        aplicarSnapshot(evento);
        onEventoRef.current?.({ tipo: evento.tipo, evento });
      }
    };

    const client = new Client({
      brokerURL: construirUrlWebSocket(),
      reconnectDelay: 3000,
      onConnect: () => {
        if (!activo) return;
        setConectado(true);
        client.subscribe(`/app/subastas/${subastaId}`, (mensaje) => {
          const evento = parsearEvento(mensaje);
          if (evento) aplicarSnapshot(evento);
        });
        client.subscribe(`/topic/subastas/${subastaId}`, (mensaje) => {
          const evento = parsearEvento(mensaje);
          if (evento) aplicarEvento(evento);
        });
      },
      onWebSocketClose: () => {
        if (activo) setConectado(false);
      },
      onStompError: (frame) => {
        console.error("Error STOMP en la sala:", frame.headers?.message);
      },
    });

    client.activate();

    return () => {
      activo = false;
      client.deactivate();
    };
  }, [subastaId]);

  const cargando = detalle === null && errorCarga === null;

  return {
    detalle,
    pujas,
    ofertaActual,
    incrementoMinimo,
    liderId,
    fechaFin,
    estado,
    cargando,
    errorCarga,
    conectado,
  };
}

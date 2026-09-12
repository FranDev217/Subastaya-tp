import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useSalaSubasta } from "../hooks/useSalaSubasta";
import { useCountdown } from "../hooks/useCountdown";
import HistorialPujas from "../components/HistorialPujas/HistorialPujas";
import "./SalaSubastaPage.css";

const USUARIO_STORAGE_KEY = "subastaya_usuario";

function usuarioGuardado() {
  try {
    const guardado = localStorage.getItem(USUARIO_STORAGE_KEY);
    return guardado ? JSON.parse(guardado) : null;
  } catch {
    return null;
  }
}

function formatearMonto(monto) {
  return `$${Number(monto ?? 0).toLocaleString("es-AR")}`;
}

function infoEstado(estado) {
  switch (estado) {
    case "ACTIVA":
      return { texto: "En curso", clase: "activa" };
    case "PROGRAMADA":
      return { texto: "Próxima", clase: "programada" };
    case "FINALIZADA":
      return { texto: "Finalizada", clase: "finalizada" };
    case "DESIERTA":
      return { texto: "Desierta", clase: "desierta" };
    default:
      return { texto: estado ?? "", clase: "desconocida" };
  }
}

function TemporizadorVivo({ fechaFin, estado }) {
  const { dias, horas, minutos, segundos, finalizada } =
    useCountdown(fechaFin);
  const cerrada = finalizada || estado === "FINALIZADA" || estado === "DESIERTA";
  const totalSegundos =
    dias * 86400 + horas * 3600 + minutos * 60 + segundos;

  let clase = "temporizador";
  if (cerrada) clase += " temporizador--cerrado";
  else if (totalSegundos <= 10) clase += " temporizador--critico";
  else if (totalSegundos <= 60) clase += " temporizador--urgente";

  const valores =
    dias > 0
      ? [dias, horas, minutos, segundos]
      : [horas, minutos, segundos];
  const etiquetas =
    dias > 0 ? ["días", "hs", "min", "seg"] : ["hs", "min", "seg"];

  return (
    <div className={clase}>
      <span className="temporizador__leyenda">
        {cerrada ? "Subasta cerrada" : "Tiempo restante"}
      </span>

      {cerrada ? (
        <span className="temporizador__cerrado">00:00:00</span>
      ) : (
        <div className="temporizador__digitos">
          {valores.map((valor, indice) => (
            <div key={etiquetas[indice]} className="temporizador__unidad">
              <span className="temporizador__valor">
                {String(valor).padStart(2, "0")}
              </span>
              <span className="temporizador__unidad-label">
                {etiquetas[indice]}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function SalaSubastaPage() {
  const { id } = useParams();
  const [usuario] = useState(usuarioGuardado);

  const {
    detalle,
    pujas,
    ofertaActual,
    incrementoMinimo,
    fechaFin,
    estado,
    cargando,
    errorCarga,
    conectado,
  } = useSalaSubasta(id, { usuarioId: usuario?.usuarioId });

  if (cargando) {
    return (
      <div className="sala sala--centrada">
        <div className="sala__spinner"></div>
        <p>Cargando sala de subasta...</p>
      </div>
    );
  }

  if (errorCarga) {
    return (
      <div className="sala sala--centrada">
        <p className="sala__error">No se pudo cargar la subasta: {errorCarga}</p>
        <Link to="/" className="sala__volver">
          Volver al catálogo
        </Link>
      </div>
    );
  }

  const info = infoEstado(estado);

  return (
    <div className="sala">
      <header className="sala__barra">
        <Link to="/" className="sala__volver">
          ← Volver al catálogo
        </Link>
        <span
          className={`sala__conexion ${
            conectado ? "sala__conexion--viva" : "sala__conexion--caida"
          }`}
        >
          <span className="sala__conexion-punto"></span>
          {conectado ? "En vivo" : "Reconectando..."}
        </span>
      </header>

      <div className="sala__layout">
        <div className="sala__columna">
          <main className="sala__principal">
            <div className="sala__imagen-container">
              <img
                src={detalle.urlImagen || "/placeholder.jpg"}
                alt={detalle.titulo}
                className="sala__imagen"
              />
              <span className={`sala__badge sala__badge--${info.clase}`}>
                {info.texto}
              </span>
            </div>

            <div className="sala__info">
              <span className="sala__categoria">{detalle.categoriaNombre}</span>
              <h1 className="sala__titulo">{detalle.titulo}</h1>
              <p className="sala__descripcion">{detalle.descripcion}</p>
            </div>
          </main>

          <HistorialPujas pujas={pujas} />
        </div>

        <aside className="sala__panel">
          <TemporizadorVivo fechaFin={fechaFin} estado={estado} />

          <div className="sala__oferta">
            <span className="sala__oferta-label">Oferta actual</span>
            <span className="sala__oferta-monto">
              {formatearMonto(ofertaActual)}
            </span>
            <span className="sala__oferta-incremento">
              Incremento mínimo: {formatearMonto(incrementoMinimo)}
            </span>
          </div>
        </aside>
      </div>
    </div>
  );
}

export default SalaSubastaPage;

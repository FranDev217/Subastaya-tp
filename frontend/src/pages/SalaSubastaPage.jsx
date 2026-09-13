import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useSalaSubasta } from "../hooks/useSalaSubasta";
import { useCountdown } from "../hooks/useCountdown";
import { useToasts } from "../context/toastsContext";
import HistorialPujas from "../components/HistorialPujas/HistorialPujas";
import ConsolaPuja from "../components/ConsolaPuja/ConsolaPuja";
import { registrarPuja } from "../services/salaService";
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

function toastDeErrorPuja(error) {
  if (error.status === 422 && /saldo|insuficiente/i.test(error.message)) {
    return {
      tipo: "error",
      mensaje: `Fondos insuficientes para esa oferta. ${error.message}`,
    };
  }
  if (error.status === 422) {
    return { tipo: "warning", mensaje: error.message };
  }
  if (error.status === 409) {
    return {
      tipo: "warning",
      mensaje: "Otro postor se adelantó. Actualizá el monto e intentá de nuevo.",
    };
  }
  if (error.status === 400) {
    return { tipo: "warning", mensaje: error.message };
  }
  return { tipo: "error", mensaje: error.message };
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
  const [enviandoPuja, setEnviandoPuja] = useState(false);
  const { mostrar } = useToasts();

  function manejarEvento(evento) {
    if (evento.tipo === "NUEVA_PUJA") {
      const esMia =
        Boolean(usuario) && evento.puja?.compradorId === usuario.usuarioId;
      if (esMia) return;
      if (evento.extendidoPorAntiSniping) {
        mostrar({
          tipo: "warning",
          mensaje: "Se extendió el tiempo 2 minutos por anti-sniping.",
          duracion: 6000,
        });
      }
      if (evento.superado) {
        mostrar({
          tipo: "error",
          mensaje: "Te superaron: otro postor lidera ahora.",
        });
      }
      return;
    }
    if (evento.tipo === "ESTADO_CAMBIADO") {
      mostrar({ tipo: "info", mensaje: "La subasta se abrió: ¡ya podés pujar!" });
      return;
    }
    if (evento.tipo === "FINALIZADA") {
      mostrar({ tipo: "info", mensaje: "La subasta finalizó." });
      return;
    }
    if (evento.tipo === "DESIERTA") {
      mostrar({
        tipo: "info",
        mensaje: "La subasta quedó desierta: no recibió pujas.",
      });
    }
  }

  const {
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
    aplicarPujaRespuesta,
  } = useSalaSubasta(id, {
    usuarioId: usuario?.usuarioId,
    onEvento: manejarEvento,
  });

  async function manejarPujar(monto) {
    if (!usuario) return false;
    setEnviandoPuja(true);
    try {
      const respuesta = await registrarPuja(id, {
        compradorId: usuario.usuarioId,
        monto,
      });
      aplicarPujaRespuesta(respuesta);
      mostrar({
        tipo: "success",
        mensaje: `Puja registrada por ${formatearMonto(respuesta.monto)}.`,
      });
      if (respuesta.extendidoPorAntiSniping) {
        mostrar({
          tipo: "warning",
          mensaje: "Tu puja extendió el tiempo 2 minutos (anti-sniping).",
          duracion: 6000,
        });
      }
      return true;
    } catch (error) {
      mostrar(toastDeErrorPuja(error));
      return false;
    } finally {
      setEnviandoPuja(false);
    }
  }

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

          <ConsolaPuja
            ofertaActual={ofertaActual}
            incrementoMinimo={incrementoMinimo}
            estado={estado}
            liderId={liderId}
            usuario={usuario}
            enviando={enviandoPuja}
            onPujar={manejarPujar}
          />
        </aside>
      </div>
    </div>
  );
}

export default SalaSubastaPage;

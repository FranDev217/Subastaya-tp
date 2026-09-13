import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import "./ConsolaPuja.css";

function formatearMonto(monto) {
  return `$${Number(monto ?? 0).toLocaleString("es-AR")}`;
}

function FormularioPuja({ sugerido, incremento, enviando, onPujar }) {
  const [monto, setMonto] = useState(String(sugerido));
  const montoNumero = Number(monto);
  const valido =
    monto !== "" && !Number.isNaN(montoNumero) && montoNumero >= sugerido;

  async function manejarEnvio(event) {
    event.preventDefault();
    if (!valido || enviando) return;
    await onPujar(montoNumero);
  }

  return (
    <form className="consola__form" onSubmit={manejarEnvio}>
      <label className="consola__campo">
        <span className="consola__campo-label">
          Tu oferta (mínimo {formatearMonto(sugerido)})
        </span>
        <input
          className="consola__input"
          type="number"
          min={sugerido}
          step={incremento > 0 ? incremento : 1}
          value={monto}
          onChange={(event) => setMonto(event.target.value)}
          disabled={enviando}
        />
      </label>

      {!valido && monto !== "" && (
        <p className="consola__validacion">
          El monto debe ser al menos {formatearMonto(sugerido)}.
        </p>
      )}

      <div className="consola__acciones">
        <button
          type="button"
          className="consola__btn-secundario"
          onClick={() => setMonto(String(sugerido))}
          disabled={enviando}
        >
          Usar sugerido
        </button>
        <button
          type="submit"
          className="consola__btn"
          disabled={!valido || enviando}
        >
          {enviando ? "Enviando..." : "Ofertar"}
        </button>
      </div>
    </form>
  );
}

function ConsolaPuja({
  ofertaActual,
  incrementoMinimo,
  estado,
  liderId,
  usuario,
  errorPuja,
  enviando,
  onPujar,
}) {
  const location = useLocation();
  const sugerido = Number(ofertaActual) + Number(incrementoMinimo);
  const activa = estado === "ACTIVA";
  const hayPujas = liderId != null;
  const esLider = Boolean(usuario) && liderId === usuario.usuarioId;

  let estadoJugador = { clase: "anonimo", texto: "Iniciá sesión para pujar" };
  if (usuario && !hayPujas) {
    estadoJugador = { clase: "neutro", texto: "Sin pujas todavía" };
  } else if (esLider) {
    estadoJugador = { clase: "lider", texto: "Estás liderando" };
  } else if (usuario) {
    estadoJugador = { clase: "superado", texto: "Superado" };
  }

  return (
    <section className="consola">
      <header className="consola__header">
        <h2 className="consola__titulo">Consola de puja</h2>
        <span className={`consola__estado consola__estado--${estadoJugador.clase}`}>
          <span className="consola__estado-punto"></span>
          {estadoJugador.texto}
        </span>
      </header>

      <p className="consola__sugerido">
        Próxima puja sugerida: <strong>{formatearMonto(sugerido)}</strong>
      </p>

      {!usuario ? (
        <p className="consola__aviso">
          Para ofertar necesitás{" "}
          <Link
            to="/login"
            state={{ from: location.pathname }}
            className="consola__link"
          >
            iniciar sesión
          </Link>
          .
        </p>
      ) : !activa ? (
        <p className="consola__aviso">
          La subasta no está activa: no se pueden registrar pujas.
        </p>
      ) : (
        <FormularioPuja
          key={sugerido}
          sugerido={sugerido}
          incremento={incrementoMinimo}
          enviando={enviando}
          onPujar={onPujar}
        />
      )}

      {errorPuja && (
        <p className="consola__error" role="alert">
          {errorPuja}
        </p>
      )}
    </section>
  );
}

export default ConsolaPuja;

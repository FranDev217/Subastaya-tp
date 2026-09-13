import "./HistorialPujas.css";

const LIMITE_VISIBLE = 20;

function formatearHora(fecha) {
  if (!fecha) return "";
  const tieneZonaExplicita = /Z$|[+-]\d{2}:\d{2}$/.test(fecha);
  const date = new Date(tieneZonaExplicita ? fecha : `${fecha}Z`);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleTimeString("es-AR", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

function formatearMonto(monto) {
  return `$${Number(monto ?? 0).toLocaleString("es-AR")}`;
}

function HistorialPujas({ pujas }) {
  const visibles = pujas.slice(0, LIMITE_VISIBLE);

  return (
    <section className="historial">
      <header className="historial__header">
        <h2 className="historial__titulo">Historial de ofertas</h2>
        <span className="historial__contador">
          {pujas.length} {pujas.length === 1 ? "puja" : "pujas"}
        </span>
      </header>

      {visibles.length === 0 ? (
        <p className="historial__vacio">
          Aún no hay pujas. ¡Sé el primero en ofertar!
        </p>
      ) : (
        <ul className="historial__lista">
          {visibles.map((puja) => (
            <li key={puja.id} className="historial__item">
              <span className="historial__alias">{puja.alias}</span>
              <span className="historial__monto">
                {formatearMonto(puja.monto)}
              </span>
              <span className="historial__hora">
                {formatearHora(puja.fechaPuja)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export default HistorialPujas;

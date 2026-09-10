import { useCountdown } from "../../hooks/useCountdown";
import "./SubastaCard.css";

function SubastaCard({ subasta }) {
  const { dias, horas, minutos, segundos, finalizada } = useCountdown(
    subasta.fechaFin,
  );

  const formatearTiempo = () => {
    if (finalizada) return "Finalizada";

    if (dias > 0) return `${dias}d ${horas}h`;
    if (horas > 0) return `${horas}h ${minutos}m`;
    if (minutos > 0) return `${minutos}m ${segundos}s`;
    return `${segundos}s`;
  };

  const obtenerClaseEstado = () => {
    if (finalizada || subasta.estado === "FINALIZADA") return "estado-finalizada";
    if (subasta.estado === "PROGRAMADA") return "estado-programada";
    if (subasta.estado === "DESIERTA") return "estado-desierta";
    return "estado-activa";
  };

  const obtenerTextoEstado = () => {
    if (subasta.estado === "DESIERTA") return "Desierta";
    if (finalizada || subasta.estado === "FINALIZADA") return "Finalizada";
    if (subasta.estado === "PROGRAMADA") return "Próxima";
    return "En curso";
  };
  return (
    <article className={`subasta-card ${obtenerClaseEstado()}`}>
      <div className="subasta-card__imagen-container">
        <img
          src={subasta.urlImagen || "/placeholder.jpg"}
          alt={subasta.titulo}
          className="subasta-card__imagen"
        />
        <span className="subasta-card__badge">{obtenerTextoEstado()}</span>
      </div>

      <div className="subasta-card__contenido">
        <h3 className="subasta-card__titulo">{subasta.titulo}</h3>

        <span className="subasta-card__categoria">
          {subasta.categoriaNombre}
        </span>

        <div className="subasta-card__oferta">
          <span className="subasta-card__oferta-label">Oferta actual</span>
          <span className="subasta-card__oferta-monto">
            ${subasta.ofertaActual.toLocaleString("es-AR")}
          </span>
        </div>

        <div className="subasta-card__info">
          <span className="subasta-card__pujas">
            {subasta.cantidadPujas}{" "}
            {subasta.cantidadPujas === 1 ? "puja" : "pujas"}
          </span>

          <span className="subasta-card__tiempo">{formatearTiempo()}</span>
        </div>
      </div>
    </article>
  );
}

export default SubastaCard;

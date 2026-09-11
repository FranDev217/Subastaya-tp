import { useState, useEffect } from "react";
import { obtenerSubastas } from "../services/subastasService";
import SubastaCard from "../components/SubastaCard/SubastaCard";
import FiltrosPanel from "../components/FiltrosPanel/FiltrosPanel";
import OrdenSelector from "../components/OrdenSelector/OrdenSelector";
import "./CatalogoPage.css";

function CatalogoPage() {
  const [subastas, setSubastas] = useState([]);
  const [filtros, setFiltros] = useState({
    estado: null,
    categoriaId: null,
    precioMin: "",
    precioMax: "",
  });
  const [sort, setSort] = useState("menorTiempo");
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchSubastas = async () => {
      setCargando(true);
      setError(null);
      try {
        const data = await obtenerSubastas({ ...filtros, sort });
        setSubastas(data);
      } catch (err) {
        setError(err.message);
        setSubastas([]);
      } finally {
        setCargando(false);
      }
    };

    fetchSubastas();
  }, [filtros, sort]);

  return (
    <div className="catalogo-page">
      <header className="catalogo-page__header">
        <h1 className="catalogo-page__titulo">SubastasYa</h1>
        <OrdenSelector valor={sort} onChange={setSort} />
      </header>

      <div className="catalogo-page__layout">
        <aside className="catalogo-page__sidebar">
          <FiltrosPanel filtros={filtros} onFiltrosChange={setFiltros} />
        </aside>

        <main className="catalogo-page__contenido">
          {cargando && (
            <div className="catalogo-page__estado">
              <div className="catalogo-page__spinner"></div>
              <p>Cargando subastas...</p>
            </div>
          )}

          {error && (
            <div className="catalogo-page__estado catalogo-page__estado--error">
              <p>Error: {error}</p>
              <button onClick={() => setFiltros({ ...filtros })}>
                Reintentar
              </button>
            </div>
          )}

          {!cargando && !error && subastas.length === 0 && (
            <div className="catalogo-page__estado catalogo-page__estado--vacio">
              <p>No se encontraron subastas</p>
              <p className="catalogo-page__estado-subtitulo">
                Probá ajustar los filtros para ver más resultados
              </p>
            </div>
          )}

          {!cargando && !error && subastas.length > 0 && (
            <div className="catalogo-page__grid">
              {subastas.map((subasta) => (
                <SubastaCard key={subasta.id} subasta={subasta} />
              ))}
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

export default CatalogoPage;

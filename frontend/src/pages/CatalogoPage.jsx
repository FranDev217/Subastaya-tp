import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { obtenerSubastas } from "../services/subastasService";
import SubastaCard from "../components/SubastaCard/SubastaCard";
import CarruselSubastas from "../components/CarruselSubastas/CarruselSubastas";
import FiltrosPanel from "../components/FiltrosPanel/FiltrosPanel";
import OrdenSelector from "../components/OrdenSelector/OrdenSelector";
import "./CatalogoPage.css";

const USUARIO_STORAGE_KEY = "subastaya_usuario";

function usuarioGuardado() {
  try {
    const guardado = localStorage.getItem(USUARIO_STORAGE_KEY);
    return guardado ? JSON.parse(guardado) : null;
  } catch {
    return null;
  }
}

function CatalogoPage() {
  const usuario = usuarioGuardado();
  const [subastas, setSubastas] = useState([]);
  const [subastasActivas, setSubastasActivas] = useState([]);
  const [subastasProgramadas, setSubastasProgramadas] = useState([]);
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

  useEffect(() => {
    let vigente = true;

    Promise.all([
      obtenerSubastas({ estado: "ACTIVA" }),
      obtenerSubastas({ estado: "PROGRAMADA" }),
    ])
      .then(([activas, programadas]) => {
        if (!vigente) return;
        setSubastasActivas(activas);
        setSubastasProgramadas(programadas);
      })
      .catch(() => {
        if (!vigente) return;
        setSubastasActivas([]);
        setSubastasProgramadas([]);
      });

    return () => {
      vigente = false;
    };
  }, []);

  return (
    <div className="catalogo-page">
      <header className="catalogo-page__header">
        <div className="catalogo-page__barra">
          <h1 className="catalogo-page__titulo">SubastasYa</h1>
          <nav className="catalogo-page__nav">
            <Link to="/publicar" className="catalogo-page__nav-link catalogo-page__nav-link--primary">
              Publicar subasta
            </Link>
            {usuario && (
              <Link to="/billetera" className="catalogo-page__nav-link">
                Mi billetera
              </Link>
            )}
            {usuario && (
              <Link to="/mis-actividades" className="catalogo-page__nav-link">
                Mis actividades
              </Link>
            )}
            <Link to="/login" className="catalogo-page__nav-link">
              {usuario ? `Hola, ${usuario.nombre}` : 'Iniciar sesión'}
            </Link>
          </nav>
        </div>
        <OrdenSelector valor={sort} onChange={setSort} />
      </header>

      <div className="catalogo-page__carruseles">
        <CarruselSubastas
          titulo="Subastas activas"
          subastas={subastasActivas}
          direccion="derecha"
        />
        <CarruselSubastas
          titulo="Próximamente"
          subastas={subastasProgramadas}
          direccion="izquierda"
        />
      </div>

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
                <Link
                  key={subasta.id}
                  to={`/subasta/${subasta.id}`}
                  className="catalogo-page__card-link"
                >
                  <SubastaCard subasta={subasta} />
                </Link>
              ))}
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

export default CatalogoPage;

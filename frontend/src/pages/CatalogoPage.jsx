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

const LIMITE_TERMINAN_PRONTO = 8;

function CatalogoPage() {
  const usuario = usuarioGuardado();
  const [subastas, setSubastas] = useState([]);
  const [terminanPronto, setTerminanPronto] = useState([]);
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

    obtenerSubastas({ estado: "ACTIVA", sort: "menorTiempo" })
      .then((activas) => {
        if (!vigente) return;
        setTerminanPronto(activas.slice(0, LIMITE_TERMINAN_PRONTO));
      })
      .catch(() => {
        if (!vigente) return;
        setTerminanPronto([]);
      });

    return () => {
      vigente = false;
    };
  }, []);

  return (
    <div className="catalogo-page">
      <header className="catalogo-page__header">
        <div className="catalogo-page__barra">
          <div className="catalogo-page__marca">
            <h1 className="catalogo-page__titulo">SubastasYa</h1>
            {terminanPronto.length > 0 && (
              <span className="catalogo-page__pulso">
                <span className="catalogo-page__pulso-punto" aria-hidden="true" />
                {terminanPronto.length}{" "}
                {terminanPronto.length === 1
                  ? "subasta activa ahora"
                  : "subastas activas ahora"}
              </span>
            )}
          </div>
          <nav className="catalogo-page__nav">
            <Link to="/publicar" className="catalogo-page__nav-link">
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
              {usuario ? `Hola, ${usuario.nombre}` : "Iniciar sesión"}
            </Link>
          </nav>
        </div>
      </header>

      <div className="catalogo-page__video-wrapper">
        <video className="catalogo-page__video" autoPlay muted loop playsInline>
          <source src="/videos/subastas.mp4" type="video/mp4" />
        </video>
        <div className="catalogo-page__video-overlay">
          <p className="catalogo-page__video-frase">
            Pujá en tiempo real. Ganá al mejor precio.
          </p>
        </div>
      </div>

      <div className="catalogo-page__carruseles">
        <CarruselSubastas
          titulo="Terminan pronto"
          subastas={terminanPronto}
          direccion="izquierda"
        />
      </div>
      <hr className="catalogo-page__separador" />
      <div className="catalogo-page__layout">
        <aside className="catalogo-page__sidebar">
          <OrdenSelector valor={sort} onChange={setSort} />
          <hr className="catalogo-page__separador" />
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

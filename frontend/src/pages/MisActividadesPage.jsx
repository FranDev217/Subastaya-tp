import { useEffect, useState } from 'react'
import { Navigate, Link } from 'react-router-dom'
import { obtenerMisCompras, obtenerMisPublicaciones } from '../services/actividadService'
import './MisActividadesPage.css'

const USUARIO_STORAGE_KEY = 'subastaya_usuario'

function usuarioGuardado() {
  try {
    const guardado = localStorage.getItem(USUARIO_STORAGE_KEY)
    return guardado ? JSON.parse(guardado) : null
  } catch {
    return null
  }
}

function formatearMonto(monto) {
  return `$${Number(monto ?? 0).toLocaleString('es-AR')}`
}

function estadoCompra(compra) {
  if (compra.estado === 'FINALIZADA') {
    return compra.soyElLider
      ? { texto: 'Ganaste', clase: 'gano' }
      : { texto: 'Perdiste', clase: 'perdio' }
  }
  return compra.soyElLider
    ? { texto: 'Liderando', clase: 'lidera' }
    : { texto: 'Superado', clase: 'superado' }
}

function estadoPublicacion(estado) {
  switch (estado) {
    case 'ACTIVA':
      return { texto: 'En curso', clase: 'activa' }
    case 'PROGRAMADA':
      return { texto: 'Próxima', clase: 'programada' }
    case 'FINALIZADA':
      return { texto: 'Finalizada', clase: 'finalizada' }
    case 'DESIERTA':
      return { texto: 'Desierta', clase: 'desierta' }
    default:
      return { texto: estado, clase: 'activa' }
  }
}

function MisActividadesPage() {
  const usuario = usuarioGuardado()
  const [tab, setTab] = useState('compras')
  const [items, setItems] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    if (!usuario) return
    let cancelado = false

    async function cargar() {
      setCargando(true)
      setError(null)
      try {
        const data =
          tab === 'compras'
            ? await obtenerMisCompras(usuario.usuarioId)
            : await obtenerMisPublicaciones(usuario.usuarioId)
        if (!cancelado) setItems(data)
      } catch (err) {
        if (!cancelado) setError(err.message)
      } finally {
        if (!cancelado) setCargando(false)
      }
    }

    cargar()
    return () => {
      cancelado = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [usuario?.usuarioId, tab, version])

  if (!usuario) {
    return <Navigate to="/login" replace state={{ from: '/mis-actividades' }} />
  }

  return (
    <div className="actividades-page">
      <header className="actividades-page__header">
        <div>
          <h1 className="actividades-page__titulo">Mis actividades</h1>
          <p className="actividades-page__subtitulo">Hola, {usuario.nombre}</p>
        </div>
        <Link className="actividades-page__volver" to="/">
          ← Volver al catálogo
        </Link>
      </header>

      <div className="actividades-page__tabs" role="tablist">
        <button
          type="button"
          role="tab"
          aria-selected={tab === 'compras'}
          className={`actividades-page__tab ${tab === 'compras' ? 'actividades-page__tab--activo' : ''}`}
          onClick={() => setTab('compras')}
        >
          Mis compras / pujas
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={tab === 'publicaciones'}
          className={`actividades-page__tab ${tab === 'publicaciones' ? 'actividades-page__tab--activo' : ''}`}
          onClick={() => setTab('publicaciones')}
        >
          Mis publicaciones
        </button>
      </div>

      {cargando && <p className="actividades-page__estado">Cargando…</p>}

      {error && !cargando && (
        <div className="actividades-page__estado actividades-page__estado--error" role="alert">
          <p>Error: {error}</p>
          <button type="button" onClick={() => setVersion((anterior) => anterior + 1)}>
            Reintentar
          </button>
        </div>
      )}

      {!cargando && !error && items.length === 0 && (
        <p className="actividades-page__estado">
          {tab === 'compras'
            ? 'Todavía no pujaste en ninguna subasta.'
            : 'Todavía no publicaste ninguna subasta.'}
        </p>
      )}

      {!cargando && !error && items.length > 0 && tab === 'compras' && (
        <div className="actividades-grid">
          {items.map((compra) => {
            const { texto, clase } = estadoCompra(compra)
            return (
              <Link
                key={compra.subastaId}
                to={`/subasta/${compra.subastaId}`}
                className={`actividad-card actividad-card--${clase}`}
              >
                <img
                  src={compra.urlImagen || '/placeholder.jpg'}
                  alt={compra.titulo}
                  className="actividad-card__imagen"
                />
                <div className="actividad-card__contenido">
                  <span className={`actividad-card__badge actividad-card__badge--${clase}`}>
                    {texto}
                  </span>
                  <h3 className="actividad-card__titulo">{compra.titulo}</h3>
                  <div className="actividad-card__datos">
                    <span>
                      Tu mejor puja: <strong>{formatearMonto(compra.miMejorPuja)}</strong>
                    </span>
                    <span>
                      Oferta actual: <strong>{formatearMonto(compra.ofertaActual)}</strong>
                    </span>
                  </div>
                </div>
              </Link>
            )
          })}
        </div>
      )}

      {!cargando && !error && items.length > 0 && tab === 'publicaciones' && (
        <div className="actividades-grid">
          {items.map((publicacion) => {
            const { texto, clase } = estadoPublicacion(publicacion.estado)
            return (
              <Link
                key={publicacion.id}
                to={`/subasta/${publicacion.id}`}
                className={`actividad-card actividad-card--${clase}`}
              >
                <img
                  src={publicacion.urlImagen || '/placeholder.jpg'}
                  alt={publicacion.titulo}
                  className="actividad-card__imagen"
                />
                <div className="actividad-card__contenido">
                  <span className={`actividad-card__badge actividad-card__badge--${clase}`}>
                    {texto}
                  </span>
                  <h3 className="actividad-card__titulo">{publicacion.titulo}</h3>
                  <div className="actividad-card__datos">
                    <span>
                      {publicacion.cantidadPujas} {publicacion.cantidadPujas === 1 ? 'puja' : 'pujas'} ·{' '}
                      {formatearMonto(publicacion.ofertaActual)}
                    </span>
                    {publicacion.recaudacion != null && (
                      <span>
                        Recaudación: <strong>{formatearMonto(publicacion.recaudacion)}</strong>
                      </span>
                    )}
                  </div>
                </div>
              </Link>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default MisActividadesPage

import { useEffect, useState } from 'react'
import { Navigate, Link } from 'react-router-dom'
import { obtenerSaldo, obtenerMovimientos, depositar } from '../services/billeteraService'
import { useToasts } from '../context/toastsContext'
import './BilleteraPage.css'

const USUARIO_STORAGE_KEY = 'subastaya_usuario'

const ETIQUETA_TIPO = {
  DEPOSITO: 'Depósito',
  RETENCION: 'Retención (puja)',
  LIBERACION: 'Liberación (superado)',
  PAGO: 'Pago (subasta ganada)',
  COBRO: 'Cobro (subasta vendida)',
}

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

function formatearFecha(fecha) {
  if (!fecha) return ''
  const tieneZonaExplicita = /Z$|[+-]\d{2}:\d{2}$/.test(fecha)
  const date = new Date(tieneZonaExplicita ? fecha : `${fecha}Z`)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('es-AR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function BilleteraPage() {
  const usuario = usuarioGuardado()
  const { mostrar } = useToasts()
  const [saldo, setSaldo] = useState(null)
  const [movimientos, setMovimientos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [monto, setMonto] = useState('')
  const [depositando, setDepositando] = useState(false)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    if (!usuario) return
    let cancelado = false

    async function cargar() {
      setCargando(true)
      setError(null)
      try {
        const [saldoData, movimientosData] = await Promise.all([
          obtenerSaldo(usuario.usuarioId),
          obtenerMovimientos(usuario.usuarioId),
        ])
        if (!cancelado) {
          setSaldo(saldoData)
          setMovimientos(movimientosData)
        }
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
    // usuario es un objeto nuevo en cada render (viene de JSON.parse en usuarioGuardado);
    // dependemos del id, que sí es estable, para no re-disparar el efecto en loop.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [usuario?.usuarioId, version])

  if (!usuario) {
    return <Navigate to="/login" replace state={{ from: '/billetera' }} />
  }

  async function handleDepositar(event) {
    event.preventDefault()
    const montoNumerico = Number(monto)
    if (!monto || montoNumerico <= 0) {
      mostrar({ tipo: 'error', mensaje: 'Ingresá un monto mayor a 0' })
      return
    }

    setDepositando(true)
    try {
      await depositar(usuario.usuarioId, montoNumerico)
      mostrar({
        tipo: 'success',
        mensaje: `Se acreditaron ${formatearMonto(montoNumerico)} a tu billetera.`,
      })
      setMonto('')
      setVersion((anterior) => anterior + 1)
    } catch (err) {
      mostrar({ tipo: 'error', mensaje: err.message })
    } finally {
      setDepositando(false)
    }
  }

  return (
    <div className="billetera-page">
      <div className="billetera-page__content">
        <header className="billetera-page__header">
          <div>
            <h1 className="billetera-page__titulo">Mi billetera</h1>
            <p className="billetera-page__subtitulo">Hola, {usuario.nombre}</p>
          </div>
          <Link className="billetera-page__volver" to="/">
            ← Volver al catálogo
          </Link>
        </header>

        {cargando && <p className="billetera-page__estado">Cargando billetera…</p>}

        {error && !cargando && (
          <div className="billetera-page__estado billetera-page__estado--error" role="alert">
            <p>Error: {error}</p>
            <button type="button" onClick={() => setVersion((anterior) => anterior + 1)}>
              Reintentar
            </button>
          </div>
        )}

        {!cargando && !error && saldo && (
          <>
            <section className="billetera-saldo">
              <div className="billetera-saldo__card">
                <span className="billetera-saldo__label">Saldo total</span>
                <span className="billetera-saldo__monto">{formatearMonto(saldo.saldoTotal)}</span>
              </div>
              <div className="billetera-saldo__card billetera-saldo__card--retenido">
                <span className="billetera-saldo__label">Retenido / en garantía</span>
                <span className="billetera-saldo__monto">{formatearMonto(saldo.saldoRetenido)}</span>
              </div>
              <div className="billetera-saldo__card billetera-saldo__card--disponible">
                <span className="billetera-saldo__label">Disponible</span>
                <span className="billetera-saldo__monto">{formatearMonto(saldo.saldoDisponible)}</span>
              </div>
            </section>

            <section className="billetera-deposito">
              <h2 className="billetera-deposito__titulo">Cargar saldo (simulado)</h2>
              <form className="billetera-deposito__form" onSubmit={handleDepositar}>
                <label className="billetera-deposito__campo">
                  <span>Monto a acreditar</span>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    inputMode="decimal"
                    placeholder="10000"
                    value={monto}
                    onChange={(event) => setMonto(event.target.value)}
                    disabled={depositando}
                  />
                </label>
                <button type="submit" className="billetera-deposito__enviar" disabled={depositando}>
                  {depositando ? 'Acreditando…' : 'Cargar saldo'}
                </button>
              </form>
            </section>

            <section className="billetera-historial">
              <h2 className="billetera-historial__titulo">Historial de movimientos</h2>
              {movimientos.length === 0 ? (
                <p className="billetera-historial__vacio">Todavía no tenés movimientos.</p>
              ) : (
                <ul className="billetera-historial__lista">
                  {movimientos.map((movimiento) => (
                    <li key={movimiento.id} className="billetera-historial__item">
                      <span
                        className={`billetera-historial__tipo billetera-historial__tipo--${movimiento.tipo.toLowerCase()}`}
                      >
                        {ETIQUETA_TIPO[movimiento.tipo] ?? movimiento.tipo}
                      </span>
                      <span className="billetera-historial__detalle">
                        {movimiento.subastaTitulo ?? '—'}
                      </span>
                      <span className="billetera-historial__monto">{formatearMonto(movimiento.monto)}</span>
                      <span className="billetera-historial__fecha">{formatearFecha(movimiento.fecha)}</span>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </>
        )}
      </div>
    </div>
  )
}

export default BilleteraPage

import { useState, useEffect } from 'react'
import { obtenerCategorias } from '../../services/subastasService'
import './FiltrosPanel.css'

function FiltrosPanel({ filtros, onFiltrosChange }) {
  const [categorias, setCategorias] = useState([])
  const [cargandoCategorias, setCargandoCategorias] = useState(true)

  useEffect(() => {
    obtenerCategorias()
      .then(setCategorias)
      .catch(() => setCategorias([]))
      .finally(() => setCargandoCategorias(false))
  }, [])

  const handleEstadoChange = (estado) => {
    onFiltrosChange({ ...filtros, estado })
  }

  const handleCategoriaChange = (e) => {
    onFiltrosChange({ ...filtros, categoriaId: e.target.value || null })
  }

  const handlePrecioMinChange = (e) => {
    onFiltrosChange({ ...filtros, precioMin: e.target.value || null })
  }

  const handlePrecioMaxChange = (e) => {
    onFiltrosChange({ ...filtros, precioMax: e.target.value || null })
  }

  const handleLimpiar = () => {
    onFiltrosChange({ estado: null, categoriaId: null, precioMin: '', precioMax: '' })
  }

  const hayFiltrosActivos = filtros.estado || filtros.categoriaId || filtros.precioMin || filtros.precioMax

  return (
    <div className="filtros-panel">
      <div className="filtros-panel__header">
        <h3 className="filtros-panel__titulo">Filtros</h3>
        {hayFiltrosActivos && (
          <button className="filtros-panel__limpiar" onClick={handleLimpiar}>
            Limpiar filtros
          </button>
        )}
      </div>

      <div className="filtros-panel__grupo">
        <label className="filtros-panel__label">Estado</label>
        <div className="filtros-panel__estados">
          {[
            { valor: null, label: 'Todas' },
            { valor: 'ACTIVA', label: 'Activas' },
            { valor: 'PROGRAMADA', label: 'Próximas' },
            { valor: 'FINALIZADA', label: 'Finalizadas' },
            { valor: 'DESIERTA', label: 'Desiertas' }
          ].map((opcion) => (
            <button
              key={opcion.valor ?? 'todas'}
              className={`filtros-panel__estado-btn ${filtros.estado === opcion.valor ? 'filtros-panel__estado-btn--activo' : ''}`}
              onClick={() => handleEstadoChange(opcion.valor)}
            >
              {opcion.label}
            </button>
          ))}
        </div>
      </div>

      <div className="filtros-panel__grupo">
        <label className="filtros-panel__label" htmlFor="categoria-select">Categoría</label>
        <select
          id="categoria-select"
          className="filtros-panel__select"
          value={filtros.categoriaId ?? ''}
          onChange={handleCategoriaChange}
          disabled={cargandoCategorias}
        >
          <option value="">Todas las categorías</option>
          {categorias.map((cat) => (
            <option key={cat.id} value={cat.id}>{cat.nombre}</option>
          ))}
        </select>
      </div>

      <div className="filtros-panel__grupo">
        <label className="filtros-panel__label">Rango de precios</label>
        <div className="filtros-panel__precios">
          <input
            type="number"
            className="filtros-panel__input-precio"
            placeholder="Mín"
            value={filtros.precioMin ?? ''}
            onChange={handlePrecioMinChange}
            min="0"
          />
          <span className="filtros-panel__separador">—</span>
          <input
            type="number"
            className="filtros-panel__input-precio"
            placeholder="Máx"
            value={filtros.precioMax ?? ''}
            onChange={handlePrecioMaxChange}
            min="0"
          />
        </div>
      </div>
    </div>
  )
}

export default FiltrosPanel

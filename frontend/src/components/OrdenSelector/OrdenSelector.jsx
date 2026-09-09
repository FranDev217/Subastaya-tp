import './OrdenSelector.css'

function OrdenSelector({ valor, onChange }) {
  return (
    <div className="orden-selector">
      <label className="orden-selector__label" htmlFor="orden-select">
        Ordenar por:
      </label>
      <select
        id="orden-select"
        className="orden-selector__select"
        value={valor}
        onChange={(e) => onChange(e.target.value)}
      >
        <option value="menorTiempo">Menor tiempo restante</option>
        <option value="mayorPuja">Mayor puja actual</option>
      </select>
    </div>
  )
}

export default OrdenSelector

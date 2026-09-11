const API_BASE_URL = '/api/v1'

export async function obtenerSubastas({ estado, categoriaId, precioMin, precioMax, sort } = {}) {
  const params = new URLSearchParams()

  if (estado) params.append('estado', estado)
  if (categoriaId) params.append('categoriaId', categoriaId)
  if (precioMin !== null && precioMin !== undefined && precioMin !== '') {
    params.append('precioMin', precioMin)
  }
  if (precioMax !== null && precioMax !== undefined && precioMax !== '') {
    params.append('precioMax', precioMax)
  }
  if (sort) params.append('sort', sort)

  const queryString = params.toString()
  const url = `${API_BASE_URL}/subastas${queryString ? `?${queryString}` : ''}`

  const response = await fetch(url)

  if (!response.ok) {
    throw new Error(`Error al obtener subastas: ${response.status} ${response.statusText}`)
  }

  return response.json()
}

export async function obtenerCategorias() {
  const response = await fetch(`${API_BASE_URL}/subastas/categorias`)

  if (!response.ok) {
    throw new Error(`Error al obtener categorías: ${response.status} ${response.statusText}`)
  }

  return response.json()
}

// El input datetime-local entrega hora local sin zona (ej: "2026-09-11T15:30").
// El backend corre en UTC y deserializa LocalDateTime sin sufijo, así que
// convertimos la hora local a sus componentes UTC y la enviamos sin "Z".
function aUtcSinZona(fechaLocal) {
  if (!fechaLocal) return fechaLocal

  const fecha = new Date(fechaLocal)
  const pad = (valor) => String(valor).padStart(2, '0')

  return (
    `${fecha.getUTCFullYear()}-${pad(fecha.getUTCMonth() + 1)}-${pad(fecha.getUTCDate())}` +
    `T${pad(fecha.getUTCHours())}:${pad(fecha.getUTCMinutes())}:${pad(fecha.getUTCSeconds())}`
  )
}

export async function crearSubasta(datos) {
  const body = {
    ...datos,
    fechaInicio: aUtcSinZona(datos.fechaInicio),
    fechaFin: aUtcSinZona(datos.fechaFin),
  }

  const response = await fetch(`${API_BASE_URL}/subastas`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })

  const data = await response.json().catch(() => null)

  if (!response.ok) {
    const error = new Error(data?.mensaje ?? `Error al crear la subasta: ${response.status}`)
    error.errores = data?.errores ?? null
    throw error
  }

  return data
}

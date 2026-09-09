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

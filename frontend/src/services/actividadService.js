const API_BASE_URL = '/api/v1'

export async function obtenerMisCompras(usuarioId) {
  const response = await fetch(`${API_BASE_URL}/usuarios/${usuarioId}/compras`)

  if (!response.ok) {
    throw new Error(`Error al obtener tus compras: ${response.status} ${response.statusText}`)
  }

  return response.json()
}

export async function obtenerMisPublicaciones(usuarioId) {
  const response = await fetch(`${API_BASE_URL}/usuarios/${usuarioId}/publicaciones`)

  if (!response.ok) {
    throw new Error(`Error al obtener tus publicaciones: ${response.status} ${response.statusText}`)
  }

  return response.json()
}

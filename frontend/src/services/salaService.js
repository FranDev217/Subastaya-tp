const API_BASE_URL = '/api/v1'

async function construirError(response, mensajePorDefecto) {
  const data = await response.json().catch(() => null)
  const error = new Error(data?.mensaje ?? mensajePorDefecto)
  error.status = response.status
  error.errores = data?.errores ?? null
  return error
}

export async function obtenerDetalleSubasta(subastaId) {
  const response = await fetch(`${API_BASE_URL}/subastas/${subastaId}`)

  if (!response.ok) {
    throw await construirError(
      response,
      `No se pudo cargar la subasta: ${response.status}`,
    )
  }

  return response.json()
}

export async function obtenerHistorialPujas(subastaId) {
  const response = await fetch(`${API_BASE_URL}/subastas/${subastaId}/pujas`)

  if (!response.ok) {
    throw await construirError(
      response,
      `No se pudo cargar el historial de pujas: ${response.status}`,
    )
  }

  return response.json()
}

export async function registrarPuja(subastaId, { compradorId, monto }) {
  const response = await fetch(`${API_BASE_URL}/subastas/${subastaId}/pujas`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ compradorId, monto }),
  })

  if (!response.ok) {
    throw await construirError(
      response,
      `No se pudo registrar la puja: ${response.status}`,
    )
  }

  return response.json()
}

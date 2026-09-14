const API_BASE_URL = '/api/v1'

export async function obtenerSaldo(usuarioId) {
  const response = await fetch(`${API_BASE_URL}/billeteras/${usuarioId}`)

  if (!response.ok) {
    throw new Error(`Error al obtener el saldo: ${response.status} ${response.statusText}`)
  }

  return response.json()
}

export async function obtenerMovimientos(usuarioId) {
  const response = await fetch(`${API_BASE_URL}/billeteras/${usuarioId}/movimientos`)

  if (!response.ok) {
    throw new Error(`Error al obtener los movimientos: ${response.status} ${response.statusText}`)
  }

  return response.json()
}

export async function depositar(usuarioId, monto) {
  const response = await fetch(`${API_BASE_URL}/billeteras/${usuarioId}/depositos`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ monto }),
  })

  const data = await response.json().catch(() => null)

  if (!response.ok) {
    const error = new Error(data?.mensaje ?? `Error al depositar: ${response.status}`)
    error.errores = data?.errores ?? null
    throw error
  }

  return data
}

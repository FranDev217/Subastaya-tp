import { useState } from 'react'
import { login } from '../api/authApi'
import './LoginPage.css'

const USUARIO_STORAGE_KEY = 'subastaya_usuario'

function usuarioGuardado() {
  try {
    const guardado = localStorage.getItem(USUARIO_STORAGE_KEY)
    return guardado ? JSON.parse(guardado) : null
  } catch {
    return null
  }
}

function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState(null)
  const [usuario, setUsuario] = useState(usuarioGuardado)

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    setCargando(true)
    try {
      const datosUsuario = await login(email, password)
      localStorage.setItem(USUARIO_STORAGE_KEY, JSON.stringify(datosUsuario))
      setUsuario(datosUsuario)
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  function handleLogout() {
    localStorage.removeItem(USUARIO_STORAGE_KEY)
    setUsuario(null)
    setEmail('')
    setPassword('')
  }

  if (usuario) {
    return (
      <div className="login-page">
        <div className="login-card">
          <h1>SubastaYa</h1>
          <p className="login-subtitle">Sesión iniciada</p>
          <div className="login-success">
            <p className="login-welcome">
              Hola, <strong>{usuario.nombre}</strong>
            </p>
            <p className="login-email">{usuario.email}</p>
          </div>
          <button type="button" className="login-button login-button-secondary" onClick={handleLogout}>
            Cerrar sesión
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={handleSubmit}>
        <h1>SubastaYa</h1>
        <p className="login-subtitle">Iniciá sesión para pujar</p>

        {error && (
          <div className="login-error" role="alert">
            {error}
          </div>
        )}

        <label className="login-field">
          <span>Email</span>
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="comprador1@test.com"
            autoComplete="email"
            required
            disabled={cargando}
          />
        </label>

        <label className="login-field">
          <span>Contraseña</span>
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="••••••••"
            autoComplete="current-password"
            required
            disabled={cargando}
          />
        </label>

        <button type="submit" className="login-button" disabled={cargando}>
          {cargando ? 'Ingresando…' : 'Ingresar'}
        </button>

        <p className="login-hint">
          Usuarios de prueba: comprador1@test.com, comprador2@test.com, vendedor@test.com o
          sinfondos@test.com — contraseña: Password123!
        </p>
      </form>
    </div>
  )
}

export default LoginPage

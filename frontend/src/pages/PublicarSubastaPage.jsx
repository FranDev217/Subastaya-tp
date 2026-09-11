import { useState, useEffect } from 'react'
import { Navigate, Link } from 'react-router-dom'
import { crearSubasta, obtenerCategorias } from '../services/subastasService'
import './PublicarSubastaPage.css'

const USUARIO_STORAGE_KEY = 'subastaya_usuario'

const FORM_VACIO = {
  titulo: '',
  descripcion: '',
  urlImagen: '',
  categoriaId: '',
  precioBase: '',
  incrementoMinimo: '',
  fechaInicio: '',
  fechaFin: '',
}

function usuarioGuardado() {
  try {
    const guardado = localStorage.getItem(USUARIO_STORAGE_KEY)
    return guardado ? JSON.parse(guardado) : null
  } catch {
    return null
  }
}

function validar(form) {
  const errores = {}

  if (!form.titulo.trim()) errores.titulo = 'El título es obligatorio'
  if (!form.descripcion.trim()) errores.descripcion = 'La descripción es obligatoria'
  if (!form.categoriaId) errores.categoriaId = 'Seleccioná una categoría'

  if (!form.precioBase || Number(form.precioBase) <= 0) {
    errores.precioBase = 'El precio base debe ser mayor a 0'
  }
  if (!form.incrementoMinimo || Number(form.incrementoMinimo) <= 0) {
    errores.incrementoMinimo = 'El incremento mínimo debe ser mayor a 0'
  }

  if (!form.fechaInicio) {
    errores.fechaInicio = 'La fecha de inicio es obligatoria'
  }
  if (!form.fechaFin) {
    errores.fechaFin = 'La fecha de finalización es obligatoria'
  } else if (form.fechaInicio && new Date(form.fechaFin) <= new Date(form.fechaInicio)) {
    errores.fechaFin = 'La fecha de finalización debe ser posterior a la de inicio'
  } else if (new Date(form.fechaFin) <= new Date()) {
    errores.fechaFin = 'La fecha de finalización debe ser futura'
  }

  if (form.urlImagen.trim() && !/^https?:\/\/.+/i.test(form.urlImagen.trim())) {
    errores.urlImagen = 'Ingresá una URL válida (http o https)'
  }

  return errores
}

function PublicarSubastaPage() {
  const usuario = usuarioGuardado()
  const [form, setForm] = useState(FORM_VACIO)
  const [errores, setErrores] = useState({})
  const [errorGeneral, setErrorGeneral] = useState(null)
  const [subastaCreada, setSubastaCreada] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [categorias, setCategorias] = useState([])
  const [cargandoCategorias, setCargandoCategorias] = useState(true)

  useEffect(() => {
    obtenerCategorias()
      .then(setCategorias)
      .catch(() => setCategorias([]))
      .finally(() => setCargandoCategorias(false))
  }, [])

  if (!usuario) {
    return <Navigate to="/login" replace state={{ from: '/publicar' }} />
  }

  function handleChange(event) {
    const { name, value } = event.target
    setForm((anterior) => ({ ...anterior, [name]: value }))
    setErrores((anterior) => ({ ...anterior, [name]: undefined }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setErrorGeneral(null)
    setSubastaCreada(null)

    const erroresValidacion = validar(form)
    setErrores(erroresValidacion)
    if (Object.keys(erroresValidacion).length > 0) {
      return
    }

    setCargando(true)
    try {
      const creada = await crearSubasta({
        titulo: form.titulo.trim(),
        descripcion: form.descripcion.trim(),
        urlImagen: form.urlImagen.trim() || null,
        categoriaId: Number(form.categoriaId),
        precioBase: Number(form.precioBase),
        incrementoMinimo: Number(form.incrementoMinimo),
        fechaInicio: form.fechaInicio,
        fechaFin: form.fechaFin,
        vendedorId: usuario.usuarioId,
      })
      setSubastaCreada(creada)
      setForm(FORM_VACIO)
      setErrores({})
    } catch (err) {
      setErrores(err.errores ?? {})
      setErrorGeneral(err.message)
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="publicar-page">
      <header className="publicar-page__header">
        <div>
          <h1 className="publicar-page__titulo">Publicar subasta</h1>
          <p className="publicar-page__subtitulo">
            Completá los datos del producto y la configuración de la subasta.
          </p>
        </div>
        <Link className="publicar-page__volver" to="/">
          ← Volver al catálogo
        </Link>
      </header>

      <form className="publicar-form" onSubmit={handleSubmit} noValidate>
        {subastaCreada && (
          <div className="publicar-mensaje publicar-mensaje--exito" role="status">
            <p>
              ¡Subasta publicada! Quedó en estado <strong>{subastaCreada.estado}</strong>.
            </p>
            <Link to="/">Ver en el catálogo</Link>
          </div>
        )}

        {errorGeneral && (
          <div className="publicar-mensaje publicar-mensaje--error" role="alert">
            {errorGeneral}
          </div>
        )}

        <fieldset className="publicar-form__seccion" disabled={cargando}>
          <legend className="publicar-form__leyenda">Datos del producto</legend>

          <label className="publicar-form__campo">
            <span>Título</span>
            <input
              name="titulo"
              value={form.titulo}
              onChange={handleChange}
              maxLength={200}
              placeholder="Ej: Notebook Gamer RTX 4070"
            />
            {errores.titulo && <small className="publicar-form__error">{errores.titulo}</small>}
          </label>

          <label className="publicar-form__campo">
            <span>Descripción</span>
            <textarea
              name="descripcion"
              value={form.descripcion}
              onChange={handleChange}
              rows={4}
              placeholder="Detallá el estado, características y accesorios del producto."
            />
            {errores.descripcion && (
              <small className="publicar-form__error">{errores.descripcion}</small>
            )}
          </label>

          <label className="publicar-form__campo">
            <span>URL de imagen (opcional)</span>
            <input
              name="urlImagen"
              type="url"
              value={form.urlImagen}
              onChange={handleChange}
              maxLength={500}
              placeholder="https://..."
            />
            {errores.urlImagen && <small className="publicar-form__error">{errores.urlImagen}</small>}
          </label>

          <label className="publicar-form__campo">
            <span>Categoría</span>
            <select
              name="categoriaId"
              value={form.categoriaId}
              onChange={handleChange}
              disabled={cargandoCategorias}
            >
              <option value="">
                {cargandoCategorias ? 'Cargando categorías…' : 'Seleccioná una categoría'}
              </option>
              {categorias.map((categoria) => (
                <option key={categoria.id} value={categoria.id}>
                  {categoria.nombre}
                </option>
              ))}
            </select>
            {errores.categoriaId && (
              <small className="publicar-form__error">{errores.categoriaId}</small>
            )}
          </label>
        </fieldset>

        <fieldset className="publicar-form__seccion" disabled={cargando}>
          <legend className="publicar-form__leyenda">Configuración económica</legend>

          <div className="publicar-form__fila">
            <label className="publicar-form__campo">
              <span>Precio base inicial</span>
              <input
                name="precioBase"
                type="number"
                min="0"
                step="0.01"
                inputMode="decimal"
                value={form.precioBase}
                onChange={handleChange}
                placeholder="40000"
              />
              {errores.precioBase && (
                <small className="publicar-form__error">{errores.precioBase}</small>
              )}
            </label>

            <label className="publicar-form__campo">
              <span>Incremento mínimo por puja</span>
              <input
                name="incrementoMinimo"
                type="number"
                min="0"
                step="0.01"
                inputMode="decimal"
                value={form.incrementoMinimo}
                onChange={handleChange}
                placeholder="1000"
              />
              {errores.incrementoMinimo && (
                <small className="publicar-form__error">{errores.incrementoMinimo}</small>
              )}
            </label>
          </div>
        </fieldset>

        <fieldset className="publicar-form__seccion" disabled={cargando}>
          <legend className="publicar-form__leyenda">Ventana temporal</legend>

          <div className="publicar-form__fila">
            <label className="publicar-form__campo">
              <span>Fecha y hora de inicio</span>
              <input
                name="fechaInicio"
                type="datetime-local"
                value={form.fechaInicio}
                onChange={handleChange}
              />
              {errores.fechaInicio && (
                <small className="publicar-form__error">{errores.fechaInicio}</small>
              )}
            </label>

            <label className="publicar-form__campo">
              <span>Fecha y hora de finalización</span>
              <input
                name="fechaFin"
                type="datetime-local"
                min={form.fechaInicio || undefined}
                value={form.fechaFin}
                onChange={handleChange}
              />
              {errores.fechaFin && (
                <small className="publicar-form__error">{errores.fechaFin}</small>
              )}
            </label>
          </div>
        </fieldset>

        <button type="submit" className="publicar-form__enviar" disabled={cargando}>
          {cargando ? 'Publicando…' : 'Publicar subasta'}
        </button>
      </form>
    </div>
  )
}

export default PublicarSubastaPage

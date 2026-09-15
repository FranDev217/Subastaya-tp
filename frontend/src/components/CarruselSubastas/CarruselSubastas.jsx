import { useEffect, useRef } from "react";
import { Link } from "react-router-dom";
import SubastaCard from "../SubastaCard/SubastaCard";
import "./CarruselSubastas.css";

const VELOCIDAD_PX_POR_FRAME = 0.6;
const MAX_CARDS_VISIBLES = 4;
const UMBRAL_ARRASTRE = 5;

function CarruselSubastas({ titulo, subastas = [], direccion = "izquierda" }) {
  const trackRef = useRef(null);
  const periodoRef = useRef(0);
  const pausadoRef = useRef(false);
  const arrastrandoRef = useRef(false);
  const capturadoRef = useRef(false);
  const movidoRef = useRef(false);
  const dragRef = useRef({ x: 0, scroll: 0 });
  const reactivarRef = useRef(null);

  const velocidad =
    direccion === "derecha" ? -VELOCIDAD_PX_POR_FRAME : VELOCIDAD_PX_POR_FRAME;

  const pausarAutoplay = () => {
    pausadoRef.current = true;
  };

  const reanudarAutoplay = () => {
    if (arrastrandoRef.current) return;
    pausadoRef.current = false;
  };

  // Mide el ancho exacto de una copia (periodo del loop) sin recalcularlo por frame.
  useEffect(() => {
    const track = trackRef.current;
    if (!track) return undefined;

    const medir = () => {
      const primero = track.children[0];
      const segundo = track.children[subastas.length];
      if (primero && segundo) {
        periodoRef.current = segundo.offsetLeft - primero.offsetLeft;
        return;
      }
      const copias = Number(track.dataset.copias) || 1;
      periodoRef.current = track.scrollWidth / copias;
    };

    medir();

    const observador = new ResizeObserver(medir);
    observador.observe(track);
    if (track.firstElementChild) observador.observe(track.firstElementChild);

    return () => observador.disconnect();
  }, [subastas.length]);

  // Autoplay continuo en loop infinito.
  useEffect(() => {
    const track = trackRef.current;
    if (!track) return undefined;

    const prefiereQuieto = window.matchMedia(
      "(prefers-reduced-motion: reduce)",
    ).matches;
    if (prefiereQuieto) return undefined;

    let frame;
    const avanzar = () => {
      if (!pausadoRef.current && !document.hidden) {
        const periodo = periodoRef.current;
        if (periodo > 0) {
          track.scrollLeft += velocidad;
          if (track.scrollLeft >= periodo) {
            track.scrollLeft -= periodo;
          } else if (track.scrollLeft <= 0) {
            track.scrollLeft += periodo;
          }
        }
      }
      frame = requestAnimationFrame(avanzar);
    };

    frame = requestAnimationFrame(avanzar);
    return () => cancelAnimationFrame(frame);
  }, [velocidad, subastas.length]);

  useEffect(() => () => window.clearTimeout(reactivarRef.current), []);

  const normalizar = () => {
    const track = trackRef.current;
    const periodo = periodoRef.current;
    if (!track || periodo <= 0) return;
    if (track.scrollLeft >= periodo) track.scrollLeft -= periodo;
    else if (track.scrollLeft <= 0) track.scrollLeft += periodo;
  };

  const manejarPointerDown = (e) => {
    if (e.pointerType === "mouse" && e.button !== 0) return;
    const track = trackRef.current;
    arrastrandoRef.current = true;
    capturadoRef.current = false;
    movidoRef.current = false;
    dragRef.current = { x: e.clientX, scroll: track.scrollLeft };
    pausarAutoplay();
  };

  const manejarPointerMove = (e) => {
    if (!arrastrandoRef.current) return;
    const delta = e.clientX - dragRef.current.x;
    if (Math.abs(delta) <= UMBRAL_ARRASTRE) return;

    const track = trackRef.current;
    if (!capturadoRef.current) {
      // Solo tomamos el control del puntero cuando hay un arrastre real,
      // así un click/tap simple llega intacto al link de la card.
      try {
        track.setPointerCapture?.(e.pointerId);
        capturadoRef.current = true;
      } catch {
        /* el navegador no soporta pointer capture */
      }
    }
    movidoRef.current = true;
    track.scrollLeft = dragRef.current.scroll - delta;
  };

  const terminarArrastre = (e) => {
    if (!arrastrandoRef.current) return;
    arrastrandoRef.current = false;
    const track = trackRef.current;

    if (capturadoRef.current) {
      try {
        track.releasePointerCapture?.(e.pointerId);
      } catch {
        /* el puntero ya se liberó */
      }
      capturadoRef.current = false;
    }

    normalizar();
    reanudarAutoplay();
  };

  const manejarClickCaptura = (e) => {
    if (!movidoRef.current) return;
    e.preventDefault();
    e.stopPropagation();
    movidoRef.current = false;
  };

  const desplazar = (sentido) => {
    const track = trackRef.current;
    if (!track) return;
    pausarAutoplay();

    const item = track.querySelector(".carrusel-subastas__item");
    const gap = parseFloat(getComputedStyle(track).columnGap) || 0;
    const salto = item
      ? item.getBoundingClientRect().width + gap
      : track.clientWidth;

    track.scrollBy({ left: sentido * salto, behavior: "smooth" });

    window.clearTimeout(reactivarRef.current);
    reactivarRef.current = window.setTimeout(() => {
      normalizar();
      reanudarAutoplay();
    }, 420);
  };

  if (!subastas.length) return null;

  const copias = Math.max(
    2,
    Math.ceil(MAX_CARDS_VISIBLES / subastas.length) + 1,
  );
  const items = Array.from({ length: copias }, (_, copia) =>
    subastas.map((subasta) => ({ subasta, copia })),
  ).flat();

  return (
    <section className="carrusel-subastas" aria-label={titulo}>
      <div className="carrusel-subastas__header">
        <h2 className="carrusel-subastas__titulo">{titulo}</h2>
      </div>

      <div className="carrusel-subastas__viewport">
        <button
          type="button"
          className="carrusel-subastas__flecha carrusel-subastas__flecha--prev"
          aria-label="Ver anteriores"
          onClick={() => desplazar(-1)}
        >
          <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
            <path
              d="M15 18l-6-6 6-6"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>

        <div
          ref={trackRef}
          className="carrusel-subastas__track"
          data-copias={copias}
          onPointerDown={manejarPointerDown}
          onPointerMove={manejarPointerMove}
          onPointerUp={terminarArrastre}
          onPointerCancel={terminarArrastre}
          onClickCapture={manejarClickCaptura}
        >
          {items.map(({ subasta, copia }) => (
            <div
              className="carrusel-subastas__item"
              key={`${subasta.id}-${copia}`}
              aria-hidden={copia > 0 ? "true" : undefined}
            >
              <Link
                to={`/subasta/${subasta.id}`}
                className="carrusel-subastas__link"
                tabIndex={copia > 0 ? -1 : undefined}
              >
                <SubastaCard subasta={subasta} />
              </Link>
            </div>
          ))}
        </div>

        <button
          type="button"
          className="carrusel-subastas__flecha carrusel-subastas__flecha--next"
          aria-label="Ver siguientes"
          onClick={() => desplazar(1)}
        >
          <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
            <path
              d="M9 6l6 6-6 6"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>
      </div>
    </section>
  );
}

export default CarruselSubastas;

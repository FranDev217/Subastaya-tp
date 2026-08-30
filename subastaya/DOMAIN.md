# Dominio de SubastaYa

Este documento consolida el modelo de dominio a partir de la consigna del TP
(diagrama sugerido, reglas de negocio y seed data obligatorio). Sirve como
contrato de referencia antes de escribir la primera migración Flyway y las
entidades JPA.

## 1. Entidades

### Usuario
| Campo | Tipo | Notas |
|---|---|---|
| id | Long (PK) | |
| email | String | único |
| nombre | String | |
| password_hash | String | |
| fecha_registro | datetime | |

Relaciones: 1:1 con `Billetera`, 1:N con `Subasta` (como vendedor), 1:N con
`Puja` (como comprador), 1:N con `AuditoriaLog` (opcional, si la acción la
disparó un usuario y no el Worker).

### Categoria
| Campo | Tipo | Notas |
|---|---|---|
| id | Long (PK) | |
| nombre | String | Tecnología, Coleccionables, Indumentaria, Vehículos (seed) |
| url_icono | String | |

### Subasta
| Campo | Tipo | Notas |
|---|---|---|
| id | Long (PK) | |
| vendedor_id | FK → Usuario | |
| categoria_id | FK → Categoria | |
| titulo | String | |
| descripcion | String | |
| url_imagen | String | |
| precio_base | Decimal | > 0 |
| incremento_minimo | Decimal | > 0 |
| fecha_inicio | datetime | |
| fecha_fin | datetime | > fecha_inicio; se extiende por anti-sniping |
| estado | Enum | `PROGRAMADA`, `ACTIVA`, `FINALIZADA`, `DESIERTA` |
| version | int | Optimistic Locking (obligatorio) |

Transiciones de estado válidas:
`PROGRAMADA → ACTIVA → (FINALIZADA | DESIERTA)`. `ACTIVA` puede reescribir su
propio `fecha_fin` (anti-sniping) sin cambiar de estado.

### Billetera
| Campo | Tipo | Notas |
|---|---|---|
| id | Long (PK) | |
| usuario_id | FK → Usuario (1:1) | |
| saldo_total | Decimal | |
| saldo_retenido | Decimal | suma de pujas donde el usuario es líder vigente |
| saldo_disponible | Decimal | `saldo_total - saldo_retenido` |
| version | int | Optimistic Locking (obligatorio) |

Decisión a tomar en la migración: si `saldo_disponible` se persiste como
columna redundante (recalculada dentro de la misma transacción que toca
`saldo_retenido`) o se expone como valor derivado en el DTO. La consigna lo
muestra como columna en el diagrama, así que lo persistimos, pero la
invariante `saldo_disponible = saldo_total - saldo_retenido` debe mantenerse
en cada operación de escrow.

### Puja
| Campo | Tipo | Notas |
|---|---|---|
| id | Long (PK) | |
| subasta_id | FK → Subasta | |
| comprador_id | FK → Usuario | |
| monto | Decimal | |
| fecha_puja | datetime | |

No lleva `version`: una puja es un hecho inmutable, no se edita. La
concurrencia se resuelve a nivel `Subasta`/`Billetera`.

### TransaccionLedger
| Campo | Tipo | Notas |
|---|---|---|
| id | Long (PK) | |
| billetera_id | FK → Billetera | |
| tipo | Enum | `DEPOSITO`, `RETENCION`, `LIBERACION`, `PAGO`, `COBRO` |
| monto | Decimal | |
| fecha | datetime | |
| subasta_id | FK → Subasta (nullable) | trazabilidad opcional |

Es el libro mayor contable: cada movimiento de saldo (depósito manual,
retención por puja, liberación por ser superado, pago del comprador ganador,
cobro del vendedor) debe dejar un registro acá dentro de la misma transacción
que modifica `Billetera`.

### AuditoriaLog
| Campo | Tipo | Notas |
|---|---|---|
| id | Long (PK) | |
| entidad | String | `SUBASTA`, `BILLETERA`, `SISTEMA` |
| entidad_id | Long | id del registro afectado |
| accion | String | ej. `EXTENSION_TIEMPO`, `CIERRE_WORKER`, `PUJA_RECHAZADA`, `ACREDITACION_MANUAL` |
| usuario_id | FK → Usuario (nullable) | null si la acción la ejecutó el Worker |
| detalle_json | String/JSON | payload con los cambios |
| fecha | datetime | |

Eventos que **obligatoriamente** deben auditarse (según consigna 3.4):
cambios de estado de subasta, extensiones anti-sniping, pujas rechazadas por
concurrencia o validación de negocio, y acreditaciones manuales de saldo.

## 2. Reglas de negocio

### 2.1 Escrow atómico (puja)
Al recibir `POST /api/auctions/{id}/bids`, dentro de una única transacción:
1. Validar que la subasta esté `ACTIVA` → si no, `400`.
2. Validar que el monto sea mayor a la puja actual + `incremento_minimo` →
   si no, `422`.
3. Validar que el comprador tenga `saldo_disponible >= monto` → si no, `422`
   y se registra un `AuditoriaLog` de puja rechazada.
4. Si todo es válido: congelar el saldo del nuevo postor, liberar el saldo
   retenido del postor anterior (si existía), registrar la `Puja` como
   líder y escribir los movimientos correspondientes en el `Ledger`.
5. Evaluar la regla anti-sniping (2.2).
6. Responder `200 OK` con el estado actualizado.

Un conflicto de versión (`version` de `Subasta` o `Billetera`) en el paso 4
debe traducirse a `409 Conflict`, nunca a un `500`.

### 2.2 Anti-sniping
Si la puja válida se registra a ≤ 60 segundos del `fecha_fin` de la subasta,
extender `fecha_fin` en +2 minutos y registrar un `AuditoriaLog` de tipo
`EXTENSION_TIEMPO`.

### 2.3 Background Worker (liquidación)
Proceso `@Scheduled` que, para cada subasta vencida (`fecha_fin` pasada y
`estado = ACTIVA`):
- **Con pujas**: pasa a `FINALIZADA` y ejecuta una liquidación atómica
  (debitar saldo retenido del comprador ganador, acreditar al vendedor,
  escribir en el `Ledger`) + `AuditoriaLog` de venta.
- **Sin pujas**: pasa a `DESIERTA` + `AuditoriaLog` del cambio de estado.

## 3. Seed data obligatorio

**Usuarios / Billeteras:**
| Email | Total | Retenido | Disponible |
|---|---|---|---|
| vendedor@test.com | 0 | 0 | 0 |
| comprador1@test.com | 150.000 | 45.000 | 105.000 |
| comprador2@test.com | 200.000 | 0 | 200.000 |
| sinfondos@test.com | 500 | 0 | 500 |

**Categorías:** Tecnología, Coleccionables, Indumentaria, Vehículos.

**Subastas (5, casos de prueba):**
1. Activa estándar — cierra en 20-30 min, 2 pujas previas, líder $45.000
   (retenido de `comprador1`, coherente con su billetera).
2. Activa crítica — cierra en < 2 min (para probar alerta visual + anti-sniping).
3. Próxima — inicio a +24 hs, pujas bloqueadas.
4. Vencida con ganador — `fecha_fin` pasada + puja ganadora (para probar el Worker).
5. Vencida desierta — `fecha_fin` pasada, sin pujas (para probar pase a `DESIERTA`).

**Registros contables:** el historial de las 2 pujas previas de la subasta
activa estándar, y las transacciones de `Ledger` que respalden los depósitos
y el `saldo_retenido` de $45.000 de `comprador1`.

## 4. API de referencia (a ampliar)

| Endpoint | Propósito |
|---|---|
| `GET /api/v1/subastas` | Listado con paginación y filtros (estado, categoría, precio, orden) |
| `POST /api/v1/subastas` | Creación de subasta |
| `GET /api/v1/subastas/{id}` | Detalle + estado + puja actual |
| `GET /api/v1/subastas/{id}/pujas` | Historial de pujas de una subasta |
| `POST /api/v1/subastas/{id}/pujas` | Nueva oferta (valida saldo, incremento, anti-sniping) |
| `GET /api/v1/billeteras/{id}` o `/me` | Desglose de saldos |
| `POST /api/v1/billeteras/{id}/depositos` | Acreditación simulada de fondos |

Nombres de recursos en plural, sin verbos en la URL (según lineamiento de la
consigna) — se ajustan levemente los ejemplos de la consigna
(`/api/auctions/{id}/bids` → `/api/v1/subastas/{id}/pujas`) para mantener
consistencia en español y con la jerarquía recurso/subrecurso.

## 5. Pendiente de definir

- Si `Puja` necesita un estado propio (`ACEPTADA`/`RECHAZADA`) o si el
  rechazo solo se refleja en el `AuditoriaLog` sin persistir la puja.
- Formato exacto de `detalle_json` en `AuditoriaLog` (por ahora, JSON libre).
- Estrategia de paginación para `GET /api/v1/subastas`.

# Dominio de SubastaYa

Este documento consolida el modelo de dominio a partir de la consigna del TP
(diagrama sugerido, reglas de negocio y seed data obligatorio). Sirve como
contrato de referencia antes de escribir la primera migración Flyway y las
entidades JPA.

## 1. Entidades

### Usuario

| Campo          | Tipo      | Notas                                                            |
| -------------- | --------- | ---------------------------------------------------------------- |
| id             | Long (PK) |                                                                  |
| email          | String    | único                                                            |
| nombre         | String    |                                                                  |
| password_hash  | String    | hash BCrypt real desde `V4__seed_passwords_bcrypt.sql` (ver 2.4) |
| fecha_registro | datetime  |                                                                  |

Relaciones: 1:1 con `Billetera`, 1:N con `Subasta` (como vendedor), 1:N con
`Puja` (como comprador), 1:N con `AuditoriaLog` (opcional, si la acción la
disparó un usuario y no el Worker).

### Categoria

| Campo     | Tipo      | Notas                                                      |
| --------- | --------- | ---------------------------------------------------------- |
| id        | Long (PK) |                                                            |
| nombre    | String    | Tecnología, Coleccionables, Indumentaria, Vehículos (seed) |
| url_icono | String    |                                                            |

### Subasta

| Campo             | Tipo           | Notas                                            |
| ----------------- | -------------- | ------------------------------------------------ |
| id                | Long (PK)      |                                                  |
| vendedor_id       | FK → Usuario   |                                                  |
| categoria_id      | FK → Categoria |                                                  |
| titulo            | String         |                                                  |
| descripcion       | String         |                                                  |
| url_imagen        | String         |                                                  |
| precio_base       | Decimal        | > 0                                              |
| incremento_minimo | Decimal        | > 0                                              |
| fecha_inicio      | datetime       |                                                  |
| fecha_fin         | datetime       | > fecha_inicio; se extiende por anti-sniping     |
| estado            | Enum           | `PROGRAMADA`, `ACTIVA`, `FINALIZADA`, `DESIERTA` |
| version           | int            | Optimistic Locking (obligatorio)                 |

Transiciones de estado válidas:
`PROGRAMADA → ACTIVA → (FINALIZADA | DESIERTA)`. `ACTIVA` puede reescribir su
propio `fecha_fin` (anti-sniping) sin cambiar de estado. La transición
`PROGRAMADA → ACTIVA` la ejecuta `SubastaActivacionWorker` cuando llega
`fecha_inicio` (ver 2.5).

### Billetera

| Campo            | Tipo               | Notas                                           |
| ---------------- | ------------------ | ----------------------------------------------- |
| id               | Long (PK)          |                                                 |
| usuario_id       | FK → Usuario (1:1) |                                                 |
| saldo_total      | Decimal            |                                                 |
| saldo_retenido   | Decimal            | suma de pujas donde el usuario es líder vigente |
| saldo_disponible | Decimal            | `saldo_total - saldo_retenido`                  |
| version          | int                | Optimistic Locking (obligatorio)                |

Decisión a tomar en la migración: si `saldo_disponible` se persiste como
columna redundante (recalculada dentro de la misma transacción que toca
`saldo_retenido`) o se expone como valor derivado en el DTO. La consigna lo
muestra como columna en el diagrama, así que lo persistimos, pero la
invariante `saldo_disponible = saldo_total - saldo_retenido` debe mantenerse
en cada operación de escrow.

### Puja

| Campo        | Tipo         | Notas |
| ------------ | ------------ | ----- |
| id           | Long (PK)    |       |
| subasta_id   | FK → Subasta |       |
| comprador_id | FK → Usuario |       |
| monto        | Decimal      |       |
| fecha_puja   | datetime     |       |

No lleva `version`: una puja es un hecho inmutable, no se edita. La
concurrencia se resuelve a nivel `Subasta`/`Billetera`.

### TransaccionLedger

| Campo        | Tipo                    | Notas                                                  |
| ------------ | ----------------------- | ------------------------------------------------------ |
| id           | Long (PK)               |                                                        |
| billetera_id | FK → Billetera          |                                                        |
| tipo         | Enum                    | `DEPOSITO`, `RETENCION`, `LIBERACION`, `PAGO`, `COBRO` |
| monto        | Decimal                 |                                                        |
| fecha        | datetime                |                                                        |
| subasta_id   | FK → Subasta (nullable) | trazabilidad opcional                                  |

Es el libro mayor contable: cada movimiento de saldo (depósito manual,
retención por puja, liberación por ser superado, pago del comprador ganador,
cobro del vendedor) debe dejar un registro acá dentro de la misma transacción
que modifica `Billetera`.

### AuditoriaLog

| Campo        | Tipo                    | Notas                                                                            |
| ------------ | ----------------------- | -------------------------------------------------------------------------------- |
| id           | Long (PK)               |                                                                                  |
| entidad      | String                  | `SUBASTA`, `BILLETERA`, `SISTEMA`                                                |
| entidad_id   | Long                    | id del registro afectado                                                         |
| accion       | String                  | ej. `SUBASTA_CREADA`, `APERTURA_WORKER`, `EXTENSION_TIEMPO`, `CIERRE_WORKER`, `PUJA_RECHAZADA`, `ACREDITACION_MANUAL` |
| usuario_id   | FK → Usuario (nullable) | null si la acción la ejecutó el Worker                                           |
| detalle_json | String/JSON             | payload con los cambios                                                          |
| fecha        | datetime                |                                                                                  |

Eventos que **obligatoriamente** deben auditarse (según consigna 3.4):
cambios de estado de subasta, extensiones anti-sniping, pujas rechazadas por
concurrencia o validación de negocio, y acreditaciones manuales de saldo.

**Implementación (2.4 cubierto):**

- `CIERRE_WORKER` — Worker cierra como `FINALIZADA` o `DESIERTA` (`usuario_id = null`).
- `EXTENSION_TIEMPO` — anti-sniping en `PujaService`.
- `PUJA_RECHAZADA` — subasta inactiva, monto inválido, saldo insuficiente o
  `409` de concurrencia (`Billetera` o `Subasta.version`). `registrarRechazo`
  usa `REQUIRES_NEW` para sobrevivir el rollback. La puja rechazada **no** se
  persiste en `puja`.
- `ACREDITACION_MANUAL` — `BilleteraService.depositar`, entidad `BILLETERA`.
- `SUBASTA_CREADA` — alta de subasta por el vendedor (`SubastaService.crearSubasta`),
  con `usuario_id` del vendedor.
- `APERTURA_WORKER` — `SubastaActivacionWorker` abre una `PROGRAMADA` cuyo
  `fecha_inicio` llegó (`usuario_id = null`).
- Consulta: `GET /api/v1/auditoria?entidad=&entidadId=` (sin PUT/DELETE).

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
  **Implementación:**
- `LiquidacionService.cerrarSubasta` re-valida `estado = ACTIVA` y `fecha_fin`
  vencida al recargar la subasta dentro de la transacción, por si anti-sniping
  extendió el plazo entre la consulta de vencidas y el cierre. Si no aplica, la
  subasta queda como está y se reprocesa en la corrida siguiente.
- Liquidación: débito al comprador antes que crédito al vendedor (si falta
  saldo retenido, no se acredita ni se cierra la subasta). Cambio de estado +
  ambas billeteras + Ledger + `AuditoriaLog` viajan en una única transacción.
- `AuditoriaLog` del Worker: `accion = CIERRE_WORKER`, `usuario_id = null`
  (acción del sistema); comprador/vendedor/monto quedan en el detalle.
- Un error al cerrar una subasta puntual no frena el resto de la corrida
  (queda logueada y se reintenta a los 60s).
- `TipoEvento` suma `FINALIZADA` y `DESIERTA` para la difusión WebSocket del
  cierre (ver sección 6).

### 2.4 Autenticación (login)

`POST /api/v1/auth/login` valida `email` + `password` contra `usuario` con
`PasswordEncoder` (BCrypt, `spring-security-crypto`) y devuelve la identidad
del usuario (`id`, `nombre`, `email`). No hay Spring Security completo ni
sesión/token: es una validación de credenciales, no una capa de autorización.

- Email inexistente o contraseña incorrecta → mismo `401` con mensaje
  genérico (`CredencialesInvalidasException`), para no revelar cuál de los
  dos datos era el incorrecto.
- El frontend persiste la respuesta en `localStorage` y la reutiliza donde
  hace falta identificar al usuario (billetera, pujas) — mismo patrón que ya
  usa `PujaRequest.compradorId`.
- Los `password_hash` de `V2__seed.sql` eran strings inventados (no BCrypt
  válido); `V4__seed_passwords_bcrypt.sql` los reemplaza por un hash real de
  `Password123!` para los 4 usuarios semilla.

### 2.5 Creación de subasta (vendedor)

`POST /api/v1/subastas` da de alta una subasta a nombre de un usuario
autenticado como vendedor. Como aún no hay sesión/token, el `vendedorId` viaja
en el body (mismo patrón que `PujaRequest.compradorId`).

Reglas:

1. Validación de campos: `titulo`, `descripcion`, `categoriaId`, `precioBase`,
   `incrementoMinimo`, `fechaInicio`, `fechaFin` y `vendedorId` son
   obligatorios; precios e incremento deben ser `> 0`; `fechaFin` debe ser
   futura.
2. Validación cruzada: `fechaFin > fechaInicio` (`@AssertTrue`, error de campo
   `fechasCoherentes`).
3. `categoriaId` y `vendedorId` deben existir → si no, `404`.
4. Estado inicial: `PROGRAMADA` si `fechaInicio > now()`, `ACTIVA` si
   `fechaInicio <= now()` (permite publicar subastas que arrancan de inmediato).
   La `PROGRAMADA` pasa a `ACTIVA` automáticamente vía `SubastaActivacionWorker`.
5. Respuesta `201 Created` + `Location`. El alta se audita como `SUBASTA_CREADA`.

## 3. Seed data obligatorio

**Usuarios / Billeteras:**
| Email | Total | Retenido | Disponible |
|---|---|---|---|
| vendedor@test.com | 0 | 0 | 0 |
| comprador1@test.com | 150.000 | 45.000 | 105.000 |
| comprador2@test.com | 200.000 | 32.000 | 168.000 |
| sinfondos@test.com | 500 | 0 | 500 |

> `comprador2` tiene $32.000 retenidos porque registra la puja ganadora de la
> subasta vencida del caso 4 (ver más abajo). Sin ese escrow congelado, el
> Worker de liquidación (2.3) no tendría saldo retenido que debitar.

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

| Endpoint                                        | Propósito                                                           |
| ----------------------------------------------- | ------------------------------------------------------------------- |
| `POST /api/v1/auth/login`                       | Login: valida email + contraseña, devuelve la identidad del usuario |
| `GET /api/v1/subastas`                          | Listado con paginación y filtros (estado, categoría, precio, orden) |
| `POST /api/v1/subastas`                         | Creación de subasta por el vendedor (`201` + `Location`)            |
| `GET /api/v1/subastas/{id}`                     | Detalle + estado + puja actual                                      |
| `GET /api/v1/subastas/{id}/pujas`               | Historial de pujas de una subasta                                   |
| `POST /api/v1/subastas/{id}/pujas`              | Nueva oferta (valida saldo, incremento, anti-sniping)               |
| `GET /api/v1/billeteras/{usuarioId}`            | Desglose de saldos                                                  |
| `POST /api/v1/billeteras/{usuarioId}/depositos` | Acreditación simulada de fondos                                     |
| `GET /api/v1/auditoria?entidad=&entidadId=`     | Trazabilidad de eventos de auditoría                                |

Nombres de recursos en plural, sin verbos en la URL (según lineamiento de la
consigna) — se ajustan levemente los ejemplos de la consigna
(`/api/auctions/{id}/bids` → `/api/v1/subastas/{id}/pujas`) para mantener
consistencia en español y con la jerarquía recurso/subrecurso.

## 5. Pendiente de definir

- `Puja` no tiene estado `ACEPTADA`/`RECHAZADA`: el rechazo solo se refleja
  en `AuditoriaLog` (`PUJA_RECHAZADA`) y no se persiste la fila en `puja`.
- Formato exacto de `detalle_json` en `AuditoriaLog`: se usa texto plano
  (convención vigente), no JSON estructurado. Sigue abierto si se quiere
  formalizar un esquema.
- Estrategia de paginación para `GET /api/v1/subastas`.
- El login (2.4) no genera sesión/token: si más adelante hace falta proteger
  endpoints por rol (ej. que solo el vendedor edite su subasta), va a hacer
  falta sumar Spring Security completo (filtro + JWT o sesión) sobre esta base.
  Mientras tanto, la creación de subasta (2.5) confía en el `vendedorId` que
  envía el cliente, por lo que un atacante podría publicar a nombre de otro
  usuario: es una limitación conocida a resolver con autenticación real.

## 6. Tiempo real (dominio)

La subasta es un proceso colaborativo en vivo. No alcanza con REST porque el estado cambia por acciones de terceros.

**Eventos de dominio que se difunden (`TipoEvento`):**

- `ESTADO_ACTUAL`: snapshot al entrar a una subasta (precio, líder, fecha_fin, estado)
- `NUEVA_PUJA`: una puja válida fue aceptada
- `ESTADO_CAMBIADO`: apertura de una subasta `PROGRAMADA` por `SubastaActivacionWorker`
- `FINALIZADA` / `DESIERTA`: cierre por Worker

**Canales:**

- Petición: cliente pide estado inicial por `/app/subastas/{id}`
- Difusión: servidor publica eventos por `/topic/subastas/{id}` a todos los suscriptores

Invariante: toda puja aceptada por `POST /api/v1/subastas/{id}/pujas` DEBE generar un `NUEVA_PUJA`. El Worker al cerrar DEBE generar `FINALIZADA` o `DESIERTA`.

Nota: la extensión por anti-sniping **no** es un evento WebSocket aparte — como
solo puede dispararse al procesar una puja, viaja embebida en el mismo
`NUEVA_PUJA` (el campo `fechaFin` del evento ya refleja la extensión, y
`PujaResponse.extendidoPorAntiSniping` le dice al frontend si corresponde
mostrar el aviso). No hace falta un `TipoEvento.EXTENSION_TIEMPO` separado
porque nunca ocurre de forma independiente de una puja.

## Catálogo de subastas — reglas de filtrado y listado

El listado de subastas (`GET /api/v1/subastas`) admite los siguientes filtros, todos opcionales y combinables entre sí (AND lógico):

| Filtro                    | Tipo            | Descripción                                                                                                    |
| ------------------------- | --------------- | -------------------------------------------------------------------------------------------------------------- |
| `estado`                  | `EstadoSubasta` | ACTIVA, PROGRAMADA, FINALIZADA o DESIERTA                                                                      |
| `categoriaId`             | `Long`          | Filtra por categoría exacta                                                                                    |
| `precioMin` / `precioMax` | `BigDecimal`    | Filtra por `precioBase` (no por oferta actual)                                                                 |
| `sort`                    | `String`        | `menorTiempo` (default, ordena por `fechaFin` ascendente) o `mayorPuja` (ordena por oferta actual descendente) |

**Nota de diseño:** el filtro de precio opera sobre `precioBase`, no sobre la oferta actual (última puja o precio base si no hay pujas). Si una subasta recibió pujas por encima del rango filtrado, igual puede aparecer en el listado — esto es una limitación conocida a resolver en una futura iteración.

### Estado DESIERTA en el listado

`DESIERTA` es un estado terminal, al igual que `FINALIZADA`: una subasta pasa a `DESIERTA` cuando el worker de liquidación (`SubastaLiquidacionWorker`) la encuentra vencida (`fechaFin < now()`) sin ninguna puja registrada. A diferencia de `FINALIZADA` (que implica una liquidación exitosa entre comprador y vendedor), `DESIERTA` no involucra ningún movimiento de billetera.

En el catálogo, el usuario puede filtrar explícitamente por subastas desiertas para revisar publicaciones que no recibieron ofertas.

### Manejo de fechas y timezone (regla operativa)

Todas las fechas del dominio (`fechaInicio`, `fechaFin`, `fechaPuja`, etc.) se almacenan y comparan en **UTC**:

- La JVM del backend corre con `-Duser.timezone=UTC` (fijado explícitamente, tanto en runtime como en los tests vía Surefire `argLine`).
- La sesión de Postgres usa `UTC` como timezone (confirmado con `SHOW timezone`).
- Al serializar a JSON, las fechas `LocalDateTime` se emiten con sufijo `Z` (vía un serializer custom en `JacksonConfig`) para dejar explícita la zona horaria y evitar que los clientes (frontend, u otros consumidores de la API) las interpreten erróneamente como hora local.

**Regla para desarrollo futuro:** cualquier nuevo campo de fecha en una entidad o DTO hereda este comportamiento automáticamente (el serializer está registrado globalmente en el `ObjectMapper` de Spring). Si en algún momento se cambia el timezone de la JVM o de Postgres, este mecanismo deja de ser válido y debe revisarse.

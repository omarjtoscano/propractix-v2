# ADR-004 — Identificadores, tiempo, zonas horarias y concurrencia

- Estado: Propuesto — revisión 2
- Fecha: 2026-09-12
- Decisores: Arquitectura
- Reemplaza: propuesta inicial del ADR-004

## Contexto

Las prácticas combinan instantes técnicos, fechas académicas y horarios locales. Registro, verificación, refresh, idempotencia y outbox además presentan carreras que no se resuelven únicamente con `@Version`.

## Decisión

### Identificadores

- Aggregates y recursos públicos usarán UUID v4 generados en la aplicación.
- El dominio empleará value objects (`UserId`, `TenantId`, `VerificationId`) y no UUID desnudos en sus APIs principales.
- PostgreSQL los persistirá como tipo `uuid`, no `varchar`.

UUID v7 queda aplazado porque Java 21 no lo ofrece de forma estándar y no se añadirá una dependencia solo por localidad de índice durante el MVP.

### Tiempo

- Instantes técnicos: `Instant`, persistidos como `timestamptz` y normalizados a UTC.
- Fechas sin hora: `LocalDate`, persistidas como `date`.
- Horas locales: `LocalTime` más una zona IANA explícita cuando la interpretación dependa de ubicación.
- Zona: identificador IANA persistido como texto validado, por ejemplo `Europe/Madrid`.
- No se usará `LocalDateTime` para timestamps globales.
- Application recibirá un puerto `Clock`; los adapters suministrarán el reloj real y los tests uno fijo.

La zona predeterminada se captura desde la organización o la selección del usuario al crear el dato relevante. No se consulta dinámicamente una configuración nacional para reinterpretar datos históricos.

### Concurrencia optimista

Aggregates mutables tendrán una versión `bigint`. El adapter JPA utilizará `@Version`. Una escritura con versión obsoleta devolverá `409 Conflict` con código `concurrent_modification` y `correlationId`.

### Operaciones atómicas especiales

No dependerán solo de `@Version`:

| Caso | Mecanismo mínimo |
|---|---|
| Consumo de verificación | `UPDATE ... WHERE consumed_at IS NULL AND expires_at > now()` y comprobación de filas. |
| Rotación refresh | Compare-and-set sobre token vigente, constraint de familia y transacción. |
| Idempotencia | Inserción/claim atómico por operación y clave; estados `PROCESSING`/terminal. |
| Outbox | Leasing con `FOR UPDATE SKIP LOCKED`, expiración del lease y contador de intentos. |

Cada mecanismo tendrá prueba concurrente sobre PostgreSQL mediante Testcontainers.

## Alternativas consideradas

- IDs autoincrementales públicos: rechazados por enumeración y acoplamiento a persistencia.
- UUID v7: aplazado.
- Bloqueo pesimista general: rechazado; se utilizará solo en operaciones concretas justificadas.
- Reloj del sistema dentro del dominio: rechazado por falta de determinismo.

## Consecuencias

- Los adapters mapearán value objects a tipos PostgreSQL nativos.
- Los tests pueden reproducir expiraciones y cambios de zona.
- Los conflictos tienen un contrato uniforme.
- Las carreras de seguridad necesitan SQL condicional y pruebas específicas, aunque el aggregate también tenga versión.

## Criterios de aceptación del ADR

- Las migraciones usan `uuid`, `timestamptz`, `date` y `bigint` según esta decisión.
- No aparecen timestamps globales como `LocalDateTime`.
- Las cuatro operaciones atómicas tienen prueba concurrente antes de exponer su endpoint o job.

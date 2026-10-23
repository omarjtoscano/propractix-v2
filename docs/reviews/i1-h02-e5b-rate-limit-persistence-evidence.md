# Evidencia E5B — Persistencia y operación del rate limiter

- Fecha: 2026-10-23.
- Estado de E5B: `ACCEPTED` por aprobación expresa del Product Owner el 2026-10-23.
- Commit funcional aprobado: `53d15ffb779d2b8e2a6680cba0388bddd3cebd30`.
- Estado de I1-H02: `IN_PROGRESS`; E5C y posteriores sin autorización.
- [PR #13](https://github.com/omarjtoscano/propractix-v2/pull/13): Draft y sin merge.
- Autorización: Product Owner, exclusivamente E5B, 2026-10-23.
- Base actualizada y creación de rama: `8f8f1b0bda8c9d066ba4751272939b4ae5cf6590`
  de `origin/increment/01-company-identity-catalog`.
- Rama: `feature/i1-h02-e5b-rate-limit-persistence`.
- E5A: integración confirmada expresamente por el Product Owner en ese SHA;
  la PR #12 consta integrada. Su aceptación funcional previa permanece sobre
  `6cc21e6c034b9c32db3f22ebb8b977f4250614cd`.
- `CL0_CLOUD_STAGING`: `PENDING`.

## Implementación revisable

V2 es la siguiente versión libre de Flyway. Añade exclusivamente
`platform_rate_limit_bucket`, con PK compuesta, contador positivo,
fingerprint base64url, expiración `timestamptz` e índice de limpieza. V1 conserva
sus bytes y checksum. Se verifican migración desde vacío, actualización V1→V2,
validación de checksums y repetición sin migraciones pendientes. La tabla es
técnica/global, propiedad exclusiva de `platform.ratelimit`, sin tenant ni FK
cross-context.

`RateLimitAdmissionService` valida la operación y obtiene todas las versiones
HMAC antes de acceder al puerto de almacenamiento. El adapter JDBC reclama en
una única sentencia todas las combinaciones de regla y versión, ordenadas por
clave. PostgreSQL calcula ventanas desde epoch UTC, TTL y techo de
`Retry-After`. Solo se confirma si todas las reglas/versiones admiten;
una denegación revierte también nuevas filas y contadores parciales.

Cada llamada usa una transacción JDBC independiente en `READ_COMMITTED`, con
`lock_timeout=1s` y `statement_timeout=2s`. Fallos SQL, de conexión, inicialización
transaccional o commit se convierten en `RateLimitFailure(STORAGE)` sin causa,
SQL ni parámetros. Una decisión incierta nunca se transforma en admisión.

La rotación conserva el contador previo al introducir otra versión, reclama
ambas y revierte todas ante un rechazo. Los tests usan claves/políticas
sintéticas; no modifican S0 ni habilitan una rotación operativa. Retirar claves
sigue exigiendo el solapamiento del runbook: TTL máximo más margen, 180 segundos,
desde el último proceso que solo utilizó la anterior. No hay backfill.

La expiración lógica no depende del borrado físico. El job deriva el intervalo
de un minuto y los umbrales de backlog directamente del YAML canónico. Borra
por lotes de hasta 1 000 con `SKIP LOCKED`, hasta 100 lotes por ciclo, sin tocar
filas activas. Un backlog anómalo emite `rate_limit_cleanup_incident` con solo
agregados; Operaciones abre la incidencia según el runbook. No hay integración
con servicios externos de incidencias.

`RateLimitHealthIndicator` comprueba tabla, columnas, permisos y backlog sin
consumir cuota. Readiness incluye `rateLimit`; liveness es independiente.
Health `DOWN` no contiene excepción ni detalles. El proceso CLI aislado usa
`management.endpoint.health.access=none` para no exigir los contributors del
servidor que no carga; la prueba de publicación administrativa sigue pasando.
No cambia el contrato ni la seguridad HTTP del servidor.

Micrometer registra exclusivamente las tres familias S0: admisiones, rechazos
por regla y errores con clase acotada. Las etiquetas se precrean desde la
política validada; una operación desconocida no genera cardinalidad nueva.
No se habilita un endpoint de métricas. Application continúa libre de frameworks,
y configuración conecta los puertos con los adapters.

## Pruebas y validación local del commit funcional aprobado

Los resultados siguientes corresponden a `53d15ffb779d2b8e2a6680cba0388bddd3cebd30`;
no son nuevas ejecuciones locales del cambio documental de aceptación.

Entorno: JDK 21 y Docker disponibles; PostgreSQL **16.15**, mediante
`postgres:16-alpine` en Testcontainers, sin H2 ni servicios AWS.

- `npm run infra:up`: PostgreSQL 16 y Mailpit locales saludables.
- `npm run server:test`: **209 pruebas, 0 fallos, 0 errores, 1 benchmark omitido**.
- `npm run server:verify`: **BUILD SUCCESS**, mismas 209 pruebas sin fallos ni
  errores y 1 benchmark omitido; JAR empaquetado. Inspección del JAR: V2
  incluida, S0 idéntica y ausencia de archivos/material HMAC sintético local.
- `scripts/check-migrations.sh`, `scripts/check-local-baseline.sh` y
  `infra/staging/scripts/deployment-gate-test.sh`: correctos, sin despliegue.
- `git diff --check`, diff completo y estado revisados antes del commit.

Las 17 pruebas de integración E5B cubren esquema/checksums, reloj PostgreSQL,
TTL canónico, primera denegación de cada cuota, reglas simultáneamente violadas,
rollback de filas nuevas, 48 peticiones concurrentes, rotación, versiones
incompletas o sobrantes, expiración lógica, limpieza idempotente, umbrales,
`SKIP LOCKED`, fallo SQL después de escrituras parciales, fallo de commit,
conexión/transacción, timeout de locks, tabla ausente y privacidad.

Las pruebas concurrentes y de agotamiento usan ventanas exclusivamente
sintéticas más largas para evitar flakiness por cruces de ventana; sus cuotas
proceden de S0. Otras pruebas verifican las ventanas y TTL canónicos directamente
con SQL y modelan sus bordes temporales mediante filas controladas, sin sleeps
largos ni reloj JVM. Tres pruebas de aplicación verifican operación/sujeto
inválidos, fallo de clave y fallo de almacenamiento sin admisión ni acceso
parcial. La integración Spring comprueba wiring y readiness/liveness reales.

Se inspeccionan columnas, filas, métricas, health y logs capturados: solo
fingerprints HMAC en la tabla; ninguna IP cruda/canónica, secreto o causa JDBC
sale por diagnósticos, health o métricas. No se registran fingerprints ni
versiones HMAC en logs ni labels. No se generan spans ni se exportan trazas.

La política canónica conserva SHA-256:
`25d742d159890951d6d8bbe09ba6f221e9a3750a41fbc99ed6cc77156e0c7600`.
El test del loader compara sus bytes empaquetados con el fichero fuente y
comprueba límites, TTL, intervalo y umbrales derivados de ese mismo fichero.

## Verificación remota y límites

Verificación del commit funcional aprobado `53d15ffb779d2b8e2a6680cba0388bddd3cebd30`:

- [CI #39](https://github.com/omarjtoscano/propractix-v2/actions/runs/39970611118):
  `completed / success`, cinco jobs correctos; 209 pruebas, 0 fallos, 0 errores
  y 1 benchmark omitido.
- [Trivy / dependency-and-filesystem-scan](https://github.com/omarjtoscano/propractix-v2/actions/runs/39970611118/job/120471971801):
  `success`, incluido `Scan dependencies and filesystem`.
- [Build OCI release candidate #22](https://github.com/omarjtoscano/propractix-v2/actions/runs/39970610896):
  `completed / success`; servidor y cliente AMD64/ARM64, manifiesto y artefactos.

El merge virtual `ed37aa7cadb19a72cc47f6df7d66aad4f32a6490` tenía el mismo árbol
que ese head: `364cf2c024236634e3fa29bcc6b3d178e59b7713`. El artefacto OCI
`12509331985` declara `workflow_run.head_sha=53d15ffb779d2b8e2a6680cba0388bddd3cebd30`.
No se integró ninguna rama.

Un commit incierto puede consumir cuota aunque la llamada falle cerrada;
no habilita bypass. Las filas bloqueadas o por encima del presupuesto de
limpieza se reevalúan en el ciclo siguiente, con señal de incidente cuando
superan los umbrales. La alerta es una señal local operable, no un ticket
externo creado automáticamente. La retirada prematura de una clave queda
prohibida por el procedimiento operativo; no se automatiza su aprobación.

E5C y posteriores siguen sin autorización: no hay filtro HTTP, cambios de
seguridad HTTP/Caddy, endpoints funcionales, OpenAPI, frontend, RUCT real ni AWS.
S0 permanece intacta y `CL0_CLOUD_STAGING` permanece `PENDING`.


## Registro documental de aceptación — 2026-10-23

El Product Owner aprueba expresamente E5B sobre el commit funcional indicado.
Este cambio registra exclusivamente estados, planes y evidencia; no modifica
código, infraestructura ni la política S0. E5B queda `ACCEPTED`; I1-H02 sigue
`IN_PROGRESS`, E5C y posteriores sin autorización y `CL0_CLOUD_STAGING`
`PENDING`. La PR #13 permanece Draft y sin merge.

Se revisan el diff completo, `git diff --check` y `git status` antes del commit.
No se atribuyen nuevas pruebas locales de servidor o cliente a este cambio.
El commit documental se publica en la misma rama; su SHA y la verificación
independiente de CI, Trivy y Build OCI se registran en el cuerpo de la PR #13
tras comprobarlas. Los resultados del commit funcional aprobado no sustituyen
la verificación del nuevo commit documental.

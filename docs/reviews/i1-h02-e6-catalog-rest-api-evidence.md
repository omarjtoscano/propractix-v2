# I1-H02 E6 — Contrato OpenAPI y API REST del catálogo

E6: `ACCEPTED` por aprobación expresa del Product Owner el 2026-10-25 sobre
`de269313888d912807536b5805e9e2c054955347`, incluida la corrección de privacidad de
`sourceUrl`. I1-H02 permanece `IN_PROGRESS`; E7 y posteriores no están
autorizadas y `CL0_CLOUD_STAGING` permanece `PENDING`. La PR
[#15](https://github.com/omarjtoscano/propractix-v2/pull/15) sigue Draft y sin merge.
La autorización original fue el 2026-10-23; las validaciones locales del
commit funcional se conservan a continuación como evidencia histórica.

## Base e integración

El Product Owner confirmó la integración de E5C mediante
`6f8c906448116d621763f8b2e7b7484b2273fb77` (PR #14). Se ejecutó fetch de
`increment/01-company-identity-catalog`, se comprobó que estaba actualizada
y se creó `feature/i1-h02-e6-catalog-rest-api` desde ese commit.
E5C conserva su aceptación sobre
`8a0a46a293b9b2d9482e92c05161f8b7b57b04e2`.

## Implementación

- Se definió y validó primero `docs/api/openapi.yaml`. Solo declara
  `GET /api/v1/academic-institutions`, `operationId: searchAcademicInstitutions`.
- El controller fino invoca el puerto `SearchPublishedInstitutions`, conectado
  al servicio y al adapter PostgreSQL existentes. No accede a persistencia.
- DTO y mapper REST devuelven exclusivamente `items[{id, officialName,
  countryCode}]`, `nextCursor` y `snapshot{retrievedAt, sourceUrl,
  sourceAttribution}`. El cursor termina como `null` y la página vacía conserva
  metadatos públicos. No se exponen referencias de origen, `sourceStatus`,
  hashes administrativos ni causas de fallo.
- `propractix.academic-institution.search` es configuración tipada y validada:
  `default-limit: 20`, `max-limit: 50`. Se rechazan valores ausentes,
  incoherentes, desconocidos o fuera de las invariantes de aplicación. La
  composición HTTP queda separada de los procesos administrativos de E4.
- Query admite hasta 300 puntos de código Unicode y rechaza controles,
  saltos de línea y sustitutos Unicode aislados. Query ausente, vacío o en
  blanco lista la primera página. Se reutiliza la normalización de aplicación.
  Limit debe ser un entero entre 1 y el máximo configurado. Los tres parámetros
  rechazan valores repetidos.
- Cursor base64url sin padding, con tamaño máximo de 4096 caracteres,
  estructura exacta versionada, UUID v4 y fingerprint de consulta. Se rechazan
  campos/tipos/versiones inválidos, JSON duplicado o con tokens posteriores y
  cursores de otra consulta. La aplicación y la persistencia existentes
  mantienen orden keyset y detección de cambio de snapshot.
- `PublicProblemDetails` centraliza el formato ya aceptado en E5C. Se conserva
  el filtro de correlación y el orden de filtros, con los mismos códigos y
  `Retry-After` del rate limiter. Los errores de catálogo usan códigos estables
  sin excepciones, SQL, parámetros ni contenido del cursor.

| HTTP | Código | Retry-After |
|---:|---|---|
| 400 | `validation_error`, con campo/código en `violations` | Ausente |
| 409 | `catalog_snapshot_changed` | Ausente |
| 429 | `rate_limited` | Segundos de la decisión de admisión |
| 503 | `rate_limiter_unavailable` | `1` |
| 503 | `academic_institution_catalog_unavailable` | Ausente |

Todas estas respuestas conservan `X-Correlation-ID`; los errores contienen
el mismo valor en `correlationId`.

## Pruebas y validación local inicial — `3e77e87`

Java 21, Maven Wrapper aprobado, PostgreSQL 16/Testcontainers y Caddy real
con la imagen fijada existente. Solo datos y secretos sintéticos.

- `npm run infra:up`: PostgreSQL 16 y Mailpit saludables.
- `npm run server:test`, tras compilación limpia: **278 pruebas, 0 fallos,
  0 errores y 1 benchmark BCrypt omitido**.
- `npm run server:verify`, sobre el estado final: **BUILD SUCCESS**, 278
  pruebas, 0 fallos, 0 errores y 1 benchmark omitido; empaquetado local correcto.
- `npm run openapi:lint` y `npm run openapi:bundle`: correctos.
- `npm run client:lint` y `npm run client:typecheck`: correctos; sin cambios
  de cliente ni generación de código.
- `check-migrations.sh`, `check-local-baseline.sh` y
  `deployment-gate-test.sh`: correctos; este último valida el contrato local
  de despliegue, sin llamadas ni mutaciones AWS.
- `git diff --check`: limpio; revisión del diff y del estado antes del commit.

Los 27 casos REST cubren límites configurados, paginación sin huecos ni IDs
duplicados, desempate por UUID, fin de página, normalización Unicode,
preservación del nombre oficial, contains y escape de comodines SQL,
parámetros inválidos/repetidos, cursor ligado a otra consulta, publicación
entre páginas, reinicio, catálogo ausente, fallo real de tabla, errores
inesperados, privacidad y correlación/MDC. Las respuestas se contrastan contra
los schemas, códigos, formatos y cabeceras declarados en OpenAPI usando el
vocabulario del contrato actual; no se introduce una librería de validación.

Los 26 casos de regresión E5C usan ahora el controller funcional y publicación
sintética a través del puerto administrativo aceptado. Conservan cuota única,
admisión única, 429/503 antes de controller y aplicación, fallo de almacenamiento,
health/readiness, confianza en proxies, spoofing, rutas y métodos denegados y
borde Caddy. Las entradas con cursores ficticios ahora reciben 400 después de
consumir la misma cuota de red. También se ejecutan las pruebas previas de
dominio, aplicación, importación, persistencia, rate limiting y ArchUnit.

## Corrección de privacidad de `sourceUrl` — 2026-10-25

El Product Owner autorizó corregir el hallazgo en la misma rama y Draft PR #15,
con commit/push y verificación independiente posterior de CI, Trivy y Build OCI.
En aquella corrección E6 e I1-H02 permanecían `IN_PROGRESS`, sin aceptación
ni merge; la aceptación posterior se registra al final de esta evidencia.

El mapper REST valida la URL antes de construir la proyección pública. Solo
admite HTTP(S), sin distinción de mayúsculas, con autoridad de servidor y host
válidos, puerto dentro del rango permitido y sin `userinfo`, tampoco vacío.
No resuelve DNS ni realiza peticiones a la URL. El contrato OpenAPI recoge la
restricción pública. No se modifica la admisión administrativa ni el valor
almacenado, sus hashes o el historial del snapshot.

Si los metadatos existentes son inseguros, la lectura devuelve
`503 academic_institution_catalog_unavailable`, incluso cuando la búsqueda no
tiene coincidencias. Se conserva `X-Correlation-ID` y el mismo `correlationId`
del Problem Detail, con violations vacías y sin `Retry-After`. La excepción del
mapper tiene mensaje fijo y ninguna causa ni valor de origen. El handler
existente registra únicamente su categoría fija de fallo. Una URL almacenada
que ni siquiera es parseable también utiliza el fallo sanitizado del adapter
de lectura existente.

Las pruebas nuevas cubren cuatro URLs públicas válidas (HTTP, HTTPS, puerto y
IPv6), trece URLs inseguras en el mapper y esos casos más una URI almacenada
malformada en REST. Incluyen usuario/contraseña sintéticos, usuario solo,
userinfo vacío, esquemas no admitidos, URI opaca, autoridad/host ausentes o
inválidos y puertos inválidos. REST comprueba respuesta, cabeceras, logs y
excepciones resueltas sin la URL ni los valores sintéticos; verifica el contrato
de errores, correlación y limpieza de MDC. También compara la fila completa
del snapshot antes y después para conservar su trazabilidad administrativa.

Validación local de la corrección:

- `npm run infra:up`: PostgreSQL 16 y Mailpit saludables.
- `npm run server:test`, desde compilación limpia, y `npm run server:verify`:
  **313 pruebas, 0 fallos, 0 errores y 1 benchmark omitido**, BUILD SUCCESS
  y empaquetado local correcto. Incluyen 45 casos REST, 29 del mapper y
  los 26 de regresión HTTP/Caddy de E5C.
- `npm run openapi:lint` y `npm run openapi:bundle`: correctos.
- `check-migrations.sh`, `check-local-baseline.sh`, `deployment-gate-test.sh`
  y `git diff --check`: correctos. Diff y estado revisados antes del commit.

Solo cambian el contrato, el mapper REST, sus pruebas y esta evidencia. S0,
migraciones, dominio, persistencia y metadatos administrativos no se modifican.
CI, Trivy y Build OCI sobre el nuevo SHA quedan pendientes de verificación del
Product Owner; el agente no espera ni supervisa sus ejecuciones.

## Alcance conservado y entrega

La política S0, su decisión de cierre y sus límites no cambian. V1 y V2
conservan sus bytes. No se añaden otras rutas, dependencias ni funcionalidad
de cliente/UI; no se adquiere RUCT real ni se ejecuta AWS.
`CL0_CLOUD_STAGING` permanece `PENDING`.

El SHA final y la Draft PR hacia `increment/01-company-identity-catalog` se
entregan en la respuesta y en el cuerpo de la PR. **CI, Trivy y Build OCI
quedan pendientes de verificación del Product Owner** sobre ese SHA. El agente
no espera ni supervisa esas ejecuciones. E6 está `ACCEPTED`; I1-H02 sigue
`IN_PROGRESS`, sin autorización de merge.

## Registro documental de aceptación — 2026-10-25

El Product Owner aprueba expresamente E6 implementada en
`de269313888d912807536b5805e9e2c054955347`. La aceptación corresponde al commit funcional,
incluida la protección de `sourceUrl`; no cierra I1-H02 ni habilita E7.

Este registro modifica únicamente `AGENTS.md`, los planes de incremento y
ejecución H02 y esta evidencia. Se actualiza también el cuerpo de la PR #15.
Se revisan el diff completo, `git diff --check` y `git status` antes del commit;
no se modifica código ni infraestructura y no se realiza merge. No se ejecutan
nuevas pruebas de aplicación para este cambio documental ni se atribuyen a él
las validaciones históricas del commit funcional.

El SHA documental publicado se entrega en la respuesta y en el cuerpo de la
PR #15. CI, Trivy y Build OCI sobre ese nuevo SHA quedan **pendientes de
verificación del Product Owner**, que informará del resultado. El agente no
espera ni supervisa esas ejecuciones. La PR permanece Draft; I1-H02
`IN_PROGRESS`, E7 y posteriores sin autorización y `CL0_CLOUD_STAGING` `PENDING`.

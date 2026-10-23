# Evidencia E5C — Filtro HTTP, seguridad y borde Caddy

- Fecha: 2026-10-23.
- E5C e I1-H02: `IN_PROGRESS`; sin aceptación ni merge de E5C.
- Autorización expresa y exclusiva: Product Owner, 2026-10-23.
- Integración de E5B confirmada por el Product Owner:
  `7bd7bf6b4191d92cfe39864333ce5b5c3467115a` ([PR #13](https://github.com/omarjtoscano/propractix-v2/pull/13)).
- Base: `origin/increment/01-company-identity-catalog`, actualizada en ese SHA.
- Rama: `feature/i1-h02-e5c-http-rate-limit`.
- H01, F0, C0 y S0 cerrados; E1 a E5B aceptadas.
- `CL0_CLOUD_STAGING`: `PENDING`, sin AWS.

## Implementación revisable

`HttpRateLimitFilter` se registra una sola vez mediante
`FilterRegistrationBean`, exclusivamente para dispatch `REQUEST`, con orden
`HIGHEST_PRECEDENCE + 1`: después de correlación y antes de Spring Security.
No es un componente autoinscrito ni se añade a la cadena de seguridad.
Las pruebas con MockMvc y Tomcat real verifican una sola llamada de admisión
y una única ejecución del controlador por petición admitida.

Filtro y seguridad comparten `RateLimitedHttpOperation`, construido desde la
operación de S0 validada: únicamente GET y la URI exacta
`/api/v1/academic-institutions`. Queries y cursores no intervienen en el sujeto.
La comparación exacta compartida impide autorizar variantes codificadas o
rutas alternativas que no hubieran pasado por admisión. Otros métodos/rutas
siguen denegados; health conserva su autorización previa.

No se añade controlador funcional del catálogo: solo existe una sonda en
`src/test`, importada expresamente y marcada `TestComponent`. E6 conserva la
propiedad del endpoint funcional, DTO y contrato REST/OpenAPI.

Los rechazos escriben directamente `application/problem+json`, con `type`,
`title`, `status`, `detail`, `instance`, `code`, `correlationId` y `violations`.
429 usa `rate_limited` y el Retry-After de la decisión de admisión; 503 usa
`rate_limiter_unavailable` y Retry-After 1. Ambos conservan exactamente el
X-Correlation-ID ya resuelto por `CorrelationIdFilter`. No usan `sendError`,
no provocan redispatch ni ejecutan controlador. `instance` es la ruta fija;
nunca contiene query/cursor. Excepciones de resolución, admisión o providers
no exponen mensajes, causas, stack traces, IP ni fingerprints. El límite de
captura termina antes de ejecutar la cadena downstream.

La configuración común conserva `REMOTE_ADDRESS`, sin CIDR ni confianza proxy
activada. `X_FORWARDED_FOR` sigue requiriendo simultáneamente CIDR explícitos y
saneamiento configurado/probado. Un peer no confiable no puede aportar su IP
mediante headers. Desde un proxy confiable, se validan las cabeceras completas
y se recorre la cadena de derecha a izquierda, tomando el primer salto no
confiable. Cabeceras ausentes, duplicadas, malformadas, conflictivas,
demasiado largas o sin cliente resoluble producen 503 antes de admisión.

`server.forward-headers-strategy=none` conserva el peer del socket. La
configuración HTTP rechaza al arranque `NATIVE` y `FRAMEWORK`, para impedir que
otro procesador reescriba la identidad de red antes del resolver validado.
Las pruebas de E5A siguen verificando ausencia de CIDR/saneamiento, ambos modos
simultáneos, CIDR universales, normalización y secretos inválidos.

Caddy elimina Forwarded y X-Forwarded-For recibidos, y reconstruye únicamente
X-Forwarded-For desde `{remote_host}`. El mismo fragmento se aplica a API y
readyz; healthz conserva su respuesta local. No se confía en cabeceras de un
proxy exterior por defecto. La prueba ejecuta la misma imagen fijada en
`client/Dockerfile` y el Caddyfile real, sustituyendo solo el upstream por el
Tomcat efímero de pruebas.

## Privacidad y diagnósticos

Una prueba detectó que DEBUG del entorno podía activar logging web con la
query completa incluso con parámetros marcados como masked. La configuración
común fija INFO para web, seguridad y JDBC: las pruebas verifican ausencia de
los marcadores sintéticos de búsqueda/cursor, IP, Authorization, Cookie,
secreto y mensajes de providers. No se registran peticiones desde el filtro.

Caddy elimina el objeto `request` y campos de petición del nivel superior en
sus logs de ejecución, también ante fallo de transporte. Se provoca una
conexión fallida a un upstream de prueba: conserva la señal `http.log.error`
y 502 sin registrar la query, headers o dirección del cliente. Este 502 de
transporte pertenece al proxy; los 429/503 del rate limiter llegan desde el
servidor con su Problem Detail y correlación.

Los errores de resolución incrementan únicamente la clase acotada `client_ip`
de la familia métrica S0 existente. Los errores tipados de admisión se cuentan
en el decorator de E5B; errores inesperados se cierran con clase `storage`.
No se añaden endpoints de métricas ni labels derivados de la petición.

## Validación local

Entorno: JDK 21.0.12.1 y Docker disponibles, PostgreSQL 16 mediante
Testcontainers y Caddy 2.10.2 con el digest fijado en el Dockerfile.

- `npm run infra:up`: PostgreSQL 16 y Mailpit locales saludables.
- `npm run server:test`: 238 pruebas, 0 fallos, 0 errores y 1 benchmark omitido.
- `npm run check`: correcto; incluye configuración Compose, baseline local,
  contrato del gate de despliegue sin desplegar, OpenAPI lint, `server:verify`,
  lint, 45 pruebas de cliente y build del cliente. `server:verify` termina
  BUILD SUCCESS con las mismas 238 pruebas sin fallos/errores y el benchmark
  previsto omitido; JAR empaquetado.
- `npm run client:typecheck`: correcto.
- `scripts/check-migrations.sh`: correcto; V1 y V2 sin cambios.
- Imagen Caddy fijada: configuración real ejecutada y formateada con `caddy fmt`.
- Inspección del JAR: filtro HTTP incluido; sonda/controladores de prueba y
  archivos de secretos ausentes; migraciones y S0 idénticos a sus fuentes.
- S0 conserva SHA-256
  `25d742d159890951d6d8bbe09ba6f221e9a3750a41fbc99ed6cc77156e0c7600`.
- `git diff --check`, diff completo y `git status`: revisados antes del commit.

Las 29 pruebas nuevas comprenden 26 de integración HTTP y 3 de configuración.
Usan Tomcat real, MockMvc, PostgreSQL 16, Caddy real y decisiones de admisión
controladas para rechazos deterministas. Cubren una única ejecución, método y
ruta exactos, variantes codificadas, cuota compartida entre queries/cursores,
ambas familias falsificadas desde peer no confiable, cadena multiproxy,
cabeceras confiables inválidas, 429/503, fallo real de tabla, mensajes de
provider/timeout sensibles, correlación válida/generada, limpieza de MDC,
health independiente y rechazo de forwarding nativo/framework paralelo.

La verificación remota sobre el SHA final, sus enlaces y el estado Draft de la
PR se registran en el cuerpo de la PR después de completar CI, Trivy y Build
OCI; este registro local no sustituye esa comprobación.

## Límites y estado

S0 canónica y sus límites/TTL no se modifican. Las migraciones V1 y V2
conservan sus bytes. OpenAPI y frontend no cambian, salvo el Caddyfile de borde
expresamente autorizado; no se obtiene RUCT real ni se ejecuta AWS.

Las cuotas, atomicidad, concurrencia y recuperación de ventanas PostgreSQL
siguen cubiertas por E5B. E5C usa decisiones controladas para verificar los
429/Retry-After de forma determinista, además de admisión y fallo de tabla
reales en PostgreSQL. Las cadenas multiproxy se prueban en el adapter HTTP;
la prueba de borde usa un Caddy real. Un proxy intermedio operativo adicional
requiere evidencia de su propio comportamiento de append antes de confiar en
su CIDR. No se activa confianza proxy en ningún perfil operativo.

E5C e I1-H02 permanecen `IN_PROGRESS`. E6 y posteriores no están autorizadas.
No se marca `ACCEPTED`, no se integra ninguna rama y `CL0_CLOUD_STAGING`
permanece `PENDING`.

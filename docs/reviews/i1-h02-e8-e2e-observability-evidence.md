# I1-H02 E8 — E2E y observabilidad operable

- Implementación inicial: 2026-10-26; cierre de la entrega técnica: 2026-11-01.
- Estado: **E8 e I1-H02 `IN_PROGRESS`**, pendientes de aceptación expresa del Product Owner.
- Rama: `feature/i1-h02-e8-e2e-observability`.
- Base e integración de E7 confirmada por el Product Owner el 2026-10-26:
  `63034a15c6f3013ac6e826a3171234eef32b7740` (PR #16). E7 permanece `ACCEPTED`.
- Destino: Draft PR hacia `increment/01-company-identity-catalog`.
- El SHA de entrega y la URL de la Draft PR se comunican en la entrega y en su
  cuerpo, sin sustituir el commit funcional aprobado de E7.

## Alcance y autorización

El Product Owner autorizó exclusivamente E8 el 2026-10-26 conforme a los apartados
15 y 16.7 del plan H02, incluidos validación, evidencia, commit, push y Draft PR.
Pidió finalizar la tarea el 2026-11-01. No se acepta E8 ni I1-H02, no se hace merge,
no se inicia E9, no se publica RUCT real y no se ejecuta AWS.
`CL0_CLOUD_STAGING` permanece `PENDING`; C0 y S0 permanecen `CLOSED`.

## Implementación

Playwright 1.63.0 queda fijado en `client/package.json` y el lockfile raíz.
`client/playwright.config.ts` ejecuta Chromium con un worker, cero reintentos y
límites de tiempo. Vitest limita su descubrimiento a las pruebas unitarias de
`src`; no intenta ejecutar la suite Playwright en jsdom.

`npm run client:e2e` compila el backend y el frontend y crea un proyecto Compose
local desechable. Usa PostgreSQL 16 vacío, Flyway, validación JPA, Spring Boot
real, Caddy 2.10.2 con el Caddyfile de producto y el frontend compilado. Solo
Caddy publica un puerto efímero en loopback. La confianza del backend se limita
al `/32` observado de ese Caddy; no cambia el límite S0 ni introduce un bypass.
Los secretos son sintéticos, aleatorios, montados mediante config tree y se
eliminan al terminar. El lock evita dos ejecuciones sobre los mismos artefactos.
La limpieza elimina exclusivamente el proyecto y volumen E8, tanto tras éxito
como tras error y señales SIGINT/SIGTERM.

Las fixtures v1 y v2 tienen 45 y 46 instituciones completamente inventadas,
`sourceSystem=SYNTHETIC`, fechas y atribución explícitas y URLs `example.invalid`.
Sus manifests JSON fijan SHA-256 de los bytes exactos. El orquestador verifica
cada hash y publica mediante `CatalogImportPublicationApplication`, que utiliza
el puerto administrativo aceptado en E4. No inserta instituciones mediante SQL.

La observabilidad vive en adapters del contexto `academicinstitution`, sin
modificar dominio ni casos de uso:

- `MeteredInstitutionSearch` mide duración con resultados acotados `found`,
  `snapshot_changed`, `unavailable` y `failed`. No retiene consulta, cursor ni
  excepciones en etiquetas; una admisión S0 rechazada no ejecuta este timer.
- `PostgresCatalogSnapshotAge` mide la edad desde `retrieved_at` del snapshot
  publicado, consultando PostgreSQL al leer el gauge. No mantiene una versión
  cacheada. Ausencia o fallo devuelve `NaN`; la republicación no rejuvenece datos.
- `CatalogPublicationMetrics` cuenta intentos administrativos correctos/fallidos.
  La CLI emite además un evento JSON acotado en stderr para conservar la señal
  cuando termina su MeterRegistry. Stdout y exit codes de E4 mantienen su contrato.
- Las métricas se consultan por JMX local del mismo proceso/usuario, sin JMX
  remoto ni nuevos endpoints HTTP. Actuator HTTP conserva únicamente health.

El [runbook S0](../operations/rate-limiting-runbook.md) conserva los límites,
ventanas, TTL, taxonomía de errores, etiquetas y umbrales aprobados. Añade la
lectura local y la interpretación de las tres señales del catálogo, distinguiendo
indisponibilidad de búsqueda de indisponibilidad del rate limiter. La
[guía E2E local](../operations/local-catalog-e2e.md) documenta ejecución,
requisitos, aislamiento, evidencia y recuperación si una interrupción impide
limpiar el entorno.

## Cobertura E2E del apartado 16.7

| Escenario | Evidencia automatizada |
|---|---|
| Publicación administrativa sintética | CSV y manifest verificados; CLI real sobre PostgreSQL 16; publicación inicial y de nueva versión. |
| Búsqueda con/sin tildes y UUID | ES/EN: `Álamo` y `alamo` devuelven los mismos IDs; UUID v4 interno en la opción ARIA y selección con flechas/Intro. |
| Paginación sin duplicados | 20, 40 y 45 opciones; todos los UUID distintos; fin de cursor. Carga con puntero y teclado. |
| Cambio de snapshot | Publicación v2 por CLI; cursor anterior devuelve 409 y la UI reinicia desde la primera página sin mezclar versiones. |
| Entrada manual | Foco y entrada por teclado, ningún request de escritura, sin cambios en instituciones/importaciones ni persistencia tras recarga. |
| 429 y recuperación | Quince peticiones reales; siguiente petición rechazada; botón inhabilitado y cero requests durante el backoff; recuperación tras `Retry-After`. |
| Cabeceras falsificadas | Rotación de ambas cabeceras a través de Caddy; la decimosexta petición sigue siendo 429 y solo existe un bucket burst activo. |
| Adapter indisponible | Renombrado temporal de tablas solo en la base desechable, restaurado en `finally`: 503 de lectura con error EN y sin resultados; 503 S0 con `Retry-After: 1`; recuperación posterior. |
| Ausencia de cuentas/tenants | Solo existen las tablas de catálogo, Flyway y rate limiting; las acciones de visitante no cambian el catálogo ni crean usuario, cuenta, tenant o workspace. |
| Observabilidad y privacidad | Lectura JMX real de S0/timer/gauge; eventos CLI de éxito y fallo; centinelas de query, cursor real, User-Agent, cabeceras, nombre manual, IP/canonicalización y fingerprints calculados localmente ausentes de logs y métricas. |

Las pruebas Java adicionales comprueban los nombres y etiquetas exactos de S0,
incrementos de admisión/rechazo/fallo cerrado y exclusión de búsquedas rechazadas;
la edad ante ausencia, republicación y error de almacenamiento; duración de
conflictos, indisponibilidad y excepciones con reloj controlado; y contadores de
publicación sin etiquetas de cardinalidad abierta. ArchUnit mantiene los límites.

El navegador real detectó dos problemas de foco no reproducidos por jsdom:
el botón enfocado podía cerrar la lista al deshabilitarse durante la carga, y el
listbox desplazable introducía una parada de Tab. Las acciones de carga/reintento
restauran primero el foco al combobox y el listbox usa `tabIndex=-1`, conforme al
patrón con `aria-activedescendant`. La prueba existente de selección conserva sus
comprobaciones de UUID, cierre y ausencia de requests adicionales, actualizando
el foco esperado tras paginar; Playwright comprueba el recorrido real completo.

## Validación local final

Los resultados definitivos se registran en
[i1-h02-e8-local-validation.json](i1-h02-e8-local-validation.json).
Los logs completos permanecen locales e ignorados por Git; no se adjuntan a esta
evidencia secretos, IP, fingerprints, contenido CSV ni trazas de navegación.

| Validación | Resultado |
|---|---|
| `npm run infra:up` | PostgreSQL 16 y Mailpit saludables. |
| `npm run server:test` | 317 pruebas, 0 fallos, 0 errores, 1 benchmark omitido. |
| `npm run server:verify` | Mismo resultado; ArchUnit, Flyway/PostgreSQL y contratos correctos. |
| `client:lint`, `client:typecheck`, `client:test` | Controles i18n y tipos correctos; 94 pruebas en 9 archivos, todas correctas; checks de scripts correctos. |
| `client:build` | TypeScript 7.0.2 y Vite correctos; este build alimenta el Caddy real de E2E. |
| `client:check-generated`, `openapi:lint` | Dos generaciones reproducibles, 16 archivos iguales; OpenAPI válido. |
| `npm run client:e2e` | 8/8 en Chromium, 0 omitidas, 0 reintentos, 0 flaky; 49,4 segundos de suite. |
| Métricas reales por JMX | S0: 63 admitidas, 3 rechazos burst, 2 errores de adapter; 63 búsquedas medidas; edad de snapshot finita. Instantánea agregada en el JSON enlazado. |
| Migraciones, baseline y guards de despliegue | Scripts locales correctos; sin ejecución AWS. SHA-256 de ambas fixtures correcto. |
| Instalación | `npm ci` con npm 11.9.0 verifica ambos árboles y lockfiles; las versiones previas no cambian. |
| Revisión de diff y limpieza | Sin errores de whitespace; contratos conservados; contenedores/volumen/red E8 y secretos/lock eliminados. |

Sin diff en la política S0, OpenAPI, migraciones aplicadas, cliente generado,
paquete/lockfile del generador ni ADR-001. La aplicación resuelve TypeScript
7.0.2 y el generador aislado 6.0.3 con @hey-api/openapi-ts 0.99.0, conservando la
excepción F0. La doble generación compara los 16 archivos versionados.

## SonarQube, dependencias y límites

Se han considerado los hallazgos locales disponibles y su
[revisión individual de E7](i1-h02-e7-sonarqube-review.md). La justificación de
S1082 sigue el patrón de combobox, ahora comprobado también en Chromium. Se
conservan los hallazgos del generador y el previo de E6; no se edita código
regenerado ni el codec para aparentar un resultado verde. No se instala un
analizador, no se desactivan reglas ni se añade un gate. Esta entrega no afirma
un nuevo análisis de SonarQube ni un Quality Gate remoto verde.

`npm audit` devuelve cinco avisos (tres high y dos moderate) en nodos ya existentes
en la baseline: source-map-js, undici, vite, vitest y @vitest/mocker. Los paquetes
Playwright añadidos no figuran en ese listado; el lockfile no modifica las
versiones anteriores. Se documentan para revisión del propietario, sin actualizar
F0 ni aplicar `audit fix` de forma unilateral y sin convertir el audit en un gate.

La inspección de privacidad cubre las señales efectivamente emitidas por esta
baseline. No se graban trazas Playwright, vídeo ni capturas; la aplicación no
configura un exportador de trazas distribuido. Esto no certifica un futuro
exportador externo ni un análisis con lector de pantalla real. Los contadores
CLI terminan con el proceso: stderr debe conservarse/agregarse como describe el
runbook. El perfil E2E usa un JDK para el attach local; no cambia las imágenes de
producto ni su despliegue.

CI, Trivy y Build OCI del SHA de entrega quedan pendientes de verificación del
Product Owner, sin espera ni supervisión por el agente. La validación local y la
Draft PR no equivalen a aceptación de E8 ni de I1-H02.

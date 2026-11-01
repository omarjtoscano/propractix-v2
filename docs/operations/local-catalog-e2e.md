# E2E local del catálogo — I1-H02 E8

E8 `ACCEPTED` por aprobación expresa del Product Owner el 2026-11-01 sobre
`fc9660e997df990d08677f6a55d91d57f92ef9c7`; I1-H02 permanece `IN_PROGRESS`. E7 `ACCEPTED` e integrada mediante
`63034a15c6f3013ac6e826a3171234eef32b7740`, confirmado por el Product Owner
el 2026-10-26. `S0_PUBLIC_ENDPOINTS` `CLOSED`; `CL0_CLOUD_STAGING` `PENDING`.
Este procedimiento no usa AWS ni publica RUCT real.

## Ejecución reproducible

Requisitos: JDK 21, Maven Wrapper 3.9.16, Node 24, npm 11.9.0, Docker Compose v2
y dependencias de sistema de Chromium. Desde la raíz:

```bash
npx --yes npm@11.9.0 ci
npx playwright install chromium
npx --yes npm@11.9.0 run client:e2e
```

La instalación usa Playwright **1.63.0** fijado en el lockfile, sin instalarlo
globalmente. La [configuración oficial](https://playwright.dev/docs/test-configuration)
documenta las opciones del runner. La aplicación conserva TypeScript 7.0.2 y el
generador conserva su árbol aislado con TypeScript 6.0.3, conforme a ADR-001.

El comando compila el backend y el frontend, crea un proyecto Compose
`propractix-e8-<pid>` con volumen PostgreSQL 16 vacío, ejecuta Flyway y valida JPA.
Arranca el backend real y Caddy 2.10.2 con el **Caddyfile de producto** y el frontend
compilado. Las imágenes están fijadas por digest. Solo Caddy publica un puerto
efímero en `127.0.0.1`; PostgreSQL, backend y JMX no publican puertos. No utiliza
el volumen, los secretos ni los puertos del perfil local habitual.

Los secretos sintéticos aleatorios se montan como archivos de config tree bajo
`.local/e2e/secrets`, con permisos restrictivos, y se eliminan al terminar. El
orquestador obtiene el peer de Caddy mediante Docker y configura exclusivamente
su `/32` como proxy confiable; ambas cabeceras del cliente se sanean antes de
reconstruir `X-Forwarded-For`. No hay un bypass de cuota ni una política de pruebas.

Las fixtures versionadas `academic-institutions-e8-synthetic-v1/v2.csv` tienen
45/46 filas sintéticas y manifests JSON con SHA-256 de bytes exactos, atribución,
URL `example.invalid` y fecha de consulta. Cada publicación comprueba el hash y
ejecuta `CatalogImportPublicationApplication`, que usa el puerto administrativo
aceptado en E4. No se insertan instituciones mediante SQL ni endpoints de prueba.

La suite ejecuta un worker, sin reintentos automáticos, con límites de tiempo.
Comprueba ES/EN, teclado, UUID, búsqueda con/sin tildes, todas las páginas sin
duplicados, cambio real de snapshot y reinicio, entrada manual efímera, ausencia
de escrituras/cuentas/tenants/workspaces, 429 y recuperación tras `Retry-After`,
spoofing y 503 de ambos adapters con recuperación. Para provocar fallos se
renombran temporalmente tablas **solo en la base desechable**, restaurándolas en
`finally`. El runner no modifica los datos de catálogo fuera de la CLI.

Se vacían los buckets entre escenarios independientes; durante las pruebas de
cuota y recuperación se usan exclusivamente las ventanas y límites S0 reales.
La paginación cubre puntero y teclado. La suite descubre y cubre también la
pérdida de foco de Chromium al deshabilitar/eliminar el botón de carga.

## Descubrimiento en VS Code

VS Code puede cargar `client/playwright.config.ts` y descubrir los ocho
escenarios sin variables E2E. El equivalente desde la raíz es:

```bash
node node_modules/playwright/cli.js test -c client/playwright.config.ts --list
```

El descubrimiento no arranca servicios ni ejecuta SQL. La configuración no lanza
excepciones al importarse ni proporciona una URL por defecto. `globalSetup`
valida la URL suministrada por el orquestador y el identificador del proyecto
`propractix-e8-<pid>` únicamente al iniciar la ejecución, antes de los escenarios
y sus hooks. Una ejecución directa sin ese entorno falla con un mensaje que
indica `npm run client:e2e`; no se omiten escenarios para evitar el error.

La ejecución completa sigue realizándose desde la terminal con
`npm run client:e2e`, que prepara el entorno desechable y lo limpia al terminar.
El botón de ejecución directa de VS Code no sustituye al orquestador. Se
conservan el lock, los secretos sintéticos temporales, los controles de proyecto
en cada operación Compose/SQL y el aislamiento de la base de datos.

## Evidencia y limpieza

`.local/e2e/results.json`, `stack.log` y `publications.log` conservan evidencia
local ignorada por Git. Un error puede producir contexto DOM sintético del test.
No se graban trazas de navegador, vídeo ni capturas, que podrían retener términos,
cursores y entradas manuales. Los centinelas verifican que los logs de aplicación,
Caddy/PostgreSQL, señales de publicación y métricas no contienen valores de
cliente, nombres manuales ni material secreto. La baseline no tiene exportador
de trazas distribuido ni un destino externo de telemetría.

Las métricas se leen mediante attach JMX local desde el mismo contenedor JDK,
usando `infra/e2e/ReadMetric.java`, sin listener remoto. La prueba Java verifica
además nombres, etiquetas exactas, contadores y duración. Véase el
[runbook S0](rate-limiting-runbook.md) para interpretar las señales.

La limpieza automática guarda logs y ejecuta `down --volumes --remove-orphans`
para ese proyecto tanto en éxito como en fallo y señales SIGINT/SIGTERM. El lock
`.local/e2e/run.lock` impide ejecuciones simultáneas que compartan los artefactos.
SIGKILL o pérdida de Docker pueden impedir la limpieza. En ese caso, identificar
el proyecto `propractix-e8-<pid>` de la salida y, tras confirmar que su proceso ya
no corre, ejecutar `docker compose -p <proyecto> -f infra/e2e/compose.yaml down
--volumes --remove-orphans`; eliminar después solo `.local/e2e/run.lock` y
`.local/e2e/secrets`. No eliminar volúmenes o contenedores del perfil local normal.

CI, Trivy y Build OCI están en verde sobre el SHA funcional aprobado de E8,
confirmados expresamente por el Product Owner el 2026-11-01. Los checks del nuevo
commit documental quedan pendientes de su verificación, sin espera ni supervisión
por el agente. La aceptación expresa de E8 no cerró I1-H02 ni autorizó merge. Posteriormente la
[PR #17](https://github.com/omarjtoscano/propractix-v2/pull/17) quedó integrada mediante
`866c4c71511c181564b8f8d165c06759fcb4442b`. E9 solo prepara RUCT para segunda revisión
según su [evidencia](../reviews/i1-h02-first-snapshot-evidence.md); este procedimiento
E2E conserva sus fixtures sintéticas y no publica el snapshot real.

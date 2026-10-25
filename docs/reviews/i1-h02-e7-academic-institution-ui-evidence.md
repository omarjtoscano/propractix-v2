# I1-H02 E7 — Cliente generado y consulta académica en UI

- Fecha: 2026-10-25.
- Estado: E7 e I1-H02 `IN_PROGRESS`; pendiente de revisión del Product Owner.
- Rama: `feature/i1-h02-e7-academic-institution-ui`.
- Base: incremento actualizado en `11dda0c9fb267b619c98baabd12465e143c43d53`.
- El SHA de entrega es el commit que incorpora esta evidencia, comunicado en la
  entrega y en la Draft PR hacia `increment/01-company-identity-catalog`.

## Autorizaciones y límites

El Product Owner confirmó la integración de E6 mediante
`11dda0c9fb267b619c98baabd12465e143c43d53` (PR #15) y autorizó exclusivamente
E7 el 2026-10-25, incluidos commit, push y Draft PR. Posteriormente autorizó
la excepción de compatibilidad F0: TypeScript 7.0.2 para la aplicación,
@hey-api/openapi-ts 0.99.0 y TypeScript 6.0.3 exclusivamente para el generador
con aislamiento efectivo. Se registra explícitamente en
[ADR-001](../adr/ADR-001-monolito-modular.md), con referencia al
[issue oficial #4235](https://github.com/hey-api/hey-api/issues/4235).

No hay cambios de backend, contrato OpenAPI aceptado, S0, migraciones, RUCT
real ni AWS. `CL0_CLOUD_STAGING` permanece `PENDING`. E8, Playwright, la
orquestación E2E y entregas posteriores no están implementadas ni autorizadas.
La PR permanece Draft; esta entrega no acepta E7 ni H02 y no hace merge.
CI, Trivy y Build OCI del SHA final quedan **pendientes de verificación del
Product Owner**; el agente no espera ni supervisa esas ejecuciones.

## Implementación

- `tools/openapi-generator`: paquete independiente fuera de workspaces, con
  lockfile propio y dependencias exactas. El `postinstall` raíz ejecuta su
  `npm ci --ignore-scripts`, usando el mismo CLI npm que instala la raíz.
  Docker y CI instalan ambos árboles; los `node_modules` están excluidos del
  repositorio y del contexto Docker.
- Cliente Fetch generado en `client/src/shared/api/generated` desde el
  OpenAPI aceptado. Ningún archivo generado ni `node_modules` se ha editado
  manualmente. `client:check-generated` compara contenido y nombres de los
  16 archivos tras dos generaciones completas y se ejecuta también en CI.
- Feature `academic-institutions`: adapter sobre el SDK generado y
  `useInfiniteQuery` de React Query 5.102.8. No hay `fetch` en componentes,
  rutas adicionales, formularios futuros ni duplicación del estado remoto
  en Zustand. La configuración tipada valida debounce, página 20/máximo 50 y
  espera de respaldo para errores sin Retry-After válido.
- Componente reusable que emite `{ kind: 'catalog', id, officialName }`, con
  UUID interno, o `{ kind: 'manual', name }`. La pantalla de App conserva la
  selección solo en memoria; no hay llamadas de escritura ni almacenamiento
  del nombre manual. La preferencia de idioma existente conserva su contrato.
- Estados inicial, carga, resultados, vacío, error, indisponibilidad, límite
  de peticiones y reintento. La paginación usa exclusivamente el cursor del
  servidor. Una query nueva oculta inmediatamente páginas anteriores, cancela
  la petición anterior y descarta respuestas tardías aun si el transporte
  ignora AbortSignal.
- `catalog_snapshot_changed` oculta todas las páginas y reinicia desde la
  primera, sin cursor. Se permite un reinicio automático por búsqueda; un
  conflicto reiterado requiere reintento explícito. También se descartan
  páginas cuyas atribuciones/fechas/fuentes difieran. El servidor sigue siendo
  autoridad del snapshot; no se intenta interpretar el cursor.
- Retry-After se aplica al conjunto de la operación durante la sesión,
  incluidas nuevas consultas y remontajes. No hay reintento automático ante
  429/503, refoco o reconexión. El botón se deshabilita hasta la fecha límite;
  después requiere una acción explícita. La espera de respaldo procede de
  configuración y evita consultas agresivas con cabeceras ausentes/inválidas.
- Atribución exacta, fuente HTTP(S) sin credenciales y fecha UTC del snapshot,
  aviso explícito de datos sintéticos y alcance informativo: no acredita
  afiliación, matrícula, elegibilidad, convenio ni cumplimiento. Las URLs y
  los datos públicos se validan antes de usarlos en enlaces o IDs ARIA.
- Teclado con flechas, Intro, Escape y Tab; foco conservado en el combobox,
  foco dirigido al campo manual, label, ayuda, listbox, opciones,
  `aria-expanded`, `aria-controls`, `aria-activedescendant`, `aria-selected`,
  `aria-busy` y anuncios de resultados mediante `role=status` y `aria-live`.
- Todos los textos y plurales de presentación en ES/EN. Se reutilizan
  selector y `ErrorMessage`; solo se transmite el código público. Los códigos
  desconocidos usan el error genérico, sin mostrar title, detail, prosa de
  violations ni mensajes de excepciones. Nombres y atribución son datos.

## Verificación de versiones e instalación reproducible

Entorno: Node 24.12.0, npm 11.9.0 para las instalaciones (CLI instalado en la
caché local; el npm global del host es 11.7.0), OpenJDK 21.0.12.1 y Docker.

`node tools/openapi-generator/verify-versions.mjs` resuelve la entrada ESM
real de Hey API y desde allí su TypeScript, frente al paquete de la aplicación:

```text
generator: @hey-api/openapi-ts 0.99.0 -> TypeScript 6.0.3
  tools/openapi-generator/node_modules/typescript/package.json
application: TypeScript 7.0.2
  node_modules/typescript/package.json
```

El script exige versiones exactas, rutas diferentes y que la entrada del
SDK generador y su compilador residan dentro del árbol aislado. La ejecución
de `node node_modules/typescript/bin/tsc --version` devuelve `Version 7.0.2`.
`client:typecheck` y `client:build` incluyen el código generado con ese compilador.

Se ejecutó `npm ci` raíz dos veces con los lockfiles finales. Cada ejecución
instala también el paquete aislado desde su lockfile. Se compararon los árboles
completos mediante `npm ls --all --json` en raíz y generador antes y después:
idénticos. Los SHA-256 de ambos lockfiles tampoco cambiaron:

```text
24195181ea7d9d3a11d1c813cafc64d1d9a11725d332c10eb9df000ffa9547e6  package-lock.json
e736f509ea989ec4ac71cdff30ba0e620de9848ca06ef9cba5a23aa0f3b52158  tools/openapi-generator/package-lock.json
```

La generación repetida después de reinstalar coincide en los 16 archivos.

## Validaciones locales

| Comando/control | Resultado |
|---|---|
| `npm ci` con npm 11.9.0, ambos árboles | Correcto; árboles y lockfiles idénticos al repetir. |
| `client:generate` / `client:check-generated` | Correctos; dos regeneraciones sin diferencias en 16 archivos. |
| `npm run client:lint` | Correcto: textos visibles, claves, interpolaciones y paridad ES/EN. |
| `npm run client:typecheck` | Correcto con TypeScript 7.0.2. |
| `npm run client:test` | 92 pruebas Vitest correctas y suite Node de controles i18n correcta. |
| `npm run client:build` | Correcto con Vite 8.3.0 y TypeScript 7.0.2. |
| `npm run openapi:lint` | Contrato aceptado válido y sin cambios. |
| `npm run infra:up` / `npm run infra:config` | PostgreSQL y Mailpit saludables; Compose válido, solo local. |
| `npm run server:test` | 313 pruebas, 0 fallos, 0 errores y 1 benchmark omitido. |
| `npm run server:verify` | Correcto; mismas 313 pruebas, integración PostgreSQL 16.15, Flyway y arquitectura. |
| `check-migrations.sh`, `check-local-baseline.sh`, `deployment-gate-test.sh` | Correctos. |
| `docker build -f client/Dockerfile -t propractix-e7-client:local .` | Correcto; instalación de ambos árboles y build en imagen, sin publicación. |
| `git diff --check`, revisión de diff y status | Correctos antes del commit. |

Las pruebas de API ejercitan el SDK Fetch generado, encoding de query/cursor,
límite configurado, UUID, 400/409/429/503, fallo de red, cabeceras de espera y
URLs inseguras. Las de componentes integran el adapter y React Query con
respuestas HTTP sintéticas: debounce, consulta vacía, selección, deduplicación,
doble clic, paginación, errores de páginas posteriores, reinicio sin cursor,
conflicto reiterado, respuesta tardía/cancelación, espera entre consultas y
remontajes, opción manual, teclado/foco/ARIA y cambio ES/EN con resultados o
error ya visibles. Los errores no filtran la prosa técnica de las respuestas.
Este catálogo es global público y E7 no maneja datos tenant-owned.

En el sandbox inicial, las pruebas backend no podían acceder a Docker ni al
attach JVM de Mockito. Se repitieron fuera del sandbox con autorización de la
validación local y los resultados correctos anteriores son de esas ejecuciones.
No se simulan pruebas ni se atribuye una ejecución local a CI.

## Riesgos y pendientes

- `npm audit` del generador termina sin vulnerabilidades. Se fija la
  dependencia transitiva `js-yaml` 4.3.2 mediante override para corregir los
  avisos de consumo de CPU de su parser YAML sin cambiar Hey API ni TypeScript.
- El árbol principal conserva cinco avisos de la baseline: tres altos y dos
  moderados en `source-map-js`, `undici`, Vite, Vitest y `@vitest/mocker`. El diff
  del lockfile principal solo añade React Query/query-core y el postinstall;
  no actualiza las dependencias ya fijadas. Esos avisos no se presentan como
  resueltos ni como resultado de Trivy; quedan visibles para revisión del PO.
- La accesibilidad se prueba con Testing Library y teclado en DOM. No se
  afirma una auditoría con lector de pantalla real ni E2E Playwright; E8 sigue
  fuera del alcance autorizado.
- H02 y E7 siguen `IN_PROGRESS`. Aceptación, E8, RUCT real, AWS y merge requieren
  sus autorizaciones correspondientes; no se infieren de esta evidencia.

# Corrección de descubrimiento Playwright en VS Code

- Fecha: 2026-11-01.
- Rama: `fix/playwright-vscode-discovery`.
- Base: `3d339d1598c4a7deafedea3d4d9d78fc16c87088`, desde la rama de incremento
  actualizada mediante fetch. PR independiente de E9; no incorpora su snapshot.
- Autorización: corrección técnica solicitada por el Product Owner, con commit,
  push y Draft PR hacia `increment/01-company-identity-catalog`.
- No se modifican los estados de E8, E9, H02 ni CL0, ni se ejecuta merge o AWS.

## Comportamiento corregido

La excepción al importar `client/playwright.config.ts` impedía que el
descubrimiento de VS Code funcionara sin las variables del orquestador.
La configuración registra ahora `globalSetup` y sigue sin URL por defecto.
La función exportada en `client/e2e/global-setup.ts` comprueba la presencia de
`PROPRACTIX_E2E_BASE_URL` y el formato `propractix-e8-<pid>` de
`PROPRACTIX_E2E_PROJECT` al iniciar una ejecución, antes de los escenarios y hooks.
Una invocación sin ese entorno termina con código 1 e indica:

```text
Run npm run client:e2e to execute tests with the isolated orchestrator environment
```

La [documentación oficial de globalSetup](https://playwright.dev/docs/test-global-setup-teardown#option-2-configure-globalsetup-and-globalteardown)
define este hook antes de las pruebas. `--list` permite cargar la configuración y
descubrir las pruebas sin ejecutarlo. El [runbook local](../operations/local-catalog-e2e.md#descubrimiento-en-vs-code)
documenta el descubrimiento en VS Code y que la ejecución completa sigue usando
`npm run client:e2e` desde la terminal. Se verificó el mecanismo CLI de
descubrimiento; no se automatizó la interfaz gráfica de VS Code.

## Validación

Todos los comandos de descubrimiento/rechazo se ejecutaron con las variables
`PROPRACTIX_E2E_*` eliminadas del entorno, salvo los valores indicados en cada
caso. Un preload temporal instrumentó las llamadas de procesos para detectar y
rechazar Docker/psql; no registró ningún intento. Los informes JSON confirman
cero pruebas ejecutadas en esas comprobaciones.

| Comprobación | Resultado |
|---|---|
| CLI `test -c client/playwright.config.ts --list` | Salida 0; ocho escenarios descubiertos, cero ejecutados, sin Docker/SQL. |
| CLI directa sin variables E2E | Salida 1 en `globalSetup`, mensaje anterior, cero pruebas ejecutadas/omitidas, sin Docker/SQL. |
| CLI directa con URL pero sin proyecto | Salida 1 en `globalSetup`, mismo mensaje, sin pruebas ni Docker/SQL. |
| CLI directa con URL y proyecto `infra` | Salida 1 en `globalSetup`, mismo mensaje, sin pruebas ni Docker/SQL. |
| `npm run client:typecheck` | Correcto, TypeScript 7.0.2. |
| Comprobación TypeScript de config y globalSetup | Correcta con `--noEmit --strict`, resolución Bundler y tipos Node disponibles en el runtime; no se cambian dependencias. |
| `npm run client:lint` | Correcto, textos visibles y paridad i18n. |
| `npm run client:test` | 94 pruebas Vitest y una comprobación Node correctas, sin fallos. |
| `npm run client:e2e` mediante npm 11.9.0 | Ocho escenarios Chromium correctos en 49,9 s; cero fallos, omisiones, reintentos o flaky. Backend empaquetado y frontend compilado por el orquestador. |
| Limpieza del proyecto `propractix-e8-333574` | Sin contenedores ni volumen restantes; secretos y lock eliminados. |

Los logs e informes locales se conservan en `.local/playwright-discovery/`;
los resultados de la suite completa siguen en `.local/e2e/results.json`.
Las comprobaciones auxiliares no se añaden como escenarios Playwright ni
filtran/omiten los ocho existentes.

## Aislamiento y entrega

El orquestador, `scripts/e2e-environment.mjs`, las fixtures y Compose se conservan
sin cambios. Continúan el lock de ejecución, secretos sintéticos temporales,
validación del proyecto en cada operación Compose/SQL, base sin puerto expuesto,
publicación de fixtures por CLI aceptada y limpieza del proyecto/volumen al
terminar. Se conservan un worker, cero reintentos, límites de tiempo y la
prohibición de trazas, capturas y vídeo.

Diff, `git diff --check` y estado revisados: cuatro archivos de configuración,
setup y documentación; sin cambios en escenarios, orquestador, Compose, fixtures,
dependencias, planes ni registros de estado. El backend no cambia; se valida su
empaquetado y el recorrido completo dentro del orquestador E2E.
El SHA se registra en el cuerpo de la Draft PR y en la entrega. CI, Trivy y
Build OCI quedan pendientes de verificación del Product Owner; el agente no
espera ni supervisa sus ejecuciones. La PR queda Draft y sin fusionar.

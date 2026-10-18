# Internacionalización y errores de presentación

Fecha: 2026-10-18. Corrección solicitada por el Product Owner durante E4.
Base: `ffb7f537125072b77666d34813a2ea94b7d6425d`.
Rama: `feature/i1-h02-i18n-errors`, para integrar mediante PR en
`feature/i1-h02-e4-catalog-import`.
I1-H02 y E4 continúan `IN_PROGRESS`. No se inicia E5A.

## Qué se traduce

Toda etiqueta, acción, ayuda, estado y error que una persona ve en la interfaz
procede de `client/src/shared/i18n/locales/{es,en}.json` y se presenta con
`react-i18next`. Las claves están tipadas mediante `i18next.d.ts`.

| Contenido | Tratamiento |
|---|---|
| Texto visible, incluido `placeholder`, `alt` y nombres accesibles | Clave i18n, ES y EN completos. |
| Error público o de campo | Código estable y traducción registrada. |
| Código desconocido o fallo de renderizado | `errors.unexpected_error`; no se muestra el dato recibido. |
| `Error.message`, `ProblemDetail.title/detail`, prosa de violations | Diagnóstico; no se usa como texto de interfaz. |
| Excepciones Java de invariantes, configuración y logs | Diagnósticos internos. No se convierten en traducciones de dominio. |
| Salida CLI `catalog_import_result code=…` | Contrato administrativo por códigos y recuentos, independiente del locale. |
| Nombre de universidad y otros datos de fuente/usuario | Se muestran como datos; no se traducen automáticamente. |

`ErrorMessage` recibe exclusivamente `code`; no admite un mensaje libre. Solo
reconoce claves propias del catálogo `errors`, sin aceptar nombres heredados
como `constructor` o `__proto__`. `AppErrorBoundary` cubre fallos de renderizado
del árbol de aplicación y ofrece una acción de reintento traducida. No captura
fallos de arranque previos al montaje, manejadores de eventos ni tareas
asíncronas: cada consumidor traduce su código mediante `ErrorMessage`.

Esta corrección registra los códigos previstos para H02; no crea ni expone
el endpoint. E6/E7 deben conectar los resultados HTTP y las violations a este
componente; nunca deben mostrar el mensaje de una excepción como fallback.

## Locale

`product-catalog.json` configura locales soportados, correspondencias de país
y fallback. `resolveLocale` aplica la decisión D-005 en este orden:

1. Preferencia explícita del usuario.
2. Locale empresarial configurado.
3. Correspondencia configurada del país empresarial.
4. Preferencias del navegador cuando aún no existe contexto empresarial.
5. Fallback configurado si el valor solicitado no está soportado.

Un locale explícito de usuario/empresa no soportado usa el fallback; el navegador
no sustituye la configuración empresarial. La resolución nunca cambia el país
ni selecciona un policy pack jurídico.

El shell permite seleccionar ES/EN y conserva la elección en
`localStorage` bajo `propractix.ui.locale`. Si el navegador deniega almacenamiento,
el cambio funciona durante la sesión. `main.tsx` actualiza `html.lang` y el
título al cambiar idioma; no contiene un fallback español literal.

Actualmente no existe onboarding empresarial: el arranque usa preferencia y
navegador. Al cargar la empresa en la historia correspondiente, deberá llamarse
a `resolveLocale` con sus datos autoritativos, conservando la preferencia del
usuario. La función y su precedencia ya tienen pruebas; no se implementa H03.

## Controles y evidencia local

- `client:lint`: recorre TS/TSX/JS/JSX con AST; detecta texto JSX, literales en
  atributos de presentación, expresiones condicionales/templates, uso directo
  de prosa de errores identificables, claves literales desconocidas y
  `defaultValue` incrustado en llamadas `t`.
- La paridad usa los locales de configuración; exige traducciones no vacías,
  mismas claves y mismos nombres de parámetros de interpolación.
- `client:typecheck` comprueba claves de traducción tipadas.
- `client:test`: 6 pruebas Node del guard y 45 pruebas Vitest, todas correctas.
  Incluye ES/EN, fallback, cambio de idioma con error ya visible, preferencia,
  país empresarial configurable, almacenamiento denegado y ausencia de prosa
  técnica en la pantalla de error.
- `client:build` y `openapi:lint`: correctos. `git diff --check`: limpio.

El guard analiza sintaxis local; no realiza seguimiento completo de datos a
través de variables, imports, spreads o componentes externos. La revisión de
cada nueva pantalla y sus pruebas de accesibilidad/errores siguen siendo
necesarias. No se afirma que un análisis estático detecte todos los textos.

El parser AST reutiliza `rolldown` 1.2.8, licencia MIT, ya presente en el lockfile
por Vite. Se declara como dependencia de desarrollo directa, sin añadir una
dependencia de runtime ni cambiar la baseline de Vite/TypeScript.

La copia local dispone de Java 17 y carece de Docker; por ello no se ejecutaron
`infra:up`, `server:test`, `server:verify` ni el `check` completo en este entorno.
No se modificó Java, configuración backend, migraciones ni contratos. CI de la
PR debe validar backend, infraestructura, secretos, Trivy y Build OCI sobre el
commit resultante antes de integrar.

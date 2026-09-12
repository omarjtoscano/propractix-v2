# ADR-008 — OpenAPI, errores, correlación e idempotencia

- Estado: Propuesto — revisión 3
- Fecha: 2026-09-12
- Decisores: Arquitectura, Backend y Frontend
- Reemplaza: revisión 2 del ADR-008

## Contexto

V2 necesita un contrato único entre backend y frontend, errores procesables y protección contra reintentos. El registro ocurre antes de disponer de actor autenticado y no puede persistir credenciales o respuestas sensibles para resolver idempotencia.

## OpenAPI

- La API del MVP vivirá bajo `/api/v1`.
- `docs/api/openapi.yaml` será la fuente de verdad design-first.
- No se generarán dominio ni implementación de servidor desde el contrato.
- El cliente TypeScript se generará desde OpenAPI.
- Cada historia funcional modifica conjuntamente contrato, API, cliente, UI, i18n y tests.
- CI comprobará validez del contrato, cambios incompatibles y que el cliente generado no presenta diff.
- OpenAPI, dominio y eventos exponen códigos estables, no etiquetas de interfaz. Toda etiqueta, validación, acción, estado o error visible se resuelve mediante una clave i18n; CI detecta literales visibles y comprueba paridad de locales.

## Errores

Las respuestas de error usarán `application/problem+json` con:

```text
type, title, status, detail, instance, code, correlationId, violations
```

`detail` no contendrá excepciones internas ni datos sensibles. `code` será estable y la UI lo traducirá. Taxonomía inicial:

| Código | HTTP | Uso |
|---|---:|---|
| `validation_error` | 400 | Entrada inválida con violations. |
| `authentication_failed` | 401 | Credenciales/token inválido sin enumeración. |
| `forbidden` | 403 | Actor autenticado sin capacidad, cuando revelar el recurso sea seguro. |
| `resource_not_found` | 404 | Inexistencia o acceso cross-tenant no revelable. |
| `conflict` | 409 | Invariante o unicidad de negocio. |
| `concurrent_modification` | 409 | Versión optimista obsoleta. |
| `idempotency_key_reused` | 409 | Misma clave con fingerprint diferente. |
| `rate_limited` | 429 | Límite de abuso. |
| `privacy_policy_unavailable` | 503 | Registro deshabilitado sin aviso vigente aprobado. |

No se expondrá `traceId` como contrato estable. OpenTelemetry podrá generar un trace técnico interno. `correlationId` será el identificador público de soporte: se aceptará uno válido o se generará, se devolverá en `X-Correlation-ID`, aparecerá en errores, logs y eventos y tendrá límites de formato/longitud.

## Idempotencia anónima

Registro y otras creaciones sensibles exigirán `Idempotency-Key`:

- valor aleatorio con al menos 128 bits; se acepta UUID v4;
- longitud y caracteres limitados;
- unicidad atómica por `operation + key`;
- estados `PROCESSING`, `SUCCEEDED`, `FAILED_REPLAYABLE` y expirado;
- TTL inicial de 24 horas;
- fingerprint HMAC-SHA-256 de operación, versión de contrato, método, plantilla de ruta y comando validado serializado de forma determinista con propiedades ordenadas y UTF-8;
- se excluyen orden original del JSON, headers no semánticos, correlation ID y datos de transporte;
- las operaciones de creación inicial de credencial no utilizan almacenamiento de idempotencia basado en la contraseña: el registro de verificación de un solo uso evita la repetición;
- la fila no almacena contraseña, token, cookie, body original ni headers sensibles;
- la respuesta reproducible se limita a status, headers permitidos e identificador opaco de operación; para registro se usa un `202` genérico.

Semántica:

1. Primera petición reclama la clave atómicamente como `PROCESSING`.
2. Misma clave/fingerprint terminal reproduce la respuesta segura.
3. Misma clave con fingerprint distinto devuelve `409 idempotency_key_reused`.
4. En onboarding sin polling público, una solicitud concurrente que observa `PROCESSING` recibe el mismo `202` genérico y el mismo identificador opaco de operación, sin ejecutar de nuevo.
5. Un fallo antes de comenzar la operación puede marcarse replayable; un estado incierto no se repite a ciegas.

`platform.idempotency` será propietario de la tabla. Los datos admitidos, acceso y limpieza se rigen también por ADR-006 y ML-15.

## Privacidad del registro

Un endpoint de registro en producción requiere una versión `APPROVED` y vigente del aviso aplicable. Un filtro anterior al binding del body ejecuta este guard: si no existe, responde `privacy_policy_unavailable` antes de deserializar, persistir, auditar o registrar el body personal y sin crear un registro de idempotencia derivado del body. Fixtures de test/local no pueden promoverse a producción.

## Alternativas consideradas

- Contrato generado solo desde controladores: rechazado como fuente única.
- Generar servidor/dominio desde OpenAPI: rechazado por mezclar transporte y dominio.
- Errores ad hoc: rechazados.
- Hash simple de una petición con contraseña: rechazado por riesgo de ataque offline.
- Idempotencia confiada al cliente: rechazada.

## Consecuencias

- Cada historia funcional es vertical y mantiene cliente/servidor sincronizados.
- Se necesita almacenamiento y limpieza de registros de idempotencia.
- Los errores cross-tenant no revelan existencia.
- `correlationId` es uniforme; tracing permanece detalle interno.

## Condiciones documentales de aceptación

- Arquitectura, Backend y Frontend aceptan OpenAPI design-first, Problem Details e idempotencia anónima protegida por HMAC.
- Canonicalización, respuesta `PROCESSING` y precedencia del guard ML-15 están cerradas.
- Los códigos públicos son independientes de cualquier locale y toda presentación queda en i18n.

## Conformidad de la implementación

- CI detecta OpenAPI inválido, cliente desactualizado o cambio incompatible no autorizado.
- Pruebas concurrentes demuestran una sola ejecución por clave.
- Pruebas verifican que ningún secreto se persiste en idempotencia.
- Todas las respuestas incluyen `X-Correlation-ID` y los errores el mismo valor.
- Registro productivo queda cerrado sin política de privacidad aprobada.

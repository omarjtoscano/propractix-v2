# ADR-008 — OpenAPI, errores, correlación e idempotencia

- Estado: Aceptado — revisión 4
- Fecha: 2026-09-12
- Decisores: Arquitectura, Backend y Frontend
- Reemplaza: revisión 3 del ADR-008

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

`detail` no contendrá excepciones internas ni datos sensibles. `code` será estable y la UI lo traducirá. La UI nunca mostrará directamente `title`, `detail` o el texto de `violations` como copy: resolverá `code` y los códigos de cada violación mediante sus catálogos i18n. Taxonomía inicial:

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
| `company_email_domain_not_allowed` | 400 | Violación de campo: el autorregistro empresarial exige un dominio admitido por la política vigente. |
| `company_email_policy_unavailable` | 503 | H03 cerrado porque su política versionada no está disponible. |
| `privacy_notice_changed` | 409 | El aviso mostrado ya no coincide con el vigente; debe presentarse de nuevo. |
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

H03 y H06 recibirán una referencia estructurada no confiable:

```text
privacyNoticeReference
- noticeId
- version
```

Después del binding y de las validaciones sintácticas, pero antes de cualquier escritura o claim de idempotencia derivado del body, application solicita a `compliance` el aviso `APPROVED` y vigente para finalidad, jurisdicción e instante del servidor. El registro continúa únicamente si `noticeId` y `version` coinciden exactamente con la resolución autoritativa.

Si el aviso falta, está expirado, pertenece a otra finalidad/jurisdicción o fue sustituido entre render y submit, no se persisten onboarding, evidencia, idempotencia derivada del body ni auditoría con PII. Un aviso inexistente produce `privacy_policy_unavailable`; una referencia manipulada u obsoleta produce `privacy_notice_changed` sin revelar datos adicionales.

Ante `privacy_notice_changed`, el cliente descarta la aceptación anterior, recupera el aviso vigente, lo muestra de nuevo y exige confirmación explícita. Nunca migra o acepta automáticamente una nueva versión. La evidencia persistida procede del resultado autoritativo del servidor definido en ADR-006.

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
- Canonicalización, respuesta `PROCESSING`, precedencia del guard ML-15 y validación autoritativa posterior al binding están cerradas.
- Los códigos públicos son independientes de cualquier locale y toda presentación queda en i18n.

## Conformidad de la implementación

- CI detecta OpenAPI inválido, cliente desactualizado o cambio incompatible no autorizado.
- Pruebas concurrentes demuestran una sola ejecución por clave.
- Pruebas verifican que ningún secreto se persiste en idempotencia.
- Todas las respuestas incluyen `X-Correlation-ID` y los errores el mismo valor.
- Registro productivo queda cerrado sin política de privacidad aprobada.
- Avisos ausentes, expirados, sustituidos, manipulados o de finalidad incorrecta se rechazan antes de cualquier escritura con PII.
- La UI traduce códigos estables y no presenta la prosa de Problem Details como texto de interfaz.

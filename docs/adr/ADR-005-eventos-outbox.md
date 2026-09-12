# ADR-005 — Eventos internos, outbox y onboarding durable

- Estado: Propuesto — revisión 3
- Fecha: 2026-09-12
- Decisores: Arquitectura, Seguridad y Operaciones
- Reemplaza: revisión 2 del ADR-005

## Contexto

El onboarding empresarial y la verificación de correo producen cambios en varios contextos y efectos externos que no pueden perderse. La entrega debe sobrevivir a fallos sin guardar tokens de verificación en claro ni convertir la outbox en un almacén indefinido de datos personales.

## Decisión

Los aggregates podrán registrar eventos de dominio Java puros. La capa de aplicación los transformará en eventos de integración cuando deban cruzar contextos o producir efectos externos. Esos eventos se guardarán en PostgreSQL dentro de la misma transacción que el cambio del contexto productor.

La entrega tendrá semántica al menos una vez. Todo consumidor será idempotente.

## Propiedad

`platform.outbox` será propietario de `platform_outbox_event` y del relay técnico. Los contextos productores definirán un puerto de salida para registrar el mensaje; la configuración proporcionará el adapter. Ningún dominio dependerá de `platform`.

Los eventos contendrán como máximo:

```text
event_id
event_type
event_version
producer_context
aggregate_type
aggregate_id
aggregate_version
tenant_id opcional
occurred_at
correlation_id
payload mínimo
delivery_status
attempt_count
available_at
lease_until
last_error_code
```

No contendrán contraseñas, hashes de credencial, tokens bearer, cookies, documentos, cuerpos HTTP ni PII innecesaria. Los eventos de provisioning contendrán únicamente IDs. La PII estrictamente necesaria para continuar un onboarding permanecerá cifrada en la tabla del process manager propietario, sujeta a acceso y retención ML-15, y se proporcionará transitoriamente al puerto de destino.

## Relay y recuperación

- Claim mediante `FOR UPDATE SKIP LOCKED` y lease con expiración.
- Ordering solo por aggregate mediante `aggregate_version`; no se promete orden global.
- Backoff exponencial con jitter y máximo configurable.
- Al superar intentos, estado `DEAD_LETTER`; no descarte silencioso.
- Métricas para pendientes, edad, reintentos y dead letters.
- Reproceso administrativo explícito, autorizado y auditado.
- Payload redactado o eliminado tras estado terminal según la política de retención; se conservan metadatos mínimos de entrega.

Los consumidores persistentes mantendrán deduplicación por `consumer + event_id` cuando repetir el efecto no sea naturalmente idempotente.

## Onboarding empresarial

`organization` conserva el estado de `CompanyOnboarding` y publica solicitudes durables. `identity` reacciona en su propia transacción y devuelve eventos de resultado. Los pasos son reintentables:

1. `CompanyOnboardingSubmitted`.
2. `AdministratorProvisioningRequested`, con `OnboardingId` y sin contraseña, email o nombre.
3. `AdministratorProvisioned` o `AdministratorProvisioningFailed`.
4. `EmailVerificationRequested`.
5. `AccountEmailVerified`.
6. `CompanyOnboardingReady`.

El esquema final de cada evento se publicará versionado bajo `docs/events`; OpenAPI continuará reservado para HTTP. Un handler propiedad de `organization` carga el onboarding, descifra solo los campos mínimos y llama transitoriamente al puerto público de `identity`. La operación de `identity` es idempotente por `OnboardingId`. Un fallo produce `IDENTITY_PENDING`, `EMAIL_PENDING` o `FAILED`, con reintento o expiración; no hay rollback distribuido.

## Enlace de verificación

La outbox guardará solo `VerificationId`, finalidad y metadatos mínimos. No guardará el token final.

Al reclamar la entrega, el relay invocará un handler propiedad de `identity`. Ese handler comprobará que la verificación sigue pendiente, recuperará el correo desde `identity`, generará un token JWS firmado y de vida corta y llamará a `NotificationDeliveryPort` con destinatario, `templateKey`, locale y enlace únicamente en memoria. `notifications` implementará el puerto y no consultará repositorios de `identity`.

El JWS incluirá únicamente:

- `jti`: identificador aleatorio de verificación;
- `purpose`: valor tipado como `INITIAL_CREDENTIAL_SETUP` u `ONBOARDING_CONTINUATION`;
- `aud`: audiencia exclusiva para ese propósito;
- `iat` y `exp`;
- `kid` para rotación de clave.

El token no incluirá correo, nombre ni tenant. Se firmará con una clave distinta de la utilizada para access tokens. `identity` conservará el registro de verificación, su expiración y consumo, pero no el token. El endpoint validará firma, propósito y audiencia, y consumirá el registro de forma atómica.

El adapter de correo no persistirá correo, enlace ni token durante el Incremento 1 y los redactará de logs, errores y métricas. Los reintentos y su último código de error quedarán en `platform_outbox_event`. Si una verificación fue invalidada entre el claim y la preparación, el handler no envía; si se invalida después del envío, el enlace recibido será rechazado de forma segura y podrá solicitarse otro.

Si el evento se intenta entregar después de la expiración, termina con estado no entregable y el usuario deberá solicitar un nuevo enlace.

## Alternativas consideradas

- Correo antes del commit: rechazado por mensajes sobre cambios no confirmados.
- Evento solo en memoria: rechazado por pérdida entre commit y publicación.
- Token en claro en outbox: rechazado.
- Token cifrado persistido: viable, pero más complejo que generar un JWS con registro de uso único.
- Broker externo: rechazado durante el MVP.

## Consecuencias

- El flujo es eventualmente consistente y la UI debe mostrar estados intermedios.
- PostgreSQL soporta outbox e inbox sin infraestructura adicional.
- El relay necesita leasing, reintentos, métricas y operación de dead letters.
- La política de retención y redacción debe aprobarse antes de producción.

## Condiciones documentales de aceptación

- Arquitectura, Seguridad y Operaciones aceptan outbox PostgreSQL, entrega al menos una vez y ausencia de broker.
- La ceremonia no transporta contraseñas, hashes de credencial ni bearer tokens en eventos.
- Los contratos de provisioning y entrega son unidireccionales, no cíclicos y tienen propietario.
- ML-15 determina campos cifrados y retención antes de recibir datos reales en producción.

## Conformidad de la implementación

- Una prueba demuestra recuperación tras commit sin entrega.
- Duplicar un evento no duplica el efecto.
- Dos workers no adquieren el mismo mensaje simultáneamente.
- Ninguna fila de outbox contiene un token utilizable o PII innecesaria.
- El token de verificación expira y solo puede consumirse una vez.

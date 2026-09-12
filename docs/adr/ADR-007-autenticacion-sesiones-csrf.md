# ADR-007 — Autenticación, sesiones, CSRF y verificación de correo

- Estado: Aceptado — revisión 4
- Fecha: 2026-09-12
- Decisores: Seguridad, Producto y Arquitectura
- Reemplaza: revisión 3 del ADR-007

## Contexto y amenazas

El Incremento 1 expone registro, verificación, login y refresh. Debe mitigar robo y reutilización de tokens, credential stuffing, enumeración de cuentas, fijación de tenant, CSRF, XSS, replay, carreras concurrentes y permisos obsoletos.

La verificación de correo acredita control de una dirección, no existencia ni representación legal de una empresa.

## Estados separados

```text
UserAccount: PENDING_EMAIL -> ACTIVE -> SUSPENDED -> CLOSED
Company: SELF_DECLARED -> PENDING_VERIFICATION -> VERIFIED | REJECTED
Company: VERIFIED -> SUSPENDED
```

Verificar el correo activa la cuenta del administrador y permite configuración segura. La empresa permanece `SELF_DECLARED`. La verificación empresarial será independiente y se exigirá antes de publicar ofertas, no antes de acceder al onboarding.

## Credenciales

- I1-H03 no recibe contraseña. `identity` crea la cuenta pendiente sin credencial.
- El enlace `INITIAL_CREDENTIAL_SETUP` abre la pantalla de verificación y el cliente envía token y contraseña nueva directamente a `POST /api/v1/email-verifications/complete`.
- `identity` valida y consume el token, persiste únicamente el hash y activa la cuenta dentro de su transacción. La contraseña en claro no sale del request ni llega a outbox, eventos, onboarding, logs o idempotencia.
- Una cuenta existente utiliza `ONBOARDING_CONTINUATION`, exige autenticación o reautenticación y nunca vuelve a pedir o sustituir su contraseña como efecto del onboarding.
- Contraseñas con BCrypt, coste inicial 12.
- I1-H01 ejecutará benchmark en el entorno objetivo y documentará latencia; el coste se ajustará si incumple el presupuesto operativo.
- Cada credencial registra algoritmo y parámetros para permitir rehash progresivo.
- Cambio o recuperación de contraseña revoca todas las familias refresh.
- Respuestas de login, registro y recuperación no enumeran cuentas.

## Access token

- JWT firmado con RS256 y `kid`.
- Claves privadas fuera del repositorio; servicio mantiene clave activa y claves públicas anteriores durante la ventana de rotación.
- Vida inicial: 10 minutos.
- Claims obligatorios: `iss`, `aud`, `sub`, `iat`, `nbf`, `exp`, `jti` y versión de autorización.
- Claims empresariales opcionales: tenant activo, `membership_id` y roles mínimos.
- No incluye correo, nombre ni otros datos personales.
- `issuer` y `audience` serán valores exactos por entorno y se validarán siempre.
- El cliente conserva el access token solo en memoria.

Una membresía revocada invalida sus familias refresh. Durante el Incremento 1, toda operación tenant-owned consulta la membership vigente; ninguna escritura empresarial depende únicamente de claims potencialmente obsoletos.

## Refresh token

- Opaco y aleatorio, con al menos 256 bits de entropía.
- Vida máxima inicial de familia: 30 días.
- En staging/producción: cookie `__Secure-propractix_refresh`, `HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/api/v1/auth` y sin `Domain`.
- En local HTTP: cookie `propractix_refresh` sin prefijo reservado y sin `Secure`; esta configuración debe fallar al arrancar fuera del perfil local.
- La base guarda solo SHA-256 del token de alta entropía, familia, secuencia, expiración, revocación y auditoría mínima.
- Cada uso rota el token mediante compare-and-set atómico.
- Reutilizar un token anterior revoca la familia completa.
- Carreras concurrentes y replay se prueban sobre PostgreSQL.

## CSRF, Origin y CORS

Refresh y logout usarán doble envío:

- cookie legible `__Secure-propractix_csrf` en staging/producción, con `Secure`, `SameSite=Lax`, `Path=/api/v1/auth` y sin `Domain`; en local se usa `propractix_csrf` sin prefijo reservado;
- header `X-XSRF-TOKEN` con coincidencia constante;
- validación exacta de `Origin` contra allowlist.

En producción, una petición que use la refresh cookie sin `Origin` válido será rechazada. Clientes no navegador que se autoricen en el futuro tendrán un flujo separado sin cookies.

CORS utilizará orígenes explícitos y nunca `*` con credenciales.

`POST /api/v1/auth/active-tenant` exigirá bearer access token y no leerá ni modificará la refresh cookie; valida la membership y devuelve un access token nuevo. `POST /api/v1/auth/refresh` puede recibir un `tenantId` preferido no secreto, pero al usar cookie queda sujeto a CSRF y Origin y siempre vuelve a validar la membership. El ID solicitado nunca es autoridad por sí mismo.

El despliegue web del MVP mantendrá frontend y API bajo el mismo sitio registrable y TLS, aunque puedan usar subdominios diferentes. Una topología realmente cross-site exige revisar `SameSite`, CSRF y CORS mediante actualización de este ADR antes de desplegarse.

## Verificación de correo

Se adopta el token JWS de propósito único definido en ADR-005, firmado con clave separada de los access tokens. El registro de verificación se consume atómicamente, tiene expiración y admite reenvío mediante un nuevo registro o invalidación controlada del anterior.

## Protección contra abuso

Registro, login, reenvío y verificación no estarán disponibles públicamente hasta tener rate limiting. La política combinará IP protegida/pseudonimizada, identificador de cuenta cuando exista y límites por operación. Se implementará detrás de un puerto para poder sustituir el adapter sin cambiar casos de uso. `platform.ratelimit` será el propietario técnico y persistirá únicamente fingerprints HMAC, ventana y contadores con TTL.

Seguridad y Operaciones mantendrán los valores exactos por operación en configuración tipada y versionada. Cada historia deberá fijarlos y probar `429`/`Retry-After` antes de exponer el endpoint; no se introducirán límites ni textos visibles dentro del dominio.

El gate `S0_PUBLIC_ENDPOINTS` del plan materializa esta decisión. Bloquea la exposición de cualquier endpoint público del Incremento 1 —incluido el catálogo si conserva rate limiting— hasta documentar límites por operación, claves pseudonimizadas, TTL, comportamiento fail-closed, métricas, alertas y pruebas. Aceptar este ADR no cierra automáticamente S0.

## Alternativas consideradas

- Tokens en `localStorage`/`sessionStorage`: rechazados por XSS.
- Refresh JWT: rechazado por menor control de rotación y revocación.
- Refresh en texto claro: rechazado.
- Sesión HTTP tradicional: viable, pero no elegida para el API actual; exige ADR si se reconsidera.
- Verificación de correo equivalente a empresa verificada: rechazada.

## Consecuencias

- El cliente necesita renovación en memoria, CSRF y selección de tenant.
- Seguridad requiere claves rotables, limpieza de sesiones, rate limiting y auditoría.
- La verificación empresarial se implementará como capacidad separada antes de `G1_PUBLICATION`.
- Cambiar algoritmo o duración exige revisión de seguridad y actualización del ADR.

## Condiciones documentales de aceptación

- Producto, Seguridad y Arquitectura aceptan la creación passwordless inicial, RS256, access de 10 minutos y refresh de 30 días.
- La taxonomía usa `COMPANY_OWNER` y separa cuenta activa de empresa verificada.
- Los endpoints que usan cookie y los que usan bearer están delimitados sin ambigüedad.
- Seguridad y Operaciones son owners del rate limiting y deben aprobar valores antes de exponer cada endpoint.
- El plan identifica S0 como gate separado de privacidad y lo aplica a cada historia que expone endpoints públicos.

## Conformidad de la implementación

- Claims, claves, cookies, CSRF, Origin y CORS tienen pruebas positivas y negativas.
- Refresh rotatorio resiste doble consumo y reutilización.
- Registro/login no enumeran cuentas y están limitados antes de exponerse.
- Cuenta activa y estado empresarial se prueban por separado.
- No se persiste ni registra ningún token utilizable.

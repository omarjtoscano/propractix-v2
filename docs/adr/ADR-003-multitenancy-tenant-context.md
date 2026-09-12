# ADR-003 — Multi-tenancy, identidad y tenant activo

- Estado: Aceptado — revisión 4
- Fecha: 2026-09-12
- Decisores: Producto, Seguridad y Arquitectura
- Reemplaza: revisión 3 del ADR-003

## Contexto

La empresa es el tenant principal. Una persona puede colaborar con más de una empresa y también actuar como estudiante. El aislamiento no puede depender de IDs suministrados por el navegador ni de filtros ORM implícitos.

## Decisión de identidad

- `UserAccount` será global y su correo normalizado será único globalmente.
- Una cuenta podrá estar asociada a un `StudentProfile`, propiedad del contexto `student`, y a cero o más membresías empresariales.
- Los formularios de empresa y estudiante seguirán separados y convergerán en una cuenta existente únicamente mediante la ceremonia definida a continuación.
- Una universidad no será tenant ni tendrá cuenta obligatoria durante el MVP.

`StudentProfile` tendrá un `StudentId` propio y una referencia uno-a-uno a `UserId`. La asociación se crea únicamente mediante un onboarding autenticado o verificado; una coincidencia de correo nunca basta.

### Convergencia de cuenta

- Si el correo no existe, `identity` crea una cuenta `PENDING_EMAIL` sin credencial y emite una verificación de propósito `INITIAL_CREDENTIAL_SETUP`.
- El usuario establece la contraseña al completar esa verificación directamente contra `identity`; la contraseña no atraviesa `organization`, `student`, outbox ni eventos.
- Si la cuenta existe y el actor ya está autenticado, debe reautenticarse antes de vincular un nuevo onboarding.
- Si la cuenta existe y el actor no está autenticado, recibe un enlace de propósito `ONBOARDING_CONTINUATION`; después debe autenticarse. La respuesta pública sigue siendo genérica.
- No se crean perfiles, memberships ni empresas por coincidencia silenciosa de correo.

## Identificador tenant

Durante el MVP, el identificador del aggregate `Company` será el `TenantId`. No habrá `CompanyId` alternativo ni tabla `tenant`. La representación persistida y pública será un UUID.

Si en el futuro otra clase de organización se convierte en tenant, un ADR de migración introducirá el concepto generalizado; no se anticipará ahora.

## País, locale y jurisdicción

`Company` conserva `registeredCountry` y un `preferredLocale` opcional. Los valores se validan contra catálogos configurados, no mediante condicionales por país. El locale inicial se resuelve según D-005 y puede caer al locale de interfaz configurado, inicialmente `es`.

Los tipos y validadores sintácticos de identificador fiscal se registran mediante un resolver/adapter por código de país. Añadir otro país no modifica `Company` ni introduce ramas `if/switch` en el aggregate. Que un formato sea válido no demuestra existencia o representación legal.

La jurisdicción de una operación regulada se resuelve por separado. Un país empresarial sin `LegalPolicyPack` soportado nunca hereda las reglas españolas y no supera la puerta de alcance.

## Empresa ya existente

La identidad empresarial candidata se compara por país registral, tipo de identificador fiscal y fingerprint HMAC del valor normalizado. Esa coincidencia impide crear automáticamente otro tenant, pero no verifica jurídicamente a la empresa ni autoriza a reclamarla.

La respuesta pública permanece genérica. El solicitante deberá autenticarse como miembro autorizado o abrir un caso administrativo con evidencia independiente. Nunca se enlaza una empresa, cuenta o membership por coincidencia de nombre, dominio, correo o identificador fiscal.

### Blind index fiscal y rotación de claves

La clave de cifrado del identificador fiscal y las claves HMAC usadas como blind index serán distintas, externas al repositorio y rotables de forma independiente. `organization` conservará una fila de fingerprint por empresa y por versión HMAC activa; la unicidad se aplicará sobre `(registered_country, tax_identifier_type, key_version, fingerprint)`.

La configuración mantendrá un conjunto ordenado de versiones HMAC activas. Cada alta:

1. normaliza el identificador mediante la estrategia registrada para país y tipo;
2. calcula y consulta el fingerprint con todas las versiones activas sin exponer el resultado fuera de `organization`;
3. si puede crear la empresa, inserta atómicamente todas las filas activas junto con ella;
4. convierte cualquier conflicto concurrente en la misma respuesta pública `202`, sin lectura cross-tenant ni enumeración.

La rotación se ejecutará como ceremonia operativa verificable:

1. registrar la nueva clave como activa para lectura y escritura sin retirar la anterior;
2. hacer backfill idempotente de la nueva versión para todas las empresas;
3. mientras dure el backfill, consultar e insertar todas las versiones activas;
4. comprobar cobertura, ausencia de duplicados y comportamiento concurrente;
5. promover la nueva versión y retirar la anterior solo cuando todas las filas estén cubiertas y haya terminado su ventana operativa.

No se admitirá una nueva versión para altas si la aplicación no puede calcular también las demás versiones activas. Las pruebas cubrirán un duplicado anterior a la rotación, durante el backfill, después de promover la clave y dos altas concurrentes del mismo identificador.

### Admisión del correo empresarial

H03 exige que el dominio normalizado después de `@` no esté clasificado como proveedor público/común por la versión activa de `CompanyEmailAdmissionPolicy`, propiedad de `organization`. El catálogo será configuración tipada y versionada con identificador, fecha efectiva y estado; no se incluirán listas de dominios en código, enums o frontend.

La comparación será exacta sobre el dominio normalizado y nunca por substring. No se inspecciona el proveedor MX: una dirección bajo un dominio propio es admisible aunque el correo esté alojado por Google Workspace, Microsoft 365 u otra plataforma. Un dominio permitido produce únicamente `NOT_LISTED_AS_PUBLIC_PROVIDER` y no verifica la empresa ni la autoridad del solicitante.

El servidor aplica la política de forma autoritativa antes de crear el onboarding. Un dominio bloqueado devuelve una violación estable `company_email_domain_not_allowed`; una política ausente o inválida en un entorno que expone H03 falla de forma cerrada con `company_email_policy_unavailable`. Ambos mensajes visibles se resuelven mediante i18n. No existe excepción automática; una futura recuperación administrativa requerirá decisión y trazabilidad propias.

## Membresías y tenant activo

Una `Membership` relacionará `UserId`, `TenantId`, rol, estado y versión. Una persona podrá tener varias membresías activas. El primer miembro de una empresa usa el rol canónico `COMPANY_OWNER`.

Después del login:

- sin membresía empresarial, el actor opera como estudiante/global;
- con una membresía activa, puede seleccionarla como tenant activo;
- con varias, la UI exige seleccionar o recuerda de forma segura la última autorizada;
- cambiar de empresa valida la membresía vigente y emite un access token nuevo.

El adaptador de seguridad formará `CurrentActor` con `UserId`, tenant activo opcional, `MembershipId`, roles, versión de autorización y correlation ID. Los casos de uso recibirán este objeto o datos derivados; nunca aceptarán tenant o rol como autoridad desde el body o la URL.

## Frescura de permisos

El access token puede incluir tenant activo, membresía y roles para autorizar operaciones ordinarias. Su vida será de 10 minutos según ADR-007.

- Cambiar o revocar una membresía revoca las familias refresh relacionadas.
- Durante el Incremento 1, toda operación tenant-owned vuelve a consultar la membership vigente en aplicación y filtra por tenant en persistencia.
- Una discrepancia de versión de autorización obliga a renovar sesión.
- La ventana residual del access token no autoriza ninguna lectura o escritura tenant-owned de I1. Puede mantenerse únicamente para recursos globales que no dependan de una membership.

## Clasificación de recursos

| Clase | Autorización |
|---|---|
| Global público | Sin tenant; solo datos publicados expresamente. |
| Global autenticado | Por `UserId` y finalidad. |
| Student-owned | Por `UserId`; otro estudiante recibe respuesta no reveladora. |
| Tenant-owned | Por `TenantId` derivado y membresía vigente. |
| Administración interna | Caso de uso excepcional, rol interno y auditoría obligatoria. |

## Defensa en profundidad

1. Aggregates tenant-owned conservan `TenantId`.
2. Puertos de repositorio exigen `TenantId` en lecturas y escrituras.
3. SQL incluye `tenant_id` en predicates.
4. Constraints e índices de negocio incluyen `tenant_id` cuando el dato no sea global.
5. Eventos y jobs transportan tenant explícitamente.
6. Acceso cruzado se responde como recurso inexistente cuando revelar existencia sea sensible.
7. Cada historia tenant-owned incluye pruebas negativas en aplicación, HTTP y persistencia.

No se confiará exclusivamente en `ThreadLocal`, filtros Hibernate o un tenant enviado por el cliente. Row Level Security queda aplazado como defensa adicional, no sustituta.

## Consecuencias

- La cuenta global evita credenciales duplicadas y permite múltiples perfiles.
- El cambio de tenant requiere un endpoint y token nuevos.
- Las consultas son más explícitas, pero el aislamiento es comprobable.
- La unión segura de un nuevo perfil con una cuenta existente necesitará reautenticación o verificación de correo, nunca coincidencia silenciosa.

## Matriz mínima de pruebas negativas

- Usuario de empresa A no lee ni modifica recursos de B.
- Membership suspendida no selecciona tenant ni renueva una sesión empresarial.
- Un tenant del body no sustituye al del token.
- Estudiante A no accede a declaración académica de B.
- Cuenta sin tenant no usa endpoints empresariales.
- Administrador interno sin caso de uso explícito no atraviesa el aislamiento.

## Condiciones documentales de aceptación

- Producto, Seguridad y Arquitectura aceptan cuenta global, perfiles múltiples, `TenantId == CompanyId` y rol `COMPANY_OWNER`.
- D-005 y D-006 están alineadas con las fuentes superiores.
- La ceremonia de cuenta existente y la política de empresa duplicada no permiten enlace ni enumeración automática.
- La rotación HMAC conserva la unicidad fiscal mediante blind indexes para todas las versiones activas y un backfill verificable.
- D-007 y `CompanyEmailAdmissionPolicy` impiden correos de dominios públicos comunes sin hardcodear proveedores ni confundir admisión con verificación empresarial.

## Conformidad de la implementación

- Existe un solo identificador persistido para empresa/tenant.
- `CurrentActor` y selección de tenant están definidos en el contrato de seguridad.
- Las pruebas anteriores se asignan a historias concretas del plan.
- Las respuestas no permiten enumerar recursos ajenos.
- Las pruebas de rotación y alta concurrente demuestran que una nueva versión HMAC no permite crear otro tenant para el mismo identificador.
- Las pruebas de email cubren normalización, coincidencia exacta, dominio bloqueado, dominio propio alojado por un proveedor común, política ausente e i18n.

# ADR-003 — Multi-tenancy, identidad y tenant activo

- Estado: Propuesto — revisión 2
- Fecha: 2026-09-12
- Decisores: Producto, Seguridad y Arquitectura
- Reemplaza: propuesta inicial del ADR-003

## Contexto

La empresa es el tenant principal. Una persona puede colaborar con más de una empresa y también actuar como estudiante. El aislamiento no puede depender de IDs suministrados por el navegador ni de filtros ORM implícitos.

## Decisión de identidad

- `UserAccount` será global y su correo normalizado será único globalmente.
- Una cuenta podrá estar asociada a un `StudentProfile`, propiedad del contexto `student`, y a cero o más membresías empresariales.
- Los formularios de empresa y estudiante seguirán separados, aunque converjan en una cuenta existente mediante un flujo seguro que se diseñará en la historia correspondiente.
- Una universidad no será tenant ni tendrá cuenta obligatoria durante el MVP.

`StudentProfile` tendrá un `StudentId` propio y una referencia uno-a-uno a `UserId`. La asociación se crea únicamente mediante un onboarding autenticado o verificado; una coincidencia de correo nunca basta.

## Identificador tenant

Durante el MVP, el identificador del aggregate `Company` será el `TenantId`. No habrá `CompanyId` alternativo ni tabla `tenant`. La representación persistida y pública será un UUID.

Si en el futuro otra clase de organización se convierte en tenant, un ADR de migración introducirá el concepto generalizado; no se anticipará ahora.

## Membresías y tenant activo

Una `Membership` relacionará `UserId`, `TenantId`, rol, estado y versión. Una persona podrá tener varias membresías activas.

Después del login:

- sin membresía empresarial, el actor opera como estudiante/global;
- con una membresía activa, puede seleccionarla como tenant activo;
- con varias, la UI exige seleccionar o recuerda de forma segura la última autorizada;
- cambiar de empresa valida la membresía vigente y emite un access token nuevo.

El adaptador de seguridad formará `CurrentActor` con `UserId`, tenant activo opcional, `MembershipId`, roles, versión de autorización y correlation ID. Los casos de uso recibirán este objeto o datos derivados; nunca aceptarán tenant o rol como autoridad desde el body o la URL.

## Frescura de permisos

El access token puede incluir tenant activo, membresía y roles para autorizar operaciones ordinarias. Su vida será de 10 minutos según ADR-007.

- Cambiar o revocar una membresía revoca las familias refresh relacionadas.
- Las operaciones sensibles volverán a consultar la membresía vigente.
- Una discrepancia de versión de autorización obliga a renovar sesión.
- Se acepta como riesgo residual que un access token ordinario conserve permisos hasta 10 minutos; se revisará si aparecen operaciones de mayor impacto.

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

## Criterios de aceptación del ADR

- Existe un solo identificador persistido para empresa/tenant.
- `CurrentActor` y selección de tenant están definidos en el contrato de seguridad.
- Las pruebas anteriores se asignan a historias concretas del plan.
- Las respuestas no permiten enumerar recursos ajenos.

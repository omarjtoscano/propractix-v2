# ADR-006 — Separación dominio/JPA y propiedad de tablas

- Estado: Propuesto — revisión 2
- Fecha: 2026-09-12
- Decisores: Arquitectura y Datos
- Reemplaza: propuesta inicial del ADR-006

## Contexto

La arquitectura hexagonal requiere separar dominio y persistencia. Al usar una sola base y un único usuario técnico existe riesgo residual de acceso accidental entre contextos, por lo que la propiedad debe ser visible y comprobable.

## Decisión

Las entidades y value objects de dominio no tendrán anotaciones JPA. Cada adapter de persistencia definirá entidades JPA, repositorios Spring Data, SQL y mappers internos.

Se utilizará PostgreSQL 16 y un esquema de aplicación único durante el MVP. Las tablas se prefijarán con su propietario.

## Matriz de propiedad inicial

| Tabla | Propietario | Scope | Escritura permitida |
|---|---|---|---|
| `identity_user_account` | `identity` | Global/user-owned | Solo adapter de `identity`. |
| `identity_password_credential` | `identity` | User-owned | Solo `identity`; contenido nunca sale en contratos. |
| `identity_email_verification` | `identity` | User-owned | Solo `identity`; consumo atómico. |
| `identity_refresh_session` | `identity` | Usuario y tenant activo opcional | Solo `identity`. |
| `organization_company` | `organization` | Tenant-owned | Solo `organization`. |
| `organization_membership` | `organization` | Tenant-owned | Solo `organization`. |
| `organization_company_onboarding` | `organization` | Registro/tenant futuro | Solo `organization`; conserva referencia y evidencia mínima del aviso mostrado. |
| `student_profile` | `student` | Student-owned | Solo adapter de `student`. |
| `student_onboarding` | `student` | User/registro | Solo `student`; conserva referencia y evidencia mínima del aviso mostrado. |
| `student_academic_declaration` | `student` | Student-owned | Solo `student`; institución por ID escalar opcional. |
| `academicinstitution_institution` | `academicinstitution` | Global catalogado | Solo importador/administración del contexto. |
| `academicinstitution_catalog_import` | `academicinstitution` | Global técnico-funcional | Solo `academicinstitution`. |
| `compliance_privacy_notice` | `compliance` | Global/jurisdicción y finalidad | Solo `compliance`. |
| `platform_outbox_event` | `platform.outbox` | Mixto, tenant opcional | Relay y adapters autorizados. |
| `platform_event_consumption` | `platform.outbox` | Técnico | Consumidores idempotentes. |
| `platform_idempotency_record` | `platform.idempotency` | Actor/operación | Adapter HTTP de idempotencia. |
| `platform_security_audit` | `platform.audit` | Tenant/usuario opcional | Writer append-only; lectura administrativa. |

La matriz se ampliará en la migración de cada historia, nunca de forma implícita.

`compliance` publica mediante contrato de lectura qué aviso está `APPROVED` y vigente para una finalidad. El contexto que recopila los datos guarda en su propio onboarding el ID/versión del aviso, fecha de presentación y evidencia estrictamente necesaria. No escribe una aceptación central ni delega en `compliance` la transacción de registro.

## Relaciones y tenant

- No habrá asociaciones JPA entre contextos.
- Una referencia externa se persistirá como UUID escalar.
- Por defecto no se crean foreign keys entre contextos; una excepción requiere justificar ciclo de vida, orden de migración y fallo parcial.
- Tablas tenant-owned usan `tenant_id NOT NULL` y todos sus índices/constraints de negocio lo incluyen cuando la unicidad sea local al tenant.
- Tablas globales y student-owned documentan expresamente su política de autorización.

## Migraciones y catálogo

Flyway será la única autoridad del esquema. `ddl-auto` será `validate` fuera de tests unitarios. Las migraciones serán forward-only e inmutables tras aplicarse.

La estructura del catálogo se crea mediante Flyway, pero los datos institucionales se cargarán mediante importaciones versionadas con fuente, licencia/permiso de uso, fecha, hash y resultado. No se ocultará una actualización de catálogo dentro de una migración de esquema. Fixtures locales/test no se promoverán a producción.

## Riesgo residual

El único usuario de base de datos tiene capacidad técnica para acceder a todas las tablas. Se mitiga con:

- paquetes y adapters propietarios;
- ArchUnit;
- revisión de SQL y migraciones;
- tests de aislamiento;
- permisos de aplicación mínimos;
- prohibición de repositorios compartidos.

Separar schemas, usuarios o activar RLS se evaluará cuando el riesgo o escala lo justifiquen.

## Alternativas consideradas

- Entidad única para dominio/JPA/JSON: rechazada.
- Schema por contexto: aplazado por coste operativo inicial.
- Acceso directo de lectura entre tablas: rechazado; se usan contratos públicos o read models autorizados.
- `ddl-auto=update`: rechazado.

## Consecuencias

- Habrá mappers y pruebas de persistencia explícitos.
- Los cambios entre contextos se coordinan sin relaciones ORM.
- La propiedad y sensibilidad de cada tabla son auditables.
- Importar catálogo exige un proceso gobernado separado del versionado del esquema.

## Criterios de aceptación del ADR

- Cada tabla nueva amplía la matriz de propiedad o una equivalente versionada.
- No existen JPA entities fuera de adapters de persistencia.
- Toda tabla tenant-owned tiene constraints e índices tenant-aware comprobados.
- Migraciones funcionan desde una base vacía y no mezclan importaciones de producción no gobernadas.

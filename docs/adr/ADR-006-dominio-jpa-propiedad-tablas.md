# ADR-006 — Separación entre dominio, JPA y propiedad de tablas

- Estado: Propuesto
- Fecha: 2026-09-12
- Decisores: Arquitectura y Datos

## Contexto

La arquitectura hexagonal requiere que el dominio no dependa del modelo de persistencia. Además, cada bounded context debe conservar la propiedad de sus datos aunque la aplicación utilice una sola base de datos.

## Decisión

Las entidades y value objects de dominio no tendrán anotaciones JPA. Cada adapter de persistencia definirá sus propias entidades JPA, repositorios Spring Data y mappers.

Se utilizará una base PostgreSQL 16 y un esquema de aplicación único durante el MVP. Cada tabla tendrá un único contexto propietario y un nombre que lo haga visible, por ejemplo:

```text
identity_user_account
identity_refresh_session
organization_company
organization_membership
academicinstitution_institution
platform_outbox_event
```

Solo el contexto propietario podrá escribir su tabla. Otro contexto utilizará su contrato público y conservará únicamente el identificador externo necesario. No habrá asociaciones JPA entre entidades de contextos diferentes.

Flyway será la única autoridad del esquema. Las migraciones serán forward-only, inmutables tras aplicarse y compatibles con despliegues graduales cuando exista una actualización en producción.

Las consultas complejas podrán usar JDBC en adapters de lectura sin devolver entidades JPA al dominio.

## Alternativas consideradas

### Una entidad para dominio, JPA y JSON

Rechazada por acoplamiento a persistencia y transporte.

### Esquema PostgreSQL por bounded context

Aplazado. Refuerza propiedad, pero complica Flyway, permisos y consultas antes de que el beneficio esté demostrado.

### `ddl-auto=update`

Rechazado. Los entornos compartidos usarán `ddl-auto=validate` y Flyway.

## Consecuencias

- Habrá mappers explícitos y pruebas de persistencia.
- No se permitirán accesos directos a repositorios de otro contexto.
- Los cambios entre contextos se coordinan mediante contratos, no mediante relaciones ORM.
- La convención de nombres facilitará auditoría y una posible separación futura.

## Criterios de aprobación

- Aceptar JPA solo en adapters.
- Aceptar un esquema con tablas prefijadas por contexto para el MVP.
- Aceptar Flyway forward-only y `ddl-auto=validate`.

# ADR-003 — Modelo multi-tenant y propagación de contexto

- Estado: Propuesto
- Fecha: 2026-09-12
- Decisores: Producto, Seguridad y Arquitectura

## Contexto

ProPractix está orientada a empresas. La auditoría de V1 encontró operaciones sobre ofertas sin comprobar pertenencia al tenant. El aislamiento no puede depender de que cada controlador recuerde añadir una validación.

## Decisión

`Organization` será el tenant empresarial. Toda información propiedad de una empresa incluirá un `TenantId` obligatorio.

El adaptador de seguridad resolverá un `CurrentActor` desde la sesión autenticada. El caso de uso recibirá explícitamente el tenant autorizado; nunca confiará en un `tenantId`, `organizationId` o rol enviado por el cliente.

Se aplicará defensa en profundidad:

1. Los aggregates tenant-owned conservan su `TenantId`.
2. Los puertos de repositorio exigen `TenantId` en búsquedas y modificaciones.
3. Las consultas SQL incluyen el tenant en el predicado.
4. Las restricciones de unicidad de negocio incluyen el tenant cuando corresponda.
5. Cada historia tenant-owned incluye una prueba negativa de acceso cruzado.
6. Eventos y trabajos asíncronos transportan el tenant explícitamente.

No se utilizará un `ThreadLocal` como única fuente de contexto, porque puede perderse o filtrarse en ejecución asíncrona. Tampoco se confiará exclusivamente en filtros globales de Hibernate.

Los estudiantes tendrán una identidad propia global y no pertenecerán automáticamente al tenant de una empresa. Su relación con una empresa se producirá mediante recursos del contexto correspondiente en incrementos posteriores.

Las universidades no serán tenants en el MVP.

## Alternativas consideradas

### Base de datos por empresa

Rechazada para el MVP por complejidad operativa y de migraciones.

### Esquema PostgreSQL por empresa

Rechazado por el número potencial de tenants y el coste de administración.

### Discriminador implícito mediante filtro ORM

Rechazado como única barrera porque consultas nativas, jobs o errores de configuración podrían omitirlo.

### Row Level Security desde el primer incremento

Aplazada. Podrá añadirse como protección adicional tras disponer de una estrategia probada de conexión y contexto; no sustituirá las comprobaciones de aplicación.

## Consecuencias

- Las firmas de repositorio serán más explícitas.
- Los índices y constraints deberán diseñarse con `tenant_id`.
- Los administradores internos usarán casos de uso excepcionales y auditados, no un bypass general.
- Un intento de acceso cruzado se responderá sin revelar la existencia del recurso.

## Criterios de aprobación

- Confirmar que la empresa es el único tenant principal del MVP.
- Confirmar que estudiantes y universidades no son tenants empresariales.
- Aceptar el paso explícito de `TenantId` en aplicación y persistencia.

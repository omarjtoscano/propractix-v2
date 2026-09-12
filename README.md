# ProPractix V2

Repositorio de ProPractix V2, reconstruido como monolito modular con Domain-Driven Design y arquitectura hexagonal.

## Estructura

- `server`: backend Java 21 y Spring Boot.
- `client`: frontend React/TypeScript; se creará durante el Incremento 1 tras aprobar el plan.
- `docs`: blueprints, auditoría, ADR y planes de incremento.
- `infra`: servicios para desarrollo local.
- `.github/workflows`: validación continua, cuando se implemente la primera historia.

## Requisitos locales

- JDK 21.
- Docker con Docker Compose v2.
- Node.js 24 LTS y npm 11.

## Comandos

```bash
npm run infra:up
npm run server:test
npm run server:run
```

Consulta [docs/README.md](docs/README.md) antes de implementar y `AGENTS.md` antes de encargar cambios a Codex.

## Estado actual

- Fundación del repositorio preparada.
- ADR-001 a ADR-008 propuestos, pendientes de aprobación.
- Plan técnico del Incremento 1 propuesto, pendiente de aprobación.
- No se ha implementado todavía ninguna historia funcional.

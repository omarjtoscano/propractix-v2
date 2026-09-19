# ProPractix V2

Repositorio de ProPractix V2, reconstruido como monolito modular con Domain-Driven Design y arquitectura hexagonal.

## Estructura

- `server`: backend Java 21 y Spring Boot.
- `client`: shell técnico React/TypeScript sin capacidades de negocio en I1-H01.
- `docs`: blueprints, auditoría, ADR y planes de incremento.
- `infra`: servicios para desarrollo local.
- `.github/workflows`: CI, construcción OCI y despliegue de staging protegido.

## Requisitos locales

- JDK 21.
- Docker con Docker Compose v2.
- Node.js 24 LTS y npm 11.

## Comandos

```bash
npm run infra:up
npm run infra:backup-restore-smoke
npm run server:test
npm run server:run
npm run check
```

Consulta [docs/README.md](docs/README.md) antes de implementar y `AGENTS.md` antes de encargar cambios a Codex.

## Estado actual

- ADR-001 a ADR-009 y el plan del Incremento 1 están aprobados.
- I1-H01 implementa la fundación técnica reproducible.
- No existe todavía ninguna historia funcional ni tabla de negocio.
- `CL0_CLOUD_STAGING` continúa pendiente: no se ha desplegado infraestructura AWS.

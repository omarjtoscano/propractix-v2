# Aceptación de I1-H01 — Fundación ejecutable mínima

- Fecha: 2026-09-20
- Historia: `I1-H01` — Fundación ejecutable mínima
- Estado: `DONE / ACCEPTED`
- Pull request: [#1](https://github.com/omarjtoscano/propractix-v2/pull/1)
- Commit final de la rama: `be7d4bf`
- Merge en `main`: `a88f24a`

## Evidencia de aceptación

| Comprobación | Resultado |
|---|---|
| CI backend | `success` |
| Cliente, OpenAPI e i18n | `success` |
| ArchUnit | `success` |
| PostgreSQL 16 y Flyway | `success` |
| Mailpit sin relay externo | `success` |
| Secretos locales mediante config tree | `success` |
| Backup/restore local sintético | `success` |
| Gitleaks | `success` |
| Trivy `CRITICAL/HIGH` | `success` |
| Build OCI AMD64 y ARM64 | `success` |
| Manifiesto y artefactos OCI | `success` |

## Alcance cloud

AWS no se utilizó para aceptar I1-H01. No se declara `staging` desplegado y
`CL0_CLOUD_STAGING` permanece `PENDING`, conforme a ADR-009 y ADR-012.

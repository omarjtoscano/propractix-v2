# Resolución de Producto — Correo empresarial y despliegue cloud

- Fecha: 2026-09-12
- Estado: `APPROVED`
- Plan resultante: Incremento 1, revisión 5
- ADR actualizado: ADR-003, revisión 5
- ADR nuevo: ADR-009, revisión 1

## 1. Decisiones aprobadas

### Correo empresarial

- `CompanyEmailAdmissionPolicy` sigue siendo la regla determinista y versionada que rechaza proveedores públicos/comunes.
- Una política ausente o inválida mantiene H03 cerrada.
- La comprobación de ruta de correo es el control técnico principal después de la política.
- `MAIL_CAPABLE` continúa; `NO_MAIL_ROUTE` mantiene la solicitud pendiente de verificación y programa una nueva comprobación.
- Timeout, `SERVFAIL`, caída del resolver o fallo equivalente producen `INDETERMINATE`; tampoco bloquean la solicitud y activan reintentos.
- Verificar el enlace es la prueba definitiva de control de la dirección y puede resolver un resultado técnico indeterminado.
- La admisión y la ruta de correo no verifican jurídicamente a la empresa, que permanece `SELF_DECLARED`.

La decisión se documenta en D-007 y ADR-003. ADR-006 no se modifica porque su responsabilidad es la separación dominio/JPA y la propiedad de tablas.

### Despliegue incremental

- Cada incremento aprobado se despliega en un único entorno AWS `staging`.
- H01 incorpora imágenes OCI, Docker Compose, manifiesto inmutable, GitHub Actions, OIDC, migraciones, health checks, smoke tests, backup/restore y rollback.
- La primera topología es económica, de un solo host y no representa producción de alta disponibilidad.
- AWS permanece fuera del dominio y la release conserva portabilidad OCI/Compose.
- Producción no se aprovisiona en el Incremento 1.

## 2. Gates

| Gate | Estado | Resolución |
|---|---|---|
| `D0_DECISIONS` | `CLOSED` | ADR-001 a ADR-009 están aceptados. |
| `B0_COMPANY_EMAIL_POLICY` | `PENDING` | V1 ya fue contrastado y existe un seed documental; falta revisar cobertura, publicar la versión y demostrar tests/rollback. |
| `CL0_CLOUD_STAGING` | `PENDING` | H01 puede preparar artefactos, pero necesita cuenta/modalidad AWS, MFA, región, presupuesto, OIDC, secretos, URL/TLS y backup antes del despliegue real. |

Los demás gates del plan conservan su estado. Esta resolución no aprueba ML-15, proveedor de correo, catálogo universitario, rate limits ni uso de datos personales.

## 3. Alcance autorizado

I1-H01 continúa siendo la única historia `READY`. La documentación cloud amplía su fundación técnica, pero no autoriza H02, H03 ni historias posteriores. No se ha aprovisionado AWS ni implementado funcionalidad de registro mediante esta resolución.

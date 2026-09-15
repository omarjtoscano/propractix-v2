# Resolución de Producto — Estrategia local y cloud, revisión 7

- Fecha: 2026-09-15
- Estado: `APPROVED`
- Plan resultante: Incremento 1, revisión 7
- ADR actualizado: ADR-009, revisión 3
- ADR nuevo: ADR-012, revisión 1
- Mantiene: ADR-011, revisión 1
- Reemplaza: precisiones locales/cloud de `resolucion-cloud-staging-revision-6.md`

## Correcciones aprobadas

1. La EC2 recibe IPv4 pública dinámica para egress porque la baseline no contiene NAT, IPv6 ni endpoints VPC. EIP permanece deshabilitada y el Security Group no tiene ingress.
2. `staging` usa exclusivamente `staging-synthetic-privacy-fixture-v1.yaml`; se prohíben datos personales reales, datos derivados de producción, mezcla, exportación y promoción a producción.
3. H01–H05 se desarrollan y aceptan sin AWS. El perfil local usa PostgreSQL 16, Mailpit y secretos sintéticos montados como archivos/config tree. LocalStack solo existe en el perfil opcional `aws-contract`.
4. Los documentos no contienen correos personales para alertas; usan `BUDGET_ALERT_EMAIL` como configuración externa.
5. Las acciones que AWS identifica como conversión automática del Free Plan a Paid Plan son condiciones de parada explícitas.
6. EBS conserva 16 GiB iniciales con umbrales 70/80/90 %, rotación acotada de logs y limpieza que protege PostgreSQL y las imágenes activa/anterior.
7. `main` se creará desde el último commit revisado de la rama del incremento, será estable/predeterminada y estará protegida; esta resolución solo documenta la acción administrativa, no la ejecuta.
8. AGENTS, índice documental, plan del Incremento 1, plan AWS y checklist CL0 quedan alineados con estas decisiones.

## Efecto sobre la ejecución

- I1-H01 continúa siendo la única historia actualmente `READY`.
- H01–H05 se cierran por aceptación local respetando sus gates y predecesoras.
- AWS se usa después para promover y verificar H05; no determina su aceptación funcional.
- La fixture de staging no constituye aprobación de ML-15 ni habilita datos personales reales.
- Crear/proteger `main`, mutar AWS, ejecutar `tofu apply`, convertir la cuenta a Paid Plan o abrir ingress requiere una acción posterior y autorización explícita.

## Evidencia requerida posteriormente

- aceptación E2E de H05 con AWS deshabilitado;
- plan OpenTofu que muestre IPv4 dinámica, sin EIP ni ingress;
- comprobación de `BUDGET_ALERT_EMAIL` sin valor versionado;
- prueba de stop conditions del Free Plan;
- evidencia de versión de fixture, aislamiento y limpieza;
- métricas de disco y conservación segura de la release anterior;
- SHA de origen, configuración predeterminada y protección de `main`.

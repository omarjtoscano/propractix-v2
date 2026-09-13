# Resolución de Producto — Baseline de `CL0_CLOUD_STAGING`

- Fecha: 2026-09-13
- Estado: `APPROVED`
- Plan resultante: Incremento 1, revisión 6
- ADR actualizado: ADR-009, revisión 2
- ADR nuevo: ADR-011, revisión 1
- Reemplaza: las decisiones cloud de `resolucion-correo-cloud-revision-5.md`; las decisiones de correo permanecen vigentes

## Decisiones aprobadas

1. Baseline en `eu-north-1`: una `t4g.small` ARM64, EBS `gp3`, Docker Compose, PostgreSQL local al host, S3, Parameter Store Standard, ECR privado y OpenTofu.
2. Red sin EIP ni ingress. Una IPv4 pública dinámica solo puede usarse para egress/SSM, con coste revisado y cubierto por créditos mientras la cuenta siga en Free Plan.
3. Acceso del único tester mediante SSM/port forwarding; DNS y HTTPS público se difieren hasta H05.
4. GitHub OIDC usa el subject inmutable restringido al Environment `staging`; el claim real se verifica antes de crear la trust policy definitiva.
5. Objetivo de desembolso aproximado 0 EUR y techo 20 EUR/mes. No se activa Paid Plan ni recursos de coste relevante sin aprobación.
6. `CL0` transita `PENDING → READY_FOR_APPLICATION → CLOSED`; solo se cierra después de desplegar y verificar H05.
7. H01 se cierra por aceptación local y puede habilitar historias posteriores conforme a sus gates; el despliegue cloud deja de ser precondición de su cierre.

## Precisión de coste incorporada

La documentación oficial revisada al aprobar esta resolución indica que Free Plan consume créditos para EC2 elegible y no incluye las ofertas de corta duración del Paid Plan. La prueba específica de 750 horas de `t4g.small` no se contabiliza como gratuidad de la cuenta Free Plan. Asimismo, una IPv4 pública dedicada cuesta USD 0,005/h en la fecha de corte.

Por tanto, “≈0 EUR” significa desembolso cubierto por créditos, no ausencia de coste medido. Antes de aplicar se revisará el consumo estimado y se detendrá la ejecución si contradice el techo o la modalidad aprobada.

## Alcance autorizado

Esta resolución autoriza documentación y futura preparación controlada. No crea infraestructura, no ejecuta OpenTofu, no activa Paid Plan, no abre puertos públicos y no autoriza datos reales.

I1-H01 continúa siendo la única historia actualmente `READY`. Las historias posteriores solo se autorizan al cerrar H01 localmente y sus gates/predecesoras respectivas.

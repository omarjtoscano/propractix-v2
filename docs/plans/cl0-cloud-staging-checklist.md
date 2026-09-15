# Checklist — `CL0_CLOUD_STAGING`

- Estado actual: `PENDING`
- Fecha de corte: 2026-09-15
- Owners: Producto, Arquitectura, Operaciones y Seguridad
- Referencias: ADR-009 revisión 3, ADR-011 revisión 1, ADR-012 revisión 1

Marcar un elemento requiere evidencia verificable; aprobar una decisión no equivale a ejecutar el control.

## `PENDING` → `READY_FOR_APPLICATION`

### Cuenta y coste

- [ ] Root MFA habilitado.
- [ ] Root sin access keys.
- [ ] Free Plan y saldo de créditos comprobados.
- [ ] Región `eu-north-1` confirmada en CLI/console.
- [ ] Moneda efectiva de billing registrada.
- [ ] Budget equivalente a 20 EUR configurado.
- [ ] `BUDGET_ALERT_EMAIL` se proporciona externamente, no aparece en Git/state y recibe alertas 50 %, 80 %, 100 % y forecast.
- [ ] Costes actuales de EC2, IPv4, EBS, ECR, S3 y transferencia revisados.
- [ ] Ningún paso convierte la cuenta a Paid Plan.
- [ ] El propietario no ha seleccionado `Upgrade Plan`.
- [ ] La cuenta no crea ni se une a AWS Organizations y no configura AWS Control Tower.
- [ ] La cuenta no se une a AWS Partner Network, Professional Services ni Enterprise Agreement.
- [ ] La cuenta no compra AWS Skill Builder Team ni se designa HIPAA/SEC compliant.
- [ ] Cualquiera de las acciones anteriores detiene el bootstrap y exige una nueva aprobación explícita.

### Repositorio

- [ ] `main` se crea desde el último commit revisado de `increment/01-company-identity-catalog`.
- [ ] `main` queda configurada como rama predeterminada.
- [ ] `main` exige pull request y checks; push directo, force push y borrado están bloqueados.
- [ ] La creación/configuración de `main` se registra como acción administrativa separada y no como efecto de OpenTofu.

### OpenTofu y state

- [ ] `bootstrap` y `staging` tienen roots y states separados.
- [ ] Bucket de state privado, cifrado y versionado.
- [ ] Locking S3 nativo habilitado con `use_lockfile=true`.
- [ ] State inicial migrado; no quedan copias locales ni state en Git.
- [ ] `.gitignore` cubre state, planes y overrides locales.
- [ ] `tofu fmt -check` y `tofu validate` pasan.
- [ ] Plan revisado sin recursos fuera de baseline.
- [ ] Destruir `staging` no alcanza `bootstrap`.

### IAM y OIDC

- [ ] Claim real de GitHub OIDC observado sin registrar el token completo.
- [ ] `sub` restringe owner ID, repository ID y Environment `staging`.
- [ ] `aud=sts.amazonaws.com` validado.
- [ ] Trust policy sin comodines de owner/repositorio.
- [ ] Instance Role limitado a SSM, ECR pull, parámetros y S3 necesarios.
- [ ] Deploy Role limitado a ECR push, manifiestos y comandos SSM de staging.
- [ ] Deploy Role sin IAM, red, creación/terminación EC2, secretos ni backups.
- [ ] Políticas validadas con IAM Access Analyzer.
- [ ] No existen access keys permanentes para GitHub o EC2.

### Red, host y datos

- [ ] Sin Elastic IP.
- [ ] Security Group sin reglas inbound.
- [ ] Puertos 22, 80, 443 y 5432 no públicos.
- [ ] IPv4 pública dinámica asignada para egress; sin NAT, IPv6 o endpoints VPC la baseline no funciona sin ella.
- [ ] Coste de IPv4 dinámica revisado y acceso entrante nuevamente comprobado como inexistente.
- [ ] Session Manager y Run Command funcionan.
- [ ] EBS `gp3` cifrado mantiene 16 GiB iniciales.
- [ ] Ocupación de disco se registra antes/después de desplegar: aviso 70 %, bloqueo 80 %, incidente 90 %.
- [ ] Docker rota logs con `max-size=10m` y `max-file=3`; la aplicación no conserva logs locales ilimitados.
- [ ] Solo se limpian capas/imágenes no referenciadas conservando release activa y anterior; nunca volúmenes PostgreSQL.
- [ ] Dumps temporales se borran solo después de verificar la copia en S3.
- [ ] Docker Compose válido y sin secretos versionados.
- [ ] ECR push/pull ARM64 sintético probado.
- [ ] Parameter Store Standard creado sin pasar secretos por OpenTofu.
- [ ] Se carga exclusivamente `staging-synthetic-privacy-fixture-v1.yaml` y la evidencia registra su versión.
- [ ] No existen datos personales reales, datos derivados de producción, datasets mezclados ni procedencia desconocida.
- [ ] La fixture no puede promoverse, exportarse ni restaurarse en producción y no envía correo externamente.
- [ ] Backup/restore sintético hacia/desde S3 verificado.
- [ ] Rollback entre dos manifiestos sintéticos verificado.
- [ ] Tags y retenciones mínimas comprobados.

Cuando todos los puntos anteriores estén completos, registrar:

- [ ] `CL0_CLOUD_STAGING = READY_FOR_APPLICATION` con fecha y evidencia.

## `READY_FOR_APPLICATION` → `CLOSED`

- [ ] H05 completada y aceptada localmente.
- [ ] H01–H05 tienen aceptación local completa sin depender de AWS, según ADR-012.
- [ ] Gates de H03–H05 requeridos para datos/servicios reales están cerrados o se usa exclusivamente la fixture sintética aprobada.
- [ ] Mecanismo de acceso del tester decidido: SSM port forwarding o exposición pública aprobada.
- [ ] Si existe exposición pública, DNS/TLS y Security Group tienen revisión específica.
- [ ] Manifiesto H05 aprobado por GitHub Environment `staging`.
- [ ] Imágenes desplegadas por digest, no por `latest`.
- [ ] Flyway termina correctamente.
- [ ] Readiness y smoke tests pasan.
- [ ] Recorrido navegable empresarial de H05 se verifica con datos sintéticos.
- [ ] Rollback a la release anterior se prueba sin down migration.
- [ ] Backup/restore se vuelve a comprobar con el esquema de H05.
- [ ] Coste y créditos restantes se revisan después de la prueba.
- [ ] Evidencia `DEPLOYED_AND_VERIFIED` registrada.
- [ ] `CL0_CLOUD_STAGING = CLOSED` con fecha y responsables.

## Revisiones temporales

- [ ] 2026-12-31: revisar fin de la prueba T4g del Paid Plan; no asumir que cubre Free Plan.
- [ ] Antes de 2027-03-13: decidir destruir recursos, exportar datos sintéticos necesarios o convertir manualmente a Paid Plan.
- [ ] Antes de cada `tofu apply`: volver a validar precios, créditos, elegibilidad y recursos del plan.

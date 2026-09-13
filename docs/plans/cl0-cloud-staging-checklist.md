# Checklist — `CL0_CLOUD_STAGING`

- Estado actual: `PENDING`
- Fecha de corte: 2026-09-13
- Owners: Producto, Arquitectura, Operaciones y Seguridad
- Referencias: ADR-009 revisión 2, ADR-011 revisión 1

Marcar un elemento requiere evidencia verificable; aprobar una decisión no equivale a ejecutar el control.

## `PENDING` → `READY_FOR_APPLICATION`

### Cuenta y coste

- [ ] Root MFA habilitado.
- [ ] Root sin access keys.
- [ ] Free Plan y saldo de créditos comprobados.
- [ ] Región `eu-north-1` confirmada en CLI/console.
- [ ] Moneda efectiva de billing registrada.
- [ ] Budget equivalente a 20 EUR configurado.
- [ ] Alertas 50 %, 80 %, 100 % y forecast llegan a `omarjtoscano@outlook.com`.
- [ ] Costes actuales de EC2, IPv4, EBS, ECR, S3 y transferencia revisados.
- [ ] Ningún paso convierte la cuenta a Paid Plan.

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
- [ ] Necesidad y coste de IPv4 dinámica revisados antes de habilitarla.
- [ ] Session Manager y Run Command funcionan.
- [ ] EBS `gp3` cifrado y tamaño inicial revisado.
- [ ] Docker Compose válido y sin secretos versionados.
- [ ] ECR push/pull ARM64 sintético probado.
- [ ] Parameter Store Standard creado sin pasar secretos por OpenTofu.
- [ ] Backup/restore sintético hacia/desde S3 verificado.
- [ ] Rollback entre dos manifiestos sintéticos verificado.
- [ ] Tags y retenciones mínimas comprobados.

Cuando todos los puntos anteriores estén completos, registrar:

- [ ] `CL0_CLOUD_STAGING = READY_FOR_APPLICATION` con fecha y evidencia.

## `READY_FOR_APPLICATION` → `CLOSED`

- [ ] H05 completada y aceptada localmente.
- [ ] Gates de H03–H05 requeridos para la prueba están cerrados o se usan exclusivamente fixtures sintéticos aprobados.
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

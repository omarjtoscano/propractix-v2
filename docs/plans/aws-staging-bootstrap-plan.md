# Plan operativo — Bootstrap de AWS staging

- Estado: Aprobado para preparación documental; ejecución pendiente
- Revisión: 2
- Fecha: 2026-09-15
- ADR: ADR-009 revisión 3, ADR-011 revisión 1 y ADR-012 revisión 1
- Gate: `CL0_CLOUD_STAGING = PENDING`

## Objetivo

Preparar un `staging` de una persona, reproducible y con desembolso aproximado de 0 EUR mientras haya créditos. El plan no autoriza por sí solo `tofu apply`, conversión a Paid Plan ni exposición pública.

## Baseline aprobada

| Área | Valor |
|---|---|
| Región | `eu-north-1` |
| Compute | 1 × `t4g.small`, ARM64, Amazon Linux 2023, CPU credits `standard` |
| Disco | 1 × EBS `gp3` cifrado, 16 GiB iniciales |
| Runtime | Docker Compose: Caddy, cliente, Spring Boot, PostgreSQL 16 |
| IaC | OpenTofu; roots `bootstrap` y `staging` |
| OCI | ECR privado, repositorios `server` y `client`, tags inmutables |
| Configuración | Parameter Store Standard; `SecureString` donde corresponda |
| Operación | SSM Session Manager y Run Command |
| Backups | `pg_dump` a S3 privado |
| Entrada | Ninguna hasta nueva decisión para H05 |
| Salida | IPv4 pública dinámica requerida por esta baseline, solo para egress |
| Presupuesto | objetivo ≈0 EUR; techo 20 EUR/mes |

## Fases

### Fase 0 — Controles humanos de cuenta y repositorio

- comprobar MFA de root;
- comprobar que root no tiene access keys;
- confirmar que la cuenta sigue en Free Plan y revisar créditos disponibles;
- confirmar región y moneda efectiva de billing;
- comprobar que ninguna acción vaya a convertir la cuenta a Paid Plan;
- comprobar que el plan no crea/une AWS Organizations, Control Tower, AWS Partner Network, Professional Services, Enterprise Agreement, Skill Builder Team ni designación HIPAA/SEC;
- antes del primer flujo de promoción, crear `main` desde el último commit revisado de `increment/01-company-identity-catalog`, establecerla como rama predeterminada y protegerla contra push directo, force push y borrado;
- conservar el desarrollo en ramas y exigir pull request y checks para integrar en `main`.

La creación y configuración de `main` es una acción administrativa GitHub futura y explícita; este plan documental no la ejecuta.

Salida: controles de cuenta y gobierno de rama evidenciados sin compartir contraseñas, tokens ni capturas sensibles.

### Fase 1 — Bootstrap OpenTofu

- crear estructura `infra/aws/bootstrap`;
- crear bucket de state con bloqueo público, cifrado, versionado y locking nativo;
- migrar el state inicial local al backend S3;
- crear o reutilizar de forma controlada el proveedor OIDC GitHub;
- validar que `tofu destroy` de `staging` no alcanza recursos fundacionales.

Salida: backend remoto operativo y bootstrap independiente.

### Fase 2 — Guardrails de coste

- crear Budget con alertas 50 %, 80 %, 100 % y forecast;
- proporcionar `BUDGET_ALERT_EMAIL` por configuración externa y confirmar su suscripción si AWS lo solicita, sin versionar el valor;
- estimar por separado EC2, IPv4, EBS, ECR, S3 y transferencia;
- registrar fechas 2026-12-31 y 2027-03-13 como revisiones operativas, sin asumir promociones incompatibles con Free Plan.

Salida: coste visible antes de mantener recursos activos.

### Fase 3 — Identidad y registro

- crear ECR privado para `server` y `client`, tags inmutables y lifecycle corto;
- definir Instance Role con SSM, pull ECR, lectura de parámetros y acceso S3 mínimo;
- ejecutar workflow diagnóstico OIDC para observar `sub` y `aud` reales;
- crear trust policy exacta para el repositorio y Environment `staging`;
- definir rol de deploy separado, sin permisos IaC ni lectura de secretos/backups.

Salida: autenticación temporal probada, sin access keys permanentes.

### Fase 4 — Host mínimo

- crear VPC, una subnet, rutas y Security Group sin ingress;
- crear `t4g.small` con EBS cifrado y sin EIP;
- asignar IPv4 pública dinámica: sin NAT, IPv6 ni endpoints VPC es la ruta requerida para egress hacia SSM, ECR y repositorios de paquetes;
- instalar/configurar Docker y SSM sin secretos en user data;
- probar Session Manager y Run Command;
- configurar Docker con `json-file`, `max-size=10m` y `max-file=3`; la aplicación escribe logs a `stdout`;
- comprobar disco con `df` y uso Docker antes/después de cada promoción: advertencia al 70 %, bloqueo de despliegue al 80 % e incidente al 90 %;
- conservar imágenes de la release activa y la inmediatamente anterior; eliminar solo imágenes/capas no referenciadas, sin `docker system prune --volumes` ni limpieza del volumen PostgreSQL;
- eliminar dumps temporales del host después de verificar su copia S3;
- detener la instancia cuando no esté siendo utilizada si la persistencia y las pruebas lo permiten.

Salida: `CL0_CLOUD_STAGING = READY_FOR_APPLICATION` después de completar todas las pruebas sintéticas aplicables.

### Fase 5 — Validación sintética

- cargar exclusivamente `docs/policies/staging-synthetic-privacy-fixture-v1.yaml` y registrar su versión en la evidencia;
- rechazar cualquier dataset real, anonimizado desde producción, mezclado o de procedencia desconocida;
- push/pull de una imagen ARM64 inocua por ECR;
- despliegue sintético por SSM usando manifiesto SHA/digest;
- readiness y smoke test internos;
- fallo controlado y rollback a manifiesto anterior;
- `pg_dump`, subida S3, restauración en PostgreSQL vacío y verificación;
- confirmar ausencia de ingress, secretos en Git/state y permisos excesivos.

Salida: baseline lista para recibir H05, todavía sin cerrar `CL0`.

### Fase 6 — Primer despliegue navegable

- completar H05 y sus gates en local;
- decidir port forwarding o exposición pública, DNS y HTTPS;
- promover el manifiesto aprobado mediante GitHub Environment;
- ejecutar Flyway, readiness, smoke y rollback controlado;
- verificar el recorrido de creación/verificación/login/mi empresa con la fixture sintética de staging, sin entrega de correo externa;
- registrar evidencia `DEPLOYED_AND_VERIFIED`.

Salida: `CL0_CLOUD_STAGING = CLOSED`.

## Responsabilidades

| Acción | Responsable |
|---|---|
| Root MFA, root keys, Free→Paid | Propietario |
| Revisar y aprobar planes OpenTofu | Propietario + Arquitectura |
| Crear recursos declarados | OpenTofu |
| Introducir valores sensibles | Propietario por canal operativo |
| Build OCI y push ECR | GitHub Actions mediante OIDC |
| Aprobar Environment `staging` | Propietario |
| Deploy, Flyway y checks | Workflow vía SSM |
| Autorizar destroy | Propietario |
| Crear/proteger `main` y establecerla como predeterminada | Propietario del repositorio |

## Stop conditions

No se aplica infraestructura si ocurre cualquiera de estos casos:

- root MFA ausente o root access keys existentes;
- selección manual de “Upgrade Plan” o cualquier conversión involuntaria a Paid Plan;
- crear o unirse a AWS Organizations;
- configurar AWS Control Tower;
- unirse a AWS Partner Network;
- crear un contrato de AWS Professional Services o adherirse a un Enterprise Agreement;
- comprar una suscripción AWS Skill Builder Team;
- designar la cuenta como HIPAA o SEC compliant;
- coste previsto por encima de 20 EUR/mes;
- servicio no disponible en Free Plan;
- trust OIDC con comodines o claim real no verificado;
- plan con secretos, `AdministratorAccess`, NAT Gateway, RDS, ALB, ECS/EKS o recursos no aprobados;
- reemplazo o destrucción no explicados;
- datos reales, anonimizados desde producción, mezclados o de procedencia desconocida en `staging`;
- EBS al 80 % o más sin limpieza segura o ampliación aprobada;
- intento de limpiar volúmenes, la imagen activa o la imagen de rollback.

## Referencias operativas

- [AWS Free Tier FAQs](https://aws.amazon.com/free/free-tier-faqs/), consultado el 2026-09-15: duración del Free Plan y acciones que provocan conversión automática a Paid Plan.
- ADR-009 revisión 3 mantiene la fuente de precios y exige volver a validarlos antes de cualquier aprovisionamiento.

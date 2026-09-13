# ADR-011 — OpenTofu y bootstrap de AWS staging

- Estado: Aceptado — revisión 1
- Fecha: 2026-09-13
- Decisores: Producto, Arquitectura, Operaciones y Seguridad

## Contexto

ADR-009 define un `staging` AWS mínimo. Su infraestructura debe ser reproducible, revisable y destruible sin convertir un workflow de despliegue en administrador de la cuenta. El state de IaC tampoco puede depender de recursos que todavía no existen ni eliminarse accidentalmente junto con el entorno.

ADR-010 permanece reservado para policy packs y publicación de reglas; por eso esta decisión usa ADR-011.

## Decisión

Se usará OpenTofu y se separarán dos roots:

```text
infra/aws/bootstrap
infra/aws/staging
```

### `bootstrap`

Contendrá únicamente recursos fundacionales con ciclo de vida independiente:

- bucket S3 privado, cifrado y versionado para state;
- bloqueo nativo S3 mediante `use_lockfile = true`;
- bloqueo de acceso público y lifecycle de versiones antiguas;
- proveedor OIDC de GitHub, si no existe uno compatible en la cuenta;
- configuración necesaria para que el state de `staging` use el backend remoto.

El primer arranque usa state local solo durante la creación del bucket. Después se migra de inmediato al backend S3, se verifica la migración y se elimina de forma segura cualquier copia local de state. Nunca se commitean `*.tfstate`, planes con valores sensibles ni credenciales.

### `staging`

Contendrá los recursos operativos de ADR-009:

- VPC, subnet, rutas, Security Group y EC2;
- EBS asociado;
- roles y políticas de instancia y despliegue;
- ECR privado para cliente y servidor;
- bucket S3 operativo;
- rutas y permisos de Parameter Store;
- AWS Budget y tags obligatorios.

`tofu destroy` sobre este root será una operación soportada y no podrá destruir el bucket de state ni el proveedor OIDC fundacional.

### Autenticación y ejecución

- El primer `tofu apply` se ejecuta manualmente por el propietario con credenciales temporales y un plan revisado.
- No se crean access keys permanentes para root, GitHub ni EC2.
- El rol `github-deploy` no puede aplicar infraestructura, administrar IAM, crear instancias ni cambiar red.
- Automatizar OpenTofu desde GitHub exigirá un rol IaC separado y una decisión posterior.
- Toda destrucción requiere plan de destrucción revisado y autorización humana explícita.

### Configuración y secretos

- Variables no sensibles pueden vivir en archivos versionados por entorno.
- Valores sensibles nunca se pasan como variables OpenTofu si ello los incorpora al state.
- OpenTofu administra nombres, metadatos y permisos de parámetros; la carga de secretos se realiza por un canal operativo separado.
- El Account ID no se incluye en nombres públicos de buckets.
- Los recursos globalmente únicos usan un sufijo no sensible y estable.

### Convenciones mínimas

Todos los recursos admitidos reciben, cuando el servicio lo soporte:

```text
Project     = propractix-v2
Environment = staging
ManagedBy   = opentofu
Owner       = product-owner
CostCenter  = staging
```

Se fijan versiones compatibles de OpenTofu y providers. Los upgrades requieren plan, revisión de changelog y validación.

## Flujo de cambio

1. `tofu fmt -check` y `tofu validate`.
2. Generar plan sin persistir secretos.
3. Revisar recursos, reemplazos, coste y permisos.
4. Validar políticas IAM con Access Analyzer.
5. Obtener aprobación humana.
6. Aplicar con credenciales temporales.
7. Ejecutar comprobaciones del checklist `CL0_CLOUD_STAGING`.
8. Registrar outputs no sensibles y evidencia; nunca credenciales.

## Consecuencias

- Bootstrap y entorno tienen blast radius separado.
- El state queda cifrado, versionado y bloqueado sin DynamoDB.
- Existe un paso inicial especial para romper la dependencia circular del bucket de state.
- Destruir `staging` no equivale a borrar backups por accidente: retención y `prevent_destroy` se definen según el plan operativo y se revisan antes de aplicar.
- GitHub despliega aplicaciones con permisos menores que los necesarios para crear infraestructura.

## Alternativas consideradas

- **Terraform:** viable, pero se elige OpenTofu por decisión aprobada y licencia abierta.
- **CloudFormation/CDK:** aplazado para conservar portabilidad de IaC y evitar código adicional.
- **State solo local:** rechazado por pérdida, falta de locking y baja trazabilidad.
- **DynamoDB para locking:** innecesario; OpenTofu soporta locking nativo en S3.
- **Un único root:** rechazado porque acopla la destrucción del entorno a sus recursos fundacionales.
- **IaC con el rol de deploy:** rechazado por exceso de privilegios.

## Conformidad

- Los roots tienen backends y estados separados.
- S3 state tiene cifrado, versionado, bloqueo público y `use_lockfile=true`.
- `.gitignore` excluye state, planes y overrides locales.
- Ningún valor sensible aparece en Git, plan persistido, output o state por diseño.
- Las políticas tienen recursos concretos y no conceden `AdministratorAccess`.
- Un plan de `staging` no contiene cambios de `bootstrap`.
- Un destroy de prueba demuestra que los recursos fundacionales permanecen.

## Fuente operativa

- [OpenTofu — backend S3 y locking](https://opentofu.org/docs/language/settings/backends/s3/), consultado el 2026-09-13.

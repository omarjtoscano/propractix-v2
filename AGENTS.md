# Instrucciones para agentes de desarrollo

## Fuentes de verdad

Antes de modificar código, lee en este orden:

1. `docs/ProPractix-V2-MVP-Espana-Especificacion-Arquitectura-y-Tecnica-para-Codex.md`.
2. `docs/ProPractix-V2-Blueprint-Dominio-y-Mapas-Legales-Espana.md`.
3. Los ADR aplicables de `docs/adr/`.
4. El plan aprobado del incremento en `docs/plans/`.

Si dos documentos se contradicen, detente y solicita una decisión. Un ADR aceptado posterior puede concretar una decisión general, pero no alterar silenciosamente el alcance de producto ni una puerta legal.

## Alcance vigente

- Trabaja únicamente en el Incremento 1 y en una historia autorizada cada vez.
- ADR-001, ADR-002 y ADR-004 a ADR-008 están aceptados en revisión 4; ADR-003 en revisión 5; ADR-009 en revisión 2; ADR-011 en revisión 1; ADR-010 está reservado para policy packs. El plan está aprobado en revisión 6. La única historia actualmente `READY` y autorizada es I1-H01.
- No inicies I1-H02 ni historias posteriores hasta que sus gates y predecesoras figuren cerrados en el plan.
- No implementes ofertas, candidaturas, prevalidación, formalización, prácticas ni documentos todavía.
- La empresa es el tenant principal.
- `student` posee perfil, onboarding y declaración académica; `identity` no absorbe esos conceptos.
- La universidad es una referencia externa y no requiere cuenta ni workspace.
- España es la primera jurisdicción del MVP según D-001.
- País empresarial, locale de interfaz y jurisdicción legal son conceptos distintos; un fallback de idioma nunca aplica reglas españolas a otro país.
- No inventes reglas legales. `UNKNOWN` e `INDETERMINATE` de cumplimiento bloquean puertas legales críticas. El `INDETERMINATE` técnico de la ruta de correo no bloquea H03 y sigue D-007/ADR-003.

## Arquitectura obligatoria

- Monolito modular desplegable como una unidad.
- Organización principal por bounded context, nunca por carpetas globales `controller`, `service`, `repository` o `entity`.
- Dependencias: `adapter -> application -> domain`.
- El dominio es Java puro: sin Spring, JPA, HTTP ni acceso a variables de entorno.
- Los casos de uso se exponen mediante puertos de entrada; persistencia, reloj, correo e integraciones son puertos de salida.
- Ningún contexto accede a tablas, repositorios o entidades de persistencia de otro contexto.
- No introduzcas condicionales de dominio por país; resuelve reglas mediante políticas jurisdiccionales versionadas.
- No uses Lombok, Spring Data REST, H2, microservicios, Kafka ni CQRS completo salvo un ADR nuevo previamente aprobado.

## Configuración e internacionalización

- Países habilitados, locales soportados, correspondencias país→locale y locale de fallback proceden de configuración tipada y validada.
- Los dominios de correo público no admitidos para el alta empresarial proceden de una política versionada; nunca de listas o condicionales incrustados en Java/TypeScript.
- La regla evalúa el dominio normalizado después de `@`, no el proveedor de hosting: un dominio corporativo propio alojado en Google Workspace o Microsoft 365 es admisible.
- Después de la política, `EmailDomainRoutingVerificationPort` es la comprobación técnica principal. Devuelve `MAIL_CAPABLE`, `NO_MAIL_ROUTE` o `INDETERMINATE`; los dos últimos no bloquean la solicitud, la mantienen pendiente de verificación y se reintentan con backoff.
- La política ausente sí mantiene H03 cerrada; no confundas esa configuración obligatoria con un fallo transitorio de DNS.
- Toda etiqueta, acción, estado, ayuda, validación o error visible usa una clave i18n; no incrustes textos de presentación en Java, TypeScript o JSX.
- API, dominio y persistencia conservan códigos estables independientes del idioma.
- Los textos legales se versionan separadamente de los catálogos generales de traducción.
- Las invariantes técnicas permanecen tipadas en código; no se convierten en configuración libre para aparentar flexibilidad.

## Seguridad y tenancy

- Nunca confíes en un `tenantId` aportado por el cliente para autorizar una operación.
- Deriva el tenant de la identidad autenticada y comprueba pertenencia en aplicación y persistencia.
- Toda consulta sobre datos empresariales incluye el tenant en su criterio y sus restricciones de unicidad.
- Añade al menos una prueba negativa de acceso entre tenants en cada historia que maneje datos tenant-owned.
- No registres secretos, tokens, contraseñas, documentos o datos personales innecesarios en logs.

## Persistencia y API

- PostgreSQL es la única base de datos soportada.
- Las migraciones Flyway son forward-only; nunca edites una migración aplicada.
- Las entidades JPA y sus mappers viven en adapters, separadas del modelo de dominio.
- La API se documenta con OpenAPI, usa Problem Details y mantiene compatibilidad dentro de `/api/v1`.
- Las operaciones de creación con riesgo de repetición siguen el ADR de idempotencia.

## Despliegue cloud

- Sigue ADR-009: un único `staging` AWS económico y portable mediante Docker Compose; producción está fuera del Incremento 1.
- La aceptación local de una historia y su promoción cloud son hitos distintos. H01 puede cerrarse localmente con `CL0_CLOUD_STAGING` pendiente y habilitar sucesoras cuando sus demás gates estén cerrados.
- `CL0_CLOUD_STAGING` sigue `PENDING -> READY_FOR_APPLICATION -> CLOSED`; solo se cierra después de desplegar y verificar H05. Consulta ADR-011 y los planes/checklists cloud antes de tocar AWS.
- Preparar IaC puede avanzar en paralelo, pero ninguna mutación AWS, `tofu apply`, conversión a Paid Plan o exposición pública se ejecuta sin autorización explícita del propietario.
- La baseline usa `eu-north-1`, una `t4g.small` ARM64, ECR privado, Parameter Store Standard, S3, EBS `gp3` y OpenTofu. No presupongas que créditos equivalen a recursos sin coste.
- No reserves EIP ni abras ingress. Una IPv4 pública dinámica solo se habilita para egress cuando el plan aprobado justifique su necesidad y coste; el acceso inicial usa SSM/port forwarding.
- Las imágenes se identifican por SHA/digest; no uses `latest` como release.
- El despliegue exige CI verde, aprobación de GitHub Environment, OIDC con permisos mínimos, migración Flyway, readiness, smoke test y rollback.
- No introduzcas RDS, App Runner, ECS, EKS, balanceadores, NAT Gateway, endpoints VPC de pago, AWS Backup o Secrets Manager en la baseline sin un ADR posterior aprobado.
- `staging` utiliza datos sintéticos mientras las puertas de privacidad aplicables estén pendientes.
- AWS es infraestructura: ningún dominio o caso de uso importa SDKs o conceptos del proveedor.

## Flujo de trabajo

1. Confirma la historia, criterios de aceptación y elementos fuera de alcance.
2. Presenta un plan breve y espera aprobación cuando cambien dominio, seguridad, privacidad o cumplimiento.
3. Implementa primero invariantes y pruebas de dominio.
4. Añade aplicación y puertos.
5. Añade adapters y configuración.
6. Integra API, frontend e i18n cuando la historia lo requiera.
7. Ejecuta las validaciones relevantes.
8. Revisa `git diff --check`, `git diff` y `git status` antes del commit.
9. Resume archivos modificados, pruebas ejecutadas, resultados y riesgos.

## Comandos mínimos

```bash
npm run infra:up
npm run server:test
npm run server:verify
git diff --check
git status --short
```

Si Docker o JDK 21 no están disponibles, informa la limitación; no simules una ejecución satisfactoria.

## Política de commits

- Un commit debe corresponder a una historia o cambio técnico coherente.
- Usa mensajes tipo Conventional Commits, por ejemplo `feat(identity): register company administrator`.
- No mezcles reformateos masivos ni cambios no relacionados.
- No hagas push, merge, rebase destructivo ni publiques artefactos sin autorización explícita.

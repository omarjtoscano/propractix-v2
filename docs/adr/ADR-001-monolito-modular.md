# ADR-001 — Monolito modular y estrategia de módulos

- Estado: Aceptado — revisión 4
- Fecha: 2026-09-12
- Decisores: Producto y Arquitectura
- Reemplaza: revisión 3 del ADR-001

## Contexto

ProPractix V2 debe entregar incrementos completos sin reproducir el acoplamiento de V1. El producto necesita una operación sencilla, límites verificables entre capacidades y una forma durable de coordinar procesos que atraviesen más de un bounded context.

El alta empresarial es el primer proceso transversal: `organization` posee la empresa y sus membresías, mientras `identity` posee cuentas, credenciales y verificaciones. La especificación prohíbe modificar aggregates de contextos diferentes en una misma transacción.

## Decisión

Se construirá un monolito modular desplegable como una única aplicación Spring Boot y una única base PostgreSQL.

Durante el MVP, `server` será un solo proyecto Maven. Los módulos de negocio serán bounded contexts identificados por paquetes raíz:

```text
com.propractix.identity
com.propractix.student
com.propractix.organization
com.propractix.academicinstitution
com.propractix.jurisdiction
com.propractix.recruitment
com.propractix.formalization
com.propractix.internship
com.propractix.learning
com.propractix.compliance
com.propractix.documents
com.propractix.notifications
com.propractix.administration
```

`student` ha sido aprobado como corrección mediante D-006. Posee el perfil, el onboarding y las declaraciones académicas personales, mientras `identity` conserva exclusivamente cuenta, credenciales, verificaciones y sesiones. La especificación técnica y el blueprint ya reflejan esta frontera y distinguen la declaración inicial de su reconfirmación en una candidatura.

Existirá además infraestructura técnica transversal, sin convertirse en bounded context de negocio:

```text
com.propractix.platform.outbox
com.propractix.platform.idempotency
com.propractix.platform.audit
com.propractix.platform.observability
com.propractix.platform.ratelimit
```

Los contextos de negocio no importarán implementaciones de `platform`. Definirán puertos en su capa de aplicación y la configuración conectará los adaptadores técnicos.

## Onboarding empresarial

`organization` será propietario de `CompanyOnboarding`, un process manager durable. Coordinará transacciones separadas mediante contratos públicos y eventos:

```text
SUBMITTED
IDENTITY_PENDING
EMAIL_PENDING
READY
FAILED
EXPIRED
```

El alta observable será reintentable e idempotente. Un fallo parcial dejará un estado recuperable; nunca una empresa presentada como activa por error. El detalle se concreta en ADR-003, ADR-005 y el plan del Incremento 1.

Un onboarding `EXPIRED` puede volver a `EMAIL_PENDING` únicamente mediante una nueva verificación enviada al mismo correo ya registrado y mientras sus datos no hayan sido eliminados por retención. Cambiar el correo o reclamar una empresa coincidente exige el proceso autenticado/administrativo de ADR-003.

`student` poseerá un `StudentOnboarding` durable equivalente, pero independiente:

```text
SUBMITTED
IDENTITY_PENDING
EMAIL_PENDING
READY
FAILED
EXPIRED
```

No existe una membership empresarial implícita y la convergencia con una cuenta global aplica la ceremonia segura de ADR-003.

## Contratos y dependencias entre módulos

La siguiente tabla es el registro autorizado de contratos cross-context del Incremento 1. El consumidor importa únicamente el paquete público del proveedor; nunca sus servicios concretos, adapters, repositorios o modelo interno.

| Consumidor | Owner/proveedor | Contrato público | Paquete público | Operación permitida |
|---|---|---|---|---|
| `organization` | `identity` | `PrepareAdministratorAccount` | `identity.application.port.in` | Provisioning idempotente por `OnboardingId`, sin contraseña, correo ni nombre en el mensaje durable. |
| `organization` | `identity` | `AdministratorProvisionedV1`, `AccountEmailVerifiedV1` | `identity.application.contract.event.v1` | Consumir resultados versionados después del commit. |
| `student` | `identity` | `PrepareStudentAccount` | `identity.application.port.in` | Provisioning o continuación idempotente, sin credenciales en el mensaje durable. |
| `student` | `identity` | `StudentAccountProvisionedV1`, `AccountEmailVerifiedV1` | `identity.application.contract.event.v1` | Consumir resultados versionados después del commit. |
| `organization` | `compliance` | `ResolveApplicablePrivacyNotice` | `compliance.application.port.in` | Resolver el aviso ML-15 vigente por finalidad, jurisdicción e instante. |
| `student` | `compliance` | `ResolveApplicablePrivacyNotice` | `compliance.application.port.in` | Resolver el aviso ML-15 vigente por finalidad, jurisdicción e instante. |
| `student` | `academicinstitution` | `SearchPublishedInstitutions` | `academicinstitution.application.port.in` | Consultar únicamente la versión publicada del catálogo. |
| `identity` | `notifications` | `DeliverNotification` | `notifications.application.port.in` | Entregar destinatario, `templateKey`, locale, enlace y `correlationId` solo en memoria. |
| `configuration` | `identity` | `HandleEmailVerificationDeliveryRequested` | `identity.application.port.in` | Conectar el relay genérico usando solo `VerificationId` y metadatos técnicos. |

La API pública de catálogo se expone mediante el adapter HTTP de `academicinstitution`; no es un contrato Java entre bounded contexts. Los contextos de negocio definen sus propios puertos de salida técnicos y `configuration` los conecta con adapters de `platform`, sin que `platform` importe dominios concretos.

Los cambios de estado entre contextos ocurrirán después del commit. Las consultas a `compliance` y `academicinstitution` no abren una transacción distribuida ni permiten escribir en el proveedor. La entrega de correo es unidireccional: `identity` resuelve transitoriamente el destinatario y el JWS y llama al puerto de entrada de `notifications`; `notifications` no consulta `identity`. La configuración registra el handler de `identity` en el relay genérico, por lo que `platform.outbox` tampoco depende de `identity`. No se usarán dependencias cíclicas, transacciones distribuidas ni acceso directo a repositorios o tablas ajenas.

## Guardrails verificables

ArchUnit comprobará al menos:

1. `domain` no depende de Spring, JPA, Jackson, Servlet ni paquetes `adapter`/`configuration`.
2. `application` no depende de JPA, controladores ni adapters.
3. Un bounded context no importa paquetes internos de otro.
4. Solo los contratos públicos declarados pueden cruzar contextos.
5. `platform` no contiene aggregates ni reglas de negocio.
6. No existen ciclos entre módulos.

Spring Modulith no será obligatorio durante el inicio. Requerirá un ADR posterior si se propone.

## Baseline fijada

| Componente | Versión/decisión |
|---|---|
| Java | 21 LTS |
| Spring Boot | 4.1.1 |
| Maven Wrapper | 3.9.16 |
| PostgreSQL | 16 (`postgres:16-alpine` en local) |
| Node.js | 24 LTS |
| npm | 11.9.0 |
| React / React DOM | 19.3.0 |
| TypeScript | 7.0.2 |
| Vite / plugin React | 8.3.0 / 6.1.1 |
| React Router DOM | 7.18.3 |
| i18next / react-i18next | 26.4.2 / 17.0.13 |
| Estado/formularios | TanStack React Query 5.102.8, Zustand 5.0.15, React Hook Form 7.88.0 y Zod 4.6.2 |
| Cliente OpenAPI | `@hey-api/openapi-ts` 0.99.0 con cliente Fetch generado |
| Tests cliente | Vitest 5.0.0, Testing Library React 16.3.3, user-event 14.6.7 y Playwright 1.63.0 |

Estas versiones constituyen `F0_CLIENT_BASELINE`. El `package-lock.json` fijará el árbol instalado y CI utilizará `npm ci`. Una actualización mayor, una nueva librería estructural o un cambio de generador OpenAPI exige ADR o actualización explícita de uno existente; parches y menores se incorporan mediante pull request con pruebas, revisión del cliente generado y evidencia de compatibilidad.

## Alternativas consideradas

- Microservicios: rechazados por complejidad operativa y transaccional prematura.
- Maven multimódulo inmediato: aplazado hasta que los límites se estabilicen.
- Capas globales: rechazadas porque no expresan propiedad funcional.
- Excepción transaccional para el registro: rechazada; el onboarding será durable.

## Consecuencias

- El MVP conserva un único despliegue y transacciones locales.
- El onboarding necesita estados intermedios, reintentos y UX recuperable.
- Los límites dependen de pruebas automáticas y revisión del diff.
- Se evaluará Maven multimódulo cuando haya al menos tres contextos activos, aparezcan violaciones repetidas que ArchUnit no evite o un módulo necesite ciclo de despliegue independiente.

## Condiciones documentales de aceptación

- D-006 y las fuentes superiores contienen el contexto `student` y el mismo calendario de declaración académica.
- La matriz anterior identifica contrato, owner/proveedor, consumidor y paquete público para todos los cruces requeridos por el Incremento 1 sin ciclos.
- Producto y Arquitectura aceptan el monolito modular, el onboarding eventual y el riesgo operativo descrito.
- `F0_CLIENT_BASELINE` está fijada y aprobada antes de I1-H02.

## Conformidad de la implementación

- Las reglas ArchUnit anteriores están automatizadas en I1-H01.
- ArchUnit consume el registro de contratos anterior y verifica que solo se importan los paquetes públicos autorizados.
- El onboarding no escribe `identity` y `organization` en una misma transacción.
- Todo componente y tabla técnica tiene propietario documentado.
- Las versiones efectivamente usadas coinciden con la matriz.

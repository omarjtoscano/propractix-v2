# ADR-001 — Monolito modular y estrategia de módulos

- Estado: Propuesto — revisión 3
- Fecha: 2026-09-12
- Decisores: Producto y Arquitectura
- Reemplaza: revisión 2 del ADR-001

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

| Origen | Destino | Contrato permitido en Incremento 1 |
|---|---|---|
| `organization` | `identity` | Solicitud durable de preparación del administrador, sin contraseña ni hash de credencial. |
| `identity` | `organization` | Evento de cuenta provisionada o correo verificado. |
| `student` | `identity` | Solicitud durable de provisioning o enlace seguro de una cuenta. |
| `identity` | `student` | Evento de cuenta provisionada o correo verificado. |
| `organization` | `compliance` | Consulta síncrona del aviso ML-15 aprobado para onboarding empresarial. |
| `student` | `compliance` | Consulta síncrona del aviso ML-15 aprobado para onboarding estudiantil. |
| `student` | `academicinstitution` | Consulta síncrona del catálogo institucional publicado. |
| `identity` | `notifications` | Entrega transitoria de destinatario, plantilla y enlace desde un handler propiedad de `identity`. |
| `academicinstitution` | cliente/API | Consulta pública del catálogo. |
| contextos de negocio | `platform` | Solo puertos implementados mediante configuración/adapters. |

Los cambios de estado entre contextos ocurrirán después del commit. Las consultas a `compliance` y `academicinstitution` no abren una transacción distribuida ni permiten escribir en el proveedor. La entrega de correo es unidireccional: `identity` resuelve transitoriamente el destinatario y el JWS y llama al puerto de `notifications`; `notifications` no consulta `identity`. No se usarán dependencias cíclicas, transacciones distribuidas ni acceso directo a repositorios o tablas ajenas.

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
| React/Vite y librerías cliente | Se fijarán antes de crear `client` en I1-H02; no se añaden por anticipado. |

Una actualización mayor o una nueva librería estructural exige ADR o actualización explícita de uno existente.

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
- La matriz anterior cubre todos los cruces requeridos por el Incremento 1 sin ciclos.
- Producto y Arquitectura aceptan el monolito modular, el onboarding eventual y el riesgo operativo descrito.
- La baseline está fijada y cualquier pendiente de frontend permanece como gate explícito anterior a I1-H02.

## Conformidad de la implementación

- Las reglas ArchUnit anteriores están automatizadas en I1-H01.
- El onboarding no escribe `identity` y `organization` en una misma transacción.
- Todo componente y tabla técnica tiene propietario documentado.
- Las versiones efectivamente usadas coinciden con la matriz.

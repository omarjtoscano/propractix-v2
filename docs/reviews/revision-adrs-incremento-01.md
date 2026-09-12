# Revisión de ADR-001 a ADR-008 y plan del Incremento 1

- Fecha de revisión: 2026-09-12
- Estado del informe: Final
- Resultado: Bloqueado para aceptación
- Alcance: revisión documental; no autoriza implementación

## 1. Objetivo

Revisar las propuestas ADR-001 a ADR-008 y el plan del Incremento 1 para detectar:

- contradicciones con las fuentes de verdad;
- decisiones costosas o difíciles de revertir;
- riesgos de seguridad, privacidad y aislamiento tenant;
- exceso de alcance;
- ausencia de criterios de aceptación y dependencias no resueltas.

Este informe no modifica el estado de ningún ADR. Todos deben permanecer en
`Propuesto` hasta que se resuelvan las observaciones aplicables y el propietario
autorice expresamente el cambio de estado de cada uno.

## 2. Fuentes revisadas

1. [`AGENTS.md`](../../AGENTS.md).
2. [Especificación de arquitectura y técnica](../ProPractix-V2-MVP-Espana-Especificacion-Arquitectura-y-Tecnica-para-Codex.md).
3. [Blueprint de dominio y mapas legales para España](../ProPractix-V2-Blueprint-Dominio-y-Mapas-Legales-Espana.md).
4. [ADR-001](../adr/ADR-001-monolito-modular.md) a [ADR-008](../adr/ADR-008-openapi-errores-idempotencia.md).
5. [Plan técnico propuesto del Incremento 1](../plans/increment-01-plan.md).

## 3. Conclusión ejecutiva

ADR-001 a ADR-008 todavía no están preparados para pasar a `Aceptado`.

Existen dos bloqueos principales:

1. El alta empresarial propuesta cruza `identity` y `organization` dentro de una
   única transacción, pese a que la especificación limita una transacción a
   aggregates de un solo contexto.
2. El envío durable del correo de verificación no define cómo conservar o
   reconstruir de forma segura el token cuyo único valor persistido debe ser el
   hash.

También deben resolverse el significado de la verificación empresarial, el
modelo exacto de tenancy e identidad, la privacidad de los registros, la
idempotencia anónima, la propiedad de tablas técnicas, el contrato de seguridad
de sesiones y los criterios de aceptación de las historias.

## 4. Observaciones ordenadas por severidad

### 4.1 Bloqueantes

#### B-01 — Alta empresarial incompatible con las fronteras transaccionales

El plan propone crear `Company`, `UserAccount` y `Membership` mediante un único
caso de uso transaccional, con rollback completo (`docs/plans/increment-01-plan.md`,
líneas 124-129). Esos aggregates pertenecen a `organization` e `identity`.

La especificación establece que una transacción modifica aggregates de un solo
contexto y que los módulos se coordinan mediante contratos públicos y eventos
después del commit (`docs/ProPractix-V2-MVP-Espana-Especificacion-Arquitectura-y-Tecnica-para-Codex.md`,
líneas 441-449). ADR-006 también reserva la escritura de cada tabla a su contexto
propietario (`docs/adr/ADR-006-dominio-jpa-propiedad-tablas.md`, líneas 15-28).

Debe elegirse y documentarse una de estas alternativas:

- proceso durable con transacciones separadas, estados intermedios e idempotencia;
- cambio explícito de propiedad de los aggregates;
- excepción documentada a la regla de una transacción por contexto.

La experiencia observable ante un alta parcial también necesita aprobación de
producto.

#### B-02 — Token de verificación incompatible con un envío durable

ADR-007 indica que el token de verificación se almacena únicamente mediante hash
(`docs/adr/ADR-007-autenticacion-sesiones-csrf.md`, línea 22). ADR-005 establece
que el correo de verificación será el primer efecto externo enviado mediante
outbox (`docs/adr/ADR-005-eventos-outbox.md`, línea 23).

Un relay no puede reconstruir el enlace desde el hash. Guardar el token en claro
dentro de la outbox anularía la protección sin declararlo. Debe definirse una
estrategia explícita, por ejemplo un secreto de entrega cifrado, con TTL corto,
claves externas, acceso restringido y borrado o redacción tras la entrega.

### 4.2 Altas

#### A-01 — Verificación de correo, cuenta y empresa están mezcladas

La especificación diferencia `RegisterCompany`, `VerifyCompany`,
`CompanyRegistered` y `CompanyVerified` (líneas 388-391 y 419-421). H03 deja
abierta la activación conjunta de cuenta y empresa
(`docs/plans/increment-01-plan.md`, línea 137).

Controlar un correo no acredita la identidad legal de una empresa. Deben existir
estados y transiciones independientes para:

- correo verificado;
- cuenta habilitada;
- empresa pendiente de revisión;
- empresa verificada, rechazada o suspendida.

#### A-02 — Modelo de tenancy insuficiente para fijar el esquema

ADR-003 no aclara si `TenantId` y `CompanyId` comparten identidad, cómo se
selecciona el tenant activo cuando una persona tiene varias membresías, ni cómo
se invalidan roles o membresías presentes en un JWT
(`docs/adr/ADR-003-multitenancy-tenant-context.md`, líneas 13-28).

Estas decisiones afectan claves, índices, sesiones, autorización y contratos
públicos. Su migración posterior sería costosa.

#### A-03 — Falta el prerrequisito de privacidad de Incremento 1

El blueprint asigna `ML-15` al Incremento 1
(`docs/ProPractix-V2-Blueprint-Dominio-y-Mapas-Legales-Espana.md`, líneas
268-274). La especificación exige finalidad, base jurídica, minimización,
información y retención antes de tratar o comunicar datos personales (líneas
789-798).

Las historias de registro no definen aviso versionado, datos mínimos, retención
de altas abandonadas, conservación de intentos de verificación o tratamiento de
la declaración académica. Estas reglas requieren aprobación de producto y
privacidad/asesoría jurídica; no deben inventarse durante la implementación.

#### A-04 — Idempotencia pública sin ámbito ni protección de datos definidos

ADR-008 propone conservar el hash de la petición y una respuesta reproducible,
pero el “ámbito del actor o registro” no está definido cuando todavía no existe
un actor autenticado (`docs/adr/ADR-008-openapi-errores-idempotencia.md`, líneas
25-32).

Faltan:

- formato y entropía de la clave;
- canonicalización de la petición;
- fingerprint protegido mediante HMAC o mecanismo equivalente;
- resolución atómica de solicitudes concurrentes;
- estado de operación en curso;
- prohibición de almacenar contraseñas, cookies, tokens o respuestas sensibles;
- política de expiración, limpieza y auditoría.

#### A-05 — Contrato de autenticación incompleto

ADR-007 no fija algoritmo y rotación de claves JWT, `issuer`, `audience`,
tratamiento de permisos obsoletos, atributos completos de cookies ni conducta
cuando falta `Origin` (`docs/adr/ADR-007-autenticacion-sesiones-csrf.md`, líneas
13-25).

Además, ADR-007 exige rate limiting para el registro, pero el plan lo introduce
en H04, después de H02 (`docs/plans/increment-01-plan.md`, líneas 140-147). El
registro público no debe quedar disponible antes de su protección contra abuso.

#### A-06 — Outbox, idempotencia y auditoría carecen de propietario

El plan introduce `platform_outbox_event`, `platform_idempotency_record` y una
auditoría técnica (`docs/plans/increment-01-plan.md`, líneas 35-66). `platform`
no aparece como módulo en ADR-001, mientras ADR-006 exige que cada tabla tenga un
único contexto propietario.

Debe definirse para cada tabla:

- módulo o componente propietario;
- API o port permitido;
- tenant scope;
- datos admitidos y prohibidos;
- retención y redacción;
- permisos de lectura y escritura;
- comportamiento append-only cuando corresponda.

#### A-07 — Las historias no cumplen la Definition of Ready

La especificación exige actor, contexto propietario, estado inicial, contrato,
invariantes, errores, eventos, migración, estrategia de prueba y criterios de
aceptación (líneas 1006-1025). El plan contiene tareas y listas de pruebas, pero
no resultados observables y verificables completos.

H02 maneja datos tenant-owned sin incluir su prueba negativa cross-tenant. H06
crea una declaración student-owned sin exigir una prueba negativa entre
estudiantes.

#### A-08 — Frontend, contrato e i18n se difieren hasta una historia final

H07 acumula React, cliente OpenAPI, español e inglés y cierre del recorrido
(`docs/plans/increment-01-plan.md`, líneas 168-173). Esto contradice el carácter
vertical de los incrementos y retrasa la detección de divergencias entre API y
cliente. Cada historia funcional debe incluir la porción necesaria de API,
OpenAPI, cliente, UI, i18n y pruebas.

### 4.3 Medias

#### M-01 — La condición de inicio permite decisiones retroactivas

El plan solo prohíbe historias “funcionales” antes de aceptar los ADR (línea 10),
pero H01 ya materializa PostgreSQL, Flyway, ArchUnit y otras decisiones
estructurales. La puerta debe alcanzar toda implementación no desechable que
dependa de un ADR todavía propuesto.

#### M-02 — El catálogo de titulaciones puede exceder el alcance

El plan contempla `academicinstitution_program`, aunque todavía condiciona su
existencia a una fuente mantenible (línea 64). La fuente superior exige una
universidad consultable y una titulación declarada, no necesariamente un
catálogo canónico completo.

Antes de implementarlo deben decidirse procedencia, licencia, versión,
actualización, centros adscritos y conducta ante universidad o titulación
desconocida. En ausencia de una fuente gobernada, debe preferirse una declaración
trazable antes que inventar un catálogo canónico.

#### M-03 — `@Version` no cubre todas las carreras críticas

ADR-004 aplica concurrencia optimista a aggregates mutables
(`docs/adr/ADR-004-identificadores-tiempo-concurrencia.md`, línea 20). Los tokens
de un solo uso, la rotación de refresh, la adquisición de outbox y la
idempotencia necesitan además operaciones condicionales atómicas, constraints
únicos y pruebas concurrentes específicas.

#### M-04 — Gobierno del roadmap y versiones tecnológicas sin cerrar

El blueprint conserva un Incremento 0 de foundation y mapas `ML-00–02`, mientras
la especificación incorpora la fundación mínima al Incremento 1. Debe declararse
qué interpretación prevalece para el plan actual.

La especificación también exige fijar versiones exactas y registrarlas en un ADR
(línea 161), pero ADR-001 a ADR-008 no contienen esa matriz.

#### M-05 — `traceId` y `correlationId` no están armonizados

ADR-008 incluye `traceId` en Problem Details y, a la vez, exige propagar un
correlation ID (`docs/adr/ADR-008-openapi-errores-idempotencia.md`, líneas 17-34).
Debe definirse si son campos distintos, cómo se generan y cuál se devuelve en
respuestas exitosas y fallidas.

## 5. Decisiones que requieren aprobación de producto

1. Separación de estados entre correo, cuenta y verificación empresarial.
2. Resultado visible ante un alta parcialmente completada entre `identity` y
   `organization`.
3. Unicidad global del correo y posibilidad de que una persona tenga varios
   perfiles funcionales o membresías empresariales.
4. Selección del tenant activo y experiencia de cambio de empresa.
5. Duraciones de 10 minutos para access token y 30 días para la familia refresh,
   junto con el riesgo aceptado de permisos obsoletos.
6. Finalidades, avisos, retención y tratamiento de registros abandonados, con
   aprobación adicional de privacidad/asesoría jurídica mediante `ML-15`.
7. Alcance, visibilidad, procedencia y mantenimiento del catálogo.
8. Absorción de la foundation del antiguo Incremento 0 dentro del Incremento 1.

No deben reabrirse sin una razón nueva las decisiones ya aceptadas en `D-001` y
`D-003`: España como primera jurisdicción, empresa como tenant principal y
universidad sin workspace obligatorio.

## 6. Cambios concretos recomendados por ADR

### ADR-001 — Monolito modular

- Añadir una matriz explícita de dependencias entre contextos.
- Definir el propietario del proceso de onboarding empresarial.
- Alinear el inventario de módulos con `administration` y con la infraestructura
  técnica transversal.
- Convertir las reglas ArchUnit en criterios de verificación concretos.
- Registrar el criterio de salida hacia Maven multimódulo.

### ADR-002 — Organización hexagonal

- Fijar la ubicación de interfaces de repositorio y límites transaccionales.
- Declarar si se permiten dependencias Spring transaccionales en `application`.
- Definir qué constituye un contrato público entre contextos.
- Enumerar las reglas ArchUnit para domain, application, adapters y cruces de
  contexto.

### ADR-003 — Multi-tenancy

- Definir la relación `TenantId`–`CompanyId`.
- Modelar membresías múltiples, tenant activo y formación de `CurrentActor`.
- Resolver revocación y frescura de roles incluidos en tokens.
- Añadir autorización para recursos globales y student-owned.
- Completar constraints y matriz de pruebas negativas en aplicación y
  persistencia.

### ADR-004 — IDs, tiempo y concurrencia

- Documentar representación PostgreSQL de UUID, instantes y zonas.
- Precisar el contrato del reloj y captura de zona predeterminada.
- Definir el Problem Details de conflicto concurrente.
- Separar `@Version` de los consumos atómicos de tokens, refresh, idempotencia y
  leasing de outbox.

### ADR-005 — Eventos y outbox

- Definir propietario, esquema, ordering, locking, backoff, dead letters e
  inbox/deduplicación.
- Resolver el secreto de entrega del correo de verificación.
- Prohibir secretos y PII innecesaria en eventos.
- Sustituir la retención indefinida por metadatos mínimos sujetos a política.
- Documentar la consistencia del onboarding entre contextos.

### ADR-006 — Dominio, JPA y tablas

- Incorporar una matriz tabla→propietario, incluyendo outbox, idempotencia y
  auditoría.
- Establecer constraints tenant compuestos y reglas para referencias entre
  contextos.
- Separar migraciones de esquema de importaciones versionadas del catálogo.
- Documentar el riesgo residual de un esquema y usuario de base de datos únicos.

### ADR-007 — Autenticación y sesiones

- Añadir el modelo de amenazas.
- Justificar BCrypt mediante benchmark y versionar algoritmo y parámetros.
- Concretar algoritmo, claves, rotación y claims JWT.
- Fijar atributos y topología de cookies, CSRF y validación de origen.
- Resolver revocación, carreras de refresh y permisos obsoletos.
- Aplicar rate limiting antes de exponer registro o reenvío.
- Integrar de forma segura el token de verificación con la outbox.

### ADR-008 — OpenAPI, errores e idempotencia

- Armonizar `traceId` y `correlationId`.
- Definir taxonomía estable de errores, incluido el acceso cross-tenant.
- Especificar idempotencia anónima, fingerprint protegido, estado en curso,
  concurrencia, TTL y replay seguro.
- Prohibir persistir credenciales, cookies o respuestas sensibles.
- Automatizar compatibilidad OpenAPI y generación del cliente por historia.

## 7. Orden ajustado de historias

| Orden | Historia propuesta | Resultado mínimo |
|---|---|---|
| 0 | Puerta de preparación, no implementación | Resolver bloqueos, decisiones de producto y `ML-15`; autorizar individualmente los ADR aplicables. |
| 1 | I1-H01 — Fundación ejecutable mínima | Versiones fijadas, Maven/JDK/PostgreSQL/Flyway/ArchUnit/CI/health. Es habilitadora, no un release. |
| 2 | I1-H02 — Catálogo institucional consultable | Fuente gobernada, API, OpenAPI, cliente, UI, ES/EN y pruebas. La declaración académica queda separada. |
| 3 | I1-H03 — Registrar empresa y administrador | Workflow cross-context resuelto, idempotencia, rate limiting, privacidad, UI y pruebas negativas tenant. |
| 4 | I1-H04 — Verificar correo empresarial | Entrega durable sin token en claro; estados de cuenta y empresa separados; carrera de doble consumo probada. |
| 5 | I1-H05 — Sesión y acceso a mi empresa | Login/refresh/logout, tenant derivado y recurso protegido que demuestre aislamiento en lectura y escritura. |
| 6 | I1-H06 — Registrar y verificar estudiante | Formulario separado, identidad resuelta, privacidad y ausencia de membresía empresarial. |
| 7 | I1-H07 — Declaración académica | Recurso student-owned, alternativa para desconocidos, dominio como indicio y prueba negativa entre estudiantes. |
| 8 | I1-H08 — Cierre del incremento | E2E, observabilidad y endurecimiento; sin acumular frontend o contratos diferidos. |

Desde la primera historia funcional, cada corte debe incluir la parte necesaria
de dominio, aplicación, adapters, persistencia, API, OpenAPI, cliente, UI, i18n,
seguridad y pruebas. La numeración definitiva debe actualizarse al aprobar el
plan para preservar trazabilidad entre historia, commit y evidencia.

## 8. Condición para cambiar el estado de los ADR

Un ADR solo podrá pasar de `Propuesto` a `Aceptado` cuando:

1. sus observaciones bloqueantes y altas estén resueltas o aceptadas expresamente
   como riesgo residual;
2. las decisiones de producto, seguridad, privacidad o arquitectura aplicables
   tengan un responsable y una resolución documentada;
3. el texto del ADR contenga la decisión final y criterios verificables;
4. no contradiga la especificación, el blueprint ni otro ADR;
5. el propietario autorice expresamente el cambio de estado de ese ADR concreto.

La aprobación del informe o del plan no implica la aceptación automática de
ningún ADR ni autoriza la implementación de historias.

# Segunda revisión de ADR-001 a ADR-008 y plan del Incremento 1

- Fecha de revisión: 2026-09-12
- Rama revisada: `increment/01-company-identity-catalog`
- Estado del informe: Final
- Resultado: Bloqueado para aprobar el plan y para cambiar estados de ADR
- Alcance: revisión documental; no autoriza implementación

## 1. Alcance y fuentes revisadas

Se revisaron, en el orden exigido por `AGENTS.md`:

1. `AGENTS.md`.
2. `docs/ProPractix-V2-MVP-Espana-Especificacion-Arquitectura-y-Tecnica-para-Codex.md`.
3. `docs/ProPractix-V2-Blueprint-Dominio-y-Mapas-Legales-Espana.md`.
4. `docs/reviews/revision-adrs-incremento-01.md`.
5. `docs/reviews/resolucion-revision-adrs-incremento-01.md`.
6. ADR-001 a ADR-008, todos en `Propuesto — revisión 2`.
7. `docs/plans/increment-01-plan.md`.

Esta revisión no modifica código, no acepta ADR y no autoriza historias. La regla de
`AGENTS.md:12` obliga a detenerse ante contradicciones entre fuentes y a solicitar una
decisión cuando el cambio afecta dominio, seguridad, privacidad o cumplimiento.

## 2. Conclusión ejecutiva

La revisión 2 corrige la mayor parte de los hallazgos originales, pero el plan aún no
está preparado para aprobación.

- **B-01 está resuelto en su núcleo arquitectónico:** `CompanyOnboarding` tiene un
  propietario y coordina transacciones separadas mediante outbox, estados
  recuperables e idempotencia. Sin embargo, el formulario todavía incluye una
  contraseña que no puede cruzar ese proceso durable conforme a los propios ADR.
  Es un bloqueo nuevo de ejecutabilidad y seguridad, no una reapertura de la regla
  transaccional.
- **B-02 está resuelto en su núcleo criptográfico:** la outbox conserva
  `VerificationId` y el JWS se genera al entregar, sin persistir un bearer token.
  Falta definir cómo obtiene `notifications` el destinatario y el material de entrega
  sin duplicar o registrar PII, por lo que I1-H04 todavía no está Ready.
- **Las 17 tablas nombradas en ADR-006 y en el modelo mínimo tienen propietario.**
  La expresión abierta “delivery metadata” de I1-H04 podría introducir una tabla no
  inventariada; debe eliminarse o concretarse antes de migrar.
- **Tenancy e identidad son coherentes en su estructura principal:** cuenta global,
  `TenantId == CompanyId`, múltiples memberships y tenant activo derivado. Persisten
  una contradicción de rol (`COMPANY_ADMIN` frente a `COMPANY_OWNER`) y decisiones no
  cerradas sobre empresas duplicadas y convergencia de cuentas.
- **ML-15 bloquea correctamente staging/producción sin bloquear I1-H01.** El guard
  técnico está definido, pero todavía faltan responsables nominales y decisiones de
  minimización/retención para que I1-H03 e I1-H06 cumplan DoR.
- **No todas las historias cumplen Definition of Ready.** H01 es la única preparada
  en contenido una vez aceptados sus ADR aplicables. H02-H07 conservan decisiones o
  contratos pendientes y H08 está redactada como gate de release, no como historia.

Por tanto, no se recomienda cambiar ahora el estado de ningún ADR. ADR-002 y ADR-004
están maduros como decisiones aisladas y podrán proponerse primero, tras corregir la
ambigüedad procesal de sus “criterios de aceptación” y con autorización expresa del
propietario.

## 3. Verificación de los objetivos de la segunda revisión

| Comprobación | Resultado | Evidencia |
|---|---|---|
| B-01: una transacción por contexto | Resuelto en arquitectura; bloqueado el transporte de la contraseña | ADR-001:49-77; ADR-005:57-68; plan:94-114 y 167-193 |
| B-02: token de verificación durable | Resuelto criptográficamente; incompleto el contrato de entrega | ADR-005:70-84; ADR-007:70-72; plan:195-216 |
| Propietario de cada tabla | Satisfecho para las tablas nombradas; “delivery metadata” es ambiguo | ADR-006:18-40; plan:70-92 y 214 |
| Tenancy/identidad | Parcialmente satisfecho | ADR-003:14-69; plan:183-193 y 218-244 |
| ML-15 | Satisfecho como gate de entorno; DoR funcional pendiente | ADR-008:69-71; plan:20-34, 173, 260 y 344-350 |
| Definition of Ready | No satisfecho para el conjunto | especificación:1006-1025; plan:143-310 |
| Contradicciones nuevas | Existen | contexto `student`, rol inicial, dependencias de compliance y calendario de declaración académica |
| Incorporación de `student` | Recomendable, pero requiere aprobación y alineación de fuentes | ADR-001:18-36; especificación:181-196 y 262-275; blueprint:206-220 |

## 4. Bloqueos restantes, ordenados por severidad

### 4.1 Crítico — SR-B01: la contraseña no puede atravesar el onboarding durable

I1-H03 incluye la contraseña en el formulario (`docs/plans/increment-01-plan.md:183`),
pero `organization` es propietario del onboarding e `identity` crea la credencial en
otra transacción (`docs/plans/increment-01-plan.md:171-187`). El salto durable se
realiza mediante outbox (`docs/adr/ADR-005-eventos-outbox.md:57-68`), que prohíbe
contraseñas (`docs/adr/ADR-005-eventos-outbox.md:43`). Además, el contenido de la
credencial solo puede ser escrito y conocido por `identity`
(`docs/adr/ADR-006-dominio-jpa-propiedad-tablas.md:22-24`).

No es aceptable enviar la contraseña, un hash reutilizable o una credencial cifrada
en un evento sin una decisión explícita. Tampoco es durable llamar a `identity` de
forma síncrona después del commit y confiar en que el cliente repita el secreto si el
proceso cae.

**Corrección necesaria:** Producto y Seguridad deben elegir una ceremonia concreta.
La opción de menor exposición para el MVP es retirar la contraseña de I1-H03 y crearla
en `identity`, después de consumir el enlace de propósito único, dentro de I1-H04.
Otra opción requiere documentar el sobre cifrado, KMS, TTL, permisos, borrado y modelo
de amenazas en un ADR; no debe inferirse durante la historia.

### 4.2 Alta — SR-B02: `student` es una buena frontera, pero contradice las fuentes aprobadas

ADR-001 incorpora `student` y solo compromete actualizar la especificación técnica
(`docs/adr/ADR-001-monolito-modular.md:18-36`). La especificación vigente no lo
incluye en el inventario ni asigna `StudentProfile` a un aggregate propietario
(`docs/ProPractix-V2-MVP-Espana-Especificacion-Arquitectura-y-Tecnica-para-Codex.md:181-196,262-275`).
El blueprint aprobado tampoco lo contiene en su traducción DDD
(`docs/ProPractix-V2-Blueprint-Dominio-y-Mapas-Legales-Espana.md:206-220`).

Existe además una diferencia de roadmap: la especificación coloca universidad y
titulación declaradas en Incremento 1 (líneas 934-948), mientras el blueprint sitúa
la declaración académica junto con Application en Incremento 3 (líneas 268-274).

La frontera propuesta es arquitectónicamente preferible a convertir `identity` en
propietario de perfil y declaraciones, y no amplía por sí sola el producto. Aun así,
no puede aceptarse silenciosamente conforme a `AGENTS.md:12`.

**Corrección necesaria:** aprobación expresa de Producto y Arquitectura; actualizar
en el mismo cambio tanto la especificación como el blueprint. El roadmap debe aclarar
que I1-H07, si se mantiene, captura una declaración personal editable y no verificada,
mientras Incremento 3 la reconfirma para una candidatura y añade las comprobaciones
que correspondan. Si Producto no aprueba esa distinción, I1-H07 debe salir del
Incremento 1.

### 4.3 Alta — SR-B03: faltan contratos públicos necesarios para privacidad y entrega

ADR-006 exige que el contexto que recopila datos consulte a `compliance` el aviso
`APPROVED` y vigente (`docs/adr/ADR-006-dominio-jpa-propiedad-tablas.md:42`). Sin
embargo, la matriz cerrada de ADR-001 no permite `organization -> compliance` ni
`student -> compliance` (`docs/adr/ADR-001-monolito-modular.md:65-75`). El context map
del plan dibuja `Compliance -> Identity` (`docs/plans/increment-01-plan.md:54-68`),
aunque los propietarios de los endpoints son `organization` y `student`.

Asimismo, ADR-005 indica que `notifications` solicita a `identity` el JWS durante la
entrega, pero no define el puerto, su autorización ni cómo obtiene el correo del
destinatario (`docs/adr/ADR-005-eventos-outbox.md:72-84`). Como el evento conserva
solo `VerificationId`, el correo no puede enviarse con el contrato actual. La posible
“delivery metadata” de I1-H04 tampoco tiene nombre, scope, propietario ni retención
(`docs/plans/increment-01-plan.md:214`).

**Corrección necesaria:** añadir a ADR-001 y al context map los contratos de lectura
`organization/student -> compliance`; definir un puerto interno autorizado
`notifications -> identity` que entregue de forma transitoria destinatario y JWS, o
aprobar otra estrategia equivalente. El correo y el token no se persisten ni se
registran en `notifications` salvo que una política y una tabla explícita lo permitan.
Eliminar “delivery metadata” o añadir toda tabla resultante a ADR-006.

### 4.4 Alta — SR-B04: persisten contradicciones y decisiones abiertas de identidad empresarial

La especificación define `COMPANY_OWNER` como rol con administración de empresa y
miembros (`docs/ProPractix-V2-MVP-Espana-Especificacion-Arquitectura-y-Tecnica-para-Codex.md:485-494`),
pero I1-H03 crea `COMPANY_ADMIN` (`docs/plans/increment-01-plan.md:185`). Ningún ADR
aprueba el nuevo rol.

El plan protege el caso de correo existente, pero no define qué ocurre si el mismo
identificador fiscal se registra con otro correo o con otra clave de idempotencia
(`docs/plans/increment-01-plan.md:183-193`). Crear un tenant duplicado o enlazarlo por
coincidencia son decisiones de producto y seguridad. Además, pedir “país” e
“identificador fiscal tipado por país” anticipa un modelo multijurisdicción, pese a
que D-001 limita el MVP a España
(`docs/ProPractix-V2-Blueprint-Dominio-y-Mapas-Legales-Espana.md:309-339`).

**Corrección necesaria:** usar `COMPANY_OWNER` o aprobar y documentar la nueva
taxonomía; decidir unicidad y recuperación/claim de empresa sin enumeración; limitar
el contrato a España o rechazar explícitamente cualquier país distinto de `ES`. La
validación sintáctica de un identificador fiscal no debe presentarse como verificación
legal de la empresa.

### 4.5 Alta — SR-B05: las historias todavía no satisfacen Definition of Ready

La especificación exige, cuando aplique, actor, propietario, estado inicial/final,
invariantes, tenant scope, contrato, criterios, errores, puerta legal, comportamiento
ante `UNKNOWN`, eventos, migración y pruebas
(`docs/ProPractix-V2-MVP-Espana-Especificacion-Arquitectura-y-Tecnica-para-Codex.md:1006-1025`).

Faltan al menos estos elementos:

- **I1-H02:** fuente/licencia y responsable; contrato de entrada del importador,
  publicación y rollback, no solo el `GET` público (plan:143-165 y 346).
- **I1-H03:** datos definitivos, ceremonia de contraseña, duplicado de empresa,
  transiciones/reintentos/expiración exactos y umbral de abuso (plan:167-193).
- **I1-H04:** contrato de preparación de entrega, destinatario, retención y significado
  exacto de reenvío/invalidez de enlaces anteriores (plan:195-216).
- **I1-H05:** si `active-tenant` usa solo bearer o también refresh cookie; en el segundo
  caso debe entrar expresamente en CSRF/Origin. También debe concretar qué operaciones
  revalidan la membership vigente (plan:218-244; ADR-003:40-47).
- **I1-H06:** request mínimo, estado inicial, respuesta asíncrona/no enumeradora,
  máquina de estados, fallos recuperables y ceremonia para cuenta nueva/existente
  (plan:246-268).
- **I1-H07:** decisión de roadmap y conducta exacta al leer o editar una institución
  despublicada, incluida la conservación de la declaración histórica (plan:270-292).
- **I1-H08:** es una lista de endurecimiento y release, sin un único propietario ni
  contrato funcional. Debe convertirse en gate de cierre o reescribirse con DoR
  completo (plan:294-310).

Los pendientes de catálogo, ML-15, rate limiting y correo tienen una puerta, pero no
un owner inequívoco (`docs/plans/increment-01-plan.md:344-350`). Esto impide cumplir la
condición declarada por el propio plan en sus líneas 353-361.

### 4.6 Alta de proceso — SR-B06: los criterios de aceptación de ADR crean un ciclo

El plan prohíbe toda implementación no desechable antes de aceptar los ADR aplicables
(`docs/plans/increment-01-plan.md:20-23`). A la vez, los ocho ADR llaman “criterios de
aceptación” a evidencias que solo existirán al implementar H01-H07; por ejemplo,
ArchUnit en ADR-001:120-125 y ADR-002:93-98, migraciones en ADR-004:64-68 y pruebas de
relay en ADR-005:101-107.

Leído literalmente, no se puede aceptar el ADR antes de implementar y no se puede
implementar antes de aceptarlo.

**Corrección necesaria:** renombrar esas secciones a “criterios de conformidad de la
implementación” y añadir condiciones documentales para aceptar la decisión: decisores,
alternativas, consecuencias, riesgos aceptados, compatibilidad con fuentes y
autorización del propietario. Las pruebas permanecen como gate de cada historia y de
release, no como requisito retroactivo para adoptar la decisión.

## 5. Riesgos aceptables y mitigaciones

Estos riesgos no bloquean la aceptación de la decisión si el responsable indicado los
acepta y se mantienen sus mitigaciones:

| Riesgo | Motivo para aceptarlo en el MVP | Mitigación y gate |
|---|---|---|
| Un esquema y usuario PostgreSQL para todos los contextos | Reduce coste operativo del monolito inicial | Prefijos/propiedad, puertos, ArchUnit, revisión SQL, pruebas tenant; reevaluar schemas/RLS si crece el riesgo (ADR-006:58-69) |
| Access token con permisos obsoletos hasta 10 minutos | Ventana acotada y evita introspección en cada request | Revocar refresh, versionar autorización y revalidar membership en operaciones sensibles; aceptación conjunta Producto/Seguridad (ADR-003:40-47; ADR-007:32-43) |
| Entrega outbox al menos una vez | Adecuada sin broker en el MVP | Deduplicación, lease, ordering por aggregate, dead letter, métricas y reproceso auditado (ADR-005:45-55) |
| Dependencia mínima de `spring-tx` en application | Reduce ceremonia inicial | Excepción limitada a `application.service`, ArchUnit y revisión si crece (ADR-002:54-56,68-77) |
| UUID v4 y menor localidad de índice | Evita dependencia para UUID v7 | Tipo PostgreSQL `uuid`, índices observados y ADR futuro si hay evidencia de impacto (ADR-004:14-20) |
| Titulación como texto declarado | Evita inventar un catálogo canónico no gobernado | Etiquetar como declaración no verificada, limitar longitud y no enviarla en eventos (plan:270-292) |
| Fixture de aviso solo local/test con ML-15 pendiente | Permite construir H01 y probar infraestructura | Configuración fail-closed en staging/production y prohibición de promover fixture (plan:20-34; ADR-008:69-71) |
| Proveedor de correo diferido | Es un adapter reemplazable | Fijar contrato, test double y owner de Operaciones antes de H04/H08; no cambiar dominio |

No son riesgos aceptables sin resolver: transportar contraseñas por eventos, enlazar
cuentas o empresas por coincidencia, aceptar registros productivos sin ML-15, o crear
un contexto nuevo sin alinear las fuentes aprobadas.

## 6. Decisiones que requieren aprobación de producto u otros decisores

| Decisión | Decisores mínimos | Debe resolverse antes de |
|---|---|---|
| Incorporar `student` y distinguir declaración inicial de reconfirmación en candidatura | Producto y Arquitectura | Aceptar ADR-001/003/006 y el plan |
| Ceremonia de alta de credencial: passwordless tras correo u otra estrategia segura | Producto, Seguridad y Arquitectura | Aceptar ADR-005/007 e I1-H03 |
| Rol inicial `COMPANY_OWNER` frente a nuevo `COMPANY_ADMIN` | Producto y Seguridad | Aceptar I1-H03 |
| Unicidad/claim de una empresa ya registrada y respuesta no enumeradora | Producto, Seguridad y Datos | Fijar esquema e I1-H03 |
| Convergencia con una `UserAccount` existente y UX de reautenticación/enlace | Producto y Seguridad | I1-H03 e I1-H06 |
| Finalidad, campos mínimos, retención, derechos y aviso ML-15 | Producto, Privacidad/Legal y Seguridad | I1-H03/I1-H06 con datos reales y cualquier despliegue productivo |
| Fuente, licencia, actualización y publicación del catálogo | Producto, Datos y Legal/licencias | Aceptar I1-H02 |
| Ventana residual de 10 minutos de permisos | Producto y Seguridad | Aceptar ADR-003/007 |
| Conducta ante institución despublicada en una declaración existente | Producto y Datos | Aceptar I1-H07 |

No deben reabrirse sin evidencia nueva España como primera jurisdicción, la empresa
como tenant principal, la universidad como referencia externa ni la separación entre
correo verificado y empresa jurídicamente verificada.

## 7. ADR que pueden pasar individualmente a `Aceptado`

Ningún estado debe cambiar como consecuencia automática de este informe.

| ADR | Recomendación actual | Condición pendiente |
|---|---|---|
| ADR-001 | No | Aprobar/alinear `student`; completar contratos compliance/entrega; separar criterios de decisión y conformidad |
| ADR-002 | **Maduro como decisión individual** | Ajuste procesal de criterios; la whitelist concreta debe remitir a una matriz vigente y aceptada |
| ADR-003 | No | Aceptar `student`, cerrar convergencia de cuenta, rol y revalidación de membership |
| ADR-004 | **Maduro como decisión individual** | Ajuste procesal de criterios y aclaración menor del reloj autoritativo en operaciones SQL atómicas |
| ADR-005 | No | Resolver contraseña, destinatario/PII y contrato de entrega |
| ADR-006 | No | Aceptar `student` y eliminar o inventariar almacenamiento de `notifications` |
| ADR-007 | No | Cerrar ceremonia de credencial y seguridad de `active-tenant`; depende del contrato de ADR-005 |
| ADR-008 | No | Fijar canonicalización y respuesta concurrente `PROCESSING`; separar criterios de decisión y conformidad |

Una vez aplicadas esas correcciones, el propietario puede autorizar ADR-002 y ADR-004
de forma independiente; esa autorización no acepta los demás ADR ni valida ML-15.

## 8. Correcciones exactas recomendadas por ADR

### ADR-001 — Monolito modular

1. Ampliar ADR-001:36 para exigir que la aceptación de `student` actualice la
   especificación **y** el blueprint, incluidos inventario, aggregates, context map y
   roadmap.
2. Añadir contratos `organization -> compliance` y `student -> compliance` para
   consultar el aviso aplicable.
3. Añadir `notifications -> identity` para preparar una entrega por
   `VerificationId`, si se elige esa solución, y declarar que el resultado es
   transitorio y no registrable.
4. Definir los estados durables mínimos de `StudentOnboarding` o remitir a un contrato
   aceptado que los contenga.
5. Renombrar los criterios de líneas 120-125 como conformidad de implementación y
   añadir condiciones documentales de aceptación.

### ADR-002 — Arquitectura hexagonal

1. Cambiar ADR-002:75 para que los puertos importables dependan de la matriz de
   contratos **vigente y aceptada**, no de una lista incompleta.
2. Precisar que una consulta síncrona cross-context no abre transacción distribuida ni
   permite que el consumidor escriba en el proveedor.
3. Renombrar líneas 93-98 como criterios de conformidad de implementación.

### ADR-003 — Multi-tenancy e identidad

1. Sustituir “se diseñará en la historia correspondiente” (línea 16) por los estados y
   pruebas de seguridad mínimos del enlace/reautenticación de una cuenta existente.
2. Declarar la taxonomía canónica de roles y alinear el plan con ella.
3. Aclarar que toda operación tenant-owned comprueba la membership vigente en
   aplicación y filtra por tenant en persistencia; enumerar qué operación ordinaria,
   si alguna, acepta la ventana residual del JWT.
4. Documentar el comportamiento ante empresa/identificador fiscal ya existente sin
   permitir claim por coincidencia ni enumeración.
5. Separar criterios documentales de aceptación de las pruebas futuras de las líneas
   87-92.

### ADR-004 — Identificadores, tiempo y concurrencia

1. Declarar el reloj de PostgreSQL como autoritativo para el `UPDATE` atómico de
   expiración, y usar `Clock` para decisiones de aplicación/dominio; añadir una prueba
   de borde que evite inconsistencias apreciables entre ambos.
2. Mantener las cuatro pruebas concurrentes como conformidad previa a exponer cada
   endpoint/job, no como prerrequisito circular para aceptar la decisión.

### ADR-005 — Eventos y outbox

1. Prohibir expresamente contraseñas y hashes de credencial en todos los contratos de
   integración y decidir la ceremonia alternativa.
2. Especificar el puerto de preparación de entrega, autorización interna, dato de
   destinatario, no persistencia/no logging, TTL y conducta si la verificación fue
   invalidada entre claim y envío.
3. Distinguir eventos de integración de OpenAPI: publicar sus esquemas versionados en
   una ubicación de contratos de eventos definida por la historia.
4. Para toda PII estrictamente necesaria en provisioning, fijar campos, cifrado en
   reposo, acceso, redacción y retención ML-15; no usar “payload mínimo” como decisión
   suficiente.
5. Mover las pruebas de líneas 101-107 a conformidad de implementación.

### ADR-006 — Dominio/JPA y propiedad de tablas

1. Sustituir “delivery metadata” del plan por campos de outbox ya inventariados o
   añadir `notifications_notification`/`notifications_delivery_attempt` con owner,
   scope, datos permitidos, retención, lectura y escritura.
2. Añadir a la matriz cualquier tabla de rate limiting si el adapter elegido persiste
   estado en PostgreSQL; si es externo, documentar que no hay tabla local.
3. Documentar constraints resultantes de la decisión sobre duplicado de empresa, sin
   confundir unicidad sintáctica con verificación legal.
4. Condicionar las tres tablas `student_*` a la aceptación expresa del contexto.
5. Mover líneas 85-90 a conformidad de implementación.

### ADR-007 — Autenticación, sesiones y CSRF

1. Añadir la ceremonia de creación inicial de contraseña o credencial y sus estados;
   si es passwordless, el enlace consumido autoriza una única creación en `identity`.
2. Especificar si `POST /auth/active-tenant` usa bearer o refresh cookie. Si usa cookie,
   incluirlo en doble envío CSRF y validación de `Origin`.
3. Alinear el rol inicial con la taxonomía aprobada.
4. Asignar owner y valores verificables al rate limiting antes de cada endpoint
   público; los valores pueden variar por entorno, pero no quedar indeterminados al
   aceptar la historia.
5. Mantener las pruebas de líneas 95-101 como conformidad previa a exposición.

### ADR-008 — OpenAPI, errores e idempotencia

1. Definir la canonicalización del fingerprint: operación, versión de contrato,
   método/ruta y comando validado serializado de forma determinista; no depender del
   orden del JSON recibido.
2. Para onboarding sin polling público, fijar que un duplicado concurrente en
   `PROCESSING` recibe el mismo `202` genérico y el mismo identificador opaco, o elegir
   una espera acotada. Eliminar “resultado/polling definido por el endpoint” como
   decisión diferida.
3. Precisar que el guard ML-15 corre antes de deserializar, persistir, auditar o
   registrar el body personal, y que la respuesta 503 tampoco crea idempotencia con
   fingerprint del body.
4. Mover líneas 88-94 a conformidad de implementación y añadir criterios documentales
   para aceptar la decisión.

## 9. Correcciones concretas del plan

1. Corregir el context map para representar `organization/student -> compliance` y la
   interacción bidireccional controlada entre `identity` y `notifications`.
2. Sustituir `COMPANY_ADMIN` por el rol aprobado.
3. Eliminar la contraseña de H03 si se adopta el flujo passwordless y añadir su
   creación de propósito único a H04.
4. Limitar H03 a España y cerrar duplicado/claim de empresa, request exacto, política
   de expiración y criterios Given/When/Then.
5. Completar H02 con owner de fuente/licencia y contrato del importador/publicador.
6. Completar H04 con puerto de destinatario/JWS y una decisión cerrada sobre
   almacenamiento de delivery.
7. Completar H05 con autenticación/CSRF de `active-tenant` y revalidación de membership.
8. Definir request, estados, respuesta y errores recuperables de H06.
9. Alinear H07 con los dos roadmaps y fijar la conducta ante catálogo despublicado.
10. Reconvertir H08 en “Gate R1 — cierre del incremento” o dotarla de DoR completo.
11. Asignar owners: Catálogo = Producto + Datos + Legal/licencias; ML-15 = Producto +
    Privacidad/Legal; abuso = Seguridad + Operaciones; correo = Operaciones +
    Arquitectura.
12. En todos los ADR, separar aceptación de la decisión y conformidad futura de la
    implementación.

## 10. Orden ajustado de historias y gates

El orden funcional puede conservar gran parte de la propuesta, pero necesita gates
explícitos y dependencias reales. Los gates no son historias de implementación.

| Orden | Elemento | Condición de entrada | Resultado mínimo |
|---:|---|---|---|
| 0 | Gate D0 — decisiones documentales | Segunda revisión | `student`/roadmap, contraseña, rol, duplicado de empresa y contratos cross-context resueltos; ADR aplicables autorizados individualmente |
| 1 | I1-H01 — Fundación ejecutable mínima | ADR-001/002/004 aplicables aceptados | Build, PostgreSQL/Flyway, CI, health, configuración y guardrails; sin datos personales ni tablas futuras |
| 2A | Gate C0 — catálogo | Puede avanzar durante H01 | Fuente, licencia, owner, formato de importación, publicación y fixtures aprobados |
| 2B | Gate P0 — privacidad y abuso | Puede avanzar durante H01 | ML-15 mínimo, campos/retención, rate limits y adapter de correo con owners; no bloquea terminar H01 |
| 3 | I1-H02 — Catálogo institucional | H01 + C0 | Importación gobernada, API/OpenAPI/cliente/UI/i18n y consulta pública |
| 4 | I1-H03 — Iniciar onboarding empresarial | H01 + P0 + ceremonia de credencial decidida | `CompanyOnboarding` durable hasta `EMAIL_PENDING`, 202 genérico, sin transportar contraseña |
| 5 | I1-H04 — Verificar correo y crear credencial inicial | H03 + contrato de entrega | Cuenta activa, membership activa, empresa `SELF_DECLARED`, JWS de un uso y recovery probado |
| 6 | I1-H05 — Sesión y acceso a mi empresa | H04 | Login/refresh/logout, tenant activo seguro y aislamiento A/B en aplicación, HTTP y PostgreSQL |
| 7 | I1-H06 — Registrar y verificar estudiante | H04 + H05 + P0 + `student` aceptado | Reutiliza identidad/verificación sin membership empresarial ni enlace silencioso |
| 8 | I1-H07 — Declaración académica | H02 + H06 + roadmap alineado | Declaración student-owned, editable, no probatoria y resistente a cambios del catálogo |
| 9 | Gate R1 — Cierre del incremento | H01-H07 en verde | E2E, recuperación, observabilidad, runbook, backup/restore, seguridad, accesibilidad, i18n y ML-15 habilitado solo en el entorno productivo objetivo |

H02 y la preparación de H03 pueden avanzar en paralelo después de H01 si sus gates
independientes están cerrados. H07 depende tanto del catálogo como del perfil de
estudiante. R1 no debe ocultar trabajo funcional pendiente ni acumular API/UI que
correspondía a historias anteriores.

## 11. Condición para una nueva recomendación de aceptación

Una nueva revisión podrá recomendar el plan y los ADR restantes cuando:

1. se haya resuelto la ceremonia de contraseña sin secreto durable cross-context;
2. `student` y el calendario de la declaración estén alineados en ambas fuentes;
3. la matriz de contratos incluya compliance y preparación de entrega;
4. toda tabla potencial, incluida notifications/rate limiting, tenga owner o quede
   explícitamente fuera de PostgreSQL;
5. rol inicial, duplicado de empresa y alcance español estén cerrados;
6. los owners de catálogo, ML-15, abuso y correo estén nombrados;
7. H02-H07 tengan DoR verificable y H08 sea un gate o una historia completa;
8. aceptación documental y conformidad de implementación no formen un ciclo; y
9. el propietario autorice expresamente el cambio de estado de cada ADR concreto.

Hasta entonces, todos los ADR deben permanecer en `Propuesto` y no debe iniciarse la
implementación de ninguna historia.

# Plan técnico propuesto — Incremento 1

- Estado: Pendiente de revisión final
- Fecha: 2026-09-12
- Rama: `increment/01-company-identity-catalog`
- Resultado: empresa y estudiante con cuentas verificadas, catálogo universitario consultable, declaración académica trazable y aislamiento tenant demostrado.
- Reemplaza: revisión 2 evaluada en `docs/reviews/segunda-revision-adrs-incremento-01.md`.

## 1. Decisiones de producto incorporadas

1. La empresa es el tenant principal y la universidad no necesita cuenta.
2. Verificar el correo activa la cuenta del administrador, pero la empresa queda `SELF_DECLARED`.
3. La verificación empresarial es independiente y será obligatoria antes de publicar ofertas en el Incremento 2.
4. Una `UserAccount` global puede actuar como estudiante y tener membresías en una o varias empresas.
5. El tenant activo se selecciona entre membresías vigentes y se incluye en un access token nuevo.
6. El catálogo inicial contiene universidades españolas gobernadas; la titulación es una declaración textual, no catálogo canónico.
7. La fundación puede desarrollarse con ML-15 pendiente, pero producción no acepta registros personales sin un aviso aprobado y vigente.
8. Cada historia funcional incluye API, OpenAPI, cliente, UI, español/inglés, seguridad y pruebas que le correspondan.
9. `student` es propietario del perfil y de la declaración académica; I1 registra una declaración no verificada e I3 la reconfirma para la candidatura.
10. H03 no recibe contraseña; H04 verifica el correo y crea la credencial directamente en `identity`.
11. El primer miembro empresarial usa `COMPANY_OWNER`.
12. País empresarial, locale y jurisdicción legal permanecen separados según D-005.
13. Países, locales, correspondencias y fallback proceden de configuración; toda etiqueta visible procede de i18n.

## 2. Gates de preparación

No se inicia implementación no desechable hasta que los ADR aplicables estén en `Aceptado`.

| Gate | Owners | Bloquea | Resultado requerido |
|---|---|---|---|
| `D0_DECISIONS` | Producto + Arquitectura + Seguridad | I1-H01 y ADR aplicables | Contextos, ceremonia de credencial, roles, duplicados, contratos y criterios documentales alineados. |
| `C0_CATALOG` | Producto + Datos + Legal/licencias | I1-H02 | Fuente, licencia, formato, actualización, publicación, rollback y fixture aprobados. |
| `P0_PRIVACY_ABUSE` | Producto + Privacidad/Legal + Seguridad + Operaciones | I1-H03 e I1-H06 con datos reales | ML-15, campos/retención, límites de abuso y configuración fail-closed aprobados. |
| `E0_EMAIL` | Arquitectura + Operaciones + Seguridad | I1-H04 | Adapter, remitente/dominio, templates i18n, TTL, métricas y runbook aprobados. |

Los gates son decisiones/evidencias y no historias de implementación. `C0`, `P0` y `E0` pueden prepararse durante I1-H01 sin introducir funcionalidad anticipada.

Antes de exponer el primer registro con datos reales deben estar resueltos:

- finalidad y datos mínimos;
- responsable y contacto de privacidad;
- base jurídica validada;
- aviso versionado y aprobado;
- conservación de onboarding abandonado, verificaciones, idempotencia, outbox y auditoría;
- tratamiento de derechos y eliminación;
- distinción entre información, términos, consentimiento opcional y marketing.

En local/test se podrá usar una fixture claramente no productiva. Esa fixture no se exporta ni promueve. En staging/production, si no existe aviso `APPROVED` vigente, el API devuelve `503 privacy_policy_unavailable` antes de aceptar el body personal.

## 3. Alcance

Incluye:

- fundación reproducible, CI y límites arquitectónicos;
- catálogo consultable de universidades españolas;
- onboarding autónomo de empresa y primer administrador;
- verificación de correo y estados empresariales separados;
- login, refresh, logout y selección de empresa;
- lectura y edición mínima de “mi empresa” con aislamiento tenant;
- onboarding autónomo de estudiante;
- declaración de universidad y titulación;
- frontend mínimo accesible en español e inglés;
- OpenAPI y cliente TypeScript generado;
- observabilidad y pruebas E2E.

No incluye ofertas, candidaturas, entrevistas, prevalidación universitaria, convenios, firmas, prácticas, gates G1–G6, facturación, analytics ni workspace universitario.

## 4. Context map

```mermaid
flowchart TD
    WEB["Cliente web"] --> ID["Identity"]
    WEB --> ORG["Organization"]
    WEB --> INST["Academic Institution"]
    WEB --> STU["Student"]
    ORG --> ID
    STU --> ID
    ORG --> COMP["Compliance: ML-15"]
    STU --> COMP
    STU --> INST
    ID --> NOTIF["Notifications"]
```

Las flechas van del consumidor al proveedor de un contrato público o capacidad; no representan acceso a tablas internas. `identity` prepara destinatario y enlace en memoria y solicita la entrega a `notifications`; no existe la dependencia inversa. `platform` implementa outbox, idempotencia, rate limiting, auditoría y observabilidad por debajo de los puertos, sin aparecer como actor de negocio.

## 5. Modelo mínimo de datos

| Contexto | Tabla | Propósito |
|---|---|---|
| Identity | `identity_user_account` | Cuenta global, correo normalizado y estado. |
| Identity | `identity_password_credential` | Hash versionado de contraseña. |
| Identity | `identity_email_verification` | Verificación, expiración y consumo atómico. |
| Identity | `identity_refresh_session` | Familias refresh y tenant activo opcional. |
| Organization | `organization_company` | Empresa/tenant, país, locale, fingerprint fiscal y estado de verificación. |
| Organization | `organization_membership` | Usuario, tenant, rol, estado y versión. |
| Organization | `organization_company_onboarding` | Process manager y evidencia mínima del aviso mostrado. |
| Student | `student_profile` | Identidad funcional del estudiante asociada a `UserId`. |
| Student | `student_onboarding` | Process manager y evidencia mínima del aviso mostrado. |
| Student | `student_academic_declaration` | Institución seleccionada/opcional y titulación declarada. |
| Academic Institution | `academicinstitution_institution` | Universidad catalogada y fuente. |
| Academic Institution | `academicinstitution_catalog_import` | Versión, fuente, hash y resultado de importación. |
| Compliance | `compliance_privacy_notice` | Aviso, finalidad, versión, vigencia y aprobación. |
| Platform | `platform_outbox_event` | Efectos durables. |
| Platform | `platform_event_consumption` | Deduplicación de consumidores. |
| Platform | `platform_idempotency_record` | Idempotencia sin almacenar requests sensibles. |
| Platform | `platform_security_audit` | Auditoría append-only de acciones sensibles. |
| Platform | `platform_rate_limit_bucket` | Ventanas y contadores por fingerprints HMAC con TTL. |

La migración de cada historia solo crea tablas necesarias para esa historia. No se crearán tablas futuras vacías.

## 6. Recorrido empresarial durable

```mermaid
sequenceDiagram
    actor Admin as Representante
    participant Web
    participant Org as Organization
    participant Id as Identity
    participant Mail as Notifications
    Admin->>Web: Envía empresa, país, locale y correo; sin contraseña
    Web->>Org: POST onboarding + idempotencia
    Org-->>Web: 202 genérico
    Org->>Id: ProvisioningRequested(OnboardingId)
    Id->>Mail: Entrega transitoria de enlace
    Mail-->>Admin: Enlace de propósito único
    Admin->>Id: Verifica correo y crea contraseña
    Id->>Org: AccountEmailVerified
    Org->>Org: Membership activa y Company SELF_DECLARED
```

Si el correo ya pertenece a una cuenta, la respuesta continúa siendo genérica y se envía al propietario un flujo seguro para autenticarse y continuar el onboarding; no se enlaza por coincidencia silenciosa.

## 7. Historias en orden

### I1-H01 — Fundación ejecutable mínima

**Actor y valor:** equipo de desarrollo; puede construir, probar y desplegar una base reproducible.

**Contextos:** ninguno de negocio; configuración y guardrails.

**Resultado observable:** checkout limpio compila con JDK 21, ejecuta migraciones en PostgreSQL 16, expone health/readiness mínimos y falla CI ante una dependencia arquitectónica prohibida.

**Incluye:**

- perfiles `local`, `test`, `staging`, `production` con diferencias mínimas;
- configuración tipada y validada; secretos externos;
- catálogo de países/locales y fallback configurable, sin etiquetas de UI en código;
- Flyway y `ddl-auto=validate`;
- ArchUnit con reglas ADR-001/002;
- logging JSON y `correlationId` básico;
- workflow CI backend, análisis de secretos y `git diff --check`;
- benchmark BCrypt documentado;
- esqueleto del contrato OpenAPI y pipeline de validación sin endpoints funcionales.

**Fuera:** tablas de negocio, registro, auth y frontend funcional.

**Pruebas/aceptación:** Maven Wrapper, contexto Spring, migración vacía, PostgreSQL Testcontainers, health/readiness sin secretos y violación ArchUnit de prueba controlada.

**Migración:** solo baseline técnico si es imprescindible; no crear tablas futuras.

### I1-H02 — Catálogo institucional consultable

**Actor y valor:** visitante/estudiante; encuentra una universidad española sin que esta se registre.

**Contexto propietario:** `academicinstitution`. Owners del dato: Producto + Datos + Legal/licencias mediante `C0_CATALOG`.

**Estado inicial/final:** importación gobernada publicada → resultados públicos paginados; el catálogo no crea afiliación ni elegibilidad.

**Contrato:**

- `GET /api/v1/academic-institutions?query=&cursor=&limit=`;
- puerto administrativo `PublishInstitutionCatalog` que recibe un artefacto CSV UTF-8 con esquema versionado, identificador de fuente, licencia/permiso, fecha efectiva y hash;
- la publicación es atómica; volver atrás significa republicar una versión anterior como nueva decisión auditada, nunca editar una importación histórica;
- OpenAPI design-first y cliente TypeScript generado;
- UI de búsqueda reutilizable, estados vacío/error/loading, ES/EN.

**Invariantes:** fuente, versión, fecha y hash de importación; nombres alternativos controlados; solo importaciones publicadas son consultables; “no encontrada” permanece disponible.

**Errores:** formato o licencia ausente, identificador duplicado/ambiguo, publicación concurrente, validación, rate limit configurado y fallo de catálogo sin exponer SQL.

**Eventos:** `InstitutionCatalogPublished` solo si una importación cambia la versión publicada.

**Migraciones:** institution e import metadata. Sin tabla de titulaciones.

**Criterios:** dado un artefacto válido y C0 aprobado, al publicarlo todas las consultas observan una sola versión; dado un artefacto inválido, la versión anterior permanece disponible; una institución no encontrada puede declararse como texto posteriormente.

**Pruebas:** dominio/importación, rollback de publicación fallida, persistencia, paginación, OpenAPI, UI ES/EN, accesibilidad, paridad i18n y consulta sin crear usuarios.

### I1-H03 — Registrar empresa y primer administrador

**Actor y valor:** representante empresarial; inicia adopción sin intervención universitaria.

**Propietario del proceso:** `organization.CompanyOnboarding`; un handler durable invoca el puerto idempotente de `identity` fuera de la transacción de `organization`, y los resultados regresan mediante eventos.

**Precondición:** `P0_PRIVACY_ABUSE` aprobado en el entorno con datos reales; rate limiting e idempotencia operativos.

**Estado final:** onboarding `EMAIL_PENDING` o estado recuperable; API siempre devuelve `202` genérico sin confirmar existencia del correo.

**Contrato/UI:**

- `POST /api/v1/company-onboardings` con `Idempotency-Key`;
- request exacto: `legalName`, `tradeName?`, `registeredCountry`, `taxIdentifierType`, `taxIdentifier`, `timeZone`, `preferredLocale?`, `administratorName`, `administratorEmail` y `privacyNoticeVersion`;
- no admite contraseña, tenant, estado ni rol;
- formulario empresa separado, aviso versionado, términos diferenciados y todas las etiquetas desde i18n;
- pantalla “revisa tu correo”, sin polling público enumerador.

**País e idioma:** `registeredCountry` se valida contra el catálogo configurado. Puede conservar un país cuya jurisdicción legal aún no esté soportada; esto no habilita ofertas o prácticas. El tipo/validador fiscal se resuelve por registro de estrategias, no por condicionales en `Company`. `preferredLocale` debe estar soportado o se resuelve mediante país→locale y fallback configurado, inicialmente `es`.

**Invariantes:** tenant generado por servidor; empresa inicia `SELF_DECLARED`; cuenta `PENDING_EMAIL` sin credencial; rol inicial fijo `COMPANY_OWNER`; no se aceptan IDs/roles del cliente; cuenta existente requiere `ONBOARDING_CONTINUATION` y autenticación/reautenticación.

**Duplicado empresarial:** país, tipo fiscal y fingerprint HMAC normalizado impiden otro tenant automático. El API conserva `202`; no revela coincidencia ni realiza claim. La continuación exige miembro autenticado o caso administrativo con evidencia independiente.

**Eventos:** `CompanyOnboardingSubmitted`, `AdministratorProvisioningRequested`, resultado de provisioning y `EmailVerificationRequested`.

**Estados y fallos:** `SUBMITTED → IDENTITY_PENDING → EMAIL_PENDING → READY`; errores recuperables conservan el estado anterior y backoff; error terminal pasa a `FAILED`; una verificación no completada dentro del TTL configurado inicial `P7D` pasa a `EXPIRED`. Mientras la retención ML-15 conserve el onboarding, un reenvío al mismo correo puede crear una verificación nueva y devolverlo a `EMAIL_PENDING`; cambiar correo o reclamar una empresa coincidente exige recuperación autenticada o caso administrativo.

**Migraciones:** onboarding, company, user sin credencial, privacidad aplicable, outbox, idempotencia, rate limiting y auditoría necesarias. La PII mínima del process manager se cifra y se elimina o redacta según el valor aprobado en ML-15.

**Criterios:** dado un request válido y no duplicado, responde `202` y llega a `EMAIL_PENDING`; dado un retry con la misma clave/fingerprint, devuelve la misma operación; ante cuenta o empresa coincidente, la respuesta no cambia y no se crea ni enlaza otro tenant.

**Pruebas:** invariantes, país/locale soportado y fallback, jurisdicción no soportada sin fallback legal, transacciones separadas, reintento, eventos duplicados, idempotencia concurrente, rate limit, enumeración, duplicado fiscal, rollback local, body tenant ignorado/rechazado y ausencia de contraseña/request en persistencia.

### I1-H04 — Verificar correo y crear credencial empresarial

**Actor y valor:** administrador pendiente; demuestra control del correo y puede completar el acceso.

**Contextos:** `identity`, `notifications` y reacción de `organization`.

**Estado inicial/final:** cuenta `PENDING_EMAIL` → `ACTIVE`; onboarding `EMAIL_PENDING` → `READY`; membership activa; empresa continúa `SELF_DECLARED`.

**Contrato/UI:**

- el correo apunta a una ruta cliente con token en fragmento para evitar envío automático en logs/referer;
- el cliente elimina el fragmento del historial y ejecuta `POST /api/v1/email-verifications/complete` con token y contraseña nueva directamente contra `identity`;
- `POST /api/v1/email-verifications/resend` responde de forma genérica;
- pantallas válida, expirada, usada y reenvío, ES/EN.

**Entrega:** el outbox contiene solo `VerificationId`; un handler de `identity` recupera destinatario, genera JWS y llama al puerto de correo con datos transitorios. `notifications` no persiste destinatario/enlace/token ni consulta `identity`.

**Invariantes:** `INITIAL_CREDENTIAL_SETUP` permite crear una única credencial; `ONBOARDING_CONTINUATION` nunca cambia la credencial y exige autenticación/reautenticación; JWS sin PII, clave separada y TTL configurable; consumo y alta del hash atómicos; la contraseña no llega a idempotencia/eventos; reenvío invalida enlaces anteriores; outbox nunca contiene token utilizable.

**Eventos:** `AccountEmailVerified` y `CompanyOnboardingReady`.

**Migraciones:** verificación y credencial si no fueron creadas en H03; ninguna tabla propia de `notifications` en I1.

**Criterios:** dado un token válido y contraseña aceptable, se consume una vez, se crea el hash y la cuenta queda `ACTIVE`; un segundo consumo o un token invalidado nunca cambia credenciales; la empresa permanece `SELF_DECLARED`.

**Pruebas:** token válido, firma inválida, propósito/audiencia incorrectos, contraseña inválida, expirado, doble consumo concurrente, reenvío, datos transitorios no registrados, caída del relay y recuperación sin duplicar efectos de dominio.

### I1-H05 — Sesión y acceso a mi empresa

**Actor y valor:** administrador verificado; inicia sesión y administra únicamente su empresa.

**Contextos:** `identity` y `organization` mediante contratos públicos.

**Estado final:** sesión refresh activa, access token en memoria y tenant autorizado seleccionado.

**Contrato/UI:**

- `POST /api/v1/auth/login`;
- `POST /api/v1/auth/refresh`;
- `POST /api/v1/auth/logout`;
- `POST /api/v1/auth/active-tenant`;
- `GET /api/v1/me`;
- `GET` y `PATCH /api/v1/companies/current`;
- login, selector de empresa y configuración mínima; etiquetas y mensajes desde i18n.

**Seguridad de selección:** `active-tenant` exige bearer y no usa refresh cookie. `refresh` usa cookie, CSRF y Origin; puede recibir un tenant preferido no secreto, pero vuelve a comprobar la membership. Toda lectura/escritura tenant-owned de I1 revalida membership en application y filtra tenant en persistencia.

**Idioma inicial:** preferencia explícita del usuario → locale empresarial → correspondencia configurada de `registeredCountry` → fallback configurado `es`. El resultado no altera jurisdicción ni reglas legales.

**Invariantes:** cuenta activa; membership vigente; tenant derivado; access JWT 10 minutos; refresh rotatorio máximo 30 días; CSRF/origin/cookies según ADR-007; versión optimista al editar empresa.

**Errores:** autenticación genérica, tenant no seleccionable, recurso no encontrado cross-tenant, concurrencia y rate limit.

**Eventos/auditoría:** login relevante, refresh reuse, tenant seleccionado, logout y cambio de empresa; sin datos sensibles.

**Migraciones:** refresh sessions y versión de membership/company.

**Criterios:** una cuenta con varias memberships solo obtiene un token tenant para una membership activa; revocarla impide selección, refresh y acceso a “mi empresa”; cambiar locale no cambia país ni jurisdicción.

**Pruebas:** login, claims, cookies, CORS, CSRF, Origin ausente/inválido, `active-tenant` sin bearer, refresh con tenant no autorizado, rotación concurrente, reuse detection, revocación de membresía, selección múltiple, fallback i18n y lectura/escritura negativa entre empresas A/B.

### I1-H06 — Registrar y verificar estudiante

**Actor y valor:** estudiante; crea una cuenta independiente de cualquier empresa.

**Propietario del proceso:** `student.StudentOnboarding`; `identity` provisiona o enlaza cuenta mediante flujo seguro.

**Estado inicial/final:** onboarding `SUBMITTED → IDENTITY_PENDING → EMAIL_PENDING → READY`; `StudentProfile` activo asociado a `UserId`; sin membership empresarial implícita.

**Contrato/UI:**

- `POST /api/v1/student-onboardings` con idempotencia;
- request exacto: `name`, `email`, `preferredLocale?` y `privacyNoticeVersion`; no admite contraseña, tenant o rol;
- responde `202` genérico y reutiliza creación inicial de credencial/reenvío sin mezclar formularios;
- formulario y confirmación de estudiante con textos i18n.

**Precondición:** ML-15 aprobado en producción y rate limiting activo.

**Invariantes:** correo único global; cuenta nueva usa `INITIAL_CREDENTIAL_SETUP`; cuenta existente usa `ONBOARDING_CONTINUATION` y exige autenticación/reautenticación; no se crea duplicado ni se enlaza silenciosamente; email institucional es indicio, no elegibilidad.

**Fallos:** backoff para provisioning/entrega; error terminal `FAILED`; verificación no completada dentro del TTL configurado `P7D` produce `EXPIRED`; mientras exista por retención, reenviar al mismo correo puede devolverlo a `EMAIL_PENDING`; todo resultado público evita enumeración.

**Eventos:** `StudentOnboardingSubmitted`, provisioning/continuation y `StudentProfileActivated`.

**Migraciones:** student profile/onboarding y campos estrictamente necesarios.

**Criterios:** un estudiante nuevo completa verificación y credencial sin crear membership; una cuenta existente solo vincula el perfil después de autenticación; retries no duplican perfil ni cuenta.

**Pruebas:** cuenta nueva/existente, reautenticación, estados/expiración, no enumeración, idempotencia, rate limit, verificación, ausencia de tenant/membership, acceso de otro usuario, i18n y minimización de datos.

### I1-H07 — Declaración académica

**Actor y valor:** estudiante autenticado; declara universidad y titulación para usos posteriores.

**Propietario:** `student`; consulta catálogo mediante contrato público de `academicinstitution`.

**Estado final:** declaración versionada student-owned con institution ID opcional, snapshot del nombre mostrado y texto de institución/titulación proporcionado. Es personal, editable y `UNVERIFIED`.

**Contrato/UI:**

- `GET /api/v1/students/me/academic-declaration`;
- `PUT /api/v1/students/me/academic-declaration` con versión optimista;
- selector de universidad más “no encontrada”; titulación como texto; ES/EN.

**Invariantes:** solo propietario; catálogo no demuestra afiliación; correo/dominio no confirma elegibilidad; se conserva qué parte fue seleccionada, el snapshot histórico y cuál fue declarada libremente. Incremento 3 exige reconfirmación para la candidatura.

**Catálogo cambiante:** no se puede seleccionar de nuevo una institución despublicada. Una declaración existente la conserva con snapshot y estado `INSTITUTION_UNAVAILABLE`; el estudiante puede mantenerla históricamente o sustituirla. La lectura nunca falla solo porque el catálogo cambió.

**Errores:** institución no seleccionable, validación, concurrencia y resource not found no revelador.

**Eventos:** `AcademicDeclarationRecorded` con IDs mínimos, sin texto sensible en payload.

**Migraciones:** `student_academic_declaration`; ninguna tabla canónica de programas.

**Criterios:** guardar una selección publicada conserva ID+snapshot; “no encontrada” conserva texto; despublicar después no borra ni invalida la lectura histórica; cada actualización incrementa versión.

**Pruebas:** encontrada/no encontrada, institución despublicada antes/después de declarar, optimistic locking, estudiante A no lee/escribe declaración B, API/cliente/UI/i18n.

### Gate R1 — Cierre del incremento desplegable

No es una historia funcional ni acumula API/UI diferida. Verifica que empresa, estudiante y operación pueden completar y operar el recorrido construido en H01–H07.

**Incluye:**

- E2E de empresa y estudiante desde navegador;
- prueba E2E con dos empresas y aislamiento de lectura/escritura;
- recuperación de onboarding y outbox;
- observabilidad, dashboards/alertas mínimas y runbook;
- build limpio, migración desde cero y backup/restore smoke test;
- auditoría de dependencias, secretos, accesibilidad e i18n;
- actualización de ADR y documentación con decisiones realmente implementadas.

**Fuera:** cualquier funcionalidad del Incremento 2.

**Salida:** versión desplegable que no depende de una historia futura; ML-15 aprobado para habilitar registros en el entorno productivo objetivo.

## 8. Estrategia de commits y revisión

- Un commit coherente por historia o subcambio técnico revisable.
- No empezar la historia siguiente con la anterior en rojo.
- Antes de cada commit:

```bash
npm run check
git diff --check
git diff --stat
git diff
git status --short
```

- Cada entrega documenta comandos ejecutados, resultados, pruebas omitidas y riesgo residual.
- `push`, merge o publicación requieren autorización y remoto autenticado; nunca force push.

## 9. Definition of Done del Incremento 1

- Journeys empresa y estudiante completos en ES/EN.
- Toda etiqueta, validación, acción, estado y error visible procede de i18n; CI verifica paridad y ausencia de literales de presentación no permitidos.
- Empresa `SELF_DECLARED`; ninguna UI la presenta como jurídicamente verificada.
- País, locale y jurisdicción permanecen separados; añadir configuración de país/locale no modifica aggregates y no existe fallback jurídico.
- Universidad consultable sin registro ni workspace.
- Titulación declarada, no inventada como catálogo canónico.
- Aislamiento tenant y student-owned demostrado con pruebas negativas.
- OpenAPI, cliente y API sincronizados por historia.
- Migraciones reproducibles en PostgreSQL vacío.
- Tokens, idempotencia, outbox y auditoría sin secretos utilizables.
- Onboardings parciales recuperables y observables.
- Registro productivo bloqueado si ML-15 no está aprobado.
- Tests de dominio, aplicación, integración, contrato, frontend y E2E en verde.
- ADR reflejan la implementación; repositorio sin secretos ni artefactos locales.

## 10. Gates externos aún pendientes

- `C0_CATALOG`: fuente definitiva, licencia y fixture antes de I1-H02.
- `P0_PRIVACY_ABUSE`: datos/retención ML-15 y valores de rate limiting antes de I1-H03/I1-H06 con datos reales.
- `E0_EMAIL`: proveedor, remitente, templates i18n y configuración operativa antes de I1-H04.
- Verificación empresarial: se diseñará antes de `G1_PUBLICATION` en el Incremento 2.
- Catálogo canónico de titulaciones: fuera del Incremento 1.

## 11. Condición para aprobar este plan

Una revisión final debe confirmar que:

1. SR-B01 a SR-B06 están resueltos sin introducir ciclos;
2. ADR-001 a ADR-008 no se contradicen y separan aceptación documental de conformidad futura;
3. H01 está Ready tras aceptar sus ADR y H02–H07 tienen gates explícitos antes de empezar;
4. cada tabla, contrato, país/locale y texto visible tiene propietario o mecanismo de configuración;
5. aprobar el plan no aprueba automáticamente ML-15, licencias, rate limits o proveedor de correo.

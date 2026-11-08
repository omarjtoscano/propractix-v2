# I1-H03 E3 — Propuesta de persistencia, privacidad y protección fiscal

- Fecha: 2026-11-08.
- Estado del diseño local: **`APPROVED`**, por aprobación expresa del Product Owner el 2026-11-08 sobre `67bc1eb182eaf294173356b825d3ad567777acdd`; sin materialización ni autorización de implementación E3.
- Autorización histórica del PO: preparar exclusivamente este diseño documental, commit, push y Draft PR hacia `increment/01-company-identity-catalog`, sin merge.
- Autorización vigente del PO: registrar la aprobación en este diseño, plan H03, AGENTS.md y cuerpo de la misma Draft PR #26; validar enlaces/diff, commit y push en la misma rama, sin merge. [Registro de aprobación y alcance](#10-aprobación-expresa-del-po--2026-11-08).
- Rama: `docs/i1-h03-e3-persistence-design`, desde el incremento actualizado en `9409b77da81a715a4256c195f2c02bc0e04a4b9f`.
- E2 integrada mediante ese commit ([PR #25](https://github.com/omarjtoscano/propractix-v2/pull/25)), confirmada expresamente por el PO el 2026-11-08. La aceptación funcional permanece sobre `7bd2fed1d06e9a45a041beeb59bb710018247a06`; no se sustituye su evidencia.
- H03 `IN_PROGRESS`; E1/E2 `ACCEPTED`; implementación E3 y posteriores `NOT_AUTHORIZED / NOT_STARTED`. Preparar una propuesta no inicia la entrega E3.

## 1. Fuentes, decisiones propuestas y frontera de E3

Fuentes leídas en orden: [especificación técnica](../ProPractix-V2-MVP-Espana-Especificacion-Arquitectura-y-Tecnica-para-Codex.md), [blueprint](../ProPractix-V2-Blueprint-Dominio-y-Mapas-Legales-Espana.md), [ADR-003](../adr/ADR-003-multitenancy-tenant-context.md), [ADR-006](../adr/ADR-006-dominio-jpa-propiedad-tablas.md), [ADR-012](../adr/ADR-012-desarrollo-local-y-adapters-aws.md), y [plan H03 aprobado](i1-h03-execution-plan.md#e3--persistencia-de-organización-privacidad-y-protección-fiscal). Se aplican también [ADR-004](../adr/ADR-004-identificadores-tiempo-concurrencia.md), [ADR-005](../adr/ADR-005-eventos-outbox.md) y [ADR-008](../adr/ADR-008-openapi-errores-idempotencia.md). Se contrasta con los [contratos y evidencia E2](../reviews/i1-h03-e2-application-evidence.md).

Las decisiones D3-01 a D3-07 que siguen quedan `APPROVED` por aprobación expresa del PO para el alcance local sintético sobre `67bc1eb182eaf294173356b825d3ad567777acdd`. Se conserva su contenido aprobado; no se atribuyen firmas o aprobaciones adicionales de otros responsables ni se cierran gates:

| Decisión | Propuesta concreta |
|---|---|
| D3-01 — Modelo | Esquema único PostgreSQL 16, tablas prefijadas por owner, sin relaciones ORM ni FK entre contextos. Empresa identificada solo por `tenant_id`; onboarding pre-tenant independiente. |
| D3-02 — Relectura | Snapshot del perfil y locale admitidos; reconstrucción sin reevaluar catálogos o políticas actuales. Versión de aggregate separada de la versión técnica de fila. |
| D3-03 — Atomicidad | Transacción de repositorio para onboarding/evidencia/huellas pendientes; otra operación para empresa y todas sus huellas. E3 no ejecuta provisioning ni el submit durable completo de E4. |
| D3-04 — Protección | AES-256-GCM en aplicación, claves externas versionadas; HMAC-SHA-256 fiscal con claves independientes, todas las versiones activas y barrera transaccional de configuración. |
| D3-05 — Aviso | Textos ES/EN y hash reproducible de sección 6, `LOCAL_TEST / TEST_ONLY`; evidencia autoritativa autosuficiente en onboarding. Sin instalar fixture ni publicar política. |
| D3-06 — Retención | Expiración lógica `P7D`, purga verificable e idempotente, teardown tras cada ejecución y límite absoluto local de `P7D` para todo el dataset y sus copias. |
| D3-07 — Evidencia futura | PostgreSQL 16 vacío y upgrade H02, rollback, aislamiento, relectura, concurrencia, corrupción criptográfica y rotación completa según sección 8. |

Este documento propone nombres físicos y operaciones; no contiene migraciones ni añade dependencias. El diseño local, las claves sintéticas y los textos/hash del aviso quedan aprobados con el alcance del registro de la sección 10. La implementación y la materialización de la fixture requieren autorización separada, no concedida por esta aprobación. B0/P0/E0 y la entrada S0 de H03 permanecen pendientes; S0 cerrado solo para H02 y CL0 `PENDING`. La concreción ADR-003/007 continúa pendiente antes de E5.

## 2. Tablas, restricciones e índices

Convenciones: `NN` significa `NOT NULL`; los demás campos son anulables solo donde se indica. UUID v4 generados en aplicación y verificados como en H02; `Instant` se guarda como `timestamptz` UTC, con precisión de microsegundos: el adapter de reloj futuro entrega instantes truncados a microsegundos antes de crear el snapshot; las pruebas usan esa precisión y el mapper rechaza fracciones adicionales en vez de perderlas silenciosamente. Códigos con `text` no vacío y validación sintáctica tipada; no enums SQL por país/locale ni FK contra un catálogo mutable de configuración. Todos los constraints tendrán nombre estable para traducir solo conflictos conocidos. Ningún error expone valores de constraint, huellas o ciphertext.

### 2.1 Matriz de propiedad propuesta

| Tabla | Owner / scope | Escritor y lector permitido |
|---|---|---|
| `organization_company` | `organization`, tenant-owned | Adapter propietario, con tenant derivado del actor o del proceso interno autorizado de creación. |
| `organization_company_onboarding` | `organization`, proceso pre-tenant / tenant futuro | Proceso interno por `OnboardingId`; lectura tenant solo si ya está asociado y coincide el tenant autorizado. |
| `organization_company_tax_fingerprint` | `organization`, índice global restringido | Creación/rotación fiscal; lookup global devuelve solo booleano. |
| `organization_company_onboarding_tax_fingerprint` | `organization`, proceso pre-tenant | Conserva las huellas del `ProtectedTaxIdentity` de E2, sin reservar identidad empresarial. |
| `organization_tax_key_set` | `organization`, control técnico global restringido | Configuración operativa de rotación; lectores/escritores fiscales con comparación de epoch. Solo IDs de versiones, nunca claves. |
| `compliance_privacy_notice` | `compliance`, global por finalidad/jurisdicción/scope | Repositorio de compliance; consumo mediante su puerto público de lectura. |
| `compliance_privacy_notice_text` | `compliance`, contenido versionado | Mismo owner; textos ligados a una versión inmutable. Sin PII de solicitantes. |

Esta matriz amplía explícitamente la de ADR-006 para las tres tablas auxiliares nuevas. No crea `tenant`, memberships, cuentas, outbox/inbox, idempotencia ni auditoría de plataforma. Esas tablas/capacidades se entregan en sus pasos autorizados del plan; no se anticipan migraciones de E4–E6.

### 2.2 Empresa — `organization_company`

| Columna | Tipo / nulabilidad | Regla |
|---|---|---|
| `tenant_id` | `uuid NN`, PK | Único identificador de Company/tenant; sin `company_id` alternativo. |
| `legal_name_encrypted`, `trade_name_encrypted` | `bytea NN` / `bytea` opcional | Envelopes autenticados; ausencia de nombre comercial se conserva como ausencia. |
| `registered_country`, `tax_identifier_type` | `text NN` | País canónico y código fiscal tipado capturados al admitir; no acreditan jurisdicción ni verificación. |
| `tax_identifier_encrypted` | `bytea NN` | Recuperable solo dentro de organization para la finalidad autorizada, nunca texto fiscal en columna o índice. |
| `preferred_locale` | `text` opcional | Preferencia original, sin sustituir ausencia por el fallback. |
| `initial_locale`, `time_zone` | `text NN` | Locale ya admitido y zona IANA del perfil original. |
| `status` | `text NN` | E3 admite únicamente `SELF_DECLARED`; no habilita estados futuros. |
| `created_at` | `timestamptz NN` | Instante técnico de creación. |
| `row_version` | `bigint NN`, inicial 0 | `CHECK >= 0`, versión de concurrencia del adapter. |

La PK ya es un índice por tenant. Añadir `UNIQUE (tenant_id, registered_country, tax_identifier_type)` como destino de la FK compuesta de las huellas; no impone unicidad fiscal por sí sola. No se indexan nombres/correo/fiscal cifrados. No se añade búsqueda global de empresas ni listado público.

### 2.3 Onboarding — `organization_company_onboarding`

| Columnas | Tipo / nulabilidad | Regla |
|---|---|---|
| `onboarding_id` | `uuid NN`, PK | Identidad interna del proceso; nunca polling público. |
| `operation_id` | `uuid NN`, UNIQUE | ID opaco E2 distinto de `onboarding_id`; `CHECK operation_id <> onboarding_id`. No es una clave de idempotencia. |
| `tenant_id` | `uuid` opcional | `NULL` en submit E2; FK a empresa del mismo owner, `ON DELETE RESTRICT`. No existe un tenant provisional ficticio. |
| `legal_name_encrypted`, `trade_name_encrypted`, `tax_identifier_encrypted` | `bytea` | Snapshot admitido; nombre legal y fiscal obligatorios mientras no se haya purgado PII; nombre comercial opcional. |
| `administrator_name_encrypted`, `administrator_email_encrypted` | `bytea` | Obligatorios mientras el proceso conserve PII; dirección canónica con case/puntos/`+` del local-part intactos. Sin columna auxiliar en claro de dominio o correo. |
| `registered_country`, `tax_identifier_type`, `initial_locale`, `time_zone` | `text NN` | Snapshot admitido, con las mismas reglas que empresa. |
| `preferred_locale` | `text` opcional | Conserva ausencia o preferencia exacta. |
| `admission_policy_id`, `admission_policy_version` | `text NN` | `PolicyReference` admitida; no FK a política vigente. |
| `status`, `continuation` | `text NN` | Estados E1; continuación `AWAIT_IDENTITY` o `COMPANY_REVIEW_REQUIRED`, independiente del estado. |
| `initial_role` | `text NN` | `CHECK = 'COMPANY_OWNER'`; no membership ni concesión de permisos. |
| `routing_result` | `text` opcional | `MAIL_CAPABLE`, `NO_MAIL_ROUTE` o `INDETERMINATE`; sin respuesta DNS. |
| `submitted_at`, `expires_at`, `last_changed_at` | `timestamptz NN` | `expires_at > submitted_at`, `last_changed_at >= submitted_at`. |
| `aggregate_version`, `row_version` | `bigint NN` | Ambas no negativas; semánticas distintas descritas en sección 3. |
| `pii_purged_at` | `timestamptz` opcional | Marca técnica idempotente de purga; sin inventar un estado nuevo de dominio. |
| Evidencia `privacy_*` | Ver debajo | Columnas inmutables procedentes de `PrivacyEvidence.resolvedNotice`, no del request. |

Constraints de estados: `SUBMITTED`/`IDENTITY_PENDING` exigen tenant nulo; `EMAIL_PENDING`/`READY` exigen tenant no nulo; `FAILED`/`EXPIRED` conservan el tenant previo si lo había. `READY` está reservado para H04: persistir su código no permite alcanzarlo en E3. Una asociación posterior no puede cambiar de tenant. La continuación de revisión no reclama ni guarda el ID de la empresa coincidente.

Si `pii_purged_at IS NULL`, los cuatro campos personales obligatorios deben existir y tener un envelope de tamaño mínimo válido; si no es nulo, los cinco campos cifrados deben ser nulos. El repositorio no reconstruye `PendingSubmission` desde una fila purgada. Los metadatos restantes también son datos vinculables y se eliminan en teardown local, nunca se consideran anónimos por carecer de nombre.

Evidencia embebida NN: `privacy_notice_id text`, `privacy_notice_version text`, `privacy_content_sha256 char(64)` con regex hexadecimal minúscula, `privacy_purpose text`, `privacy_jurisdiction text`, `privacy_status text`, `privacy_scope text`, `privacy_valid_from timestamptz`, `privacy_valid_until timestamptz`, `privacy_recorded_at timestamptz`. Exigir finalidad `COMPANY_ONBOARDING`, intervalo positivo y `valid_from <= recorded_at < valid_until`. Pares admisibles: `TEST_ONLY / LOCAL_TEST` o `APPROVED / OPERATIONAL`; el adapter/wiring limita E3 local al primero. `recordedAt` es el instante servidor de E2, compartido con la resolución de ese submit: no se inventa otro timestamp de lectura/presentación. Sin IP, user-agent, casilla de consentimiento o prueba ficticia de lectura.

No hay FK a compliance ni cascade desde el aviso: sustituir o suspender una versión no reescribe evidencias históricas. Se guarda el snapshot completo de `Notice`, incluida su huella obligatoria, incluso si el catálogo de avisos cambia después.

Índices: `UNIQUE(operation_id)`; `(tenant_id, onboarding_id)` parcial donde tenant no es nulo para accesos tenant; `(expires_at, onboarding_id)` parcial para estados pendientes y PII sin purgar; `(pii_purged_at, onboarding_id)` para el borrado de tombstones. La PK soporta acceso exclusivo de proceso. Ningún índice sobre correo/nombre/ciphertext. E3 no añade columnas de leasing/retry de E4/E7C.

### 2.4 Huellas fiscales

`organization_company_tax_fingerprint`:

- `company_id uuid NN`: referencia al **mismo** `organization_company.tenant_id`, no otro identificador de empresa; `registered_country text NN`, `tax_identifier_type text NN`, `key_version text NN`, `fingerprint bytea NN` de exactamente 32 bytes y `created_at timestamptz NN`.
- PK `(company_id, key_version)`; FK compuesta `(company_id, registered_country, tax_identifier_type)` a empresa, `ON DELETE CASCADE` solo para borrado autorizado del owner. Evita un país/tipo discordante con la empresa.
- Constraint UNIQUE global `(registered_country, tax_identifier_type, key_version, fingerprint)` de ADR-003/006. El scope global es deliberado: incluir el tenant en esa unicidad permitiría duplicar empresas.
- La UNIQUE sirve al lookup por país/tipo/versión/huella; la PK sirve a cobertura/backfill por empresa. No hace falta un índice de fingerprints sin país/tipo/versión.

`organization_company_onboarding_tax_fingerprint`:

- `onboarding_id uuid NN`, `registered_country text NN`, `tax_identifier_type text NN`, `key_version text NN`, `fingerprint bytea NN` de 32 bytes y `created_at timestamptz NN`.
- PK `(onboarding_id, key_version)`; FK compuesta a `(onboarding_id, registered_country, tax_identifier_type)` del onboarding (requiere UNIQUE equivalente), `ON DELETE CASCADE`. Ninguna FK a una empresa coincidente.
- **Sin UNIQUE global fiscal ni consulta anti-duplicado sobre pendientes**: varios solicitantes pueden presentar el mismo fiscal, sin reservar ni reclamar un tenant. Se conserva la lista protegida de E2 para relectura; al crear una empresa se recalculan todas las versiones entonces activas desde el valor cifrado.
- Purga de PII elimina también estas huellas pseudónimas. No se copian a logs, eventos ni respuesta.

El adapter convierte `Fingerprint.value` a/de hexadecimal minúsculo de 64 caracteres para el contrato E2 y a 32 bytes en PostgreSQL; rechaza encoding inválido o versiones duplicadas. El conjunto activo completo se valida por transacción, no mediante un `CHECK` que consulte otras filas.

### 2.5 Aviso y textos — propiedad de `compliance`

`compliance_privacy_notice`: PK `(notice_id text, notice_version text)` NN; `purpose text NN`, `jurisdiction text NN`, `scope text NN`, `status text NN`, `content_sha256 char(64) NN`, `content_format text NN` (`company-onboarding-notice-json-v1`), `valid_from timestamptz NN`, `valid_until timestamptz NN`, `row_version bigint NN` inicial 0. Hash canónico y vigencia positiva. Códigos de estado/scope del contrato E2; `TEST_ONLY` exige `LOCAL_TEST`, `APPROVED` exige `OPERATIONAL`. La aplicación local solo selecciona `LOCAL_TEST`; no importa la fixture de staging ni la convierte a este scope.

`compliance_privacy_notice_text`: PK `(notice_id, notice_version, locale)` NN; `locale text NN`, `body_utf8 bytea NN`; FK de las dos primeras columnas al aviso del mismo owner `ON DELETE RESTRICT`. Textos UTF-8 estrictos, NFC, LF y un LF final; se verifican en el repositorio. En la propuesta local se exigen exactamente `es` y `en`. La representación binaria conserva los bytes auditados, sin conversión de saltos de línea por la base. Textos públicos sintéticos sin datos de solicitantes, no necesitan cifrado.

Resolver por `(purpose, jurisdiction, scope)` y `valid_from <= :at AND :at < valid_until`, con estado admisible, leyendo metadatos y textos de una misma versión coherente. Índice `(purpose, jurisdiction, scope, status, valid_from)` y PK de textos. Retornar cero o exactamente una resolución; más de una o hash/textos incompletos produce indisponibilidad, nunca `LIMIT 1` arbitrario.

Para evitar intervalos activos solapados sin añadir extensiones PostgreSQL, cualquier carga/cambio autorizado de avisos toma un bloqueo transaccional exclusivo sobre `compliance_privacy_notice`, comprueba solapamientos de `[valid_from, valid_until)` por finalidad/jurisdicción/scope y escribe metadatos/textos juntos. El bloqueo se limita a esa operación excepcional; no al submit. La carga E3 futura será solo un harness local de pruebas, no un publicador operativo. Contenido/hash/referencia son inmutables: un cambio de bytes exige otra versión; cambios de estado no modifican evidencia ya capturada.

## 3. Repositorios, relectura y concurrencia

Se conservan las responsabilidades de los puertos E2: `CompanyRepository`, `CompanyOnboardingRepository`, `CompanyIdentityLookup`, `TaxIdentityProtectionPort` y `ResolveApplicablePrivacyNotice`. La ampliación mínima propuesta de sus tipos internos se concreta en la sección 3.1; ningún contrato cambia en código en esta PR. JPA, Spring Data y SQL quedan dentro de los adapters propietarios. Ningún repositorio de organization lee tablas de compliance; consume su contrato de lectura sin escribir aceptaciones centrales.

| Operación | Predicado / autoridad |
|---|---|
| `CompanyRepository.find(tenant)` | `WHERE tenant_id = :authorizedTenant`; el parámetro proviene de aplicación/proceso, jamás del body como autoridad. |
| `insert(ScopedCompany)` | Igualdad scope/Company.id antes del adapter y en el mapper; empresa y huellas activas en una transacción. No alta automática desde submit E2. |
| `findForProcess(onboardingId)` | Capacidad interna exclusiva del proceso propietario; devuelve solo registro no purgado, sin endpoint, listado global ni uso como fallback de un acceso tenant. |
| `findForTenant(tenant, onboardingId)` | `WHERE tenant_id = :authorizedTenant AND onboarding_id = :id AND pii_purged_at IS NULL`; pre-tenant y tenant ajeno dan vacío. |
| Escritura tenant posterior | `WHERE tenant_id = :authorizedTenant AND onboarding_id = :id AND row_version = :expected`; cero filas es conflicto/no encontrado según la comprobación autorizada, sin lookup global posterior. |
| Listado tenant de prueba | Si se incorpora una proyección interna para probar listados, exige tenant y filtra en SQL antes de paginar; no nuevo endpoint ni listado pre-tenant. |
| `CompanyIdentityLookup.exists` | `EXISTS` sobre huellas de empresas para **todas** las versiones activas; OR entre coincidencias. Devuelve exclusivamente booleano, sin tenant/ID/nombre. |

La membresía vigente se comprobará por aplicación cuando exista la ceremonia de actor de H05. En E3 se ensaya con scopes internos sintéticos A/B; no se acredita autenticación ni se introduce un bypass de autorización para uso futuro. El proceso pre-tenant y la administración excepcional son capacidades distintas de un actor empresarial.

Relectura: descifrar con la versión de clave de cada envelope; reconstruir `CompanyProfile`, correo, `PolicyReference`, `initialLocale`, estado, tenant opcional, resultado de ruta, instantes y versión del snapshot persistido. No llamar a `CompanyOnboarding.submit`, ni repetir admisión, resolver el aviso actual, recalcular locale o ejecutar transiciones para simular restauración. `Company.selfDeclared(id, storedProfile, storedInitialLocale)` ya conserva esa decisión para empresas. El onboarding necesita una factoría de restauración tipada con validación estructural/invariantes, que se propondrá dentro de la implementación E3 autorizada; E1/E2 no cambian en esta PR. Una política o mapeo país→locale nuevo se aplica a altas nuevas, sin bloquear un proceso ya admitido.

`aggregate_version` conserva exactamente `CompanyOnboarding.version()` (el submit E2 admitido llega a 2 tras dos transiciones); `row_version` es `@Version` del adapter, inicial 0. No se fuerza a Hibernate a insertar la versión de dominio como si fuera una versión ORM. Cada escritura compara/incrementa `row_version`; una transición guarda también la nueva `aggregate_version`. Reencryptar o purgar incrementa solo la versión técnica; no inventa eventos de dominio. Company, actualmente inmutable, solo necesita versión técnica. El snapshot técnico cargado conserva la versión esperada sin exponer JPA al dominio.

Los puertos E2 aún no exponen actualización/expiración/purga. La implementación E3 deberá añadir operaciones internas específicas con versión esperada y scope, más la restauración, para demostrar estos casos; no un `save` global ni CRUD público. Un registro purgado devuelve ausencia al proceso, no un aggregate con cadenas vacías. Conflicto de versión se conserva como resultado tipado; el mapeo HTTP `409 concurrent_modification` pertenece a E8.

### 3.1 Ampliación mínima de los contratos fiscales internos — propuesta

| Tipo u operación | Concreción propuesta para una implementación posterior autorizada |
|---|---|
| `TaxKeySetSnapshot(epoch, orderedActiveVersions)` | Valor inmutable capturado **antes del cálculo**, con epoch positivo y lista ordenada completa, sin duplicados. Identifica la configuración usada; los IDs de clave nunca se reutilizan para otros bytes y cada cambio del conjunto incrementa el epoch. No contiene secretos. |
| `FingerprintBatch(calculatedUnder, fingerprints)` | Une inseparablemente ese snapshot y las huellas calculadas con él. Una huella por versión, en el mismo orden y sin faltantes/extras. Lo produce el adapter criptográfico; no se recibe del cliente ni se reconstruye añadiendo el epoch actual a una lista anterior. |
| `ProtectedTaxIdentity` de escritura | Mantiene versión de cifrado, pero sustituye el ciphertext indiferenciado por `TransientTaxCiphertext` y la lista suelta por `FingerprintBatch`. Es el resultado de `TaxIdentityProtectionPort.protect(country, taxIdentifier)` y solo sirve como preparación transitoria para escritura. |
| `StoredTaxIdentity` de lectura | Tipo distinto: `RowBoundTaxCiphertext` con contexto de fila de origen y huellas históricas por versión. No posee un `FingerprintBatch` autorizado para una escritura nueva. No hay conversión por cast ni constructor que lo etiquete como transitorio. |
| Entrada de repositorios y lookup | `PendingSubmission.protectedTaxIdentity` y `ScopedCompany.protectedTaxIdentity` transportan sin separar/modificar el `ProtectedTaxIdentity` calculado. `CompanyIdentityLookup.exists` recibe el `FingerprintBatch` completo en lugar de una lista suelta; comprueba su snapshot bajo bloqueo compartido en una transacción breve de lectura antes de consultar, y sigue devolviendo solo booleano. Un snapshot obsoleto produce fallo técnico tipado, no una coincidencia inventada. |
| Salida de repositorios | `findForProcess`/`findForTenant` devuelven `LoadedSubmission`, con los mismos datos admitidos/evidencia, `StoredTaxIdentity` y versión técnica; `CompanyRepository.find` devuelve análogamente `LoadedCompany` con scope autorizado. Estos resultados de lectura no son entradas insertables. |
| Revalidación del origen | Operación interna de `CompanyOnboardingRepository`: `lockForCompanyCreation(onboardingId, expectedRowVersion)`, con capacidad de proceso. Dentro de la transacción y después del bloqueo del key set, bloquea la fila origen, comprueba versión, vigencia mediante PostgreSQL y estado elegible; devuelve `LoadedSubmission` o conflicto/indisponibilidad tipados. El coordinador compara su perfil con la preparación antes de insertar empresa y asociar el onboarding en esa misma transacción. |
| Soporte criptográfico del adapter | Operaciones distintas para abrir un ciphertext transitorio, abrir uno de fila con su contexto esperado y cifrar para una fila destino. El contexto esperado se deriva de la consulta autorizada: owner, tabla, PK, campo, país/tipo y formato; no se confía en una etiqueta del envelope como autoridad. Se comprueba también la versión de cifrado interior/exterior. |

No se añade un nuevo caso de uso público: tras una lectura autorizada, el proceso propietario puede llamar de nuevo al `protect` existente con el fiscal canónico recuperado en memoria. Ese nuevo cálculo genera otra preparación completa; no reutiliza las huellas históricas. La ampliación separa entrada/salida de persistencia y añade el snapshot de cálculo sin tipos SQL/JPA, acceso cross-context ni cambios al comando HTTP. No inicia E3 ni modifica los contratos aceptados E2 en esta entrega documental.

## 4. Límites de las transacciones y fallos

1. **Preparación E2, conservada:** políticas y aviso autoritativo preceden cifrado/fingerprints, lookup fiscal y ruta técnica. No se mantiene una transacción SQL abierta durante DNS, cifrado costoso o llamadas a otro contexto. La evidencia expresa la resolución autoritativa realizada en ese instante; el mapper no la sustituye por otra versión vigente al insertar.
2. **Inserción E3 de onboarding:** transacción `organization` con fila del proceso, evidencia embebida y todas las huellas pendientes. Bajo bloqueo compara el `FingerprintBatch.calculatedUnder` recibido desde el cálculo con el epoch/conjunto activo, además de validar la representación protegida; no obtiene un comprobante nuevo en el repositorio. Cualquier fallo revierte las tres partes. No escribe compliance, identity ni una empresa.
3. **Creación empresarial E3 como capacidad de repositorio:** un proceso autorizado futuro vuelve a evaluar el duplicado fiscal con todas las versiones activas al crear, porque el lookup previo al submit no reserva el fiscal. Empresa, huellas activas y asociación/transición del onboarding, cuando proceda, comparten una transacción de organization. En E3 las pruebas de esa capacidad usan escenarios internos sintéticos, sin handler de provisioning o llamada a identity. La UNIQUE resuelve la carrera final: dos procesos que no vieron coincidencia no crean dos tenants. El conflicto conocido se trata fuera de la transacción fallida como revisión sin reclamación; la respuesta pública futura sigue siendo genérica `202`, nunca un `409` revelador por duplicado fiscal.
4. **Compliance:** inserción/lectura coherente de aviso/textos en su propio repositorio/transacción. No participa en la transacción de registro ni recibe PII. No hay transacción distribuida.
5. **Rotación, expiración y purga:** lotes breves idempotentes, bloqueo de fila/versión esperada y predicados de tiempo; no transacción de todo el dataset.

E2 exige atomicidad de onboarding + outbox + auditoría en `OnboardingUnitOfWork`. **E3 no puede acreditarla todavía**: el plan asigna las tablas técnicas y la entrega recuperable a E4. Se propone validar en E3 únicamente los repositorios y su rollback local con harness interno; mantener el submit sin wiring ejecutable hasta disponer de los adapters de outbox/auditoría y del boundary completo. No sustituirlos por no-ops ni ejecutar dobles de producción. E4 deberá probar el conjunto con sus adapters propietarios participando en la transacción JDBC local, sin acceso de organization a sus tablas.

`COMMITTED` exige confirmación de commit; rollback conocido permite `ROLLED_BACK`; pérdida de conexión o confirmación ambigua produce `UNKNOWN / CommitUncertain` aunque no se encuentre después ninguna fila. No repetir automáticamente por ausencia aparente de `operation_id`: puede haber una transacción aún en curso. E3 puede comprobar/diagnosticar esa fila por capacidad interna, pero no implementa reconciliación durable ni idempotencia E6. Una UNIQUE de operación evita repetir el **mismo** ID; no deduplica dos submits con IDs nuevos. Se conserva la corrección autoritativa de E2.

Rollback significa rollback de transacción fallida. Flyway es forward-only: no scripts down, no editar V1/V2 de H02, no deshacer migraciones aplicadas para volver de versión. La futura entrega propone nuevas migraciones después de V2, numeración a confirmar desde su base actualizada. Ensayar app H02 anterior sobre esquema ampliado sin uso de tablas nuevas; si se necesita reparación, migración correctiva posterior. Restore solo en bases efímeras sintéticas de pruebas, separado del rollback de aplicación.

## 5. Cifrado, HMAC y rotación sin pérdida de unicidad

### 5.1 Cifrado recuperable y claves

Propuesta: cifrado en el adapter antes de enviar parámetros a PostgreSQL, con `AES/GCM/NoPadding`, clave de 256 bits, nonce aleatorio de 96 bits generado con `SecureRandom` y tag de 128 bits. No usar cifrado determinista, hash simple del fiscal ni `pgcrypto` con claves en SQL. GCM aporta cifrado autenticado con datos asociados según [NIST SP 800-38D](https://csrc.nist.gov/pubs/sp/800/38/d/final); los tamaños y el formato siguientes son decisiones propuestas del diseño.

Envelope binario versionado: versión de formato `1`, identificador de algoritmo, `encryption_key_version`, nonce, ciphertext y tag; campos de longitud explícita y validación estricta antes de descifrar. AAD canónica con longitudes UTF-8: aplicación/entorno, owner, tabla, PK, campo, versión de formato, país/tipo fiscal cuando corresponda. Copiar un envelope entre filas/campos/entornos debe fallar. No aparece PII en el AAD ni en identificadores de claves. Nonce nuevo por cifrado/reintento; no reutilizar ciphertext entre onboarding y empresa.

El puerto `TaxIdentityProtectionPort.protect` corre antes de asignar `OnboardingId`: su `TransientTaxCiphertext` usa AAD de finalidad fiscal + país/tipo + formato, sin inventar una PK. Su representación textual es Base64 URL sin padding del envelope completo; `encryptionKeyVersion` debe coincidir con el identificador interior. El mapper solo admite esta variante como entrada de preparación: la abre con el contexto transitorio, verifica coherencia con país/tipo/perfil y cifra de nuevo para la fila destino, con su PK/campo y nonce nuevo. PostgreSQL conserva los bytes del nuevo `RowBoundTaxCiphertext`. Ningún fiscal se persiste en claro.

Al releer, el resultado protegido es `StoredTaxIdentity`, **nunca** un `ProtectedTaxIdentity` transitorio que contenga el blob almacenado. El adapter abre el ciphertext en el contexto de la fila realmente consultada y rehidrata el VO solo en memoria; sus huellas almacenadas siguen siendo históricas. Pasar ese blob al descifrador transitorio o a una fila de otro tenant debe fallar, aunque coincidan valor fiscal y clave de cifrado. Esta distinción se aplica también a la relectura de Company.

#### Recorrido propuesto: onboarding persistido → empresa

1. El proceso autorizado relee el onboarding mediante su capacidad interna y obtiene `LoadedSubmission`. El mapper usa el contexto esperado de `organization_company_onboarding / onboarding_id / tax_identifier_encrypted`, país/tipo y formato de esa fila, y su versión de clave de cifrado; autentica y descifra el envelope **en origen**. Reconstruye el perfil y locale admitidos sin reevaluar políticas. Fila purgada, tag inválido, contexto discordante o clave ausente impiden continuar.
2. El proceso conserva ese perfil, locale y la versión técnica de origen; el fiscal canónico existe solo transitoriamente en memoria. Invoca de nuevo `TaxIdentityProtectionPort.protect` con ese país/fiscal. El adapter captura un snapshot de claves vigente y calcula **todas** sus huellas activas, produciendo un nuevo `ProtectedTaxIdentity` transitorio con su `FingerprintBatch`; no copia las huellas históricas ni les asigna el epoch de la lectura.
3. La preparación viaja en el `ScopedCompany` de inserción. En la transacción de organization se bloquea primero el key set y se compara el snapshot de cálculo original; se bloquea/revalida después el onboarding por ID, versión y vigencia, y se comprueba su elegibilidad para la asociación futura. Si cambió desde la preparación, rollback confirmado y nueva lectura/preparación; no insertar usando un perfil obsoleto. Un cambio de epoch tampoco se subsana relabelando el resultado anterior.
4. Con la preparación válida se reconsulta el duplicado fiscal usando todas sus huellas. El mapper abre **solo el envelope transitorio nuevo** y cifra con la clave de escritura vigente para `organization_company / tenant_id destino / tax_identifier_encrypted`, AAD de destino y nonce nuevo. No descifra el blob de onboarding con AAD de empresa ni lo copia a Company. Revalida que el valor corresponda al perfil admitido; errores revierten toda la operación.
5. Empresa, todas las huellas activas y asociación/transición autorizada del onboarding se confirman juntas según sección 4. El ciphertext origen permanece ligado al onboarding hasta su purga; descifrado en origen, cálculo de huellas y cifrado en destino no alteran la evidencia del aviso ni la decisión de locale. Las pruebas E3 futuras usan un harness interno; no se incorpora aquí el handler de provisioning E5.

Nombre legal/comercial se cifran también: pueden identificar personas. Nombre y correo del administrador solo residen cifrados en el process manager; no se duplican en Company. El texto fiscal canónico no aparece en bind logs, mensajes SQL, `toString`, auditoría, outbox o artefactos. Desactivar logs de parámetros y traducir fallos criptográficos a códigos técnicos sin input/clave; tag inválido o clave de descifrado ausente falla cerrado, nunca devuelve texto parcial.

Keyrings distintos: `organization-pii-enc-*` para cifrado; `organization-tax-search-*` para HMAC. Claves de búsqueda fiscal tampoco se comparten con idempotencia, rate limiting, firma o identidad. Ficheros sintéticos generados por ejecución, fuera de Git/imágenes/.env, permisos restrictivos y montaje de solo lectura por config tree conforme a ADR-012. El repositorio solo documenta IDs/esquemas, sin bytes ni secretos de ejemplo reutilizables. Tests pueden generar claves efímeras sin imprimirlas.

Rotación de cifrado independiente: nueva versión para escritura, versiones previas solo para lectura; lote descifra/recifra con nonce nuevo y CAS de `row_version`. Comprobar que no queda envelope con la versión antigua en empresa, onboarding ni copias admitidas antes de retirar esa clave. Una fila fallida detiene la retirada. No cambia fingerprints, fiscal canónico, locale ni evidencia del aviso. No confundir borrar una clave compartida con borrar un registro individual.

### 5.2 Blind index y conjunto activo

Material HMAC propuesto: prefijo de finalidad `propractix/organization/tax-search/v1` seguido de tres componentes `(registeredCountry, taxIdentifierType, normalizedValue)` codificados como longitud de 4 bytes big-endian + UTF-8. Sin concatenación ambigua ni locale del sistema; HMAC-SHA-256 completo, sin truncar. La normalización fiscal pertenece a la estrategia registrada por país/tipo; E3 recibe valores canónicos sintéticos y no introduce reglas por país ni una nueva normalización silenciosa. La normalización del correo sigue ADR-001 y no se sustituye por un motor propio en E3.

`organization_tax_key_set` tiene una fila: `singleton_id smallint` PK con `CHECK = 1`, `epoch bigint NN > 0`, `active_versions text[] NN`, `updated_at timestamptz NN`. Array unidimensional, no vacío, sin elementos nulos/vacíos/duplicados (validación del adapter); versiones ordenadas y con sintaxis estable. Es metadato técnico de fencing, no un almacén de claves ni una política de dominio libre. Las claves externas deben cubrir exactamente ese conjunto para lectura/escritura; el orden fija el orden de inserciones para reducir deadlocks.

El dato que liga cálculo y escritura es `FingerprintBatch.calculatedUnder = TaxKeySetSnapshot(epoch, orderedActiveVersions)`. El adapter de protección obtiene ambos valores juntos en una única lectura de la fila confirmada **antes** de calcular; fija los handles de clave correspondientes a esa lista y valida su disponibilidad/versiones. Calcula el batch con ese snapshot inmutable, aunque otra transacción rote mientras calcula. No combina un epoch recién leído con una lista caché anterior. La configuración puede tener claves preparadas adicionales, pero solo las versiones del snapshot participan en ese batch.

Ese batch se devuelve dentro de `ProtectedTaxIdentity` y llega intacto, pasando por `PendingSubmission` o `ScopedCompany`, hasta el adapter de escritura. El repositorio no lo recrea con sus propios valores actuales. Epoch/lista son metadatos internos de procedencia del cálculo, no una autorización del cliente ni una prueba criptográfica autosuficiente: se confía en el productor criptográfico propietario, se impide su construcción desde entrada externa y las pruebas verifican que calculó realmente cada versión declarada.

Cada transacción que inserta onboarding/empresa, cambia identidad fiscal, completa un onboarding o hace backfill toma `FOR SHARE` sobre la fila del key set y conserva el bloqueo hasta terminar. Compara por separado **igualdad exacta del epoch** e **igualdad de la lista ordenada completa** con `batch.calculatedUnder`; exige además que las versiones de las huellas coincidan exactamente con esa lista y correspondan al país/tipo/valor protegido de la operación. Solo entonces consulta/inserta las huellas. Discrepancia, falta de snapshot o clave ausente provocan fallo cerrado y rollback, sin ninguna fila ni confirmación de recepción. No basta que las versiones sean un subconjunto, ni que la lista vuelva a coincidir en un epoch posterior.

El bloqueo protege la comprobación y las escrituras contra otra rotación: esta toma `FOR UPDATE`, espera a los escritores anteriores y cambia epoch/lista juntos. Orden común: key set, onboarding/empresa por ID y huellas por versión ordenada; el backfill respeta ese orden y produce un batch calculado con el snapshot que mantiene bloqueado. No se actualiza el key set por alta. La compatibilidad de bloqueos está descrita en la [documentación PostgreSQL 16](https://www.postgresql.org/docs/16/explicit-locking.html#LOCKING-ROWS).

Si el batch se preparó antes del cambio de epoch, la lectura bajo bloqueo **solo lo compara y rechaza**; nunca le asigna retrospectivamente el epoch actual. Tras un rollback confirmado se descarta la preparación completa y se vuelve a proteger el fiscal con un nuevo snapshot, recalculando todas las huellas; ante commit incierto sigue prohibido repetir a ciegas. Las tablas de huellas propuestas conservan valores por versión, no un comprobante de cálculo reutilizable. Al releerlas se devuelven como históricas, sin inventar un epoch; no permiten reconstruir un batch válido para insertar ni requieren nuevas columnas para este protocolo.

El lookup previo E2 es solo orientativo. La creación final recibe una preparación recalculada desde el fiscal recuperado en origen; dentro de la transacción protegida valida su snapshot, consulta **todas** sus versiones activas e inserta todas junto a empresa. El backfill puede calcular dentro de su lote con el snapshot ya bloqueado; ninguna de las dos vías reutiliza un batch histórico relabelado. Un fallo de cualquier clave activa falla cerrado; no se permite continuar solo con la nueva ni omitir una huella que no pudo calcularse. Las restricciones UNIQUE PostgreSQL son la autoridad final ante concurrencia, según [PostgreSQL 16, constraints](https://www.postgresql.org/docs/16/ddl-constraints.html#DDL-CONSTRAINTS-UNIQUE-CONSTRAINTS).

### 5.3 Ceremonia propuesta y verificaciones de retirada

1. Estado inicial `epoch=1`, activas `[tax-v1]`. Medir cobertura de todas las empresas y ensayar duplicado de una empresa anterior a la rotación.
2. Distribuir la clave sintética `tax-v2` a todos los procesos capaces de escribir; pausar altas durante la conmutación local. Activar atómicamente `epoch=2`, `[tax-v1,tax-v2]` con bloqueo exclusivo y recargar configuración. Escritor con epoch anterior queda rechazado hasta recargar. Reanudar únicamente cuando ambos keys estén disponibles. No retirar v1.
3. Desde entonces **cada lookup y cada creación** usa ambas versiones, también antes de acabar el backfill. Un fiscal antiguo aún sin v2 coincide por v1; dos altas nuevas compiten en ambas UNIQUE. Recalcular huellas de pendientes al usarlas; no confiar en su lista histórica.
4. Backfill de empresas por lotes ordenados por tenant: bloquear fila, descifrar fiscal dentro del adapter, calcular v2 e insertar la fila faltante. Repetir un lote es seguro: `ON CONFLICT (company_id, key_version)` permite no insertar solo tras verificar igualdad de país/tipo/huella. Un conflicto del índice fiscal global contra **otra** empresa o una huella distinta en la misma versión aborta y alerta; nunca se ignora mediante `ON CONFLICT DO NOTHING` sin target. Mismo tratamiento para renovar snapshots pendientes no purgados; estos no reservan unicidad global.
5. Verificar cobertura mediante anti-join por empresa × versión activa: cero faltantes; recomputar huellas y comprobar cero divergencias/duplicados de valor canónico, claves/ciphertext legibles y carreras superadas. Los informes solo muestran conteos/IDs técnicos mínimos con acceso restringido, nunca fiscales o huellas. Comprobar pendientes y workers en vuelo, backups/caches y lectores/escritores registrados. Un error impide promoción.
6. Propuesta local de ventana operativa: mantener ambas versiones hasta completar un ciclo de arranque/escritura/lectura de todos los procesos, repetir la batería concurrente y verificar que no queda operación en vuelo con el epoch previo; no sustituir esa evidencia por una espera fija. En una transacción exclusiva, volver a verificar cobertura y cambiar a `epoch=3`, `[tax-v2]`. Un proceso `[v1,v2]` obsoleto también falla cerrado. Iniciar operaciones nuevas con v2 únicamente tras recarga.
7. Ensayar duplicados con solo v2. Después de comprobar que no existen lectores/copias dependientes, eliminar filas antiguas y destruir el secreto v1, registrando versión/epoch/conteos y resultado, sin clave. No retirar la clave de cifrado para esta rotación. Para retroceder antes de la retirada, mantener ambas activas; después no volver a v1 sola sin backfill/verificación completos y otra decisión operativa.

La base no puede demostrar por sí sola que una huella es el HMAC correcto: se necesita verificación criptográfica del adapter. La PK por empresa/versión evita repetición local, la UNIQUE global evita dos empresas para la misma huella, y la barrera impide escribir con conjuntos incompatibles. La comprobación booleana nunca enlaza ni revela el tenant coincidente.

## 6. Aviso local propuesto y SHA-256 reproducible

Referencia aprobada para el alcance local sintético: `local-synthetic-company-onboarding`, versión `2026-11-02.1`; finalidad `COMPANY_ONBOARDING`, jurisdicción explícita de prueba `SYNTHETIC-ONBOARDING-JURISDICTION`, scope `LOCAL_TEST`, status `TEST_ONLY`. No deriva de país/locale ni habilita una jurisdicción jurídica. La vigencia de cada ensayo se suministra con reloj fijo en su harness, intervalo finito `[validFrom, validUntil)`; no se hace un aviso perpetuo ni se inventa aprobación legal.

Los textos ES/EN siguientes y su huella exacta quedan aprobados expresamente por el PO sobre el SHA indicado en la sección 10, exclusivamente para `LOCAL_TEST / TEST_ONLY`. No se crea YAML/JSON de política, recurso cliente, loader, seed SQL ni fixture ejecutable. Staging conserva [su fixture separada](../policies/staging-synthetic-privacy-fixture-v1.yaml) intacta. Materializar estos textos/hash en E3/E7A requiere autorización expresa posterior; la aprobación documental no cierra P0 ni convierte el estado del aviso `TEST_ONLY` en `APPROVED` operativo.

### 6.1 Texto ES propuesto

<!-- notice-es:start -->
```text
Aviso de prueba local: alta de empresa y primer administrador

Esta prueba usa exclusivamente datos sintéticos para ensayar el alta de una empresa y la preparación del primer administrador en ProPractix. Introduce solo nombres claramente ficticios, correos de prueba .test o .invalid e identificadores fiscales generados para esta ejecución. No introduzcas datos de personas o empresas reales, copias de producción ni datos de procedencia desconocida.

La prueba conserva el perfil empresarial admitido, el nombre y correo ficticios del administrador y la referencia y huella de este aviso. Protege los campos personales mediante cifrado y utiliza huellas HMAC separadas para ensayar la detección de duplicados fiscales. Admitir la solicitud no verifica la empresa, su representación ni el control del correo.

El uso queda limitado al entorno local de pruebas, sin envío de correo externo ni promoción o exportación a staging o producción. Los datos y sus copias se borran al terminar cada ejecución y, en todo caso, antes de superar siete días. Una solicitud pendiente expira a los siete días. Comunica cualquier dato posiblemente real al responsable de la prueba y detén la ejecución para su eliminación.

Este texto es TEST_ONLY y no es un aviso para tratar datos reales, una aprobación jurídica, una aceptación de términos ni una solicitud de consentimiento para otras finalidades.
```
<!-- notice-es:end -->

### 6.2 Texto EN propuesto

<!-- notice-en:start -->
```text
Local test notice: company and first administrator onboarding

This test uses synthetic data only to exercise company onboarding and preparation of the first administrator in ProPractix. Enter only clearly fictitious names, .test or .invalid test email addresses, and tax identifiers generated for this run. Do not enter data about real people or companies, production copies, or data of unknown provenance.

The test retains the admitted company profile, the administrator's fictitious name and email address, and the reference and content hash of this notice. It protects personal fields with encryption and uses separate HMAC fingerprints to exercise tax identity duplicate detection. Receiving a submission does not verify the company, the applicant's authority, or control of the email address.

Use is limited to the local test environment, with no external email delivery or promotion or export to staging or production. Data and copies are deleted at the end of each run and, in all cases, within seven days. A pending submission expires after seven days. Report any possibly real data to the person responsible for the test and stop the run so that it can be deleted.

This text is TEST_ONLY. It is not a notice for processing real data, legal approval, acceptance of terms, or a request for consent to other purposes.
```
<!-- notice-en:end -->

### 6.3 Bytes cubiertos y reproducción

SHA-256 propuesto del contenido bilingüe: **`968411cdab71a3de77b0195ab00e64612a59f7a7e053cc3fa98a1014c9c81824`**.

Se calcula sobre un objeto JSON canónico propio de formato `company-onboarding-notice-json-v1`, con las claves `contentFormat`, `jurisdiction`, `noticeId`, `purpose`, `scope`, `texts` (objeto `en`/`es`) y `version`. Se serializa con claves ordenadas recursivamente, sin espacios separadores, caracteres Unicode literales y UTF-8 sin BOM ni LF añadido al JSON. Cada texto comprende exactamente los bytes **dentro** de su bloque `text`, incluidos párrafos y un único LF final; exige NFC y LF, no normaliza silenciosamente. Los backticks, marcadores HTML, títulos de Markdown y resto del documento no entran en el hash.

La huella vincula ambos textos, sus locales y los metadatos indicados, no solo ID/versión. No cubre estado de ciclo de vida del aviso (suspenderlo no cambia su contenido), vigencia del ensayo, timestamps del solicitante, ciphertext o evidencia personal: la vigencia se valida y conserva separadamente en el `Notice` autoritativo. Cambiar cualquiera de los bytes cubiertos obliga a recalcular hash y revisar otra versión antes de materializar; este algoritmo no autoriza modificar una versión ya aprobada.

Reproducción documental desde la raíz del repositorio (solo lee este Markdown, no instala una fixture):

```bash
python3 - <<'PY'
import hashlib
import json
import unicodedata
from pathlib import Path

path = Path('docs/plans/i1-h03-e3-persistence-design.md')
document = path.read_bytes().decode('utf-8')
assert '\r' not in document
texts = {}
for locale in ('es', 'en'):
    start = '<!-- notice-' + locale + ':start -->\n```text\n'
    end = '```\n<!-- notice-' + locale + ':end -->'
    assert document.count(start) == document.count(end) == 1
    body = document.split(start, 1)[1].split(end, 1)[0]
    assert body.endswith('\n') and not body.endswith('\n\n')
    assert unicodedata.normalize('NFC', body) == body
    texts[locale] = body
payload = {
    'contentFormat': 'company-onboarding-notice-json-v1',
    'jurisdiction': 'SYNTHETIC-ONBOARDING-JURISDICTION',
    'noticeId': 'local-synthetic-company-onboarding',
    'purpose': 'COMPANY_ONBOARDING',
    'scope': 'LOCAL_TEST',
    'texts': texts,
    'version': '2026-11-02.1',
}
canonical = json.dumps(payload, ensure_ascii=False, sort_keys=True,
                       separators=(',', ':')).encode('utf-8')
print('bytes:', len(canonical))
print('sha256:', hashlib.sha256(canonical).hexdigest())
PY
```

Resultado reproducido en esta revisión: **`3008` bytes**, SHA-256 indicado arriba. Esto verifica los bytes de la propuesta, no su aprobación ni la conformidad de un adapter aún inexistente.

### 6.4 Conservación de la evidencia autoritativa E2

Los dobles E2 suministraban `a`/`b` repetidas 64 veces como hashes sintéticos para demostrar que se guarda **la huella devuelta por compliance**, incluso con la misma referencia. Se conservan el código, pruebas y resultados originales; no se reemplazan por este hash ni se presenta ese histórico como si hubiera usado estos textos.

La futura prueba E3 debe cargar, únicamente tras autorización de implementación/materialización, la pareja exacta referencia/contenido ya aprobada en compliance, verificar sus bytes/hash al leer y persistir en organization el `Notice` devuelto por el puerto. El comando continúa enviando solo la referencia no confiable; no se acepta un hash calculado o enviado por el cliente. Un cambio posterior del aviso no modifica la evidencia ya grabada. Un aviso inválido o inexistente produce cero PII, cero huellas y cero evidencia persistida.

## 7. Retención, expiración y borrado sintético

Las condiciones locales aprobadas del plan H03 prevalecen: dataset exclusivamente sintético, teardown tras **cada** ejecución y máximo absoluto `P7D`, incluidas copias, cachés, logs y artefactos. Las propuestas generales del plan de purga diaria y evidencia a 30 días no autorizan conservar fixtures locales más de siete días; aquí se ensayan con reloj acelerado y se eliminan al finalizar. No se define retención jurídica para datos reales ni se toca una cuenta/empresa ajena por coincidencia fiscal.

- **Expiración lógica:** `expiresAt = submittedAt + P7D` en escenarios aprobados. Exactamente en el borde no se puede completar ni usar PII para provisioning; se marca `EXPIRED` una sola vez. `NO_MAIL_ROUTE`/`INDETERMINATE` no provocan rechazo ni expiración anticipada. Un proceso `READY` futuro no vuelve a expirar como pendiente.
- **Purga por estado/plazo:** operación interna por lotes y CAS; para expirados/fallidos elimina envelopes personales y huellas pendientes, marca `pii_purged_at` y conserva solo tombstone/evidencia mínima hasta el teardown. Para `READY`, el ensayo futuro acredita purga de PII transitoria dentro de 24 h; no alcanza ese estado mediante una ceremonia falsa E3. Snapshot `READY` de test no equivale a aceptar H04.
- **Tiempo:** `Clock` gobierna dominio/aplicación; predicados SQL de expiración/claim usan el instante PostgreSQL conforme a ADR-004. Ensayar antes/en/después del borde y desfase de reloj. Una diferencia de reloj no concede uso extra: antes de cualquier efecto futuro se revalida vigencia en SQL. El hard deadline local elimina dataset/copias aunque aún hubiera procesos pendientes o un margen diario de limpieza.
- **Concurrencia:** la purga y la actualización de estado compiten por `row_version` y bloqueo de fila; relectura interna autorizada antes de reintentar solo cuando se confirmó rollback. No borrar una empresa en uso ni un registro global de identity; limpieza de otros owners mediante sus contratos/teardown del entorno aislado, nunca SQL cross-context desde organization.
- **Borrado completo local:** borrar dependientes fiscales, onboarding, empresas de esa ejecución y sus huellas, avisos/textos de test de su owner; destruir base/volúmenes efímeros y secretos de ejecución. Mailpit no recibe mensajes en E3; si hay un stack compartido de pruebas, su limpieza requiere identificar exclusivamente datos de esa ejecución. No borrar evidencia histórica aprobada de H02 ni sus fixtures versionadas.
- **Copias/artefactos:** sin dumps persistentes de PII; un restore smoke test solo usa snapshot sintético efímero bajo el mismo plazo original. La restauración no reinicia la retención: antes de habilitar acceso se reaplica expiración/purga y se vuelve a verificar ausencia. Eliminar temporales, capturas y backups al finalizar. No prometer borrado físico de páginas/WAL con un simple `DELETE`; el teardown de los volúmenes y copias completa el ciclo local.

E3 propone demostrar operaciones explícitas de expiración/limpieza con harness y reloj controlado, sin scheduler de recheck/relay ni endpoint administrativo. El runbook futuro registrará dataset/versión, inicio, deadline, conteos anteriores/posteriores, fallos y resultado de limpieza, sin PII, claves ni valores HMAC. Fallo de limpieza impide declarar terminado el ensayo.

## 8. Matriz de validación futura y criterios de revisión

**No ejecutada en esta PR documental.** Requiere implementación y autorización E3, Docker y JDK 21 reales; no H2, DNS público, AWS ni envío de correo. Las pruebas significativas se harán con Testcontainers/PostgreSQL 16 y secretos efímeros. Un doble no acredita ninguna de estas propiedades SQL/criptográficas.

| ID | Escenario / preparación | Evidencia exigida |
|---|---|---|
| P3-01 | PostgreSQL 16 vacío, cadena Flyway completa | `migrate` y `validate` correctos; esquema/constraints/índices esperados, sin datos de política en migraciones, JPA `validate`. |
| P3-02 | Base creada con las migraciones H02 V1/V2 y dataset sintético propio | Actualizar con las nuevas migraciones sin tocar checksums H02; catálogo/importaciones y buckets conservados; validar tras reinicio. No usar copia de producción. |
| P3-03 | Rollback de aplicación y de transacción | H02 anterior funciona sobre esquema ampliado; fallo inyectado tras insertar onboarding/evidencia y después de una huella revierte todo. Fallo tras insertar empresa/una versión fiscal no deja tenant ni huellas parciales. No ejecutar down migration. |
| P3-04 | Relectura de perfil/locale | Igualdad de campos opcionales, country/type/fiscal canónico, timezone, case/puntos/`+`, `initialLocale`, política, tiempos/versiones y `Notice` completo/hash. Cambiar país→locale, catálogo/política vigente o idioma del proceso no reevalúa el snapshot; una solicitud nueva sí usa configuración nueva. |
| P3-05 | A/B y pre-tenant | A lee/modifica/lista solo A; B no lee/escribe/lista A, incluso con UUID válido ajeno. Pre-tenant invisible para ambos; proceso interno lo lee por ID. Body/scopes incoherentes rechazados, cero filas ajenas alteradas, sin fallback global ni filtrado posterior a paginar. |
| P3-06 | Concurrencia optimista y purga | Dos transacciones con la misma `row_version`: solo una escritura confirmada; la otra conflicto tipado. Purga vs actualización nunca resucita PII. La versión E1 2 se relee como 2 aunque la versión ORM inicial sea 0. |
| P3-07 | Cifrado y manipulación | Consulta SQL no contiene marcadores sintéticos en claro; descifrado exacto autorizado. Mismo valor cifrado dos veces da nonces/ciphertexts distintos; alterar tag/nonce/key version/AAD o copiar entre campos/filas/entornos falla cerrado. Logs/errores/eventos sin datos ni claves. |
| P3-08 | Independencia de claves | Rotar cifrado no altera HMAC; rotar HMAC no necesita cambiar el fiscal ni la clave de cifrado. Clave activa ausente impide escritura; clave antigua de descifrado ausente impide lectura, sin fallback inseguro. Reencrypt interrumpido se reanuda por CAS sin perder cambios. |
| P3-09 | Antes de rotar: empresa solo con v1 | Duplicado devuelve coincidencia booleana; no tenant/membership nuevo ni ID ajeno. Dos creaciones simultáneas con el mismo fiscal y IDs distintos dejan exactamente una empresa. |
| P3-10 | Durante `[v1,v2]`, empresa vieja todavía sin v2 | Duplicado detectado por v1; altas nuevas insertan ambas. Carrera entre alta duplicada/backfill y dos altas nuevas conserva unicidad; fallo de segunda huella revierte empresa. Pausar/reanudar y repetir lote no cambia el resultado. |
| P3-11 | Backfill corrupto y configuración obsoleta | Colisión con otra empresa o huella divergente aborta/alerta; no `DO NOTHING` global. Escritores viejos bloqueados por epoch, incluido uno preparado antes del cambio; clave activa que falta falla cerrado. Cero retirada si falta una sola fila. |
| P3-11a | Escritor preparado antes de rotar, detenido mediante barrera | A calcula y conserva batch `(epoch=1, [tax-v1])`; B activa y confirma `(epoch=2, [tax-v1,tax-v2])`; A reanuda escritura y toma `FOR SHARE`. Debe comparar el snapshot **original**, rechazar y confirmar rollback con cero onboarding/empresa/huellas/asociación nuevas. Verificar que no se llamó a un constructor para relabelar el batch con epoch 2. Solo una nueva preparación que recalcule v1 y v2 tras rollback confirmado puede persistir ambas. Probar también lista idéntica con epoch posterior, lista incompleta y rotación que espera a un escritor que ya tenía el bloqueo. |
| P3-12 | Después de promover v2 y retirar v1 | Cobertura completa recomputada; duplicado de empresa pre-rotación y dos altas concurrentes siguen dejando una empresa usando solo v2. Lecturas funcionan; rollback no reactiva v1 sola ni omite empresas creadas durante el backfill. |
| P3-13 | Aviso local autoritativo | Hash de ambos textos coincide; alterar un byte/local/metadata cubierta falla. Ausente, expirado, futuro, suspendido, sustituido, otra finalidad/jurisdicción/scope o múltiples aplicables: cero escritura personal. ID/versión del cliente nunca reemplaza `Notice`; cambiar catálogo después no modifica evidencia guardada. |
| P3-13a | Relectura fiscal de onboarding y creación de empresa | Persistir origen con AAD de onboarding y huellas antiguas; releer con contexto esperado de origen y conservar perfil/locale. Activar un conjunto HMAC nuevo y comprobar que `protect` recalcula todas las versiones con su snapshot, sin reutilizar huellas históricas. Insertar destino con AAD de empresa/tenant y nonce nuevo; descifrar ambos con sus respectivos contextos da el mismo fiscal. Rechazar el blob origen como ciphertext transitorio o destino, contexto/tenant ajeno, y versión de origen o epoch cambiados después de preparar. Cero escrituras parciales y evidencia del aviso intacta. |
| P3-14 | Retención antes/en/después de `P7D` | Expiración una vez, impedir uso posterior, purga idempotente, cero huellas/envelopes pendientes tras purga y cero reconstrucción de fila purgada. Probar FAILED y snapshot READY solo como relectura/retención, sin acreditar ceremonia H04. |
| P3-15 | Teardown, copias y restore | Borrado al finalizar y hard cap `P7D` demostrados para dataset, archivos, cachés, Mailpit si aplica y volúmenes efímeros; restaurar no resetea deadline ni recupera PII accesible ya expirada. B no se borra al purgar A. |
| P3-16 | Commit incierto | Fallo de conexión en frontera de commit da incertidumbre, no recepción ni rollback garantizado; ni retry ciego ni creación de otra operación. Prueba del boundary completo, outbox/auditoría e idempotencia asignada a E4/E6, sin acreditarla con este harness. |

Para P3-09/10/12 usar barreras entre transacciones reales, no ejecuciones secuenciales llamadas concurrentes. Comprobar filas finales y constraints después de commit/rollback y tras reinicio; diferenciar error recuperable conocido de fallo de programación. El aislamiento A/B fiscal tiene una excepción documentada: el lookup global booleano de anti-duplicado, jamás un repositorio global de empresas.

El PO aprueba D3-01 a D3-07, incluidos los campos/retención locales, la estrategia de claves sintéticas y rotación, las ampliaciones internas descritas y los textos/hash, sobre el SHA registrado en la sección 10. Sigue pendiente la autorización expresa para implementar E3 y materializar la fixture. Ninguna casilla de esta matriz se marca superada hasta aportar comandos, SHA y resultados de una entrega funcional autorizada. La autorización E3 futura no concede E4/E5 ni cierra gates.

## 9. Validación de esta entrega documental

Validación documental realizada el 2026-11-08: **93 destinos/anclas locales en cinco documentos, cero errores**; reproducción del hash sobre **3008 bytes** mediante el snippet publicado y confirmación independiente con `sha256sum`; coherencia de estados y diff exclusivamente documental revisados. `git diff --check` correcto. No se ejecutan infra, migraciones, `server:test` ni `server:verify`: las 466 pruebas por suite del informe E2 son evidencia anterior, no resultados del SHA documental. CI, Trivy y Build OCI del nuevo SHA los verificará e informará el PO, sin espera ni supervisión del agente.

Concreción posterior solicitada por el PO en la misma Draft PR #26: únicamente procedencia del cálculo fiscal (epoch/conjunto transportados hasta el bloqueo), separación transitorio/fila y recorrido onboarding→empresa, ampliación mínima de contratos internos y pruebas futuras P3-11a/P3-13a. Los textos ES/EN, sus bytes y SHA-256 se conservan; el diff de esta concreción se valida por separado. No se ejecutan las pruebas funcionales propuestas ni se inicia E3. Los checks remotos siguen a cargo del PO.

Entrega limitada a documentación y Draft PR hacia el incremento, con SHA comunicado fuera del propio commit. E1/E2 siguen `ACCEPTED`, H03 `IN_PROGRESS`, E3 `NOT_AUTHORIZED / NOT_STARTED`; el diseño local está `APPROVED` por el PO, sin autorización de implementación. Sin código, migraciones, fixture ejecutable, publicación de políticas, cierre de gates, AWS ni merge.

## 10. Aprobación expresa del PO — 2026-11-08

El Product Owner aprueba expresamente el diseño local de I1-H03 E3 sobre **`67bc1eb182eaf294173356b825d3ad567777acdd`** en este chat. Esta decisión sustituye el estado histórico `PROPOSED / PENDING_REVIEW` de la propuesta; se conservan el contenido técnico, los textos y la evidencia de las revisiones anteriores.

| Elemento aprobado | Alcance y procedencia |
|---|---|
| D3-01 a D3-07 | Esquema, repositorios, relectura y contratos internos, límites transaccionales, claves sintéticas, cifrado/rotación, retención y pruebas futuras del diseño local aprobado. Incluye el snapshot de cálculo ligado a epoch/conjunto y el recorrido fiscal origen→destino concretados en el SHA aprobado. |
| Aviso | `local-synthetic-company-onboarding`, versión `2026-11-02.1`, finalidad `COMPANY_ONBOARDING`, scope `LOCAL_TEST`, estado `TEST_ONLY`; textos ES/EN exactos de sección 6, exclusivamente sintéticos. |
| Huella exacta aprobada | SHA-256 **`968411cdab71a3de77b0195ab00e64612a59f7a7e053cc3fa98a1014c9c81824`**, sobre los **3008 bytes** canónicos definidos en sección 6.3. No se modifican los bytes cubiertos, el algoritmo ni su referencia. |
| Evidencia histórica | Las validaciones documentales anteriores y la evidencia autoritativa E2 se conservan. La aprobación del PO no acredita ejecución de las pruebas futuras ni aprobación jurídica para tratar datos reales. |
| Autorización de esta actualización | Solo registrar aprobación en diseño, plan H03, AGENTS.md y cuerpo de la misma Draft PR #26, validar enlaces/diff, commit y push en `docs/i1-h03-e3-persistence-design`. |

H03 sigue `IN_PROGRESS`, E1/E2 `ACCEPTED`, implementación E3 y posteriores `NOT_AUTHORIZED / NOT_STARTED`. La aprobación del **diseño** no es aceptación funcional de E3 ni autorización para iniciarla, materializar la fixture o publicar una política. B0/P0/E0 y entrada S0 de H03 pendientes; S0 cerrado solo para H02, CL0 `PENDING`, concreción ADR-003/007 pendiente antes de E5. Sin código, migraciones, nuevas suites funcionales, cierre de gates, AWS ni merge; PR #26 permanece Draft.

Los checks CI, Trivy y Build OCI del nuevo SHA documental los verificará e informará el PO, sin espera ni supervisión del agente. No se afirma en esta actualización un resultado remoto sobre el SHA aprobado o el nuevo SHA. La validación local se limita a enlaces/anclas, conservación del contenido aprobado y de los bytes/hash del aviso, revisión del diff y `git diff --check`.

Validación del registro de aprobación: **57 destinos/anclas locales en los tres documentos modificados, cero errores**; decisiones D3-01 a D3-07, secciones técnicas, retención y matriz de pruebas comparadas sin cambios con el SHA aprobado. Textos ES/EN y algoritmo de reproducción idénticos; SHA-256 y longitud de 3008 bytes comprobados de nuevo. `git diff --check` correcto; sin nuevas suites funcionales.

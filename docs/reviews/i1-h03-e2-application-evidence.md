# Evidencia local — I1-H03 E2: puertos y casos de uso con dobles

- Fecha: 2026-11-07.
- Estado: E2 **`ACCEPTED`** por aprobación expresa del PO el 2026-11-07 sobre `7bd2fed1d06e9a45a041beeb59bb710018247a06`. H03 sigue `IN_PROGRESS`; E1 sigue `ACCEPTED`; E3 y posteriores `NOT_AUTHORIZED / NOT_STARTED`.
- Autorización histórica de implementación: Product Owner en este chat, exclusivamente E2 conforme al [plan H03 aprobado](../plans/i1-h03-execution-plan.md#e2--puertos-y-casos-de-uso-con-dobles).
- Integraciones confirmadas por el PO: E1 mediante `e5c8461c124b284762b782f7a51c590e78b5fb6d` (PR #23); limpieza Jackson mediante `7854d2f4bbb2d5d8fc1c21d3408431505c00b510` (PR #24). La evidencia histórica E1 y sus aprobaciones se conservan [sin modificar](i1-h03-e1-domain-evidence.md).
- Rama: `feature/i1-h03-e2-company-onboarding-application`, creada desde el incremento actualizado, base `7854d2f4bbb2d5d8fc1c21d3408431505c00b510`. Workspace limpio al inicio; los imports H02 del PO ya están integrados en esta base.
- SHA funcional aceptado: `7bd2fed1d06e9a45a041beeb59bb710018247a06`. La actualización documental de aceptación se entrega en un commit posterior de la misma rama/PR #25; no altera el SHA funcional aprobado ni acredita checks nuevos.

## Integración confirmada y continuidad documental — 2026-11-08

El Product Owner confirma expresamente la integración de E2 mediante `9409b77da81a715a4256c195f2c02bc0e04a4b9f` ([PR #25](https://github.com/omarjtoscano/propractix-v2/pull/25)). Esta integración sustituye la situación histórica de PR Draft/sin merge descrita en la aceptación del 2026-11-07. No cambia el SHA funcional aceptado, las pruebas/resultados originales ni la evidencia autoritativa del aviso. No se atribuyen checks del SHA integrado o de la propuesta nueva al agente.

E1/E2 siguen `ACCEPTED`, H03 `IN_PROGRESS`, implementación E3 y posteriores `NOT_AUTHORIZED / NOT_STARTED`. El PO autoriza solo preparar la [propuesta de diseño E3](../plans/i1-h03-e3-persistence-design.md), pendiente de revisión, con commit/push y nueva Draft PR hacia el incremento. Los textos/hash locales propuestos no sustituyen los hashes sintéticos suministrados por los dobles E2 ni se instalan como fixture. Sin código, migraciones, nuevas suites funcionales, publicación de políticas, cierre de gates, AWS ni merge. CI, Trivy y Build OCI del nuevo SHA quedan a cargo del PO, sin espera ni supervisión del agente. Los apartados que siguen conservan la evidencia y restricciones históricas de E2 en su fecha.

## Implementación y orden de efectos

`SubmitCompanyOnboarding` recibe un comando tipado con perfil E1, nombre/correo canónico del administrador, referencia no confiable al aviso y correlación. No recibe tenant, rol, cuenta ni contraseña. Solo un commit confirmado devuelve `Received` con un ID opaco de operación, distinto del ID de onboarding. Un rollback confirmado devuelve `TemporarilyUnavailable`; un commit incierto devuelve `CommitUncertain`, cuyo ID identifica el intento y no acredita recepción durable ni habilita reintento sin reconciliación. Los rechazos y la indisponibilidad son resultados tipados; no hay contrato HTTP ni textos de presentación.

Secuencia comprobada:

1. Leer reloj y snapshot tipado de políticas. La jurisdicción de privacidad se obtiene explícitamente del contexto del servidor, independientemente del locale; no hay fallback jurídico.
2. Consultar `compliance.application.port.in.ResolveApplicablePrivacyNotice` por finalidad empresarial, jurisdicción e instante del servidor. Comprobar estado/scope, vigencia `[validFrom, validUntil)` y coincidencia exacta de ID/versión. Solo `APPROVED / OPERATIONAL` es admisible en scope operativo; `TEST_ONLY / LOCAL_TEST` se admite exclusivamente en ejecución `LOCAL_TEST` explícita. No hay wiring ejecutable de estos scopes en E2.
3. Aplicar admisión exacta del dominio y validar el catálogo E1. Solo después se derivan cifrado/fingerprints fiscales a través del puerto. El índice global restringido devuelve únicamente coincidencia booleana; no revela IDs ni datos de otro tenant. Se consultan todas las versiones suministradas por protección fiscal.
4. Construir el onboarding `IDENTITY_PENDING`, capturando perfil, locale, política y expiración suministrados. Consultar ruta técnica mediante doble. `NO_MAIL_ROUTE` e `INDETERMINATE` conservan la solicitud pendiente y añaden intención diferida de recheck si cabe antes de expirar. Una indisponibilidad técnica tipada del resolver se convierte en `INDETERMINATE`.
5. Dentro del puerto de unidad de trabajo local, insertar onboarding/evidencia autoritativa, eventos y auditoría. El doble prueba rollback conjunto ante fallo de repositorio, outbox o auditoría. La evidencia conserva el objeto resuelto por el servidor, incluida la huella SHA-256 obligatoria de su contenido, y su instante de registro; no copia la referencia del cliente como fuente de verdad.

El submit no crea empresa, cuenta ni membership. Si hay coincidencia fiscal, conserva `COMPANY_REVIEW_REQUIRED` sin reclamar el tenant ni solicitar provisioning automático. En otro caso deja una petición de provisioning para consumo posterior al commit. El submit no depende de `identity` ni de `CompanyRepository`; ArchUnit lo verifica. Dos solicitudes con el mismo correo quedan separadas y pendientes, también bajo concurrencia lógica con dobles; ninguna coincidencia autoriza un vínculo.

`PrepareAdministratorAccount` es únicamente un contrato público en el paquete de ADR-001: comando transitorio con PII mínima, redactado en `toString`, y resultado de IDs/código. No existe implementación de producción ni handler que lo invoque. Los dobles de sus resultados comprueban forma/coherencia, no implementan provisioning ni acreditan recuperación. La rama propuesta `PENDING_ACCOUNT_RECOVERY_REQUIRED` conserva la concreción ADR-003/007 pendiente antes de E5; no se ejecuta ninguna ceremonia H04/H05.

## Puertos, tenancy y mensajes

| Capacidad | Contrato / alcance |
|---|---|
| Aviso | Contrato público de lectura de `compliance`; sin escritura en el proveedor. |
| Políticas | Snapshot tipado e inmutable, sin loader ni política publicada. |
| Tiempo e IDs | `Instant` y UUID v4 suministrados por puertos; submit no asigna tenant. |
| Protección fiscal | Ciphertext con versión de cifrado y lista inmutable de fingerprints por versión HMAC; sin implementación criptográfica ni claves reales. |
| Índice anti-duplicado | Consulta global excepcional de `organization`, país/tipo/fingerprints, solo booleano. No es lectura de empresa ajena. |
| Empresas | Lectura por tenant y escritura mediante `ScopedCompany`, cuyo constructor rechaza scope/empresa distintos antes de alcanzar el adapter. |
| Onboarding | Acceso interno por proceso antes de tener tenant; lookup tenant-aware no expone registros pre-tenant ni de otro tenant. No hay consulta pública. |
| Unidad de trabajo | Onboarding, outbox y auditoría locales. `COMMITTED` confirma recepción durable y produce `Received`; `ROLLED_BACK` confirma cero escrituras y produce `TemporarilyUnavailable`; `UNKNOWN` produce `CommitUncertain`, sin confirmar recepción ni rollback, incluso si no se conserva ninguna escritura. Exige reconciliación antes de cualquier reintento; sin reejecución automática. |
| Ruta | Resultado tipado; doble determinista, sin DNS ni inspección del hosting. |

`CompanyOnboardingEventV1` contiene exclusivamente `eventId`, tipo, `onboardingId`, versión de aggregate, instantes de ocurrencia/disponibilidad y `correlationId`. Versión de evento `1`, productor `organization`, aggregate `CompanyOnboarding`. El tenant se omite porque aún no existe. Tipos: `COMPANY_ONBOARDING_SUBMITTED`, `ADMINISTRATOR_PROVISIONING_REQUESTED`, `EMAIL_DOMAIN_ROUTING_RECHECK_REQUESTED`. Sin nombres, correo/dominio, fiscales, fingerprints, ciphertext ni credenciales. La auditoría preautenticada conserva recurso/correlación/instante y códigos fijos de actor, acción y resultado, sin PII. Las pruebas verifican una lista exacta de campos permitidos y ausencia de valores sensibles sintéticos.

## Cobertura y validación

| Escenario | Evidencia |
|---|---|
| Aviso ausente, expirado, futuro, sustituido, manipulado, suspendido, otra finalidad/jurisdicción o scope incorrecto | Cero onboarding, outbox y auditoría; ningún cálculo fiscal ni consulta de ruta. ID/versión antiguos no se aceptan ni migran automáticamente. |
| Aviso correcto | Se conserva la instancia autoritativa con su huella de contenido, no la referencia suministrada por el cliente; instante fijo del servidor. Referencia textual local `local-synthetic-company-onboarding` / `2026-11-02.1`, sin fixture materializada. |
| Política ausente / dominio bloqueado / país deshabilitado | Fail-closed y cero efectos; privacidad precede protección fiscal. |
| Ruta capaz / sin ruta / indeterminada / resolver indisponible | Alta pendiente, sin activación; intención de recheck para resultados no concluyentes, con demora suministrada. |
| Cuenta/correo coincidente | Submit sin dependencia ni llamada a `identity`, IDs de intención distintos y ningún vínculo/tenant. Los resultados propuestos del contrato se prueban solo mediante dobles. Resolución de cuentas y deduplicación futura: E5. |
| Empresa fiscal coincidente | Misma forma pública de recepción, retención para revisión sin empresa/membership nueva ni revelación del tenant coincidente. |
| Fallos y concurrencia lógica | Fallo previo sin escrituras, rollback conjunto y reintento seguro cuando se garantiza rollback; commit desconocido con escrituras conservadas o totalmente perdidas, sin afirmar recepción ni rollback y sin repetir; error de programación propagado tras rollback del doble; dos candidatos simultáneos sin vínculo. |
| Tenant A/B | Lectura ajena vacía, escritura cruzada rechazada por el scope tipado; onboarding pre-tenant no visible por tenant y asociado visible solo a su tenant. |
| Perfil / locale | Conservación de nombre comercial, país, fiscal, timezone, preferencia y correo case/puntos/`+`; locale soportado y fallback E1 sin selección de jurisdicción por idioma. |
| Arquitectura | Matriz cross-context ADR-001, ausencia de ciclos, aplicación sin frameworks/I/O/ICU/platform, submit sin identity/repositorio empresarial. |

### Validación inicial — `fbfffce4e2111439a7039129bab0d962b530b4e3`

Estos resultados iniciales se conservan como histórico; los resultados de las correcciones se registran debajo.

| Comando | Resultado local inicial |
|---|---|
| `npm run infra:up` | Correcto; PostgreSQL 16 y Mailpit saludables, infraestructura local existente. |
| Compilación `javac --release 21` sin classpath de dependencias | Correcta: 34 archivos de dominio/aplicación/contratos de organization, compliance e identity. |
| `jdeps -s /tmp/h03-e2-java-only` | Solo `java.base`. |
| `npm run server:test` (estado final) | **462 pruebas, 0 fallos, 0 errores, 1 benchmark BCrypt omitido; BUILD SUCCESS**. |
| `npm run server:verify` (estado final) | **462 pruebas, 0 fallos, 0 errores, 1 benchmark BCrypt omitido; BUILD SUCCESS**. JAR empaquetado/reempaquetado; Failsafe sin suites adicionales (las integraciones existentes se ejecutan en Surefire). |
| ArchUnit | Correcto: 15 reglas arquitectónicas, 2 pruebas de matriz/transacciones y 1 detector negativo. Ejecutadas en la suite completa; también hubo una ejecución focalizada de ArchitectureRulesTest junto al submit. |
| Casos nuevos E2 | 38 de submit + 3 de scope tenant + 6 de contrato identity = **47**, todos correctos; se añaden 2 reglas ArchUnit. Los 94 casos de dominio E1 siguen correctos. |
| Enlaces locales Markdown | 66 destinos comprobados, ninguno ausente. |
| `git diff --check` / `git diff --cached --check` | Correctos; diff y status revisados antes del commit. |

Entorno: JDK 21.0.12.1. La primera ejecución completa de `server:test` dentro del sandbox falló por acceso Docker/Testcontainers e instrumentación Mockito (430 pruebas observadas, 3 fallos, 86 errores y 1 omitida). No se modificó H02 para sortearlo. Al ejecutar con acceso local habilitado, la suite pasó con 461 casos; tras añadir el caso de concurrencia lógica se repitió el estado final con 462. Los errores de esa primera ejecución se distinguen de los resultados finales.

Logs locales: `/tmp/h03-e2-focused.log`, `/tmp/h03-e2-server-test.log` (intento aislado), `/tmp/h03-e2-server-test-unrestricted.log` (461 casos), `/tmp/h03-e2-server-test-final.log` y `/tmp/h03-e2-server-verify-final.log`. Los logs de `/tmp` son auxiliares del workspace y no se versionan.

## Corrección de hallazgos de PR #25 — 2026-11-07

Corrección histórica autorizada expresamente por el PO en este chat dentro de la misma Draft PR y rama, sin ampliar E2 ni aceptar entonces la entrega. Los dos hallazgos quedaron corregidos en el SHA funcional ahora aceptado:

- El switch del caso de uso distingue confirmación de commit, confirmación de rollback e incertidumbre. `UNKNOWN` devuelve exclusivamente `CommitUncertain`; no se transforma en éxito ni en indisponibilidad reintentable. Su ID es diagnóstico del intento y no prueba que exista un registro durable. El doble reproduce tanto escrituras conservadas como pérdida total; en ambos casos se comprueba el resultado incierto y una sola ejecución del boundary. Se añade regresión separada del rollback confirmado.
- `ResolveApplicablePrivacyNotice.Notice` exige `ContentHash`, un SHA-256 canónico, y la evidencia conserva esa huella dentro de la resolución autoritativa. El doble de compliance devuelve una huella diferente para la misma referencia; la prueba verifica que la evidencia contiene exactamente esa huella e instancia del servidor, sin derivarla del ID/versión ni recibirla en el comando. Se rechazan huellas ausentes o malformadas antes de poder formar una resolución válida.
- `Reference.noticeId` pasa de UUID a identificador textual; las pruebas usan el ID local previsto y versión candidata de las [secciones 3.1 y 5 del plan](../plans/i1-h03-execution-plan.md#5-propuesta-p0-minimización-aviso-y-retención): `local-synthetic-company-onboarding` / `2026-11-02.1`. No se crea alias UUID, archivo de política, texto ES/EN, hash aprobado ni fixture ejecutable. Los cambios de ID/versión siguen provocando rechazo.

| Validación de la corrección | Resultado |
|---|---|
| `npm run infra:up` | PostgreSQL y Mailpit saludables. |
| `npm run server:test` | **466 pruebas, 0 fallos, 0 errores, 1 benchmark BCrypt omitido; BUILD SUCCESS**. |
| `npm run server:verify` | **466 pruebas, 0 fallos, 0 errores, 1 benchmark BCrypt omitido; BUILD SUCCESS**. JAR empaquetado/reempaquetado; Failsafe sin suites adicionales. |
| Regresiones / ArchUnit | Submit: 42 casos; 4 más que en el SHA inicial. Contratos/aplicación E2: 51 casos en total. ArchUnit: 15 reglas + 2 pruebas de matriz/transacciones + 1 detector negativo, correctos. |
| Compilación Java 21 sin classpath / `jdeps` | 34 archivos de dominio/aplicación/contratos; solo `java.base`. |
| Enlaces locales / diff | 3 destinos del informe comprobados; `git diff --check` correcto. |

Logs de las ejecuciones locales anteriores de corrección: `/tmp/h03-e2-fixes-focused.log`, `/tmp/h03-e2-fixes-server-test.log`, `/tmp/h03-e2-fixes-server-verify.log`. CI, Trivy y Build OCI del SHA funcional fueron confirmados en verde por el PO en la aceptación siguiente; los checks del nuevo SHA documental los verificará e informará el PO, sin espera ni supervisión del agente.

## Aceptación expresa del PO — 2026-11-07

E2 `ACCEPTED` por aprobación expresa del Product Owner en este chat sobre `7bd2fed1d06e9a45a041beeb59bb710018247a06`. La aceptación se limita a E2 con dobles; H03 sigue `IN_PROGRESS`, E1 sigue `ACCEPTED` y E3/posteriores siguen `NOT_AUTHORIZED / NOT_STARTED`. PR #25 permanece Draft y sin merge.

| Evidencia / decisión | Procedencia y alcance |
|---|---|
| Pruebas locales reportadas | Ejecuciones anteriores de `server:test` y `server:verify`: 466 pruebas cada una, 0 fallos, 0 errores, 1 benchmark omitido; 51 casos E2, ArchUnit y empaquetado correctos según el informe anterior. No se repiten suites en esta actualización ni se atribuyen esos resultados al nuevo SHA documental. |
| Revisión técnica | Revisión asistida por Codex en este chat (2026-11-07), incluida revisión del diff y corrección de los dos hallazgos sobre commit incierto y huella autoritativa del aviso. Es distinta de la decisión de aceptación del PO y de la comprobación de checks remotos. |
| CI, Trivy y Build OCI | Verdes sobre `7bd2fed1d06e9a45a041beeb59bb710018247a06` según confirmación expresa del PO el 2026-11-07. No es una verificación realizada por el agente. |
| Aceptación de producto | Decisión expresa del Product Owner sobre ese SHA funcional. Las pruebas/revisión técnica no constituyen por sí mismas esta aprobación. |
| Nuevo SHA documental | Commit/push exclusivamente documentales y cuerpo de la misma Draft PR #25. Checks pendientes de verificación y comunicación por el PO, sin espera ni supervisión del agente. |

Validación de este cambio documental: coherencia de estados/procedencia, 69 destinos de enlaces locales comprobados en los cuatro archivos y `git diff --check` correcto. Alcance revisado: exclusivamente AGENTS.md, ambos planes y este informe; sin código, nuevas suites funcionales ni cambios de resultados históricos.

## Límites conservados

Solo datos sintéticos en memoria. Los dobles de aviso son metadatos construidos dentro de las pruebas, sin textos jurídicos, huellas aprobadas ni fixture publicada. Las huellas de pruebas (`a` o `b` repetidas 64 veces) son metadatos sintéticos suministrados por el doble de compliance, no hashes de una fixture local aprobada. No se modifica ninguna política existente. Los valores `APPROVED` usados para probar el contrato son datos sintéticos, no una aprobación jurídica.

El agregado/perfil que recibe el repositorio es **entrada transitoria** del adapter; el contrato exige proteger PII al persistir y usar la representación fiscal protegida. Las pruebas E2 no acreditan cifrado real, separación de claves, unicidad fiscal concurrente ni predicados SQL: E3 deberá demostrarlo con PostgreSQL. No hay implementación del boundary transaccional, outbox/inbox/relay, backoff ejecutable, reconciliación durable, deduplicación de provisioning ni idempotencia durable. Tampoco se acredita todavía igualdad temporal de respuestas ante coincidencias: el adapter público y su coordinación asíncrona se validarán en E8. `CommitUncertain` expresa la incertidumbre del intento, sin constancia durable ni permiso de reintento; la reconciliación e idempotencia durables no se implementan ni se acreditan en E2.

E3 y posteriores `NOT_AUTHORIZED / NOT_STARTED`. Sin SQL/JPA, migraciones, normalizador ICU4J, DNS real, HTTP/OpenAPI, frontend, provisioning, credenciales, envío de correo, AWS ni merge; E2 aceptada exclusivamente sobre el SHA funcional indicado. B0/P0/E0 y entrada S0 propia de H03 siguen pendientes; S0 continúa cerrado solo para H02 y `CL0_CLOUD_STAGING` sigue `PENDING`. CI, Trivy y Build OCI del SHA funcional `7bd2fed1d06e9a45a041beeb59bb710018247a06` están verdes según confirmación del PO; los checks del nuevo SHA documental quedan a cargo del PO, sin espera ni supervisión del agente.

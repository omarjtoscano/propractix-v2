# Evidencia local — I1-H03 E1: dominio empresarial e invariantes

- Fecha de validación: 2026-11-07 (Europe/Madrid).
- Estado: H03 y E1 `IN_PROGRESS`, sin aceptación; E2 y posteriores `NOT_AUTHORIZED / NOT_STARTED`.
- Autorización: PO, 2026-11-02, después de confirmar la integración de [PR #22](https://github.com/omarjtoscano/propractix-v2/pull/22) mediante `2e797ebfd5c22481ec0a21f4c0ca8d9933fffbbd`.
- Rama: `feature/i1-h03-e1-company-domain`, creada desde `increment/01-company-identity-catalog` actualizado en ese SHA.
- Fuentes: [especificación](../ProPractix-V2-MVP-Espana-Especificacion-Arquitectura-y-Tecnica-para-Codex.md), [blueprint](../ProPractix-V2-Blueprint-Dominio-y-Mapas-Legales-Espana.md), ADR-001/002/003/004/005/006/007/008/012 y [plan H03](../plans/i1-h03-execution-plan.md). No se cambian sus decisiones de producto ni se aprueban políticas nuevas.

## Alcance implementado

`organization.domain` contiene `Company`, `CompanyOnboarding`, `TenantId` como único identificador empresarial, `OnboardingId`, perfil y value objects para nombre, país, fiscal, locale, zona, correo/dominio canónicos y referencia de política. UUID v4 externos al aggregate; sin generador, reloj real, persistencia ni configuración de entorno.

`Company` es inmutable e inicia exclusivamente `SELF_DECLARED`; no existe método de aprobación empresarial. `CompanyOnboarding` guarda el candidato admitido, dirección canónica, versión de política evaluada, locale inicial, instantes explícitos y versión `long` que aumenta tras cada cambio válido. El rol inicial fijo `COMPANY_OWNER` representa una intención; no crea ni activa una membership.

| Origen | Operación E1 | Resultado / guard |
|---|---|---|
| Nuevo | `submit` | `SUBMITTED`, sin tenant vinculado ni resultado técnico inferido; política y catálogo obligatorios. |
| `SUBMITTED` | `awaitIdentity` | `IDENTITY_PENDING`, antes de expirar. |
| `IDENTITY_PENDING` | `awaitEmailVerification` | `EMAIL_PENDING`, empresa con perfil y locale que coinciden con el candidato; guarda su `TenantId`. No ejecuta provisioning. |
| Cualquier pendiente | `recordRoutingResult` | Conserva el estado; resultado técnico ortogonal, sustituible por una comprobación posterior. |
| Cualquier pendiente | `fail` | `FAILED`, antes de expirar; sin recuperación automática. |
| Cualquier pendiente | `expire` | `EXPIRED` cuando `at >= expiresAt`; también cubre procesos detenidos antes de `EMAIL_PENDING`. |
| `FAILED` / `EXPIRED` | Mutación o continuación | Rechazo, sin cambio parcial ni avance de versión. |

`READY` queda reservado en la enum para la ceremonia verificada futura de H04. E1 no ofrece una transición a `READY`, reenvío desde `EXPIRED`, activación de cuenta, credencial o vínculo de una cuenta existente. Los métodos representan invariantes de estado en memoria; no acreditan las ceremonias de H04/H05 ni la coordinación durable de entregas posteriores.

`CompanyRegistrationPolicy` es una instantánea inmutable suministrada: valida país habilitado y referencias de sus catálogos; resuelve preferencia soportada → correspondencia país/locale → fallback configurado. Cambiar el catálogo permite otros países/locales sin modificar el aggregate. Conserva país y preferencia declarados; la resolución de presentación no devuelve jurisdicción ni `LegalPolicyPack`. La zona es explícita y no se infiere del locale o país. `TaxIdentifier` exige tipo estable y valor normalizado no vacío, sin validadores españoles, pruebas de existencia ni algoritmo fiscal: la estrategia específica futura sigue fuera de `Company`.

`CompanyEmailAdmissionPolicy` recibe referencia ID/versión y conjunto inmutable no vacío de dominios canónicos. Comparación exacta con `EmailAddress.domain()`, sin listas de proveedores en producción, reglas de substring/subdominio/sufijo educativo, comodines o inspección de hosting/MX. Una política ausente, inválida o vacía no se convierte en permiso general. Una nueva instancia cambia la admisión de nuevas solicitudes; el onboarding anterior conserva la versión que lo admitió. Carga, publicación, vigencia y selección de versión pertenecen a entregas posteriores.

`MAIL_CAPABLE`, `NO_MAIL_ROUTE` e `INDETERMINATE` son evidencia técnica separada. Todos permiten conservar la progresión pendiente; ninguno verifica la empresa ni el control del correo, rechaza el alta o activa cuentas. E1 almacena el resultado explícito; scheduling, backoff, DNS, agotamiento y recheck durable siguen fuera del alcance.

## Frontera ASCII / IDNA

E1 recibe valores ASCII canónicos y comprueba sintaxis dot-atom/LDH, minúsculas del dominio y límites combinados: local-part 1–64 octetos, etiquetas 1–63, dominio 1–253 y dirección 1–254 (con local/dominio obligatorios). Conserva case, puntos y `+` del local-part. Rechaza entradas no canónicas, controles, espacios, local Unicode, dominios Unicode, etiquetas vacías/punto final, literales IP, comillas y comentarios; no los repara ni transforma.

**No se ejecutó el motor IDNA.** No hay trimming, mapping UTS #46, NFC, ToASCII, decodificación Punycode o round-trip, ni dependencia/importación ICU o uso alternativo de `java.net.IDN`. Las A-labels suministradas se comprueban estructuralmente como ASCII; validar su contenido decodificado exige la frontera técnica futura. Por ejemplo, el rechazo por Punycode inválido de `xn--0`, por U+0080 de `xn--a`, por falta de NFC de `xn--u-ccb` y la convergencia compuesto/descompuesto de `bücher` pertenecen a los tests futuros del adapter con ICU4J 78.3 y el perfil de [ADR-001](../adr/ADR-001-monolito-modular.md#normalización-técnica-de-correo-e-idna).

Las pruebas E1 de `xn--bcher-kva.test`, `xn--fa-hia.test` y `fass.test` verifican conservación/comparación de valores ASCII suministrados. No son un normalizador simulado ni evidencia de conformidad Unicode/ICU. No se modificó `pom.xml`.

## Pruebas y validación

Fixtures `LOCAL_TEST / TEST_ONLY` exclusivamente en memoria, con nombres ficticios, fiscal `SYNTHETIC-*`, correos `.test` y catálogos sintéticos. No se materializan avisos, textos/hash legales ni políticas publicadas; no se reutiliza la fixture de staging. Las pruebas no escriben estos datos de E1 a BD, Mailpit o archivos ni consultan DNS público.

| Suite nueva JUnit, sin Spring ni BD | Casos | Cobertura |
|---|---:|---|
| `CanonicalEmailTest` | 42 | Valores canónicos, sensibilidad local, ASCII/A-label, entradas inválidas, límites local/etiqueta/dominio/dirección y combinación de límites. |
| `CompanyValueObjectsTest` | 17 | UUID v4/variante, nulos, país, locale canónico, zona nombrada, fiscal opaco y diagnósticos redactados. |
| `CompanyTest` | 7 | `SELF_DECLARED`, identidad tenant, datos opcionales, precedencia de locale, catálogos alternativos/inmutables y referencias inválidas, sin fallback jurídico. |
| `CompanyEmailAdmissionPolicyTest` | 12 | Coincidencia exacta, sustitución de versión, copias defensivas, ausencia de reglas de hosting/sufijos/subdominios y política inválida/ausente/vacía. |
| `CompanyOnboardingTest` | 15 | Transiciones, rol fijo, sustitución de política/empresa, tres resultados técnicos, fallo terminal, tiempo monotónico, duración suministrada, límite exacto de expiración y ausencia de cambios parciales. |

Total nuevo: **93 casos**, todos correctos. El escenario local aprobado usa `P7D` desde `2026-11-02T10:00:00Z`; otro escenario suministra `PT0.000000001S` para demostrar que el aggregate no fija siete días. Se admite una operación en `expiresAt - 1 ns`, se bloquea en `expiresAt` y la expiración explícita se permite exactamente en ese instante. `Clock.fixed`/`Clock.offset` se usan en tests; el dominio solo recibe `Instant` y `Duration`.

Negativo A/B de consistencia: un onboarding no admite otra empresa con perfil distinto ni puede reasignar el tenant después de llegar a `EMAIL_PENDING`; el rechazo conserva la referencia original. **No acredita autorización cross-tenant en aplicación/HTTP/SQL**: no hay aún recursos accesibles, repositorios ni actor autenticado en E1. Las pruebas de acceso negativas reales siguen asignadas a E2/E3 y posteriores en el plan, sin introducir esas capas en esta entrega.

Las reglas ArchUnit existentes comprueban frameworks, límites/ciclos y contratos entre contextos. Se añaden dos reglas sobre `organization.domain`: solo Java y su propio dominio; sin red/IO, normalizador técnico, ICU, `platform`, aplicación, adapters ni configuración.

| Comando ejecutado localmente | Resultado |
|---|---|
| `npm run infra:up` | Correcto; PostgreSQL y Mailpit saludables en el Compose local. Acceso Docker concedido fuera del sandbox. |
| `./server/mvnw -q -f server/pom.xml -Dtest='com.propractix.organization.domain.**,com.propractix.architecture.*' test` | Correcto tras corregir una cuenta de longitud en el test y unificar el rechazo diagnóstico de locale inválido; 93 casos nuevos y suites arquitectónicas. |
| `javac --release 21 -d /tmp/h03-e1-java-only $(rg --files server/src/main/java/com/propractix/organization/domain -g '*.java')` | Correcto, sin classpath de Spring, ICU u otras dependencias. |
| `jdeps -s /tmp/h03-e1-java-only` | Única dependencia: `java.base`. |
| `npm run server:test` | Correcto: **412 pruebas, 0 fallos, 0 errores, 1 benchmark BCrypt omitido** (`BUILD SUCCESS`). |
| `npm run server:verify` | Correcto: **412 pruebas, 0 fallos, 0 errores, 1 benchmark BCrypt omitido**, empaquetado JAR y `BUILD SUCCESS`. Failsafe no tiene suites adicionales; las pruebas de integración existentes corren en Surefire. |
| `./scripts/check-migrations.sh` | Correcto, sin nuevas migraciones. |
| Enlaces/anclas locales de documentos modificados | Correcto: 66 enlaces/anclas locales comprobados, 0 errores. |
| `git diff --check`, revisión de diff y `git status --short` antes del commit | Correcto: diff revisado, sin errores de whitespace; solo dominio E1, tests, arquitectura y documentación. |

JDK disponible: OpenJDK `21.0.12.1`. Logs de ejecución locales en `/tmp/h03-e1-unit.log`, `/tmp/h03-e1-server-test.log` y `/tmp/h03-e1-server-verify.log`; no se incorporan al repositorio ni se presentan logs de entregas anteriores como resultados nuevos. La suite completa ejecuta las pruebas existentes de H02 con su propia infraestructura sintética Testcontainers como regresión, sin cambiar su código o configuración.

## Entrega y límites conservados

Cambios: `AGENTS.md`, plan H03, sincronización del estado en el plan del incremento, esta evidencia, `organization.domain` y sus pruebas, más reglas arquitectónicas. La entrega corresponde al commit que incorpora este documento; su SHA y el enlace de Draft PR se informan al PO y quedan en Git/PR, evitando una autorreferencia imposible del hash dentro del mismo commit.

B0/P0/E0 y entrada S0 de H03 permanecen pendientes. S0 continúa cerrado solo para H02; CL0 `PENDING`. H03 no está expuesta por HTTP ni siquiera localmente. Sin E2+, puertos, casos de uso, adapters, JPA, migraciones, DNS, OpenAPI, frontend, provisioning, credenciales, recuperación ni publicación de políticas. Sin AWS, merge o aceptación de E1. Se conservan las aprobaciones y evidencia histórica de H02 y PR #21/#22. La concreción ADR-003/007 sigue pendiente antes de E5.

CI, Trivy y Build OCI del commit entregado quedan a cargo del PO, quien verificará e informará sus resultados. El agente no espera ni supervisa esas ejecuciones.

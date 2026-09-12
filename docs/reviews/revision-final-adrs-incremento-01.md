# Revisión final de ADR-001 a ADR-008 y plan del Incremento 1

- Fecha de revisión: 2026-09-12
- Rama revisada: `increment/01-company-identity-catalog`
- Alcance: revisión documental; no autoriza implementación
- Estados observados: ADR-001 a ADR-008 continúan en `Propuesto — revisión 3`
- Resultado: requiere correcciones documentales antes de aprobar el plan e iniciar I1-H01

## 1. Fuentes y criterio de revisión

Se revisaron `AGENTS.md`, la especificación técnica, el blueprint legal, las dos
revisiones y sus resoluciones, ADR-001 a ADR-008 revisión 3 y el plan del Incremento
1. La revisión comprueba coherencia entre fuentes, seguridad de secretos y PII,
dirección y propiedad de contratos, decisiones D-005/D-006, tenancy, i18n, propiedad
de tablas, separación entre aceptación documental y conformidad futura, y gates de
I1-H01 a I1-H07.

No se ha modificado ningún ADR ni se ha cambiado su estado. Este informe tampoco
acepta el plan ni autoriza una historia.

## 2. Conclusión ejecutiva

La revisión 3 resuelve sustancialmente SR-B01 a SR-B06 y alinea las decisiones de
producto que antes se contradecían: `student` es propietario del perfil y la
declaración, H03 es passwordless, el rol inicial es `COMPANY_OWNER`, no existe claim
automático de empresa, D-005 separa país/locale/jurisdicción y H08 fue sustituida por
Gate R1.

También es segura la intención del flujo de credencial y correo: la contraseña solo
entra en `identity`; la outbox conserva IDs y metadatos mínimos; el correo y el enlace
se entregan de forma transitoria; y `notifications` no consulta `identity`. Las 18
tablas actualmente previstas tienen owner, scope y escritor. Todos los ADR separan
condiciones documentales de aceptación y conformidad de implementación.

Persisten cuatro bloqueos documentales. Dos afectan seguridad o cumplimiento: la
rotación de la clave del fingerprint fiscal no conserva hoy la garantía de unicidad,
y el `privacyNoticeVersion` enviado por el cliente no se vincula explícitamente al
aviso vigente resuelto por el servidor. Los otros dos impiden demostrar la arquitectura
y el readiness: falta declarar el owner y paquete exactos del puerto de entrega, y el
plan no contiene el gate de baseline frontend prometido ni cubre de manera explícita
rate limiting y ADR aplicables para todos los endpoints públicos.

Por ello se recomienda la aceptación individual de ADR-002, ADR-004 y ADR-007, siempre
mediante autorización expresa del propietario. ADR-001, ADR-003, ADR-005, ADR-006 y
ADR-008 deben corregirse antes de recomendar su cambio de estado. I1-H01 no está
autorizada mientras sus ADR aplicables sigan propuestos o no se cierre su matriz de
entrada.

## 3. Verificación de SR-B01 a SR-B06

| ID | Resultado final | Evidencia |
|---|---|---|
| SR-B01 | Resuelto | H03 excluye contraseña (`docs/plans/increment-01-plan.md:201-204`); la contraseña llega directamente a `identity` en H04 (`docs/plans/increment-01-plan.md:231-240`); ADR-003 y ADR-007 prohíben que cruce onboarding, outbox o eventos (`docs/adr/ADR-003-multitenancy-tenant-context.md:21-27`; `docs/adr/ADR-007-autenticacion-sesiones-csrf.md:24-34`). |
| SR-B02 | Resuelto | D-006 está `ACCEPTED` y asigna perfil/onboarding/declaración a `Student` con captura en I1 y reconfirmación en I3 (`docs/ProPractix-V2-Blueprint-Dominio-y-Mapas-Legales-Espana.md:409-420`); especificación y plan repiten el mismo roadmap (`docs/ProPractix-V2-MVP-Espana-Especificacion-Arquitectura-y-Tecnica-para-Codex.md:959-987`; `docs/plans/increment-01-plan.md:284-339`). |
| SR-B03 | Resuelto en diseño funcional; queda el bloqueo BF-03 de propiedad física | ADR-001 incorpora las consultas `organization/student -> compliance` y una única llamada `identity -> notifications` (`docs/adr/ADR-001-monolito-modular.md:80-95`). ADR-005 elimina token y PII innecesaria de la outbox (`docs/adr/ADR-005-eventos-outbox.md:22-43,70-88`) y ADR-006 descarta tablas de `notifications` en I1 (`docs/adr/ADR-006-dominio-jpa-propiedad-tablas.md:41-47`). |
| SR-B04 | Resuelto en producto; queda el bloqueo técnico BF-01 | D-005 separa país, locale y jurisdicción y D-006 fija `student` (`docs/ProPractix-V2-Blueprint-Dominio-y-Mapas-Legales-Espana.md:396-420`). ADR-003 fija `COMPANY_OWNER`, tenant derivado, revalidación de membership y política no enumeradora de duplicados (`docs/adr/ADR-003-multitenancy-tenant-context.md:43-69`). |
| SR-B05 | Resuelto en estructura; gates todavía incompletos | Las historias ya incluyen propietario, estado, contrato, invariantes, errores/fallos, migración, criterios y pruebas (`docs/plans/increment-01-plan.md:161-339`). BF-02 y BF-04 identifican las dos condiciones de entrada aún no cerradas. |
| SR-B06 | Resuelto | Los ocho ADR tienen secciones separadas `Condiciones documentales de aceptación` y `Conformidad de la implementación`; por ejemplo ADR-001 (`docs/adr/ADR-001-monolito-modular.md:138-150`) y ADR-008 (`docs/adr/ADR-008-openapi-errores-idempotencia.md:90-102`). |

## 4. Bloqueos restantes, ordenados por severidad

### BF-01 — Alta — La rotación del fingerprint fiscal puede permitir un tenant duplicado

ADR-003 decide que país registral, tipo fiscal y fingerprint HMAC impiden crear otro
tenant (`docs/adr/ADR-003-multitenancy-tenant-context.md:43-47`). ADR-006 convierte esa
decisión en una restricción única y almacena versión de clave
(`docs/adr/ADR-006-dominio-jpa-propiedad-tablas.md:47`). Sin embargo, no define qué
ocurre al rotar la clave HMAC. El mismo identificador produce otro fingerprint bajo la
nueva clave; una restricción que solo compara el valor nuevo no colisiona con la fila
antigua. Esto contradice la garantía de H03 de no crear otro tenant
(`docs/plans/increment-01-plan.md:209-219`).

**Corrección exacta:** ADR-003 y ADR-006 deben distinguir clave de cifrado de clave de
búsqueda y elegir una estrategia de rotación verificable. Se recomienda una tabla o
índice auxiliar propietario de `organization` con una fila por empresa y versión HMAC
activa, unicidad por `(registered_country, tax_identifier_type, key_version,
fingerprint)`, backfill de la versión nueva para todas las empresas antes de admitir
altas con ella, cómputo/comprobación de todas las versiones activas y retirada de la
anterior solo al terminar el backfill. El alta concurrente debe insertar los
fingerprints activos atómicamente y convertir cualquier conflicto en el mismo `202`
no enumerador. Añadir pruebas de duplicado durante rotación y de dos altas concurrentes.
La consulta nunca debe exponer una lectura cross-tenant al caso de uso anónimo.

### BF-02 — Alta — La versión del aviso aportada por el cliente no es evidencia autoritativa

H03 y H06 reciben `privacyNoticeVersion` desde el navegador
(`docs/plans/increment-01-plan.md:201-204,292-297`). ADR-008 solo garantiza que exista
algún aviso aplicable `APPROVED` y vigente antes del binding
(`docs/adr/ADR-008-openapi-errores-idempotencia.md:71-73`), mientras ADR-006 ordena
guardar el ID/versión mostrado (`docs/adr/ADR-006-dominio-jpa-propiedad-tablas.md:43`).
No se exige que el valor no confiable coincida con el aviso que el servidor resolvió
para la finalidad, jurisdicción y fecha. Podría quedar una evidencia obsoleta o
fabricada aunque el guard general estuviera abierto.

**Corrección exacta:** ADR-008 debe conservar el guard previo al binding y añadir una
validación de aplicación posterior al binding pero anterior a cualquier escritura:
resolver desde `compliance` el aviso `APPROVED` y vigente por finalidad/jurisdicción,
comparar su ID y versión con la referencia mostrada/enviada, rechazar mismatch con un
código estable no enumerador y no persistir onboarding, idempotencia derivada del body
ni auditoría con PII. ADR-006 debe declarar que la evidencia persistida procede del
resultado autoritativo del servidor, no del request. H03/H06 necesitan criterios y
pruebas para aviso ausente, expirado, sustituido entre render y submit, versión
manipulada y finalidad equivocada.

### BF-03 — Alta — `NotificationDeliveryPort` no tiene owner físico inequívoco

La dirección lógica es correcta: ADR-001 y el plan solo permiten
`identity -> notifications` y prohíben la consulta inversa
(`docs/adr/ADR-001-monolito-modular.md:91-95`;
`docs/plans/increment-01-plan.md:84`). ADR-005 dice que un handler de `identity` llama
a `NotificationDeliveryPort` y que `notifications` lo implementa
(`docs/adr/ADR-005-eventos-outbox.md:70-86`). No declara, sin embargo, el bounded
context propietario del contrato ni su paquete público. Bajo ADR-002, ubicarlo como
outbound port de `identity` obligaría a `notifications` a importar un paquete de
`identity`; ubicarlo como inbound port de `notifications` conserva la flecha. La
ambigüedad impide demostrar la condición documental de contrato propietario y ausencia
de ciclo (`docs/adr/ADR-005-eventos-outbox.md:105-110`).

**Corrección exacta:** registrar el contrato como puerto de entrada propiedad de
`notifications`, por ejemplo
`notifications.application.port.in.DeliverNotification`, consumido por `identity` e
implementado dentro de `notifications`. Su comando mínimo debe declarar destinatario,
`templateKey`, locale, enlace y `correlationId` como valores transitorios, prohibir
persistencia/logging y no devolver PII. Registrar por separado el handler de
`VerificationId` como contrato de entrada propiedad de `identity`; la configuración
lo conecta al relay genérico sin hacer que `platform` dependa de un dominio concreto.
ADR-001 debe ampliar su matriz con columnas `contrato`, `owner/provider`, `consumer` y
`paquete público`; ADR-002/ArchUnit debe verificar la dirección física. No añadir
`notifications -> identity`.

### BF-04 — Alta — La matriz de gates no demuestra el readiness de H01–H07

ADR-001 condiciona su aceptación a un gate frontend explícito antes de H02
(`docs/adr/ADR-001-monolito-modular.md:120,138-143`), pero el plan solo define C0 para
fuente/licencia/datos del catálogo (`docs/plans/increment-01-plan.md:29-36`). Además,
H01 usa decisiones de ADR-001/002, Flyway de ADR-006, benchmark de ADR-007 y pipeline
OpenAPI de ADR-008 (`docs/plans/increment-01-plan.md:135-159`), pero `ADR aplicables`
no está convertido en una matriz verificable. Finalmente ADR-007 exige valores y
pruebas `429/Retry-After` por registro, login, reenvío y verificación antes de exponer
cada endpoint (`docs/adr/ADR-007-autenticacion-sesiones-csrf.md:80-84`); P0 solo bloquea
H03/H06 con datos reales, E0 no nombra límites y H05 no tiene gate de abuso
(`docs/plans/increment-01-plan.md:31-36,223-311`).

**Corrección exacta:** añadir al plan:

1. Una matriz historia→ADR. Para H01 debe listar al menos ADR-001, ADR-002, ADR-006,
   ADR-007 y ADR-008 por los elementos que implementa. ADR-004 se añade si H01 crea
   `IdGenerator`, `Clock` o primitivas concurrentes; en otro caso esos elementos se
   difieren hasta la primera historia que los use.
2. `F0_CLIENT_BASELINE`, owners Arquitectura + Frontend, antes de H02, con versiones
   exactas de React/Vite, generador OpenAPI, router, i18n, testing y política de
   actualización.
3. `S0_PUBLIC_ENDPOINTS`, owners Seguridad + Operaciones, anterior a exponer H03–H06,
   con límites por operación, claves/fingerprints permitidos, TTL, fail-closed,
   métricas/alertas y pruebas `429`/`Retry-After`. H04 requiere E0+S0; H05 requiere
   S0; H03/H06 requieren P0+S0 en entornos con datos reales.
4. Criterios explícitos de `rate_limited` en H04, H05 y H06, no solo una mención en la
   taxonomía global.
5. Ampliar E0 con Privacidad/Legal como owner de la transferencia al proveedor y con
   evidencia de rol contractual, región/subencargados, retención/borrado, redacción de
   errores e incident response antes de enviar correo real.

## 5. Comprobaciones sin bloqueo adicional

- **Contraseña, correo y PII:** la contraseña no cruza contextos ni almacenamiento
  auxiliar; provisioning publica solo IDs; la PII necesaria queda cifrada en el
  process manager propietario; el enlace y destinatario son transitorios
  (`docs/adr/ADR-005-eventos-outbox.md:43,57-88`;
  `docs/adr/ADR-007-autenticacion-sesiones-csrf.md:24-34`). BF-02/BF-03 deben cerrarse
  antes de considerar completa esta garantía.
- **D-005 y D-006:** blueprint, especificación, ADR y plan son coherentes. El fallback
  `es` solo gobierna presentación y nunca el `LegalPolicyPack`; un país no soportado
  no hereda reglas españolas (`docs/ProPractix-V2-Blueprint-Dominio-y-Mapas-Legales-Espana.md:396-420`;
  `docs/adr/ADR-003-multitenancy-tenant-context.md:35-41`).
- **Roles y tenancy:** `COMPANY_OWNER`, cuenta global, múltiples memberships,
  `TenantId == CompanyId`, tenant derivado y revalidación en application/persistencia
  están alineados (`docs/adr/ADR-003-multitenancy-tenant-context.md:49-91`;
  `docs/plans/increment-01-plan.md:250-282`).
- **i18n:** especificación, ADR-003/008 y DoD cubren etiquetas, acciones, estados,
  ayudas, validaciones y errores; los códigos técnicos permanecen estables y los
  textos legales se versionan aparte
  (`docs/ProPractix-V2-MVP-Espana-Especificacion-Arquitectura-y-Tecnica-para-Codex.md:796-808`;
  `docs/adr/ADR-008-openapi-errores-idempotencia.md:12-30`;
  `docs/plans/increment-01-plan.md:376-390`). La UI no debe mostrar directamente
  `title`, `detail` o `violations` del backend; debe resolver `code`/códigos de
  violación mediante i18n.
- **Tablas:** ADR-006 asigna propietario, scope y escritura a las 18 tablas previstas
  y declara que `notifications` no persiste tablas en I1
  (`docs/adr/ADR-006-dominio-jpa-propiedad-tablas.md:18-47`). La estructura auxiliar
  propuesta en BF-01 deberá incorporarse a esa matriz antes de una migración.
- **Roadmap:** I1 captura una declaración académica editable y no verificada; I3 la
  reconfirma al candidatar. Universidad sigue como referencia externa, no tenant ni
  workspace (`docs/ProPractix-V2-MVP-Espana-Especificacion-Arquitectura-y-Tecnica-para-Codex.md:959-987`;
  `docs/ProPractix-V2-Blueprint-Dominio-y-Mapas-Legales-Espana.md:409-420`).
- **Aceptación frente a conformidad:** los ocho ADR ya separan adoptar una decisión de
  comprobar después su implementación. Aceptar un ADR no satisface automáticamente
  sus pruebas de conformidad ni aprueba ML-15, C0, E0, S0 o una historia.

## 6. Riesgos residuales que pueden aceptarse con mitigación

| Riesgo residual | Mitigación/gate exigido |
|---|---|
| Un único esquema y usuario PostgreSQL permite técnicamente acceso entre contextos | Prefijos y owner, puertos, ArchUnit, revisión SQL/migraciones y pruebas negativas; reevaluar schemas/usuarios/RLS si aumenta el riesgo (`docs/adr/ADR-006-dominio-jpa-propiedad-tablas.md:63-74`). |
| Outbox al menos una vez puede enviar más de un correo si el proveedor acepta y se pierde el acuse | Enlace de un solo uso, consumidor idempotente cuando sea posible, copy no alarmista, métricas, dead letter y runbook E0. No presentar “exactly once”. |
| Correo, nombre y enlace existen transitoriamente en memoria y salen hacia el proveedor de correo | E0 debe aprobar proveedor, contrato de tratamiento, región/retención, redacción de errores y observabilidad sin PII; pruebas de caída y recuperación. |
| Access token de 10 minutos puede conservar claims globales obsoletos | Revocar familias refresh y revalidar membership en todas las operaciones tenant-owned; la ventana no autoriza datos empresariales (`docs/adr/ADR-003-multitenancy-tenant-context.md:62-69`). |
| Catálogo, ML-15 y proveedor de correo siguen siendo decisiones externas pendientes | C0/P0/E0 permanecen fail-closed y no se sustituyen por fixtures productivos (`docs/plans/increment-01-plan.md:393-399`). |
| `notifications` no tiene persistencia propia en I1, reduciendo trazabilidad de entrega | Conservar solo metadatos técnicos no sensibles en outbox, métricas agregadas y operación auditada; cualquier persistencia futura requiere ampliar ADR-006. |

## 7. Decisiones que requieren aprobación de producto u otros owners

Las decisiones anteriores sobre `student`, H03 passwordless, `COMPANY_OWNER`, no claim
automático, D-005, D-006 y Gate R1 ya están resueltas; no deben reabrirse sin nueva
evidencia.

| Decisión pendiente | Decisores mínimos | Antes de |
|---|---|---|
| UX y respuesta ante aviso sustituido entre presentación y submit; el servidor debe seguir siendo autoritativo | Producto + Privacidad/Legal + Seguridad | Corregir ADR-008 y aceptar H03/H06 |
| Si se permite trabajo con datos sintéticos antes de P0 y qué significa exactamente “exponer” un endpoint | Producto + Seguridad + Operaciones | Aprobar la matriz de gates |
| Estrategia HMAC y operación de rotación/backfill del identificador fiscal | Seguridad + Datos + Arquitectura | Aceptar ADR-003/006 y diseñar la primera migración empresarial |
| Owner/paquete público de los dos contratos de entrega y wiring del relay | Arquitectura | Aceptar ADR-001/005 |
| Baseline exacta de cliente y política de actualización | Arquitectura + Frontend | Cerrar F0 antes de H02 |
| Valores por operación y política fail-closed de rate limiting | Seguridad + Operaciones | Cerrar S0 antes de exposición pública |
| Tratamiento de destinatario y enlace por el proveedor de correo | Privacidad/Legal + Seguridad + Operaciones | Cerrar E0 antes de H04 con correo real |

## 8. Recomendación individual y correcciones por ADR

Ninguna recomendación cambia estados automáticamente.

| ADR | Recomendación | Corrección exacta antes de aceptación o seguimiento |
|---|---|---|
| ADR-001 | **No todavía** | Incorporar el registro de contratos de BF-03 y enlazar la baseline frontend con `F0_CLIENT_BASELINE`, cumpliendo su propia condición de líneas 138-143. |
| ADR-002 | **Sí, individualmente** | No tiene bloqueo documental. Como seguimiento, hacer que ArchUnit consuma el registro owner/provider/consumer/paquete de ADR-001 y valide que `configuration` sea el único wiring de callbacks genéricos. |
| ADR-003 | **No todavía** | Añadir invariantes y ceremonia de rotación HMAC de BF-01; conservar respuesta `202`, no enumeración, no claim y ausencia de lectura cross-tenant anónima. |
| ADR-004 | **Sí, individualmente** | No requiere cambio documental. Sus pruebas de reloj SQL/`Clock`, expiración y concurrencia siguen siendo conformidad de implementación. |
| ADR-005 | **No todavía** | Declarar los owners, paquetes, comandos y dirección física de BF-03; documentar que el relay genérico no importa `identity` y que ninguna PII forma parte del mensaje durable. |
| ADR-006 | **No todavía** | Añadir la estructura/owner de fingerprints por versión y su backfill (BF-01); declarar que la evidencia de aviso es resuelta por servidor (BF-02); actualizar la matriz si aparece una tabla auxiliar. |
| ADR-007 | **Sí, individualmente** | No tiene contradicción documental. El plan debe materializar S0 y añadir `429/Retry-After` por endpoint antes de exposición; esto es gate de historia/conformidad, no prerrequisito retroactivo para adoptar el ADR. |
| ADR-008 | **No todavía** | Añadir la vinculación autoritativa exacta de `privacyNoticeVersion` de BF-02 y aclarar que el cliente presenta errores mediante claves i18n derivadas de códigos estables, nunca mostrando prosa de `title/detail` como copy de UI. |

## 9. Orden ajustado de historias y gates

| Orden | Elemento | Condición de entrada verificable |
|---:|---|---|
| 0 | Correcciones BF-01–BF-04 y autorización individual de ADR | Registro de contratos, rotación HMAC, evidencia de privacidad y matriz de gates corregidos; cada ADR necesario pasa a `Aceptado` solo por autorización del propietario. |
| 1 | I1-H01 — Fundación ejecutable mínima | D0 cerrado y matriz historia→ADR satisfecha; como mínimo ADR-001/002/006/007/008 aceptados para el contenido actual. ADR-004 solo si H01 incluye sus primitivas. |
| 1A | C0, P0, E0, F0 y S0 en paralelo | Pueden prepararse durante H01; no introducen funcionalidad anticipada ni permiten datos reales/exposición antes de su aprobación. |
| 2A | I1-H02 — Catálogo institucional | H01 + C0 + F0; ADR de persistencia, OpenAPI y concurrencia aplicables aceptados. |
| 2B | I1-H03 — Onboarding empresarial | H01 + P0 + S0; ADR-003/004/005/006/007/008 aceptados; BF-01/BF-02 probados en contrato. Puede avanzar en paralelo con H02. |
| 3 | I1-H04 — Verificación y credencial empresarial | H03 + E0 + S0; contrato `identity -> notifications` con owner y dirección física verificados. |
| 4 | I1-H05 — Sesión y acceso empresarial | H04 + S0; límites de login fijados y aislamiento tenant probado. |
| 5 | I1-H06 — Onboarding de estudiante | H04 + H05 + P0 + S0; evidencia de aviso autoritativa y flujo de cuenta existente cerrados. |
| 6 | I1-H07 — Declaración académica | H02 + H06; owner `student`, catálogo publicado y semántica de institución despublicada conservada. |
| 7 | Gate R1 — Cierre desplegable | H01–H07 en verde, gates de entorno aprobados, E2E/aislamiento/recuperación/backup/seguridad/accesibilidad/i18n verificados. |

H02 y H03 pueden desarrollarse en paralelo después de H01 porque sus gates y datos son
independientes. R1 no compensa criterios, API, UI, i18n ni pruebas que falten en una
historia anterior.

## 10. Condición para levantar el bloqueo

Una nueva comprobación puede recomendar el plan cuando:

1. ADR-003/006 garanticen unicidad fiscal durante rotación de clave;
2. ADR-006/008 y H03/H06 traten la referencia del aviso como dato no confiable y
   persistan únicamente la resolución autoritativa;
3. ADR-001/005 declaren owner, paquete y dirección física de los contratos de entrega;
4. el plan incorpore matriz historia→ADR, F0 y S0, y complete pruebas 429;
5. los ADR necesarios sean autorizados individualmente por el propietario.

Hasta entonces, todos los ADR deben permanecer en `Propuesto`, el plan no debe darse
por aprobado y no debe iniciarse ninguna historia.

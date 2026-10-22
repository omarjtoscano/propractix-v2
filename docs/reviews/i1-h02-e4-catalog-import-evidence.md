# Evidencia de implementación de I1-H02 E4

- Entrega: `E4 — Parser e importación administrativa`
- Estado: `ACCEPTED`
- Rama: `feature/i1-h02-e4-catalog-import`
- Base integrada y aceptada de E3: `1549a4be80c87a1f7639a05ae66cda93efd10f3e`
- Autorización: Product Owner, 2026-10-18
- Aprobación expresa: Product Owner, 2026-10-22
- Commit revisado y aprobado: `700ac21de60748dce3365ba9209288e8b5108926`
- Fecha de validación local de la corrección de revisión: 2026-10-19
- Commit revisado, con PR #11 integrada: `f23abe0d8cfcb5139ba00ab385a41b4f93d4f744`
- Commit funcional corregido y validado localmente: `cf23c1ebbd698e24bf8edb3472f492a366ded6bd`
- Fixture reutilizada sin modificación:
  `docs/catalog/fixtures/academic-institutions-es-synthetic-v1.csv`
- SHA-256 exacto:
  `612b46e73d3fa6843e79b0efd2b0f253326803b976de5627a58daf6d416cc89a`

Se registra la aceptación expresa de E4 por el Product Owner sobre el SHA
indicado. I1-H02 permanece `IN_PROGRESS`; E5A y las entregas posteriores siguen
sin autorización. La PR #10 permanece abierta y sin merge.
`CL0_CLOUD_STAGING` permanece `PENDING`.

## Correcciones de revisión del 2026-10-19

- El CSV que contiene únicamente la cabecera exacta y su LF final se rechaza
  explícitamente como `RejectedArtifact(INVALID_SCHEMA)`, antes de extraer los
  registros. No lanza `StringIndexOutOfBoundsException` ni se confunde con un
  error de configuración.
- Las pruebas cubren parser, dry run a través del arranque Boot y adapter CLI,
  y publicación mediante el caso de uso E2. Verifican códigos de rechazo,
  ausencia de llamadas a persistencia, reloj y generadores UUID durante el
  rechazo de aplicación, y conservación de todas las columnas de ambas tablas
  del snapshot anterior en PostgreSQL 16.
- Ambos modos arrancan mediante el mismo `SpringApplicationBuilder` y usan la
  misma configuración tipada y el mismo bean de parser. Se elimina la carga
  manual de YAML del dry run. Los argumentos CLI se propagan desde ambas
  entradas; Boot resuelve perfiles, configuración externa e imports.
- Una prueba con `spring.config.additional-location` y `spring.config.import`
  sobrescribe `artifact-path`, `artifact-hash`, metadatos y fuentes/países
  permitidos. Compara las propiedades de ambos modos, valida las listas en ambos
  parsers, ejecuta el dry run y la publicación y contrasta los metadatos
  persistidos con la configuración externa.
- El contexto de dry run solo registra propiedades y parser, sin
  autoconfiguración. La prueba comprueba ausencia de DataSource, Flyway, JPA,
  transacciones, repositorios, publicación y generadores UUID. Tras ejecutarlo,
  PostgreSQL aún no tiene tablas del catálogo ni historial Flyway.

La [PR #11](https://github.com/omarjtoscano/propractix-v2/pull/11) ya está
integrada en esta rama mediante `f23abe0d8cfcb5139ba00ab385a41b4f93d4f744`.
Por tanto, el diff acumulado de la PR #10 **sí contiene cambios frontend**:
errores ES/EN por códigos, `ErrorMessage`, límite de errores del shell,
selector/preferencia de idioma, resolución de locale y controles del cliente.
Véase la [guía i18n](../development/i18n-and-errors.md). Su integración no
constituyó la aceptación de E4 ni inició E5A. E4 se acepta por la aprobación
posterior del 2026-10-22; la corrección de revisión no añadió cambios frontend.

## Alcance verificado

- `StrictCsvCatalogArtifactParser` implementa el contrato sin añadir una
  librería CSV.
- El parser recibe las fuentes y países permitidos mediante configuración
  tipada; no contiene condicionales por fuente o país.
- Los perfiles `local` y `test` habilitan únicamente `SYNTHETIC` y `ES`. Los
  perfiles `staging` y `production` deshabilitan la importación manual y solo
  permiten la fuente configurada `RUCT`, por lo que no admiten `SYNTHETIC`.
- Los comandos `npm run catalog:dry-run` y `npm run catalog:publish` son
  entradas administrativas explícitas y no se ejecutan durante el arranque
  normal del servidor.
- El dry run usa un contexto ligero sin DataSource, generadores UUID ni caso de
  uso de publicación. La publicación usa un contexto administrativo no-web que
  conecta el caso de uso E2 con el adapter PostgreSQL E3.
- `artifactHash` se calcula sobre los bytes exactos antes de parsear y se
  compara con el valor declarado. `sourceDownloadHash` permanece como metadato
  opcional separado; también se conservan `sourceUrl`, `retrievedAt` y
  `sourceAttribution`.

## Contrato CSV probado

Las pruebas cubren UTF-8 válido e inválido, BOM, CRLF, CR, ausencia y exceso del
LF final, cabecera incorrecta o reordenada, cinco columnas exactas, filas
vacías, campos quoted, comas, comillas duplicadas, tildes, NFC, espacios
exteriores, controles, saltos, `sourceRecordId` con cero inicial,
`sourceStatus` vacío sin inferencia, referencias externas duplicadas y fuente o
país fuera de configuración. La fixture se lee antes y después de parsear para
verificar que sus bytes no cambian.

## Comportamiento administrativo y sanitización

Las pruebas del adapter verifican que el dry run no invoca la publicación y
solo devuelve código, hash y recuentos agregados. También comprueban códigos de
salida no-cero para hash inválido, concurrencia, rechazo y excepción inesperada
de almacenamiento. La salida y los eventos de log capturados se inspeccionan
para descartar filas CSV, nombres, referencias individuales, URL con
credencial, atribución privada, SQL y contenido del artefacto.

La integración PostgreSQL 16 verifica la publicación sintética completa,
repetición idempotente sin nuevos UUID ni importaciones, artefacto inválido que
conserva el snapshot anterior, rollback ante fallo de almacenamiento y wiring
real del contexto administrativo E4 con el caso de uso E2 y los adapters E3.

## Evidencia manual local histórica del 2026-10-18

El dry run terminó con código 0 y emitió únicamente:

```text
catalog_import_result code=catalog_artifact_valid total_rows=3 published_rows=3 omitted_rows=0 artifact_hash=612b46e73d3fa6843e79b0efd2b0f253326803b976de5627a58daf6d416cc89a
```

Inmediatamente después del dry run, ambas tablas del catálogo seguían
ausentes en PostgreSQL, demostrando que no inició Flyway ni escribió el
snapshot.

La primera publicación manual terminó con código 0:

```text
catalog_import_result code=catalog_published import_id=54424c4e-2c8d-4d6a-ae52-38fcdb16bc1d artifact_hash=612b46e73d3fa6843e79b0efd2b0f253326803b976de5627a58daf6d416cc89a
```

La repetición devolvió el mismo import y el artefacto activo sin cambios:

```text
catalog_import_result code=catalog_active_artifact_unchanged import_id=54424c4e-2c8d-4d6a-ae52-38fcdb16bc1d artifact_hash=612b46e73d3fa6843e79b0efd2b0f253326803b976de5627a58daf6d416cc89a
```

La comprobación agregada posterior mostró una importación, tres instituciones,
un único snapshot `PUBLISHED`, el hash esperado, `sourceDownloadHash` ausente y
coincidencia exacta de URL, fecha de recuperación y atribución configuradas.
No se consultaron ni mostraron nombres o referencias individuales.

## Validaciones históricas del 2026-10-18

| Comando | Resultado |
|---|---|
| `npm run infra:up` | Correcto; PostgreSQL y Mailpit saludables |
| `npm run catalog:dry-run` | Correcto; 3 filas válidas y ninguna escritura |
| `npm run catalog:publish` | Correcto; publicación completa y repetición idempotente |
| `sha256sum docs/catalog/fixtures/academic-institutions-es-synthetic-v1.csv` | Correcto; hash exacto aprobado |
| `npm run server:test` | Correcto; 121 pruebas, 0 fallos, 0 errores y 1 benchmark omitido |
| `npm run server:verify` | Correcto; 121 pruebas y empaquetado completado |
| `npm run check` | Correcto; baseline, gate de despliegue, OpenAPI, servidor y cliente |
| `git diff --check` | Correcto |

## Validaciones de la corrección del 2026-10-19

Entorno: Java 21.0.12.1, Node 24.12.0, Docker 29.8.0 y PostgreSQL 16.15
mediante Testcontainers. `check` se ejecutó con la versión fijada npm 11.9.0
(`npx --yes npm@11.9.0 run check`).

| Comando | Resultado |
|---|---|
| `npm run infra:up` | Correcto; PostgreSQL y Mailpit saludables |
| Pruebas dirigidas de parser, CLI, integración y configuración E4 | 26 pruebas correctas |
| `npm run catalog:dry-run` | Código 0; 3 filas válidas y hash aprobado |
| `npm run server:test` | 126 pruebas, 0 fallos, 0 errores, 1 benchmark omitido |
| `npm run server:verify` | Mismas 126 pruebas; empaquetado correcto |
| `npm run check` | Correcto; baseline, gate, OpenAPI, servidor, lint i18n, 6 pruebas del guard, 45 Vitest y build/typecheck del cliente |
| `git diff --check` | Correcto |
| Hash de la fixture | `612b46e73d3fa6843e79b0efd2b0f253326803b976de5627a58daf6d416cc89a`, sin cambios |

## Fuera de alcance preservado

No se obtuvo, transformó ni publicó RUCT real; no se creó documentación bajo
`docs/catalog/snapshots/`; no se añadió REST, OpenAPI, rate limiting,
AWS ni cambios de `CL0_CLOUD_STAGING`; no se inició E5A. Los cambios frontend
del diff acumulado proceden de la corrección i18n integrada de la PR #11.
La fixture sintética existente se reutilizó sin modificar sus bytes.

## Evidencia remota del commit aprobado

Verificada el 2026-10-22 sobre `700ac21de60748dce3365ba9209288e8b5108926`:

| Comprobación | Resultado |
|---|---|
| [CI #34](https://github.com/omarjtoscano/propractix-v2/actions/runs/39282934222) | `completed / success`; los cinco jobs correctos |
| [dependency-and-filesystem-scan, incluido Trivy](https://github.com/omarjtoscano/propractix-v2/actions/runs/39282934222/job/118160825110) | `completed / success`; paso `Scan dependencies and filesystem` correcto |
| [Build OCI release candidate #17](https://github.com/omarjtoscano/propractix-v2/actions/runs/39282934186) | `completed / success`; construcción de servidor y cliente, manifiesto y archivos OCI correctos |

Las ejecuciones se contrastaron con el `head_sha` exacto del commit aprobado.
Esta evidencia sustituye la espera en cola registrada el 2026-10-19. La
aceptación no se apoya únicamente en las ejecuciones históricas de `ffb7f537`.
Las pruebas locales del 2026-10-19 corresponden al commit funcional
`cf23c1ebbd698e24bf8edb3472f492a366ded6bd`; `700ac21` añadió su evidencia
documental y es el SHA revisado y aprobado expresamente por el Product Owner.

## Registro documental de aceptación

La actualización del 2026-10-22 solo modifica documentación de estado, planes
y evidencia; no modifica código, infraestructura ni la fixture. Se comprueban
`git diff --check`, el diff completo y el estado de Git antes del commit. No se
atribuyen nuevas ejecuciones locales de servidor o cliente a este cambio.

El nuevo commit documental se publica en `feature/i1-h02-e4-catalog-import`.
Su SHA y los resultados independientes de CI, Trivy y Build OCI se registran
en el [cuerpo de la PR #10](https://github.com/omarjtoscano/propractix-v2/pull/10)
tras verificarlos, sin trasladar automáticamente el verde del commit aprobado.
E4 está `ACCEPTED`; I1-H02 permanece `IN_PROGRESS`; E5A y las entregas
posteriores siguen sin autorización. No se realiza el merge.

## Aislamiento de logging entre pruebas — 2026-10-22

Base de esta corrección técnica: `d0a422f8497b0bc8698936ed2df8d036419b4f5f`.
E4 conserva su aceptación sobre `700ac21`; I1-H02 permanece `IN_PROGRESS`.
E5A y las entregas posteriores siguen sin autorización. No se realiza merge.

### Causa reproducida y corrección limitada a pruebas

El arranque Boot de `CatalogImportApplicationIntegrationTest` cargaba
`logback-catalog-import.xml`, que establece el logger raíz en `OFF` y
reconfigura el `LoggerContext` compartido por toda la JVM. Cerrar el contexto
Spring no recuperaba el logging previo. Si esa clase se ejecutaba antes de
`CatalogPersistenceIntegrationTest`, los appenders de las dos pruebas de
fallos de apertura transaccional capturaban listas vacías. Antes de corregirlo,
el orden importación → persistencia reprodujo exactamente 2 fallos en 13
pruebas, sin errores ni omisiones.

El único archivo de código modificado es
`server/src/test/java/com/propractix/academicinstitution/configuration/CatalogImportApplicationIntegrationTest.java`:

- Antes de cada prueba selecciona temporalmente `LoggingSystem.NONE` mediante
  `LoggingSystem.SYSTEM_PROPERTY`. Esto impide que Boot reconfigure el Logback
  del runner; Logback continúa emitiendo eventos y las pruebas de sanitización
  siguen capturándolos.
- Después de cada prueba restaura el valor previo de esa propiedad, incluida
  su ausencia, antes de cualquier aserción de limpieza. JUnit ejecuta esta
  limpieza también cuando una prueba falla.
- Comprueba que tanto el nivel raíz como las instancias y el orden de los
  appenders raíz se conservan tras los contextos Boot.
- `@Isolated` evita que otros tests de JUnit se ejecuten en paralelo mientras
  esta clase usa la propiedad de alcance JVM.

`CatalogPersistenceIntegrationTest`, su helper de captura y todas sus
aserciones permanecen intactos: se exige el evento exacto, sin throwable ni
URL JDBC privada, usuario, contraseña, SQL o detalle de socket. No se fuerza
un nivel de logger en el consumidor para ocultar el fallo del productor.
La configuración de logging de producción y del comando administrativo no
cambia; tampoco cambian dependencias, plugins, migraciones, fixture,
infraestructura, endpoints, dominio ni contratos de aplicación.

### Ambos órdenes dentro de una misma JVM

Las siguientes dos ejecuciones usan un único fork reutilizado para ambas
clases. Se comprobó en sus logs el orden real de ejecución:

```bash
./server/mvnw -f server/pom.xml -Dtest=CatalogImportApplicationIntegrationTest,CatalogPersistenceIntegrationTest -DforkCount=1 -DreuseForks=true -Dsurefire.runOrder=reversealphabetical test
./server/mvnw -f server/pom.xml -Dtest=CatalogImportApplicationIntegrationTest,CatalogPersistenceIntegrationTest -DforkCount=1 -DreuseForks=true -Dsurefire.runOrder=alphabetical test
```

| Orden | Resultado después de la corrección |
|---|---|
| Importación → persistencia (`reversealphabetical`) | 13 pruebas, 0 fallos, 0 errores, 0 omitidas |
| Persistencia → importación (`alphabetical`) | 13 pruebas, 0 fallos, 0 errores, 0 omitidas |

Además, una sonda temporal fuera del repositorio, con JUnit Platform Launcher
6.0.3 ya utilizado por Surefire, ejecutó ambas secuencias consecutivas en un
**único proceso Java**, sin reiniciar JVM ni Logback entre clases. El proceso
`69258` ejecutó importación → persistencia → persistencia → importación:

```text
ORDER_PROBE pid=69258 step=1 class=com.propractix.academicinstitution.configuration.CatalogImportApplicationIntegrationTest tests=2 failed=0 logging_property_restored=true
ORDER_PROBE pid=69258 step=2 class=com.propractix.academicinstitution.adapter.out.persistence.CatalogPersistenceIntegrationTest tests=11 failed=0 logging_property_restored=true
ORDER_PROBE pid=69258 step=3 class=com.propractix.academicinstitution.adapter.out.persistence.CatalogPersistenceIntegrationTest tests=11 failed=0 logging_property_restored=true
ORDER_PROBE pid=69258 step=4 class=com.propractix.academicinstitution.configuration.CatalogImportApplicationIntegrationTest tests=2 failed=0 logging_property_restored=true
ORDER_PROBE_SUCCESS pid=69258 tests=26 order=import,persistence,persistence,import
```

Cada paso comprueba recuento esperado, ausencia de fallos y omisiones y
restauración de la selección de logging. La primera secuencia parte de la
propiedad ausente; la segunda parte del valor explícito
`org.springframework.boot.logging.logback.LogbackLoggingSystem`. La sonda usa
las clases compiladas reales y no añade dependencias al proyecto.

### Validación completa del ajuste de soporte de pruebas

| Comprobación | Resultado |
|---|---|
| `npm run infra:up` | Correcto; PostgreSQL y Mailpit locales saludables |
| Ambos órdenes con Surefire y un único fork reutilizado | 13 pruebas por orden, sin fallos, errores ni omisiones |
| Ambas secuencias consecutivas en la JVM `69258` | 26 pruebas correctas; selección de logging restaurada en cada paso |
| `npm run server:test` | 126 pruebas, 0 fallos, 0 errores, 1 benchmark omitido |
| `npm run server:verify` | Mismas 126 pruebas; empaquetado correcto |
| `git diff --check` | Correcto |
| Fixture sintética | SHA-256 `612b46e73d3fa6843e79b0efd2b0f253326803b976de5627a58daf6d416cc89a`, sin cambios |

El SHA del ajuste y sus comprobaciones remotas independientes de CI, Trivy y
Build OCI se registran en el [cuerpo de la PR #10](https://github.com/omarjtoscano/propractix-v2/pull/10)
tras el push y la verificación. No se toma el verde de `700ac21` ni el reintento
del commit documental `d0a422f` como evidencia remota de esta corrección.

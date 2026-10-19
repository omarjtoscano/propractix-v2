# Evidencia de implementación de I1-H02 E4

- Entrega: `E4 — Parser e importación administrativa`
- Estado: `IN_PROGRESS`
- Rama: `feature/i1-h02-e4-catalog-import`
- Base integrada y aceptada de E3: `1549a4be80c87a1f7639a05ae66cda93efd10f3e`
- Autorización: Product Owner, 2026-10-18
- Fecha de validación local de la corrección de revisión: 2026-10-19
- Commit revisado, con PR #11 integrada: `f23abe0d8cfcb5139ba00ab385a41b4f93d4f744`
- Commit funcional corregido y validado localmente: `cf23c1ebbd698e24bf8edb3472f492a366ded6bd`
- Fixture reutilizada sin modificación:
  `docs/catalog/fixtures/academic-institutions-es-synthetic-v1.csv`
- SHA-256 exacto:
  `612b46e73d3fa6843e79b0efd2b0f253326803b976de5627a58daf6d416cc89a`

E4 permanece `IN_PROGRESS`: este documento no registra aceptación, no cierra
I1-H02 y no autoriza E5A ni ninguna entrega posterior.

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
Véase la [guía i18n](../development/i18n-and-errors.md). Esta integración no
acepta E4 ni inicia E5A; esta corrección de revisión no añade cambios frontend.

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

## Evidencia remota

La Draft PR #10 permanece abierta y sin merge. La evidencia histórica de
`ffb7f537125072b77666d34813a2ea94b7d6425d` no valida esta corrección.
Las validaciones locales del 2026-10-19 corresponden al commit funcional
`cf23c1ebbd698e24bf8edb3472f492a366ded6bd`, publicado en la misma rama y PR #10.
GitHub ha creado estas ejecuciones para ese SHA:

| Comprobación | Ejecución del SHA funcional corregido |
|---|---|
| CI | [CI #33](https://github.com/omarjtoscano/propractix-v2/actions/runs/39282326341) |
| Dependencias y filesystem, incluido Trivy | [dependency-and-filesystem-scan](https://github.com/omarjtoscano/propractix-v2/actions/runs/39282326341/job/118158889001) |
| Build OCI | [Build OCI release candidate #16](https://github.com/omarjtoscano/propractix-v2/actions/runs/39282326439) |

Al registrar esta evidencia, las ejecuciones siguen en cola: no se atribuye
un resultado verde antes de que terminen. El estado final y las ejecuciones
del HEAD posterior que solo registra esta evidencia se mantienen en el
[cuerpo de la PR #10](https://github.com/omarjtoscano/propractix-v2/pull/10),
con su SHA exacto. E4 e I1-H02 siguen `IN_PROGRESS`.

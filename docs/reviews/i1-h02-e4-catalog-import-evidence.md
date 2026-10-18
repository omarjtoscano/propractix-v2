# Evidencia de implementación de I1-H02 E4

- Entrega: `E4 — Parser e importación administrativa`
- Estado: `IN_PROGRESS`
- Rama: `feature/i1-h02-e4-catalog-import`
- Base integrada y aceptada de E3: `1549a4be80c87a1f7639a05ae66cda93efd10f3e`
- Autorización: Product Owner, 2026-10-18
- Fecha de validación local: 2026-10-18
- Fixture reutilizada sin modificación:
  `docs/catalog/fixtures/academic-institutions-es-synthetic-v1.csv`
- SHA-256 exacto:
  `612b46e73d3fa6843e79b0efd2b0f253326803b976de5627a58daf6d416cc89a`

E4 permanece `IN_PROGRESS`: este documento no registra aceptación, no cierra
I1-H02 y no autoriza E5A ni ninguna entrega posterior.

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

## Evidencia manual local

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

## Validaciones

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

## Fuera de alcance preservado

No se obtuvo, transformó ni publicó RUCT real; no se creó documentación bajo
`docs/catalog/snapshots/`; no se añadió REST, OpenAPI, frontend, rate limiting,
AWS ni cambios de `CL0_CLOUD_STAGING`; no se inició E5A. La fixture sintética
existente se reutilizó sin modificar sus bytes.

## Evidencia remota

La Draft PR debe permanecer abierta y sin merge. CI, el job
`dependency-and-filesystem-scan` con Trivy y Build OCI se documentarán aquí
cuando terminen; E4 no se marcará `ACCEPTED` como parte de esta implementación.

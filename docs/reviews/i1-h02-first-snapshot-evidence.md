# I1-H02 E9 — Primer snapshot RUCT y evidencia de publicación local

- Validación original: 2026-11-01. Aceptación expresa del PO: 2026-11-02.
- Estado vigente: E9 `ACCEPTED`; I1-H02 `DONE / ACCEPTED` localmente.
- Commit funcional aprobado: [`9c3c6cdb3ad44fb4b289208f073a1f76694cbf84`](https://github.com/omarjtoscano/propractix-v2/commit/9c3c6cdb3ad44fb4b289208f073a1f76694cbf84).
- Mapeo: `MAPPING_APPROVED_BY_PRODUCT_OWNER`; revisión técnica asistida de ChatGPT trazada abajo.
- Publicación local: autorizada; resultados registrados abajo.
- Rama: `feature/i1-h02-e9-ruct-first-snapshot`.
- Base inicial: `3d339d1598c4a7deafedea3d4d9d78fc16c87088`.
- PR #20 integrada en el incremento mediante `15eb4d03ae2e4a0834e5f2a808edb75b416eebc9`;
  E9 actualizada mediante merge `0069c9a`, conservando `globalSetup` y descubrimiento Playwright.
- E8 permanece `ACCEPTED` sobre `fc9660e997df990d08677f6a55d91d57f92ef9c7`.
  La PR [#17](https://github.com/omarjtoscano/propractix-v2/pull/17) está integrada
  mediante `866c4c71511c181564b8f8d165c06759fcb4442b`, contrastado con Git y GitHub.
  El Product Owner confirma la integración de la PR
  [#18](https://github.com/omarjtoscano/propractix-v2/pull/18) mediante el SHA base
  anterior. No se modifican aquellas PR ni sus resultados históricos.
- El SHA de esta entrega se registra en su Draft PR y en el mensaje de entrega.

## Autorización y revisión diferenciadas

La primera fase preparó CSV, manifest y evidencia para segunda revisión, sin
publicar. El Product Owner aprueba después expresamente en este chat, el
2026-11-01, el mapeo revisado en la PR #19 sobre el hash exacto
`ad2a9ca6f05dfe35851dcc00f3cf568e034d64db5b63d4009a2029fe78633b04` y autoriza continuar E9 tras integrar la PR #20.

Se registran por separado la revisión técnica y la decisión de producto:

1. **ChatGPT, revisión técnica asistida en este chat:** contraste del mapeo el
   **2026-11-01** y revisión final **favorable el 2026-11-02**. Identidad, tipo y
   fechas completados por declaración expresa del PO el 2026-11-02. La versión
   original de la evidencia solo la registraba como referida por el PO; esta
   actualización completa esa trazabilidad sin inventar una revisión humana
   ni convertir las comprobaciones del agente en aprobación de producto.
2. **Aprobaciones del PO:** aprobación del mapeo y autorización de continuación
   local el 2026-11-01; aprobación expresa de **E9 y aceptación local de I1-H02
   el 2026-11-02** sobre `9c3c6cdb3ad44fb4b289208f073a1f76694cbf84`. E9 queda
   `ACCEPTED`; I1-H02 `DONE / ACCEPTED`. El PO confirma CI, Trivy y Build OCI
   verdes sobre ese SHA; es su confirmación, no una consulta remota del agente.

El manifest conserva ambos registros con procedencia separada. No se asignan
horas de revisión no aportadas. Los resultados originales de validación del
2026-11-01 y sus estados de entonces se conservan **byte a byte** en
[i1-h02-e9-local-validation.json](i1-h02-e9-local-validation.json), correspondientes
al commit funcional aprobado. Son evidencia histórica de aquella ejecución; la
aceptación vigente posterior se registra aquí, en el manifest y en el
[documento de aceptación H02](aceptacion-i1-h02.md).

La publicación original se realizó por la CLI existente contra PostgreSQL de
loopback, sin inserts/updates directos del catálogo; las consultas SQL de
comprobación fueron de solo lectura. El E2E sintético utilizó su proyecto
Compose desechable separado. No se repiten esas operaciones para este registro
documental. C0/S0 siguen `CLOSED`; CL0 sigue `PENDING`.

Solo se autoriza documentación y cuerpo de PR #19 en la misma rama, con commit
y push. Código y bytes del CSV aprobado intactos; sin merge, inicio H03 ni AWS.
La PR continúa Draft. CI, Trivy y Build OCI del nuevo SHA documental los verifica
el PO, sin espera ni supervisión del agente; no condicionan retroactivamente
la aceptación local expresa del SHA funcional.

## Adquisición oficial y procedencia

Ventana de adquisición ejecutada conforme a la autorización de Producto:
`2026-11-01T10:38:02Z` a `2026-11-01T10:38:29Z`.
Desde la [consulta oficial de universidades RUCT](https://www.educacion.gob.es/ruct/consultauniversidades?actual=universidades)
se envió el formulario **Consultar** sin filtros de comunidad, universidad ni
naturaleza. La respuesta indicó **109 registros**. Se siguió el enlace **Excel**
de **Opciones de exportación**, mediante peticiones HTTP dirigidas manualmente.
No se utiliza el desplegable de universidades como sustituto de la exportación.

La [exportación oficial utilizada](https://www.educacion.gob.es/ruct/listauniversidades?actual=universidades&cccaa=&tipo_univ=&d-8320336-e=2&6578706f7274=1&codigoUniversidad=&consulta=1)
respondió HTTP 200, sin redirección, a las
`2026-11-01T10:38:28.735493Z`. Es un OLE2/BIFF8 real de **62.976 bytes**,
con una hoja llamada `-`, 110 filas incluida cabecera y 23 columnas.
El charset ISO-8859-1 anunciado por HTTP no se aplica al binario XLS.

El original se conserva temporalmente en
`.local/catalog-acquisition/20261101T103802Z/universidades.xls`.
El directorio contiene también páginas de consulta/resultados, metadatos de
descarga, extracción mínima A/B, informe de transformación y log/configuración
del dry run. Está ignorado por Git y excluido del contexto Docker. El CSV/manifest
documental tampoco entra en las imágenes. Conservar el original hasta resolver
la segunda revisión; su eliminación posterior queda a decisión del owner.
No se versionan las demás columnas del Excel ni el original.

| Campo | Valor |
|---|---|
| `sourceSystem` | `RUCT` |
| `sourceAttribution` | Origen de los datos: Ministerio de Ciencia, Innovación y Universidades — Registro de Universidades, Centros y Títulos (RUCT) |
| `sourceDownloadHash` | `63e3d2bdff91bb780521a7a5354f6e2e1d7338b75f2d8300627032409024199a` |
| `artifactHash` | `ad2a9ca6f05dfe35851dcc00f3cf568e034d64db5b63d4009a2029fe78633b04` |
| Bytes del CSV canónico | 4.512 |

Ambos hashes son SHA-256 calculados por ProPractix sobre bytes exactos. No son
checksums emitidos por RUCT ni acreditan una revisión humana.

## Mapeo y reconciliación

Artefactos preparados: [CSV canónico](../catalog/snapshots/ruct-2026-11-01/academic-institutions-es.csv)
y [manifest](../catalog/snapshots/ruct-2026-11-01/manifest.yaml).
El manifest registra las 23 columnas originales, las 21 descartadas, tipos de
celda, transformación por columna, orden, serialización, recuentos, todas las
omisiones, trazabilidad de revisión, publicación y aceptación local posterior.

- `Código` (B) → `sourceRecordId`: las 109 celdas son texto en el XLS. Copia
  literal, sin conversión numérica ni padding; 90 de las 99 claves incluidas
  comienzan por cero. No se deriva ningún UUID.
- `Universidad` (A) → `officialName`: NFC y trim exterior, conservando grafía,
  tildes y mayúsculas. Ninguna de las 99 denominaciones requirió modificación.
- `sourceSystem=RUCT`, `countryCode=ES`, conforme al ámbito C0 aprobado.
- `sourceStatus` vacío en las 99 filas. No existe una columna de estado
  inequívoca; `Tipo`, las fechas y la presencia registral no se usan para inferirlo.
- Se revisaron las 109 parejas clave/denominación y se excluyeron explícitamente
  diez agrupaciones. No se usa una heurística de prefijo: se conservan, entre
  otras, CUNEF, ESIC, IE, Mondragón y las denominaciones `Universitat`.

| Control | Resultado |
|---|---:|
| Filas de datos del XLS y resultado de consulta | 109 |
| Universidades canónicas | 99 |
| Agrupaciones de centros omitidas | 10 |
| Centros individuales / titulaciones / filas ambiguas / incompletas omitidas | 0 / 0 / 0 / 0 |
| Claves duplicadas en origen / referencias duplicadas en CSV | 0 / 0 |
| Denominaciones canónicas duplicadas | 0 |
| Reconciliación | 109 = 99 + 10 |

Las diez omisiones están enumeradas con clave textual, nombre original, fila
Excel y motivo `aggregate_not_university` en el manifest: `099`, `086`, `105`,
`092`, `101`, `095`, `093`, `091`, `096` y `000`. Se omiten agrupaciones de
enseñanzas artísticas, Defensa, centros extranjeros y otros centros. La fila 16
del Excel (`000`) también se excluye aunque esté intercalada entre universidades.

Se mantiene el orden fuente. CSV UTF-8 sin BOM, cinco columnas exactas, comas,
quoting mínimo, LF y un salto final. El nombre con coma de la clave `100` se
serializa entre comillas. Tras calcular el hash no se transforman los bytes.

## Reproducción desde el original conservado

Requiere Python y `xlrd==2.0.2`, instalado temporalmente en
`.local/catalog-acquisition/python-deps`; no es dependencia de la aplicación.
Desde la raíz, este procedimiento reconstruye en memoria los bytes y los compara
con el artefacto versionado. Falla si cambia el original, sus columnas, tipos,
recuentos, omisiones, duplicados o el CSV. No escribe ni publica:

```python
import csv, hashlib, io, sys, unicodedata
from collections import Counter
from pathlib import Path
sys.path.insert(0, '.local/catalog-acquisition/python-deps')
import xlrd  # 2.0.2

original = Path('.local/catalog-acquisition/20261101T103802Z/universidades.xls')
assert hashlib.sha256(original.read_bytes()).hexdigest() == \
    '63e3d2bdff91bb780521a7a5354f6e2e1d7338b75f2d8300627032409024199a'
book = xlrd.open_workbook(original)
assert book.sheet_names() == ['-']
sheet = book.sheet_by_index(0)
assert (sheet.nrows, sheet.ncols) == (110, 23)
assert sheet.row_values(0)[:2] == ['Universidad', 'Código']
omitted = {'099', '086', '105', '092', '101', '095', '093', '091', '096', '000'}
seen, excluded, rows = Counter(), set(), []
for row in range(1, sheet.nrows):
    assert sheet.cell(row, 0).ctype == sheet.cell(row, 1).ctype == xlrd.XL_CELL_TEXT
    name, key = sheet.row_values(row)[:2]
    seen[key] += 1
    if key in omitted:
        excluded.add(key)
        continue
    rows.append(['RUCT', key, unicodedata.normalize('NFC', name.strip()), 'ES', ''])
assert len(seen) == 109 and all(count == 1 for count in seen.values())
assert excluded == omitted and len(rows) == 99
assert len({row[2] for row in rows}) == 99
assert sum(row[1].startswith('0') for row in rows) == 90
buffer = io.StringIO(newline='')
writer = csv.writer(buffer, lineterminator='\n')
writer.writerow(['sourceSystem', 'sourceRecordId', 'officialName', 'countryCode', 'sourceStatus'])
writer.writerows(rows)
exact = buffer.getvalue().encode('utf-8')
assert hashlib.sha256(exact).hexdigest() == \
    'ad2a9ca6f05dfe35851dcc00f3cf568e034d64db5b63d4009a2029fe78633b04'
assert exact == Path('docs/catalog/snapshots/ruct-2026-11-01/academic-institutions-es.csv').read_bytes()
print('109 source rows; 99 canonical rows; 10 omissions; exact bytes match')
```

Una nueva descarga puede variar. Este procedimiento reproduce el original con
el hash registrado; una descarga distinta necesita su propia adquisición y revisión.

## Validación con el parser y dry run existentes

Se creó exclusivamente bajo `.local/catalog-acquisition/20261101T103802Z/dry-run.yaml`
un override de `propractix.academic-institution.catalog-import`, con
`enabled=true`, `allowed-source-systems=[RUCT]`, `allowed-countries=[ES]`, ruta
absoluta al CSV y los seis metadatos del manifest (`schemaVersion`, `sourceUrl`,
`retrievedAt`, `sourceAttribution`, `artifactHash`, `sourceDownloadHash`), usando
las claves kebab-case de las propiedades existentes. No se cambia el perfil local
versionado, que sigue apuntando a la fixture sintética.

```bash
npm run catalog:dry-run -- \
  "-Dspring-boot.run.arguments=--spring.config.additional-location=file:$(pwd)/.local/catalog-acquisition/20261101T103802Z/dry-run.yaml"
```

Código de salida **0**. Línea de resultado del adapter:

```text
catalog_import_result code=catalog_artifact_valid total_rows=99 published_rows=99 omitted_rows=0 artifact_hash=ad2a9ca6f05dfe35851dcc00f3cf568e034d64db5b63d4009a2029fe78633b04
```

`published_rows` cuenta candidatos válidos en este contrato de dry run; **no
significa publicación**. Las diez omisiones ocurrieron antes, al transformar
109 filas fuente en 99 canónicas. El parser admite las 99 sin omitir ninguna.
La CLI verifica `artifactHash`; el hash del original se verifica separadamente
por el procedimiento anterior, pues el dry run no recibe el XLS.

## Revisión del mapeo y muestra trazable

El estado inicial `PENDING_REVIEW` del commit `b31eda0` queda sustituido por
la aprobación expresa del mapeo del PO registrada arriba, que autoriza la
publicación local. La trazabilidad completada el 2026-11-02 identifica la
revisión técnica asistida de ChatGPT, separada de la aprobación PO. Las
comprobaciones reproducibles y la muestra siguientes conservan su procedencia
del 2026-11-01 y no se presentan como una revisión humana independiente.

Muestra conservada para trazabilidad del contraste B/A del XLS, CSV y fuente RUCT:

| Fila Excel | Clave literal | Denominación incluida | Motivo de muestra |
|---:|---|---|---|
| 11 | `089` | CUNEF Universidad | No depende del prefijo Universidad. |
| 15 | `061` | Mondragón Unibertsitatea | Grafía y tilde conservadas. |
| 30 | `010` | Universidad Complutense de Madrid | Cero inicial. |
| 33 | `001` | Universidad de Alicante | Dos ceros iniciales. |
| 42 | `100` | Universidad de Diseño, Innovación y Tecnología (UDIT) | Coma y quoting CSV. |
| 54 | `110` | Universidad de Mallorca | Clave sin cero inicial. |
| 106 | `018` | Universitat de València (Estudi General) | Variante lingüística y tildes. |
| 110 | `027` | Universitat Politècnica de València | Última fila, exportación completa. |

La ficha pública para una muestra usa
`https://www.educacion.gob.es/ruct/universidad.action?codigoUniversidad=<clave>&actual=universidades`.
Revisar también las diez omisiones, especialmente `000` y las agrupaciones
extranjeras. No se deriva vigencia ni significado legal de este contraste.

## Aviso neutral de UI ES/EN autorizado

`client/src/features/academic-institutions/AcademicInstitutionSearchPage.tsx`
renderizaba siempre `academicInstitutions.synthetic`, incluso si la API devolvía
el snapshot real. La atribución, URL y fecha ya se muestran desde los metadatos
del snapshot en el autocompletado. OpenAPI no expone un discriminador de origen.

Se sustituye únicamente la clave y el párrafo incondicional por un aviso
neutral `academicInstitutions.provenance`, manteniendo el aviso
`academicInstitutions.scope` y la atribución dinámica existentes:

| Locale | Texto implementado para `academicInstitutions.provenance` |
|---|---|
| ES | Consulta la atribución y la fecha del snapshot para conocer el origen de los datos mostrados. |
| EN | Check the snapshot attribution and retrieval date for the source of the displayed data. |

Así el texto sigue siendo verdadero tanto con fixtures sintéticas, cuya
atribución las identifica, como con RUCT real. No se añade flag ni se infiere el
origen examinando nombres, URLs o textos traducidos. Se implementa por autorización expresa del PO: referencia del componente y ambos
catálogos, expectativa de pantalla y comprobación de atribución/fecha/alcance
para datos sintéticos y RUCT en ES/EN. El aviso de alcance informativo sigue sin acreditar afiliación,
matrícula, elegibilidad, convenio ni cumplimiento.

## Validaciones de la primera fase (antes de publicación)

| Comprobación | Resultado |
|---|---|
| `npm run infra:up` | Correcto; PostgreSQL 16 y Mailpit saludables. |
| `npm run server:test` | `BUILD SUCCESS`: 317 pruebas, 0 fallos, 0 errores, 1 benchmark omitido. |
| `npm run server:verify` | `BUILD SUCCESS`: 317 pruebas, 0 fallos, 0 errores, 1 benchmark omitido; ArchUnit y empaquetado correctos. |
| Parser/dry run sobre bytes candidatos | Salida 0, 99 candidatos válidos, 0 omisiones canónicas. |
| Reproducción desde XLS conservado | Bytes reconstruidos idénticos al CSV; ambos SHA-256 correctos. |
| Manifest y reconciliación | 109 = 99 + 10; tipos textuales, ceros iniciales y estados pendientes comprobados. |
| Enlaces Markdown locales en los ocho documentos afectados | 93 comprobados, destinos existentes. |
| `git diff --check`, diff y estado | Revisados; sin errores de whitespace ni cambios funcionales. |

JDK 21 disponible; Docker se ejecutó con acceso autorizado fuera del sandbox.
Los logs locales de backend quedan bajo `.local/catalog-acquisition/`:
`server-test.log` y `server-verify-final.log`. La primera ejecución de verify
quedó interrumpida sin resultado final; la ejecución final completa anterior es
la que acredita el resultado. El benchmark sigue omitido por su condición
explícita existente. No se añaden tests ni se repiten cliente/E2E en esta fase
documental; la evidencia sintética aceptada de E8 conserva su procedencia y alcance.

## Publicación y verificaciones locales de continuación

Publicación mediante el comando existente, con override no versionado
`.local/e9-ruct-validation/publication.yaml`: fuente permitida `RUCT`, país `ES`,
bytes del CSV aprobado, metadatos exactos del manifest y DataSource restringido a
`jdbc:postgresql://127.0.0.1:5432/propractix`. La contraseña sintética sigue
procediendo del config tree local; no se incrusta ni se registra. PostgreSQL
reporta **16.15**. No se publica por SQL ni mediante endpoints administrativos.

```bash
npm run catalog:publish -- \
  "-Dspring-boot.run.arguments=--spring.config.additional-location=file:$(pwd)/.local/e9-ruct-validation/publication.yaml"
```

Resultado inicial (salida 0):

```text
catalog_import_result code=catalog_published import_id=29ca8394-9878-44ba-82f2-2971534b1ead artifact_hash=ad2a9ca6f05dfe35851dcc00f3cf568e034d64db5b63d4009a2029fe78633b04
```

| Evidencia persistida | Resultado |
|---|---|
| `catalogImportId` | `29ca8394-9878-44ba-82f2-2971534b1ead` |
| `publishedAt` | `2026-11-01T18:16:34.162754Z` |
| `artifactHash` | `ad2a9ca6f05dfe35851dcc00f3cf568e034d64db5b63d4009a2029fe78633b04` |
| Recuentos de importación | 99 totales, 99 publicadas, 0 omisiones del CSV canónico. |
| Omisiones previas de transformación | 10 agrupaciones de las 109 filas del XLS, documentadas en el manifest. |
| Versiones `PUBLISHED` / snapshots visibles | 1 / 1. |
| Filas actualmente seleccionables | 99, exactamente iguales a las cinco columnas del CSV; 90 claves conservan ceros iniciales. |
| Metadatos | URL, instante de adquisición, atribución y `sourceDownloadHash` idénticos al manifest. |

La fixture local previa de tres instituciones conserva su importación histórica
`54424c4e-2c8d-4d6a-ae52-38fcdb16bc1d`; la CLI la sustituye por el snapshot RUCT.
No se borra ni se modifica una migración. Las tres instituciones históricas no
pertenecen a la versión seleccionable actual.

La repetición del mismo comando devuelve salida 0:

```text
catalog_import_result code=catalog_active_artifact_unchanged import_id=29ca8394-9878-44ba-82f2-2971534b1ead artifact_hash=ad2a9ca6f05dfe35851dcc00f3cf568e034d64db5b63d4009a2029fe78633b04
```

Se compararon recuentos, metadatos y resúmenes de todas las columnas de ambas
tablas de catálogo, ordenadas por sus IDs: sin nuevas importaciones, cambios de
IDs, versiones, timestamps ni filas. Un cursor HTTP emitido antes de repetir
sigue devolviendo la misma página exacta después.

Se preparó **solo bajo `.local`** una copia inválida del CSV sin LF final,
calculando el hash real de esa copia para ejercitar el parser del publicador.
El artefacto aprobado permanece byte a byte idéntico. La CLI existente devuelve:

```text
catalog_import_result code=catalog_publication_rejected reason=artifact_invalid
```

El proceso administrativo termina con código **6**; npm/Maven propaga código
**1** e informa `Process terminated with exit code: 6`. No se confunden ambos
códigos. Los mismos resúmenes de tablas, import ID, hash, recuentos y página HTTP
permanecen iguales. El cursor previo tampoco se invalida tras el rechazo.
No se provocan fallos destruyendo o renombrando tablas del perfil local.

## Búsqueda HTTP del snapshot real

Se arrancó el backend empaquetado solo en `127.0.0.1`, puerto efímero, contra la
base local anterior; se detuvo al terminar. Sin cambiar S0, cuota, configuración
de proxy ni perfiles versionados. Las peticiones usan exclusivamente
`GET /api/v1/academic-institutions` y readiness existente.

| Comprobación | Resultado |
|---|---|
| Paginación `limit` por defecto | 5 páginas: 20, 20, 20, 20 y 19; cursor final nulo. |
| Identidades | 99 UUID v4 distintos, sin duplicados entre páginas. |
| Denominaciones | Las 99 coinciden exactamente con el CSV aprobado y conservan orden estable de búsqueda. |
| `Alcalá` / `alcala` | Mismos resultados: 1 universidad. |
| `València` / `valencia` | Mismos resultados: 5 universidades. |
| `Politécnica` / `politecnica` | Mismos resultados: 4 universidades. |
| Procedencia pública | Atribución, URL y fecha exactas del snapshot publicado. |
| Repetición / fallo | Mismo cursor y misma segunda página antes y después de ambas operaciones CLI. |

Los resultados agregados, sin cursores ni filas completas, se versionan en
[i1-h02-e9-local-validation.json](i1-h02-e9-local-validation.json).
Los logs, overrides y comparaciones locales permanecen ignorados en
`.local/e9-ruct-validation/`. El original RUCT temporal sigue conservado para
reproducción; no se incorpora como fixture ni imagen.

## Validaciones de continuación y estado final de la entrega

| Comprobación de esta continuación | Resultado |
|---|---|
| `npm run infra:up` | PostgreSQL 16.15 y Mailpit saludables. |
| `npm run server:test` y `npm run server:verify` | 317 pruebas cada uno, 0 fallos, 0 errores, 1 benchmark omitido; ArchUnit y empaquetado correctos. |
| `client:typecheck`, `client:lint`, `client:build` | Correctos; paridad ES/EN, textos visibles y build verificados. |
| `client:test` | 96 pruebas Vitest y una comprobación Node correctas. |
| `client:e2e` mediante npm 11.9.0 | 8/8 Chromium, 49,8 s, sin omisiones, reintentos o flaky; fixtures exclusivamente sintéticas. |
| Limpieza de E2E | Sin contenedores/volumen del proyecto `propractix-e8-408914`; secretos y lock eliminados. |
| OpenAPI lint y `check-migrations.sh` | Correctos; contrato y migraciones sin cambios. |
| Hashes y alcance | CSV aprobado idéntico al de `b31eda0`; SHA-256 del CSV y original correctos; corrección Playwright intacta. |
| `git diff --check`, diff y estado | Revisados; sin cambios en S0, migraciones, OpenAPI, generador ni AWS. |

El servidor efímero de comprobación HTTP se detuvo al terminar. PostgreSQL y
Mailpit locales permanecen disponibles con el snapshot publicado; el E2E no
lo sustituye porque usa otra base/proyecto desechable. El siguiente arranque
normal del backend consultará la versión RUCT activa sin activar ningún import
administrativo automáticamente.
El [documento de aceptación H02](aceptacion-i1-h02.md) registra la aprobación
expresa del PO del 2026-11-02 sobre `9c3c6cdb3ad44fb4b289208f073a1f76694cbf84`:
E9 `ACCEPTED`; I1-H02 `DONE / ACCEPTED` localmente. Las tablas anteriores
conservan sus resultados originales y su fecha/procedencia de ejecución, sin
simular validaciones nuevas. PR #19 Draft y sin merge; CL0 `PENDING`.

## Verificación remota y actualización documental de aceptación

| Elemento | Resultado y procedencia |
|---|---|
| CI sobre `9c3c6cdb3ad44fb4b289208f073a1f76694cbf84` | Verde, confirmado expresamente por el PO el 2026-11-02. |
| Trivy sobre el mismo SHA | Verde, confirmado expresamente por el PO el 2026-11-02. |
| Build OCI sobre el mismo SHA | Verde, confirmado expresamente por el PO el 2026-11-02. |
| Checks del nuevo commit documental | Pendientes de verificación del PO; el agente no espera ni supervisa. |

No se añaden IDs de ejecuciones ni enlaces a checks no aportados. No se consultan
sus ejecuciones. El SHA documental se comunica en la entrega y cuerpo de PR;
no sustituye el SHA funcional aprobado. Se revisan enlaces, coherencia YAML/JSON,
alcance exclusivamente documental, hashes, diff y `git diff --check`: 97 enlaces
locales y sus anclas correctos; YAML/JSON y conservación de resultados verificados. No se
modifican código, CSV ni JSON original, ni se repiten suites o publicación para
esta actualización documental. Sin merge, H03 ni AWS; CL0 sigue `PENDING`.

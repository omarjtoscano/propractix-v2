# I1-H02 E9 — Primer snapshot RUCT, preparación para segunda revisión

- Fecha: 2026-11-01. E9 e I1-H02: `IN_PROGRESS`.
- Segunda revisión del mapeo: `PENDING_REVIEW`. Publicación: `NOT_PUBLISHED`.
- Rama: `feature/i1-h02-e9-ruct-first-snapshot`.
- Base actualizada mediante fetch: `3d339d1598c4a7deafedea3d4d9d78fc16c87088`.
- E8 permanece `ACCEPTED` sobre `fc9660e997df990d08677f6a55d91d57f92ef9c7`.
  La PR [#17](https://github.com/omarjtoscano/propractix-v2/pull/17) está integrada
  mediante `866c4c71511c181564b8f8d165c06759fcb4442b`, contrastado con Git y GitHub.
  El Product Owner confirma la integración de la PR
  [#18](https://github.com/omarjtoscano/propractix-v2/pull/18) mediante el SHA base
  anterior. No se modifican aquellas PR ni sus resultados históricos.
- El SHA de esta entrega se registra en su Draft PR y en el mensaje de entrega.

## Autorización y límite de la fase

El Product Owner autoriza el 2026-11-01 adquirir la exportación oficial,
preparar CSV/manifest y evidencia para la segunda revisión del apartado 8.2 del
[plan H02](../plans/i1-h02-execution-plan.md#82-obtención-del-primer-snapshot-real),
validarlos con parser/dry run existentes, proponer el ajuste de UI y realizar
commit, push y Draft PR. Los artefactos se versionan como candidatos pendientes
de revisión, por esta autorización expresa; esto no completa el paso de revisión
ni habilita el paso de publicación del plan.

No se ejecuta `catalog:publish`, no se entrega RUCT a `PublishInstitutionCatalog`
y no se carga este CSV en PostgreSQL. La validación administrativa usa el contexto
ligero de dry run, sin DataSource ni publicador. Los tests backend existentes usan
sus propias fixtures y bases Testcontainers desechables.
C0 y S0 continúan `CLOSED`; `CL0_CLOUD_STAGING` continúa `PENDING`.
No se reabre C0, no se amplía el análisis jurídico y no cambian código funcional,
S0, migraciones, OpenAPI, generador ni AWS. No se acepta E9 ni se cierra H02.

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
omisiones y los campos pendientes de segunda revisión/publicación.

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

## Segunda revisión pendiente

La preparación y las comprobaciones del agente no sustituyen la segunda persona
exigida por el plan. Antes de publicar debe registrarse en `secondReview` del
manifest persona revisora, fecha UTC, hash exacto revisado, decisión y evidencia
de recuentos/duplicados, muestra y omisiones. Los campos permanecen nulos y el
estado es `PENDING_REVIEW`.

Muestra propuesta para que esa persona contraste B/A del XLS, CSV y fuente RUCT:

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

## Propuesta mínima de UI, pendiente de revisión

`client/src/features/academic-institutions/AcademicInstitutionSearchPage.tsx`
renderiza siempre `academicInstitutions.synthetic`, incluso si la API devuelve
el snapshot real. La atribución, URL y fecha ya se muestran desde los metadatos
del snapshot en el autocompletado. OpenAPI no expone un discriminador de origen.

Se propone sustituir únicamente la clave y el párrafo incondicional por un aviso
neutral `academicInstitutions.provenance`, manteniendo el aviso
`academicInstitutions.scope` y la atribución dinámica existentes:

| Locale | Texto propuesto para `academicInstitutions.provenance` |
|---|---|
| ES | Consulta la atribución y la fecha del snapshot para conocer el origen de los datos mostrados. |
| EN | Check the snapshot attribution and retrieval date for the source of the displayed data. |

Así el texto sigue siendo verdadero tanto con fixtures sintéticas, cuya
atribución las identifica, como con RUCT real. No se añade flag ni se infiere el
origen examinando nombres, URLs o textos traducidos. La propuesta no se implementa
en esta fase. Al autorizarla: cambiar solo la referencia del componente y ambos
catálogos, ajustar la expectativa pertinente y verificar ES/EN, paridad y ambos
orígenes. El aviso de alcance informativo sigue sin acreditar afiliación,
matrícula, elegibilidad, convenio ni cumplimiento.

## Validaciones locales y aceptación pendiente

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

El [documento de aceptación H02](aceptacion-i1-h02.md) es un borrador pendiente.
No hay import ID, snapshot publicado, prueba de búsqueda del RUCT real ni
aceptación expresa de E9/H02. Esas evidencias quedan pendientes tras la segunda
revisión y autorización de continuación. CI, Trivy y Build OCI de esta entrega
los verifica el Product Owner; el agente no espera ni supervisa esas ejecuciones.

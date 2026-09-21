# Esquema mínimo del catálogo académico v1

## Propósito

Este CSV alimenta exclusivamente la búsqueda y el autocompletado informativo de universidades españolas. No representa convenios, aprobación de prácticas, reglas jurídicas ni validaciones realizadas por una universidad.

El fichero no transporta la identidad de dominio. Cada `AcademicInstitution` recibe un `AcademicInstitutionId` interno UUID v4, encapsulado en un value object conforme al [ADR-004](../adr/ADR-004-identificadores-tiempo-concurrencia.md).

## Formato

| Propiedad | Regla |
|---|---|
| Versión | `academic-institution-catalog/v1` |
| Codificación | UTF-8 sin BOM |
| Separador | Coma |
| Terminación | LF y salto final obligatorio |
| Cabecera | Exacta y en el orden documentado |
| Filas | Una universidad inequívoca por fila |

La cabecera exacta es:

```text
sourceSystem,sourceRecordId,officialName,countryCode,sourceStatus
```

## Columnas

| Columna | Obligatoria | Regla |
|---|---:|---|
| `sourceSystem` | Sí | Para datos productivos, valor exacto `RUCT`. La fixture usa `SYNTHETIC` para impedir su confusión con datos oficiales. |
| `sourceRecordId` | Sí | Clave registral de RUCT tratada como texto opaco. Se conservan ceros iniciales. No es identidad de dominio. |
| `officialName` | Sí | Denominación oficial publicada por la fuente, normalizada a Unicode NFC, sin espacios exteriores y con su grafía y tildes conservadas. |
| `countryCode` | Sí | ISO 3166-1 alfa-2. Para este catálogo, valor exacto `ES`. |
| `sourceStatus` | No | Valor publicado claramente por la fuente. Si no existe o resulta ambiguo, la celda queda vacía. No se infiere un estado. |

El par `(sourceSystem, sourceRecordId)` es una referencia externa para procedencia y conciliación manual. El importador lo asocia con la identidad interna definida por el dominio; no lo expone como sustituto de `AcademicInstitutionId`.

## Reglas básicas de validación

- cinco columnas en cada fila y cabecera exacta;
- `sourceSystem`, `sourceRecordId`, `officialName` y `countryCode` no vacíos;
- `countryCode=ES`;
- combinación `(sourceSystem, sourceRecordId)` no duplicada dentro del fichero;
- `officialName` de 1 a 300 puntos de código, en NFC y sin controles ni saltos de línea;
- campos CSV entre comillas cuando contengan coma o comillas; las comillas internas se duplican;
- ausencia de filas en blanco y existencia de un único LF final;
- `sourceStatus` vacío salvo que el origen lo proporcione inequívocamente.

Una fila agregada, ambigua o que represente un centro o una titulación se omite. No es necesario resolverla para aceptar las demás filas válidas.

## Datos aplazados

No forman parte de v1:

- `institutionId` derivado de la clave RUCT;
- aliases, acrónimos y nombres alternativos;
- dirección, municipio, provincia, URL, teléfono o identificadores fiscales;
- centros, titulaciones y planes de estudio;
- estados inferidos;
- reglas de convenio, aprobación o elegibilidad.

Podrán añadirse en una versión posterior solo si aportan valor comprobado a la búsqueda o a otro caso de uso aprobado.

## Metadatos de importación

Los siguientes datos acompañan al snapshot, pero no se repiten en el CSV:

| Metadato | Obligatorio | Uso |
|---|---:|---|
| `sourceUrl` | Sí | Identificar la URL oficial utilizada. |
| `retrievedAt` | Sí | Registrar cuándo se obtuvo la fuente. |
| `sourceAttribution` | Sí | Conservar `Origen de los datos: Ministerio de Ciencia, Innovación y Universidades — Registro de Universidades, Centros y Títulos (RUCT)`. |
| `artifactHash` | Sí | SHA-256 de los bytes exactos del CSV entregado a `PublishInstitutionCatalog` y posteriormente publicado. |
| `sourceDownloadHash` | No | SHA-256 del fichero original descargado de RUCT, únicamente cuando dicho fichero se conserva. |

`artifactHash` siempre identifica el artefacto importado y publicado. `sourceDownloadHash` permite verificar opcionalmente la descarga original y no sustituye a `artifactHash`. Ninguno requiere un checksum proporcionado por RUCT: ambos valores los calcula ProPractix.

## Actualización manual

1. Descargar el fichero desde `sourceUrl` y registrar `retrievedAt` y `sourceAttribution`.
2. Si se conserva el fichero original de RUCT, calcular su `sourceDownloadHash`.
3. Transformar únicamente las columnas necesarias y omitir filas agregadas o ambiguas.
4. Ejecutar la validación básica y serializar el CSV definitivo.
5. Calcular `artifactHash` sobre los bytes exactos de ese CSV; no transformarlo después del cálculo.
6. Si falla la validación, conservar la última versión válida.
7. Si pasa, entregar esos mismos bytes y metadatos a `PublishInstitutionCatalog` y activar manualmente el snapshot.

El primer snapshot real y su evidencia pertenecen a la Definition of Done de `I1-H02`.

## Entrada manual

Una universidad no encontrada no se añade a este CSV. La aplicación conserva una entrada manual separada, marcada como pendiente de verificación y sin referencia RUCT, para que el catálogo no sea una dependencia bloqueante.

## Fixture sintética

La [fixture sintética v1](fixtures/academic-institutions-es-synthetic-v1.csv):

- contiene tres instituciones ficticias;
- usa `sourceSystem=SYNTHETIC`;
- deja `sourceStatus` vacío para verificar que es opcional;
- no deriva de RUCT ni es apta para producción;
- tiene `artifactHash` SHA-256 `612b46e73d3fa6843e79b0efd2b0f253326803b976de5627a58daf6d416cc89a`, calculado sobre los bytes exactos de este CSV, que es el artefacto importado.

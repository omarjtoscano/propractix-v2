# Gobierno mínimo de C0_CATALOG

## Estado y propósito

| Campo | Valor |
|---|---|
| Gate | `C0_CATALOG` |
| Fecha de evaluación | 2026-09-21 |
| Estado | `CLOSED` |
| Aprobado por | Product Owner |
| Fecha de aprobación | 2026-09-21 |
| Commit objeto de aprobación | `e98e11655da7a8f37b6c26d180d6b7ca2d3e8e52` |
| Fuente | Registro de Universidades, Centros y Títulos (`RUCT`) |
| Uso en el MVP | Búsqueda y autocompletado informativo de universidades españolas |
| Implementación | No incluida; `I1-H02` continúa `BLOCKED` únicamente por `S0_PUBLIC_ENDPOINTS` |

RUCT se utiliza como fuente informativa para facilitar la selección de una universidad. No es un bounded context jurídico ni una autoridad sobre prácticas académicas.

## Límite funcional

El catálogo:

- no certifica la aprobación de una práctica;
- no demuestra que exista un convenio entre universidad y empresa;
- no contiene ni determina reglas jurídicas aplicables a una práctica;
- no sustituye la validación realizada por la universidad, la empresa o los responsables del proceso;
- no convierte una selección en prueba de afiliación o representación.

La aprobación, convenio, elegibilidad y cumplimiento pertenecen a flujos posteriores y a sus fuentes correspondientes. El catálogo solo reduce fricción de escritura y mejora la consistencia de búsqueda.

## Fuentes oficiales consultadas

| Fuente | Organismo | Aporte a la decisión | URL |
|---|---|---|---|
| RUCT | Ministerio de Ciencia, Innovación y Universidades | Registro administrativo público y fuente oficial de denominaciones y claves registrales. La consulta ofrece una exportación etiquetada como Excel. | [Página institucional](https://www.ciencia.gob.es/Universidades/RUCT.html), [consulta de universidades](https://www.educacion.gob.es/ruct/consultauniversidades?actual=universidades) |
| Real Decreto 1509/2008 | Estado / BOE | Define el carácter público del RUCT, la sección de Universidades y la clave registral de cada universidad. | [Texto consolidado](https://www.boe.es/buscar/act.php?id=BOE-A-2008-15464) |
| Ley 37/2007 | Estado / BOE | Establece el régimen general de reutilización de información del sector público. | [Texto consolidado](https://www.boe.es/buscar/act.php?id=BOE-A-2007-19814) |
| Real Decreto 1495/2011 | Estado / BOE | Permite la reutilización general de documentos estatales con atribución y condiciones generales. | [Texto consolidado](https://www.boe.es/eli/es/rd/2011/10/24/1495/con) |
| datos.gob.es | Ministerio para la Transformación Digital y de la Función Pública / Red.es | Punto nacional de descubrimiento. No se localizó una distribución RUCT específica el 2026-09-20; esto no impide usar la descarga oficial disponible. | [Catálogo](https://datos.gob.es/es/catalogo/conjuntos-datos), [API del catálogo](https://datos.gob.es/es/apidata) |

La base general de reutilización se considera suficiente para el MVP. No se requiere confirmación escrita, licencia individual, API oficial, SLA, checksum emitido por el Ministerio ni confirmación de no reutilización de la clave registral.

## Alcance del catálogo

### Se incluye

- una fila que represente claramente una universidad española;
- la clave registral mostrada por RUCT como referencia externa;
- la denominación oficial;
- `countryCode=ES`;
- el estado publicado por la fuente solo cuando sea inequívoco.

### Se excluye

- centros, facultades, escuelas, titulaciones y planes de estudio;
- agrupaciones o filas agregadas que no representen claramente una universidad;
- filas ambiguas o incompletas;
- aliases, acrónimos y nombres alternativos durante el MVP;
- inferencias sobre vigencia, convenios, aprobación o elegibilidad.

Una fila excluida no bloquea la actualización. El catálogo inicial puede ser deliberadamente incompleto mientras sea útil y no presente información ambigua como cierta.

## Identidad de dominio y referencia RUCT

`AcademicInstitution` usa un `AcademicInstitutionId` interno, representado como value object y generado como UUID v4 conforme al [ADR-004](../adr/ADR-004-identificadores-tiempo-concurrencia.md).

La clave RUCT no es identidad de dominio ni identificador público de ProPractix. Se conserva como referencia externa:

```text
sourceSystem = RUCT
sourceRecordId = <clave registral>
```

El par `(sourceSystem, sourceRecordId)` permite rastrear el origen y localizar una fila importada. No se exige demostrar que la clave nunca será reutilizada. Un cambio o una colisión observada se trata durante la actualización manual, sin modificar silenciosamente la identidad interna.

## CSV mínimo

El contrato se define en [academic-institution-catalog-schema-v1.md](../catalog/academic-institution-catalog-schema-v1.md). Sus únicas columnas son:

```text
sourceSystem,sourceRecordId,officialName,countryCode,sourceStatus
```

`sourceStatus` queda vacío cuando la fuente no lo proporciona claramente. Los aliases y demás metadatos se aplazan hasta que exista evidencia de que mejoran la búsqueda.

## Metadatos del snapshot

| Metadato | Obligatorio | Regla |
|---|---:|---|
| `sourceUrl` | Sí | URL oficial desde la que se obtuvo la información. |
| `retrievedAt` | Sí | Fecha y hora en que se obtuvo la fuente. |
| `sourceAttribution` | Sí | Atribución a RUCT y al Ministerio responsable. |
| `artifactHash` | Sí | SHA-256 de los bytes exactos del CSV entregado a `PublishInstitutionCatalog` y posteriormente publicado. |
| `sourceDownloadHash` | No | SHA-256 del fichero original descargado de RUCT, únicamente cuando dicho fichero se conserva. |

`artifactHash` identifica el artefacto importado y publicado. `sourceDownloadHash` documenta, de forma opcional, la descarga previa y nunca sustituye a `artifactHash`.

## Controles del MVP

La actualización inicial es manual y aplica únicamente estos controles:

1. registrar `sourceSystem` y `sourceUrl` con la URL oficial utilizada;
2. registrar `retrievedAt` con la fecha y hora de obtención;
3. conservar `sourceAttribution` con el valor `Origen de los datos: Ministerio de Ciencia, Innovación y Universidades — Registro de Universidades, Centros y Títulos (RUCT)`;
4. calcular `artifactHash` sobre los bytes exactos del CSV entregado a `PublishInstitutionCatalog` y verificar que esos mismos bytes sean los publicados;
5. calcular `sourceDownloadHash` sobre el fichero original de RUCT solo cuando dicho fichero se conserve;
6. validar codificación, cabecera, columnas obligatorias, campos vacíos, país y duplicados básicos;
7. omitir filas agregadas o ambiguas;
8. conservar la última versión válida si la descarga o la validación falla;
9. publicar la actualización manual solo después de revisar el resultado.

No se necesita API, SLA, checksum oficial, automatización, resolución jurídica individual de filas ni un historial avanzado para comenzar el MVP.

## Universidad no encontrada

La interfaz debe ofrecer «No encuentro mi universidad». La entrada manual:

- permanece separada del catálogo RUCT;
- conserva el nombre introducido por la persona usuaria;
- queda marcada como pendiente de verificación;
- no crea automáticamente una fila global ni una referencia RUCT;
- no bloquea el flujo por ausencia o desactualización del catálogo.

## Fallo y actualización

- Si RUCT no está disponible, se sigue usando la última versión válida.
- Si cambia el formato, se mantiene la versión anterior hasta adaptar manualmente el importador.
- Si una fila es dudosa, se omite; no se detiene el conjunto completo salvo que falle la validación básica del fichero.
- La frecuencia inicial es manual, según necesidad de producto. La automatización se evaluará cuando el coste operativo lo justifique.

## Transferencia a I1-H02

El contrato de adquisición de C0 se considera satisfecho mediante descarga manual controlada. La generación, revisión y carga del primer snapshot real forman parte de la Definition of Done de `I1-H02`; no son precondición para autorizar su implementación.

La fixture [academic-institutions-es-synthetic-v1.csv](../catalog/fixtures/academic-institutions-es-synthetic-v1.csv) permite desarrollar y probar el importador sin incorporar datos reales al repositorio. Su hash documentado es `artifactHash`, porque ese CSV es el artefacto exacto entregado al importador.

## Decisión aprobada

El Product Owner aprobó expresamente el alcance documentado de `C0_CATALOG` el 2026-09-21 sobre el commit `e98e11655da7a8f37b6c26d180d6b7ca2d3e8e52`. El gate queda **`CLOSED`**. `I1-H02` permanece `BLOCKED` y no pasa a `READY` únicamente porque `S0_PUBLIC_ENDPOINTS` continúa pendiente.

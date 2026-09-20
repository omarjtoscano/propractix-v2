# Esquema del catálogo de instituciones académicas v1

## Estado del contrato

| Campo | Valor |
|---|---|
| `schemaVersion` | `academic-institution-catalog/v1` |
| Estado | Propuesta para decidir `C0_CATALOG`; no autorizada para producción |
| Alcance | Universidades españolas; no centros ni titulaciones |
| Codificación | UTF-8 sin BOM |
| Terminación de línea | LF (`U+000A`) y salto final obligatorio |
| Orden | Cabecera exacta y filas ordenadas ascendentemente por `institutionId` |

Este contrato define el artefacto portable; no implementa `I1-H02`, no cambia OpenAPI y no crea una migración. Las reglas de fuente, aprobación y publicación se documentan en el [dossier de gobierno](../plans/c0-catalog-governance.md).

## Modelo conceptual

El contrato tiene dos niveles para evitar que el hash se contenga a sí mismo.

### Sobre de importación

| Campo | Tipo y cardinalidad | Regla v1 |
|---|---|---|
| `schemaVersion` | string, obligatorio | Valor exacto `academic-institution-catalog/v1`. |
| `sourceId` | string, obligatorio | Identificador controlado de la fuente o fixture. Producción prevista: `RUCT-UNIVERSITIES-ES`. |
| `sourceVersion` | string, obligatorio | Versión oficial si existe. Si no existe, identificador de snapshot ProPractix basado en recuperación UTC y marcado como no oficial. |
| `effectiveAt` | RFC 3339 UTC, obligatorio | Instante desde el que la versión puede considerarse efectiva tras aprobación; termina en `Z`. |
| `sourceLicense` | URI/URN o referencia de permiso, obligatorio | Debe resolver a condiciones aprobadas o a una autorización archivada. No se acepta `unknown`, vacío ni una inferencia basada solo en acceso público. |
| `artifactHash` | string, obligatorio | `sha256:` seguido de 64 dígitos hexadecimales minúsculos, calculados sobre los bytes exactos del CSV. |

### Fila CSV

| Columna | Tipo y cardinalidad | Regla v1 |
|---|---|---|
| `institutionId` | string, obligatorio, único | `<country>:<source>:<source-code>`. Producción: `ES:RUCT:<clave registral>`; fixture: `ES:SYNTH:<code>`. La clave de origen se trata como opaca y conserva ceros iniciales. |
| `officialName` | string, obligatorio | Denominación oficial en NFC, 1–300 puntos de código, sin controles ni espacios exteriores. Conserva grafía, mayúsculas, tildes y lengua de la fuente. |
| `controlledAlternativeNames` | array JSON de strings, obligatorio | Celda que contiene un array JSON UTF-8, incluso cuando está vacío (`[]`). Solo variantes con procedencia oficial; cada valor sigue las reglas de nombre. |
| `countryCode` | string, obligatorio | ISO 3166-1 alfa-2 en mayúsculas. Para este catálogo, valor exacto `ES`. |
| `institutionStatus` | enum, obligatorio | `ACTIVE`, `INACTIVE` o `UNKNOWN`; no se infiere por ausencia de una fila. |

## Cambios justificados respecto del contrato mínimo

Los once campos conceptuales solicitados se conservan sin renombrar. Los seis campos del artefacto no se repiten por fila y `artifactHash` queda fuera del CSV para impedir una autorreferencia criptográfica.

La publicación añade metadatos internos, no columnas CSV:

| Campo interno | Motivo |
|---|---|
| `catalogVersion` | Identifica de forma única cada decisión de publicación, incluidos rollbacks por republicación. |
| `retrievedAt` | Distingue el instante de adquisición de `effectiveAt`. |
| `sourceUrl` | Conserva procedencia directa. |
| `approvedBy` / `approvedAt` | Prueba aprobación expresa. |
| `previousCatalogVersion` | Permite auditoría y concurrencia optimista del puntero activo. |
| `decisionReference` | Enlaza licencia, permiso, informe de validación y motivo. |

No se añaden ciudad, campus, titularidad, URL, CIF, títulos ni datos personales: quedan fuera del alcance de v1.

## Perfil CSV

La primera línea debe ser exactamente:

```text
institutionId,officialName,controlledAlternativeNames,countryCode,institutionStatus
```

Se aplican las reglas de delimitación y escapado de campos de RFC 4180, con la salvedad explícita de que este perfil usa LF en vez de CRLF:

- separador coma U+002C;
- comillas dobles para campos que contengan coma, comillas o salto de línea;
- una comilla dentro de un campo citado se representa como `""`;
- los saltos de línea dentro de campos se prohíben aunque el CSV pudiera representarlos;
- no se admiten filas en blanco, comentarios, BOM ni columnas adicionales;
- el fichero termina con un único LF;
- las filas se ordenan por los bytes UTF-8 de `institutionId` después de validar el identificador.

`controlledAlternativeNames` es JSON embebido en la celda CSV. Por ejemplo, el valor lógico:

```json
["UFE", "Universidad Ficticia de Ejemplo"]
```

se representa así en CSV:

```text
"[""UFE"",""Universidad Ficticia de Ejemplo""]"
```

El orden de alternativas es significativo para reproducibilidad: acrónimo oficial primero si existe y, después, variantes oficiales en el orden documentado por la fuente. No se permiten duplicados tras la normalización canónica.

## Reglas de identificador

La gramática es:

```text
institutionId = country ":" source ":" source-code
country       = 2 letras ASCII mayúsculas
source        = 2..16 caracteres ASCII mayúsculos, dígitos o "-"
source-code   = 1..64 letras ASCII, dígitos, punto, guion o guion bajo
```

Expresión de validación:

```regex
^[A-Z]{2}:[A-Z0-9-]{2,16}:[A-Za-z0-9][A-Za-z0-9._-]{0,63}$
```

Restricciones de este dataset:

- producción: prefijo exacto `ES:RUCT:` y código igual a la clave oficial, sin convertirlo a número;
- fixture: prefijo exacto `ES:SYNTH:`;
- un identificador nunca se reasigna a otra institución;
- una clave cambiada, fusionada o dividida requiere evidencia oficial y revisión manual.

La estabilidad/no reutilización de la clave RUCT aún no está confirmada oficialmente; por eso el esquema es una propuesta y el gate permanece pendiente.

## Normalización de texto

Para `officialName` y cada alternativa:

1. decodificar UTF-8 de forma estricta;
2. normalizar a Unicode NFC;
3. retirar espacios Unicode al principio y al final;
4. colapsar espacios Unicode consecutivos a un espacio U+0020;
5. rechazar controles, tabuladores, saltos de línea, caracteres no asignados y separadores invisibles no admitidos;
6. conservar grafía, mayúsculas, minúsculas, tildes, eñe y puntuación significativa.

La transformación de acentos o mayúsculas solo se usa para una clave auxiliar de detección/búsqueda. No modifica el texto almacenado ni decide identidad.

## Semántica de estado

| Valor | Uso permitido | Presentación ordinaria |
|---|---|---|
| `ACTIVE` | La vigencia está confirmada por la señal oficial acordada en el contrato de fuente. | Seleccionable. |
| `INACTIVE` | Existe evidencia oficial de supresión, revocación o cese. | No seleccionable para nuevas afiliaciones; resoluble para historia. |
| `UNKNOWN` | La fuente no permite decidir sin inferir. | No seleccionable; requiere revisión. |

Una baja aparente entre snapshots no cambia el estado automáticamente. Un cambio de estado debe incluir evidencia y queda visible en el diff de aprobación.

## Validaciones obligatorias

### Del sobre

- campos obligatorios presentes y sin campos con significado contradictorio;
- `schemaVersion` soportado;
- `sourceLicense` en la lista de permisos aprobados y resoluble a evidencia;
- fechas RFC 3339 UTC coherentes;
- hash aportado igual al calculado;
- fuente y namespace de identificadores compatibles.

### Del CSV

- bytes UTF-8 válidos, sin BOM, solo LF y salto final;
- cabecera, número y orden de columnas exactos;
- JSON válido y de tipo array en alternativas;
- campos y enums válidos;
- NFC y normalización de espacios ya canónicos;
- `institutionId` único y ordenado;
- nombres alternativos únicos dentro de la fila;
- ausencia de colisiones entre nombres/alias de instituciones distintas;
- `countryCode=ES` y namespace admitido;
- ausencia de centros, títulos, agregados y datos personales;
- diff respecto de la versión activa dentro de umbrales aprobados o revisado manualmente.

La validación es total: una sola fila inválida impide la publicación completa.

## Cálculo del hash

El proceso canónico es:

1. validar y serializar el CSV según este perfil;
2. conservar exactamente esos bytes como artefacto;
3. calcular SHA-256 sobre todos los bytes, incluido el LF final;
4. codificar el digest en hexadecimal minúsculo;
5. anteponer `sha256:` en `artifactHash`;
6. verificar de nuevo antes de persistir/publicar.

No se recalcula tras cargar filas para encubrir una transformación. El artefacto validado y el auditado deben ser idénticos.

## Fixture sintética de conformidad

La [fixture sintética v1](fixtures/academic-institutions-es-synthetic-v1.csv) no contiene universidades reales, no deriva de RUCT y no es apta para producción. Su sobre de prueba es:

```yaml
schemaVersion: academic-institution-catalog/v1
sourceId: PROPRAX-SYNTHETIC-ES
sourceVersion: synthetic-v1
effectiveAt: 2026-09-20T00:00:00Z
sourceLicense: urn:propractix:source-license:synthetic-fixture-only:v1
artifactHash: sha256:fdc6d8034f317accda939ed04f7f09b51497d4552de0277d1cb36bc817bb1cc4
```

El valor de `sourceLicense` identifica únicamente el carácter sintético y no productivo de esta fixture; no expresa ni presupone licencia sobre RUCT.

## Versionado y compatibilidad

- La cabecera, tipos, semántica, normalización, hash u orden de v1 no cambian silenciosamente.
- Una modificación incompatible crea una nueva versión mayor de esquema y una decisión de arquitectura/gobierno.
- Una nueva fuente o país necesita su propio contrato de procedencia aunque pueda reutilizar el formato.
- Un cambio de licencia o semántica de la fuente bloquea publicación aunque el CSV siga siendo sintácticamente válido.
- El historial conserva el `schemaVersion` original de cada artefacto; no se reescribe.

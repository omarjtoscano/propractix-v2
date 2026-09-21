# Evaluación de C0_CATALOG

## Dictamen

| Elemento | Resultado |
|---|---|
| Gate | `C0_CATALOG` |
| Fecha | 2026-09-21 |
| Recomendación | **`READY_FOR_OWNER_APPROVAL`** |
| Fuente | RUCT |
| Uso | Búsqueda y autocompletado informativo |
| Cierre automático | No |
| `I1-H02` | `BLOCKED` hasta aprobación del Product Owner y resolución de `S0_PUBLIC_ENDPOINTS` |

El alcance está suficientemente definido para solicitar la aprobación del propietario. RUCT no se convierte en una dependencia jurídica ni funcional de la práctica: es una fuente informativa, reemplazable por la última versión válida y complementada por entrada manual.

## Evidencia oficial suficiente

| Evidencia | Conclusión |
|---|---|
| [Página oficial RUCT](https://www.ciencia.gob.es/Universidades/RUCT.html) y [consulta de universidades](https://www.educacion.gob.es/ruct/consultauniversidades?actual=universidades) | Fuente oficial adecuada para denominación y referencia registral. |
| [RD 1509/2008](https://www.boe.es/buscar/act.php?id=BOE-A-2008-15464) | RUCT es público, tiene sección de universidades y asigna claves registrales. |
| [Ley 37/2007](https://www.boe.es/buscar/act.php?id=BOE-A-2007-19814) y [RD 1495/2011](https://www.boe.es/eli/es/rd/2011/10/24/1495/con) | La reutilización general permite trabajar con la información pública bajo atribución y condiciones generales; no se necesita licencia individual para comenzar el MVP. |
| Exportación disponible en la consulta | Suficiente para una descarga manual controlada; no se requiere API, SLA o checksum oficial. |

La ausencia de una ficha específica en datos.gob.es no bloquea el uso de la descarga oficial.

## Alcance aceptado

El catálogo únicamente ayuda a localizar una universidad. No:

- certifica la aprobación de una práctica;
- acredita convenios;
- aporta reglas jurídicas;
- reemplaza la validación de una universidad;
- demuestra afiliación.

Estas limitaciones deben mantenerse en producto y documentación para evitar que una referencia informativa adquiera un significado que no tiene.

## Decisiones de MVP

| Tema | Decisión |
|---|---|
| Identidad | `AcademicInstitutionId` interno UUID v4 según ADR-004. |
| Referencia externa | `sourceSystem=RUCT` y `sourceRecordId=<clave registral>`. |
| CSV | `sourceSystem`, `sourceRecordId`, `officialName`, `countryCode`, `sourceStatus` opcional. |
| Alias | Aplazados hasta demostrar valor para búsqueda. |
| Filas agregadas o ambiguas | Se omiten sin bloquear el catálogo. |
| Universidad ausente | Entrada manual separada y pendiente de verificación. |
| Actualización | Manual inicialmente. |
| Fallo de descarga o formato | Se conserva la última versión válida. |
| Integridad | SHA-256 calculado por ProPractix sobre el archivo descargado. |

## Reevaluación de los bloqueos anteriores

| Bloqueo anterior | Resolución |
|---|---|
| Contrato de adquisición | **Satisfecho** mediante descarga manual controlada, con URL, fecha, atribución, hash y validación básica. |
| Filas agregadas y semántica completa | **No bloqueante**: las filas dudosas se omiten y `sourceStatus` solo se carga cuando es claro. |
| Estabilidad jurídica de la clave RUCT | **No requerida**: la clave es referencia externa, no identidad de dominio. |
| Primer snapshot real | **Transferido a la Definition of Done de `I1-H02`**; no es precondición para autorizar su implementación. |

No quedan incertidumbres materiales dentro del alcance limitado de `C0_CATALOG`.

## Controles conservados

- fuente y URL;
- fecha de descarga;
- atribución a RUCT;
- checksum SHA-256 calculado sobre la descarga;
- validación básica del formato;
- conservación de la última versión válida;
- actualización manual inicial.

No se requieren para el MVP confirmación escrita, licencia individual, API oficial, SLA, checksum oficial ni confirmación sobre la reutilización de la clave registral.

## Entregables

| Entregable | Resultado |
|---|---|
| [Gobierno mínimo](../plans/c0-catalog-governance.md) | Define alcance informativo, controles y fallback manual. |
| [Esquema CSV v1](../catalog/academic-institution-catalog-schema-v1.md) | Contrato mínimo sin identidad de dominio ni aliases. |
| [Fixture sintética](../catalog/fixtures/academic-institutions-es-synthetic-v1.csv) | Conforme, ficticia y no productiva. |
| [Índice documental](../README.md) | Enlaza los documentos y muestra el estado del gate. |

## Conclusión

Se recomienda que el Product Owner apruebe expresamente `C0_CATALOG`. Hasta esa decisión el gate permanece **`READY_FOR_OWNER_APPROVAL`**, no cerrado. `I1-H02` continúa `BLOCKED` y no pasa a `READY` hasta que el propietario apruebe C0 y se resuelva `S0_PUBLIC_ENDPOINTS`. No se implementa código ni se realizan operaciones AWS.

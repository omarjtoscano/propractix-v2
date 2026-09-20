# Evaluación de C0_CATALOG

## Dictamen

| Elemento | Resultado |
|---|---|
| Gate evaluado | `C0_CATALOG` |
| Fecha | 2026-09-20 |
| Recomendación | **Mantener `PENDING`** |
| Fuente primaria candidata | RUCT, sección Universidades |
| Reutilización de RUCT | Habilitada por el régimen general; publicación productiva todavía no aprobada |
| `I1-H02` | `BLOCKED`; no `READY` |
| `S0_PUBLIC_ENDPOINTS` | `PENDING`; no evaluado ni cerrado |
| Operaciones AWS | Ninguna |

La evidencia confirma la autoridad y el alcance nacional de RUCT y permite reutilizar sus datos bajo el régimen general de la Ley 37/2007 y el RD 1495/2011. Persisten incertidumbres materiales de adquisición y semántica, por lo que todavía no procede recomendar el cierre ni aprobar una versión productiva.

## Evidencia revisada

### Estado del repositorio

- La rama base `increment/01-company-identity-catalog` existe.
- [I1-H01](aceptacion-i1-h01.md) está documentada como `DONE / ACCEPTED`.
- El [plan del Incremento 1](../plans/increment-01-plan.md) mantiene `C0_CATALOG` y `S0_PUBLIC_ENDPOINTS` como gates previos y `I1-H02` bloqueada.
- Los ADR y blueprints aplicables permiten el modelo propuesto de puerto de importación, artefacto versionado, persistencia propiedad del módulo, publicación transaccional, errores estables e internacionalización.
- No se encontraron contradicciones documentales que obliguen a detener esta preparación.

### Fuentes oficiales

| Comprobación | Evidencia | Resultado |
|---|---|---|
| RUCT es registro administrativo público y de revisión continua | [Ministerio — RUCT](https://www.ciencia.gob.es/Universidades/RUCT.html), [procedimiento](https://universidades.sede.gob.es/pagina/index/directorio/Proc_Ruct) y [RD 1509/2008](https://www.boe.es/buscar/act.php?id=BOE-A-2008-15464) | Satisfecha. |
| Existe sección diferenciada de universidades | [Consulta RUCT](https://www.educacion.gob.es/ruct/consultauniversidades?actual=universidades) y arts. 2 y 9 del RD 1509/2008 | Satisfecha. |
| Existe identificador oficial | Art. 9 del [RD 1509/2008](https://www.boe.es/buscar/act.php?id=BOE-A-2008-15464) prevé `clave registral` | Parcial: falta confirmar estabilidad/no reutilización. |
| Hay distribución máquina a máquina gobernada | La UI ofrece exportación «Excel», sin API, esquema, versión o checksum documentados | No satisfecha. |
| La reutilización permite el uso previsto | [Ley 37/2007](https://www.boe.es/buscar/act.php?id=BOE-A-2007-19814), [RD 1495/2011](https://www.boe.es/eli/es/rd/2011/10/24/1495/con), [aviso del portal](https://www.ciencia.gob.es/InfoGeneralPortal/AvisoLegal.html) y [aviso de sede](https://universidades.sede.gob.es/pagina/index/directorio/avisos_legales) | Satisfecha por la modalidad general: uso comercial/no comercial, copia, transformación, extracción y redistribución con atribución y condiciones generales. No se localizó decisión motivada que imponga una modalidad específica a RUCT. |
| RUCT está publicado como dataset en datos.gob.es | Búsqueda del catálogo y [API de datos.gob.es](https://datos.gob.es/es/apidata), realizada el 2026-09-20 | No: no se localizaron datasets ni servicios con «RUCT» o el nombre completo. |
| Existe alternativa oficial equivalente | [QEDU](https://www.ciencia.gob.es/Universidades/QEDU.html) combina RUCT/SIIU y es orientativa | No: sirve solo como contraste, no como fuente equivalente. |

El [dossier de gobierno](../plans/c0-catalog-governance.md) registra propietario, mantenedor, alcance, formatos, identificadores, actualización, reutilización, atribución, límites, tratamiento histórico y riesgos para cada fuente.

## Decisiones preparadas

| Decisión | Propuesta |
|---|---|
| Fuente primaria | RUCT, sección Universidades, bajo la modalidad general del RD 1495/2011. |
| Contingencia | Última versión aprobada; como alternativa de adquisición, extracto oficial del Ministerio con procedencia y condiciones que lo acompañen. |
| Alcance | Solo universidades españolas confirmadas; se excluyen centros, títulos, agregados y entidades extranjeras. |
| Identidad | `ES:RUCT:<clave registral>`, con código opaco y ceros preservados. |
| Nombres | Denominación oficial; aliases solo de evidencia oficial. |
| Estado | `ACTIVE`, `INACTIVE`, `UNKNOWN`, sin inferir bajas por ausencia. |
| Normalización | UTF-8/NFC, espacios canónicos y grafía oficial conservada; clave auxiliar tolerante para detectar candidatos. |
| Duplicados | ID duplicado rechaza; colisión de nombre/alias bloquea y exige revisión. |
| Artefacto | [CSV v1](../catalog/academic-institution-catalog-schema-v1.md) versionado con sobre de metadatos. |
| Integridad | SHA-256 de bytes canónicos, antes de persistir y publicar. |
| Publicación | Validación total, versión inmutable y cambio atómico del puntero activo. |
| Rollback | Republicación auditada como versión nueva; nunca reescritura del historial. |
| Fallo de fuente | Servir última versión buena, degradar frescura, alertar y bloquear sustitución automática. |
| No encontrada | Opción explícita separada; no altera el catálogo ni infiere afiliación. |
| Aprobación futura | Data Steward prepara; Cumplimiento verifica el régimen; Arquitectura valida; Product Owner aprueba expresamente. |

## Riesgos y controles

| Riesgo | Severidad | Control propuesto | Situación |
|---|---|---|---|
| Incumplir atribución o una condición específica futura | Media | Referencia normativa en `sourceLicense`, atribución generada y comprobación de cambios antes de cada versión | Mitigado por diseño. |
| Centros agregados tratados como universidades | Alta | Discriminador oficial, no heurística de nombre; rechazo por defecto | Abierto. |
| Código reasignado o cambiado | Alta | Confirmación de no reutilización y reglas de fusión/extinción | Abierto. |
| Exportación cambia sin aviso | Alta | Contrato de adquisición, validación de esquema y bloqueo ante drift | Abierto. |
| Fuente caída genera catálogo vacío | Alta | Última versión buena y publicación atómica | Diseñado; no implementado. |
| Alias crea afiliación ambigua | Media | Procedencia oficial, detección global de colisiones y revisión | Diseñado; no implementado. |
| Rollback elimina trazabilidad | Media | Republicación append-only con motivo y enlace a versión anterior | Diseñado; no implementado. |

## Bloqueos exactos del gate

El propietario no debe cerrar `C0_CATALOG` hasta disponer de:

1. **G1 — contrato de adquisición:** canal oficial operativo, formato y mapeo documentados, versión o protocolo de snapshot, frecuencia de comprobación y tratamiento de indisponibilidad/cambios;
2. **G2 — semántica:** forma verificable de distinguir universidades de contenedores agregados, estados y transiciones, y tratamiento de la clave en cambios, fusiones y extinciones;
3. **G3 — evidencia de primera versión:** artefacto obtenido bajo el régimen general, procedencia, atribución, hash, informe de validación/diff y aprobación expresa de los responsables.

Son bloqueos acumulativos. La base de reutilización se considera resuelta y no exige una licencia RUCT individual; un artefacto reutilizable pero semánticamente ambiguo tampoco cierra el gate.

## Revisión de entregables

| Entregable | Evaluación |
|---|---|
| [Dossier de gobierno](../plans/c0-catalog-governance.md) | Cubre fuentes, decisiones, operación, auditoría, incertidumbres y responsables. |
| [Esquema CSV v1](../catalog/academic-institution-catalog-schema-v1.md) | Conserva todos los campos conceptuales y justifica metadatos adicionales. |
| [Fixture sintética](../catalog/fixtures/academic-institutions-es-synthetic-v1.csv) | Pequeña, UTF-8, conforme al esquema, inequívocamente no productiva y sin universidades reales. |
| Índice documental | Enlaza los tres documentos y esta evaluación. |

## Criterio para una reevaluación

Una futura revisión podrá recomendar `C0_CATALOG = READY_FOR_OWNER_APPROVAL` solo cuando G1–G3 estén documentados y las validaciones sean reproducibles. El cierre seguirá requiriendo aprobación expresa del Product Owner; no será una consecuencia automática de este dossier.

## Conclusión

**`C0_CATALOG` permanece `PENDING`.** RUCT es el candidato oficial adecuado y su reutilización general se considera habilitada, pero el contrato de adquisición, la semántica y la primera versión aprobada siguen pendientes. No se implementa `I1-H02`, no se cierra `S0_PUBLIC_ENDPOINTS`, no se despliega infraestructura y no se realizan operaciones AWS.

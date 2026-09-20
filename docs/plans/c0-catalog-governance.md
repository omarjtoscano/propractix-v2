# Dossier de gobierno para C0_CATALOG

## Estado y propósito

| Campo | Valor |
|---|---|
| Gate | `C0_CATALOG` |
| Fecha de evaluación | 2026-09-20 |
| Estado recomendado | `PENDING` |
| Alcance | Catálogo inicial de universidades españolas; excluye centros y titulaciones |
| Fuente primaria candidata | Registro de Universidades, Centros y Títulos (`RUCT`), sección Universidades |
| Implementación autorizada | Ninguna; este dossier no habilita `I1-H02` |

Este documento prepara la decisión del gate; no la sustituye. La evidencia confirma que `RUCT` es el registro administrativo oficial y la mejor fuente primaria candidata, pero no permite recomendar el cierre: faltan condiciones de reutilización específicas e inequívocas para la exportación, un contrato de adquisición estable y aclaraciones sobre la semántica de algunos registros e identificadores.

## Precondiciones y consistencia documental

- La rama base `increment/01-company-identity-catalog` existe.
- `I1-H01` consta como `DONE / ACCEPTED` en la [aceptación de I1-H01](../reviews/aceptacion-i1-h01.md) y en el [plan del Incremento 1](increment-01-plan.md).
- `I1-H02` sigue `BLOCKED`; no se cambia a `READY`.
- `S0_PUBLIC_ENDPOINTS` sigue `PENDING` y queda fuera de esta decisión.
- Se revisaron `AGENTS.md`, el índice documental, el plan del incremento, los blueprints y los ADR aplicables a monolito modular, arquitectura hexagonal, identificadores y concurrencia, persistencia, eventos, API pública, errores e internacionalización, y entorno local/cloud.
- No se identificaron contradicciones entre esas decisiones y el contrato conceptual propuesto. La separación entre artefacto de catálogo, aplicación y persistencia respeta los puertos/adaptadores y la propiedad de tablas; la publicación inmutable evita convertir la importación en una escritura parcial observable.

## Método y límite de la investigación

Se consultaron únicamente fuentes oficiales y primarias. La consulta se realizó el **2026-09-20**. No se usaron blogs, agregadores ni repositorios de terceros como evidencia. La evaluación jurídica es una decisión de gobierno de producto y no asesoramiento legal; cualquier autorización de reutilización debe quedar archivada por el responsable competente.

## Inventario de fuentes oficiales

| Fuente | Organismo responsable; propietario y mantenedor | URL directa | Alcance y formato disponible | Identificadores y actualización | Reutilización, atribución y límites | Inactivos/modificados y riesgos |
|---|---|---|---|---|---|---|
| RUCT: registro y consulta de universidades | Administración General del Estado. Gestión del registro atribuida al órgano competente en universidades; la página vigente identifica a la Secretaría General de Universidades. Universidades, administraciones educativas y órganos competentes responden por la información que comunican, conforme al art. 18 del RD 1509/2008. | [Página institucional](https://www.ciencia.gob.es/Universidades/RUCT.html), [procedimiento de sede](https://universidades.sede.gob.es/pagina/index/directorio/Proc_Ruct), [consulta de universidades](https://www.educacion.gob.es/ruct/consultauniversidades?actual=universidades) y [RD 1509/2008](https://www.boe.es/buscar/act.php?id=BOE-A-2008-15464) | Registro administrativo público de universidades, centros y títulos, con secciones separadas. La consulta ofrece HTML y una exportación etiquetada como «Excel»; no publica un API ni una especificación de fichero. | El art. 9 prevé una `clave registral` de identificación. La página declara revisión continua, pero no una cadencia, SLA, versión de entrega ni checksum oficiales. | No aparece una licencia específica junto a la exportación. El acceso público no basta por sí solo para acreditar copia, transformación, almacenamiento y redistribución comercial. La atribución exacta y la obligación de fecha de actualización requieren confirmación. | El RD regula comunicación de creación, reconocimiento, supresión, revocación y modificación. La consulta no expone un estado de institución inequívoco. La sección observada devuelve 109 filas e incluye filas agregadas de centros que no son universidades. Riesgos: disponibilidad web, exportación no contractual, cambio de columnas, ausencia de versión y ambigüedad semántica. |
| Ministerio competente: página RUCT, FAQ y avisos legales | Ministerio de Ciencia, Innovación y Universidades; mantenimiento institucional del portal y la sede. | [RUCT](https://www.ciencia.gob.es/Universidades/RUCT.html), [FAQ oficial](https://www.ciencia.gob.es/dam/jcr:73c24c62-bef3-41f9-a965-cb59f5349eab/RUCT_PreguntasFrecuentes_acc.pdf), [aviso del portal](https://www.ciencia.gob.es/InfoGeneralPortal/AvisoLegal.html) y [aviso de la sede](https://universidades.sede.gob.es/pagina/index/directorio/avisos_legales) | Información institucional y ayuda de consulta; no es una distribución versionada del catálogo. HTML y PDF. | Remite a códigos y fichas RUCT. No aporta política de no reutilización de códigos, release ID ni frecuencia contractual. | El aviso del portal contiene una prohibición general de reproducción sin cita o autorización «en su caso» y una autorización expresa separada para QEDU. El aviso de sede limita reproducción, distribución y transformación salvo autorización y permite descarga/uso privado en determinadas condiciones. No queda claro cuál rige la exportación RUCT. | Puede cambiar sin versionado de datos. La diferencia de redacción entre avisos y el régimen general de reutilización es una incertidumbre material que no debe resolverse por inferencia. |
| datos.gob.es: Catálogo Nacional y API de metadatos | Ministerio para la Transformación Digital y de la Función Pública; iniciativa mantenida a través de la entidad pública Red.es. | [Catálogo](https://datos.gob.es/es/catalogo/conjuntos-datos), [API](https://datos.gob.es/es/apidata), [función del portal](https://datos.gob.es/es/que-hacemos) y [aviso legal](https://datos.gob.es/es/aviso-legal) | Catálogo nacional de metadatos, no fuente material del RUCT. API de catálogo en JSON, XML, RDF, Turtle y CSV. La búsqueda de «RUCT» y del nombre completo no devolvió conjuntos ni servicios de datos el 2026-09-20. | Identifica datasets catalogados mediante metadatos; al no existir una ficha RUCT localizable, no proporciona identificador, versión ni distribución para este catálogo. | Su aviso permite, para documentos sometidos a esas condiciones, copia, difusión, modificación, adaptación, extracción, reordenación y combinación, con cita, última actualización, no insinuar respaldo y conservar metadatos. Esas condiciones no licencian un RUCT ausente del catálogo. | Dependencia de cosechado y metadatos del publicador. Una futura ficha podría servir para descubrir una distribución oficial, pero habría que evaluar su licencia y procedencia entonces. |
| Ley 37/2007 sobre reutilización de la información del sector público | Estado; publicación oficial en el BOE. | [Texto consolidado](https://www.boe.es/buscar/act.php?id=BOE-A-2007-19814) | Marco legal, no dataset. HTML y formatos oficiales del BOE. | No define identificadores ni cadencia de RUCT. | Prevé reutilización sin condiciones, mediante licencia tipo o previa solicitud; admite condiciones como no alterar el sentido, citar la fuente e indicar la última actualización. También prevé un procedimiento de solicitud. No convierte automáticamente todo contenido públicamente visible en una distribución libre de condiciones. | Cambios normativos y necesidad de determinar el régimen concreto del documento. El incumplimiento puede ser sancionable. |
| Real Decreto 1495/2011 | Estado; publicación oficial en el BOE. | [Texto consolidado](https://www.boe.es/eli/es/rd/2011/10/24/1495/con) | Desarrollo del régimen de reutilización estatal, no dataset. HTML y formatos oficiales del BOE. | Exige favorecer identificación, formatos y actualización de documentos reutilizables; no versiona RUCT. | El régimen general permite reutilización comercial y no comercial de documentos sujetos a él, con las condiciones generales de no desnaturalizar, citar fuente, indicar actualización, no insinuar patrocinio y conservar metadatos. También admite modalidad específica o solicitud previa. | No identifica expresamente la exportación RUCT ni resuelve por sí solo los avisos específicos observados. No garantiza continuidad del servicio. |
| QEDU | Ministerio de Ciencia, Innovación y Universidades. | [Página oficial QEDU](https://www.ciencia.gob.es/Universidades/QEDU.html) | Herramienta informativa que combina datos de RUCT y SIIU para orientar sobre estudios; no es el registro de universidades ni una entrega íntegra del catálogo. | Hereda datos de varias fuentes; no ofrece el identificador contractual requerido para el catálogo institucional. | El aviso del portal sí contiene condiciones expresas para reutilizar información QEDU, pero esa autorización es específica de QEDU y no debe trasladarse a RUCT. | Puede servir como contraste humano, no como fuente primaria ni contingencia automática. Su mezcla de fuentes y finalidad orientativa añaden riesgo de divergencia. |

## Resultado de la evaluación de fuentes

### Fuente primaria propuesta

La fuente primaria propuesta es **RUCT, sección Universidades**, por su naturaleza de registro administrativo público, su cobertura nacional y la `clave registral` prevista por el RD 1509/2008.

La propuesta queda condicionada a cerrar los huecos G1–G3 de este dossier. No se autoriza todavía descargar y redistribuir su conjunto, ni publicar un artefacto productivo.

### Fuente alternativa y contingencia

No se ha identificado una segunda fuente oficial con igual autoridad, cobertura e identificadores. La alternativa admisible es una **entrega oficial del Ministerio** —distribución publicada o extracto suministrado— con procedencia, esquema, versión y condiciones de reutilización expresas.

Ante indisponibilidad de RUCT:

1. se mantiene publicada la última versión aprobada e inmutable;
2. se registra degradación de frescura y se alerta al responsable;
3. se reintenta la adquisición sin vaciar ni sustituir el catálogo;
4. no se cambia automáticamente a QEDU, datos.gob.es ni una fuente no oficial;
5. si el Ministerio entrega un extracto autorizado, se valida como una versión nueva con la misma gobernanza.

datos.gob.es queda como mecanismo de descubrimiento de una futura distribución oficial, no como fuente actual. QEDU puede emplearse únicamente para contraste manual.

## Alcance y reglas de selección

### Incluido

- Instituciones que el órgano competente identifique positivamente como universidades españolas dentro de la sección Universidades de RUCT.
- Universidades públicas o privadas, sin distinción por modelo de titularidad, siempre que satisfagan la regla anterior.
- Estados históricos únicamente en versiones inmutables; la selección pública activa muestra por defecto instituciones `ACTIVE`.

### Excluido

- Centros universitarios, facultades, escuelas, institutos, campus y unidades dependientes.
- Títulos, planes de estudio y oferta académica.
- Filas agregadas o contenedores de centros, aunque aparezcan en la consulta de universidades.
- Instituciones extranjeras o agrupaciones de centros extranjeros.
- Cualquier registro cuya condición de universidad no pueda demostrarse con un atributo o confirmación oficial.
- Altas sugeridas por usuarios y fuentes no oficiales.

No se admitirán heurísticas basadas solo en el nombre. La presencia de «Universidad» tampoco prueba por sí sola la inclusión.

## Identidad, nombres y estado

### Identificador estable

`institutionId` se construirá como `ES:RUCT:<source-code>`, preservando la `clave registral` como cadena opaca, incluidos ceros iniciales. El prefijo evita colisiones con otros países y fuentes. No se reciclará un identificador internamente.

Esta regla necesita confirmación oficial de que la clave RUCT no se reasigna y de cómo se comporta en fusiones, escisiones o cambios de naturaleza. Hasta entonces no puede cerrarse el gate.

### Nombre oficial y alternativas controladas

- `officialName` reproduce la denominación oficial de la versión fuente, sin traducirla ni corregirla editorialmente.
- `controlledAlternativeNames` solo acepta denominaciones anteriores, acrónimos o variantes lingüísticas publicados por una fuente oficial y con procedencia trazable.
- Un nombre introducido por un usuario, un nombre comercial no oficial o una variante generada por normalización no se incorpora como alias.
- Un cambio de nombre con el mismo identificador crea una nueva versión. El nombre anterior solo pasa a alternativa si la evidencia oficial permite conservarlo así.

### Estado

Valores v1: `ACTIVE`, `INACTIVE` y `UNKNOWN`.

- `ACTIVE`: existencia vigente confirmada por señal oficial acordada.
- `INACTIVE`: supresión, revocación o cese confirmado oficialmente.
- `UNKNOWN`: la fuente no permite determinar el estado sin inferencias.

La desaparición de una fila no equivale a `INACTIVE`: bloquea la publicación y abre revisión. Las instituciones inactivas se conservan en el historial y pueden resolverse para referencias previas, pero no aparecen en la selección ordinaria de nuevas afiliaciones.

## Normalización y control de ambigüedad

La representación canónica aplica:

1. UTF-8 sin BOM y normalización Unicode NFC;
2. eliminación de espacios exteriores;
3. sustitución de secuencias de espacios Unicode por un único espacio U+0020;
4. conservación de mayúsculas, minúsculas, tildes, eñes y signos del nombre oficial;
5. rechazo de controles, saltos de línea, tabuladores y espacios invisibles no admitidos.

Para búsqueda y detección de candidatos duplicados se genera una clave no persistida como nombre: Unicode NFKD, eliminación de marcas combinantes, `casefold`, normalización de espacios y puntuación. Esa clave puede confluir nombres distintos —por ejemplo, al retirar tildes— y **nunca decide identidad por sí sola**.

Reglas de validación:

- `institutionId` duplicado: rechazo.
- Mismo nombre oficial normalizado con identificadores distintos: revisión manual y bloqueo de publicación.
- Alias que coincide con el nombre o alias de otra institución: rechazo del alias o resolución documentada antes de publicar.
- Cambio de identificador para lo que parece la misma institución: revisión de fusión/escisión; nunca se une automáticamente.
- Homónimos oficialmente confirmados: deben conservar identificadores distintos y requieren una presentación que permita desambiguarlos antes de habilitar el endpoint público.

## Contrato del artefacto

El detalle normativo está en el [esquema CSV v1](../catalog/academic-institution-catalog-schema-v1.md). El contrato conceptual mantiene los campos solicitados y separa correctamente metadatos de artefacto y filas:

| Nivel | Campos obligatorios |
|---|---|
| Sobre de importación | `schemaVersion`, `sourceId`, `sourceVersion`, `effectiveAt`, `sourceLicense`, `artifactHash` |
| Fila CSV | `institutionId`, `officialName`, `controlledAlternativeNames`, `countryCode`, `institutionStatus` |

`artifactHash` no se incluye dentro del propio CSV porque produciría una autorreferencia imposible; se calcula sobre sus bytes exactos y se aporta en el sobre. Se añaden como metadatos internos `retrievedAt`, `sourceUrl`, `catalogVersion`, `approvedBy`, `approvedAt`, `previousCatalogVersion` y `decisionReference`. No alteran los once campos conceptuales mínimos: permiten procedencia, auditoría y republicación.

Cuando la fuente no publique una versión, `sourceVersion` será un identificador de snapshot controlado con instante UTC, marcado como versión ProPractix y no como release oficial. `catalogVersion` identifica cada decisión de publicación, incluida una republicación de rollback.

## Flujo gobernado de adquisición y publicación

### Adquisición

1. Recuperar solo desde la URL oficial o canal oficial autorizado.
2. Capturar `retrievedAt`, URL final, cabeceras relevantes y bytes originales en un área no pública de evidencia.
3. Asociar licencia o autorización archivada mediante `sourceLicense` y `decisionReference`.
4. Transformar el origen al CSV canónico sin inventar estados, nombres o identificadores.

### Validación previa

La versión candidata se rechaza completa si falla cualquiera de estos controles:

- codificación UTF-8, NFC, LF, cabecera y orden de columnas;
- esquema y presencia de todos los metadatos;
- identificadores y códigos de país válidos;
- enum de estado y reglas de nombres/alias;
- unicidad, ambigüedades y cambios anómalos respecto de la versión activa;
- clasificación positiva como universidad y exclusión de centros/títulos/agregados;
- licencia/autorización presente en la lista aprobada;
- ausencia de datos personales y de campos fuera de alcance;
- umbrales de variación de recuento, altas, bajas y cambios, con revisión humana cuando se superen.

El informe de validación queda asociado a la versión, incluso si se rechaza.

### Hash SHA-256

El importador calcula SHA-256 sobre los bytes exactos del CSV canónico, incluidos cabecera, separadores LF y salto final. El resultado son 64 caracteres hexadecimales en minúsculas con prefijo lógico `sha256:` en el sobre. El valor aportado debe coincidir en tiempo constante con el calculado antes de persistir.

### Publicación atómica e historial

1. Se persisten en una transacción una importación inmutable y todas sus filas validadas.
2. La misma transacción mueve el puntero de versión activa desde `previousCatalogVersion` a `catalogVersion` mediante control de concurrencia optimista.
3. Los lectores resuelven siempre una única versión publicada; nunca observan carga parcial.
4. Las importaciones, informes, aprobaciones y transiciones son append-only. Una versión publicada no se edita ni se borra por el flujo ordinario.
5. El evento de cambio se registra conforme al patrón outbox cuando la versión activa cambia; este dossier no implementa ese comportamiento.

### Auditoría

Cada intento conserva: actor, instante, fuente y URL, versiones de fuente/esquema/catálogo, hash, licencia o autorización, recuentos, diferencias, resultado de validación, aprobadores, versión anterior, motivo, identificador de correlación y resultado de publicación.

### Actualización y fallo

- Frecuencia propuesta: comprobación mensual y comprobación extraordinaria ante aviso oficial; no es una afirmación sobre la cadencia RUCT.
- Una descarga idéntica por hash se registra como comprobación, no como nueva publicación.
- Un cambio de esquema, licencia, identificadores o semántica bloquea automáticamente la publicación.
- Ante fallo de fuente se conserva la última versión buena, se marca la frescura como degradada, se alerta y se reintenta. Nunca se publica un catálogo vacío.
- Si se supera el umbral operativo de antigüedad que se apruebe, se escala al propietario; la lectura puede continuar con indicación interna de degradación.

### Rollback por republicación

No se mueve el puntero hacia atrás ni se modifica historia. Se selecciona un artefacto previamente aprobado, se vuelve a validar y se publica con un `catalogVersion` nuevo, el mismo `artifactHash` si los bytes son idénticos, referencia a la versión revertida y motivo obligatorio. La decisión queda auditada como una nueva publicación.

## Comportamiento «universidad no encontrada»

- La respuesta funcional mantiene la opción «No encuentro mi universidad» prevista en el plan.
- No crea una institución en el catálogo ni convierte el texto libre en alias.
- Una propuesta se registra, si una historia futura lo autoriza, separada del catálogo gobernado y sin inferir afiliación.
- Solo una versión posterior aprobada puede incorporar una institución, tras evidencia oficial.
- Este comportamiento no autoriza `I1-H02` ni cambia contratos de API.

## Responsabilidad de versiones futuras

| Rol | Responsabilidad |
|---|---|
| Data Steward del catálogo | Adquirir, transformar, validar, preparar diferencias y conservar evidencia. |
| Responsable jurídico/licencias | Confirmar que `sourceLicense` autoriza los usos previstos y fijar la atribución. |
| Arquitectura | Verificar compatibilidad de esquema, persistencia, publicación atómica y consumidores. |
| Product Owner | Aprobar expresamente cada versión productiva y el cierre del gate. |

La publicación requiere evidencia del Data Steward, conformidad de licencias y aprobación expresa del Product Owner. Arquitectura puede bloquear por incompatibilidad técnica. Ninguna actualización automática omite estas responsabilidades mientras el proceso no sea objeto de una decisión posterior.

## Incertidumbres materiales y condiciones de salida

`C0_CATALOG` permanece `PENDING` hasta cerrar, con evidencia enlazable, todos los puntos siguientes:

- **G1 — Reutilización:** confirmación escrita del Ministerio o licencia específica publicada que autorice copiar, transformar, almacenar y redistribuir la exportación RUCT para el uso previsto, y determine atribución, fecha de actualización y demás condiciones.
- **G2 — Adquisición:** canal oficial estable o entrega oficial, formato y esquema documentados, regla de versionado/snapshot, cadencia o mecanismo de aviso y tratamiento de indisponibilidad/cambios.
- **G3 — Semántica:** regla oficial para separar universidades de filas agregadas, significado y transiciones de estado, y confirmación de persistencia/no reutilización de la `clave registral`, incluidos cambios de nombre, fusiones y extinciones.
- **G4 — Primera versión:** extracción productiva realizada bajo G1–G3, validación completa, hash y procedencia conservados, revisión de diferencias y aprobación expresa de Data Steward, licencias y Product Owner.

Si una respuesta oficial niega la reutilización o no permite cumplir el contrato, deberá abrirse una nueva decisión de fuente; no se sustituirá silenciosamente por datos de terceros.

## Decisión recomendada

**Mantener `C0_CATALOG` en `PENDING`.** La evidencia basta para seleccionar RUCT como candidato primario y definir la gobernanza, pero no para distribuir un catálogo productivo ni cerrar el gate. `I1-H02` continúa `BLOCKED`, no `READY`; `S0_PUBLIC_ENDPOINTS` continúa abierto; no se autoriza ninguna operación AWS.

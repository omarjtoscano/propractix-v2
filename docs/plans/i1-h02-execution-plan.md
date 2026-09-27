# Plan de ejecución — I1-H02 Catálogo institucional consultable

- Estado del plan: `APPROVED`
- Aprobación: Product Owner, 2026-09-27
- Commit aprobado: `5f64aeb9e4ef4fd1c0deae845b0607407fe9133b`
- Autorización de ejecución: después del merge de la PR documental, únicamente `E1 — Dominio e invariantes`
- Historia: `I1-H02`
- Estado de la historia: `READY`; no iniciada ni implementada por este documento
- Rama prevista: `feature/i1-h02-academic-institution-catalog`
- Contexto propietario: `academicinstitution`
- Endpoint autorizado: `GET /api/v1/academic-institutions`
- `operationId`: `searchAcademicInstitutions`
- Gates de entrada: H01 `DONE / ACCEPTED`; `F0_CLIENT_BASELINE`,
  `C0_CATALOG` y `S0_PUBLIC_ENDPOINTS` `CLOSED`

Este documento concreta la ejecución técnica de H02. No reabre C0 ni S0, no
implementa la historia, no modifica `CL0_CLOUD_STAGING` y no autoriza ningún
endpoint adicional.

## 1. Fuentes y decisiones aplicables

El plan aplica, por orden de autoridad y especialización:

1. el plan del Incremento 1, revisión 7;
2. el gobierno, esquema y evaluación aprobados de `C0_CATALOG`;
3. la decisión y política aprobadas de `S0_PUBLIC_ENDPOINTS`;
4. ADR-001, ADR-002, ADR-004, ADR-005, ADR-006, ADR-007, ADR-008,
   ADR-009 y ADR-012;
5. la especificación técnica y el blueprint de dominio y mapas legales.

Las menciones históricas de H02 como `BLOCKED` en el dossier de C0 reflejan el
estado anterior al cierre posterior de S0. No contradicen el estado vigente
`READY` registrado en el plan revisión 7 y en la decisión S0 aprobada el
2026-09-27.

No se necesita un ADR nuevo: las decisiones estructurales, de identidad,
persistencia, API, rate limiting y aceptación local ya están cubiertas.

## 2. Objetivo, actor y valor

**Objetivo.** Publicar y consultar un snapshot gobernado de universidades
españolas mediante una búsqueda pública paginada y un componente accesible de
autocompletado.

**Actor.** Visitante o estudiante, sin necesidad de autenticación.

**Valor.** La persona localiza una universidad con denominación consistente y
puede seleccionarla mediante el UUID interno de ProPractix sin que la
institución tenga registro, cuenta, membership, tenant o workspace.

RUCT se utiliza únicamente como fuente informativa. Una aparición en el
catálogo no acredita afiliación, matrícula, elegibilidad, convenio, aprobación
de una práctica, representación ni cumplimiento jurídico.

## 3. Alcance

### 3.1 Incluido

- Modelo de dominio mínimo de `AcademicInstitution` y publicación de catálogo.
- Importación manual gobernada del CSV v1 y trazabilidad del snapshot.
- UUID v4 interno estable e independiente de la referencia RUCT.
- Publicación atómica y conservación de la última versión válida.
- Búsqueda pública normalizada y paginación por cursor.
- Contrato OpenAPI design-first de `searchAcademicInstitutions` y cliente
  TypeScript generado.
- Únicamente `GET /api/v1/academic-institutions` como endpoint funcional.
- Rate limiting S0 en PostgreSQL, resolución segura de IP y fallo cerrado.
- Problem Details, `X-Correlation-ID` y códigos estables.
- Componente/pantalla de autocompletado accesible en español e inglés.
- Opción visible «no encuentro mi universidad» que solo produce una selección
  manual local para el consumidor; H02 no la persiste.
- Observabilidad operativa sin IP, fingerprint, consulta ni datos personales.
- Pruebas de dominio, aplicación, arquitectura, persistencia, contrato, API,
  proxy/rate limiting, frontend y E2E.
- Creación, revisión y publicación local controlada del primer snapshot real
  como evidencia de la Definition of Done.

### 3.2 Fuera de alcance

- Registro, cuenta, autenticación, membership, tenant o workspace de una
  universidad.
- Centros, facultades, escuelas, titulaciones, planes de estudio, aliases,
  acrónimos, direcciones, contactos, dominios institucionales o fiscalidad.
- Inferir `sourceStatus`, vigencia, elegibilidad, convenio o cumplimiento.
- Verificar que una persona pertenece a la universidad seleccionada.
- Persistir la entrada manual «no encontrada»; pertenece a H07.
- Ofertas, candidaturas, prevalidación, formalización, prácticas y documentos.
- Endpoints administrativos HTTP o cualquier endpoint distinto de
  `searchAcademicInstitutions`.
- Automatización periódica de RUCT, API externa, SLA o checksum del Ministerio.
- Redis, broker, servicio de rate limiting externo o recurso AWS.
- Despliegue o mutación AWS, modificación del estado de
  `CL0_CLOUD_STAGING`, exposición productiva pública o datos personales reales.
- Un catálogo canónico de titulaciones.

## 4. Bounded context y modelo de dominio

### 4.1 Propiedad

`academicinstitution` es el único propietario de las instituciones, los
metadatos de importación y la publicación. Es un catálogo global, no
tenant-owned. Ningún otro contexto accede a sus tablas o repositorios.
`student` podrá consumir posteriormente el puerto público
`SearchPublishedInstitutions`, sin importar modelo interno ni persistencia.

`platform.ratelimit` es infraestructura técnica transversal. No contiene
reglas del catálogo y `academicinstitution` no importa ninguna implementación
de rate limiting.

### 4.2 Aggregates y objetos

| Tipo | Nombre | Responsabilidad e invariantes |
|---|---|---|
| Aggregate root | `AcademicInstitution` | Conserva `AcademicInstitutionId`, referencia externa, denominación oficial, país, estado de fuente opcional y snapshot publicado. Solo se crea/actualiza mediante publicación gobernada. |
| Aggregate root | `CatalogImport` | Conserva identidad de importación, versión de esquema, metadatos, hashes, resultado, recuentos y estado `PUBLISHED`, `SUPERSEDED` o `REJECTED`. Solo una importación puede estar publicada. Repetir el artefacto activo no crea otro aggregate; republicar uno `SUPERSEDED` sí crea una importación nueva y trazable. |
| Domain event | `InstitutionCatalogPublished` | Se registra solo cuando cambia el snapshot publicado, incluida la republicación de un artefacto `SUPERSEDED`; repetir el artefacto activo no lo emite. H02 no lo convierte aún en evento de integración ni crea outbox porque no existe consumidor cross-context o efecto externo. |
| Value object | `AcademicInstitutionId` | UUID v4 generado por la aplicación; es la única identidad pública de dominio. |
| Value object | `CatalogImportId` | UUID v4 de cada intento trazable de importación. |
| Value object | `ExternalInstitutionReference` | Par opaco `(sourceSystem, sourceRecordId)`; sirve para procedencia y reconciliación, nunca como identidad de dominio. |
| Value object | `OfficialInstitutionName` | De 1 a 300 puntos de código, Unicode NFC, sin espacios exteriores, controles ni saltos de línea. Conserva grafía y tildes. |
| Value object | `CountryCode` | ISO 3166-1 alfa-2 válido. Los países admitidos por el catálogo proceden de configuración tipada; el dominio no contiene un condicional para `ES`. |
| Value object | `CatalogImportMetadata` | `sourceUrl`, `retrievedAt`, `sourceAttribution`, `artifactHash` y `sourceDownloadHash` opcional. |
| Value object | `CatalogArtifactHash` | SHA-256 hexadecimal del byte stream exacto entregado al puerto de publicación. |
| Value object | `NormalizedInstitutionSearchTerm` | Forma técnica de búsqueda; nunca sustituye ni modifica la denominación oficial. |
| Value object | `InstitutionSearchCursor` | Cursor opaco, versionado y ligado al snapshot, a la consulta normalizada y a la última clave de ordenación. |

No se necesita una entidad hija persistente ni un aggregate que cargue todo el
catálogo en memoria. Cada fila validada se convierte en un candidato de
publicación y se reconcilia con `AcademicInstitution` dentro de una única
transacción del contexto.

Las entidades de persistencia previstas son
`AcademicInstitutionJpaEntity` y `CatalogImportJpaEntity`; pertenecen
exclusivamente al adapter PostgreSQL y se mapean de forma explícita. Los
buckets técnicos se manejan mediante un record JDBC interno de
`platform.ratelimit`, no como entidad de dominio ni de catálogo.

### 4.3 Invariantes

1. El ID interno siempre es UUID v4 y nunca se deriva de RUCT.
2. La misma referencia externa reutiliza el mismo ID interno entre snapshots.
3. Una colisión o cambio dudoso de referencia no reasigna silenciosamente un
   ID: la publicación se rechaza para revisión manual.
4. El CSV importado tiene exactamente el esquema v1 y su hash se calcula antes
   de cualquier transformación posterior.
5. Los metadatos obligatorios están completos y los dos hashes tienen
   significados distintos.
6. Solo una versión publicada es consultable.
7. Una publicación fallida o concurrente no altera la versión anterior.
8. Una institución ausente del snapshot nuevo deja de ser seleccionable, pero
   su ID se conserva para futuras referencias históricas de H07.
9. Publicar nuevamente el artefacto que ya está activo es idempotente: devuelve
   el snapshot vigente sin crear `CatalogImport`, emitir
   `InstitutionCatalogPublished`, cambiar el snapshot ni invalidar cursores.
10. Republicar un artefacto anteriormente `SUPERSEDED` es un nuevo acto
    trazable: crea otro `CatalogImport`, cambia el snapshot, emite
    `InstitutionCatalogPublished` e invalida los cursores anteriores; no es una
    down migration ni reescribe el historial.
11. `sourceStatus` solo se conserva si llega explícitamente; nunca se infiere.
12. El catálogo no produce ninguna decisión legal o de elegibilidad.

## 5. Paquetes DDD y hexagonales

Solo se crearán los paquetes que tengan clases reales:

```text
com.propractix.academicinstitution
├── domain.model
├── domain.event
├── domain.service
├── application.port.in
├── application.port.out
├── application.command
├── application.query
├── application.service
├── adapter.in.rest
├── adapter.in.commandline
├── adapter.out.persistence
├── adapter.out.integration
└── configuration

com.propractix.platform.ratelimit
├── application.port.in
├── application.port.out
├── application.service
├── adapter.in.http
├── adapter.out.persistence
├── adapter.out.crypto
└── configuration
```

Se mantiene `adapter -> application -> domain`. El dominio no importará
Spring, JPA, Jackson, Servlet, Micrometer ni configuración. Las entidades JPA,
repositorios Spring Data/JDBC, mappers REST y mappers de persistencia no saldrán
de sus adapters.

## 6. Puertos, servicios y adapters

### 6.1 Puertos de entrada

- `PublishInstitutionCatalog`: recibe bytes CSV exactos y metadatos; valida,
  reconcilia y publica atómicamente. No es HTTP.
- `SearchPublishedInstitutions`: recibe término normalizado, cursor y tamaño de
  página validados; devuelve una proyección mínima y los metadatos públicos de
  atribución del snapshot.
- `RateLimitAdmission`: decide, antes del caso de uso, si una operación pública
  puede ejecutarse.

### 6.2 Puertos de salida

- `CatalogArtifactParser`: convierte el CSV estricto en candidatos validados
  sin cambiar los bytes usados para `artifactHash`.
- `CatalogPublicationRepository`: reconcilia IDs y publica todo el snapshot en
  una transacción local.
- `PublishedInstitutionQueryRepository`: ejecuta la búsqueda keyset sobre la
  única importación publicada.
- `AcademicInstitutionIdGenerator` y `CatalogImportIdGenerator`: generan UUID
  v4 fuera del modelo.
- `ApplicationClock`: suministra `Instant` controlable; PostgreSQL sigue siendo
  autoritativo dentro de los predicados SQL atómicos.
- `RateLimitBucketStore`: reclama de forma atómica todas las reglas y versiones
  HMAC activas y devuelve admisión o ventanas violadas.
- `ClientIpResolver`: resuelve el sujeto de red según el modo tipado.
- `ClientIpFingerprint`: calcula HMAC-SHA-256 con la versión de clave indicada.
- `RateLimitPolicyProvider`: carga y valida la política aprobada exacta.

### 6.3 Adapters

- Adapter CLI/command-line para la publicación manual. Lee un archivo y su
  manifest, llama a `PublishInstitutionCatalog` y termina con un resultado
  estable; no abre un endpoint administrativo.
- Parser CSV UTF-8 estricto para cabecera, cinco columnas, quoting, LF final,
  NFC, campos y duplicados. Las filas ambiguas se excluyen durante la revisión
  de Datos porque el esquema mínimo no contiene información suficiente para
  inferirlas automáticamente.
- Adapter PostgreSQL del catálogo con JPA para estado/mapeo y SQL explícito
  donde se necesite publicación masiva y locking.
- Adapter REST fino que valida transporte, invoca
  `SearchPublishedInstitutions` y mapea la proyección.
- Filtro HTTP de rate limiting, posterior al filtro de correlación y anterior a
  seguridad/controlador.
- Adapter PostgreSQL de rate limiting con una sola transacción para todas las
  reglas/versiones; la denegación revierte cualquier incremento parcial.
- Adapter criptográfico JCA para HMAC; el material llega desde secretos
  externos y jamás desde el repositorio o la política YAML.
- Adapter Micrometer con labels allowlisted.

## 7. PostgreSQL y migraciones Flyway

Se proponen dos migraciones forward-only, globalmente ordenadas, sin datos de
catálogo embebidos:

1. `V1__create_academic_institution_catalog.sql`.
2. `V2__create_public_endpoint_rate_limit_buckets.sql`.

Si al comenzar la implementación ya existen versiones globales, se asignarán
los siguientes números libres sin renombrar ni editar migraciones aplicadas.

### 7.1 `academicinstitution_catalog_import`

Columnas mínimas:

- `catalog_import_id uuid primary key`;
- `schema_version varchar(...) not null`;
- `source_url text not null`;
- `retrieved_at timestamptz not null`;
- `source_attribution text not null`;
- `artifact_hash char(64) not null`;
- `source_download_hash char(64) null`;
- `status varchar(...) not null` con check tipado;
- `total_row_count`, `published_row_count`, `omitted_row_count` enteros no
  negativos;
- `failure_code varchar(...) null`, sin excepción o contenido de la fila;
- `created_at`, `published_at` y `superseded_at` como `timestamptz`;
- `previous_published_import_id uuid null` como referencia dentro del contexto;
- `version bigint not null` para concurrencia optimista.

Un índice parcial único sobre el estado publicado impide dos snapshots
consultables. `artifact_hash` se indexa, pero no es único: un rollback manual
puede republicar un artefacto `SUPERSEDED` como un acto nuevo y trazable. Una
coincidencia con el artefacto actualmente `PUBLISHED` se resuelve como no-op
idempotente antes de insertar otra importación.

### 7.2 `academicinstitution_institution`

Columnas mínimas:

- `institution_id uuid primary key`;
- `source_system varchar(...) not null`;
- `source_record_id varchar(...) not null`;
- `official_name varchar(300) not null`;
- `search_name_normalized varchar(300) not null`;
- `country_code char(2) not null`;
- `source_status varchar(...) null`;
- `published_catalog_import_id uuid null`;
- `created_at`, `updated_at` como `timestamptz`;
- `version bigint not null`.

Se añade unicidad sobre `(source_system, source_record_id)` y un índice de
búsqueda estable sobre
`(published_catalog_import_id, search_name_normalized, institution_id)`.
No se crea FK hacia otros contextos ni tabla de titulaciones. Una fila retirada
conserva el ID y queda con `published_catalog_import_id = null`.

### 7.3 `platform_rate_limit_bucket`

Columnas mínimas:

- `operation_id`, `policy_version`, `rule_id`;
- `hmac_key_version`;
- `subject_fingerprint` base64url sin padding; nunca IP;
- `window_started_at timestamptz`;
- `request_count integer`;
- `expires_at timestamptz`;
- clave primaria compuesta por operación, política, regla, versión HMAC,
  fingerprint y comienzo de ventana;
- índice sobre `expires_at` para limpieza por lotes.

Una sentencia SQL reclama las combinaciones `burst`/`sustained` por todas las
versiones HMAC activas. PostgreSQL serializa conflictos de la misma clave; si
algún contador excedería el límite, la transacción completa se revierte. La
expiración lógica depende del instante PostgreSQL y no de que el job haya
borrado ya la fila.

### 7.4 Atomicidad de publicación

El adapter obtiene un lock transaccional no bloqueante y específico del
catálogo. Si otro publicador lo posee, el puerto devuelve el resultado estable
`catalog_publication_in_progress` y el adapter CLI termina sin publicar. Tras
adquirir el lock, compara el hash con la importación activa. Si coincide,
devuelve el resultado idempotente con el mismo snapshot y finaliza sin escritura,
sin `CatalogImport` nuevo y sin evento. En otro caso, incluida la coincidencia
con una importación `SUPERSEDED`, ejecuta en una sola transacción:

1. registra el intento válido;
2. reutiliza IDs por referencia externa y genera UUID v4 solo para nuevas
   instituciones;
3. actualiza los atributos del snapshot;
4. despublica ausentes sin borrar su identidad;
5. marca la importación anterior como `SUPERSEDED`;
6. marca la nueva como `PUBLISHED`;
7. registra `InstitutionCatalogPublished` con el nuevo snapshot.

Un error revierte los siete pasos. Una validación fallida puede conservar un
registro `REJECTED` mínimo en una transacción separada, sin filas fuente ni
texto libre sensible, y nunca modifica el snapshot publicado.

## 8. Importación gobernada y snapshot real

### 8.1 Contrato de importación

El adapter acepta únicamente UTF-8 sin BOM, coma como separador, LF, salto
final y la cabecera exacta:

```text
sourceSystem,sourceRecordId,officialName,countryCode,sourceStatus
```

La referencia RUCT se trata como texto opaco y conserva ceros iniciales. El
importador no transforma la clave en UUID, no infiere `sourceStatus` y no
añade aliases. Los países y fuentes admitidos para cada perfil proceden de
configuración tipada y validada; producción no admite la fixture `SYNTHETIC`.

El SHA-256 se calcula sobre los bytes recibidos antes de parsear y debe coincidir
con `artifactHash`. Si existe fichero original conservado, su hash se compara
separadamente con `sourceDownloadHash`.

### 8.2 Obtención del primer snapshot real

Orden exacto:

1. Producto + Datos fija una ventana de adquisición y descarga manualmente la
   exportación ofrecida por la consulta oficial RUCT indicada en C0.
2. El fichero original se guarda temporalmente en
   `.local/catalog-acquisition/<timestamp>/`; no se promueve como fixture ni se
   incluye por accidente en una imagen.
3. Se registran URL oficial, instante UTC y atribución exacta. Si se conserva el
   original, se calcula `sourceDownloadHash`.
4. Datos identifica las columnas reales de clave y denominación, selecciona
   solo universidades inequívocas, omite centros/titulaciones/agregados/filas
   ambiguas y deja `sourceStatus` vacío si no es explícito.
5. Se serializa el CSV canónico sin BOM y con LF final. Después de ese punto no
   se modifica; se calcula `artifactHash` sobre sus bytes exactos.
6. Una segunda revisión compara recuentos, duplicados, una muestra contra RUCT,
   omisiones y hashes. La revisión no atribuye significado legal al contenido.
7. Se versionan el CSV normalizado y un manifest sin PII bajo
   `docs/catalog/snapshots/ruct-<YYYY-MM-DD>/`.
8. Se valida primero con el mismo parser que usará publicación y se ejecuta un
   dry run que informa solo recuentos y códigos.
9. Se publica localmente mediante el adapter CLI. Se verifica import ID, hash,
   recuentos, única versión visible y búsquedas de muestra.
10. Se registra evidencia reproducible en
    `docs/reviews/i1-h02-first-snapshot-evidence.md`.

Si la descarga, el mapeo o la validación falla, no se publica nada y continúa
la última versión válida. Cambiar el formato de origen exige adaptar y revisar
la transformación; nunca relajar el importador en caliente.

## 9. Búsqueda, paginación y normalización

### 9.1 Semántica de búsqueda

- `query` es opcional; vacío devuelve la primera página ordenada.
- Para buscar se aplica: trim Unicode, NFC, minúsculas con `Locale.ROOT`,
  descomposición técnica para retirar diacríticos y colapso de espacios.
- La denominación oficial devuelta permanece intacta y en NFC.
- La búsqueda v1 es una coincidencia `contains` escapada sobre
  `search_name_normalized`; no usa aliases, fuzzy matching ni inferencias.
- El orden total es `search_name_normalized`, `institution_id`.
- La consulta recupera `limit + 1` filas para producir `nextCursor` sin
  ejecutar un count global.

El pequeño volumen inicial no justifica `pg_trgm` ni extensiones. Si la
evidencia posterior muestra problemas de rendimiento o calidad, se propondrá
una decisión separada.

### 9.2 Cursor

El cursor base64url contiene una estructura versionada con:

- ID del snapshot publicado;
- hash de la consulta normalizada;
- última clave `search_name_normalized`;
- último `AcademicInstitutionId`.

Es opaco para clientes y no contiene referencias RUCT. Un cursor malformado o
ligado a otra consulta produce `400 validation_error`. Si el catálogo cambió
entre páginas, devuelve `409 catalog_snapshot_changed`; el cliente reinicia la
búsqueda para no mezclar snapshots.

La paginación aceptada usa `default=20` y `max=50`. Ambos valores viven en
configuración tipada backend/frontend y en restricciones OpenAPI, no como
constantes dispersas.

## 10. Contrato OpenAPI

`docs/api/openapi.yaml` incorporará exactamente:

```text
GET /api/v1/academic-institutions
operationId: searchAcademicInstitutions
query: string opcional
cursor: string opcional
limit: integer opcional
```

Respuesta `200 application/json`:

```text
AcademicInstitutionSearchPage
  items[]
    id: uuid
    officialName: string
    countryCode: string
  nextCursor: string | null
  snapshot
    retrievedAt: date-time
    sourceUrl: uri
    sourceAttribution: string
```

No se exponen `sourceRecordId` ni la clave RUCT como identidad pública. El
`sourceStatus` no se muestra en v1 porque C0 permite que esté vacío o sea opaco
y H02 no tiene un uso de producto aprobado para presentarlo.

Respuestas declaradas:

| HTTP | Código estable | Uso |
|---:|---|---|
| 400 | `validation_error` | Query, cursor o límite inválidos. |
| 409 | `catalog_snapshot_changed` | El cursor pertenece a una publicación anterior. |
| 429 | `rate_limited` | Alguna ventana S0 agotada; incluye `Retry-After`. |
| 503 | `rate_limiter_unavailable` | No puede demostrarse admisión; incluye `Retry-After: 1`. |
| 503 | `academic_institution_catalog_unavailable` | No existe snapshot publicado o falla su lectura sin exponer SQL. |

Todas incluyen `X-Correlation-ID`; los errores usan el schema común
`ProblemDetail`. OpenAPI contiene códigos, no copy localizado.

El cliente se genera con `@hey-api/openapi-ts` 0.99.0 y Fetch. CI ejecuta lint,
generación y `git diff --exit-code` sobre el resultado para impedir drift.

## 11. Rate limiting S0

La implementación consume directamente la política aprobada
`docs/policies/public-endpoint-rate-limits-v1.yaml`; no copia sus números a
Java, TypeScript, OpenAPI o tests de producción.

Para `searchAcademicInstitutions` deben aprobarse simultáneamente:

- `burst`: 15 peticiones por ventana fija de 10 segundos; primera rechazada 16;
- `sustained`: 60 peticiones por ventana fija de 60 segundos; primera
  rechazada 61;
- TTL lógico/persistido: 70 y 120 segundos respectivamente;
- limpieza física idempotente por lotes cada minuto.

La clave lógica incluye política, operación, regla, versión HMAC, fingerprint y
ventana. `query`, `cursor` y `limit` nunca crean una cuota nueva.

### 11.1 Activación y fallo cerrado

La carga tipada verifica `apiVersion`, `kind`, estado aprobado/cerrado,
`approvedCommit`, operación exacta, método, path, versión, ventanas, TTL,
algoritmo, versiones HMAC y ausencia de operaciones autorizadas adicionales.

- Falta de entrada exacta: la ruta no se expone.
- Política o proxy incoherentes, clave activa ausente o HMAC imposible: entorno
  no preparado y petición `503 rate_limiter_unavailable`.
- Error, timeout o rollback PostgreSQL: `503`, nunca bypass.
- Límite superado: `429 rate_limited` y `Retry-After` igual al techo de tiempo
  hasta que se recuperen todas las ventanas violadas, mínimo un segundo.
- El filtro no invoca controlador ni caso de uso en `429` o `503`.

### 11.2 Rotación HMAC

El material se monta mediante config tree en local/test con valor
inequívocamente sintético y mediante secreto externo en otros entornos. La
política solo contiene IDs de versión.

Durante solapamiento se calculan y reclaman todas las `readVersions` en la
misma transacción. La nueva `writeVersion` pertenece siempre al conjunto de
lectura. La versión anterior no se retira hasta TTL máximo de 120 segundos más
60 segundos de margen. No existe backfill de buckets ni reinicio permisivo de
cuota.

## 12. Resolución segura de IP y Caddy

### 12.1 Local directo

Modo `REMOTE_ADDRESS`: se usa la dirección del socket y se ignoran siempre
`Forwarded` y `X-Forwarded-For`, aunque el cliente las envíe.

### 12.2 Detrás de Caddy

Se adopta `X_FORWARDED_FOR` como único modo detrás de Caddy, condicionado a que
el saneamiento y las pruebas de borde siguientes estén aprobados:

1. Caddy elimina tanto `Forwarded` como `X-Forwarded-For` recibidos del cliente.
2. Reconstruye únicamente `X-Forwarded-For` desde `{remote_host}`, el salto de
   red observado.
3. El servidor solo confía en la cabecera si el peer inmediato pertenece a un
   CIDR tipado y validado y está activado el contrato de saneamiento de Caddy.
4. Con varios proxies confiables, recorre de derecha a izquierda y toma el
   primer salto no confiable.
5. Cabeceras de un peer no confiable se ignoran.
6. Cadena ausente, malformada o sin cliente resoluble desde un peer confiable
   falla cerrada.
7. Se rechazan `0.0.0.0/0`, `::/0` y la configuración simultánea de ambas
   familias de cabecera.

Normalización previa al HMAC:

- IPv4 binaria canónica `/32`; sintaxis ambigua, octal, entero o fuera de rango
  se rechaza.
- IPv6 sin zone ID y reducida a `/64`.
- IPv4 mapeada en IPv6 se normaliza como IPv4 `/32`.
- La familia forma parte del sujeto canónico.

No se conservará la IP canónica después de calcular el fingerprint.

## 13. Problem Details y correlación

El `CorrelationIdFilter` existente continúa siendo el primero: acepta un valor
válido o genera uno, lo añade a MDC y responde `X-Correlation-ID`. El filtro de
rate limiting reutiliza exactamente ese valor.

Los Problem Details incluyen `type`, `title`, `status`, `detail`, `instance`,
`code`, `correlationId` y `violations` cuando corresponda. `title` y `detail`
son diagnósticos técnicos no sensibles; la UI no los presenta como copy. Nunca
se devuelve SQL, stack trace, IP, fingerprint, clave HMAC, contenido del cursor
ni datos de una fila descartada.

## 14. Frontend e i18n

Se implementará una feature `academic-institutions` y un componente reusable
de combobox/autocompletado. `App` lo renderizará como la primera pantalla
funcional sin introducir rutas o endpoints ajenos a H02.

Comportamiento:

- input con label, ayuda y descripción del alcance informativo;
- debounce y tamaño de página desde configuración tipada;
- estados inicial, loading, resultados, vacío, error, rate limited,
  indisponible y retry;
- scroll/botón de siguiente página mediante cursor, sin offset;
- teclado, `aria-expanded`, `aria-controls`, `aria-activedescendant`, foco y
  anuncio accesible de resultados;
- selección por `AcademicInstitutionId` y nombre mostrado;
- acción «no encuentro mi universidad» que emite un valor manual al consumidor
  y no escribe servidor;
- atribución RUCT y aviso de que el catálogo no certifica afiliación,
  elegibilidad, convenio ni cumplimiento;
- respeto de `Retry-After`; sin bucle automático agresivo ante 429/503.

Toda etiqueta, ayuda, estado, acción, validación y error utiliza claves de
`es.json` y `en.json` con paridad. `sourceAttribution` y `officialName` son datos
de fuente, no copy incrustado. El cliente mapea `ProblemDetail.code` a i18n y no
muestra directamente `title` o `detail`.

## 15. Observabilidad y privacidad

Métricas exactas de S0:

```text
propractix_rate_limit_allowed_total{operation_id,policy_version}
propractix_rate_limit_rejected_total{operation_id,policy_version,rule_id}
propractix_rate_limit_adapter_errors_total{operation_id,policy_version,error_class}
```

`error_class` solo admite `storage`, `configuration`, `key` o `client_ip`.
También se medirán publicación exitosa/fallida, edad del snapshot y duración de
búsqueda con labels acotadas, sin nombres ni términos buscados.

Prohibido en logs, métricas y trazas:

- IP cruda o normalizada;
- fingerprint o versión de clave HMAC;
- query, cursor, User-Agent o cuerpo CSV;
- referencias RUCT individuales en mensajes de error;
- nombres de institución como labels;
- datos personales.

Los logs operativos pueden contener `correlationId`, release, operation ID,
policy version, rule ID, código estable, recuentos agregados, import ID y hash
del artefacto. El hash identifica un artefacto público gobernado, no un sujeto.

La limpieza ignora lógicamente filas expiradas y las borra en el siguiente
ciclo. Más de 10 000 expiradas o una antigüedad superior a 15 minutos genera
alerta/incidencia sin ampliar retención ni exportar fingerprints.

## 16. Estrategia de pruebas

### 16.1 Dominio y aplicación

- UUID v4 interno y referencia externa independiente.
- Reutilización del ID para una referencia ya conocida.
- Nombre NFC, límites, controles, espacios y país configurado.
- `sourceStatus` ausente y no inferido.
- metadatos obligatorios y separación de hashes;
- duplicado de referencia dentro del artefacto;
- repetición del artefacto activo sin `CatalogImport`, evento, nuevo snapshot
  ni invalidación de cursores;
- republicación de un artefacto `SUPERSEDED` con nueva importación, evento,
  snapshot e invalidación de cursores anteriores;
- publicación válida, inválida y concurrente;
- fallo que conserva la publicación anterior;
- retirada y reaparición que conservan el UUID;
- evento solo al cambiar versión publicada;
- búsqueda vacía/con término y cursor ligado a query/snapshot.

### 16.2 Arquitectura

- Dominio Java puro.
- Adapters dependen hacia dentro.
- JPA confinado a `adapter.out.persistence`.
- Ningún acceso cross-context a tablas/repositorios.
- `academicinstitution` no importa `platform.ratelimit` y `platform` no importa
  el dominio del catálogo.
- Solo el puerto público registrado queda disponible para un futuro consumo de
  `student`.

### 16.3 CSV y persistencia PostgreSQL/Testcontainers

- UTF-8/BOM, CRLF, LF final, cabecera/orden, cinco columnas y quoting;
- nombres con coma/comillas/tildes y `sourceRecordId` con cero inicial;
- campos vacíos, controles, país/fuente no permitidos y duplicados;
- hash calculado sobre bytes exactos y hash de descarga separado;
- migración desde base vacía y `ddl-auto=validate`;
- unicidad de referencia externa y de publicación activa;
- publicación transaccional y rollback por fallo inyectado;
- dos publicadores concurrentes: uno publica y otro recibe conflicto;
- artefacto activo repetido sin inserciones y artefacto `SUPERSEDED` republicado
  con un `catalog_import_id` nuevo;
- keyset estable, `limit + 1`, cursor obsoleto y fila despublicada;
- ausencia de titulaciones, cuentas, tenants o FKs cross-context.

### 16.4 API y contrato

- `200` con orden, cursor, atribución y solo campos públicos;
- validaciones de query/cursor/limit;
- repetir el artefacto activo conserva la validez del cursor;
- republicar un artefacto `SUPERSEDED` hace que el cursor anterior obtenga
  `409 catalog_snapshot_changed`;
- catálogo ausente/fallo como `503` sin SQL;
- `X-Correlation-ID` válido en éxito y error y mismo valor en Problem Details;
- método/ruta exactos y operación OpenAPI única;
- no existe ningún otro endpoint funcional;
- lint OpenAPI, compatibilidad y cliente generado sin diff.

### 16.5 Rate limiting, spoofing y recuperación

- petición bajo ambas cuotas admitida;
- 16.ª en 10 segundos y 61.ª en 60 segundos rechazadas;
- `429`, content type, código, correlation ID y `Retry-After`;
- reloj/filas controladas para recuperar cada ventana sin sleeps largos;
- carga concurrente PostgreSQL nunca supera plazas disponibles;
- fallo, timeout o error SQL devuelve `503` y no invoca búsqueda;
- falta de política, operación o clave activa falla cerrada;
- rotación comprueba/incrementa todas las versiones sin reiniciar cuota;
- IPv4, sintaxis ambigua, IPv4 mapeada, IPv6 comprimida, zone ID y dos IPv6
  del mismo `/64`;
- headers falsificados directos se ignoran;
- cliente externo contra Caddy no puede escoger sujeto mediante
  `X-Forwarded-For` o `Forwarded`;
- Caddy elimina ambas y reconstruye solo la seleccionada;
- cadena multiproxy se resuelve de derecha a izquierda;
- cadena malformada desde proxy confiable falla cerrada;
- al llegar a 70/120 segundos la fila deja de contar y el job la elimina;
- umbrales de backlog generan señal sin persistencia adicional;
- BD, logs, errores, métricas y trazas no contienen IP, fingerprint, query ni
  material HMAC.

### 16.6 Frontend

- render y búsqueda en ES/EN;
- debounce/configuración y cliente generado;
- loading, vacío, resultados, selección, siguiente página y retry;
- 400, 409, 429 y 503 mapeados a claves i18n;
- `Retry-After` respetado;
- opción manual separada del catálogo;
- paridad i18n y detector de literales visibles;
- navegación completa por teclado, roles ARIA, foco y anuncios.

### 16.7 E2E

1. Publicar la fixture sintética mediante el mismo puerto administrativo.
2. Abrir la pantalla, buscar con y sin tildes y seleccionar por UUID interno.
3. Paginar sin duplicados y reiniciar ante snapshot cambiado.
4. Elegir «no encontrada» sin crear fila global ni llamada de escritura.
5. Alcanzar el límite a través de Caddy, observar 429 y recuperar la ventana.
6. Repetir con cabeceras falsificadas y demostrar que no alteran la cuota.
7. Derribar/invalidar el adapter y demostrar 503 sin resultados del catálogo.
8. Verificar que no se crea usuario, cuenta, tenant, workspace ni dato personal.

## 17. Entregas pequeñas y orden exacto

Cada entrega debe quedar verde antes de comenzar la siguiente. Los nombres de
clase son previstos; podrán ajustarse sin cambiar los límites descritos.

### E1 — Dominio e invariantes

Archivos previstos:

- `server/src/main/java/com/propractix/academicinstitution/domain/model/AcademicInstitution.java`
- `.../domain/model/AcademicInstitutionId.java`
- `.../domain/model/CatalogImport.java`
- `.../domain/model/CatalogImportId.java`
- `.../domain/model/ExternalInstitutionReference.java`
- `.../domain/model/OfficialInstitutionName.java`
- `.../domain/model/CatalogImportMetadata.java`
- `.../domain/model/CatalogArtifactHash.java`
- `.../domain/event/InstitutionCatalogPublished.java`
- `.../domain/service/CatalogPublicationPolicy.java`
- tests espejo bajo `server/src/test/java/com/propractix/academicinstitution/domain/`

### E2 — Casos de uso y puertos

Archivos previstos:

- `.../application/port/in/PublishInstitutionCatalog.java`
- `.../application/port/in/SearchPublishedInstitutions.java`
- `.../application/command/PublishInstitutionCatalogCommand.java`
- `.../application/query/SearchPublishedInstitutionsQuery.java`
- `.../application/query/AcademicInstitutionSearchPage.java`
- `.../application/port/out/CatalogArtifactParser.java`
- `.../application/port/out/CatalogPublicationRepository.java`
- `.../application/port/out/PublishedInstitutionQueryRepository.java`
- `.../application/port/out/ApplicationClock.java`
- `.../application/port/out/AcademicInstitutionIdGenerator.java`
- `.../application/service/PublishInstitutionCatalogService.java`
- `.../application/service/SearchPublishedInstitutionsService.java`
- tests de aplicación con fakes bajo el mismo contexto.

### E3 — Esquema y persistencia del catálogo

Archivos previstos:

- `server/src/main/resources/db/migration/V1__create_academic_institution_catalog.sql`
- `.../adapter/out/persistence/AcademicInstitutionJpaEntity.java`
- `.../adapter/out/persistence/CatalogImportJpaEntity.java`
- repositorios/mappers internos y `PostgresCatalogPublicationAdapter.java`
- `PostgresPublishedInstitutionQueryAdapter.java`
- `.../configuration/AcademicInstitutionConfiguration.java`
- tests Testcontainers de migración, mapping, búsqueda y concurrencia.

### E4 — Parser e importación administrativa

Archivos previstos:

- `.../adapter/out/integration/StrictCsvCatalogArtifactParser.java` o paquete
  adapter equivalente sin dependencia hacia el dominio desde fuera;
- `.../adapter/in/commandline/PublishInstitutionCatalogCommandLineAdapter.java`
- configuración tipada del catálogo en `application*.yaml`;
- `package.json` con comando de publicación/dry run;
- `docs/catalog/fixtures/academic-institutions-es-synthetic-v1.csv` se reutiliza
  sin cambiar sus bytes;
- tests de contrato CSV y verificación de su hash aprobado.

### E5A — Policy loader, configuración, IP, HMAC y secretos

Archivos previstos:

- puertos y modelo técnico bajo
  `server/src/main/java/com/propractix/platform/ratelimit/application/**`;
- policy loader, propiedades tipadas, `ClientIpResolver` y adapter HMAC bajo
  `server/src/main/java/com/propractix/platform/ratelimit/configuration/**` y
  `.../adapter/out/crypto/**`;
- `server/pom.xml` para empaquetar la política canónica sin duplicarla;
- `server/src/main/resources/application.yaml` y perfiles aplicables;
- `infra/local/templates/rate-limit-hmac-rl-ip-v1.example`;
- `scripts/prepare-local-secrets.sh` y `scripts/check-local-baseline.sh`;
- tests unitarios del loader, configuración, normalización IP, HMAC y ausencia
  de material secreto en errores/logs.

### E5B — Persistencia PostgreSQL y operación del rate limiter

Archivos previstos:

- `server/src/main/resources/db/migration/V2__create_public_endpoint_rate_limit_buckets.sql`;
- adapter JDBC y servicio atómico bajo
  `server/src/main/java/com/propractix/platform/ratelimit/adapter/out/persistence/**`;
- componentes de rotación, limpieza, health/readiness y métricas bajo
  `server/src/main/java/com/propractix/platform/ratelimit/**`;
- tests Testcontainers de migración, atomicidad de ambas ventanas, rotación,
  expiración/limpieza, métricas y concurrencia PostgreSQL.

### E5C — Filtro HTTP, seguridad y borde Caddy

Archivos previstos:

- filtro HTTP y Problem Details técnicos bajo
  `server/src/main/java/com/propractix/platform/ratelimit/adapter/in/http/**`;
- integración exacta en
  `server/src/main/java/com/propractix/configuration/security/FoundationSecurityConfiguration.java`;
- `client/Caddyfile` para eliminar ambas familias recibidas y reconstruir solo
  `X-Forwarded-For`;
- tests de integración de seguridad, spoofing directo y detrás de Caddy,
  cadena confiable/no confiable y fail-closed por cabecera, proxy, política,
  secreto o persistencia inválidos.

No se modifica el estado ni la checklist de `CL0_CLOUD_STAGING`, no se ejecuta
AWS y no se añade secreto operativo al repositorio.

### E6 — OpenAPI, Problem Details y REST

Archivos previstos:

- `docs/api/openapi.yaml`;
- `server/src/main/java/com/propractix/academicinstitution/adapter/in/rest/AcademicInstitutionSearchController.java`;
- DTO/mappers REST en ese mismo adapter;
- utilidades/handler comunes de Problem Details bajo
  `com.propractix.platform.web`;
- tests MockMvc/contrato/correlación;
- ninguna ruta adicional.

### E7 — Cliente generado y componente UI

Archivos previstos:

- `client/package.json`, `package-lock.json` y configuración de
  `@hey-api/openapi-ts`;
- `client/src/shared/api/generated/**`;
- `client/src/features/academic-institutions/api/**`;
- `client/src/features/academic-institutions/components/AcademicInstitutionAutocomplete.tsx`;
- `client/src/features/academic-institutions/AcademicInstitutionSearchPage.tsx`;
- `client/src/configuration/academicInstitutionSearch.json` y loader tipado;
- `client/src/shared/i18n/locales/es.json` y `en.json`;
- `client/src/app/App.tsx`;
- tests de componente, API, i18n y accesibilidad.

Solo se añadirán de F0 las dependencias usadas: cliente OpenAPI, React Query y
Playwright para E2E. Router, Zustand y formularios no se incorporan sin uso.

### E8 — E2E y observabilidad operable

Archivos previstos:

- `client/playwright.config.ts`;
- `client/e2e/academic-institution-search.spec.ts`;
- script de orquestación local H02 bajo `scripts/`;
- pruebas de métricas/redacción y comprobación de que el runbook permanece
  alineado con S0 `CLOSED`, sin cambiar la política aprobada;
- scripts raíz `client:generate`, `client:e2e` y checks de cliente generado.

### E9 — Primer snapshot real y evidencia de aceptación

Archivos previstos:

- `docs/catalog/snapshots/ruct-<YYYY-MM-DD>/academic-institutions-es.csv`;
- `docs/catalog/snapshots/ruct-<YYYY-MM-DD>/manifest.yaml`;
- `docs/reviews/i1-h02-first-snapshot-evidence.md`;
- `docs/reviews/aceptacion-i1-h02.md`, preparado con resultados reales y
  pendiente de aceptación expresa;
- actualización final de `docs/README.md` y, solo tras aceptación, del estado
  de H02 en `docs/plans/increment-01-plan.md`.

La aceptación no se autoasigna desde la rama de implementación.

## 18. Comandos de validación previstos

Validación por entrega y final:

```bash
npm run infra:up
npm run server:test
npm run server:verify
npm run openapi:lint
npm run client:generate
git diff --exit-code -- client/src/shared/api/generated
npm run client:lint
npm run client:typecheck
npm run client:test
npm run client:build
npm run client:e2e
./scripts/check-migrations.sh
npm run check
git diff --check
git diff --stat
git diff
git status --short
```

Validaciones específicas adicionales:

- SHA-256 de fixture y snapshot contra sus manifests;
- Flyway sobre PostgreSQL 16 vacío y actualización soportada;
- concurrencia real con Testcontainers;
- inspección automatizada de BD/logs/métricas para ausencia de IP,
  fingerprint, query y secretos;
- prueba Caddy con headers falsificados;
- búsqueda local del snapshot real y prueba de conservación de la versión
  anterior ante importación inválida;
- `docker compose config` sin LocalStack y sin secretos inline.

No se ejecutan `tofu`, AWS CLI, despliegues, cambios de GitHub Environment ni
mutaciones de `CL0_CLOUD_STAGING`.

## 19. Definition of Done y evidencia

H02 estará técnicamente terminada cuando exista evidencia de que:

- las precondiciones y gates continúan cerrados sin reabrirse;
- el único endpoint nuevo es `searchAcademicInstitutions`;
- una universidad se consulta sin cuenta, tenant ni workspace;
- cada resultado usa UUID v4 interno y no clave RUCT como identidad;
- el primer snapshot real fue adquirido, revisado, hasheado, publicado y puede
  reproducirse desde su CSV/manifest;
- la publicación es atómica y la última versión válida sobrevive a fallos;
- repetir el artefacto activo es un no-op completo y republicar uno
  `SUPERSEDED` crea importación, snapshot y evento nuevos e invalida cursores;
- búsqueda, normalización, cursor y paginación cumplen el contrato;
- OpenAPI, servidor y cliente generado están sincronizados;
- rate limiting cumple exactamente S0, falla cerrado y resiste concurrencia,
  spoofing, rotación y recuperación de ventana;
- 429/503 usan Problem Details, `Retry-After` y el mismo correlation ID;
- UI/autocompletado cubre estados, teclado, accesibilidad, ES/EN y opción
  manual separada;
- logs, trazas, métricas y tabla no contienen IP, query, fingerprint, secretos
  ni datos personales;
- migraciones funcionan desde cero y son forward-only;
- pruebas de dominio, aplicación, arquitectura, persistencia, API, contrato,
  frontend y E2E están verdes;
- la aceptación se realizó localmente sin AWS y `CL0_CLOUD_STAGING` sigue
  `PENDING`;
- el diff fue revisado y no contiene secretos, artefactos temporales ni cambios
  fuera de H02.

La evidencia mínima adjunta a la revisión incluirá SHA del commit, comandos y
resultados, versión de PostgreSQL, IDs/hashes no sensibles del snapshot,
recuentos, extracto de OpenAPI lint, resultados de concurrencia/spoofing,
capturas o trazas E2E sin PII y riesgos residuales.

## 20. Decisiones aceptadas para la ejecución

Estas decisiones forman parte del plan aprobado. La aprobación no inicia
I1-H02: la historia permanece `READY` hasta el merge de la PR documental y,
después de ese merge, solo queda autorizada `E1 — Dominio e invariantes`:

| Decisión aceptada | Aplicación |
|---|---|
| Paginación `default=20`, `max=50` | Configuración tipada compartida por contrato, servidor y cliente; OpenAPI declara ambos límites. |
| `409 catalog_snapshot_changed` | Un cursor de otro snapshot se rechaza y el cliente reinicia la búsqueda. |
| `X_FORWARDED_FOR` como único modo detrás de Caddy | Solo puede activarse tras aprobar el saneamiento de ambas familias de cabeceras y las pruebas de spoofing/fail-closed; local directo continúa en `REMOTE_ADDRESS`. |
| Procedimiento de revisión del mapeo RUCT | El manifest documenta columnas, omisiones y decisiones; una segunda persona revisa recuentos, duplicados y muestra contra la fuente antes de publicar. No se introducen heurísticas silenciosas. |
| Revisión de privacidad del fingerprint HMAC | Es obligatoria antes de exposición productiva pública. No bloquea implementación ni aceptación local con secretos y datos sintéticos. |

## 21. Riesgos y decisiones realmente pendientes

| Elemento | Tratamiento | Aprobación necesaria |
|---|---|---|
| Librería CSV, si se propone una en implementación | Solo se aceptará una utilidad fijada, con licencia/dependencias revisadas; no puede cambiar el contrato ni convertirse en librería estructural. Un parser propio estricto es la alternativa. | Revisión técnica ordinaria; no ADR salvo cambio estructural. |
| Deriva del formato RUCT real | El procedimiento aceptado de doble revisión detecta cambios; un mapeo ambiguo bloquea esa publicación sin alterar el snapshot vigente. | Producto + Datos resuelven la ambigüedad concreta antes de publicar; no reabre C0. |

No hay contradicción que obligue a crear un ADR. La aprobación del plan no
autoriza código antes del merge de la PR documental. Después de ese merge solo
se autoriza `E1 — Dominio e invariantes`; no se autoriza AWS, otro endpoint ni
una ampliación funcional de H02.

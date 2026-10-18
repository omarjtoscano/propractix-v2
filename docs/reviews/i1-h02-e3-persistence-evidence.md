# Evidencia de implementación de I1-H02 E3

- Entrega: `E3 — Esquema y persistencia del catálogo`
- Estado: `IN_PROGRESS`
- Rama: `feature/i1-h02-e3-catalog-persistence`
- Base integrada de E2: `51e2fdf47841cce78a30a0189c3da3e3feff0c5c`
- Fecha de validación local: 2026-10-18
- Base de datos de integración: PostgreSQL 16.15 mediante Testcontainers

Este registro reúne la evidencia técnica previa a solicitar aceptación. No
declara E3 `ACCEPTED`, no cierra I1-H02 y no autoriza E4 ni entregas
posteriores.

## Alcance verificado

- La migración Flyway crea exclusivamente las tablas del catálogo del contexto
  `academicinstitution`, sus claves, constraints e índices.
- La identidad interna usa UUID v4 y la referencia externa se conserva como la
  pareja única `source_system + source_record_id`.
- Los modelos JPA, mapeadores, repositorios JDBC y adapters permanecen dentro
  del adapter de salida; aplicación y dominio no dependen de persistencia.
- La publicación completa se ejecuta en una transacción PostgreSQL y adquiere
  primero un advisory lock transaccional mediante
  `pg_try_advisory_xact_lock`.
- La consulta usa una transacción de solo lectura `REPEATABLE READ`, valida el
  identificador de snapshot del cursor y pagina en SQL mediante la clave
  `(search_name_normalized, institution_id)` más `LIMIT`.
- No se añadieron parser CSV, CLI, snapshot real, controladores REST, cambios
  OpenAPI, frontend, rate limiting ni cambios AWS.

## Pruebas PostgreSQL 16

`CatalogPersistenceIntegrationTest` ejecuta once pruebas; las pruebas de
persistencia usan una instancia real `postgres:16-alpine` y las de apertura de
transacción usan un `DataSource` controlado incapaz de obtener una conexión. El
conjunto cubre:

1. migración Flyway desde esquema vacío y constraints de UUID v4, snapshot
   publicado único y referencia externa única;
2. mapping de ida y vuelta entre dominio y entidades internas;
3. publicación y reconciliación con reutilización del UUID conocido;
4. retirada de instituciones ausentes sin borrar su identidad y reutilización
   de esa identidad cuando reaparecen;
5. repetición del artefacto activo sin crear importación, snapshot o evento ni
   consumir reloj o generadores de UUID;
6. fallos tipados diferenciados para publicación inválida, conflicto de
   reconciliación y almacenamiento, con resultados sin referencias ni filas;
7. búsqueda filtrada y paginación keyset sin cargar el catálogo completo;
8. rechazo consistente de un cursor ligado a un snapshot anterior;
9. competencia real entre dos publicadores, donde el segundo no espera el lock
   y devuelve `PublicationInProgress` antes de dos segundos;
10. indisponibilidad al abrir la transacción de publicación, convertida en
    `Rejected(STORAGE_FAILURE)` sin propagar `TransactionException`;
11. indisponibilidad al abrir la transacción de consulta, convertida en
    `CatalogUnavailable(READ_FAILED)` sin propagar `TransactionException`.

## Fallo al abrir la transacción y sanitización

Las pruebas `publicationMapsTransactionOpeningFailureToSanitizedStorageFailure`
y `queryMapsTransactionOpeningFailureToSanitizedReadFailure` usan un
`DataSource` que rechaza toda conexión con una `SQLException` cuyo mensaje de
prueba contiene una URL JDBC, usuario, contraseña, SQL y un detalle interno.
`JdbcTransactionManager` la convierte en una `TransactionException`. Se
verifica que ninguno de los dos puertos propaga la excepción, que cada adapter
devuelve su fallo tipado y que ni el resultado ni el evento capturado de Logback
contienen ninguno de esos datos o un throwable asociado.

## Atomicidad y rollback

La prueba `rollsBackEveryPublicationMutationWhenStorageFails` publica primero
un snapshot válido y después instala temporalmente un trigger PostgreSQL que
falla durante la actualización de una institución. Se verifica que el adapter
devuelve `STORAGE_FAILURE`, que no queda una importación parcial, que el
snapshot anterior continúa siendo el único `PUBLISHED` y que sus datos no se
modifican. Esto ejercita en una misma transacción el superseded del snapshot
anterior, la creación del nuevo y la reconciliación de instituciones.

## Concurrencia

La prueba `aConcurrentPublisherFailsFastWhileTheLockHolderPublishes` mantiene
abierta una publicación después de adquirir el advisory lock y lanza otra desde
una conexión independiente. La segunda termina en menos de dos segundos con
`PublicationInProgress`; tras liberar la primera, solo su snapshot queda
publicado. Así se comprueba que el lock es no bloqueante y se toma antes de
comparar el artefacto activo.

## Comandos ejecutados

| Comando | Resultado |
|---|---|
| `npm run infra:up` | Correcto; PostgreSQL y Mailpit saludables |
| `npm run server:test` | Correcto; 96 pruebas, 0 fallos, 0 errores, 1 benchmark omitido |
| `npm run server:verify` | Correcto; 96 pruebas y empaquetado completado |
| `npm run check` | Correcto; baseline, gate de despliegue, OpenAPI, servidor y cliente |
| `git diff --check` | Correcto |

Las comprobaciones remotas de CI, Trivy y Build OCI se registrarán en la Draft
PR cuando GitHub Actions termine sobre el commit de E3.

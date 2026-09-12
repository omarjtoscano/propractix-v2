# ADR-008 — OpenAPI, errores e idempotencia

- Estado: Propuesto
- Fecha: 2026-09-12
- Decisores: Arquitectura, Backend y Frontend

## Contexto

V1 mantenía manualmente el contrato entre frontend y backend. V2 necesita una API predecible, errores procesables y protección frente a repeticiones producidas por red, doble clic o reintentos.

## Decisión

La API pública del MVP vivirá bajo `/api/v1`. El contrato OpenAPI versionado en `docs/api/openapi.yaml` será la fuente de verdad y se modificará junto con la historia correspondiente.

No se generará el servidor completo desde OpenAPI. Los adaptadores REST se implementarán explícitamente y una prueba de contrato comprobará que no divergen. El cliente TypeScript sí se generará desde el contrato cuando exista `client`.

Los errores utilizarán `application/problem+json` y el modelo Problem Details, con al menos:

```text
type, title, status, detail, instance, code, traceId, violations
```

`detail` no expondrá excepciones internas ni datos sensibles. `code` será estable y traducible por el cliente.

Los endpoints de creación que puedan repetirse, incluido el registro público, exigirán `Idempotency-Key`. Se almacenará de forma acotada:

- operación y ámbito del actor o registro;
- hash de la petición normalizada;
- estado y respuesta reproducible;
- expiración inicial de 24 horas.

Repetir clave y contenido devolverá el resultado anterior. Reutilizar la clave con otro contenido devolverá `409 Conflict`.

La API utilizará UUID como identificador externo, fechas ISO 8601, paginación por cursor cuando sea necesaria y correlation ID propagado a logs y eventos.

## Alternativas consideradas

### Contrato generado únicamente desde controladores

Rechazado como fuente única porque permite diseñar el API después de implementar y dificulta una revisión previa con frontend.

### Generar servidor y modelos de dominio desde OpenAPI

Rechazado porque mezclaría modelos de transporte con dominio y dificultaría la arquitectura hexagonal.

### Errores ad hoc por endpoint

Rechazados porque obligan al cliente a interpretar formatos distintos.

### Idempotencia confiada al cliente

Rechazada; los reintentos de red requieren garantía del servidor.

## Consecuencias

- Cada historia con API actualizará contrato, pruebas y cliente generado.
- Se necesitará almacenamiento y limpieza de claves idempotentes.
- El diseño de errores será uniforme desde el primer incremento.
- Un cambio incompatible requerirá una nueva versión o estrategia de transición.

## Criterios de aprobación

- Aceptar OpenAPI design-first sin generar el servidor.
- Aceptar Problem Details como formato único.
- Aceptar `Idempotency-Key` obligatorio en registros y creaciones sensibles.

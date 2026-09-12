# ADR-004 — Identificadores, tiempo, zonas horarias y concurrencia

- Estado: Propuesto
- Fecha: 2026-09-12
- Decisores: Arquitectura

## Contexto

Las prácticas combinan instantes técnicos, fechas académicas, horarios locales y procesos concurrentes. El modelo debe evitar IDs secuenciales expuestos, ambigüedad temporal y actualizaciones perdidas.

## Decisión

- Los identificadores públicos y de aggregates serán UUID v4 generados en la aplicación.
- Cada tipo relevante utilizará un value object (`UserId`, `TenantId`, `AcademicInstitutionId`) en el dominio.
- Los instantes técnicos se representarán con `Instant` y se persistirán en UTC.
- Las fechas académicas sin hora se representarán con `LocalDate`.
- Los horarios que dependan de ubicación combinarán fecha/hora local con una zona IANA explícita (`ZoneId`).
- No se usará `LocalDateTime` para timestamps globales.
- El dominio recibirá un puerto `Clock` o un instante desde la aplicación; no invocará directamente el reloj del sistema.
- Los aggregates mutables tendrán versión de concurrencia optimista. El adapter JPA utilizará `@Version` y la API devolverá conflicto cuando exista una modificación concurrente.

La zona horaria predeterminada podrá proceder de la organización o jurisdicción, pero quedará registrada en los datos relevantes; no se codificará `Europe/Madrid` dentro del dominio.

## Alternativas consideradas

### IDs numéricos autoincrementales

Rechazados para recursos públicos porque revelan secuencia y dificultan generación previa a persistencia.

### UUID v7

Aplazado. Mejora localidad de índices, pero Java 21 no lo proporciona de forma estándar y no se justifica introducir una dependencia solo para el MVP.

### Bloqueo pesimista general

Rechazado por coste y contención. Se reservará para un caso probado y documentado.

## Consecuencias

- Los adaptadores deben mapear value objects y UUID.
- Los tests pueden fijar el tiempo de forma determinista.
- Los conflictos se exponen mediante un error de API estable y comprobable.
- Las fechas legales futuras deberán distinguir con precisión entre fecha, instante y zona.

## Criterios de aprobación

- Aceptar UUID v4 durante el MVP.
- Aceptar UTC más zona IANA explícita.
- Aceptar concurrencia optimista en aggregates mutables.

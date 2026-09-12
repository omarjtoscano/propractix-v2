# ADR-005 — Eventos internos y outbox transaccional

- Estado: Propuesto
- Fecha: 2026-09-12
- Decisores: Arquitectura y Operaciones

## Contexto

El registro y la verificación generan efectos secundarios como correo y auditoría. Si se ejecutan dentro de la petición sin persistencia durable, una caída puede dejar el negocio confirmado y la comunicación perdida.

## Decisión

Los aggregates podrán registrar eventos de dominio Java puros. La capa de aplicación los transformará en eventos de aplicación y, cuando produzcan efectos asíncronos o crucen contextos, los almacenará en una outbox dentro de la misma transacción que el cambio de negocio.

La entrega tendrá semántica al menos una vez:

- cada evento tendrá `eventId`, tipo, versión, aggregate, tenant cuando aplique, `occurredAt`, payload y correlation ID;
- un publicador local recuperará eventos pendientes y registrará intentos;
- los consumidores serán idempotentes;
- los fallos quedarán reintentables y observables;
- no se eliminará evidencia de entrega; se aplicará la política de retención que se defina.

Durante el Incremento 1 no se introducirá Kafka, RabbitMQ ni otro broker. PostgreSQL será el almacenamiento durable de outbox. El correo de verificación será el primer efecto externo que use esta ruta.

Los handlers puramente internos que deban participar en una misma invariante no se modelarán como integración eventual; el caso de uso coordinará explícitamente esa operación.

## Alternativas consideradas

### Enviar correo antes de confirmar la transacción

Rechazada porque puede enviar una comunicación sobre datos que finalmente no se guardan.

### Evento en memoria después del commit

Rechazado para efectos que no se puedan perder; una caída entre commit y publicación perdería el evento.

### Broker desde el inicio

Rechazado por complejidad operativa sin necesidad de escalado demostrada.

## Consecuencias

- Se añadirán tabla, job, métricas y pruebas de outbox.
- Un consumidor debe tolerar duplicados.
- La consistencia entre contextos podrá ser eventual y visible en el modelo de estado.
- Los esquemas de eventos se versionarán y no transportarán entidades internas completas.

## Criterios de aprobación

- Confirmar PostgreSQL outbox sin broker durante el MVP.
- Confirmar entrega al menos una vez e idempotencia obligatoria.

# ADR-002 — Organización hexagonal de paquetes

- Estado: Propuesto
- Fecha: 2026-09-12
- Decisores: Arquitectura

## Contexto

La auditoría de V1 detectó dependencias desde el dominio hacia infraestructura y una separación nominal que no impedía el acoplamiento. V2 necesita un dominio comprobable sin Spring ni PostgreSQL.

## Decisión

Cada bounded context utilizará, cuando sean necesarias, estas áreas:

```text
<context>.domain.model
<context>.domain.event
<context>.domain.service
<context>.application.port.in
<context>.application.port.out
<context>.application.command
<context>.application.query
<context>.application.service
<context>.adapter.in.rest
<context>.adapter.out.persistence
<context>.adapter.out.integration
<context>.configuration
```

La regla de dependencias será:

```text
adapter -> application -> domain
configuration -> adapter/application
```

El dominio será Java puro. No contendrá anotaciones o tipos de Spring, JPA, Jackson, Servlet, seguridad, almacenamiento o transporte.

Los servicios de aplicación orquestarán casos de uso y transacciones. Los controladores traducirán HTTP a comandos y resultados. Los repositorios de dominio serán puertos de salida y sus implementaciones vivirán en adapters.

No se crearán directorios vacíos anticipadamente. Cada paquete aparecerá cuando una historia necesite una responsabilidad concreta.

## Alternativas consideradas

### Entidades de dominio anotadas con JPA

Rechazada porque acopla el modelo a persistencia y dificulta probar invariantes aisladas.

### Arquitectura global controller/service/repository

Rechazada porque no expresa propiedad funcional ni límites de contexto.

### Modelo anémico con reglas en servicios

Rechazado. Las invariantes que pertenecen a aggregates estarán en el dominio.

## Consecuencias

- Existirán mappers entre dominio y persistencia.
- Habrá algo más de código explícito, compensado por límites y pruebas más claros.
- ArchUnit impedirá dependencias prohibidas.
- Los DTO HTTP no atravesarán el puerto de aplicación como modelos de dominio.

## Criterios de aprobación

- Aceptar la separación entre modelo de dominio y entidades JPA.
- Aceptar que los paquetes se creen por necesidad, no como plantilla vacía.

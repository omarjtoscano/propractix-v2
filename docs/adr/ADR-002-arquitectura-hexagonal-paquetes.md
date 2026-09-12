# ADR-002 — Organización hexagonal de paquetes

- Estado: Aceptado — revisión 4
- Fecha: 2026-09-12
- Decisores: Arquitectura
- Reemplaza: revisión 3 del ADR-002

## Contexto

V1 permitió dependencias desde reglas de negocio hacia infraestructura. V2 necesita un dominio comprobable sin Spring ni PostgreSQL y contratos explícitos para los procesos entre contextos.

## Decisión

Cada bounded context utilizará únicamente los paquetes que necesite:

```text
<context>.domain.model
<context>.domain.event
<context>.domain.service
<context>.application.port.in
<context>.application.port.out
<context>.application.command
<context>.application.query
<context>.application.service
<context>.application.contract
<context>.adapter.in.rest
<context>.adapter.out.persistence
<context>.adapter.out.integration
<context>.configuration
```

No se crearán paquetes vacíos como plantilla.

## Responsabilidades

- `domain`: aggregates, entities, value objects, políticas puras, invariantes y eventos de dominio.
- `application.port.in`: casos de uso públicos.
- `application.port.out`: repositorios y capacidades externas requeridas por el caso de uso.
- `application.service`: orquestación y límites transaccionales locales al contexto.
- `application.contract`: DTO o esquemas públicos mínimos para comunicación entre contextos; nunca entidades internas.
- `adapter.in`: traducción desde HTTP, jobs o mensajería hacia puertos de entrada.
- `adapter.out`: persistencia e integraciones.
- `configuration`: composición de dependencias y aspectos técnicos.

Las interfaces de repositorio residirán en `application.port.out`. No se expondrán repositorios de dominio entre contextos. Cuando un contexto ofrece una capacidad a otro, el contrato pertenece al proveedor y se publica como `application.port.in` o `application.contract`; el consumidor no define una copia equivalente en su propio `port.out`.

## Regla de dependencias

```text
adapter -> application -> domain
configuration -> adapter/application
```

El dominio será Java puro. No contendrá tipos o anotaciones de Spring, JPA, Jackson, Servlet, seguridad, almacenamiento o transporte.

Como excepción pragmática y explícita, las implementaciones en `application.service` podrán usar únicamente `org.springframework.transaction.annotation.Transactional` para declarar una transacción local al contexto. No podrán depender de repositorios Spring Data, entidades JPA ni otros componentes de infraestructura. Si esta excepción crece, deberá reemplazarse por un boundary transaccional externo.

## Contratos públicos entre contextos

Se consideran públicos únicamente:

- interfaces de `application.port.in` declaradas para consumo interno;
- tipos mínimos de `application.contract`;
- eventos de integración versionados publicados después del commit.

No son contratos públicos los aggregates, entidades, repositorios, mappers, JPA entities, servicios concretos o DTO REST. Para comandos que modifican otro contexto se preferirá un proceso durable y eventos. Las consultas síncronas se permitirán solo mediante un puerto público sin compartir modelo interno, no abrirán una transacción distribuida y nunca permitirán al consumidor escribir datos propiedad del proveedor.

## Reglas ArchUnit

1. `..domain..` solo depende de Java y del propio dominio/shared kernel autorizado.
2. `..application..` no depende de `..adapter..`, `..configuration..`, JPA o Spring Data.
3. Solo `..application.service..` puede depender de `spring-tx` y únicamente de `@Transactional`.
4. `..adapter.in.rest..` no es consumido por application o domain.
5. `..adapter.out.persistence..` no expone entidades JPA fuera de su adapter.
6. Los contextos no se importan entre sí salvo paquetes `application.contract` o puertos públicos incluidos en la matriz vigente y aceptada de ADR-001.
7. Cada importación cross-context coincide con `consumer`, `owner/provider` y paquete público del registro de ADR-001; ArchUnit falla ante una dirección inversa o un contrato no registrado.
8. `configuration` es el único lugar que conecta callbacks de un relay técnico genérico con puertos de entrada de un contexto concreto.
9. `platform` no depende de dominios concretos; implementa interfaces suministradas en configuración.
10. La aplicación no contiene paquetes raíz globales `controller`, `service`, `repository` o `entity`.

## Alternativas consideradas

- Entidad única para dominio/JPA/JSON: rechazada por acoplamiento.
- `TransactionRunner` propio desde el inicio: aplazado por ceremonia; se limita estrictamente la excepción `@Transactional`.
- Arquitectura global por capas: rechazada.
- Modelo anémico: rechazado; las invariantes de aggregates viven en dominio.

## Consecuencias

- Existirán mappers explícitos entre transporte, aplicación, dominio y persistencia.
- Los contratos públicos serán deliberadamente pequeños y versionables.
- ArchUnit impedirá erosión estructural antes de cada merge.
- La capa application conserva una dependencia mínima en `spring-tx`, aceptada como riesgo controlado.

## Condiciones documentales de aceptación

- Arquitectura acepta la dirección de dependencias y la excepción limitada de `spring-tx`.
- Las alternativas y el coste de mappers explícitos están aceptados.
- La matriz vigente de ADR-001 define los únicos contratos cross-context del incremento.

## Conformidad de la implementación

- Una prueba de dominio compila y se ejecuta sin levantar Spring.
- Las diez reglas ArchUnit quedan implementadas en I1-H01 a partir del registro vigente de ADR-001.
- Ningún contrato público contiene JPA entities o DTO REST.
- Cada transacción modifica aggregates de un solo contexto.

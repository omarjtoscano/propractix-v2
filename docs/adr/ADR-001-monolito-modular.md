# ADR-001 — Monolito modular y estrategia de módulos

- Estado: Propuesto
- Fecha: 2026-09-12
- Decisores: Producto y Arquitectura

## Contexto

ProPractix V2 debe reconstruir capacidades de V1 sin reproducir su acoplamiento. El equipo necesita entregar incrementos completos con una operación sencilla y conservar límites que permitan evolucionar el producto.

## Decisión

Se construirá un monolito modular desplegable como una única aplicación Spring Boot y una única base PostgreSQL.

Durante el MVP, `server` será un único proyecto Maven. Los módulos serán bounded contexts identificados por paquetes raíz:

```text
com.propractix.identity
com.propractix.organization
com.propractix.academicinstitution
com.propractix.jurisdiction
com.propractix.recruitment
com.propractix.formalization
com.propractix.internship
com.propractix.learning
com.propractix.compliance
com.propractix.documents
com.propractix.notifications
```

Los límites se comprobarán con ArchUnit. Un contexto solo podrá consumir contratos públicos de aplicación o eventos de otro contexto. No accederá a sus repositorios, tablas ni clases internas.

Spring Modulith no será obligatorio en el inicio. Podrá evaluarse mediante un ADR posterior si aporta validación o publicación de eventos sin duplicar los guardrails existentes.

## Alternativas consideradas

### Microservicios desde el inicio

Rechazada por coste operativo, transacciones distribuidas y complejidad innecesaria para un MVP.

### Proyecto Maven multimódulo desde el inicio

Aplazado. Ofrece límites de compilación más fuertes, pero aumenta la configuración antes de conocer la estabilidad real de cada contexto.

### Aplicación por capas globales

Rechazada porque agrupa por tecnología y facilita dependencias entre funcionalidades no relacionadas.

## Consecuencias

- Un único despliegue y una sola transacción local simplifican el MVP.
- Los límites modulares requieren pruebas automáticas y disciplina de revisión.
- Un contexto podrá extraerse en el futuro solo con una justificación operativa y un ADR nuevo.
- No se crearán dependencias cíclicas entre contextos.

## Criterios de aprobación

- Aceptar un único módulo Maven durante el MVP.
- Aceptar ArchUnit como guardrail inicial.
- Confirmar que no se requiere Spring Modulith en el Incremento 1.

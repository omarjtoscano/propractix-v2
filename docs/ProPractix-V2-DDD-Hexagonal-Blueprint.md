# ProPractix V2 — Product & Architecture Blueprint

> **Objetivo:** reconstruir ProPractix de forma incremental, orientada a valor de negocio, utilizando **Domain-Driven Design (DDD)** y **Arquitectura Hexagonal**, manteniendo desde el inicio la capacidad **multidioma** y **multijurisdicción**.

**Fecha:** 11 de septiembre de 2026  
**Punto de partida:** `MVP-develop.zip`

---

## 1. Visión

ProPractix debe ayudar a una empresa a gestionar de principio a fin sus prácticas profesionales:

```text
Empresa
  │
  ├── Crear oferta
  │       ↓
  ├── Recibir candidaturas
  │       ↓
  ├── Seleccionar candidato
  │       ↓
  ├── Incorporar candidato
  │       ↓
  ├── Crear y activar práctica
  │       ↓
  ├── Gestionar práctica
  │       ├── Documentación
  │       ├── Cumplimiento
  │       ├── Horas
  │       ├── Objetivos
  │       └── Evaluaciones
  │       ↓
  └── Finalizar práctica
```

La V2 **no debe entenderse como una simple reescritura de V1**, sino como una reconstrucción del producto alrededor de un modelo de dominio más claro.

---

# 2. Principios de producto

## 2.1 Cada incremento debe entregar valor

La unidad de planificación no será una entidad técnica ni un CRUD, sino un **flujo de negocio completo**.

### ❌ Ejemplo incorrecto

> Crear módulo Candidate.

Resultado: entidad + repository + controller + pantalla, pero sin flujo útil.

### ✅ Ejemplo correcto

> Una empresa puede publicar una oferta y un candidato puede presentar una candidatura.

Resultado:

```text
Empresa
  ↓
Crear oferta
  ↓
Publicar oferta
  ↓
Oferta pública
  ↓
Candidato consulta oferta
  ↓
Candidato presenta candidatura
  ↓
Empresa consulta candidatura
```

Al terminar el incremento existe una funcionalidad utilizable.

---

# 3. Principios arquitectónicos

La V2 se construirá siguiendo:

1. **Domain-Driven Design**
2. **Arquitectura Hexagonal / Ports & Adapters**
3. **Modular Monolith inicialmente**
4. **Separación estricta entre dominio y tecnología**
5. **Casos de uso explícitos**
6. **Invariantes de negocio dentro del dominio**
7. **Dependencias dirigidas hacia el dominio**
8. **Integraciones externas aisladas mediante adapters**
9. **Configuración de país/jurisdicción separada de la lógica de negocio**
10. **Evolución incremental y despliegues frecuentes**

---

# 4. DDD como principio estructural

La arquitectura no se organizará principalmente alrededor de tablas o endpoints.

La pregunta será:

> ¿Qué conceptos, reglas y procesos existen en el negocio de ProPractix?

Los módulos deberán representar **bounded contexts / capacidades de negocio**, no capas técnicas.

Propuesta inicial:

```text
propractix
│
├── identity
├── company
├── recruitment
├── internship
├── compliance
├── performance
├── documents
├── notifications
├── billing
├── integrations
└── administration
```

Estos nombres son una hipótesis inicial y deben validarse durante la auditoría detallada de V1.

---

# 5. Bounded Contexts iniciales

## 5.1 Identity

Responsabilidad:

- usuarios
- autenticación
- autorización
- roles
- sesiones
- identidad

No debe contener reglas específicas de recruitment o internships.

## 5.2 Company

Responsabilidad:

- empresas
- centros/locations
- configuración de empresa
- jurisdicción de operación
- usuarios pertenecientes a una empresa

Conceptualmente:

```text
Company
 ├── id
 ├── name
 ├── jurisdiction
 └── configuration
```

## 5.3 Recruitment

Responsabilidad:

```text
Job Offer
Application
Candidate
Interview
Selection Process
```

Flujo principal:

```text
Draft
  ↓
Published
  ↓
Applications
  ↓
Screening
  ↓
Interview
  ↓
Selected / Rejected
```

El contexto Recruitment no debería conocer detalles internos de Compliance ni persistir directamente entidades de Internship.

## 5.4 Internship

Es uno de los núcleos principales del producto.

Responsabilidad:

- prácticas
- tutores
- participantes
- estado de la práctica
- plan de trabajo
- ciclo de vida

Ejemplo:

```text
Internship
 ├── Planned
 ├── Onboarding
 ├── Active
 ├── Suspended
 ├── Completed
 └── Cancelled
```

## 5.5 Compliance

Responsabilidad:

- requisitos legales
- documentos obligatorios
- validaciones
- vencimientos
- estados de cumplimiento

El contexto debe trabajar con reglas configurables por jurisdicción.

No:

```java
if (country == "ES") ...
```

Sí:

```text
CompliancePolicy
    ↓
requirementsFor(jurisdiction, internshipType)
```

## 5.6 Performance

Responsabilidad:

- objetivos
- seguimiento
- evaluaciones
- feedback
- progreso

Debe depender conceptualmente de Internship, pero no de sus detalles internos.

---

# 6. Arquitectura Hexagonal

La estructura lógica de cada bounded context será:

```text
┌───────────────────────────────────────────┐
│                 ADAPTERS                  │
│                                           │
│ REST / DB / Kafka / Email / S3 / etc.    │
└──────────────────┬────────────────────────┘
                   │
                   ▼
┌───────────────────────────────────────────┐
│                   PORTS                   │
│                                           │
│ Inbound              Outbound             │
│ Use Cases             Repositories        │
│ Commands              Event Publishers    │
│ Queries               External Services   │
└──────────────────┬────────────────────────┘
                   │
                   ▼
┌───────────────────────────────────────────┐
│                  DOMAIN                   │
│                                           │
│ Entities                                  │
│ Aggregates                                │
│ Value Objects                             │
│ Domain Services                           │
│ Domain Events                             │
│ Business Rules                            │
└───────────────────────────────────────────┘
```

Regla principal:

> **El dominio no conoce Spring, PostgreSQL, Kafka, HTTP, AWS ni ninguna tecnología externa.**

---

# 7. Estructura propuesta de un módulo

Ejemplo para `recruitment`:

```text
recruitment/
│
├── domain/
│   ├── model/
│   │   ├── offer/
│   │   │   ├── JobOffer.java
│   │   │   ├── OfferId.java
│   │   │   ├── OfferStatus.java
│   │   │   └── ...
│   │   ├── application/
│   │   │   ├── Application.java
│   │   │   └── ...
│   │   └── candidate/
│   │       └── ...
│   ├── service/
│   ├── event/
│   └── repository/
│
├── application/
│   ├── command/
│   ├── query/
│   ├── usecase/
│   └── dto/
│
├── adapters/
│   ├── in/
│   │   └── rest/
│   └── out/
│       ├── persistence/
│       ├── messaging/
│       └── integrations/
│
└── configuration/
```

La estructura exacta se decidirá después de la auditoría de V1.

---

# 8. Application Layer

La capa Application coordina casos de uso:

```text
CreateJobOffer
PublishJobOffer
ApplyToJobOffer
RejectApplication
SelectCandidate
```

Conceptualmente:

```java
public interface PublishJobOffer {
    void execute(PublishJobOfferCommand command);
}
```

Flujo:

```text
Command
  ↓
Application Service
  ↓
Load Aggregate
  ↓
Execute Domain Behavior
  ↓
Save Aggregate
  ↓
Publish Domain Event
```

La Application Layer **no debe convertirse en un lugar donde se acumulen reglas de negocio**.

---

# 9. Domain Layer

Aquí estará el corazón de ProPractix.

Ejemplo conceptual:

```java
public class JobOffer {

    public void publish() {
        // validar invariantes
        // cambiar estado
        // generar evento de dominio
    }
}
```

No:

```java
jobOffer.setStatus(PUBLISHED);
```

desde cualquier servicio.

La entidad debe controlar sus propias transiciones.

---

# 10. Aggregates

No todas las entidades serán aggregates.

Posibles aggregates iniciales:

```text
JobOffer
Application
Internship
Company
```

La decisión definitiva se tomará mediante las invariantes reales del dominio.

Un aggregate debe proteger invariantes.

Ejemplo:

```text
JobOffer
 ├── title
 ├── description
 ├── status
 ├── publicationDate
 └── ...
```

Regla:

> Una oferta no puede pasar a `PUBLISHED` si no cumple los requisitos mínimos definidos por el dominio.

---

# 11. Value Objects

Se evitará representar conceptos importantes únicamente con tipos primitivos.

En lugar de:

```java
String country;
String currency;
String email;
String internshipType;
```

utilizar conceptos del dominio:

```text
CountryCode
Currency
Email
JurisdictionId
InternshipType
OfferId
CandidateId
InternshipId
```

Esto permite encapsular validaciones y evitar estados inválidos.

---

# 12. Domain Events

Los bounded contexts se comunicarán mediante eventos o contratos explícitos.

Ejemplo:

```text
CandidateSelected
        ↓
Internship context
        ↓
PrepareInternship
```

Otro:

```text
InternshipActivated
        ↓
Notifications
        ↓
Compliance
        ↓
Performance
```

Inicialmente estos eventos pueden ser internos al modular monolith.

**No es necesario introducir Kafka para todo.**

---

# 13. Modular Monolith

La primera arquitectura desplegable será:

```text
                React
                  │
                  ▼
            Spring Boot
                  │
      ┌───────────┴────────────┐
      │                        │
 Recruitment               Internship
      │                        │
 Company                  Compliance
      │                        │
 Performance             Documents
      │                        │
      └───────────┬────────────┘
                  ▼
              PostgreSQL
```

Cada módulo tendrá límites claros.

La posibilidad de evolucionar posteriormente hacia microservicios será una consecuencia de los límites de dominio, no el objetivo inicial.

---

# 14. Regla de dependencias

Dirección deseada:

```text
Adapters
   ↓
Application
   ↓
Domain
```

Nunca:

```text
Domain
   ↓
Spring
   ↓
JPA
   ↓
Kafka
```

El dominio no debe depender de infraestructura.

---

# 15. Persistencia

JPA/Hibernate debe permanecer fuera del dominio siempre que sea razonable.

Ejemplo:

```text
domain
   JobOffer

infrastructure / adapter
   JobOfferJpaEntity

adapter
   JobOfferRepositoryAdapter
```

El repositorio del dominio define la necesidad:

```text
JobOfferRepository
```

La infraestructura implementa:

```text
JpaJobOfferRepository
```

Esto permite que el dominio sea testeable sin levantar PostgreSQL.

---

# 16. España como primera jurisdicción

España será la primera implementación, pero no debe convertirse en una excepción arquitectónica.

Modelo conceptual:

```text
Jurisdiction
 ├── ES
 ├── CO
 └── ...
```

Configuración:

```text
Jurisdiction ES
 ├── locale
 ├── currency
 ├── timezone
 ├── internship types
 ├── compliance rules
 ├── required documents
 ├── working-time rules
 └── legal configuration
```

Colombia será posteriormente otra configuración:

```text
Jurisdiction CO
 ├── locale
 ├── currency
 ├── timezone
 ├── rules
 └── requirements
```

---

# 17. No hardcodear reglas por país

### Evitar

```java
if (country.equals("ES")) {
    requireAgreement();
}
```

### Preferir

```text
CompliancePolicy
       │
       ├── jurisdiction
       ├── internshipType
       └── requirements
```

Y:

```text
requirementsFor(
    jurisdiction,
    internshipType
)
```

España será el primer conjunto de reglas sin acoplar el dominio a España.

---

# 18. Multidioma

Hay que diferenciar tres tipos de información.

## UI

Claves:

```text
offer.create
offer.publish
candidate.reject
internship.activate
```

Idiomas iniciales:

```text
es
en
```

## Datos introducidos por usuarios

Ejemplo:

```text
JobOffer.title
JobOffer.description
```

Son datos de negocio y no deberían tratarse como simples traducciones de UI.

## Catálogos parametrizables

Ejemplo:

```text
DocumentType
InternshipType
ComplianceRequirement
```

Podrán necesitar traducciones:

```text
document_type
----------------
code
jurisdiction
active

document_type_translation
-------------------------
document_type_id
locale
name
description
```

---

# 19. Configuration vs Business Rules

Hay que distinguir:

### Configuration

> "España utiliza EUR."

### Business Rule

> "Una práctica no puede activarse si falta un requisito obligatorio."

La configuración alimenta las reglas, pero no sustituye al dominio.

```text
Configuration
      ↓
Domain Policy
      ↓
Business Decision
```

---

# 20. Entitlements y planes

La V1 contiene conceptos de planes/features.

En V2 se recomienda separar:

```text
Business Domain
      +
Entitlements
      +
Subscription/Billing
```

El dominio no debería estar lleno de:

```java
if (plan == STARTER)
```

La aplicación debería consultar un servicio de capacidades:

```text
EntitlementService
```

Ejemplo:

```text
CanCreateInternship(company)
CanUseAdvancedEvaluation(company)
CanAccessAnalytics(company)
```

Esto desacopla la lógica comercial del dominio operativo.

---

# 21. Roadmap incremental V2

## Incremento 0 — Foundation

Objetivo:

> Crear la base arquitectónica de V2.

Incluye:

- estructura modular
- DDD
- Hexagonal
- CI/CD
- PostgreSQL
- Flyway
- testing
- observabilidad básica
- i18n
- configuración de jurisdicción

**Valor:** base preparada para construir sin repetir los problemas de V1.

## Incremento 1 — Company & Identity

Resultado:

```text
Usuario
  ↓
Login
  ↓
Empresa
  ↓
Contexto de empresa
```

## Incremento 2 — Job Offer

Resultado:

```text
Empresa
  ↓
Crear oferta
  ↓
Publicar oferta
  ↓
Oferta pública
```

## Incremento 3 — Application

Resultado:

```text
Candidato
  ↓
Consulta oferta
  ↓
Aplica
  ↓
Empresa recibe candidatura
```

## Incremento 4 — Selection

Resultado:

```text
Application
  ↓
Screening
  ↓
Interview
  ↓
Selected / Rejected
```

## Incremento 5 — Internship

Resultado:

```text
Candidate Selected
       ↓
Create Internship
       ↓
Internship planned
```

## Incremento 6 — Onboarding

Resultado:

```text
Internship
   ↓
Checklist
   ↓
Documents
   ↓
Requirements
   ↓
Ready to activate
```

## Incremento 7 — Activate Internship

Resultado:

```text
Planned
   ↓
Onboarding
   ↓
Compliance OK
   ↓
Active
```

Este incremento conecta Recruitment + Internship + Compliance.

## Incremento 8 — Internship Management

Incluye inicialmente:

- horas
- objetivos
- seguimiento básico

## Incremento 9 — Evaluation

Incluye:

- evaluaciones
- feedback
- progreso

## Incremento 10 — Compliance España

Implementar las reglas reales de la primera jurisdicción:

```text
ES
 ├── requirements
 ├── documents
 ├── rules
 └── deadlines
```

---

# 22. Criterio para decidir qué entra en cada incremento

Antes de desarrollar una feature debemos responder:

1. ¿Qué usuario obtiene valor?
2. ¿Qué problema resuelve?
3. ¿Qué flujo completo habilita?
4. ¿Puede utilizarse al terminar?
5. ¿Qué parte pertenece realmente al dominio?
6. ¿Qué reglas de negocio existen?
7. ¿Qué depende de la jurisdicción?
8. ¿Qué es infraestructura?
9. ¿Qué queda fuera?

Si no podemos responder estas preguntas, probablemente todavía no está lista para entrar en desarrollo.

---

# 23. Definition of Done

Un incremento de V2 no estará terminado simplemente porque compile.

Debe tener:

```text
✓ Domain model
✓ Use cases
✓ Ports
✓ Adapters
✓ Persistence
✓ API
✓ UI
✓ i18n
✓ Tests unitarios
✓ Tests de integración
✓ Flujo end-to-end
✓ Migración Flyway
✓ Documentación mínima
✓ Observabilidad básica
✓ Definition of Done cumplida
```

Y, sobre todo:

> **Debe poder demostrarse el valor del incremento desde la perspectiva del usuario.**

---

# 24. Testing basado en DDD

La estrategia será:

```text
Domain Tests
      ↓
Application Tests
      ↓
Adapter Tests
      ↓
Integration Tests
      ↓
End-to-End Tests
```

La mayor parte de las reglas de negocio deberían poder probarse sin Spring.

Ejemplo:

```text
Given:
  Offer in DRAFT

When:
  publish()

Then:
  Offer becomes PUBLISHED
```

Y:

```text
Given:
  Internship with mandatory requirement missing

When:
  activate()

Then:
  activation is rejected
```

---

# 25. Integraciones externas

Cualquier integración deberá entrar mediante un port.

Ejemplo:

```text
domain/application
        │
        ▼
DocumentSigner
        ▲
        │
adapter
   DocuSignAdapter
```

El dominio nunca debe conocer:

```text
DocuSign
AWS S3
Kafka
SendGrid
Google Calendar
```

Solo conceptos propios del negocio.

---

# 26. Eventos e integraciones

No introducir tecnología por anticipación.

Primero:

```text
Domain Event
```

Después decidimos si el adapter necesita:

```text
In-memory
Kafka
SNS/SQS
RabbitMQ
```

Por ejemplo:

```text
InternshipActivated
        ↓
Kafka
        ↓
Notifications
Compliance
Analytics
```

solo cuando exista una necesidad real.

---

# 27. Qué conservar de V1

La V1 debe tratarse como **fuente de conocimiento**.

Conservar conceptualmente:

- funcionalidades que aportan valor
- reglas de negocio ya descubiertas
- flujos que funcionan
- necesidades de compliance
- conceptos de configuración
- experiencia adquirida

Pero no asumir:

> "Existe en V1 → debe copiarse a V2."

Cada funcionalidad deberá clasificarse:

```text
KEEP
REFACTOR
MERGE
POSTPONE
REMOVE
REPLACE
```

---

# 28. Auditoría V1 → V2

La siguiente fase debe producir una matriz:

| V1 | Valor | DDD Context | Acción V2 | Incremento |
|---|---:|---|---|---:|
| Job Offer | Alto | Recruitment | Keep/Rebuild | 2 |
| Candidate | Alto | Recruitment | Rebuild | 3 |
| Interviews | Alto | Recruitment | Simplify | 4 |
| Internship | Muy alto | Internship | Rebuild | 5 |
| Compliance | Muy alto | Compliance | Rebuild | 6-10 |
| Analytics | Medio | Reporting | Postpone | Posterior |
| Integrations | Variable | Integrations | Postpone | Posterior |
| Billing | Comercial | Billing | Isolate | Posterior |

La tabla definitiva debe elaborarse a partir de una revisión completa del código V1.

---

# 29. Arquitectura conceptual final

```text
                           PRO PRACTIX
                               │
              ┌────────────────┴────────────────┐
              │                                 │
          PRODUCT                           PLATFORM
              │                                 │
    ┌─────────┼─────────┐             ┌─────────┼─────────┐
    │         │         │             │         │         │
Recruitment Internship Compliance   Identity  Config   Integrations
    │         │         │                       │
    └─────────┼─────────┘                       │
              │                                 │
        Domain Model                       Jurisdictions
              │                                 │
        Application                      ES / CO / ...
              │
        Hexagonal Ports
              │
          Adapters
              │
      PostgreSQL / APIs /
      Messaging / Files
```

---

# 30. Principios finales

### Arquitectura

> **El negocio debe poder evolucionar sin depender de la tecnología.**

### Producto

> **La aplicación debe crecer en profundidad antes que en cantidad de funcionalidades.**

### Incrementos

> **Cada incremento debe dejar una versión desplegable y usable.**

### DDD

> **El código debe expresar el lenguaje del negocio y proteger sus invariantes.**

### Hexagonal

> **La tecnología debe ser reemplazable sin reescribir el dominio.**

### Internacionalización

> **España será la primera jurisdicción, no una excepción especial.**

En resumen:

```text
menos funcionalidades iniciales
        +
mejor modelado
        +
bounded contexts claros
        +
dominio independiente
        +
flujos completos
        +
reglas parametrizables
        =
ProPractix V2 más sólido y evolutivo
```

---

# 31. Próximo paso recomendado

Este documento es el **Blueprint inicial**, no todavía el diseño definitivo.

La siguiente fase debería ser una **auditoría detallada del ZIP V1**, módulo por módulo, para producir:

1. Mapa de bounded contexts.
2. Modelo de dominio V2.
3. Entidades, Value Objects y Aggregates.
4. Matriz V1 → V2.
5. Dependencias entre contextos.
6. Roadmap definitivo de incrementos.
7. Estructura exacta de paquetes Java.
8. Diseño del Incremento 0.
9. Diseño completo del primer flujo funcional.

La intención es que después podamos empezar a programar V2 con una arquitectura decidida, en lugar de ir tomando decisiones estructurales durante cada feature.

---

# Anexo A — Estructura observada del proyecto

La estructura del ZIP se incluye como referencia de la V1 y **no representa la arquitectura objetivo**:

```text
📁 MVP-develop
  📁 .claude
    📄 CLAUDE.md
    📁 commands
      📄 content-review.md
      📄 e2e-test-generator.md
      📄 propractix-bugfix.md
      📄 propractix-feature.md
      📁 references
        📄 backend-module.md
        📄 backend-testing.md
        📄 frontend-page.md
        📄 migrations.md
      📄 requirements-engineer.md
      📄 security-review.md
      📄 test-flow.md
      📄 ux-review.md
    📄 settings.json
    📄 settings.local.json
    📁 skills
      📁 graphify
        📄 .graphify_version
        📄 SKILL.md
        📁 references
  📄 .claudeignore
  📄 .env.example
  📄 .gitattributes
  📁 .github
    📄 copilot-instructions.md
    📁 prompts
      📄 content-review.prompt.md
      📄 e2e-test-generator.prompt.md
      📄 security-review.prompt.md
      📄 ux-ui-review.prompt.md
    📁 skills
      📁 propractix-feature
        📄 SKILL.md
        📁 references
    📁 workflows
      📄 ci.yml
  📄 .gitignore
  📄 .graphifyignore
  📁 .idea
    📄 .gitignore
    📄 misc.xml
    📄 modules.xml
    📄 propractix.iml
    📄 vcs.xml
  📁 .vscode
    📄 mcp.json
    📄 settings.json
  📄 CLAUDE.md
  📄 README.md
  📁 client
    📄 .dockerignore
    📄 Dockerfile
    📄 index.html
    📄 nginx.conf
    📄 package-lock.json
    📄 package.json
    📄 playwright.config.js
    📄 postcss.config.js
    📁 public
      📄 propractix-favicon.svg
      📄 propractix-header-dark.svg
      📄 propractix-hero.svg
      📄 propractix-horizontal.svg
      📄 propractix-vertical.svg
    📁 src
      📄 App.jsx
      📁 api
        📁 __tests__
        📄 adminAccessRequests.js
        📄 adminAuditLog.js
        📄 adminDashboard.js
        📄 adminSettings.js
        📄 analytics.js
        📄 auth.js
        📄 client.js
        📄 company.js
        📄 companySettings.js
        📄 countries.js
        📄 cv.js
        📄 dashboard.js
        📄 documentTemplates.js
        📄 evaluations.js
        📄 integrations.js
        📄 interns.js
        📄 internships.js
        📄 interviews.js
        📄 jurisdictions.js
        📄 legal.js
        📄 meetings.js
        📄 messaging.js
        📄 notifications.js
        📄 objectiveTemplates.js
        📄 objectives.js
        📄 offers.js
        📄 onboarding.js
        📄 pipeline.js
        📄 publicClient.js
        📄 publicOffers.js
        📄 registration.js
        📄 reports.js
        📄 rubrics.js
        📄 sena.js
        📄 studentPublic.js
        📄 subscription.js
        📄 talentPool.js
        📄 timesheets.js
        📄 tutors.js
        📄 universityTutor.js
        📄 universityTutors.js
        📄 workplans.js
      📁 components
        📁 cv
        📁 internships
        📁 landing
        📁 layout
        📁 legal
        📁 pipeline
        📁 plans
        📁 registration
        📁 settings
        📁 ui
      📁 content
        📄 privacyPolicyContent.js
      📁 hooks
        📄 useJurisdiction.js
        📄 useJurisdiction.test.jsx
        📄 useMobile.js
        📄 usePlanStatus.js
        📄 usePlanStatus.test.jsx
      📁 i18n
        📄 index.js
        📁 locales
      📄 index.css
      📄 main.jsx
      📁 pages
        📄 CompanySetup.jsx
        📄 Landing.jsx
        📁 admin
        📁 analytics
        📁 auth
        📁 compliance
        📁 dashboard
        📁 hiring
        📁 legal
        📁 onboarding
        📁 operations
        📁 performance
        📁 public
        📁 reports
        📁 settings
        📁 student
        📁 tutors
        📁 university-tutor
      📁 store
        📁 __tests__
        📄 authStore.js
      📁 test
        📄 setup.js
        📄 utils.jsx
      📁 types
        📄 registration.js
      📁 utils
        📄 apiErrors.js
        📄 apiErrors.test.js
        📄 authRedirect.js
        📄 constants.js
        📄 cvCompleteness.js
        📄 formatters.js
        📄 locale.js
    📄 tailwind.config.js
    📁 tests
      📁 e2e
        📄 auth.setup.js
        📁 fixtures
        📁 pages
        📁 specs
        📁 utils
      📁 tests
        📁 e2e
    📄 vite.config.js
    📄 vitest.config.js
  📁 docker
    📁 localstack
      📄 init-localstack.sh
    📁 minio
      📄 init-minio.sh
  📄 docker-compose.yml
  📁 docs
    📄 starter-plan-features.md
    📁 test-plans
      📄 practica-colombia-starter.md
      📄 sena-aprendiz.md
  📁 graphify-out
    📄 .graphify_labels.json
    📄 .graphify_labels.json.sig
    📄 .graphify_python
    📄 .graphify_root
    📁 2026-08-15
      📄 .graphify_labels.json
      📄 GRAPH_REPORT.md
      📄 cost.json
      📄 graph.json
      📄 manifest.json
    📁 2026-08-16
      📄 .graphify_labels.json
      📄 GRAPH_REPORT.md
      📄 cost.json
      📄 graph.json
      📄 manifest.json
    📄 GRAPH_REPORT.md
    📄 graph.html
    📄 graph.json
    📄 manifest.json
  📄 package.json
  📁 server
    📄 .dockerignore
    📁 .mvn
      📁 wrapper
        📄 maven-wrapper.properties
    📄 Dockerfile
    📄 mvnw
    📄 mvnw.cmd
    📄 pom.xml
    📄 run-local.sh
    📁 src
      📁 main
        📁 java
        📁 resources
      📁 test
        📁 java
        📁 resources
```

# Documentación de ProPractix V2

## Orden de lectura

1. [Blueprint DDD y arquitectura hexagonal](ProPractix-V2-DDD-Hexagonal-Blueprint.md).
2. [Auditoría de V1](ProPractix-V1-Auditoria-Hallazgos-V2-DDD-Hexagonal.md).
3. [Blueprint de dominio y mapas legales de España](ProPractix-V2-Blueprint-Dominio-y-Mapas-Legales-Espana.md).
4. [Especificación técnica para Codex](ProPractix-V2-MVP-Espana-Especificacion-Arquitectura-y-Tecnica-para-Codex.md).
5. [Plan técnico del Incremento 1](plans/increment-01-plan.md).
6. ADR disponibles en [`adr`](adr/); ADR-010 permanece reservado, ADR-011 define OpenTofu/bootstrap AWS y ADR-012 el desarrollo local sin dependencia cloud.
7. [Revisión del bootstrap](reviews/bootstrap-validation.md).
8. [Primera revisión de ADR y plan](reviews/revision-adrs-incremento-01.md).
9. [Resolución de la primera revisión](reviews/resolucion-revision-adrs-incremento-01.md).
10. [Segunda revisión de ADR y plan](reviews/segunda-revision-adrs-incremento-01.md).
11. [Resolución de la segunda revisión](reviews/resolucion-segunda-revision-adrs-incremento-01.md).
12. [Prompt para la revisión final](prompts/review-adrs-increment-01.md).
13. [Revisión final de ADR y plan](reviews/revision-final-adrs-incremento-01.md).
14. [Resolución de la revisión final](reviews/resolucion-revision-final-adrs-incremento-01.md).
15. [Revisión de la política de correo empresarial de V1](reviews/revision-politica-correo-empresarial-v1.md).
16. [Resolución de correo y cloud — revisión 5](reviews/resolucion-correo-cloud-revision-5.md).
17. [Resolución de cloud staging — revisión 6](reviews/resolucion-cloud-staging-revision-6.md).
18. [Resolución de estrategia local/cloud — revisión 7](reviews/resolucion-estrategia-local-cloud-revision-7.md).
19. [Plan operativo de bootstrap AWS](plans/aws-staging-bootstrap-plan.md).
20. [Checklist de CL0 cloud staging](plans/cl0-cloud-staging-checklist.md).
21. [Seed documental de la política de correo empresarial](policies/company-email-admission-seed-v1.yaml).
22. [Fixture/política sintética exclusiva de staging](policies/staging-synthetic-privacy-fixture-v1.yaml).
23. [Aceptación de I1-H01 — Fundación ejecutable mínima](reviews/aceptacion-i1-h01.md).
24. [Dossier de gobierno de C0_CATALOG](plans/c0-catalog-governance.md).
25. [Esquema del catálogo de instituciones académicas v1](catalog/academic-institution-catalog-schema-v1.md).
26. [Fixture sintética del catálogo académico v1](catalog/fixtures/academic-institutions-es-synthetic-v1.csv).
27. [Evaluación de C0_CATALOG](reviews/c0-catalog-assessment.md).

## Estado de decisión

ADR-001, ADR-002 y ADR-004 a ADR-008 tienen estado `Aceptado — revisión 4`; ADR-003, `Aceptado — revisión 5`; ADR-009, `Aceptado — revisión 3`; ADR-011 y ADR-012, `Aceptado — revisión 1`; ADR-010 está reservado para policy packs. El plan del Incremento 1 está aprobado en revisión 7. I1-H01 está `DONE / ACCEPTED` y no debe volver a implementarse; su evidencia se conserva en la [aceptación de I1-H01](reviews/aceptacion-i1-h01.md). El Product Owner aprobó expresamente el alcance documentado de `C0_CATALOG` el 2026-09-21 sobre el commit `e98e11655da7a8f37b6c26d180d6b7ca2d3e8e52`; la [evaluación de C0_CATALOG](reviews/c0-catalog-assessment.md) registra el gate como `CLOSED`. RUCT se limita a búsqueda y autocompletado informativo; la descarga manual controlada satisface la adquisición del MVP. I1-H02 permanece `BLOCKED`, no `READY`, únicamente por `S0_PUBLIC_ENDPOINTS`, que continúa `PENDING`. H01–H05 se aceptan localmente sin AWS según ADR-012. `CL0_CLOUD_STAGING` permanece `PENDING` y solo se cierra después de desplegar y verificar H05 con la fixture sintética exclusiva de staging.

Las conclusiones jurídicas del blueprint continúan sujetas a contraste profesional. Un ADR técnico no convierte una interpretación jurídica en una regla aprobada.

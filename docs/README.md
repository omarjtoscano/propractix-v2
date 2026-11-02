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
28. [Decisión aprobada S0_PUBLIC_ENDPOINTS](plans/s0-public-endpoints.md).
29. [Política de rate limiting de endpoints públicos v1](policies/public-endpoint-rate-limits-v1.yaml).
30. [Runbook de rate limiting](operations/rate-limiting-runbook.md).
31. [Evaluación de S0_PUBLIC_ENDPOINTS](reviews/s0-public-endpoints-assessment.md).
32. [Plan de ejecución de I1-H02 — Catálogo institucional consultable](plans/i1-h02-execution-plan.md).
33. [Evidencia E5A — Fundación del rate limiter](reviews/i1-h02-e5a-rate-limit-foundation-evidence.md).
34. [Evidencia E5B — Persistencia y operación del rate limiter](reviews/i1-h02-e5b-rate-limit-persistence-evidence.md).
35. [Evidencia E5C — Filtro HTTP y borde Caddy](reviews/i1-h02-e5c-http-rate-limit-evidence.md).
36. [Internacionalización y presentación de errores ES/EN](development/i18n-and-errors.md).
37. [Evidencia y aceptación E7 — Cliente generado y UI](reviews/i1-h02-e7-academic-institution-ui-evidence.md).
38. [Evidencia y aceptación E8 — E2E y observabilidad operable](reviews/i1-h02-e8-e2e-observability-evidence.md).
39. [E9 — Primer snapshot RUCT y aceptación local](reviews/i1-h02-first-snapshot-evidence.md).
40. [CSV aprobado RUCT 2026-11-01](catalog/snapshots/ruct-2026-11-01/academic-institutions-es.csv) y [manifest](catalog/snapshots/ruct-2026-11-01/manifest.yaml).
41. [Aceptación local de H02 — DONE / ACCEPTED](reviews/aceptacion-i1-h02.md).
42. [Resultados locales originales E9 (2026-11-01) — publicación y verificaciones](reviews/i1-h02-e9-local-validation.json).
43. [Plan de ejecución de I1-H03 — APPROVED; implementación no autorizada](plans/i1-h03-execution-plan.md).

## Estado de decisión

**Última decisión del PO (2026-11-02):** [plan H03](plans/i1-h03-execution-plan.md) `APPROVED` sobre `27b404c56b0d918e69c20b0319c6cc29daa92b89`, integrado mediante [PR #21](https://github.com/omarjtoscano/propractix-v2/pull/21) / `b398694bf1a68b65c674efa579b9c468a207c464`, confirmado por el PO. Quedan sustituidas sus restricciones históricas de Draft/sin merge y rama. Solo se autoriza preparar las [decisiones pendientes de entrada de E1](plans/i1-h03-execution-plan.md#31-decisiones-de-entrada-de-e1--propuestas-pendientes-de-aprobación) en el mismo plan y sincronizar referencias, con rama documental nueva, enlaces/diff, commit, push y nueva Draft PR hacia el incremento. Las propuestas y una posible autorización local condicionada no están aprobadas; H03/E1 siguen sin iniciar ni autorizar. B0/P0, entrada S0 de H03 y concreción ADR-003/007 antes de E5 siguen pendientes, sin ampliar recuperación de cuentas ni cerrar gates. Sin código, migraciones, políticas publicadas, cambios funcionales H02 ni AWS; CL0 sigue `PENDING`. H02 permanece integrada y `DONE / ACCEPTED` localmente mediante `f387f260acc0b32ed8a85373cf12d45476371ebe` (PR #19), con aceptación funcional y evidencia conservadas. Los checks remotos los verifica e informa el PO, sin espera ni supervisión del agente. Los registros que siguen conservan las condiciones históricas de cada aceptación; las restricciones anteriores de rama/PR #19 no son la autorización vigente.

ADR-001, ADR-002 y ADR-004 a ADR-008 tienen estado `Aceptado — revisión 4`; ADR-003, `Aceptado — revisión 5`; ADR-009, `Aceptado — revisión 3`; ADR-011 y ADR-012, `Aceptado — revisión 1`; ADR-010 está reservado para policy packs. El plan del Incremento 1 está aprobado en revisión 7. I1-H01 está `DONE / ACCEPTED` y no debe volver a implementarse; su evidencia se conserva en la [aceptación de I1-H01](reviews/aceptacion-i1-h01.md). El Product Owner aprobó expresamente el alcance documentado de `C0_CATALOG` el 2026-09-21 sobre el commit `e98e11655da7a8f37b6c26d180d6b7ca2d3e8e52`; la [evaluación de C0_CATALOG](reviews/c0-catalog-assessment.md) registra el gate como `CLOSED`. RUCT se limita a búsqueda y autocompletado informativo; la descarga manual controlada satisface la adquisición del MVP. El Product Owner aprobó expresamente la [decisión S0_PUBLIC_ENDPOINTS](plans/s0-public-endpoints.md) el 2026-09-27 sobre el commit `fec3b2ed3a5afa04d1c079bda4f2d8981efd5920`; S0 está `CLOSED`; I1-H02 está `DONE / ACCEPTED` localmente, con E1–E9 `ACCEPTED` por aprobación expresa del PO del 2026-11-02 sobre `9c3c6cdb3ad44fb4b289208f073a1f76694cbf84`. El Product Owner confirmó la integración de E4 mediante `9d128cc3df8c2a90177b1f18cea47708d8abfc03` el 2026-10-22 y autorizó exclusivamente E5A en `feature/i1-h02-e5a-rate-limit-foundation`. El Product Owner aprobó E5A el 2026-10-23 sobre `6cc21e6c034b9c32db3f22ebb8b977f4250614cd`; su estado es `ACCEPTED` y su integración fue confirmada por el Product Owner el 2026-10-23 mediante `8f8f1b0bda8c9d066ba4751272939b4ae5cf6590` (PR #12 integrada). E5B está `ACCEPTED` por aprobación expresa del Product Owner el 2026-10-23 sobre `53d15ffb779d2b8e2a6680cba0388bddd3cebd30` en `feature/i1-h02-e5b-rate-limit-persistence`; la PR #13 está integrada mediante `7bd7bf6b4191d92cfe39864333ce5b5c3467115a`, confirmado por el Product Owner el 2026-10-23; E5C está `ACCEPTED` por aprobación expresa del Product Owner el 2026-10-23 sobre `8a0a46a293b9b2d9482e92c05161f8b7b57b04e2`; E6 está `ACCEPTED` sobre `de269313888d912807536b5805e9e2c054955347` e integrada mediante `11dda0c9fb267b619c98baabd12465e143c43d53`, confirmado por el Product Owner el 2026-10-25. E7 está `ACCEPTED` por aprobación expresa del Product Owner el 2026-10-26 sobre `d1db0ddf925a441ba8370d9a10b1f6cac5ffeff2` en `feature/i1-h02-e7-academic-institution-ui`, con la excepción F0 de ADR-001 (TypeScript 6.0.3 aislado para el generador, aplicación 7.0.2). La PR #16 está integrada mediante `63034a15c6f3013ac6e826a3171234eef32b7740`, confirmado por el Product Owner el 2026-10-26. CI, Trivy y Build OCI están en verde sobre el SHA funcional aprobado de E7, según confirmación expresa del Product Owner; los checks del commit documental de aceptación de E8 quedan pendientes de su verificación, sin espera ni supervisión por el agente. El análisis local de SonarQube for IDE no acredita un Quality Gate remoto verde. E8 está `ACCEPTED` por aprobación expresa del Product Owner el 2026-11-01 sobre `fc9660e997df990d08677f6a55d91d57f92ef9c7`; CI, Trivy y Build OCI en verde sobre ese SHA, confirmados expresamente por el Product Owner. La PR #17 está integrada mediante `866c4c71511c181564b8f8d165c06759fcb4442b`; el PO confirma la PR #18 integrada mediante `3d339d1598c4a7deafedea3d4d9d78fc16c87088`. E9 está `ACCEPTED` e I1-H02 `DONE / ACCEPTED` localmente por aprobación expresa del PO el 2026-11-02 sobre `9c3c6cdb3ad44fb4b289208f073a1f76694cbf84`. El PO confirma CI, Trivy y Build OCI en verde sobre ese SHA. ChatGPT realizó la revisión técnica asistida en este chat, con contraste del mapeo el 2026-11-01 y revisión final favorable el 2026-11-02, diferenciada de la aprobación PO. Se conservan el CSV aprobado, código y resultados originales de ejecución. Aquella aceptación solo autorizaba registro documental y cuerpo de PR #19, commit y push en la misma rama; entonces la PR permanecía Draft y sin merge, H03 no autorizada, sin AWS. Su integración posterior y la autorización exclusiva del plan H03 constan arriba. Los checks del nuevo SHA documental los verifica el PO sin espera ni supervisión del agente. La aprobación solo autoriza `searchAcademicInstitutions`: cada endpoint posterior necesita su propia entrada revisada y aprobada. H01–H05 se aceptan localmente sin AWS según ADR-012. `CL0_CLOUD_STAGING` permanece `PENDING` y solo se cierra después de desplegar y verificar H05 con la fixture sintética exclusiva de staging.

Las conclusiones jurídicas del blueprint continúan sujetas a contraste profesional. Un ADR técnico no convierte una interpretación jurídica en una regla aprobada.

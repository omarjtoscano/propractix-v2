# Documentación de ProPractix V2

## Orden de lectura

1. [Blueprint DDD y arquitectura hexagonal](ProPractix-V2-DDD-Hexagonal-Blueprint.md).
2. [Auditoría de V1](ProPractix-V1-Auditoria-Hallazgos-V2-DDD-Hexagonal.md).
3. [Blueprint de dominio y mapas legales de España](ProPractix-V2-Blueprint-Dominio-y-Mapas-Legales-Espana.md).
4. [Especificación técnica para Codex](ProPractix-V2-MVP-Espana-Especificacion-Arquitectura-y-Tecnica-para-Codex.md).
5. [Plan técnico del Incremento 1](plans/increment-01-plan.md).
6. ADR-001 a ADR-009 en [`adr`](adr/), incluido el despliegue cloud incremental en AWS.
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
17. [Seed documental de la política de correo empresarial](policies/company-email-admission-seed-v1.yaml).

## Estado de decisión

ADR-001, ADR-002 y ADR-004 a ADR-008 tienen estado `Aceptado — revisión 4`; ADR-003, `Aceptado — revisión 5`; ADR-009, `Aceptado — revisión 1`. El plan del Incremento 1 está aprobado en revisión 5 y autoriza únicamente I1-H01. `CL0_CLOUD_STAGING` no bloquea preparar la baseline, pero sí declarar H01 desplegada/cerrada; las historias posteriores siguen bloqueadas por sus gates y predecesoras.

Las conclusiones jurídicas del blueprint continúan sujetas a contraste profesional. Un ADR técnico no convierte una interpretación jurídica en una regla aprobada.

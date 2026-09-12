# Prompt de segunda revisión — ADR y plan del Incremento 1

Usar desde la raíz del repositorio con la extensión de Codex:

> Lee `AGENTS.md`, la especificación técnica, el blueprint legal, `docs/reviews/revision-adrs-incremento-01.md`, `docs/reviews/resolucion-revision-adrs-incremento-01.md`, ADR-001 a ADR-008 revisados y `docs/plans/increment-01-plan.md`. No modifiques código. Verifica específicamente que B-01 y B-02 estén resueltos, que cada tabla tenga propietario, que tenancy/identidad sean coherentes, que ML-15 bloquee producción sin impedir la fundación, que cada historia cumpla Definition of Ready y que no se hayan introducido contradicciones nuevas. Evalúa también la incorporación propuesta del contexto `student`. Devuelve: 1) bloqueos restantes con referencia de archivo; 2) riesgos aceptables y mitigaciones; 3) ADR que pueden pasar individualmente a `Aceptado`; 4) correcciones exactas aún necesarias. No cambies estados y no implementes historias.

Después de resolver las observaciones, el propietario podrá autorizar expresamente el cambio de estado de cada ADR de `Propuesto` a `Aceptado`.

# Prompt de revisión final — ADR y plan del Incremento 1

Usar desde la raíz del repositorio con la extensión de Codex:

> Lee `AGENTS.md`, la especificación técnica, el blueprint legal, las dos revisiones y sus resoluciones, ADR-001 a ADR-008 en revisión 3 y `docs/plans/increment-01-plan.md`. Verifica: 1) que SR-B01–SR-B06 estén resueltos; 2) que contraseña, correo y PII no crucen outbox o contextos de forma insegura; 3) que la entrega `identity -> notifications` sea unidireccional y no exista ciclo; 4) que D-005/D-006, roles, empresa duplicada, tenancy y roadmap sean coherentes; 5) que país, locale y jurisdicción estén separados, sin fallback legal ni condicionales por país; 6) que toda etiqueta visible esté gobernada por i18n; 7) que cada tabla/contrato tenga owner; 8) que los ADR separen aceptación documental y conformidad de implementación; 9) que H01 pueda comenzar al aceptar sus ADR y H02–H07 permanezcan protegidas por sus gates. Guarda únicamente el informe en `docs/reviews/revision-final-adrs-incremento-01.md`, con bloqueos restantes y evidencia, riesgos residuales, ADR que recomiendas aceptar individualmente y correcciones exactas. No modifiques ningún otro archivo, no cambies estados y no implementes historias.

Después de resolver las observaciones, el propietario podrá autorizar expresamente el cambio de estado de cada ADR de `Propuesto` a `Aceptado`.

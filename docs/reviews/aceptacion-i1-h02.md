# Aceptación de I1-H02 — Catálogo institucional consultable

- Fecha de preparación: 2026-11-01.
- Estado del documento: `DRAFT / PENDING_REVIEW_AND_ACCEPTANCE`.
- Estado de I1-H02: `IN_PROGRESS`.
- E1–E8: `ACCEPTED`; E9: `IN_PROGRESS`, fase de preparación para segunda revisión.
- Aceptación expresa de H02: **pendiente**. Persona y fecha: **pendientes**.
- Rama candidata: `feature/i1-h02-e9-ruct-first-snapshot`.
- SHA de esta entrega y Draft PR: se registran en la PR y el mensaje de entrega.

## Evidencia disponible

| Elemento | Resultado y procedencia |
|---|---|
| C0 y S0 | `CLOSED`, conservados; no se reabren ni amplían. |
| E1–E7 | Aceptaciones registradas por el PO en el [plan H02](../plans/i1-h02-execution-plan.md). |
| E8 | `ACCEPTED` sobre `fc9660e997df990d08677f6a55d91d57f92ef9c7`; [evidencia E8](i1-h02-e8-e2e-observability-evidence.md). |
| Integración E8 | PR #17 mediante `866c4c71511c181564b8f8d165c06759fcb4442b`, contrastada en Git/GitHub. |
| Integración PR #18 | `3d339d1598c4a7deafedea3d4d9d78fc16c87088`, confirmada expresamente por el PO. |
| Adquisición RUCT | Exportación oficial del 2026-11-01, 109 registros fuente. |
| CSV candidato | 99 universidades, 10 agrupaciones omitidas, 0 duplicados, 90 claves con ceros iniciales. |
| Hash exacto del CSV | `ad2a9ca6f05dfe35851dcc00f3cf568e034d64db5b63d4009a2029fe78633b04` |
| Hash del original temporal | `63e3d2bdff91bb780521a7a5354f6e2e1d7338b75f2d8300627032409024199a` |
| Parser/dry run | `catalog_artifact_valid`, salida 0, 99 candidatos, sin publicación. |
| Validación local de esta fase | `server:test` y `server:verify`: 317 pruebas cada uno, 0 fallos, 0 errores, 1 benchmark omitido; hashes y reproducción correctos. |
| Segunda revisión del mapeo | `PENDING_REVIEW`; persona, fecha, hash revisado y decisión sin registrar. |
| Publicación local del RUCT real | `NOT_PUBLISHED`; no autorizada en esta fase. |
| Propuesta UI ES/EN | Documentada, pendiente de revisión e implementación autorizada. |
| CI, Trivy y Build OCI de esta entrega | Pendientes de verificación del PO, sin espera ni supervisión por el agente. |

La [evidencia del primer snapshot](i1-h02-first-snapshot-evidence.md) contiene
procedencia, transformación reproducible, recuentos, omisiones, validaciones y
muestra propuesta para la segunda persona. El [manifest candidato](../catalog/snapshots/ruct-2026-11-01/manifest.yaml)
conserva los campos de revisión/publicación pendientes.

## Condiciones todavía pendientes para aceptar H02

1. Registrar la segunda revisión del mapeo sobre el hash exacto candidato,
   contrastando fuente, muestra, omisiones, duplicados y ambos hashes.
2. Obtener autorización para continuar y publicar localmente mediante la CLI
   existente; registrar import ID, hash, recuentos y única versión visible.
3. Verificar búsquedas del snapshot real y conservación de la última versión
   válida ante una importación inválida, conforme al plan.
4. Resolver e implementar el ajuste mínimo revisado del aviso de UI, conservar
   ES/EN y alcance informativo y aportar sus comprobaciones.
5. Completar la evidencia de validación requerida por el plan para la entrega
   final y registrar la verificación remota comunicada por el PO.
6. Recibir aceptación expresa del PO para E9 y H02 sobre un SHA concreto.

La aceptación previa de E8, las suites existentes con datos sintéticos y el dry
run del CSV real no completan esas condiciones. Este documento no asigna
`ACCEPTED` a E9 ni `DONE` a H02 y no habilita historias posteriores.

## Alcance cloud y funcional

Aceptación local según ADR-012; AWS no se utiliza.
`CL0_CLOUD_STAGING` permanece `PENDING`. Sin cambios en S0, migraciones,
OpenAPI ni generador. El catálogo sigue siendo exclusivamente informativo y no
acredita afiliación, matrícula, elegibilidad, convenio ni cumplimiento.

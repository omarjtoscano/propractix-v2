# Resolución de la segunda revisión de ADR y plan

- Fecha: 2026-09-12
- Estado: Lista para revisión final
- Alcance: documentación; no acepta ADR ni autoriza implementación
- Informe de origen: `segunda-revision-adrs-incremento-01.md`

## Decisiones confirmadas

1. `student` es un bounded context propio; D-006 alinea especificación, blueprint y roadmap.
2. H03 no recopila contraseña; H04 verifica correo y crea la credencial directamente en `identity`.
3. El primer miembro empresarial usa `COMPANY_OWNER`.
4. Una coincidencia empresarial nunca produce claim o enlace automático; exige autenticación o caso administrativo.
5. País empresarial, locale y jurisdicción legal son conceptos separados según D-005.
6. El fallback de interfaz es configurable, inicialmente `es`; no existe fallback jurídico.
7. Países, locales y correspondencias son configuración validada, y toda etiqueta visible se obtiene de i18n.
8. I1-H08 se sustituye por `Gate R1`, porque el cierre no es una historia funcional.

## Resolución de bloqueos

| ID | Resolución | Evidencia principal |
|---|---|---|
| SR-B01 | Resuelta con onboarding passwordless: el secreto solo llega a `identity` al completar una verificación de propósito único. | ADR-003, ADR-005, ADR-007 y plan H03/H04. |
| SR-B02 | Resuelta mediante D-006 y actualización simultánea de especificación, blueprint y calendario I1/I3. | Blueprint D-006, especificación §§9/11/30 y ADR-001. |
| SR-B03 | Resuelta sin introducir el ciclo sugerido: `organization/student` consultan `compliance`; un handler de `identity` prepara destinatario/JWS y llama unidireccionalmente a `notifications`. No hay tabla de notifications en I1. | ADR-001, ADR-005, ADR-006 y plan §§4–7. |
| SR-B04 | Resuelta con `COMPANY_OWNER`, duplicado fiscal no enumerador y separación país–locale–jurisdicción. El país no se fija a España, pero otra jurisdicción no obtiene políticas españolas. | D-005, ADR-003/006 y plan H03. |
| SR-B05 | Resuelta estructuralmente con gates y owners; cada historia define estado, contrato, invariantes, fallos, migración, criterios y pruebas. Las aprobaciones externas permanecen como gates anteriores a la historia afectada. | Plan §§2 y 7. |
| SR-B06 | Resuelta separando en cada ADR condiciones documentales de aceptación y conformidad futura de la implementación. | ADR-001 a ADR-008. |

## Ajuste a la recomendación de entrega

No se añade `notifications -> identity`, porque produciría un ciclo junto a `identity -> notifications`. El relay entrega `VerificationId` a un handler de `identity`; este comprueba estado, obtiene el correo, genera el JWS y llama a `NotificationDeliveryPort` con datos transitorios. El adapter de correo no persiste ni registra destinatario, enlace o token.

## Gates externos visibles

- `C0_CATALOG`: Producto + Datos + Legal/licencias deben aprobar fuente, licencia, formato y publicación antes de I1-H02.
- `P0_PRIVACY_ABUSE`: Producto + Privacidad/Legal + Seguridad + Operaciones deben aprobar ML-15, retención y límites antes de registros con datos reales.
- `E0_EMAIL`: Arquitectura + Operaciones + Seguridad deben aprobar adapter, remitente, templates i18n y operación antes de I1-H04.

Estos gates no autorizan inventar decisiones durante una historia. Pueden prepararse en paralelo con H01 una vez aceptados los ADR aplicables.

## Estado de aprobación

Todos los ADR permanecen en `Propuesto — revisión 3`. La revisión final debe recomendar cuáles pueden pasar individualmente a `Aceptado`; solo el propietario puede autorizar ese cambio. No se ha implementado código funcional.

# Resolución de la primera revisión de ADR y plan

- Fecha: 2026-09-12
- Estado: Lista para segunda revisión
- Alcance: documentación; no autoriza implementación
- Informe de origen: `revision-adrs-incremento-01.md`

## Decisiones de producto confirmadas

1. Verificar correo activa la cuenta; la empresa queda `SELF_DECLARED`.
2. La verificación empresarial es independiente y se exige antes de publicar ofertas.
3. Una cuenta global puede actuar como estudiante y pertenecer a una o varias empresas.
4. El catálogo inicial contiene universidades; la titulación es texto declarado.
5. La fundación puede avanzar con ML-15 pendiente, pero producción no recibe registros reales sin aviso aprobado.

## Resolución de observaciones

| ID | Resolución | Evidencia principal |
|---|---|---|
| B-01 | Resuelta mediante `CompanyOnboarding` durable, transacciones por contexto y estados recuperables. | ADR-001, ADR-005 y plan §§6–7. |
| B-02 | Resuelta: outbox conserva `VerificationId`; el delivery genera JWS de propósito único sin PII ni token persistido. | ADR-005 y ADR-007. |
| A-01 | Resuelta con estados independientes de cuenta y empresa. | ADR-007 y plan I1-H04. |
| A-02 | Resuelta: `TenantId` es el ID de Company, cuenta global, membresías múltiples y tenant activo. | ADR-003. |
| A-03 | Resuelta arquitectónicamente; aprobación jurídica sigue como puerta de producción. | ADR-008 y plan §2. |
| A-04 | Resuelta con HMAC, claim atómico, estados, TTL y prohibición de datos sensibles. | ADR-008. |
| A-05 | Resuelta con RS256, rotación, claims, cookies, CSRF/origin, frescura y rate limiting previo. | ADR-007. |
| A-06 | Resuelta mediante `platform` técnico y matriz tabla→propietario. | ADR-001 y ADR-006. |
| A-07 | Resuelta ampliando cada historia con actor, propietario, estados, contratos, invariantes, errores, eventos, migración y pruebas. | Plan §7. |
| A-08 | Resuelta: cada historia funcional incluye el corte UI/API/OpenAPI/i18n/tests. | ADR-008 y plan §7. |
| M-01 | Resuelta: ninguna implementación no desechable comienza con ADR aplicable propuesto. | Plan §2. |
| M-02 | Resuelta: sin catálogo canónico de titulaciones; declaración textual. | Plan I1-H02/I1-H07. |
| M-03 | Resuelta con mecanismos CAS/locking específicos y pruebas concurrentes. | ADR-004. |
| M-04 | Resuelta: baseline versionada y foundation absorbida como I1-H01 habilitadora, no release. | ADR-001 y plan I1-H01. |
| M-05 | Resuelta: `correlationId` es contrato público; `traceId` queda interno. | ADR-008. |

## Corrección adicional propuesta

Se propone añadir el bounded context `student`, ausente en el inventario original, para que `identity` no posea perfil ni declaraciones académicas. Si ADR-001 se acepta, deberá actualizarse la especificación técnica en el commit de aceptación.

## Pendientes que no se ocultan

- Fuente y licencia del catálogo antes de aceptar I1-H02.
- Texto, base jurídica y retención de ML-15 antes de registros productivos.
- Umbrales de rate limiting según topología de despliegue.
- Proveedor de correo como decisión de adapter.

Estos pendientes tienen puertas explícitas y no impiden revisar la coherencia arquitectónica de los ADR.

## Regla de aprobación

La segunda revisión puede recomendar ADR individuales para aceptación. El estado solo cambiará tras autorización expresa del propietario; aceptar un ADR no valida textos jurídicos pendientes.

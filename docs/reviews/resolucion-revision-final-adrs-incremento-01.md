# Resolución de la revisión final de ADR y plan del Incremento 1

- Fecha: 2026-09-12
- Revisión resuelta: `docs/reviews/revision-final-adrs-incremento-01.md`
- Resultado: bloqueos documentales corregidos
- ADR resultantes: ADR-001 a ADR-008 `Aceptado — revisión 4`
- Plan resultante: `Aprobado — revisión 4`
- Autorización de implementación: únicamente I1-H01

## 1. Resultado ejecutivo

Se aceptan los cuatro bloqueos BF-01 a BF-04 de la revisión final y se incorporan sus correcciones. También se aplican tres precisiones detectadas al revisar el informe:

1. ADR-002 no se acepta de forma aislada hasta completar el registro físico de contratos de ADR-001; ambos quedan corregidos y aceptados en la misma revisión.
2. `S0_PUBLIC_ENDPOINTS` cubre también I1-H02 mientras el catálogo público conserve rate limiting.
3. H03 y H06 sustituyen `privacyNoticeVersion` por `privacyNoticeReference { noticeId, version }`; ambos valores son no confiables hasta que el servidor los contraste.

Antes de cerrar esta resolución, Producto añadió D-007: el autorregistro empresarial no admite dominios de correo público/común. Se incorpora como política versionada y configurable, sin tratarla como verificación de la empresa.

La aceptación de los ADR adopta decisiones arquitectónicas. No declara satisfechas sus pruebas de conformidad ni cierra ML-15, C0, P0, S0 o E0.

## 2. Resolución de bloqueos

| ID | Estado | Resolución aplicada |
|---|---|---|
| BF-01 | `RESOLVED` | ADR-003/006 y el plan incorporan `organization_company_tax_fingerprint`, una fila por empresa/versión HMAC activa, escritura y consulta de todas las versiones, backfill y pruebas de rotación/concurrencia. |
| BF-02 | `RESOLVED` | ADR-006/008 y H03/H06 tratan la referencia enviada como no confiable, resuelven el aviso vigente en servidor, persisten solo el resultado autoritativo y devuelven `privacy_notice_changed` cuando corresponde. |
| BF-03 | `RESOLVED` | ADR-001/005 registran `DeliverNotification` como puerto de entrada de `notifications` y `HandleEmailVerificationDeliveryRequested` como puerto de entrada de `identity`; `configuration` realiza el wiring y `platform` permanece genérico. |
| BF-04 | `RESOLVED` | El plan añade matriz historia→ADR, F0 con baseline exacta, S0 separado, `429`/`Retry-After` por historia y E0 ampliado con Privacidad/Legal. |

## 3. Decisiones cerradas

### 3.1 Aviso sustituido

El servidor es autoritativo. Si el aviso cambió entre render y submit:

- responde `409 privacy_notice_changed`;
- no escribe onboarding, evidencia, idempotencia derivada del body ni auditoría con PII;
- la UI recupera y muestra el aviso vigente;
- el usuario debe confirmarlo expresamente; no existe aceptación automática.

### 3.2 Datos antes de P0

Se permiten únicamente fixtures sintéticas marcadas como no productivas en local/test. Staging o producción no reciben datos personales de registro hasta cerrar `P0_PRIVACY`.

### 3.3 Identificador fiscal

El blind index HMAC y el cifrado usan claves diferentes. Durante una rotación, todas las versiones activas se consultan y se escriben hasta completar y verificar el backfill. Una colisión conserva la respuesta pública genérica y nunca autoriza claim o lectura cross-tenant.

### 3.4 Entrega de correo

`identity` posee la preparación del enlace y consume la capacidad pública de `notifications`. Destinatario y enlace solo existen transitoriamente. `notifications` no consulta `identity`, y el relay técnico no depende de ningún contexto concreto.

### 3.5 Baseline y endpoints públicos

F0 queda cerrado con versiones exactas y política de actualización. S0 permanece pendiente hasta que Seguridad y Operaciones definan valores por operación y demuestren fail-closed, pseudonimización, métricas, alertas y respuestas `429`/`Retry-After`.

### 3.6 Creación frente a aprobación empresarial

I1-H03 a H05 cubren autorregistro, verificación del correo, creación de credencial, login y acceso a la empresa. La compañía queda `SELF_DECLARED`.

La verificación o aprobación empresarial no forma parte del Incremento 1. Se diseñará e implementará en el Incremento 2 y será obligatoria antes de `G1_PUBLICATION`. Verificar un correo nunca convierte la empresa en `VERIFIED`.

### 3.7 Correo empresarial

H03 evalúa el dominio literal normalizado después de `@` mediante `CompanyEmailAdmissionPolicy`. Los dominios públicos configurados se bloquean; un dominio corporativo propio no se bloquea por estar alojado en Google Workspace, Microsoft 365 u otro proveedor.

La política es versionada, configurable y fail-closed. Sus códigos son estables y el texto se obtiene de i18n. La lista observada en V1 se revisará antes de cerrar `B0_COMPANY_EMAIL_POLICY`; no se copiará código V1 ni se asumirá que sigue completa.

## 4. Estado de gates

| Gate | Estado | Consecuencia |
|---|---|---|
| `D0_DECISIONS` | `CLOSED` | I1-H01 puede comenzar. |
| `F0_CLIENT_BASELINE` | `CLOSED` | Baseline preparada, pero H02 sigue esperando C0 y S0. |
| `C0_CATALOG` | `PENDING` | H02 bloqueada. |
| `B0_COMPANY_EMAIL_POLICY` | `PENDING` | H03 bloqueada hasta aprobar la política y contrastar V1 si está disponible. |
| `P0_PRIVACY` | `PENDING` | H03/H06 bloqueadas para datos reales. |
| `S0_PUBLIC_ENDPOINTS` | `PENDING` | Ningún endpoint público de H02-H06 puede exponerse. |
| `E0_EMAIL` | `PENDING` | H04 no puede enviar correo real. |

## 5. Próximo paso autorizado

Iniciar únicamente I1-H01 — Fundación ejecutable mínima. Antes de modificar código, el agente debe presentar el plan técnico breve de H01, confirmar sus criterios y mantener fuera de alcance las tablas de negocio, registros, autenticación y frontend funcional.

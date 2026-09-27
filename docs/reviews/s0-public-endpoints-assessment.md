# Evaluación de S0_PUBLIC_ENDPOINTS

## Dictamen

| Elemento | Resultado |
|---|---|
| Gate | `S0_PUBLIC_ENDPOINTS` |
| Fecha de evaluación | 2026-09-21 |
| Estado | **`READY_FOR_OWNER_APPROVAL`** |
| Owners técnicos | Seguridad + Operaciones |
| Aprobación pendiente | Product Owner sobre un commit concreto |
| Operación evaluada | `searchAcademicInstitutions` |
| Endpoint | `GET /api/v1/academic-institutions?query=&cursor=&limit=` |
| `I1-H02` | **`BLOCKED`** |
| `C0_CATALOG` | **`CLOSED`** |
| Código implementado | Ninguno |

La propuesta es coherente con ADR-001, ADR-004, ADR-006, ADR-007, ADR-008 y
ADR-012. No fue necesario modificar ADR-007 ni ADR-008: ambos ya exigen la
propiedad `platform.ratelimit`, configuración versionada, Problem Details,
`rate_limited`, `correlationId`, PostgreSQL y cierre del gate por operación.

## Cobertura de la decisión

| Requisito | Resultado y evidencia |
|---|---|
| Identificador estable | `searchAcademicInstitutions`, reservado también como futuro `operationId` OpenAPI de H02. |
| Límite y ventana | 60/60 s y regla adicional 15/10 s para ráfagas. |
| Ráfagas | Sin crédito acumulable; la petición debe superar ambas ventanas. |
| Pseudonimización | HMAC-SHA-256 con separación de dominio y clave externa; identificador seudonimizado, no dato anónimo. |
| Proxies | Confianza conjunta en CIDR y saneamiento probado de Caddy; local usa `REMOTE_ADDRESS` y Caddy selecciona exactamente una cabecera. |
| IPv4/IPv6 | IPv4 `/32`, IPv6 `/64`, IPv4-mapped tratada como IPv4. |
| Rotación | Lectura multiversión, escritura nueva y solapamiento mínimo de 180 s. |
| TTL | Expiración lógica a 70/120 s, borrado físico en el siguiente ciclo normal y alerta+incidencia por umbral. |
| Fail-closed | `503 rate_limiter_unavailable`; el caso de uso no se invoca. |
| Respuesta de límite | `429 application/problem+json`, `rate_limited`, `correlationId` y `Retry-After`. |
| Configuración | Artefacto YAML tipado/versionado; sin límites en Java/TypeScript. |
| Propiedad | `platform.ratelimit`, tabla propia y acceso detrás de puerto técnico. |
| Persistencia | PostgreSQL 16; Redis, terceros y AWS excluidos. |
| Métricas | Allowed, rejected y adapter errors con allowlist específica por métrica, sin PII/fingerprint. |
| Alertas/runbook | Umbrales absolutos+ratio y runbook de triage, rotación y recuperación. |
| Pruebas | Matriz completa definida en el plan S0 para ejecución dentro de H02. |

## Revisión de proporcionalidad

La doble ventana fija añade una sola tabla y una operación transaccional a la
base ya aprobada. Evita Redis, coordinación distribuida, reputación externa,
device fingerprinting y una consola administrativa. Es suficiente para una
única instancia y el volumen inicial del catálogo.

La ventana corta reduce picos de autocompletado; la sostenida limita scraping
básico. El `/64` de IPv6 dificulta evadir la cuota con direcciones temporales,
mientras el límite relativamente amplio reduce el impacto esperado de NAT
compartido en IPv4. Los valores deben validarse con pruebas de H02 y observarse
después de una exposición autorizada; no se autoajustan.

## Seguridad y privacidad

- No se almacena la IP ni su forma normalizada.
- El HMAC usa secreto externo y separación de dominio; un hash sin clave queda
  expresamente prohibido.
- El fingerprint continúa siendo un identificador seudonimizado sujeto a
  protección; no se declara anónimo ni fuera del ámbito de privacidad.
- Su finalidad exclusiva es prevención de abuso y se prohíbe reutilizarlo para
  cualquier otra finalidad o correlacionarlo con otros datos.
- La política no contiene material criptográfico, credenciales ni datos reales.
- Los headers del cliente no alteran la identidad: Caddy elimina ambas
  cabeceras de entrada y reconstruye solo la configurada a partir del salto de
  red observado. El CIDR de Caddy sin saneamiento probado no confiere confianza.
- La indisponibilidad nunca convierte una petición en admitida.
- Métricas y alertas no permiten consultar sujetos concretos.
- La rotación conserva cuota durante el solapamiento y no necesita backfill de
  datos efímeros.
- La expiración lógica ocurre a 70/120 segundos y el borrado físico en el
  siguiente ciclo normal; superar 10 000 filas expiradas o 15 minutos de
  antigüedad genera alerta e incidencia, no ampliación de retención.
- Una revisión de privacidad precede la exposición productiva pública, pero no
  bloquea la aceptación local de H02.

## Evidencia obligatoria de proxy

H02 debe probar antes de exposición:

1. conexión directa local con `REMOTE_ADDRESS` e ignorado de ambas cabeceras;
2. perfil Caddy con exactamente una de `FORWARDED` o `X_FORWARDED_FOR`;
3. cliente externo que envía `X-Forwarded-For` falsificado a Caddy;
4. eliminación de ambas cabeceras recibidas y reconstrucción de la seleccionada;
5. IP efectiva procedente del salto observado por Caddy;
6. cadena con varios proxies confiables resuelta de derecha a izquierda;
7. cadena malformada con fallo cerrado;
8. cabecera enviada directamente a un peer no confiable e ignorada.

## Coherencia de estados

| Elemento | Estado tras esta propuesta | Motivo |
|---|---|---|
| `S0_PUBLIC_ENDPOINTS` | `READY_FOR_OWNER_APPROVAL` | La decisión y evidencia documental están preparadas, pero falta aprobación expresa. |
| `I1-H01` | `DONE / ACCEPTED` | No se modifica ni reimplementa. |
| `C0_CATALOG` | `CLOSED` | Se conserva la aprobación previa. |
| `I1-H02` | `BLOCKED` | S0 aún no está `CLOSED`; no se ha implementado ninguna prueba ni endpoint. |
| H03-H06 | Sin autorización nueva | No tienen entradas de política ni valores aprobados. |

El cierre futuro de S0 no será una autorización general. Solo permitirá que H02
implemente y pruebe `searchAcademicInstitutions` con esta entrada. Cada endpoint
nuevo deberá incorporar y aprobar su propia política antes de exposición.

## Riesgos residuales y aceptación requerida

1. NAT compartido puede agrupar personas bajo una IPv4; se acepta inicialmente
   por no introducir tracking adicional.
2. Agrupar IPv6 por `/64` puede unir varios dispositivos de una red; es una
   defensa deliberada frente a direcciones temporales.
3. PostgreSQL es dependencia de disponibilidad: el fail-closed sacrifica
   disponibilidad del catálogo cuando el control no puede decidir.
4. Las ventanas fijas tienen bordes; la ventana de ráfaga limita el pico máximo
   sin exigir un algoritmo más complejo.
5. Los valores son iniciales y necesitarán evidencia operativa antes de una
   futura revisión, siempre mediante versión y aprobación.
6. La seudonimización reduce exposición, pero no elimina obligaciones de
   privacidad; la revisión previa a exposición productiva conserva ese control
   sin convertirlo en gate de aceptación local.

Estos riesgos son explícitos, reversibles mediante una política posterior y
proporcionales al MVP.

## Verificaciones documentales requeridas

Antes de solicitar aprobación se debe demostrar:

- YAML válido y sin alias/configuración ambigua;
- enlaces locales resolubles;
- ausencia de secretos y datos personales;
- diff limitado a documentación autorizada;
- estados exactamente `READY_FOR_OWNER_APPROVAL`, `BLOCKED` y `CLOSED`;
- ninguna modificación de Java, TypeScript, OpenAPI, migraciones, Docker,
  workflows o infraestructura.

## Conclusión

Seguridad y Operaciones pueden elevar esta propuesta al Product Owner. El gate
queda **`READY_FOR_OWNER_APPROVAL`**, no `CLOSED`. La aprobación debe referirse a
un commit concreto y registrarse después; hasta entonces I1-H02 continúa
**`BLOCKED`**. No se implementa código, no se inicia la historia y no se realiza
ninguna operación AWS.

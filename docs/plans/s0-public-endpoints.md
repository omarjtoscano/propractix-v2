# Decisión propuesta — S0_PUBLIC_ENDPOINTS

## Estado y alcance

| Campo | Valor |
|---|---|
| Gate | `S0_PUBLIC_ENDPOINTS` |
| Fecha | 2026-09-21 |
| Estado | `READY_FOR_OWNER_APPROVAL` |
| Owners | Seguridad + Operaciones |
| Aprobación pendiente | Product Owner sobre un commit concreto |
| Endpoint incluido | `GET /api/v1/academic-institutions?query=&cursor=&limit=` |
| Historia afectada | `I1-H02`, que permanece `BLOCKED` |
| Implementación | Fuera de esta decisión documental |

Esta propuesta materializa el gate exigido por ADR-007 para el único endpoint
público de H02. No implementa H02, no modifica OpenAPI y no autoriza ningún
endpoint de H03-H06.

El cierre futuro de S0 solo aprobará la baseline técnica y las entradas que
figuren expresamente en la política vigente. Cada historia posterior deberá
añadir, revisar y aprobar su propia entrada antes de exponer una operación
pública. La ausencia de una entrada exacta es denegación, no herencia de un
límite por defecto.

## Decisión para H02

### Operación y alcance de la clave

El identificador estable será `searchAcademicInstitutions`. H02 deberá usar ese
mismo valor como `operationId` de OpenAPI y como clave de configuración. Método,
plantilla de ruta y versión de contrato se validan al cargar la política para
evitar que otra operación reutilice accidentalmente el límite.

Los parámetros `query`, `cursor` y `limit` no forman parte del sujeto de rate
limiting: cambiar una búsqueda o un cursor no crea una cuota nueva. La operación
se limita por la combinación de:

```text
policyVersion + operationId + ruleId + hmacKeyVersion + ipFingerprint + window
```

### Límite, ventana y ráfagas

Una petición debe superar simultáneamente dos ventanas fijas:

| Regla | Límite | Ventana | Primera petición rechazada | TTL de fila |
|---|---:|---:|---:|---:|
| `sustained` | 60 | 60 segundos | 61 | 120 segundos |
| `burst` | 15 | 10 segundos | 16 | 70 segundos |

La ventana de 60 segundos permite el uso normal de búsqueda y autocompletado.
La ventana corta impide consumir toda la cuota sostenida en un pico inmediato.
No existe capacidad de ráfaga adicional ni crédito acumulable. Al finalizar una
ventana, la cuota de esa regla vuelve a estar disponible; una petición solo se
admite si ambas reglas disponen de capacidad.

Los valores son una propuesta de MVP y quedan versionados en
[`public-endpoint-rate-limits-v1.yaml`](../policies/public-endpoint-rate-limits-v1.yaml).
Cambiar un límite, ventana, TTL, algoritmo o identidad de sujeto requiere una
nueva versión de política, revisión de Seguridad y Operaciones y aprobación
antes de activarla.

### Identificación pseudonimizada del cliente

La IP nunca se persiste ni se registra en claro. El adapter de borde obtiene la
IP efectiva, la normaliza y entrega a `platform.ratelimit` únicamente el valor
necesario para calcular:

```text
HMAC-SHA-256(keyVersion,
  "propractix:ratelimit-ip:v1\n" + normalizedNetworkSubject)
```

El prefijo de separación de dominio evita reutilizar la misma clave lógica para
otros fines. El fingerprint se codifica en base64url sin padding y solo se
persiste junto con su `keyVersion`.

Normalización obligatoria:

- IPv4 se parsea como dirección binaria de 32 bits y se representa como red
  `/32`; se rechazan formas ambiguas, octales, enteros y valores fuera de rango.
- IPv6 se parsea como 128 bits, se eliminan identificadores de zona y se reduce
  a la red `/64` antes del HMAC. Esto evita que extensiones temporales dentro de
  la misma red eludan trivialmente el límite.
- Una IPv4 mapeada en IPv6 se trata como IPv4 `/32`.
- La entrada canónica incluye la familia para impedir colisiones entre formatos.

El `/64` de IPv6 y el `/32` de IPv4 son sujetos técnicos, no identificadores de
persona. No se exponen en respuestas, métricas, trazas ni logs.

### Proxies confiables

La dirección del socket es autoritativa salvo que pertenezca a un CIDR de proxy
configurado explícitamente como confiable. Solo en ese caso se puede interpretar
`Forwarded` o `X-Forwarded-For`.

Cada entorno selecciona mediante configuración tipada exactamente un modo:
`REMOTE_ADDRESS`, `FORWARDED` o `X_FORWARDED_FOR`. No se mezclan cabeceras ni se
aplica precedencia implícita. En modo proxy, la cadena se recorre de derecha a
izquierda, se eliminan únicamente saltos que pertenezcan a los CIDR confiables y
el primer salto no confiable es la IP efectiva.

- Cabeceras enviadas por un peer no confiable se ignoran.
- Una cadena ausente, malformada o sin cliente resoluble desde un proxy
  confiable falla de forma cerrada.
- La lista de CIDR es configuración validada por entorno; no se acepta `0.0.0.0/0`
  ni `::/0`.

### Versionado y rotación HMAC

El repositorio contiene identificadores de versión y referencias, nunca claves.
El material HMAC se suministra como secreto externo. La configuración mantiene:

- una `writeVersion`;
- una o más `readVersions`, incluyendo siempre la de escritura;
- algoritmo fijo `HMAC-SHA-256` y propósito fijo `RATE_LIMIT_CLIENT_IP`.

Rotación sin pérdida de cuota:

1. instalar la clave nueva fuera del repositorio;
2. añadir su versión a lectura y convertirla en `writeVersion`, conservando la
   versión anterior durante solapamiento;
3. para cada petición, calcular, comprobar e incrementar atómicamente los
   buckets de todas las versiones activas;
4. mantener el solapamiento al menos durante el TTL máximo de 120 segundos más
   60 segundos de margen operativo;
5. comprobar métricas y cobertura, retirar la versión anterior de lectura y
   eliminar su secreto mediante el procedimiento autorizado.

Si falta una versión activa, no se puede calcular un HMAC o las versiones no se
pueden actualizar de forma atómica, la petición falla cerrada. No se requiere
backfill: los contadores son efímeros y el solapamiento duplica la observación.

### Persistencia y concurrencia

Durante el Incremento 1 el único adapter persistente es PostgreSQL 16. No se
añaden Redis, servicios externos, brokers ni recursos AWS.

`platform.ratelimit` es propietario técnico de `platform_rate_limit_bucket` y
de la limpieza por TTL. La admisión se ejecuta detrás de un puerto técnico; el
adapter HTTP no accede a la tabla y `academicinstitution` no importa una
implementación de plataforma.

Una decisión de admisión es una operación PostgreSQL atómica: las dos reglas y
todas las versiones HMAC activas se reclaman como una unidad. Dos peticiones
concurrentes no pueden observar la misma última plaza como disponible. Una
transacción rechazada no consume parcialmente otra regla o versión.

Las filas expiradas dejan de participar en decisiones aunque el job de limpieza
aún no las haya borrado. `expiresAt` es el final de la ventana más 60 segundos:
de ahí los TTL de 70 y 120 segundos. La limpieza es idempotente, por lotes y no
forma parte del camino crítico.

### Fallo cerrado y respuestas

No existe bypass por timeout, error SQL, configuración inválida, IP efectiva no
resoluble o clave HMAC ausente.

Cuando se supera una cuota, la respuesta es `429` con
`Content-Type: application/problem+json`, `code=rate_limited`, el
`correlationId` de la petición y `Retry-After` como entero de segundos. El valor
es el techo del tiempo restante hasta que todas las reglas violadas vuelvan a
admitir, con mínimo de un segundo.

Ejemplo no normativo:

```json
{
  "type": "about:blank",
  "title": "Too Many Requests",
  "status": 429,
  "code": "rate_limited",
  "correlationId": "01-example"
}
```

Una indisponibilidad del rate limiter responde `503` con
`code=rate_limiter_unavailable`, el mismo `correlationId` y `Retry-After: 1`.
No alcanza el caso de uso ni se sirve el catálogo sin control. `title` y
`detail` no son copy de interfaz; el cliente resolverá los dos códigos mediante
claves i18n ES/EN que H02 deberá incorporar junto con OpenAPI y la API.

### Configuración y propiedad

La política se enlaza por identificador y versión desde configuración tipada,
se valida al arranque y no contiene límites alternativos en Java, TypeScript,
JSX o OpenAPI. Un entorno que pretenda exponer la operación debe arrancar como
no preparado si:

- la política no está `APPROVED` y vigente;
- no existe exactamente una entrada para `searchAcademicInstitutions`;
- una ventana, TTL o versión HMAC es inválida;
- falta una clave activa o el modo de proxy es incoherente.

`platform.ratelimit` posee el mecanismo, persistencia, normalización,
pseudonimización y métricas. La política no es regla de dominio y no entra en
aggregates de `academicinstitution`.

## Observabilidad

Métricas mínimas:

```text
propractix_rate_limit_allowed_total{operation_id,policy_version}
propractix_rate_limit_rejected_total{operation_id,policy_version,rule_id}
propractix_rate_limit_adapter_errors_total{operation_id,policy_version,error_class}
```

Los valores de labels proceden de allowlists de configuración. `error_class` es
una taxonomía pequeña (`storage`, `configuration`, `key`, `client_ip`) y nunca
un mensaje libre. Se prohíben IP, fingerprint, key version, query, cursor,
User-Agent, correlation ID o cualquier dato personal como label.

Umbrales iniciales:

- aviso por al menos un `adapter error` en 5 minutos;
- crítico por cinco o más `adapter errors` en 5 minutos o por indisponibilidad
  continua durante 2 minutos;
- aviso de posible abuso si hay al menos 30 rechazos y representan al menos el
  20 % de las decisiones en 10 minutos;
- crítico si hay al menos 100 rechazos y representan al menos el 50 % de las
  decisiones en 10 minutos.

Los umbrales son operativos y se revisan con tráfico sintético/real autorizado;
no cambian automáticamente límites ni habilitan bypass. La respuesta está en el
[`runbook de rate limiting`](../operations/rate-limiting-runbook.md).

## Pruebas obligatorias para I1-H02

H02 no podrá exponer el endpoint hasta demostrar:

1. una petición bajo ambos límites es admitida;
2. la petición 16 en 10 segundos y la 61 en 60 segundos son las primeras
   rechazadas por sus reglas respectivas;
3. `429` contiene `application/problem+json`, `rate_limited`, `correlationId` y
   `Retry-After` correcto;
4. la admisión se recupera al finalizar cada ventana con reloj controlado;
5. concurrencia PostgreSQL no admite más plazas que el límite;
6. caída, timeout o error del adapter produce `503 rate_limiter_unavailable` y
   no invoca el caso de uso;
7. durante rotación HMAC no se reinicia ni duplica de forma permisiva la cuota;
8. cabeceras de proxy falsificadas desde peers no confiables se ignoran, y una
   cadena inválida desde proxy confiable falla cerrada;
9. BD, logs, trazas, errores y métricas no contienen la IP normalizada ni sin
   normalizar, y las métricas no contienen fingerprints como labels;
10. códigos estables y sus claves de presentación tienen paridad ES/EN.

Se añaden además casos de IPv4, IPv4 mapeada en IPv6, IPv6 comprimida y dos
direcciones IPv6 del mismo `/64` para probar el tratamiento consistente.

## Fuera de alcance

- implementar Java, TypeScript, OpenAPI, migraciones, Docker o workflows;
- iniciar I1-H02;
- asignar valores a endpoints de H03-H06;
- rate limiting distribuido, Redis, servicios gestionados o infraestructura AWS;
- fingerprinting de navegador, cookies de tracking, device ID o CAPTCHA;
- listas de bloqueo dinámicas, reputación externa o administración en caliente.

## Condición de cierre

Esta rama deja S0 en `READY_FOR_OWNER_APPROVAL`, no en `CLOSED`. El Product Owner
deberá aprobar expresamente un commit concreto. Solo después se actualizará el
gate a `CLOSED`; esa aprobación habilitará únicamente la entrada H02 aquí
definida. Hasta entonces `I1-H02` permanece `BLOCKED` y no se inicia ninguna
historia.

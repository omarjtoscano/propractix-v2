# Decisión aprobada — S0_PUBLIC_ENDPOINTS

## Estado y alcance

| Campo | Valor |
|---|---|
| Gate | `S0_PUBLIC_ENDPOINTS` |
| Fecha de preparación | 2026-09-21 |
| Estado | `CLOSED` |
| Owners | Seguridad + Operaciones |
| Aprobación | Product Owner, 2026-09-27 |
| Commit aprobado | `fec3b2ed3a5afa04d1c079bda4f2d8981efd5920` |
| Endpoint incluido | `GET /api/v1/academic-institutions?query=&cursor=&limit=` |
| Historia afectada | `I1-H02`, que pasa a `READY` sin iniciarse |
| Implementación | Fuera de esta decisión documental |

Esta decisión materializa el gate exigido por ADR-007 para el único endpoint
público de H02. No implementa H02, no modifica OpenAPI y no autoriza ningún
endpoint de H03-H06.

El cierre de S0 solo aprueba la baseline técnica y las entradas que
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

Los valores aprobados para el MVP quedan versionados en
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

El resultado HMAC es un **identificador seudonimizado**, no un dato anónimo ni
un dato automáticamente excluido de protección de datos. El `/64` de IPv6 y el
`/32` de IPv4 son sujetos técnicos utilizados exclusivamente para prevención de
abuso. Se prohíbe reutilizar el fingerprint para analítica de usuarios,
autenticación, autorización, personalización, publicidad, enriquecimiento,
correlación entre finalidades o listas permanentes. No se expone en respuestas,
métricas, trazas ni logs.

Antes de una exposición productiva pública deberá realizarse una revisión de
privacidad que confirme finalidad, minimización, acceso, retención y operación
de derechos aplicables. Esta revisión no bloquea el desarrollo ni la aceptación
local de H02 con datos sintéticos y sin exposición pública.

### Proxies confiables

La confianza requiere dos condiciones simultáneas: la dirección del socket
pertenece a un CIDR configurado como proxy confiable **y** el proxy de borde
aplica un contrato de saneamiento configurado y probado. Llegar desde el CIDR
de Caddy no basta por sí solo para confiar en `Forwarded` o
`X-Forwarded-For`.

Modos previstos:

| Perfil | Modo | Contrato |
|---|---|---|
| Conexión directa local | `REMOTE_ADDRESS` | La dirección del socket es la IP efectiva; se ignoran `Forwarded` y `X-Forwarded-For`. |
| Detrás de Caddy | exactamente uno entre `FORWARDED` o `X_FORWARDED_FOR` | La selección es configuración tipada explícita; no existe precedencia implícita. |

En el perfil detrás de Caddy, el proxy de borde debe eliminar **ambas**
cabeceras recibidas del cliente. Después reconstruye únicamente la cabecera
seleccionada a partir del salto observado en la conexión de red y elimina la no
seleccionada. Nunca reenvía como origen confiable una cadena aportada por el
cliente.

Si existen varios proxies confiables, el proxy de borde sanea el origen y cada
salto confiable posterior añade el peer que observó directamente. El servidor
recorre la cadena de derecha a izquierda, elimina solo saltos incluidos en la
allowlist de proxies confiables y toma como IP efectiva el primer salto no
confiable. Todos los proxies de la cadena y su comportamiento de append deben
estar configurados y probados; un CIDR sin esa evidencia no se declara
confiable.

- Cabeceras enviadas directamente a un peer no confiable se ignoran.
- Una cadena ausente, malformada o sin cliente resoluble desde un proxy
  confiable falla de forma cerrada.
- La lista de CIDR es configuración validada por entorno; no se acepta
  `0.0.0.0/0` ni `::/0`.
- Confiar simultáneamente en `Forwarded` y `X-Forwarded-For` es un error de
  configuración que impide exponer la operación.

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

Las filas tienen expiración lógica a los 70 o 120 segundos, según la regla, y
desde ese instante dejan de participar en decisiones aunque aún no se hayan
borrado. La eliminación física ocurre en el siguiente ciclo normal de limpieza,
configurado cada minuto. La limpieza es idempotente, por lotes y no forma parte
del camino crítico.

Si existen más de 10 000 filas expiradas o la más antigua lleva más de 15
minutos expirada, se genera alerta y se abre incidencia de limpieza. El
fingerprint no se reutiliza ni se conserva más tiempo para investigar abuso;
la evidencia del incidente usa únicamente agregados y metadatos técnicos.

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
8. un cliente externo que envía `X-Forwarded-For` falsificado a Caddy no puede
   seleccionar la IP efectiva;
9. Caddy elimina `Forwarded` y `X-Forwarded-For` recibidas del cliente y
   reconstruye solo la cabecera configurada desde el salto de red observado;
10. la IP efectiva procede de ese salto observado y no del valor falsificado;
11. una cadena con varios proxies confiables se resuelve de derecha a izquierda
    hasta el primer salto no confiable;
12. una cadena malformada desde un proxy confiable falla cerrada;
13. una cabecera enviada directamente a un peer no confiable se ignora;
14. BD, logs, trazas, errores y métricas no contienen la IP normalizada ni sin
   normalizar, y las métricas no contienen fingerprints como labels;
15. códigos estables y sus claves de presentación tienen paridad ES/EN;
16. al alcanzar 70/120 segundos el contador expira lógicamente y se elimina en
    el siguiente ciclo normal; superar el umbral de filas expiradas genera
    alerta e incidencia sin ampliar la retención.

Se añaden además casos de IPv4, IPv4 mapeada en IPv6, IPv6 comprimida y dos
direcciones IPv6 del mismo `/64` para probar el tratamiento consistente.

## Fuera de alcance

- implementar Java, TypeScript, OpenAPI, migraciones, Docker o workflows;
- iniciar o implementar I1-H02 dentro de este cambio documental;
- asignar valores a endpoints de H03-H06;
- rate limiting distribuido, Redis, servicios gestionados o infraestructura AWS;
- fingerprinting de navegador, cookies de tracking, device ID o CAPTCHA;
- listas de bloqueo dinámicas, reputación externa o administración en caliente.

## Registro de cierre

El Product Owner aprobó expresamente esta política el 2026-09-27 sobre el commit
`fec3b2ed3a5afa04d1c079bda4f2d8981efd5920`. El gate queda `CLOSED` e I1-H02
pasa a `READY`, sin que este cambio documental inicie o implemente la historia.
La aprobación habilita únicamente la entrada `searchAcademicInstitutions` aquí
definida. No autoriza endpoints posteriores ni permite heredar límites: cada
nueva operación pública requiere su propia entrada revisada y aprobada.

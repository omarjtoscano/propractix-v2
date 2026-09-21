# Runbook — rate limiting de endpoints públicos

## Alcance y estado

Este runbook opera la baseline propuesta en
[`public-endpoint-rate-limits-v1.yaml`](../policies/public-endpoint-rate-limits-v1.yaml).
Solo contiene la operación `searchAcademicInstitutions`. Mientras
`S0_PUBLIC_ENDPOINTS` esté `READY_FOR_OWNER_APPROVAL`, no autoriza exposición ni
ejecución de H02.

El mecanismo pertenece a `platform.ratelimit`, usa PostgreSQL 16 y falla
cerrado. No se habilita un bypass para recuperar disponibilidad.

## Señales

| Señal | Interpretación | Umbral |
|---|---|---|
| `propractix_rate_limit_allowed_total` | Peticiones admitidas. | Referencia de volumen; sin alerta propia. |
| `propractix_rate_limit_rejected_total` | Peticiones `429`. | Aviso: ≥30 y ≥20 % de decisiones en 10 min. Crítico: ≥100 y ≥50 % en 10 min. |
| `propractix_rate_limit_adapter_errors_total` | Peticiones cerradas con `503`. | Aviso: ≥1 en 5 min. Crítico: ≥5 en 5 min o indisponibilidad continua 2 min. |

Las consultas y dashboards solo agrupan por `operation_id`, `policy_version`,
`rule_id` y una clase de error acotada. Nunca se añaden IP, fingerprint, versión
de clave, query, cursor, User-Agent o correlation ID como labels.

## Triage inicial

1. Confirmar el `operation_id`, versión de política, ventana temporal y estado
   de la alerta.
2. Separar rechazo normal (`429 rate_limited`) de fallo de control
   (`503 rate_limiter_unavailable`).
3. Verificar readiness de la aplicación y PostgreSQL, latencia, pool de
   conexiones, locks y errores de transacción.
4. Confirmar que la versión de política cargada es la aprobada, que existe una
   única entrada de operación y que todas las claves HMAC activas están
   disponibles. No imprimir ni copiar el material secreto.
5. Revisar despliegues y cambios de configuración recientes. No inspeccionar
   queries de usuario ni intentar identificar personas a partir de contadores.
6. Conservar `correlationId`, instante, versión de release, código técnico
   acotado y resultado. No conservar IP ni fingerprint en el ticket.

## Respuesta a una alerta de rechazos

1. Comprobar si aumentó el volumen permitido además del rechazado.
2. Identificar qué regla se activa (`burst` o `sustained`) mediante la label
   permitida `rule_id`.
3. Confirmar que las respuestas son `429 application/problem+json`, contienen
   `code=rate_limited`, `correlationId` y un `Retry-After` entero positivo.
4. Verificar que las peticiones admitidas se recuperan al finalizar la ventana.
5. Si parece abuso, mantener el límite. Registrar el incidente sin PII y
   escalar a Seguridad.
6. Si parece tráfico legítimo, no cambiar valores en caliente. Proponer una
   nueva versión de política con evidencia, pruebas y aprobación.

No se bloquean manualmente IP ni rangos desde esta baseline y no se añade
fingerprinting de dispositivo como respuesta improvisada.

## Respuesta a indisponibilidad del adapter

1. Confirmar que el endpoint devuelve `503 rate_limiter_unavailable`,
   `correlationId` y `Retry-After: 1`, y que el caso de uso no se ejecuta.
2. Comprobar conectividad y salud de PostgreSQL, saturación del pool, timeout,
   locks y capacidad de disco.
3. Validar que el job de limpieza no mantiene transacciones largas. Las filas
   expiradas se ignoran en admisión aunque sigan pendientes de borrado.
4. Si el error es de configuración o clave, restaurar la última versión
   aprobada y su conjunto completo de claves activas mediante el mecanismo de
   secretos autorizado.
5. Si el error es de resolución de IP, comprobar modo configurado, peer remoto,
   CIDR confiables y sintaxis de la cabecera seleccionada. No ampliar el trust a
   `0.0.0.0/0` ni `::/0`.
6. Mantener fallo cerrado hasta recuperar y verificar una petición sintética
   bajo límite, una sobre límite y la recuperación de ventana.

Un rollback es de configuración o release completa. Nunca consiste en omitir el
puerto, devolver `allowed` ante una excepción o cambiar temporalmente a memoria
local.

## Comprobaciones PostgreSQL

Las consultas operativas deben devolver solo agregados y metadatos técnicos:

- número de filas activas/expiradas por `operation_id`, `policy_version` y
  `rule_id`;
- edad de la fila expirada más antigua;
- duración y resultado del último lote de limpieza;
- conflictos o reintentos de la operación atómica.

No se selecciona ni exporta la columna de fingerprint. No se vuelcan tablas,
parámetros ni logs de sentencias con valores. Si hay más de 10 000 filas
expiradas o la más antigua supera 15 minutos, se abre incidencia de limpieza;
esto no justifica borrar filas activas ni truncar la tabla.

## Rotación de clave HMAC

Precondiciones: política nueva revisada, material nuevo instalado fuera de Git,
ventanas de 10 y 60 segundos conocidas y métricas disponibles.

1. Añadir la versión nueva a `readVersions` y establecerla como
   `writeVersion`; conservar la anterior.
2. Validar al arranque que ambas claves existen y que la versión de escritura
   pertenece al conjunto de lectura.
3. Desplegar y ejecutar pruebas sintéticas: bajo límite, primera superación,
   concurrencia y ausencia de IP/fingerprint en observabilidad.
4. Confirmar que cada petición comprueba e incrementa ambas versiones dentro de
   la misma transacción y que la cuota no se reinicia al cambiar de versión.
5. Esperar al menos 180 segundos (TTL máximo de 120 segundos más margen de 60)
   desde el último proceso que usó solo la clave anterior.
6. Retirar la versión anterior de `readVersions`, desplegar y repetir smoke.
7. Eliminar el material anterior por el canal autorizado y conservar evidencia
   técnica sin el secreto.

Si cualquier instancia carece de una versión activa, detener la rotación y
restaurar el conjunto anterior completo. No retirar primero la clave antigua.

## Incidente de proxies o cabeceras falsificadas

1. Confirmar si el peer del socket pertenece realmente a un CIDR confiable.
2. Para peers no confiables, verificar que `Forwarded` y `X-Forwarded-For` se
   ignoran y que cambiar esas cabeceras no cambia el sujeto.
3. Para proxy confiable, comprobar que solo está habilitado el header elegido y
   que la cadena se recorre de derecha a izquierda.
4. Una cadena ausente o malformada debe producir fallo cerrado; no usar la
   primera IP proporcionada por el cliente como fallback.
5. Corregir la configuración tipada mediante revisión. No añadir CIDR amplios o
   autodetectados durante el incidente.

## Cierre y evidencia

Antes de cerrar una incidencia:

- causa y periodo están identificados;
- alertas han vuelto a estado normal;
- una prueba bajo límite, una primera superación y una recuperación de ventana
  han pasado;
- `Retry-After`, `correlationId` y códigos estables son correctos;
- no se habilitó bypass ni se añadieron servicios externos;
- la evidencia no contiene IP, fingerprint, secreto, query ni datos personales;
- cualquier cambio permanente está en una nueva versión revisada y aprobada.

## Escalado

- Operaciones: PostgreSQL, despliegue, limpieza y salud.
- Seguridad: abuso, proxies confiables, claves y exposición de datos.
- Arquitectura: cambios de algoritmo, storage, puertos o propiedad técnica.
- Product Owner: aprobación de una política nueva o cambio de capacidad visible.

Una alerta no autoriza por sí sola a modificar límites ni a añadir una operación
no incluida.

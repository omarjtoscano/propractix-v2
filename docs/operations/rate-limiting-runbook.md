# Runbook — rate limiting de endpoints públicos

## Alcance y estado

Este runbook opera la baseline aprobada en
[`public-endpoint-rate-limits-v1.yaml`](../policies/public-endpoint-rate-limits-v1.yaml).
Solo contiene la operación `searchAcademicInstitutions`.
`S0_PUBLIC_ENDPOINTS` está `CLOSED` e I1-H02 está `READY`, pero todavía no se ha
iniciado ni implementado. El cierre de S0 no autoriza por sí solo la exposición
ni amplía la política o el alcance de H02.

El mecanismo pertenece a `platform.ratelimit`, usa PostgreSQL 16 y falla
cerrado. No se habilita un bypass para recuperar disponibilidad.

El fingerprint HMAC es un identificador seudonimizado sujeto a protección, no
un dato anónimo. Su única finalidad es prevenir abuso de la operación pública.
No se reutiliza para identificar usuarios, analítica, autenticación,
autorización, personalización, publicidad, enriquecimiento o correlación con
otros datos.

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
5. Si el perfil está detrás de Caddy, verificar las dos condiciones de
   confianza: peer inmediato dentro del CIDR permitido y evidencia vigente de
   que Caddy elimina las cabeceras aportadas por el cliente y reconstruye solo
   la seleccionada desde el salto de red observado.
6. Revisar despliegues y cambios de configuración recientes. No inspeccionar
   queries de usuario ni intentar identificar personas a partir de contadores.
7. Conservar `correlationId`, instante, versión de release, código técnico
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
parámetros ni logs de sentencias con valores. La expiración lógica ocurre a los
70 segundos para `burst` y a los 120 segundos para `sustained`; desde entonces
la fila no participa en decisiones. La eliminación física debe ocurrir en el
siguiente ciclo normal de limpieza, ejecutado cada minuto.

Si hay más de 10 000 filas expiradas o la más antigua supera 15 minutos, se
genera alerta y se abre incidencia de limpieza. Se comprueba el último ciclo,
locks, transacciones y capacidad, y se recupera el job sin ampliar la retención
ni reutilizar fingerprints. El umbral no justifica borrar filas activas,
truncar la tabla o exportar identificadores.

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

1. Identificar el modo configurado. La conexión directa local usa
   `REMOTE_ADDRESS` e ignora ambas cabeceras. El perfil detrás de Caddy elige
   explícitamente exactamente uno entre `FORWARDED` y `X_FORWARDED_FOR`.
2. Rechazar como inválida cualquier configuración que confíe simultáneamente en
   ambas cabeceras.
3. Confirmar si el peer inmediato del socket pertenece realmente a un CIDR
   confiable. Esto es necesario, pero no suficiente.
4. Verificar mediante prueba de borde que un cliente externo puede enviar un
   `X-Forwarded-For` falsificado a Caddy, pero Caddy elimina tanto esa cabecera
   como `Forwarded` antes de reconstruir únicamente la cabecera seleccionada.
5. Confirmar que el primer valor reconstruido procede de la IP observada por
   Caddy en la conexión de red y no del contenido enviado por el cliente.
6. Para una cadena con varios proxies confiables, comprobar que cada proxy
   intermedio añade el peer observado directamente y que el servidor recorre de
   derecha a izquierda hasta el primer salto no confiable.
7. Enviar una cadena malformada desde un proxy confiable y confirmar respuesta
   fail-closed `503 rate_limiter_unavailable`; no usar el primer valor como
   fallback.
8. Enviar las cabeceras directamente a un peer no confiable y confirmar que se
   ignoran y no cambian el sujeto.
9. Corregir configuración y evidencia de Caddy mediante revisión. No añadir
   CIDR amplios o autodetectados durante el incidente.

Una prueba del CIDR sin prueba del saneamiento de Caddy no restablece la
confianza ni permite cerrar el incidente.

## Revisión de privacidad antes de exposición productiva

Antes de habilitar acceso productivo público, Privacidad y Seguridad revisan la
finalidad exclusiva de prevención de abuso, minimización, acceso, expiración
lógica, eliminación física e incident response. La evidencia no incluye IP ni
fingerprint. Esta revisión es requisito de exposición productiva pública, pero
no bloquea la aceptación local de H02 con datos sintéticos y sin exposición.

## Cierre y evidencia

Antes de cerrar una incidencia:

- causa y periodo están identificados;
- alertas han vuelto a estado normal;
- una prueba bajo límite, una primera superación y una recuperación de ventana
  han pasado;
- `Retry-After`, `correlationId` y códigos estables son correctos;
- no se habilitó bypass ni se añadieron servicios externos;
- la evidencia no contiene IP, fingerprint, secreto, query ni datos personales;
- las filas expiradas se eliminaron en el ciclo normal o existe una incidencia
  abierta dentro del umbral definido;
- cualquier cambio permanente está en una nueva versión revisada y aprobada.

## Escalado

- Operaciones: PostgreSQL, despliegue, limpieza y salud.
- Seguridad: abuso, proxies confiables, claves y exposición de datos.
- Arquitectura: cambios de algoritmo, storage, puertos o propiedad técnica.
- Product Owner: aprobación de una política nueva o cambio de capacidad visible.

Una alerta no autoriza por sí sola a modificar límites ni a añadir una operación
no incluida.

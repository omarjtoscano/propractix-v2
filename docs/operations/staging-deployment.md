# Despliegue de staging

Estado actual: `CL0_CLOUD_STAGING=PENDING`. Este documento describe el proceso,
pero no autoriza su ejecución ni permite declarar H01 desplegada.

Semántica del gate:

- `PENDING` bloquea cualquier despliegue y cualquier prueba que contacte AWS.
- `READY_FOR_APPLICATION` permite el primer despliegue de H05 y las pruebas
  sintéticas de rollback y backup/restore necesarias para cerrar CL0.
- `CLOSED` se registra únicamente después de desplegar y verificar H05; permite
  promociones posteriores.

## Precondiciones

1. CI verde y release construida una sola vez para `linux/amd64` y `linux/arm64`.
2. Imágenes publicadas en el registro aprobado, con SHA completo y digest.
3. GitHub Environment `staging` con aprobación humana.
4. Cuenta, facturación, MFA, región, presupuesto, OIDC, instancia SSM, URL/TLS y
   secretos aprobados en CL0.
5. Manifiesto anterior conservado para rollback.

## Secuencia

1. Validar el manifiesto y los digests.
2. Solicitar el despliegue mediante OIDC y Systems Manager, sin claves AWS largas
   ni SSH público.
3. Descargar imágenes inmutables.
4. Ejecutar Flyway una sola vez.
5. Arrancar PostgreSQL, backend y Caddy.
6. Esperar `/actuator/health/readiness` y ejecutar `/healthz` y `/readyz`.
7. Registrar la release como activa solo después del smoke test.
8. Ante fallo, recuperar las imágenes anteriores sin deshacer migraciones.

El workflow y `deploy.sh` aplican la misma transición: aceptan
`READY_FOR_APPLICATION` o `CLOSED` y fallan de forma cerrada para `PENDING` o un
estado desconocido. La aprobación humana del GitHub Environment `staging` sigue
siendo obligatoria.

El workflow requiere variables externas `STAGING_DEPLOY_ROLE_ARN`,
`STAGING_AWS_REGION`, `STAGING_INSTANCE_ID` y `STAGING_DEPLOY_DOCUMENT`. No se
incluyen valores ficticios ni defaults productivos en el repositorio.

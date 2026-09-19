# Staging portable

Esta topología implementa la preparación de I1-H01 para ADR-009. No aprovisiona
AWS y no contiene cuenta, región, dominio, registro ni secretos. PostgreSQL y el
backend permanecen en una red interna; Caddy es la única entrada pública.

## Entradas obligatorias

- `PROPRACTIX_SERVER_IMAGE` y `PROPRACTIX_CLIENT_IMAGE`: referencias publicadas
  con digest `sha256`.
- `PROPRACTIX_RELEASE_SHA`: SHA Git completo.
- `PROPRACTIX_SITE_ADDRESS`: dirección HTTPS aprobada en CL0.
- `PROPRACTIX_DB_PASSWORD_FILE`: archivo externo creado desde el gestor de
  secretos aprobado.

El preflight es seguro para CI y no despliega:

```bash
./infra/staging/scripts/preflight.sh
```

`deploy.sh` se niega a continuar con `CL0_CLOUD_STAGING=PENDING` o con un estado
desconocido. `READY_FOR_APPLICATION` permite el primer despliegue de H05;
`CLOSED`, que solo se registra después de verificar H05, permite promociones
posteriores. El script ejecuta la migración antes del cambio, espera readiness y
hace smoke test. El rollback recupera únicamente las imágenes del manifiesto
anterior; nunca revierte una migración Flyway.

Los scripts de backup requieren destino y clave KMS aprobados y se pueden usar en
`READY_FOR_APPLICATION` para producir la evidencia de cierre de CL0. Staging
conserva exclusivamente datos sintéticos mientras las puertas de privacidad
estén abiertas.

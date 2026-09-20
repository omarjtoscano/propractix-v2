# Backup y restauración

## Aceptación local de I1-H01

`infra/local/scripts/backup-restore-smoke.sh` crea una base fuente aislada,
inserta un marcador inequívocamente sintético, ejecuta `pg_dump` en formato
custom, restaura el dump en una base vacía y verifica el marcador restaurado.
Elimina las dos bases de smoke y el dump temporal al terminar.

```bash
npm run infra:up
npm run infra:backup-restore-smoke
```

Los dumps locales se escriben bajo `.local/`, que está ignorado por Git. Esta
prueba no usa AWS, datos personales ni la base activa de la aplicación.

## Evidencia sintética de AWS staging

Los scripts `infra/staging/scripts/backup.sh` y `restore-smoke.sh` requieren un
destino S3 compatible y una clave KMS aprobados externamente. `PENDING` los
bloquea; pueden ejecutarse en `READY_FOR_APPLICATION` para reunir la evidencia
necesaria y continúan disponibles en `CLOSED`.

El backup usa `pg_dump` en formato custom, cifra el objeto en destino y nunca lo
escribe en el repositorio. La prueba de restauración crea una base aislada con
prefijo `propractix_restore_smoke_`, aplica `pg_restore --exit-on-error` y elimina
esa base al terminar. No modifica la base activa.

La frecuencia, retención y eliminación no se fijan hasta aprobar ML-15. Mientras
tanto, staging solo puede contener datos sintéticos.

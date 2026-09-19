# Infraestructura local

Arranque:

```bash
npm run infra:up
```

Parada:

```bash
docker compose -f infra/compose.yaml down
```

PostgreSQL queda disponible en `127.0.0.1:5432`. Para usar otro archivo local
puede definirse `PROPRACTIX_DB_PASSWORD_FILE`; el valor del secreto nunca se pasa
como variable de entorno.

El arranque crea, a partir de
`infra/local/templates/database.password.example`,
un archivo sintético local ignorado por Git. PostgreSQL consume ese archivo como
secreto Compose y Spring lo carga mediante config tree; no se aceptan contraseñas
inline ni en `.env`.

Mailpit captura el correo de desarrollo en `localhost:1025` y publica su interfaz
solo en `http://127.0.0.1:8025`. No se configura relay ni forwarding SMTP, de
modo que los mensajes quedan capturados localmente y no se entregan a Internet.

La aceptación local de backup y restauración se ejecuta con datos sintéticos:

```bash
npm run infra:backup-restore-smoke
```

La topología portable de staging se documenta en `infra/staging/README.md`.
No debe ejecutarse contra AWS mientras `CL0_CLOUD_STAGING` permanezca pendiente.

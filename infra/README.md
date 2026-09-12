# Infraestructura local

Arranque:

```bash
docker compose -f infra/compose.yaml up -d
```

Parada:

```bash
docker compose -f infra/compose.yaml down
```

PostgreSQL queda disponible en `localhost:5432`. La contraseña local puede sobrescribirse con `PROPRACTIX_DB_PASSWORD`.

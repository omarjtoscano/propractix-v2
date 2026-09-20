# Desarrollo local de I1-H01

## Toolchain

- JDK 21.
- Maven Wrapper 3.9.16.
- Node.js 24 y npm 11.9.0.
- Docker y Docker Compose v2.

## Ejecución

```bash
npx --yes npm@11.9.0 ci
npm run infra:up
npm run server:run
npm run infra:backup-restore-smoke
npm run check
```

`npm run infra:prepare` materializa
`infra/local/secrets/database.password` desde
`infra/local/templates/database.password.example`. El archivo real está
ignorado, tiene permisos `0600` y Spring lo consume mediante config tree. El
preparador elimina terminadores de línea finales para que
PostgreSQL y Spring interpreten exactamente el mismo valor. Compose no contiene
contraseñas inline. `staging` y `production` requieren configuración y secretos
externos.

PostgreSQL 16 escucha únicamente en loopback. Mailpit captura SMTP sintético en
`127.0.0.1:1025` y expone su interfaz en `127.0.0.1:8025`. La configuración no
habilita relay ni forwarding SMTP: los mensajes permanecen en Mailpit y no se
entregan a Internet. Flyway es la autoridad del esquema y Hibernate usa siempre
`ddl-auto=validate`.

I1-H01 no contiene migraciones ni tablas de negocio. Tampoco expone endpoints de
producto; únicamente health y readiness.

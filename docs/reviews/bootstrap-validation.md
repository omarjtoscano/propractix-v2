# Revisión del bootstrap del repositorio

- Fecha: 2026-09-12
- Resultado: estructura preparada; validación completa pendiente en el entorno local del propietario.

## Comprobaciones realizadas

| Comprobación | Resultado |
|---|---|
| ZIP de Spring Initializr | Correcto. |
| Spring Boot | `4.1.1`. |
| Java objetivo | `21`. |
| Maven Wrapper | Correcto; resolvió Maven `3.9.16`. |
| Dependencias iniciales | Web MVC, Security, OAuth2 Resource Server, JPA, Validation, PostgreSQL, Flyway, Mail, Actuator y Testcontainers presentes. |
| npm | `package-lock.json` generado correctamente con npm `11.9.0`. |
| Docker Compose | Configuración preparada para PostgreSQL `16-alpine`. |
| Pruebas Maven | No ejecutadas completamente por bloqueo de acceso a Maven Central en el entorno de preparación. |

## Limitaciones del entorno de preparación

- El runtime disponible tenía JDK 17, mientras el proyecto requiere JDK 21.
- Docker no estaba instalado.
- Maven Central no era resoluble desde la red del entorno.

Estas limitaciones no demuestran un fallo del proyecto. La primera validación local debe ejecutarse con JDK 21, Docker activo y acceso a Maven Central:

```bash
java -version
docker version
./server/mvnw -v
npm run infra:up
npm run server:test
```

No debe iniciarse la primera historia hasta que estas comprobaciones pasen localmente.

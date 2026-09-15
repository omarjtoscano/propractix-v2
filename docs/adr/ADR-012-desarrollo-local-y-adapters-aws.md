# ADR-012 — Desarrollo local y adapters AWS opcionales

- Estado: Aceptado — revisión 1
- Fecha: 2026-09-15
- Decisores: Producto, Arquitectura, Desarrollo, Operaciones y Seguridad
- Relacionados: ADR-009 revisión 3 y ADR-011 revisión 1

## Contexto

El Incremento 1 debe entregar un recorrido navegable antes del primer despliegue AWS. H01 a H05 necesitan bases de datos, correo de desarrollo y secretos de prueba, pero su aceptación no puede depender de una cuenta cloud, conectividad externa, créditos ni emulaciones incompletas de AWS.

La solución debe conservar la paridad útil con `staging` sin introducir SDK de AWS en dominio o aplicación. Emular todos los servicios cloud en cada arranque local aumentaría tiempo y fragilidad sin aportar valor a la mayoría de pruebas.

## Decisión

### Baseline local predeterminada

El perfil `local` se ejecuta con Docker Compose y contiene únicamente:

- PostgreSQL 16 como única base de datos;
- Mailpit como sumidero SMTP y visor de mensajes de desarrollo;
- backend, cliente y proxy cuando la historia correspondiente los incorpore.

Mailpit no entrega correo a Internet. Su interfaz se publica solo en loopback y nunca se habilita en producción. Los datos, destinatarios, tokens y mensajes utilizados son sintéticos.

### Secretos locales

Los secretos del perfil `local` son valores sintéticos sin validez fuera del entorno. Se guardan como archivos no versionados y se montan en el contenedor; Spring los consume mediante config tree, por ejemplo:

```text
SPRING_CONFIG_IMPORT=optional:configtree:/run/secrets/
```

El repositorio puede contener nombres de archivos, plantillas `.example` y valores inequívocamente inútiles, pero nunca secretos operativos. `.env` solo referencia rutas o configuración no sensible. Los archivos locales de secretos, outputs y volúmenes quedan excluidos por `.gitignore`.

### Perfil opcional `aws-contract`

LocalStack solo se inicia mediante el perfil Compose opcional `aws-contract`. Su alcance es probar contratos de adapters AWS concretos —por ejemplo, serialización, nombres, rutas y manejo de errores de S3 o Parameter Store— sin contactar AWS real.

Este perfil:

- no arranca con `local` ni con las pruebas habituales;
- no sustituye pruebas reales de IAM, OIDC, SSM, ECR, red o costes;
- no se usa como base de datos ni como relay de correo;
- puede omitirse al aceptar H01 a H05 si la historia aún no incorpora un adapter AWS;
- nunca recibe credenciales ni datos reales.

Los adapters AWS continúan detrás de puertos de salida. Una incompatibilidad descubierta en `staging` se corrige en el adapter o en su prueba contractual, no en el dominio.

### Aceptación local del Incremento 1

H01, H02, H03, H04 y H05 se implementan y aceptan sin AWS. En particular:

- H01 demuestra compilación, migraciones, health, arquitectura, Compose y empaquetado;
- H02 demuestra el catálogo con una fuente/fixture aprobada;
- H03 usa exclusivamente datos sintéticos y adapters deterministas;
- H04 captura el correo en Mailpit y completa la verificación desde ese mensaje;
- H05 permite navegar login y “mi empresa” de extremo a extremo en local.

`CL0_CLOUD_STAGING` no es un criterio de aceptación de estas historias. Después de aceptar H05 localmente, el plan cloud promueve una release ya funcional y verifica integración AWS, rollback y backup/restore.

Las puertas `P0_PRIVACY`, `S0_PUBLIC_ENDPOINTS` y `E0_EMAIL` siguen bloqueando respectivamente datos personales reales, exposición pública y correo real. El entorno local no las evade: utiliza loopback, Mailpit y fixtures sintéticas.

## Consecuencias

- El desarrollo habitual funciona sin cuenta AWS ni consumo de créditos.
- PostgreSQL mantiene paridad de versión con `staging`.
- Mailpit permite verificar contenido y enlaces sin enviar correo real.
- Los secretos no se incrustan en Compose, Git ni imágenes.
- LocalStack deja de ser dependencia accidental de cada historia y se reserva para contratos que lo justifiquen.
- La prueba definitiva de controles administrados por AWS sigue perteneciendo a `CL0_CLOUD_STAGING`.

## Alternativas consideradas

- **Usar AWS durante H01–H05:** rechazado; acopla la aceptación funcional a infraestructura y coste externos.
- **H2 para desarrollo/pruebas:** rechazado; no reproduce PostgreSQL ni está permitido por la arquitectura.
- **Variables con secretos en Compose o `.env`:** rechazado; facilitan exposición y mezcla de configuración sensible.
- **LocalStack siempre activo:** rechazado; añade peso y falsa equivalencia para historias que no consumen adapters AWS.
- **SMTP real de pruebas:** rechazado; introduce terceros y riesgo de enviar PII o tokens.

## Conformidad

- `docker compose config` demuestra que `local` no necesita LocalStack.
- PostgreSQL reporta versión mayor 16.
- Mailpit solo escucha en loopback y no tiene relay externo.
- Un escaneo confirma que Git no contiene valores de archivos montados ni credenciales.
- Las pruebas `aws-contract` se seleccionan explícitamente y no contactan endpoints públicos.
- La aceptación E2E de H05 se completa con AWS deshabilitado.
- Ningún módulo `domain` o `application` importa SDK o tipos de AWS/LocalStack.

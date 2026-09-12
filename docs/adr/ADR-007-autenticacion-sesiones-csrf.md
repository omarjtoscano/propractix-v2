# ADR-007 — Access token, refresh cookie y protección CSRF/origin

- Estado: Propuesto
- Fecha: 2026-09-12
- Decisores: Seguridad, Producto y Arquitectura

## Contexto

El Incremento 1 necesita cuentas de empresa y estudiante, verificación de correo y sesiones web. V1 almacenaba refresh tokens sin hash ni rotación suficiente. V2 no debe exponer tokens persistentes al JavaScript del navegador.

## Decisión

- Las contraseñas se almacenarán con BCrypt y un coste inicial 12, ajustable mediante configuración y pruebas de rendimiento.
- El access token será un JWT firmado de vida corta, inicialmente 10 minutos.
- El access token se devolverá en la respuesta y el cliente lo conservará solo en memoria; no se guardará en `localStorage` ni `sessionStorage`.
- El refresh token será opaco, aleatorio, de alta entropía y se entregará en una cookie `HttpOnly`, `Secure` en entornos no locales, `SameSite=Lax` y restringida a `/api/v1/auth`.
- En base de datos se almacenará únicamente un hash SHA-256 del refresh token, junto con familia, expiración, rotación, revocación y datos mínimos de auditoría.
- Cada refresh rotará el token. La reutilización de un token anterior revocará toda su familia.
- La duración inicial máxima de una familia será 30 días.
- Refresh y logout exigirán protección CSRF de doble envío y validación estricta de `Origin` contra una allowlist.
- CORS se configurará con orígenes explícitos; nunca `*` con credenciales.
- Los tokens de verificación de correo serán aleatorios, de un solo uso, con expiración y almacenados mediante hash.
- Login, registro, reenvío y recuperación tendrán límites de intentos y respuestas que no permitan enumerar cuentas.

Las claves de firma y secretos procederán de configuración externa validada al arranque. No se incluirán en Git.

## Alternativas consideradas

### JWT y refresh token en almacenamiento del navegador

Rechazada por exposición ante XSS.

### Refresh token persistido en texto claro

Rechazada porque una filtración de base de datos permitiría reutilizar sesiones activas.

### Sesión HTTP tradicional en servidor

Viable, pero no elegida para conservar una API stateless con clientes futuros. Podrá revisarse si la complejidad del refresh supera el beneficio.

### Refresh token como JWT

Rechazado. Un token opaco facilita revocación, rotación y detección de reutilización.

## Consecuencias

- El cliente deberá renovar tokens y mantener el access token en memoria.
- La gestión de sesiones requerirá tablas, limpieza y auditoría.
- Los tiempos exactos y parámetros criptográficos se configurarán y probarán.
- Cualquier cambio de estrategia necesitará revisión de seguridad y ADR nuevo.

## Criterios de aprobación

- Aprobar las duraciones iniciales de 10 minutos y 30 días.
- Aprobar access token en memoria y refresh cookie rotatoria.
- Aprobar BCrypt coste 12 para la primera medición.

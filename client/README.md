# Client

Shell técnico React/TypeScript de I1-H01. Permite validar el toolchain, i18n y la
imagen OCI sin implementar ninguna capacidad de negocio.

```bash
npm run client:typecheck
npm run client:lint
npm run client:test
npm run client:build
```

No contiene registro, autenticación, empresas, estudiantes, catálogo ni rutas
funcionales de historias posteriores.

El shell incluye selector ES/EN, preferencia local y mensajes de error por código.
La [guía de i18n y errores](../docs/development/i18n-and-errors.md) documenta
el catálogo, la resolución de idioma y los controles que se ejecutan en CI.

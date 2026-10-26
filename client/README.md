# Client

Pantalla funcional de consulta del catálogo académico de I1-H02 E7, en App,
con autocompletado reusable, React Query, paginación por cursor y ES/EN.
Solo usa datos sintéticos; la selección y el nombre manual viven en memoria.
No se añade ninguna ruta, formulario de registro ni escritura al servidor.

```bash
# Node 24 y npm 11.9.0; instala ambos lockfiles mediante postinstall.
npm ci
npm run client:generate
npm run client:check-generated
npm run client:typecheck
npm run client:lint
npm run client:test
npm run client:build
```

El generador está aislado en `tools/openapi-generator` con su propio lockfile:
Hey API 0.99.0 utiliza TypeScript 6.0.3; la aplicación compila con 7.0.2.
Antes de generar se comprueban ambas resoluciones. Nunca editar archivos bajo
`src/shared/api/generated`; modificar el contrato exige autorización separada.
La [excepción F0 de ADR-001](../docs/adr/ADR-001-monolito-modular.md)
registra la autorización del Product Owner y el issue oficial.

`src/configuration/academicInstitutionSearch.json` configura debounce, tamaño
de página (20 por defecto, máximo contractual 50) y espera de respaldo para
429/503 sin Retry-After válido. La UI no reintenta automáticamente errores,
refoco o reconexión. Un cambio de snapshot descarta páginas y reinicia una vez;
un conflicto reiterado exige reintento explícito. Retry-After bloquea también
consultas nuevas y remontajes durante la sesión. No se persiste en disco.

La [guía i18n](../docs/development/i18n-and-errors.md) describe el selector,
preferencia y errores por código. La atribución es dato del snapshot y se
muestra junto al aviso explícito de datos sintéticos y el alcance informativo.

En local, `npm exec --workspace=client -- vite --host 127.0.0.1` sirve la UI;
Vite reenvía `/api` a `http://127.0.0.1:8080`. El backend debe estar arrancado
con el perfil local y una fixture sintética publicada mediante el flujo
administrativo ya aceptado. La imagen OCI conserva el proxy Caddy existente.
Playwright y la orquestación E2E permanecen reservados a E8.

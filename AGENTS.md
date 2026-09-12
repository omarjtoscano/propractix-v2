# Instrucciones para agentes de desarrollo

## Fuentes de verdad

Antes de modificar código, lee en este orden:

1. `docs/ProPractix-V2-MVP-Espana-Especificacion-Arquitectura-y-Tecnica-para-Codex.md`.
2. `docs/ProPractix-V2-Blueprint-Dominio-y-Mapas-Legales-Espana.md`.
3. Los ADR aplicables de `docs/adr/`.
4. El plan aprobado del incremento en `docs/plans/`.

Si dos documentos se contradicen, detente y solicita una decisión. Un ADR aceptado posterior puede concretar una decisión general, pero no alterar silenciosamente el alcance de producto ni una puerta legal.

## Alcance vigente

- Trabaja únicamente en el Incremento 1 y en una historia autorizada cada vez.
- No implementes ofertas, candidaturas, prevalidación, formalización, prácticas ni documentos todavía.
- La empresa es el tenant principal.
- `student` posee perfil, onboarding y declaración académica; `identity` no absorbe esos conceptos.
- La universidad es una referencia externa y no requiere cuenta ni workspace.
- España es la primera jurisdicción del MVP según D-001.
- País empresarial, locale de interfaz y jurisdicción legal son conceptos distintos; un fallback de idioma nunca aplica reglas españolas a otro país.
- No inventes reglas legales. `UNKNOWN` e `INDETERMINATE` bloquean puertas críticas.

## Arquitectura obligatoria

- Monolito modular desplegable como una unidad.
- Organización principal por bounded context, nunca por carpetas globales `controller`, `service`, `repository` o `entity`.
- Dependencias: `adapter -> application -> domain`.
- El dominio es Java puro: sin Spring, JPA, HTTP ni acceso a variables de entorno.
- Los casos de uso se exponen mediante puertos de entrada; persistencia, reloj, correo e integraciones son puertos de salida.
- Ningún contexto accede a tablas, repositorios o entidades de persistencia de otro contexto.
- No introduzcas condicionales de dominio por país; resuelve reglas mediante políticas jurisdiccionales versionadas.
- No uses Lombok, Spring Data REST, H2, microservicios, Kafka ni CQRS completo salvo un ADR nuevo previamente aprobado.

## Configuración e internacionalización

- Países habilitados, locales soportados, correspondencias país→locale y locale de fallback proceden de configuración tipada y validada.
- Toda etiqueta, acción, estado, ayuda, validación o error visible usa una clave i18n; no incrustes textos de presentación en Java, TypeScript o JSX.
- API, dominio y persistencia conservan códigos estables independientes del idioma.
- Los textos legales se versionan separadamente de los catálogos generales de traducción.
- Las invariantes técnicas permanecen tipadas en código; no se convierten en configuración libre para aparentar flexibilidad.

## Seguridad y tenancy

- Nunca confíes en un `tenantId` aportado por el cliente para autorizar una operación.
- Deriva el tenant de la identidad autenticada y comprueba pertenencia en aplicación y persistencia.
- Toda consulta sobre datos empresariales incluye el tenant en su criterio y sus restricciones de unicidad.
- Añade al menos una prueba negativa de acceso entre tenants en cada historia que maneje datos tenant-owned.
- No registres secretos, tokens, contraseñas, documentos o datos personales innecesarios en logs.

## Persistencia y API

- PostgreSQL es la única base de datos soportada.
- Las migraciones Flyway son forward-only; nunca edites una migración aplicada.
- Las entidades JPA y sus mappers viven en adapters, separadas del modelo de dominio.
- La API se documenta con OpenAPI, usa Problem Details y mantiene compatibilidad dentro de `/api/v1`.
- Las operaciones de creación con riesgo de repetición siguen el ADR de idempotencia.

## Flujo de trabajo

1. Confirma la historia, criterios de aceptación y elementos fuera de alcance.
2. Presenta un plan breve y espera aprobación cuando cambien dominio, seguridad, privacidad o cumplimiento.
3. Implementa primero invariantes y pruebas de dominio.
4. Añade aplicación y puertos.
5. Añade adapters y configuración.
6. Integra API, frontend e i18n cuando la historia lo requiera.
7. Ejecuta las validaciones relevantes.
8. Revisa `git diff --check`, `git diff` y `git status` antes del commit.
9. Resume archivos modificados, pruebas ejecutadas, resultados y riesgos.

## Comandos mínimos

```bash
npm run infra:up
npm run server:test
npm run server:verify
git diff --check
git status --short
```

Si Docker o JDK 21 no están disponibles, informa la limitación; no simules una ejecución satisfactoria.

## Política de commits

- Un commit debe corresponder a una historia o cambio técnico coherente.
- Usa mensajes tipo Conventional Commits, por ejemplo `feat(identity): register company administrator`.
- No mezcles reformateos masivos ni cambios no relacionados.
- No hagas push, merge, rebase destructivo ni publiques artefactos sin autorización explícita.

# Revisión de V1 — Política de correo empresarial para V2

- Fecha: 2026-09-12
- Estado: `REVIEWED`
- Decisión relacionada: D-007
- Gate relacionado: `B0_COMPANY_EMAIL_POLICY`
- Alcance: análisis estático del código V1 aportado por Producto

## 1. Resultado ejecutivo

V1 ya intentaba impedir el autorregistro empresarial con proveedores públicos, correos temporales, determinados dominios educativos y dominios aparentemente incapaces de recibir correo. La intención se conserva, pero la implementación no se copia: existen fuentes de verdad desconectadas, comportamiento permisivo ante fallos de carga, reglas educativas hardcodeadas, consultas DNS síncronas y un endpoint público que favorece enumeración.

La lista de 84 dominios se conserva como artefacto `DRAFT` trazable en [`../policies/company-email-admission-seed-v1.yaml`](../policies/company-email-admission-seed-v1.yaml). No queda publicada ni cierra por sí sola B0.

## 2. Evidencia examinada

| Componente V1 | Hallazgo |
|---|---|
| `registration/application/CorporateEmailValidator.java` | Normaliza el dominio, consulta una lista, aplica sufijos educativos hardcodeados y ejecuta verificación MX síncrona. |
| `registration/infrastructure/config/BlockedDomainLoader.java` | Carga un recurso estático en memoria; si falta o falla su lectura, continúa sin bloquear. Las adiciones se pierden al reiniciar. |
| `resources/blocked-email-domains.txt` | Contiene 84 dominios únicos: 45 proveedores públicos/comunes y 39 temporales. SHA-256 del archivo original: `6e33c1d8dcd825a3a895dd2379e7181e9e63aebd6619bbf294d886a5ba213c7e`. |
| Módulo administrativo de dominios bloqueados | Persiste otro catálogo en base de datos que el validador de registro no consulta. |
| `MxRecordVerifier.java` | Usa DNS durante la petición; algunos errores técnicos permiten continuar sin distinguir un estado indeterminado. |
| `CompanySetupController` y cliente | Exponen una comprobación GET con el correo en query string y respuestas que permiten inferir existencia. |
| Pruebas | Cubren proveedores conocidos y MX, pero varias dependen del DNS público real. |

## 3. Decisiones para V2

1. `organization` posee una única `CompanyEmailAdmissionPolicy` versionada.
2. El runtime no mantiene una lista estática paralela, una tabla administrativa desconectada ni mutaciones solo en memoria.
3. La coincidencia es exacta sobre el dominio normalizado después de `@`; nunca usa substring.
4. Un dominio propio no se bloquea por utilizar Google Workspace, Microsoft 365 u otro hosting.
5. Las categorías iniciales son códigos estables, no etiquetas visibles: `PUBLIC_OR_COMMON_PROVIDER` y `DISPOSABLE_PROVIDER`.
6. Las reglas educativas por sufijo de V1 no se migran. Cualquier bloqueo educativo deberá publicarse como política explícita y gobernada, sin asumir que un TLD demuestra el tipo de organización.
7. La política de admisión es determinista y autoritativa. Dominio incluido bloquea; política ausente o inválida mantiene H03 cerrada.
8. La comprobación de ruta de correo es el control técnico principal después de la política, mediante un puerto de salida sustituible.
9. Un fallo de resolver, timeout, `SERVFAIL` o indisponibilidad técnica produce `INDETERMINATE`, acepta la solicitud con respuesta genérica y programa reintento; no se presenta como correo inválido.
10. `NO_MAIL_ROUTE`, calculado después de considerar la semántica de ruta implícita aplicable, no rechaza H03: conserva la solicitud pendiente de verificación y programa una nueva comprobación.
11. La verificación mediante enlace sigue siendo la prueba definitiva de control de la dirección. Una verificación completada resuelve una comprobación MX previamente indeterminada para ese onboarding.
12. No existe endpoint público `check-domain`; H03 valida dentro del comando idempotente y mantiene respuestas no enumeradoras.
13. No se registran correo completo, dominio, respuestas DNS ni tokens salvo evidencia mínima, pseudonimizada y justificada.
14. Los tests DNS usan un adapter determinista; las pruebas E2E no dependen de Internet.

## 4. Estados técnicos

| Resultado | Significado | Efecto en H03 |
|---|---|---|
| `MAIL_CAPABLE` | Existe una ruta de correo técnicamente admisible. | Continúa. |
| `NO_MAIL_ROUTE` | La comprobación no encontró una ruta de correo admisible. | Continúa pendiente de verificación y reintenta. |
| `INDETERMINATE` | No puede concluirse por fallo transitorio o de infraestructura. | Continúa, registra evidencia mínima y reintenta. |

`INDETERMINATE` no equivale a `MAIL_CAPABLE`. Tampoco es el `UNKNOWN` de una puerta legal: esta comprobación técnica no autoriza ni verifica jurídicamente a la empresa.

## 5. Estado de B0

| Resultado requerido | Estado |
|---|---|
| Código V1 contrastado | `COMPLETED` |
| Seed V1 trazable | `DRAFT_CREATED` |
| Modelo de versión, ámbito y estados | `DECIDED` |
| Normalización y coincidencia exacta | `DECIDED` |
| Semántica MX y degradación | `DECIDED` |
| Revisión de cobertura/actualidad de los 84 dominios | `PENDING` |
| Aprobación y publicación de la primera versión | `PENDING` |
| Estrategia de actualización y rollback ejercitada | `PENDING` |
| Tests automatizados del adapter y casos de política | `PENDING` |

Por tanto, `B0_COMPANY_EMAIL_POLICY` permanece `PENDING`. El contraste solicitado de V1 está terminado, pero H03 no comienza hasta publicar y probar una versión inicial.

## 6. Criterios para cerrar B0

- Producto y Seguridad aprueban las categorías y la cobertura inicial.
- El artefacto publicado tiene ID, versión, estado, fecha efectiva, fuente y hash.
- Existe exactamente una versión activa y rollback significa publicar una nueva versión auditada.
- Los adapters cargan la política de forma tipada y rechazan una configuración ausente o inválida.
- El resolver técnico implementa `MAIL_CAPABLE`, `NO_MAIL_ROUTE` e `INDETERMINATE` sin filtrar excepciones de infraestructura al usuario.
- Timeout, backoff, caché y número de reintentos proceden de configuración validada.
- OpenAPI utiliza códigos estables y toda presentación se resuelve mediante i18n.
- Las pruebas cubren proveedores públicos, temporales, dominio propio con hosting común, ruta implícita admitida, ausencia concluyente de ruta, timeout, `SERVFAIL`, política ausente y no enumeración.

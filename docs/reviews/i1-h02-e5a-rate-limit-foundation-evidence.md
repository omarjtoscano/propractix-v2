# Evidencia de implementación de I1-H02 E5A

- Entrega: `E5A — Policy loader, configuración, IP, HMAC y secretos`.
- Estado de E5A: `ACCEPTED`.
- Aprobación expresa del Product Owner: 2026-10-23.
- Commit funcional revisado y aprobado: `6cc21e6c034b9c32db3f22ebb8b977f4250614cd`.
- Estado de I1-H02: `IN_PROGRESS`; E5B autorizada separadamente el 2026-10-23 y `IN_PROGRESS`; E5C y posteriores sin autorización.
- [PR #12](https://github.com/omarjtoscano/propractix-v2/pull/12): integrada; confirmación expresa del Product Owner el 2026-10-23 sobre `8f8f1b0bda8c9d066ba4751272939b4ae5cf6590`.
- Autorización expresa del Product Owner: 2026-10-22.
- Integración de E4 confirmada por el Product Owner:
  `9d128cc3df8c2a90177b1f18cea47708d8abfc03`.
- Base: `increment/01-company-identity-catalog`, actualizada mediante fetch y
  comprobada sin divergencia antes de crear
  `feature/i1-h02-e5a-rate-limit-foundation`.
- `CL0_CLOUD_STAGING`: `PENDING`.

## Alcance y límites

`platform.ratelimit.application` contiene el modelo técnico, los contratos
`RateLimitAdmission`, `RateLimitBucketStore`, `ClientIpResolver`,
`ClientIpFingerprint` y `RateLimitPolicyProvider`. No implementa todavía
admisión ni almacenamiento. El catálogo no importa este módulo y la plataforma
no importa contextos de negocio; ArchUnit verifica ambas fronteras y la ausencia
de frameworks en la aplicación técnica.

No se añaden migraciones, filtro HTTP, endpoints, OpenAPI, frontend, cambios de
Caddy, snapshot RUCT real, métricas/jobs/readiness del rate limiter ni AWS.
La comprobación del saneamiento real del borde corresponde a E5C y no se
atribuye a las pruebas unitarias de resolución de esta entrega.

## Política canónica y empaquetado

Maven incluye directamente `docs/policies/public-endpoint-rate-limits-v1.yaml`
como recurso `policies/public-endpoint-rate-limits-v1.yaml`, sin filtrado ni
copia versionada en `src/main/resources`. Los límites, ventanas, TTL, intervalo
de limpieza y configuración HMAC se obtienen de ese recurso.

El loader limita el tamaño, usa YAML seguro y exige el SHA-256 de integridad
del artefacto aprobado, antes de convertirlo en el modelo técnico. Ese digest
ancla la aprobación de **todo** S0, incluidas privacidad, proxy, activación,
normalización, almacenamiento y observabilidad. No es una segunda tabla de
límites; cualquier modificación, incluso de formato, requiere revisar el
artefacto y su ancla de aprobación. Se comprueban además esquema, clase,
estado `CLOSED`, aprobador, commit y la única operación exacta autorizada.
No se añade una ruta configurable que permita sustituir la política aprobada.

SHA-256 inalterado:
`25d742d159890951d6d8bbe09ba6f221e9a3750a41fbc99ed6cc77156e0c7600`.

El servidor OCI se construye ahora desde la raíz:
`docker build -f server/Dockerfile .`. `.dockerignore` admite el servidor y
únicamente el YAML canónico dentro de `docs`; el workflow OCI usa ese contexto.
El JAR contiene los mismos bytes y excluye plantillas y secretos de prueba.

## Configuración, IP y HMAC

`propractix.rate-limit` se enlaza a `RateLimitProperties` mediante un boundary
que descarta las causas de binding: estas podrían incluir el valor rechazado.
La configuración valida también propiedades desconocidas. El modo inicial es
`REMOTE_ADDRESS`, sin proxies ni contrato de saneamiento activado.
`X_FORWARDED_FOR` exige simultáneamente CIDR explícitos y el contrato de Caddy
configurado y probado; se rechaza `FORWARDED` según la selección del plan.
Se rechazan CIDR globales `/0`, prefijos inválidos o ambiguos y redes con bits
de host. Los CIDR se comparan contra la dirección completa, antes de reducirla.

La resolución directa y el peer no confiable ignoran ambas familias de headers.
Desde un peer confiable solo se admite una cabecera XFF, sin `Forwarded`, con
cadena completa válida y acotada; se recorre de derecha a izquierda hasta el
primer salto no confiable. Falta, ambigüedad o ausencia de cliente falla cerrada.
No se hacen consultas DNS: solo se aceptan literales numéricos estrictos.

IPv4 se representa como `ipv4:<8 hex minúsculas>/32`; IPv6 como
`ipv6:<32 hex minúsculas con los últimos 64 bits a cero>/64`. Se elimina el
zone ID válido antes del parseo y la IPv4 mapeada se trata como IPv4.
La entrada HMAC es el separador de S0 seguido de esa representación UTF-8,
sin LF adicional. Se usa JCA HmacSHA256 y base64url sin padding, con todas las
versiones activas; una clave ausente o débil impide crear el adapter.
Los vectores verifican también separación de propósito y copia defensiva de
claves; no implementan el procedimiento operativo de rotación de E5B.

Los secretos se leen del config tree como material UTF-8 externo de al menos
32 bytes, fuera de las propiedades enlazadas. `infra:prepare` crea el archivo
local ignorado desde la plantilla sintética `.example`, conserva material
existente y aplica directorio `0700` y archivos `0600`. El perfil de pruebas
usa un archivo sintético exclusivo de test resources, ausente del JAR.

Las excepciones solo conservan `rate_limiter_unavailable` y una clase acotada
(`configuration`, `key`, `client_ip`, `storage`), sin causas ni valores libres.
Las representaciones diagnósticas de red, fingerprints, claves/versiones y
configuración están redactadas. Las pruebas capturan logs y stack traces para
detectar IP, material HMAC, versiones, pseudónimos o valores rechazados.

## Validación local — 2026-10-22

- JDK 21 y Node 24 disponibles.
- `npm run infra:up`: PostgreSQL 16 y Mailpit saludables.
- `npm run server:test`: correcto. La primera ejecución aislada del sandbox
  no accedió al socket Docker; se repitió con acceso local y terminó en verde.
- `npm run server:verify`: 188 pruebas, 0 fallos, 0 errores, 1 benchmark omitido;
  incluye Testcontainers PostgreSQL, ArchUnit, empaquetado y las pruebas E5A.
- `npm run infra:config`, `check-local-baseline.sh`, sintaxis de scripts,
  `check-migrations.sh` y `deployment-gate-test.sh`: correctos; no despliegan.
- `npm run openapi:lint`: correcto, sin modificar el contrato.
- Lint, typecheck, pruebas y build del cliente: correctos, 45 pruebas Vitest y
  la comprobación Node de i18n; sin cambios frontend.
- Comparación binaria del recurso dentro del JAR contra el YAML canónico y
  comprobación de ausencia de secretos/plantillas en el JAR: correctas.
- `git diff --check`, diff completo y `git status`: revisados antes del commit.

## Evidencia remota del commit funcional aprobado

Sobre `6cc21e6c034b9c32db3f22ebb8b977f4250614cd` terminaron correctamente:

- [CI #37](https://github.com/omarjtoscano/propractix-v2/actions/runs/39809605429):
  cinco jobs correctos; backend con 188 pruebas, 0 fallos, 0 errores y
  1 benchmark omitido.
- [dependency-and-filesystem-scan, incluido Trivy](https://github.com/omarjtoscano/propractix-v2/actions/runs/39809605429/job/119931969449):
  `success`, incluido el paso de escaneo.
- [Build OCI release candidate #20](https://github.com/omarjtoscano/propractix-v2/actions/runs/39809605488):
  `success`; servidor y cliente AMD64/ARM64, manifiesto y subida de artefactos.

Ambos runs identifican ese head. El merge virtual de la PR
`d63dbd40f192f9b2bc760bc261a2f14c67f04e9e` tiene el mismo árbol
`d33b26779e1016d12327db5bd05fb4939cfbae34` que el commit aprobado.
El artefacto OCI `12440051904` está asociado a ese head; no se realizó merge.

## Registro documental de aceptación — 2026-10-23

El Product Owner aprobó expresamente E5A sobre el commit funcional indicado.
Esta actualización solo registra estado, planes y evidencia; no modifica código
ni infraestructura. I1-H02 permanece `IN_PROGRESS`, E5B y posteriores siguen
sin autorización y `CL0_CLOUD_STAGING` permanece `PENDING`.
La PR #12 continúa Draft y sin merge.

Se comprueban `git diff --check`, el diff completo y `git status` antes del
commit. No se atribuyen nuevas ejecuciones locales de servidor o cliente a
este cambio documental. El nuevo commit se publica en la misma rama
`feature/i1-h02-e5a-rate-limit-foundation`; su SHA y las verificaciones
independientes de CI, Trivy y Build OCI se registran en el cuerpo de la PR #12
tras comprobarlas, sin trasladar el verde del commit funcional aprobado.

La evidencia de sanitización E5A no sustituye las futuras comprobaciones de
HTTP, Caddy, BD, métricas y trazas.


## Integración confirmada y autorización separada de E5B

El Product Owner confirmó el 2026-10-23 la integración de E5A mediante
`8f8f1b0bda8c9d066ba4751272939b4ae5cf6590` y autorizó exclusivamente E5B en
`feature/i1-h02-e5b-rate-limit-persistence`. Las referencias anteriores a la
PR Draft y a E5B sin autorización documentan el estado de la aceptación previa;
esta decisión posterior las sustituye. E5B e I1-H02 permanecen `IN_PROGRESS`,
sin aceptación de E5B ni merge de su PR; E5C y posteriores no se autorizan y
`CL0_CLOUD_STAGING` permanece `PENDING`.

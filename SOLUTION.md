# Solución técnica

## Resumen

Servicio de gestión de proveedores de la empresa, implementado como una solución full-stack sobre
arquitectura hexagonal (backend) y una SPA en React (frontend), orquestados con Docker Compose
junto a PostgreSQL y el mock del servicio de países (WireMock).

Estado de la implementación:

- **Backend**: los 7 endpoints definidos en el contrato OpenAPI están implementados y verificados
  end to end contra PostgreSQL real y, para `accept`, contra el servicio de países mockeado a
  través de un Circuit Breaker de resilience4j realmente conectado a la petición HTTP. La
  compatibilidad con el contrato OpenAPI se verifica además de forma automática (ver
  [Validación automática contra el contrato OpenAPI](#validación-automática-contra-el-contrato-openapi)).
- **Frontend**: el dashboard de proveedores potenciales implementa todos los requisitos de la
  tabla de la sección "Frontend" del `Readme.md` (búsqueda con validación de mínimo 250, tabla
  ordenable/filtrable, filtros de cliente por nombre/DUNS/país/rating, paginación `limit`/`offset`
  con distinción entre total y visibles, estados de carga/error/vacío —con mensajes distintos según
  su origen, ver [Estados vacíos del dashboard](#estados-vacíos-del-dashboard)—, y protección frente
  a respuestas de red fuera de orden mediante `AbortController`).
- **Pruebas**: la suite de backend cubre dominio, aplicación, mappers, controladores, contrato
  OpenAPI, persistencia (con Testcontainers), concurrencia/rollback, el adaptador del servicio de
  países (con WireMock embebido, en contextos separados para evitar inestabilidad temporal) y la
  arquitectura hexagonal (ArchUnit); la suite de frontend cubre formateadores, hooks y el
  `Dashboard` de forma integrada. Los recuentos exactos y el resultado de la última ejecución en
  este entorno están en [Cómo ejecutar las pruebas](#cómo-ejecutar-las-pruebas).
- **Aspectos dejados fuera**: documentados explícitamente en su propia sección, con la
  justificación de cada uno.

## Cómo ejecutar la solución

Requisito único: Docker y Docker Compose. No hace falta instalar Java, Node ni ningún SDK.

```bash
docker compose up --build
```

Esto levanta los cuatro servicios (`country-service`, `db`, `backend`, `frontend`) en orden,
esperando a que cada dependencia esté saludable (`depends_on: condition: service_healthy`) antes
de arrancar el siguiente. El arranque completo (Flyway + validación de esquema de Hibernate +
Tomcat) suele tardar entre 20 y 40 segundos para el `backend`.

Variables de entorno opcionales para el backend (ya traen un valor por defecto sensato en
`docker-compose.yml`/`application.yml`):

| Variable | Valor por defecto | Propósito |
|---|---|---|
| `COUNTRY_SERVICE_CONNECT_TIMEOUT_MS` | `1000` | Timeout de conexión HTTP al servicio de países |
| `COUNTRY_SERVICE_READ_TIMEOUT_MS` | `2000` | Timeout de lectura/respuesta HTTP al servicio de países |

Para parar la solución sin perder los datos sembrados:

```bash
docker compose down
```

## URLs y servicios

| Servicio | URL | Notas |
|---|---|---|
| Frontend | http://localhost:5173 | Dashboard servido por Nginx |
| Backend | http://localhost:8080 | API REST, contrato en `wiki/iop_tech-supplier_flow-main-openapi3_1.yaml` |
| Country service (WireMock) | http://localhost:8088 | Mock proporcionado, contrato en `wiki/iop_tech-supplier_flow-country-openapi3_1.yaml` |
| Postgres | localhost:5432 | Usuario/contraseña/BD: `supplier` |

Endpoint de salud del backend: `GET http://localhost:8080/actuator/health`.

## Cómo ejecutar las pruebas

### Backend

El repositorio incluye Maven Wrapper, por lo que no es necesario tener Maven instalado:

```bash
cd backend
./mvnw test
./mvnw package
```

Última ejecución verificada en este entorno (macOS, Docker Desktop, Java 25 vía `sdkman` como JDK
del sistema, Maven 3.9.9 descargado por el wrapper):

- `./mvnw test` ejecutado **tres veces consecutivas**: **124 tests ejecutados, 0 fallos** en las
  tres ejecuciones. La suite de `CountryCheckAdapterTest` (antes una única clase con un timeout
  corto compartido entre casos funcionales y el caso de timeout lento, causa de los fallos
  intermitentes de `Read timed out` reportados anteriormente en este documento) está ahora separada
  en tres clases con contextos Spring independientes — ver
  [Estabilidad de los tests del servicio de países](#estabilidad-de-los-tests-del-servicio-de-países) —
  y no ha mostrado ningún fallo intermitente en ninguna de las tres ejecuciones.
- `./mvnw package`: **BUILD SUCCESS** (vuelve a ejecutar los 124 tests como parte del ciclo
  `test`, sin `-DskipTests`).

Los tests que dependen de Testcontainers necesitan un daemon Docker accesible. En este entorno
concreto hizo falta `TESTCONTAINERS_RYUK_DISABLED=true` para que el sidecar de limpieza de
Testcontainers (Ryuk) arrancara correctamente contra este Docker Desktop; sin esa variable, los
tests `@Testcontainers`/`@SpringBootTest` con Postgres o WireMock fallaban por un `ContainerLaunch`
fallido, no por el código bajo test. Un entorno CI con acceso directo al daemon de Docker
normalmente no necesita esta variable.

### Frontend

```bash
cd frontend
npm ci
npm test
npm run lint
npm run build
```

Última ejecución verificada en este entorno:

- `npm test` (Vitest): **30/30 tests, 5 ficheros, 0 fallos**
  (`formatters.test.ts`, `useClientFilters.test.ts`, `useTableSort.test.ts`,
  `SearchForm.test.tsx`, `Dashboard.test.tsx` — este último con los dos casos de estado vacío
  verificados por separado, ver [Estados vacíos del dashboard](#estados-vacíos-del-dashboard)).
- `npm run lint` (ESLint 9, flat config): sin errores ni avisos.
- `npm run build` (`tsc -b && vite build`): compilación TypeScript y build de Vite correctos.

### End-to-end (Playwright + Gherkin)

Suite E2E en [`e2e/`](e2e/README.md): escenarios Gherkin (`.feature`) ejecutados con Playwright
(vía `playwright-bdd`) contra el stack completo levantado en Docker (navegador → nginx/SPA →
Spring Boot → PostgreSQL/WireMock), en un proyecto Compose propio con base de datos efímera
(tmpfs). Solo requiere Node 20+ y Docker:

```bash
cd e2e
npm ci
npx playwright install chromium
npm run e2e          # construye, arranca, siembra datos, ejecuta y destruye el stack
npm run e2e:headed   # con navegador visible
npm run e2e:ui       # Playwright UI mode
npm run e2e:report   # informe HTML de la última ejecución
```

Última ejecución verificada en este entorno: **30/30 escenarios** (24 del dashboard, en
paralelo, + 6 de ciclo de vida), sin reintentos. Estabilidad comprobada repitiendo cada escenario
5 veces con 6 workers y `--retries 0` (120/120 y 54/54). Estrategia de datos, separación entre
escenarios genuinos e interceptados y decisiones de diseño en [`e2e/README.md`](e2e/README.md).

## Arquitectura

Hexagonal / puertos y adaptadores, con un único agregado, `SupplierRecord` (identidad = DUNS), en
lugar de entidades separadas `Candidate`/`Supplier`:

```
domain/            Java puro, sin imports de framework — agregado SupplierRecord, value objects,
                    el enum SupplierStatus (5 estados), excepciones de negocio.
application/       port/in (una interfaz por caso de uso), port/out (repositorio + comprobación
                    de país), service (implementación de los casos de uso; @Transactional
                    únicamente aquí).
infrastructure/     web (controladores/DTOs/mappers/manejador de excepciones), persistence
                    (entidad JPA, repositorio, adaptador, mapper — la entidad JPA nunca se
                    reutiliza como objeto de dominio), external.country (cliente HTTP + adaptador
                    con Circuit Breaker), config.
```

La regla arquitectónica se hace cumplir en tiempo de compilación con `HexagonalArchitectureTest`
(ArchUnit, 4 reglas): `domain` no depende de ningún paquete de framework
(`org.springframework..`, `jakarta..`, `org.hibernate..`, `io.github.resilience4j..`); `domain` no
depende de `application` ni de `infrastructure`; `application` no depende de `infrastructure`; y
`@Transactional` solo se usa dentro de `application.service`.

Una única restricción `UNIQUE(duns)` en la tabla `supplier_record` (ver
`V1__create_supplier_record_table.sql`) hace automáticas las tres reglas de integridad del
`Readme.md` (sección "Integrity Rules") sin necesidad de comprobaciones cruzadas entre entidades:
solo una candidatura activa por DUNS, solo un proveedor por DUNS, y un candidato activo y un
proveedor nunca coexisten para el mismo DUNS.

## Modelo de dominio y ciclo de vida

`SupplierRecord` es el único agregado, con estado `SupplierStatus`:
`CANDIDATE`, `ACTIVE`, `ON_PROBATION`, `REFUSED`, `BANNED`.

Transiciones implementadas en el propio agregado (nunca mutando campos desde fuera):

```
apply()                          ->  CANDIDATE
CANDIDATE --accept(rating)-->        ACTIVE (rating A/B) | ON_PROBATION (rating C/D/E)
CANDIDATE --refuse()-->               REFUSED
REFUSED --reapply(datos nuevos)-->    CANDIDATE   (limpia el rating anterior)
ON_PROBATION --ban()-->               BANNED       (terminal)
ACTIVE --restrict()-->                ON_PROBATION (sin endpoint expuesto, ver más abajo)
ON_PROBATION --promote()-->           ACTIVE       (sin endpoint expuesto, ver más abajo)
```

**`BANNED` es el único estado terminal.** `REFUSED` tiene exactamente una transición de salida
(`reapply`); el resto de operaciones mutadoras (`accept`/`refuse`/`ban`/`restrict`/`promote`)
siguen rechazándolo. `SupplierStatus#isTerminal` solo devuelve `true` para `BANNED`.

`ban()` únicamente es válido desde `ON_PROBATION`, nunca desde `ACTIVE`
(`SupplierNotBannableException` en cualquier otro caso) — es la transición con más riesgo de
implementarse mal ("banear desde Active u On Probation"), por lo que tiene un test de regresión
dedicado tanto en `SupplierRecordTest` como en `BanSupplierServiceTest`.

### API pública vs. estado interno

El esquema OpenAPI de `Supplier.status` solo admite `[Active, Disqualified]` — no puede representar
`ON_PROBATION`. El mapeo está confinado a `infrastructure.web.mapper.SupplierWebMapper`, nunca al
dominio:

| Estado interno | `status` expuesto |
|---|---|
| `ACTIVE`, `ON_PROBATION` | `Active` |
| `BANNED` | `Disqualified` |

Visibilidad de recursos (confinada también al límite web/aplicación, vía
`SupplierRecord#isVisibleAsCandidate`/`#isVisibleAsSupplier`):

| Endpoint | 200 cuando el estado interno es | 404 en cualquier otro caso |
|---|---|---|
| `GET /candidates/{duns}` | `CANDIDATE`, `REFUSED` | otro estado, o sin registro |
| `GET /suppliers/{duns}` | `ACTIVE`, `ON_PROBATION`, `BANNED` | otro estado, o sin registro |

Esto no está 100% explícito en el OpenAPI — se deduce de que "candidato" y "proveedor" son dos
vistas sobre un mismo agregado, y un registro solo puede ser visible a través de uno de los dos
recursos en cada momento.

## Cumplimiento de las reglas de negocio

| Regla | Implementación |
|---|---|
| Aceptación requiere país aprobado y facturación ≥ 1.000.000 € | `AnnualTurnover#meetsMinimumForAcceptance`, `CountryCheckPort` vía `AcceptCandidateService` |
| Rating A/B → Active; C/D/E → On Probation | `SustainabilityRating#qualifiesForActive`, `SupplierRecord#accept` |
| Una candidatura rechazada permite reaplicar | `SupplierRecord#reapply` (ver tabla siguiente) |
| Un proveedor en probation puede ser baneado; un baneado no puede volver a serlo | `SupplierRecord#ban`, `SupplierNotBannableException` |
| Solo una candidatura activa y un proveedor por DUNS | `UNIQUE(duns)` + `SupplierRecord` como agregado único |
| `status` no distingue Active de On Probation | `SupplierWebMapper` (tabla anterior) |

**`POST /candidates` para un DUNS existente** — comportamiento completo:

| Estado existente | Comportamiento | HTTP |
|---|---|---|
| *(sin registro)* | `SupplierRecord.apply` — nueva candidatura | 201 |
| `REFUSED` | `SupplierRecord.reapply` — se actualizan los campos, se limpia el rating, vuelve a `CANDIDATE` | 201 |
| `BANNED` | `SupplierBannedException` | 409 `{"info":"Supplier banned"}` |
| cualquier otro (`CANDIDATE`, `ACTIVE`, `ON_PROBATION`) | `CandidateAlreadyExistsException` | 409 `{"info":"Candidate already exists"}` |

La transición está encapsulada por completo en el agregado: `RegisterCandidateService` nunca muta
campos de `SupplierRecord` directamente, solo decide qué método invocar según el estado existente.
Ver `SupplierStatus`, el javadoc de `SupplierRecord#reapply` y
`RegisterCandidateServiceTest#reappliesWhenExistingRecordIsRefused`.

### `restrict`/`promote`: en dominio y aplicación, sin endpoint

El diagrama de estados (`wiki/iop-techtest-fsm-supplier.png`) dibuja las transiciones
`Active --Restrict--> On Probation` y `On Probation --Promote--> Active`, ausentes tanto del texto
de negocio del `Readme.md` como del contrato OpenAPI (no existe `/suppliers/{duns}/restrict` ni
`/suppliers/{duns}/promote`). **Decisión**: los métodos de dominio, los casos de uso
(`port/in`), los servicios de aplicación y las excepciones dedicadas
(`SupplierNotRestrictableException`, `SupplierNotPromotableException`) existen y están
probados, pero **no se exponen mediante ningún endpoint** — añadir un endpoint público no
especificado sería extender el contrato por cuenta propia. Si el contrato llegara a incluir estas
operaciones, solo `infrastructure.web` necesitaría código nuevo.

## Contrato API

Los 7 endpoints de `wiki/iop_tech-supplier_flow-main-openapi3_1.yaml` están implementados:

| Método y ruta | Descripción |
|---|---|
| `POST /candidates` | Alta de candidato (o reapply si el DUNS está en `REFUSED`) |
| `GET /candidates/{duns}` | Consulta de un candidato |
| `POST /candidates/{duns}/refuse` | Rechazo de un candidato |
| `POST /candidates/{duns}/accept` | Aceptación de un candidato (requiere país y rating) |
| `GET /suppliers/{duns}` | Consulta de un proveedor |
| `POST /suppliers/{duns}/ban` | Baneo de un proveedor en probation |
| `GET /suppliers/potential` | Listado de proveedores potenciales para un `rate` dado |

### Gap conocido: 422 en `POST /candidates`

El OpenAPI declara una respuesta `422 Unprocessable Content` en `POST /candidates`, pero ninguna
regla de negocio del `Readme.md` la distingue claramente de un `400` (esquema inválido, cubierto
por Bean Validation) o un `409` (duplicado/baneado, cubierto por las dos excepciones de dominio
existentes). **No se implementa.** `GlobalExceptionHandler` documenta este vacío en su javadoc de
clase en lugar de inventar una regla de negocio solo para justificar una respuesta 422. Si en el
futuro se define una condición diferenciada (por ejemplo, un código de país sintácticamente válido
pero inexistente para el servicio de países, distinto de un país meramente no aprobado), este es
el lugar natural para añadirla.

## Validación automática contra el contrato OpenAPI

`infrastructure.web.contract.OpenApiContractTest` (36 tests) valida automáticamente, contra el
propio fichero `wiki/iop_tech-supplier_flow-main-openapi3_1.yaml` (nunca copiado ni
modificado — se referencia por ruta relativa desde `backend/`), que la implementación real de los
7 endpoints es compatible con el contrato: rutas, métodos, parámetros (tipo, `minimum`/`maximum`),
cuerpos de petición y respuesta (propiedades requeridas, tipos, enums), `content-type` y código
HTTP, para cada código declarado que la implementación puede producir (`POST /candidates`:
201/400/409; `GET /candidates/{duns}`: 200/404; `POST /candidates/{duns}/accept`: 204/400/404/409;
`POST /candidates/{duns}/refuse`: 204/404/409; `GET /suppliers/{duns}`: 200/404;
`POST /suppliers/{duns}/ban`: 204/404/409; `GET /suppliers/potential`: 200/400), más los casos
límite: DUNS mínimo/máximo, `rate = 250`/`< 250`, `limit = 1/10/0/11`, `offset = 0/< 0`, rating
válido/inválido, campos obligatorios ausentes, y una respuesta de `GET /suppliers/potential` con un
elemento completamente poblado (todos los campos de `PotentialSupplier`).

**Con una excepción real y documentada, no oculta bajo el "36/36 en verde"**: un `duns` de path
sintácticamente válido pero fuera de `[100000000, 999999999]` (`GET /candidates/{duns}`,
`GET /suppliers/{duns}`, `POST /candidates/{duns}/refuse`, `POST /suppliers/{duns}/ban`) produce hoy
un `400` que el contrato **no declara** para esas operaciones (solo declaran 2xx/404/409) — ver
[el hallazgo detallado más abajo](#otro-hallazgo-real-documentado-pero-no-corregido-fuera-de-alcance).
Ese caso concreto se comprueba solo por código de estado, deliberadamente sin encadenar la
validación de contrato, porque ninguna respuesta 400 está declarada ahí para que el test pueda
compararse contra ella.

### Librería y por qué

`com.atlassian.oai:swagger-request-validator-mockmvc` (ahora renombrada
`openapi-request-validator`) es la librería más madura con un `ResultMatcher` de MockMvc ya
integrado para Spring Boot — exactamente la pieza que este test usa (`OpenApiInteractionValidator`
+ `openApi().isValid(...)`). Su versión actual (3.0.0) exige Spring Framework 7/Spring Boot 4, así
que se fija deliberadamente la última versión compatible con Spring Boot 3.3.4: **2.46.1** (`pom.xml`,
scope `test`). Se añade también `org.awaitility:awaitility` (scope `test`), usada en la suite del
Circuit Breaker (ver más abajo), sin relación con esta librería.

### Límite real de compatibilidad con OpenAPI 3.1 (verificado empíricamente, no asumido)

El parser subyacente de esta versión (swagger-parser-v3) es anterior al soporte completo de OpenAPI
3.1: lee un documento `openapi: 3.1.0` sin rechazarlo, pero lo valida con semántica de OpenAPI
3.0/JSON Schema Draft-4, no con el dialecto JSON Schema 2020-12 en el que se basa 3.1. Para
**este** contrato en concreto, comprobado ejecutando la suite completa (36/36 tests en verde):

- **Se valida por completo**: rutas, métodos, presencia/tipo/límites de parámetros de ruta y
  query, cuerpos de petición obligatorios, códigos de estado de respuesta, `content-type` de
  respuesta, y forma del JSON (propiedades requeridas, tipos, `minLength`/`maxLength`, límites
  numéricos, enums) — ninguno de los esquemas de este contrato usa una construcción exclusiva de
  3.1 (no hay `type` como array para nulabilidad, ni `prefixItems`, ni `const`, ni forma booleana de
  `exclusiveMinimum`/`exclusiveMaximum`).
- **No se valida / se ignora en silencio**: la anotación `examples: [...]` en plural (estilo 3.1;
  3.0 usa `example` singular) presente en todo el contrato. Es una anotación no normativa, no una
  restricción — un parser que no la entiende simplemente no valida nada a partir de ella, no genera
  falsos negativos sobre restricciones reales.
- **Gotcha documentado de la propia librería, no un problema de 3.1**: por defecto, esta versión
  inyecta `additionalProperties: false` en cada rama de un `allOf` (aquí, `Supplier` y
  `PotentialSupplier`, compuestos como `allOf: [Candidate, {campos extra}]`), lo que rompe
  cualquier composición `allOf` porque ninguna rama declara el 100% de las propiedades del objeto
  final — un problema de JSON Schema general con `allOf`, no específico de 3.1. Se desactiva
  exactamente esa comprobación (`LevelResolver` con la clave
  `validation.schema.additionalProperties` a `IGNORE`, el propio mecanismo que la librería expone
  para este caso — ver el código fuente de `SchemaValidator`), dejando activas todas las demás
  reglas de validación de esquema.

En resumen: para un contrato como este —etiquetado 3.1 pero sin usar construcciones exclusivas de
3.1— la librería da una validación estructural/de tipos/de rangos totalmente fiel. **No** sería
fiable contra un contrato que sí usara `type: [string, "null"]`, `prefixItems` o la forma numérica
de `exclusiveMinimum`/`exclusiveMaximum` — eso exigiría un validador nativo de JSON Schema 2020-12
(p. ej. `networknt/json-schema-validator`), a costa de perder la integración lista para usar con
MockMvc que se aprovecha aquí.

### Peticiones deliberadamente inválidas: solo se valida la respuesta

Para los casos límite que fuerzan un 400 (campo obligatorio ausente, DUNS/`rate`/`limit`/`offset`
fuera de rango, rating con un valor de enum inválido), la propia petición viola el contrato por
construcción — no tiene sentido pedirle a la librería que valide una petición deliberadamente
inválida contra su propio esquema. Estos tests usan `matchesResponseContractOnly()` (helper propio,
`OpenApiInteractionValidator#validateResponse`), que valida solo que la respuesta —el 400 declarado
y su esquema `Error`— es la que promete el contrato, sin exigir que la petición en sí sea válida.

### Hallazgo real detectado y corregido dentro de este alcance

Al escribir el caso "rating con valor de enum inválido" (`{"sustainabilityRating":"Z"}`), la
petición fallaba en la deserialización JSON antes de llegar a Bean Validation
(`HttpMessageNotReadableException`), y `GlobalExceptionHandler` no la capturaba — el 400 resultante
llegaba con el cuerpo vacío, incumpliendo el esquema `Error` que el contrato exige para todo 400.
Se añadió un `@ExceptionHandler(HttpMessageNotReadableException.class)` (mismo patrón que el resto
de la clase: 400 + `{"info": "..."}"`) — no es una regla de negocio nueva ni un endpoint nuevo, es
hacer que una respuesta 400 ya declarada cumpla el esquema ya declarado.

### Otro hallazgo real, documentado pero no corregido (fuera de alcance)

`getCandidateReturns400ForADunsOutsideTheContractRangeUndeclaredButReal` deja constancia de que un
DUNS sintácticamente válido pero fuera de `[100000000, 999999999]` en `GET /candidates/{duns}`
produce hoy un 400 (vía `IllegalArgumentException` al construir `new Duns(duns)` en la rama de "no
encontrado", tanto en el controlador como en `GetCandidateService`), aunque el contrato solo declara
200/404 para esa operación — nunca 400. El mismo patrón (construir el value object antes de decidir
"no encontrado") se repite en los demás endpoints con `{duns}`. No se corrige aquí: hacerlo
implicaría cambiar el comportamiento actual de varios casos de uso, lo cual excede el alcance de
"añadir validación de contrato" — queda documentado como lo pide el enunciado, no oculto.

## Cálculo de proveedores potenciales

```
score = annual_turnover × 0.1 × rating_constant × bonus
bonus = 1.25 si la facturación está entre las 2 facturaciones únicas más bajas del país; si no, 1
```

Al volumen indicado en el `Readme.md` (100.000 a 1.000.000 de proveedores), este cálculo no puede
hacerse cargando filas en la JVM. Se implementa como una única consulta nativa en
`SupplierRecordJpaRepository#findPotentialSuppliersRaw`: un CTE `WITH ranked AS (...)` calcula
`DENSE_RANK() OVER (PARTITION BY country ORDER BY annual_turnover)` sobre toda la población
`ACTIVE`/`ON_PROBATION` de cada país; la consulta externa filtra `annual_turnover > :rate`, calcula
el score y aplica `ORDER BY score DESC, duns ASC LIMIT/OFFSET`. Los resultados se leen mediante
`PotentialSupplierProjection` (una proyección de interfaz de Spring Data, ya que `score` no es una
columna real). `CANDIDATE`/`REFUSED` se excluyen de la población de ranking, no solo del resultado
final, porque no son proveedores y no tienen rating que puntuar.

**Desambiguación decidida**: el `Readme.md` no especifica si "las dos facturaciones únicas más
bajas del país" se calculan sobre todos los proveedores no descalificados del país, o solo sobre
el subconjunto elegible para el `rate` concreto de la consulta. **Se decidió lo primero**: el bonus
se calcula sobre todos los proveedores `ACTIVE`/`ON_PROBATION` del país, independientemente del
`rate`, convirtiéndolo en un rasgo estable del proveedor ("uno de los dos más pequeños de su
país") en lugar de algo que aparece y desaparece según la consulta. El ejemplo del `Readme.md` (5
proveedores de un país, sin mencionar ningún `rate`) encaja mejor con esta interpretación.
Verificado empíricamente: consultar con un `rate` que excluye del resultado a algunas de las filas
usadas en el ranking no cambia el bonus de las filas que sí aparecen.

**Paginación estable (desempate por `duns`)**: `score` por sí solo no es único — varios proveedores
pueden coincidir exactamente (misma facturación, rating y elegibilidad de bonus). `ORDER BY score
DESC` sin más no garantiza el orden relativo de las filas empatadas entre llamadas separadas de
`LIMIT`/`OFFSET`, lo que puede duplicar una fila en una página y perder otra en la siguiente.
Desempatar por `duns ASC` (ya `UNIQUE`) hace el orden total, por lo que paginar sobre filas
empatadas es determinista. Verificado en
`SupplierPersistenceAdapterTest#findPotentialSuppliersBreaksScoreTiesByDunsAscendingForStablePagination`.

**Medición con `EXPLAIN (ANALYZE, BUFFERS)` sobre 300.000 filas sembradas**:
`idx_supplier_record_status_turnover` sí es usado por el planificador (`Bitmap Index Scan`) para
el filtro `status IN ('ACTIVE','ON_PROBATION')`. `idx_supplier_record_country_turnover` **no** se
usa para el orden `(country, annual_turnover)` de la ventana `DENSE_RANK()` — Postgres hace un
ordenamiento explícito tras el bitmap scan (~540ms sobre ~120k filas elegibles de 300k, con o sin
spill a disco según `work_mem`). Un índice compuesto `(status, country, annual_turnover)` tampoco
cambió el plan elegido.

Esto **no es un problema de índices, sino estructural**: como el bonus depende de rankear toda la
población elegible por país, Postgres tiene que materializar y rankear (casi) todas las filas
`ACTIVE`/`ON_PROBATION` antes de poder ordenar por score y aplicar `LIMIT` — ningún índice permite
un atajo de "top 10", porque la respuesta correcta depende de conocer el rango de cada fila dentro
de su país. Al extremo superior del rango de 100k-1M filas, la mejora honesta sería un ranking por
país precalculado y cacheado en lugar de `DENSE_RANK()` por petición — una desnormalización fuera
del alcance de esta prueba (ver [Aspectos no implementados](#aspectos-no-implementados)).

## Persistencia, integridad y concurrencia

### Optimistic locking

`SupplierRecordEntity` incluye una columna `@Version` (`version`,
`V2__add_supplier_record_version.sql`, puramente aditiva sobre `V1`). Dos transacciones que leen
la misma fila antes de que ninguna haga commit provocan que la segunda falle al hacer flush con
`ObjectOptimisticLockingFailureException`, en lugar de sobrescribir en silencio el cambio de la
primera. `SupplierPersistenceAdapter#save` usa `saveAndFlush` para que el conflicto aflore de forma
síncrona dentro de la propia llamada. `GlobalExceptionHandler` lo traduce a
`409 {"info":"Supplier record was modified concurrently, please retry"}`. Aplica de manera
uniforme a `accept`, `refuse`, `ban` y `reapply`.

Verificado en
`ConcurrencyIntegrationTest#concurrentUpdatesToTheSameRowDoNotSilentlyOverwriteEachOther` contra
PostgreSQL real — a nivel de `SupplierPersistenceAdapter`, orquestando manualmente dos transacciones
superpuestas con `TransactionTemplate` para demostrar que el conflicto de versión impide una
actualización perdida y que el estado final en base de datos es el de la transacción que ganó (no
una mezcla de ambas).

**Rollback a través del caso de uso real**: ese test no pasa por el método `@Transactional` real de
ningún servicio de aplicación (usa el adaptador directamente). Para cubrir exactamente eso —que
`AcceptCandidateService#accept`, el caso de uso real detrás de `POST /candidates/{duns}/accept`,
haga rollback completo y no dejar una transición a medias cuando el optimistic locking detecta un
conflicto—, `AcceptCandidateServiceConcurrencyIntegrationTest#concurrentAcceptCallsForTheSameCandidateNeverPersistAMixOfBothOutcomes`
lanza dos hilos reales contra el mismo `AcceptCandidateUseCase.accept(duns, rating)` para un mismo
candidato, cada uno pidiendo un rating distinto (`A` → `ACTIVE`, `D` → `ON_PROBATION`) sobre
PostgreSQL real (Testcontainers). Verifica tanto la excepción (`ObjectOptimisticLockingFailureException`
en el hilo perdedor) como el estado final en base de datos, y la aserción crítica es más fuerte que
"el valor antiguo sobrevivió": el registro final debe ser **exactamente** una de las dos transiciones
completas (`ACTIVE`+`A` o `ON_PROBATION`+`D`), nunca una combinación imposible como `ACTIVE`+`D` —
que es justo lo que produciría una transacción parcialmente aplicada. No duplica la cobertura de
`ConcurrencyIntegrationTest`: prueba una capa distinta (el servicio `@Transactional` real, bajo
concurrencia real de hilos, no una orquestación manual de transacciones) con una aserción de
atomicidad más exigente.

### Restricción única y la carrera de inserción concurrente

La secuencia `findByDuns` + `save` de `RegisterCandidateService` tiene una carrera TOCTOU
inherente: dos peticiones pueden observar "no existe registro" y ambas intentar insertar. La
restricción `uk_supplier_record_duns` es la última línea de defensa, pero una violación de
restricción nunca debe llegar al cliente como un 500 sin mapear.
`SupplierPersistenceAdapter#save` captura `DataIntegrityViolationException`, inspecciona la cadena
de causas buscando específicamente esta restricción, relee la fila que ganó la carrera y lanza
`SupplierBannedException` si esa fila está `BANNED`, o `CandidateAlreadyExistsException` en caso
contrario — las mismas dos excepciones que ya maneja `GlobalExceptionHandler` para el caso sin
carrera.

Un matiz encontrado probando contra la pila real de `docker compose up`: releer la fila ganadora a
través de la misma transacción del `saveAndFlush` fallido no funciona, porque tras un flush fallido
la sesión de Hibernate queda "envenenada" y cualquier operación posterior repite el mismo fallo —
esto convertía cada inserción perdedora en un 500 no mapeado cuando `save()` corría dentro del
único método `@Transactional` de `RegisterCandidateService` (a diferencia de llamar a `save()` de
forma aislada, donde cada invocación obtiene su propia mini-transacción). Solucionado releyendo la
fila ganadora en una transacción nueva e independiente (`TransactionTemplate` con
`PROPAGATION_REQUIRES_NEW`, programático para no violar la regla de `HexagonalArchitectureTest` de
que `@Transactional` solo se usa en `application.service`).

Verificado en `ConcurrencyIntegrationTest#concurrentInsertsForTheSameDunsAreSerializedByTheUniqueConstraint`
(a nivel de adaptador) y en `RegisterCandidateServiceConcurrencyIntegrationTest` (a través del
servicio real, la única forma de reproducir el matiz anterior) — ambos con PostgreSQL real vía
Testcontainers, y confirmado además disparando peticiones `POST /candidates` concurrentes reales
contra la pila de `docker compose up`: siempre `201`/`409`, nunca un `500`.

## Integración con el servicio de países

`CountryClient` (un `RestClient` que llama a `GET /countries/{country}`) está envuelto por
`CountryCheckAdapter` con `@CircuitBreaker(name = "countryService")` de resilience4j; su fallback
siempre lanza `CountryCheckUnavailableException` — nunca devuelve un booleano — para que la regla
de fail-safe (no asumir nunca que un país no está baneado si no se puede comprobar) no pueda
saltarse por accidente. `AcceptCandidateService` captura esa excepción y la trata como "país
baneado".

**Timeouts HTTP reales**: `RestClientConfig` configura `connectTimeout`/`readTimeout` en el
`ClientHttpRequestFactory`, parametrizados por `COUNTRY_SERVICE_CONNECT_TIMEOUT_MS`/
`COUNTRY_SERVICE_READ_TIMEOUT_MS` (por defecto 1000ms/2000ms) — estos son los que realmente acotan
una llamada bloqueante; un `@TimeLimiter` de resilience4j no habría tenido efecto sobre una llamada
síncrona como `CountryClient#getCountry`, por lo que se eliminó en lugar de dejarlo como
configuración inerte.

### Estabilidad de los tests del servicio de países

La suite de `CountryCheckAdapter` está dividida en **tres clases**, cada una con su propio contexto
Spring (y por tanto su propia instancia del Circuit Breaker `countryService`, sin estado compartido
entre clases):

| Clase | Qué cubre | Timeout HTTP del contexto |
|---|---|---|
| `CountryCheckAdapterTest` | Casos funcionales 200/404 y `circuitBreakerOpensAfterRepeatedFailures` (el breaker abre tras 5 fallos y corta el tráfico hacia WireMock) | Los valores por defecto de `application.yml` (1000ms/2000ms) |
| `CountryCheckAdapterTimeoutTest` | `respondsWithinBoundedTimeAndFailsSafeOnSlowCountryService`: WireMock con 5s de retraso deliberado, la llamada se corta muchísimo antes de esos 5s | 300ms, deliberadamente corto — solo para este escenario |
| `CountryCheckAdapterCircuitBreakerRecoveryTest` | `circuitBreakerTransitionsFromOpenToHalfOpenToClosedOnceTheCountryServiceRecovers`: `OPEN` → `HALF_OPEN` → `CLOSED` una vez el servicio de países vuelve a responder | HTTP por defecto; `wait-duration-in-open-state=300ms` y `permitted-number-of-calls-in-half-open-state=1` solo en este contexto de test (no se toca el valor de producción, `10s` en `application.yml`) |

Antes de esta separación, un único timeout corto (300ms) se aplicaba a toda la clase, incluidos los
casos funcionales que stubean una respuesta casi instantánea de WireMock — bajo carga del host
(compilaciones, otros tests, `docker pull` en paralelo), ese margen de 300ms se superaba
ocasionalmente por motivos de entorno y no del código bajo test, produciendo fallos intermitentes de
`Read timed out`. Separar el escenario lento en su propio contexto permite que los casos funcionales
usen un timeout realista (el de producción) mientras el caso de timeout sigue siendo determinista
(300ms de margen frente a los 5s del stub deja sobra de sobra incluso con jitter del host).

La transición `HALF_OPEN`/`CLOSED` se verifica sin `Thread.sleep`: se usa Awaitility
(`org.awaitility:awaitility`, scope test) haciendo *polling* acotado (intervalo de 50ms, máximo 3s)
que reintenta la llamada real hasta que el breaker deja pasar la petición de prueba tras agotarse
`wait-duration-in-open-state` y esta tiene éxito. Un listener de eventos del propio
`CircuitBreaker` registra la secuencia de transiciones y la prueba afirma explícitamente que
`HALF_OPEN` ocurrió antes que el `CLOSED` final, no solo el estado final.

Verificado con tres ejecuciones consecutivas de `./mvnw test` sin ningún fallo intermitente — ver
[Cómo ejecutar las pruebas](#cómo-ejecutar-las-pruebas).

## Frontend

**Capa API**: `api/client.ts` (wrapper de `fetch` que resuelve `VITE_API_BASE_URL` y traduce el
esquema de error `{info}` a un `ApiClientError` tipado, distinguiendo fallos de red de respuestas
de error HTTP) y `api/suppliersApi.ts` (`GET /suppliers/potential`).

**Hooks**: `usePotentialSuppliers` (ciclo de vida de la petición — loading/error/data — más un flag
`hasSearched` para distinguir "nunca se ha buscado" de "se buscó y no hay resultados"),
`useClientFilters` (filtro de texto por nombre/DUNS, país y rating sobre la página ya cargada, sin
disparar una nueva llamada al backend), `useTableSort` (por defecto ordena por `score`/`desc` según
el `Readme.md`, alterna asc/desc en clics repetidos, ordena las columnas numéricas numéricamente).

**Componentes**: `SearchForm` (input numérico con mensaje personalizado de mínimo 250),
`LoadingIndicator`, `ErrorMessage`, `EmptyState`, `FiltersBar` (búsqueda libre, selector múltiple
de país, checkboxes de rating), `ResultsTable` (las 6 columnas del `Readme.md`, formato de
moneda/score, cabeceras ordenables con indicador asc/desc), `Pagination` (controles de
`limit`/`offset` más el recuento de resultados). `Dashboard` los compone a todos.

**Peticiones fuera de orden**: `usePotentialSuppliers` usa un `AbortController` para cancelar la
petición en curso al iniciar una nueva búsqueda (nuevo importe, o cambio de página); todos los
callbacks `then`/`catch`/`finally` comprueban `signal.aborted` antes de tocar el estado, y un
`isMountedRef` evita actualizar el estado tras desmontar el componente. Verificado en
`Dashboard.test.tsx#ignoresAStaleResponseThatResolvesAfterANewerOne`.

**Coherencia de filtros y contador**: una nueva búsqueda llama a `useClientFilters#reset()`
(limpia texto libre, países y ratings) para que un filtro heredado de la página anterior no deje
el nuevo resultado vacío de forma indistinguible de que el servidor no devolvió nada. `Pagination`
recibe el `total` del servidor y el `visibleCount` tras filtrar, y muestra
`"{visible} visible suppliers out of {total} total"` solo mientras hay algún filtro activo.

### Estados vacíos del dashboard

Existen dos causas distintas para que la tabla de resultados quede vacía, y cada una tiene su
propio mensaje — antes mostraban el mismo texto genérico, indistinguible entre sí:

| Causa | Mensaje | Dónde se decide |
|---|---|---|
| El backend devuelve cero proveedores elegibles para el `rate` buscado | "No potential suppliers found for this order amount." | `Dashboard` — `suppliers.length === 0` (antes de aplicar ningún filtro de cliente) |
| El backend devolvió proveedores, pero los filtros de cliente (nombre/DUNS/país/rating) ocultan todos los de la página cargada | "No suppliers on this page match the selected filters." | `Dashboard` — `visibleSuppliers.length === 0` (después de `useClientFilters#apply`) |

`EmptyState` es un único componente parametrizado por una prop `reason: 'no-results' |
'filtered-out'` (tipada, sin cadenas mágicas en el punto de uso) en lugar de dos componentes
duplicados — el marcado (`role="status"`, `aria-live="polite"`) es idéntico en ambos casos, solo
cambia el texto. La lógica de paginación y el conteo `"{visible} visible suppliers out of {total}
total"` no cambian: ambos estados vacíos conviven con `FiltersBar`/`Pagination` exactamente igual
que antes (el segundo caso, con filtros activos, sigue mostrando el contador de visibles frente al
total). Verificado en dos casos separados de `Dashboard.test.tsx` y manualmente contra la pila de
`docker compose up` (importe sin resultados vs. filtro de texto sin coincidencias sobre una página
no vacía).

**Limitación deliberada del contrato actual**: `GET /suppliers/potential` solo expone
`rate`/`limit`/`offset` — no hay filtro ni ordenación en servidor. Por tanto, la búsqueda de texto,
el filtro de país/rating y el ordenado por cabecera **solo operan sobre la página ya cargada**
(como máximo `limit`, 10 filas), nunca sobre el conjunto completo. Un proveedor que cumpliría un
filtro pero está en la página 3 no aparecerá hasta que el usuario navegue hasta ella. No es un
defecto del frontend: es la consecuencia honesta del contrato tal como está definido (ampliarlo
con nuevos parámetros quedaba fuera de alcance). Si se añadiera filtrado/ordenación en servidor,
`usePotentialSuppliers`/`suppliersApi.ts` serían los únicos puntos a cambiar.

## Rendimiento y escalabilidad

`findPotentialSuppliers` nunca materializa más de una página de entidades: todo el cálculo
(ranking, filtro de elegibilidad, score, orden, paginación) ocurre en una única consulta SQL
nativa. El benchmark con `EXPLAIN (ANALYZE, BUFFERS)` sobre 300.000 filas y la mejora identificada
pero no implementada (ranking por país precalculado) están documentados en
[Cálculo de proveedores potenciales](#cálculo-de-proveedores-potenciales), para no repetir el
análisis dos veces.

## Docker Compose y operabilidad

Versiones fijadas explícitamente en todas las imágenes (ninguna usa `latest`):

| Imagen | Versión | Uso |
|---|---|---|
| `wiremock/wiremock` | `3.9.1` | Mock del servicio de países (coincide con la versión de `wiremock-standalone` usada en tests) |
| `postgres` | `16-alpine` | Base de datos |
| `maven` (etapa de build) | `3.9-eclipse-temurin-21` | Compilación del backend |
| `eclipse-temurin` (imagen final) | `21-jre-jammy` | Runtime del backend |
| `node` (etapa de build) | `20-alpine` | Build del frontend |
| `nginx` (imagen final) | `1.27-alpine` | Servido del frontend |

**`curl` verificado como realmente presente en la imagen final** (comprobado extrayendo e
inspeccionando las imágenes publicadas, no solo asumido): tanto `eclipse-temurin:21-jre-jammy`
(backend) como `wiremock/wiremock:3.9.1` (country-service) incluyen `curl` de fábrica, así que los
healthchecks funcionan sin instalar nada adicional en el Dockerfile.

- `country-service`: `curl -f http://localhost:8080/__admin/mappings` — un 200 confirma que los
  mappings de WireMock están cargados.
- `backend`: `curl -f http://localhost:8080/actuator/health`, `start_period: 30s` (el arranque de
  Flyway + validación de esquema + contexto de Spring ronda los 20-40s) y 20 reintentos.
- `db`: `pg_isready -U supplier -d supplier`.
- `backend` depende de `db` y `country-service` con `condition: service_healthy`; `frontend`
  depende de `backend` con la misma condición.

Las imágenes finales son deliberadamente pequeñas: el backend es solo el `.jar` sobre un JRE (sin
JDK ni herramientas de build), y el frontend es el resultado estático de `vite build` servido por
`nginx:1.27-alpine`, sin Node en la imagen final. Ambos `.dockerignore` excluyen `.git`, artefactos
de IDE, logs y (en el frontend) `coverage`.

## Estrategia de pruebas

| Capa | Técnica | Qué cubre |
|---|---|---|
| `domain.model` | JUnit puro, sin mocks | Transiciones de estado, cada guarda, filtros de visibilidad, `reapply` |
| `application.service` | Mockito, sin contexto de Spring | Casos de uso, orquestación de puertos, mapeo de excepciones |
| `infrastructure.web.mapper` | JUnit puro | Mapeo interno ↔ DTO, incluida la traducción de estado |
| `infrastructure.web.controller` | `@WebMvcTest` + `MockMvc`, casos de uso mockeados | Códigos HTTP y cuerpo exacto `{"info": "..."}` de `GlobalExceptionHandler` |
| `infrastructure.web.contract` | `@WebMvcTest` + `MockMvc` + swagger-request-validator | Compatibilidad automática de los 7 endpoints con `wiki/iop_tech-supplier_flow-main-openapi3_1.yaml` (rutas, parámetros, cuerpos, códigos), incluidos los casos límite de DUNS/`rate`/`limit`/`offset`/rating |
| `infrastructure.persistence` | `@SpringBootTest` + Testcontainers (PostgreSQL real) | Upsert por DUNS, filtro de elegibilidad, score/bonus contra el ejemplo del `Readme.md`, paginación estable |
| `infrastructure.external.country` | `@SpringBootTest` acotado + WireMock embebido, en tres contextos separados | Circuit Breaker realmente conectado (AOP), timeouts efectivos, recuperación `OPEN`→`HALF_OPEN`→`CLOSED` (Awaitility, sin `Thread.sleep`) |
| Concurrencia y rollback | Testcontainers + hilos reales | Optimistic locking a nivel de adaptador y a través del caso de uso `@Transactional` real (sin transición parcial persistida), carrera de inserción concurrente |
| Arquitectura | ArchUnit | Las 4 reglas de la arquitectura hexagonal |
| Frontend | Vitest + Testing Library | Formateadores, hooks de filtro/orden, validación de `SearchForm`, `Dashboard` de forma integrada (carga, éxito, error, los dos estados vacíos por separado, paginación, filtros, orden, petición fuera de orden, reset de filtros) |
| End-to-end | Playwright + Gherkin (`playwright-bdd`) contra el stack Docker completo | Búsqueda, validación del importe mínimo, orden y formato exactos de resultados con score/bonus, filtros, ordenación, paginación, estados vacío/carga/error (red interceptada solo para fallos y latencia) y visibilidad en el dashboard de candidatos pendientes, rechazados, aceptados y baneados |

Los recuentos exactos de la última ejecución están en
[Cómo ejecutar las pruebas](#cómo-ejecutar-las-pruebas).

## Decisiones y trade-offs

| Decisión | Motivo |
|---|---|
| Agregado único `SupplierRecord` en vez de `Candidate`/`Supplier` separados | Son dos vistas del mismo ciclo de vida, no dos entidades independientes; evita duplicar y sincronizar datos entre dos tablas |
| `REFUSED` no es terminal, `BANNED` sí | El `Readme.md` es explícito: "a refused candidacy allows the candidate to reapply" (detalle en [Cumplimiento de las reglas de negocio](#cumplimiento-de-las-reglas-de-negocio)) |
| `restrict`/`promote` en dominio y aplicación, sin endpoint | El OpenAPI no los define; no se amplía el contrato por cuenta propia |
| Bonus de proveedor pequeño sobre toda la población del país, no solo el subconjunto elegible por `rate` | Ambigüedad del `Readme.md` resuelta a favor de un rasgo estable del proveedor (ver [Cálculo de proveedores potenciales](#cálculo-de-proveedores-potenciales)) |
| `resilience4j.timelimiter` eliminado, sustituido por timeouts HTTP reales | Un `@TimeLimiter` solo actúa sobre métodos asíncronos; `CountryClient` es una llamada síncrona, así que nunca tenía efecto |
| `saveAndFlush` + `TransactionTemplate REQUIRES_NEW` para releer tras violar la restricción única | Ver [Persistencia, integridad y concurrencia](#persistencia-integridad-y-concurrencia) |
| Sin autenticación/autorización | Fuera de alcance: el `Readme.md` describe "un supervisor" sin especificar modelo de autenticación |
| Sin idempotency key en `POST /candidates` | No la pide el OpenAPI (sin parámetro de cabecera definido) |
| `swagger-request-validator-mockmvc` fijado a `2.46.1`, no a la última (`3.0.0`) | `3.0.0` exige Spring Framework 7/Spring Boot 4; este proyecto usa Spring Boot 3.3.4 — ver [Validación automática contra el contrato OpenAPI](#validación-automática-contra-el-contrato-openapi) |
| `EmptyState` parametrizado por prop (`reason`), no dos componentes | Mismo marcado accesible en ambos casos, solo cambia el texto; evita duplicar el componente |

## Aspectos no implementados

- **422 en `POST /candidates`** — ver [el gap documentado](#gap-conocido-422-en-post-candidates).
  Ninguna regla de negocio del `Readme.md` lo distingue de 400/409.
- **Endpoints `restrict`/`promote`** — existen los stubs de dominio y aplicación, pero no hay
  contrato OpenAPI ni controlador para ellos.
- **Filtrado/ordenación en servidor para `GET /suppliers/potential`** — el contrato solo expone
  `rate`/`limit`/`offset`; el filtrado y ordenado de texto/país/rating del frontend son
  necesariamente client-side sobre la página cargada. Ver
  [Frontend](#frontend) para el detalle de esta limitación.
- **Ranking por país precalculado/cacheado** — mejora de escalabilidad identificada pero no
  implementada por ser un cambio de diseño (denormalización), no una optimización local. Ver
  [Rendimiento y escalabilidad](#rendimiento-y-escalabilidad).
- **Autenticación/autorización** — fuera de alcance según el `Readme.md`.
- **Idempotency key en `POST /candidates`** — no requerido por el contrato.
- **400 no declarado para un DUNS fuera de rango en los endpoints `{duns}`** — ver
  [el hallazgo documentado](#validación-automática-contra-el-contrato-openapi). Corregirlo
  implicaría cambiar el comportamiento de varios casos de uso existentes, fuera del alcance de
  "añadir validación automática de contrato".

## Matriz de cumplimiento

| Requisito (Readme.md) | Implementación | Prueba |
|---|---|---|
| Alta de candidato con datos obligatorios | `RegisterCandidateService`, `POST /candidates` | `RegisterCandidateServiceTest`, `CandidateControllerTest` |
| Rechazo de candidato | `RefuseCandidateService`, `POST /candidates/{duns}/refuse` | `RefuseCandidateServiceTest`, `SupplierRecordTest` |
| Reapply tras rechazo | `SupplierRecord#reapply` | `SupplierRecordTest#reapplyResetsToCandidateWithNewDataAndClearsRating`, `RegisterCandidateServiceTest#reappliesWhenExistingRecordIsRefused` |
| Aceptación con país/facturación/rating | `AcceptCandidateService`, `SupplierRecord#accept` | `AcceptCandidateServiceTest`, `SupplierRecordTest` |
| Rating A/B → Active, C/D/E → On Probation | `SustainabilityRating#qualifiesForActive` | `SustainabilityRatingTest`, `SupplierRecordTest` |
| Baneo solo desde On Probation | `SupplierRecord#ban` | `BanSupplierServiceTest#throwsWhenStatusIsActiveNotOnProbation`, `SupplierRecordTest#banOnlyValidFromOnProbationNotFromActive` |
| Baneado no puede reingresar | `SupplierBannedException` en `RegisterCandidateService` | `RegisterCandidateServiceTest` |
| Una candidatura/proveedor activo por DUNS | `UNIQUE(duns)` | `ConcurrencyIntegrationTest#concurrentInsertsForTheSameDunsAreSerializedByTheUniqueConstraint` |
| `status` no distingue Active/On Probation | `SupplierWebMapper` | `SupplierWebMapperTest` |
| Elegibilidad de proveedores potenciales (`turnover > rate`) | `findPotentialSuppliersRaw` | `SupplierPersistenceAdapterTest` |
| Fórmula de score y bonus del 25% | `findPotentialSuppliersRaw` | `SupplierPersistenceAdapterTest` (ejemplo exacto del `Readme.md`) |
| Comprobación de país no aprobado | `CountryCheckPort`/`CountryCheckAdapter` | `CountryCheckAdapterTest`, `AcceptCandidateServiceTest` |
| Fail-safe si el servicio de países falla | `CountryCheckUnavailableException` | `AcceptCandidateServiceTest#failSafeWhenCountryCheckUnavailable`, `CountryCheckAdapterTest` |
| Búsqueda por importe con mínimo 250 | `SearchForm` | `SearchForm.test.tsx` |
| Tabla de resultados con las 6 columnas | `ResultsTable`, `utils/formatters` | `formatters.test.ts` |
| Orden por defecto score descendente | `useTableSort` | `useTableSort.test.ts`, `Dashboard.test.tsx` |
| Estados de carga/error/vacío (dos causas de vacío distinguidas) | `LoadingIndicator`, `ErrorMessage`, `EmptyState` | `Dashboard.test.tsx` (un test por causa) |
| Filtro de cliente por nombre/DUNS/país/rating | `useClientFilters` | `useClientFilters.test.ts`, `Dashboard.test.tsx` |
| Ordenación por columna al hacer clic | `useTableSort` | `Dashboard.test.tsx#togglesSortDirectionWhenAColumnHeaderIsClicked` |
| Paginación `limit`/`offset` con recuento | `Pagination`, `usePotentialSuppliers` | `Dashboard.test.tsx#supportsPaginatingWithLimitOffsetAndShowsTheTotalCount` |
| `docker compose up` levanta la solución completa | `docker-compose.yml`, healthchecks | Verificación manual, ver [Cómo ejecutar la solución](#cómo-ejecutar-la-solución) |
| Arquitectura hexagonal / DDD | Estructura de paquetes + ArchUnit | `HexagonalArchitectureTest` |
| Compatibilidad de los 7 endpoints con el contrato OpenAPI | `OpenApiInteractionValidator` (swagger-request-validator) | `OpenApiContractTest` (36 tests) |
| Rollback de una transición ante conflicto de optimistic locking, sin caso de uso ficticio | `AcceptCandidateService` (real, `@Transactional`) | `AcceptCandidateServiceConcurrencyIntegrationTest` |
| Tests del servicio de países deterministas (sin timeout compartido inestable) | 3 contextos Spring separados | `CountryCheckAdapterTest`, `CountryCheckAdapterTimeoutTest`, `CountryCheckAdapterCircuitBreakerRecoveryTest` |

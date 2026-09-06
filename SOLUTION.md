# SOLUTION.md

Status: **Feature-complete end to end, including a full concurrency/robustness review pass
(iteration 16).** Backend: all 7 OpenAPI endpoints implemented and verified end to end, 100% of
the backend test suite green (**86/86**, zero `@Disabled` stubs, including 4 ArchUnit rules
enforcing the hexagonal layering as a build-time check), and the `potential-suppliers` query
benchmarked with `EXPLAIN` against 300k seeded rows (index usage confirmed for the status filter;
a documented, honest performance trade-off found for the country-based bonus ranking — see §4 and
"Progress log" iteration 13). Every endpoint works for real against Postgres (and, for `accept`,
the WireMock country service through a genuinely wired resilience4j Circuit Breaker with real,
effective HTTP timeouts), through every layer. Reapply-after-refusal, optimistic locking, the
concurrent-duplicate-DUNS race, and stable score-tied pagination are all implemented and covered
by real-PostgreSQL integration tests (see decisions 1a, 6, and §4's tie-break note). Frontend: the
potential-suppliers dashboard implements every requirement in the README's frontend table —
amount search with minimum-250 validation, sortable/filterable results table, client-side
name/DUNS/country/rating filtering, limit/offset pagination with a total-vs-visible count
distinction, distinct loading/error/empty states, and out-of-order-request-safe state updates via
`AbortController` — verified against the real backend through the full `docker compose up` stack,
not just unit tests (**29/29 frontend tests green**). **Remaining work: none identified; see
"Aspectos dejados fuera" for scope intentionally left out.**

## Progress log

- **Iteration 1** — hexagonal skeleton: packages, ports, DTOs, controller/entity/mapper stubs,
  test stubs, Docker Compose wiring. Nothing executable yet.
- **Iteration 2** (commit `e214277`) — implemented the core business logic in
  `domain.model.SupplierRecord`: `apply`, `reconstitute`, `accept`, `refuse`, `ban`, `restrict`,
  `promote`, `isVisibleAsCandidate`, `isVisibleAsSupplier`. Also implemented the two value-object
  helper methods `accept()` directly depends on —
  `AnnualTurnover#meetsMinimumForAcceptance`/`#isEligibleFor` and
  `SustainabilityRating#qualifiesForActive` — since leaving them as stubs would have made
  `accept()` throw regardless of its own logic. VO input-range validation (`Duns`, `CountryCode`,
  compact constructors) and `SupplierStatus#isTerminal` are still `TODO`, deliberately left for a
  later slice.
  - `domain.model.SupplierRecordTest` un-disabled and implemented: 13 tests, all green (state
    transitions, every guard, visibility filters, and a dedicated test proving `REFUSED` has no
    outgoing transition from any of the 5 mutating operations — see decision 1a below).
  - **Verified in Docker**, not just by reading the code:
    - `mvn test` run inside a `maven:3.9-eclipse-temurin-21` container against `backend/`:
      `Tests run: 74, Failures: 0, Errors: 0, Skipped: 61` — the 13 `SupplierRecordTest` cases are
      the only ones actually executing; the other 61 remain `@Disabled` stubs as expected.
    - `docker compose up --build db backend` (first attempt): the image builds and Spring Boot
      starts, but the container exits with
      `SchemaManagementException: Schema-validation: missing table [supplier_record]` — confirming
      the (at-the-time) commented-out Flyway migration was a real gap, not just a note in this
      file.
- **Iteration 3** (commit `7558c5c`) — activated `V1__create_supplier_record_table.sql` (uncommented,
  fully executable). Two column-type fixes were needed to actually pass Hibernate's
  `ddl-auto: validate` against `SupplierRecordEntity`, found only by running the real container,
  not by reading the code:
  - `country VARCHAR(2)`, not `CHAR(2)` — Hibernate maps a plain `String` field to `VARCHAR`
    regardless of `@Column(length=...)`.
  - `sustainability_rating CHAR(1)`, not `VARCHAR(1)` — the opposite: an
    `@Enumerated(EnumType.STRING)` field with `@Column(length = 1)` validates against `CHAR(1)` in
    Hibernate 6. Both are documented as a comment directly in the migration file so nobody
    "fixes" one back without re-verifying in Docker.
  - **Verified in Docker**: `docker compose down -v` (dropping the stale `db-data` volume from
    the iteration-2 check, which still had the empty migration's checksum recorded) →
    `docker compose up --build db backend` → `Started SupplierManagementApplication in 22.896
    seconds`, `curl http://localhost:8080/actuator/health` → `200`. Re-ran `mvn test` in the
    Maven container afterwards to confirm nothing else broke: `Tests run: 74, Failures: 0,
    Errors: 0, Skipped: 61` (same as iteration 2 — no test touches the database yet).
- **Iteration 4** (commit `ffdb9c8`) — finished the `domain.model` layer: input-range validation in
  the `Duns`, `CountryCode` and `AnnualTurnover` compact constructors, and
  `SupplierStatus#isTerminal`. `CountryCode` is a deliberate design decision: strict validation,
  no lowercase→uppercase normalization (documented in its javadoc) — a code like `"es"` is
  rejected rather than silently accepted.
  - Un-disabled and implemented `DunsTest`, `CountryCodeTest`, `AnnualTurnoverTest`,
    `SustainabilityRatingTest`, `SupplierStatusTest` (10 tests). `domain.model` is now 100%
    implemented and 100% tested — 23/23 green.
  - **Verified in Docker**: `mvn test` in the Maven container → `Tests run: 74, Failures: 0,
    Errors: 0, Skipped: 51` (down from 61 — exactly the 10 newly-enabled domain tests).
    `docker compose up --build db backend` → `Started SupplierManagementApplication in 37.719
    seconds` → `/actuator/health` → `200`, confirming the new constructor validation doesn't
    break the boot path (nothing yet calls `SupplierRecord.reconstitute` with real DB rows, so
    this was a compile/wiring check more than a behavioral one).
- **Iteration 5** (commit `c92204d`) — closed the first full use case end to end:
  `SupplierPersistenceMapper` (domain ↔ JPA entity, both directions), `SupplierPersistenceAdapter`
  (`findByDuns`/`save` with upsert-by-DUNS semantics — `save` looks up by DUNS first and either
  updates the managed entity in place or inserts a new one), `RegisterCandidateService` (the
  existence/BANNED/already-exists guard, then `SupplierRecord.apply` + save),
  `CandidateWebMapper`, and the `POST /candidates` controller method (`@ResponseStatus(CREATED)`).
  Also added a public all-args constructor to `SupplierRecordEntity` (its no-arg constructor is
  `protected` for JPA, so the mapper — in a different package — needed one to actually build a new
  entity; this was already flagged as a TODO on the entity from iteration 1) and a
  `GlobalExceptionHandler` handler for `CandidateAlreadyExistsException`, `SupplierBannedException`,
  Bean Validation failures, and a new `IllegalArgumentException` handler (400) — needed because the
  DTO's `@Size(min=2,max=2)` on `country` doesn't enforce uppercase, so a value like `"es"` passes
  Bean Validation but fails `CountryCode`'s domain invariant; without this handler it would have
  been an uncaught 500 instead of a 400.
  - **Verified against real Postgres**, not mocks: rebuilt and started `db`+`backend`, then via
    `curl`: a valid `POST /candidates` → `201` with the `Candidate` JSON body (no `status` field,
    correctly); the same DUNS again → `409 {"info":"Candidate already exists"}`; a lowercase
    country (`"es"`) → `400 {"info":"isoCode must be exactly 2 uppercase letters, got es"}`; a
    missing `name` → `400 {"info":"name must not be blank"}`. Then queried
    `supplier_record` directly via `psql` inside the `db` container and confirmed the row exists
    with `status = CANDIDATE` and `sustainability_rating` empty (not yet accepted) — the round
    trip is real, not just an in-memory illusion.
  - `mvn test` in the Maven container still green after the compile fix (protected constructor):
    `Tests run: 74, Failures: 0, Errors: 0, Skipped: 51` (unchanged — no test stub was un-disabled
    this iteration; the verification was manual end-to-end against Docker instead).
- **Iteration 6** (commit `7714183`) — implemented the read paths: `GetCandidateService`/
  `GetSupplierService` (`findByDuns` + the visibility filter already on `SupplierRecord`),
  `SupplierWebMapper#toResponseDto`/`#toStatusDto` (the internal→external status mapping —
  `CANDIDATE`/`REFUSED` throw `IllegalStateException` if they ever reach it, since upstream
  filtering should make that impossible), `GET /candidates/{duns}` and `GET /suppliers/{duns}`
  on both controllers, and the `SupplierRecordNotFoundException` → 404 handler in
  `GlobalExceptionHandler`. `SupplierWebMapper#toPotentialResponseDto` stays a stub — out of
  scope until `GetPotentialSuppliersService`/the SQL query are implemented.
  - **Verified against real Postgres**: `GET /candidates/123456789` (the row from iteration 5,
    still `CANDIDATE`) → `200`; `GET /suppliers/123456789` on that same row → `404` (correctly
    not yet visible as a supplier); `GET /candidates/999999999` (never existed) → `404`. Then,
    since `AcceptCandidateService`/`BanSupplierService` aren't implemented yet, used `psql`
    directly against the `db` container to flip the row's status and confirmed the
    internal→external mapping empirically, not just by reading the code:
    `UPDATE ... SET status='ACTIVE'` → `GET /suppliers/{duns}` → `"status":"Active"`;
    `UPDATE ... SET status='BANNED'` → `"status":"Disqualified"`;
    `UPDATE ... SET status='ON_PROBATION'` → `"status":"Active"` (the whole reason this
    internal/external split exists — README §"Integrity Rules": "The API does not distinguish
    between Active and On Probation").
  - `mvn test` in the Maven container: `Tests run: 74, Failures: 0, Errors: 0, Skipped: 51`
    (unchanged — verification was manual against Docker again).
- **Iteration 7** (commit `c240748`) — implemented `refuse`/`ban`: `RefuseCandidateService`
  (find-or-404 + `SupplierRecord#refuse` + save) and `BanSupplierService` (same shape with
  `#ban`), the `POST /candidates/{duns}/refuse` and `POST /suppliers/{duns}/ban` controller
  methods (both now explicitly `@ResponseStatus(NO_CONTENT)` — a void controller method defaults
  to 200, not 204, without it), and the two remaining conflict handlers in
  `GlobalExceptionHandler` (`CandidateNotRefusableException`, `SupplierNotBannableException`).
  - **Verified against real Postgres**, covering every branch, including the two decisions most
    likely to be miscoded:
    - `refuse`: `CANDIDATE` → `204`; `GET` right after → still `200` (REFUSED stays visible as a
      candidate); refusing the same DUNS again → `409 "Candidate can not be refused"` — at this
      point in the build, `reapply()` did not exist yet (added in iteration 16, see "Design
      decisions" §1a), so this was also, at the time, the only way back into `CANDIDATE`.
    - `ban`: from `ON_PROBATION` → `204`, then `GET /suppliers/{duns}` → `"status":"Disqualified"`;
      banning again → `409`. **Critically, from `ACTIVE`** (set via `psql`, since `accept()` isn't
      wired to a controller yet) → `409 "Supplier can not be banned"`, and a follow-up `GET`
      confirms the record is untouched (still `"status":"Active"`) — confirms the confirmed
      project decision that `ban()` is `ON_PROBATION`-only, not `ACTIVE`-or-`ON_PROBATION`.
    - `POST /suppliers/{duns}/ban` on a DUNS that never existed → `404`.
  - `mvn test` in the Maven container: `Tests run: 74, Failures: 0, Errors: 0, Skipped: 51`
    (unchanged — verification was manual against Docker again).
- **Iteration 8** (commit `cc6b491`) — implemented `accept`, the last piece requiring the external
  country service: `CountryClient` (a `RestClient` calling `GET /countries/{country}`, base URL
  from `country-service.base-url`), `CountryCheckAdapter` (wraps the client in
  `@CircuitBreaker(name = "countryService")`; its fallback method always throws
  `CountryCheckUnavailableException`, never returns a boolean, so the fail-safe rule can't be
  silently bypassed by a future edit), `AcceptCandidateService` (find-or-404, resolve the country
  check catching `CountryCheckUnavailableException` and treating it as banned, delegate to
  `SupplierRecord#accept`, save), the controller wiring, and the last two
  `GlobalExceptionHandler` entries (`CandidateNotAcceptableException`,
  `CountryCheckUnavailableException` — the latter is defense-in-depth, since the service already
  absorbs it internally and it shouldn't normally reach the web layer).
  - **Verified against all three real services together** (backend + Postgres + the WireMock
    country-service), covering every branch:
    - Country not banned (`ES`, first letter A-M per the WireMock mappings) + rating `A` → `204`,
      `GET /suppliers/{duns}` → `"status":"Active"`.
    - Country banned (`PT`, first letter N-Z) → `409 "Candidate can not be accepted"`.
    - Rating `D` (same non-banned country) → `204`; confirmed via `psql` the internal status is
      really `ON_PROBATION`, while `GET /suppliers/{duns}` still reports `"status":"Active"` —
      the internal/external split working end to end through a real `accept()` call, not just a
      manually-flipped row this time.
    - Turnover `< 1,000,000` with an approved country → `409` (guard order confirmed: country ok
      alone isn't enough).
    - Accepting an already-`ACTIVE` candidate again → `409`; accepting a DUNS that never
      existed → `404`.
    - **The fail-safe path, the single most important test in this slice**: registered a
      candidate, then `docker compose stop country-service` to simulate a real outage, then
      called `accept` → `409 "Candidate can not be accepted"`, **not a 500** — and confirmed via
      `GET /candidates/{duns}` that the record was left untouched in `CANDIDATE` (the guard fires
      before any mutation). Restarted `country-service` afterwards and confirmed
      `GET /actuator/circuitbreakers` shows the `countryService` instance genuinely tracking real
      traffic (`bufferedCalls`/`failedCalls` reflecting the calls just made, `state: CLOSED`,
      since one failure among six calls doesn't cross the 50% threshold) — proof the Circuit
      Breaker annotation is actually wired into the request path, not just present in
      `application.yml`.
  - `mvn test` in the Maven container: `Tests run: 74, Failures: 0, Errors: 0, Skipped: 51`
    (unchanged — verification was manual against Docker again).
  - **All 7 OpenAPI endpoints now implemented except `GET /suppliers/potential`.**
- **Iteration 9** (commit `2fa5b3a`) — implemented `GET /suppliers/potential`, the last endpoint:
  `PotentialSupplierProjection` (a Spring Data interface projection, since `score` isn't a real
  column and can't be mapped onto `SupplierRecordEntity`), the native query in
  `SupplierRecordJpaRepository#findPotentialSuppliersRaw` (a `WITH ranked AS (...)` CTE computing
  `DENSE_RANK() OVER (PARTITION BY country ORDER BY annual_turnover)` for the bonus, then the
  outer query applies the `annual_turnover > :rate` eligibility filter, the score formula, and
  `ORDER BY score DESC LIMIT/OFFSET`) plus its companion `countPotentialSuppliers`,
  `SupplierPersistenceAdapter#findPotentialSuppliers` (maps projection rows to `ScoredSupplier`),
  `GetPotentialSuppliersService`, `SupplierWebMapper#toPotentialResponseDto`, and the controller.
  **Confirmed decision on an ambiguity the README doesn't resolve**: the small-supplier bonus
  ranking is computed over ALL `ACTIVE`/`ON_PROBATION` suppliers in a country — never restricted
  to the current request's `rate`-eligible subset. The bonus is a stable, rate-independent trait
  of "being one of the two smallest suppliers in your country," which best matches the README's
  worked example (5 suppliers in a country, no rate mentioned at all). Documented in the
  repository's javadoc so this isn't silently "fixed" into the other interpretation later.
  `CANDIDATE`/`REFUSED` (no rating yet) and `BANNED` are excluded from the ranking population
  itself, not just from the final eligible result — they aren't "suppliers" in the first place.
  - **Verified against real Postgres — the exact README worked example, not just an analogous
    case**: seeded 5 rows for a country (`DE`) matching the worked example verbatim
    (200k/200k/200k/210k/250k, all rating A) alongside the pre-existing test data from earlier
    iterations. `GET /suppliers/potential?rate=250&limit=10` returned all 9 eligible suppliers
    across 3 countries, sorted by score descending, and the DE scores matched hand-calculated
    values exactly: the three 200k rows and the 210k row all scored `25000`/`26250` (bonus
    applied, `1.25×`), the 250k row scored `25000` with no bonus (`1×`) — precisely "s1, s2, s3,
    s4 receive the bonus" per the README, s5 does not.
    - Pagination verified: `limit=3&offset=0` then `limit=3&offset=3` returned two disjoint
      3-row pages summing correctly against `total: 9`.
    - **The confirmed rate-independence decision, verified empirically**: re-queried with
      `rate=205000` (which excludes the three 200k rows from the eligible result, since
      `200000 > 205000` is false) and confirmed the 210k row's score was still `26250` — the
      bonus survived even though the rows it was ranked against were no longer all present in
      this particular result page, proving the ranking is computed independently of `rate` as
      decided, not recomputed per-query over only the visible subset.
    - Query-param validation (`rate < 250`, `limit > 10`, `offset < 0`) — **found and fixed a real
      bug here**: `@Validated` at the controller class level with `@Min`/`@Max` on
      `@RequestParam`s throws `jakarta.validation.ConstraintViolationException` via
      `MethodValidationInterceptor`, not `MethodArgumentNotValidException` — this had no handler
      and was silently falling through to Spring Boot's default 500 error page instead of the
      OpenAPI-documented 400. Added a `ConstraintViolationException` handler to
      `GlobalExceptionHandler`; re-verified all three invalid-param cases now return 400 with the
      violation message, and the happy path still works.
  - `mvn test` in the Maven container: `Tests run: 74, Failures: 0, Errors: 0, Skipped: 51`
    (unchanged — verification was manual against Docker again).
  - **All 7 OpenAPI endpoints are now implemented.**
- **Iteration 10** (commit `7f037c2`) — un-disabled and implemented 32 of the 51 remaining test stubs:
  all 9 `application.service` test classes (26 tests, pure Mockito — mock
  `SupplierRepositoryPort`/`CountryCheckPort`, no Spring context, no database) and both
  `infrastructure.web.mapper` test classes (6 tests, no mocks needed at all). Also implemented
  `RestrictSupplierService#restrict`/`PromoteSupplierService#promote` (previously
  `UnsupportedOperationException` stubs) since their tests would otherwise have nothing real to
  assert against — same shape as `BanSupplierService`/`RefuseCandidateService`, still not wired to
  any controller (see decision 1b).
  - Notable coverage: `AcceptCandidateServiceTest#failSafeWhenCountryCheckUnavailable` (mocks
    `CountryCheckPort` to throw `CountryCheckUnavailableException`, asserts the service converts
    it to `CandidateNotAcceptableException` and never saves) and
    `BanSupplierServiceTest#throwsWhenStatusIsActiveNotOnProbation` (regression-proofs the
    confirmed "ban only from ON_PROBATION" decision) are now permanent automated tests, not just
    the one-off manual `curl` checks from earlier iterations.
  - `mvn test` in the Maven container: `Tests run: 74, Failures: 0, Errors: 0, Skipped: 19` (down
    from 51 — all 32 newly-enabled tests passed on the first run).
- **Iteration 11** (commit `fc2e38a`) — un-disabled and implemented `CandidateControllerTest`/
  `SupplierControllerTest` (11 tests) via `@WebMvcTest` + `MockMvc`, mocking each use case port
  with `@MockBean` while `@Import`-ing the real `CandidateWebMapper`/`SupplierWebMapper` beans
  (plain deterministic mappers, not use cases — no reason to mock them, and it exercises the
  mapper wiring for free). `@WebMvcTest` auto-includes `@RestControllerAdvice` beans, so
  `GlobalExceptionHandler` is exercised for real too — these tests assert the exact
  `{"info": "..."}` body and status code for each conflict/not-found/validation case, not just
  that *a* 4xx came back.
  - `mvn test` in the Maven container: `Tests run: 74, Failures: 0, Errors: 0, Skipped: 8` (down
    from 19 — all 11 newly-enabled tests passed on the first run). Remaining 8 `@Disabled`:
    `SupplierPersistenceAdapterTest` (4, needs Testcontainers PostgreSQL — the single
    highest-value test still pending, per the checklist) and `CountryCheckAdapterTest` (4, needs
    a WireMock test instance to exercise the Circuit Breaker).
- **Iteration 12** (commit `292912c`) — implemented the last 8 tests, closing out test coverage
  entirely:
  - **`CountryCheckAdapterTest`**: added `org.wiremock:wiremock-standalone` (test-scope) and used
    a real embedded `WireMockServer` (dynamic port, wired in via `@DynamicPropertySource`
    overriding `country-service.base-url`) inside a **deliberately narrow**
    `@SpringBootTest(classes = {CountryClient.class, CountryCheckAdapter.class,
    RestClientConfig.class}) @EnableAutoConfiguration` context — not the full application. A
    plain unit test instantiating `CountryCheckAdapter` directly cannot exercise the
    `@CircuitBreaker` annotation at all (it only takes effect through Spring AOP proxying), so a
    real Spring context is required; scoping it to 3 classes plus
    `spring.autoconfigure.exclude` for JPA/DataSource/Flyway keeps it fast and avoids needing a
    database for a test that has nothing to do with persistence. Since the context (and therefore
    the `CircuitBreaker` singleton) is cached and reused across test methods, `@BeforeEach` resets
    it via `CircuitBreakerRegistry` — otherwise one test's forced failures would leak into the
    next and make results order-dependent.
    - `circuitBreakerOpensAfterRepeatedFailures` drives 5 real failing calls (matching the
      default `minimum-number-of-calls: 5` / `failure-rate-threshold: 50` in `application.yml`)
      to trip the breaker, then resets WireMock's request log and asserts a 6th call is answered
      with `CountryCheckUnavailableException` **and that WireMock received zero requests for
      it** — proof the call was actually short-circuited by resilience4j, not just another failed
      HTTP round-trip.
  - **`SupplierPersistenceAdapterTest`**: `@SpringBootTest` (full application context — simplest
    way to get Flyway/JPA wired exactly as production does) + `@Testcontainers` with a real
    `PostgreSQLContainer`, datasource properties injected via `@DynamicPropertySource`. Covers
    the save/find round trip (including upsert-by-DUNS — saving twice never creates a duplicate
    row), the eligibility filter (`BANNED` and `CANDIDATE` excluded, `annualTurnover <= rate`
    excluded), score ordering with pagination, and — the single highest-value test in the whole
    suite — **the README's exact worked example** (200k/200k/200k/210k/250k) seeded as real rows
    and asserted against hardcoded expected scores (`25000.0`/`26250.0`), not a
    floating-point recomputation in the test itself (Postgres computes the score in exact
    `numeric` arithmetic before the final `::double precision` cast, so recomputing via Java
    `double` multiplication in the assertion could in principle drift by a ULP or two — hardcoding
    the values already verified via `curl` in iteration 9 avoids that risk entirely).
  - **Environment note (not a code issue):** running these two Testcontainers-backed tests via
    `mvn test` inside this session's verification container (`maven:3.9-eclipse-temurin-21`,
    itself a Docker container, talking to the host's Docker daemon through a mounted
    `/var/run/docker.sock`) needed two extra environment variables that a normal local `mvn test`
    or CI runner with direct Docker access would **not** need:
    `TESTCONTAINERS_RYUK_DISABLED=true` (Ryuk, Testcontainers' resource-reaper sidecar, couldn't
    reach back to the calling container over the sibling-container network — cleanup instead
    relied on the JVM shutdown hook, confirmed working: no leftover containers after the run) and
    `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` (without it, Testcontainers computed the
    Postgres container's reachable address as the bridge gateway IP, which the calling container
    couldn't route to). Purely an artifact of nesting Docker-in-Docker for this session's own
    verification method — the test code itself is standard Testcontainers usage and needs no
    special configuration in a normal environment.
  - **`mvn test`: `Tests run: 74, Failures: 0, Errors: 0, Skipped: 0` — the full backend test
    suite is 100% green with zero `@Disabled` stubs remaining.**
- **Iteration 13** (this commit) — closed the two remaining checklist items:
  - **Architecture and design, as a build-time check**: added `com.tngtech.archunit:archunit-junit5`
    and `HexagonalArchitectureTest` (4 rules): `domain` depends on no framework package
    (`org.springframework..`, `jakarta..`, `org.hibernate..`, `io.github.resilience4j..`);
    `domain` depends on neither `application` nor `infrastructure`; `application` depends on
    neither `infrastructure`; and `@Transactional` is used only inside `application.service`.
    These were previously only claims in package-info javadoc — now a failing build enforces
    them. All 4 pass on the current codebase.
  - **`EXPLAIN` against a seeded large dataset**: seeded 300,000 synthetic rows directly via
    `psql` (random country/turnover/status, `sustainability_rating` intentionally *not*
    correlated with `status` — this dataset is for query-plan/volume testing only, it doesn't
    need to satisfy domain invariants) and ran `EXPLAIN (ANALYZE, BUFFERS)` on the exact
    `findPotentialSuppliersRaw` query. Findings, both genuine and both worth raising in the
    interview:
    - `idx_supplier_record_status_turnover` **is** used by the planner (`Bitmap Index Scan`) for
      the `status IN ('ACTIVE','ON_PROBATION')` filter — confirmed working as designed.
    - `idx_supplier_record_country_turnover` is **not** used for the `DENSE_RANK()` window's
      `(country, annual_turnover)` ordering — Postgres instead does an explicit sort after the
      bitmap scan (external merge to disk at default `work_mem`; `SET work_mem = '32MB'` switches
      it to an in-memory quicksort, but total execution time barely changes, ~540ms either way,
      on ~120k eligible rows out of 300k). Tried adding a composite
      `(status, country, annual_turnover)` index — the planner still preferred the existing plan,
      so it was dropped again rather than left as unused dead weight.
    - **Root cause is structural, not an indexing gap**: the confirmed design decision that the
      bonus ranking runs over the *entire* eligible population per country (§4, "Potential
      suppliers score") means Postgres must materialize and rank ~all `ACTIVE`/`ON_PROBATION`
      rows before it can sort by score and apply `LIMIT` — there is no index that lets it take a
      "top 10" shortcut, because the correct answer depends on knowing every row's rank within
      its country first. This is a real, honest performance trade-off of prioritizing the
      confirmed business semantics over a cheaper alternative (e.g. ranking only within the
      rate-filtered subset, which was explicitly rejected in that decision). At the 100k-1M row
      scale this test targets, this query will scan a large fraction of the table on every call;
      a production system at the high end of that range would likely want a precomputed/cached
      per-country rank (refreshed periodically or via triggers) rather than computing it live on
      every request — left as a documented "aspecto dejado fuera" rather than implemented, since
      it's a materially different design (denormalization) beyond this test's scope.
    - Cleaned up afterward: dropped the experimental index, truncated the 300k synthetic rows,
      tore down the containers.
  - `mvn test`: `Tests run: 78, Failures: 0, Errors: 0, Skipped: 0` (74 + 4 new ArchUnit rules).
- **Iteration 14** (commit `8b58526`) — removed the last stale "TODO: implement" javadoc left over
  from the skeleton stage on the 7 `application.service` classes.
- **Iteration 15** (this commit) — implemented the frontend, the only piece left in the whole
  project: the potential-suppliers dashboard against the scaffold left in `frontend/src`
  (types, empty component/hook stubs, and a `SOLUTION.md` checklist item flagging that every
  component returned `null`).
  - **API layer**: `api/client.ts` (`fetch` wrapper resolving `VITE_API_BASE_URL`, parsing the
    `{info}` error schema into a typed `ApiClientError`, distinguishing network failures from
    HTTP error responses) and `api/suppliersApi.ts` (`GET /suppliers/potential`).
  - **Hooks**: `usePotentialSuppliers` (fetch lifecycle — loading/error/data — plus a `hasSearched`
    flag so the dashboard can distinguish "never searched yet" from "searched, zero results"),
    `useClientFilters` (name/DUNS substring + country + rating, applied to the already-fetched
    page — does not re-trigger the backend call), `useTableSort` (defaults to `score`/`desc` per
    the README, toggles asc/desc on repeat clicks, sorts numeric columns numerically rather than
    lexicographically).
  - **Components**: all 7 implemented per their existing prop contracts — `SearchForm` (numeric
    input, custom "must be at least 250" message), `LoadingIndicator`, `ErrorMessage`,
    `EmptyState`, `FiltersBar` (free-text search, multi-select country dropdown, rating
    checkboxes), `ResultsTable` (the 6 README columns, currency/score formatting via
    `utils/formatters`, clickable sortable headers with an asc/desc indicator), `Pagination`
    (limit/offset controls plus the result count). `Dashboard` composes all of them per the
    ordering already sketched in its TODO comment.
  - **Two real bugs found only by testing in a real browser, not by reading the code**:
    - `SearchForm`'s `<input min={250}>` triggers the browser's own native constraint-validation
      tooltip on submit, which silently prevents the `onSubmit` handler (and therefore the custom
      "Amount must be at least 250" message) from ever running — confirmed by driving the actual
      rendered page, where clicking Search with `100` in the field did nothing visible at all.
      Fixed with `noValidate` on the `<form>`, so the app's own validation message is what the
      user always sees, matching the README's "display validation message if not met".
    - The `Dashboard` only showed `EmptyState` when the *server* page was empty; filtering an
      already-nonempty page down to zero rows with the client-side search/country/rating filters
      rendered a bare table (headers, no rows) instead of any message. Found by filtering a real
      populated result down to nothing in the browser. Fixed: `Dashboard` now also renders
      `EmptyState` when the post-filter row count is zero, distinct from the server-empty case
      (the `FiltersBar` itself stays visible in that case, since the user needs it to adjust the
      filter that produced zero rows — unlike the server-empty case, where there is nothing to
      filter yet).
  - **A real CORS gap, not a frontend bug**: there is no Spring Security dependency in the
    backend, so nothing was emitting `Access-Control-Allow-Origin`. `curl` (used for every
    backend verification in this file) doesn't enforce CORS, so this was invisible until the
    dashboard was actually opened in a browser against the real backend, where every
    `GET /suppliers/potential` call failed silently (blocked client-side, no network entry showing
    a server-side rejection). Fixed with
    `backend/.../infrastructure/config/WebCorsConfig.java`, a `WebMvcConfigurer` allowing
    `http://localhost:*` origins for `GET`/`POST` — scoped to `infrastructure.config`, same as the
    existing `RestClientConfig`, so it doesn't violate the `HexagonalArchitectureTest` rules.
    Re-ran the full backend suite afterward: `Tests run: 78, Failures: 0, Errors: 0, Skipped: 0`,
    unchanged.
  - **Two scaffold defects that would have broken `docker compose up` / CI, unrelated to the
    frontend logic itself, fixed alongside it**:
    - `package.json` pinned `eslint-plugin-react-hooks@^4.6.2` against `eslint@^9.11.1` — an
      unsatisfiable peer dependency (`eslint-plugin-react-hooks@4` peers on ESLint 3-8) that makes
      a plain `npm install` fail with `ERESOLVE` (confirmed by running it before touching
      anything). Bumped to `^5.0.0`, which supports ESLint 9. The `frontend/Dockerfile`'s
      `RUN npm install` would have hit this same failure on every build.
    - No `eslint.config.js` existed at all, so `npm run lint` failed outright
      ("ESLint couldn't find an eslint.config.(js|mjs|cjs) file") — ESLint 9 requires the flat
      config format and ships no fallback. Added one (typescript-eslint + react-hooks +
      react-refresh, browser globals via the `globals` package), then fixed the 6 real `no-undef`
      errors it surfaced (`fetch`, `window`, `URL`, `Response`, `document`, `HTMLSelectElement`)
      once browser globals were correctly wired in.
    - Also switched `frontend/Dockerfile` from `npm install` to `npm ci` against the now-committed
      `package-lock.json`, for a reproducible, faster Docker build.
  - **Testing**: added the frontend's first tests — `formatters.test.ts` (currency/score
    formatting, including the es-ES non-breaking-space detail), `useClientFilters.test.ts` and
    `useTableSort.test.ts` (the client-side logic most likely to silently regress), and
    `SearchForm.test.tsx` (the validation behavior above, as a regression test for the
    `noValidate` fix). `npm run test`: 17/17 green. `vitest`/`jsdom`/`@testing-library/react`
    added as dev dependencies for this (none existed in the scaffold).
  - **Verified against the real stack, not mocks**: `docker compose up --build` (all 4 services),
    seeded real data through the actual API (`POST /candidates` + `.../accept`, including the
    turnover-eligibility guard rejecting an acceptance attempt below €1,000,000 — confirming that
    rule is still enforced, not just the frontend's own >250 check), then drove the running
    dashboard in a real browser: amount validation, the loading spinner mid-request, the
    populated results table sorted score-descending by default, column-header sort toggling
    (verified alphabetical *and* numeric columns sort correctly, not lexicographically), the
    country + rating filters combining correctly, the client-filtered-to-zero empty state, the
    server-side-zero empty state (a rate high enough that no supplier qualifies), and
    limit/offset pagination across a real second page (13 seeded suppliers, `limit=10` from the
    OpenAPI's `QueryLimit.maximum`, confirmed "Page 2 of 2" with `Next` correctly disabled).
    `npm run build`/`npm run lint`/`npm run test` all clean throughout. Cleaned up afterward:
    `docker compose down` + removed the seeded `db-data` volume, so the delivered environment
    starts empty.
- **Iteration 16** (this commit) — a full review pass closing every gap raised against the
  delivered solution: reapply-after-refusal, optimistic locking, the concurrent-DUNS race, stable
  pagination, frontend request-race cancellation, filter/counter coherence, real country-service
  timeouts, and Docker/accessibility cleanup. See "Design decisions" §§1a, 6, 7, 8, 9 above for the
  full rationale behind each change; summarized here:
  - **Reapply after refusal** (§1a): `SupplierRecord#reapply`, `RegisterCandidateService` branches
    on `REFUSED` before falling through to `CandidateAlreadyExistsException`. New tests:
    `SupplierRecordTest#reapplyResetsToCandidateWithNewDataAndClearsRating`/
    `#reapplyFailsWhenNotRefused`, `SupplierStatusTest#onlyBannedIsTerminal`,
    `RegisterCandidateServiceTest#reappliesWhenExistingRecordIsRefused`. Removed every claim in
    this file that the FSM diagram overrides the written README requirement.
  - **Optimistic locking + concurrent-DUNS race** (§6): `SupplierRecordEntity#version` (`@Version`,
    `V2__add_supplier_record_version.sql`), `SupplierPersistenceAdapter#save` now uses
    `saveAndFlush` and translates a `uk_supplier_record_duns` constraint violation into
    `SupplierBannedException`/`CandidateAlreadyExistsException`; `GlobalExceptionHandler` maps
    `ObjectOptimisticLockingFailureException` to `409`. New `ConcurrencyIntegrationTest` (2 tests,
    real Testcontainers PostgreSQL). **A real bug was found here only by firing two genuinely
    concurrent `POST /candidates` at the actual running `docker compose up` stack**: the
    constraint-violation re-check was reading through the same (by then poisoned) persistence
    context as the failed flush, turning every racing insert into an unmapped `500` instead of the
    intended `409` whenever it ran through `RegisterCandidateService`'s real `@Transactional`
    method — a failure mode the adapter-level test alone could not catch, since it calls `save()`
    standalone. Fixed with a `PROPAGATION_REQUIRES_NEW` `TransactionTemplate` for the re-check; new
    permanent regression test `RegisterCandidateServiceConcurrencyIntegrationTest` (goes through
    the real service, not the adapter); re-verified against the rebuilt Docker stack (5 repeated
    real concurrent races, `201`/`409` every time, never a `500`). See "Design decisions" §6.
  - **Stable pagination**: `findPotentialSuppliersRaw`'s `ORDER BY` gained `, duns ASC`. New
    `SupplierPersistenceAdapterTest#findPotentialSuppliersBreaksScoreTiesByDunsAscendingForStablePagination`.
  - **Real country-service timeouts** (§7): `RestClientConfig` now configures
    `SimpleClientHttpRequestFactory` connect/read timeouts from `country-service.connect-timeout-ms`/
    `read-timeout-ms` (env-overridable); the inert `resilience4j.timelimiter` block was deleted.
    New `CountryCheckAdapterTest#respondsWithinBoundedTimeAndFailsSafeOnSlowCountryService`
    (WireMock 5s fixed delay, 300ms test timeout, asserts completion well under 2s).
  - **Frontend request races and filter coherence** (§8): `usePotentialSuppliers` gained
    `AbortController`-based cancellation of superseded requests plus an unmount guard;
    `useClientFilters` gained `reset()`/`hasActiveFilters`, called from `Dashboard#handleSearch` on
    every new amount; `Pagination` now shows `"{visible} visible suppliers out of {total} total"`
    while filters are active. New `Dashboard.test.tsx` (10 tests): loading, success, API error,
    empty state, pagination, client-side filtering + visible/total count, default score-descending
    sort, sort-toggle on header click, out-of-order request resolution, and filter reset on a new
    search.
  - **Accessibility + Docker** (§9): `aria-sort` moved from the sort `<button>` onto its `<th>`
    (per the WAI-ARIA table-sort pattern), plus a visually-hidden "sorted ascending/descending"
    announcement for screen readers. `docker-compose.yml`: `wiremock/wiremock:latest` pinned to
    `3.9.1`, real `curl`-based healthchecks added for `backend`/`country-service`, `depends_on`
    upgraded to `condition: service_healthy` for both `backend→country-service` and
    `frontend→backend`. Both `.dockerignore` files extended. Two stale `TODO` comments removed
    from `application.yml`.
  - **Verification**: `mvn test` → **86/86 green**, zero `@Disabled`, zero regressions (up from
    78 — iteration 13's count — with 8 new tests: 2 reapply, 1 terminal-status update, 2
    adapter-level concurrency-integration, 1 service-level concurrency-integration regression test
    for the bug above, 1 stable-pagination, 1 timeout). `npm run test` → **29/29 green** (up
    from 17 at the end of iteration 15: +2 for `useClientFilters`' reset/hasActiveFilters, +10 for
    the new `Dashboard.test.tsx`).
    `npm run lint` and `npm run build` both clean. `mvn package` → `BUILD SUCCESS`. Full
    `docker compose config` → validates. `docker compose up --build` → all four containers up,
    `backend`/`country-service` report `(healthy)`. Smoke-tested all 7 OpenAPI endpoints via
    `curl` against the live stack, including the exact reapply flow (`refuse` → `POST /candidates`
    again → `201` with the new data → `GET` confirms `CANDIDATE`) and the ban-blocks-reapply rule
    (`POST /candidates` on a `BANNED` duns → `409 {"info":"Supplier banned"}`). Drove the running
    dashboard in a real browser against the live backend: search, the default score-descending
    sort with `aria-sort="descending"` confirmed on the `<th>` (not the button) via the live DOM,
    the free-text filter producing both the `EmptyState` and the distinct
    `"0 visible suppliers out of 1 total"` counter together, and the filter correctly clearing on
    a brand-new search. `docker compose down` afterward, **no volumes removed**.

## How to start

```bash
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Country service (WireMock, provided): http://localhost:8088
- Postgres: localhost:5432 (user/pass/db: `supplier`)

`docker compose up --build` boots all four services end to end (Flyway migration → Hibernate
schema validation → Tomcat for the backend; `db`/`country-service` gate the backend via
`depends_on: condition: service_healthy`, and the backend in turn gates the frontend the same way
— see "Design decisions" §9). All 7 OpenAPI endpoints are live once the backend reports healthy.

Optional environment overrides for the backend (sensible defaults apply if unset — see
`docker-compose.yml` and `application.yml`):

| Variable | Default | Purpose |
|---|---|---|
| `COUNTRY_SERVICE_CONNECT_TIMEOUT_MS` | `1000` | Country-service HTTP connect timeout |
| `COUNTRY_SERVICE_READ_TIMEOUT_MS` | `2000` | Country-service HTTP read/response timeout |

## Architecture

Hexagonal / ports-and-adapters with a single aggregate, `SupplierRecord` (identity = DUNS),
instead of separate `Candidate`/`Supplier` entities:

```
domain/            plain Java, no framework imports — SupplierRecord aggregate, value objects,
                   the 5-state SupplierStatus enum, 8 business exceptions
application/       port/in (one interface per use case), port/out (repository + country check),
                   service (use case implementations, @Transactional here only)
infrastructure/    web (controllers/DTOs/mappers/exception handler), persistence (JPA entity,
                   repository, adapter, mapper — entity never reused as domain object), external
                   country (HTTP client + Circuit Breaker adapter), config
```

A single `UNIQUE(duns)` constraint (see the commented schema) makes these three integrity rules
(README §"Integrity Rules") automatic instead of enforced by cross-entity checks:
- only one active candidacy per DUNS,
- only one supplier per DUNS,
- an active candidate and a supplier can never coexist for the same DUNS.

## Design decisions

### 1. FSM diagram vs. the written spec

Before writing any code, the FSM diagram (`wiki/iop-techtest-fsm-supplier.png`) was compared
against the README text and the task's own state-machine description.

**a) Reapply after refusal — implemented per the written requirement.**
The README's prose is explicit: "a refused candidacy allows the candidate to reapply." This is
implemented as `SupplierRecord#reapply(name, country, annualTurnover)`: `POST /candidates` for a
DUNS currently in `REFUSED` status updates `name`/`country`/`annualTurnover` with the newly
submitted values, discards any previous `sustainabilityRating` (a reapplication is a fresh
candidacy, not a resumption of the old one), and moves the record back to `CANDIDATE`. Only
`BANNED` is terminal now (`SupplierStatus#isTerminal`) — `REFUSED` has exactly one outgoing
transition (`reapply`), every other mutating operation (`accept`/`refuse`/`ban`/`restrict`/
`promote`) still rejects it. The transition is fully encapsulated in the aggregate: the
application service (`RegisterCandidateService`) never mutates `SupplierRecord` fields directly,
it only decides *which* aggregate method to call based on the existing record's status. See
`domain.model.SupplierStatus`, `SupplierRecord#reapply` javadoc, and
`RegisterCandidateServiceTest#reappliesWhenExistingRecordIsRefused`.

`POST /candidates` full behavior for an existing DUNS:

| Existing status | Behavior | HTTP |
|---|---|---|
| *(no record)* | `SupplierRecord.apply` — new candidacy | 201 |
| `REFUSED` | `SupplierRecord.reapply` — fields updated, rating cleared, back to `CANDIDATE` | 201 |
| `BANNED` | `SupplierBannedException` | 409 `{"info":"Supplier banned"}` |
| any other (`CANDIDATE`, `ACTIVE`, `ON_PROBATION`) | `CandidateAlreadyExistsException` | 409 `{"info":"Candidate already exists"}` |

**b) `Restrict`/`Promote` extension stubs, not wired to any endpoint.**
The diagram also draws `Active --Restrict--> On Probation` and `On Probation --Promote--> Active`
— transitions absent from the README business text and from the OpenAPI contract entirely (no
`/suppliers/{duns}/restrict` or `/promote` path exists). **Decision: add the domain method stubs,
port/in use cases, application services, and dedicated exceptions
(`SupplierNotRestrictableException`, `SupplierNotPromotableException`) now, but do not add a
controller endpoint for them** — inventing an unspecified public endpoint would be scope creep
against a fixed contract. If the contract is ever extended with these operations, only
`infrastructure.web` needs new code; `domain`/`application` are already shaped for it.

### 2. `ban()` only valid from `ON_PROBATION`, never from `ACTIVE`

Confirmed per the task's own instructions. `SupplierNotBannableException` is thrown for any
source status other than `ON_PROBATION`, including `ACTIVE` — this is the one state transition
most likely to be miscoded as "ban from either Active or On Probation," so
`BanSupplierServiceTest`/`SupplierRecordTest` both carry an explicit regression test placeholder
for the ACTIVE case.

### 3. API pública vs. estado interno (`status` mapping)

The OpenAPI `Supplier.status` schema only has `[Active, Disqualified]` — it cannot represent
`ON_PROBATION`. Mapping (confined to `infrastructure.web.mapper.SupplierWebMapper`, never the
domain):

| Internal status | Exposed `status` |
|---|---|
| `ACTIVE`, `ON_PROBATION` | `Active` |
| `BANNED` | `Disqualified` |

Resource visibility (also confined to the web/application boundary, via
`SupplierRecord#isVisibleAsCandidate` / `#isVisibleAsSupplier`, checked in
`GetCandidateService`/`GetSupplierService`):

| Endpoint | 200 when internal status is | 404 otherwise |
|---|---|---|
| `GET /candidates/{duns}` | `CANDIDATE`, `REFUSED` | any other status, or no record |
| `GET /suppliers/{duns}` | `ACTIVE`, `ON_PROBATION`, `BANNED` | any other status, or no record |

This is not 100% explicit in the OpenAPI spec — it follows from the fact that "candidate" and
"supplier" are two views over one aggregate, and a record must be visible through exactly one of
the two resources at any time (or neither, mid-transition, which cannot actually happen since
transitions are synchronous).

### 4. Potential suppliers score — must run in SQL

```
score = annual_turnover × 0.1 × rating_constant × bonus
bonus = 1.25 if turnover is among the 2 lowest UNIQUE turnovers in its country, else 1
```

At the stated 100k–1,000,000 supplier volume (README §6), this cannot be computed by loading rows
into the JVM. **Implemented** (iteration 9) as a single native query in
`SupplierRecordJpaRepository#findPotentialSuppliersRaw`: a `WITH ranked AS (...)` CTE computes
`DENSE_RANK() OVER (PARTITION BY country ORDER BY annual_turnover)` over the full
`ACTIVE`/`ON_PROBATION` population per country, then the outer query filters
`annual_turnover > :rate`, computes the score, and applies
`ORDER BY score DESC, duns ASC LIMIT/OFFSET`. Results are read via `PotentialSupplierProjection`
(a Spring Data interface projection — `score` isn't a real column, so it can't be mapped onto
`SupplierRecordEntity`). See the proposed indexes in `V1__create_supplier_record_table.sql`
(`(country, annual_turnover)` for the bonus window function, `(status, annual_turnover)` for the
filter/order).

**Stable pagination (`duns` as tie-breaker):** `score` alone is not unique — several suppliers can
land on the exact same value (identical turnover, rating, and bonus eligibility). `ORDER BY score
DESC` alone gives Postgres no guarantee about the relative order of tied rows across separate
`LIMIT`/`OFFSET` calls, which can duplicate a row on one page and silently drop another across
consecutive pages. Breaking ties by `duns ASC` (already `UNIQUE`) makes the ordering total, so
paging through tied rows is deterministic and lossless. Verified in
`SupplierPersistenceAdapterTest#findPotentialSuppliersBreaksScoreTiesByDunsAscendingForStablePagination`
with 4 suppliers sharing an identical score: two consecutive 2-row pages cover all 4 DUNS exactly
once, in ascending order.

**Benchmarked with `EXPLAIN (ANALYZE, BUFFERS)` against 300,000 seeded rows** (iteration 13): the
`status` index is used by the planner as designed; the `country` index is not — the window
function still needs an explicit sort of the ~120k-row eligible set. This is not a missing index,
it is the direct cost of the confirmed rate-independence decision below: since the bonus depends
on ranking the *entire* per-country population, Postgres cannot use any index to shortcut straight
to a "top 10" answer — it must materialize and rank the full eligible set on every call
(~540ms at this scale; wall time didn't meaningfully improve either with a composite
`(status, country, annual_turnover)` index or with `work_mem` raised enough to keep the sort in
memory instead of spilling to disk). At the upper end of the stated 100k-1M row range, the honest
answer is that this query would benefit from a precomputed/cached per-country rank (refreshed
periodically, or maintained via a trigger on write) rather than computing `DENSE_RANK()` live on
every request — that denormalization is a materially different design and is left out of scope
for this test, not attempted as a partial fix.

**Confirmed decision on an ambiguity the README leaves open**: "the two lowest unique annual
turnovers in their country" doesn't say whether that ranking is computed over every
non-disqualified supplier of the country, or only over the subset that happens to be eligible for
the specific `rate` being queried. **Decided: the former** — the bonus ranking runs over ALL
`ACTIVE`/`ON_PROBATION` suppliers of a country regardless of `rate`, making it a stable trait of
the supplier ("one of the two smallest in your country") rather than something that flickers on
and off depending on what a caller happens to query. The README's worked example (5 suppliers in
a country, no `rate` mentioned at all) reads far more naturally under this interpretation.
Verified empirically: querying with a `rate` that excludes some of the ranked rows from the
result still preserves the correct bonus on the rows that remain — see "Progress log", iteration
9. `CANDIDATE`/`REFUSED` are excluded from the ranking population itself (not just the final
result), since they aren't suppliers and have no rating to score at all.

### 5. Known gap — 422 on `POST /candidates`

The OpenAPI declares a `422 Unprocessable Content` response on `POST /candidates`, but nothing in
the README's business rules triggers it distinctly from `400` (invalid request schema — handled
by Bean Validation) or `409` (duplicate/banned — handled by the two domain exceptions).
**Left unimplemented.** `GlobalExceptionHandler` documents this gap in its class javadoc rather
than fabricating a business rule to justify a 422 response. If a distinction becomes clear later
(e.g. semantically valid-but-nonsensical input, like a country code that doesn't exist per the
country service, as opposed to a country that is merely banned), this is the natural place to add
it.

### 6. Optimistic locking and concurrency

Two independent races were closed, both surfaced as `application/json` `409`s rather than a raw
500 or (worse) a silent lost update:

**a) Concurrent modification of the same row.** `SupplierRecordEntity` gained a `@Version` column
(`version`, `V2__add_supplier_record_version.sql` — purely additive, does not touch `V1`). Two
overlapping transactions that both read the same row before either commits will have the second
one's flush fail with `org.springframework.orm.ObjectOptimisticLockingFailureException` instead of
silently overwriting the first transaction's change. `SupplierPersistenceAdapter#save` was changed
from `save()` to `saveAndFlush()` so the conflict surfaces synchronously inside the call, not
whenever the enclosing `@Transactional` service method happens to commit. `GlobalExceptionHandler`
maps it to `409 {"info":"Supplier record was modified concurrently, please retry"}` — no stack
trace, no internal detail. Applies uniformly to `accept`, `refuse`, `ban`, and `reapply`, since all
four go through the same `findByDuns` + mutate + `save` shape in `application.service`.

Verified in `ConcurrencyIntegrationTest#concurrentUpdatesToTheSameRowDoNotSilentlyOverwriteEachOther`
against real PostgreSQL: transaction B opens and reads a row (version 0), an independent
`PROPAGATION_REQUIRES_NEW` transaction A reads the same row, refuses it, and commits (version 1);
transaction B then tries to persist its own (individually valid) mutation built from its
now-stale version-0 snapshot — this throws `ObjectOptimisticLockingFailureException`, and the
final row is confirmed still `REFUSED` (A's change), never silently overwritten by B.

**b) Concurrent insert for the same DUNS.** The `findByDuns`-then-`save` sequence in
`RegisterCandidateService` has an inherent TOCTOU race: two requests can both observe "no record
yet" and both attempt to insert. The `uk_supplier_record_duns` unique constraint remains the last
line of defense, but a raw constraint violation must never reach the client as an unmapped 500.
`SupplierPersistenceAdapter#save` catches `DataIntegrityViolationException`, inspects the cause
chain for `org.hibernate.exception.ConstraintViolationException` with exactly this constraint name
(deliberately narrow — any other integrity violation is rethrown as-is, never swallowed into a
misleading "candidate already exists"), re-reads whichever row won the race, and throws
`SupplierBannedException` if that row is `BANNED` or `CandidateAlreadyExistsException` otherwise —
the same two exceptions `GlobalExceptionHandler` already maps for the non-racing case.

Verified in
`ConcurrencyIntegrationTest#concurrentInsertsForTheSameDunsAreSerializedByTheUniqueConstraint`:
two real threads, synchronized with a `CyclicBarrier`, both call `save()` for a brand-new,
identical DUNS. Exactly one succeeds, the other receives `CandidateAlreadyExistsException`, and
exactly one row is ever persisted.

**A real bug found only by testing against the live Docker stack, not by the test above alone**:
the first version of `resolveConstraintViolation` re-read the "who won" row through the *same*
ambient transaction as the failed `saveAndFlush` — fine when `save()` is called standalone (as
`ConcurrencyIntegrationTest` does, where each repository call gets its own independent
mini-transaction), but wrong when `save()` runs inside `RegisterCandidateService`'s single
`@Transactional` method, exactly as the real `POST /candidates` controller does. A failed flush
leaves Hibernate's persistence context poisoned; any further operation on the same session
(including a plain read) re-triggers the identical auto-flush failure. Firing two real concurrent
`POST /candidates` requests at the actual `docker compose up` stack for the same new DUNS
reproduced this exactly: one request got `201`, the other an **unmapped `500`** — the opposite of
this whole requirement. Fixed by re-reading the winning row through a brand-new, independent
transaction (`TransactionTemplate` with `PROPAGATION_REQUIRES_NEW`, programmatic rather than
`@Transactional` so it doesn't violate `HexagonalArchitectureTest`'s "only `application.service`
uses `@Transactional`" rule). Re-verified both against a new permanent regression test,
`RegisterCandidateServiceConcurrencyIntegrationTest` (calls the real `RegisterCandidateService`,
not the adapter directly — the only way to exercise this exact bug), and by firing the same two
real concurrent requests at the rebuilt Docker stack five more times in a row: `201`/`409` every
time, never a `500`, and the retried duplicate afterward still correctly returns `409`.

### 7. Real HTTP timeouts for the country-service call

`resilience4j.timelimiter` in `application.yml` was dead configuration: a `@TimeLimiter` annotation
only has an effect on a method returning a `CompletableFuture`/`Supplier` run asynchronously, and
`CountryClient#getCountry` is a plain synchronous `RestClient` call — the `TimeLimiter` never
applied to it. A country service that accepted the connection but then hung would block the
calling thread indefinitely, `resilience4j.timelimiter` config notwithstanding. **Removed** the
inert `resilience4j.timelimiter` block entirely rather than leave a comment saying "this doesn't
work."

**Replaced with real, effective timeouts** on `RestClientConfig`'s `SimpleClientHttpRequestFactory`
(`setConnectTimeout`/`setReadTimeout`, plain `java.net.HttpURLConnection`-backed socket timeouts —
these actually bound a blocking call, unlike the TimeLimiter), externalized via
`country-service.connect-timeout-ms` / `country-service.read-timeout-ms`
(`COUNTRY_SERVICE_CONNECT_TIMEOUT_MS` / `COUNTRY_SERVICE_READ_TIMEOUT_MS` env vars, defaulting to
1000ms/2000ms — generous for a same-network call but bounded). The Circuit Breaker and fail-safe
behavior are unchanged: a timeout throws `RestClientException` from `CountryClient`, which
`CountryCheckAdapter`'s `@CircuitBreaker` fallback converts into `CountryCheckUnavailableException`,
which `AcceptCandidateService` treats as "country is banned" — the candidate is not accepted.

Verified in
`CountryCheckAdapterTest#respondsWithinBoundedTimeAndFailsSafeOnSlowCountryService`: a WireMock
stub configured with a 5-second fixed delay, and a 300ms read timeout for the test. The call
returns `CountryCheckUnavailableException` in well under 2 seconds — proof the timeout is actually
enforced, not merely declared in config.

### 8. Frontend — out-of-order responses and filter/counter coherence

**Request races.** `usePotentialSuppliers` now tracks the in-flight request via an
`AbortController` (`hooks/usePotentialSuppliers.ts`): starting a new search (a new amount, or a
page change) aborts whatever request is still pending, and every `then`/`catch`/`finally`
callback checks its own controller's `signal.aborted` before touching state. This makes it
impossible for a superseded request to overwrite `suppliers`/`total`/`error`/`loading`, regardless
of which network call actually resolves first — verified in
`Dashboard.test.tsx#ignoresAStaleResponseThatResolvesAfterANewerOne` by resolving the *second*
request before the *first* and asserting the first's (stale) data never renders. A cancelled
request is explicitly distinguished from a real failure (`AbortError` is checked before it would
ever reach the user-facing error message) so cancellation never flashes an error. On unmount, an
`isMountedRef` guard (set in the `useEffect` cleanup, alongside aborting the in-flight request)
prevents any state update after the component is gone.

**Filter/counter coherence.** Two problems, both fixed in `Dashboard`/`useClientFilters`:
- Starting a new search (a different `rate`) now calls `useClientFilters#reset()`, clearing the
  free-text search, selected countries, and selected ratings. Without this, a filter left over
  from the previous result page (e.g. a country that doesn't appear in the new page at all) would
  silently filter the new, non-empty result down to zero rows — indistinguishable in the UI from
  the server genuinely returning nothing, which is a materially different situation.
- `Pagination` now receives both the server-reported `total` and the post-filter `visibleCount`,
  and — only while at least one client-side filter is active (`useClientFilters#hasActiveFilters`)
  — renders `"{visible} visible suppliers out of {total} total"` instead of the plain
  `"{total} suppliers found"`. The two numbers are never conflated: `total` always describes what
  the server matched for the current `rate`/page; `visibleCount` is only what survives the
  client-side name/DUNS/country/rating filters on the page that happens to be loaded right now.

**Known, deliberate limitation of the current contract**: the OpenAPI's `GET /suppliers/potential`
only exposes `rate`/`limit`/`offset` — there is no server-side text/country/rating filter and no
server-side column-sort parameter. This means the dashboard's client-side search box, country
filter, rating filter, and column-header sorting **only ever operate on the one page of results
already loaded from the server** (at most `limit`, i.e. 10, rows) — never on the full matching
dataset. A supplier that would match a filter but sits on page 3 will not appear until the user
pages to it first. This is not a frontend bug; it is the direct, honest consequence of the
contract as given (extending the API with new query parameters was explicitly out of scope per
this task's own instructions). If server-side filtering/sorting were added to the OpenAPI
contract, `usePotentialSuppliers`/`suppliersApi.ts` are the only places that would need to change.

### 9. Docker Compose — healthchecks and pinned versions

- `wiremock/wiremock:latest` pinned to `wiremock/wiremock:3.9.1` (matching the
  `wiremock-standalone` test dependency version), so a rebuild months from now can't silently pick
  up a breaking WireMock release.
- `country-service` gets a `curl -f http://localhost:8080/__admin/mappings` healthcheck (WireMock
  ships `curl`; verified by actually pulling the pinned image and exec-ing into it) — a 200 there
  means the mock mappings are loaded and serving.
- `backend` gets a `curl -f http://localhost:8080/actuator/health` healthcheck with a 30s
  `start_period` (Flyway + Hibernate schema validation + Spring context startup routinely takes
  20-40s per the progress log below) and 20 retries.
- `backend` now `depends_on: country-service: condition: service_healthy` (was
  `service_started`) — previously the backend could start accepting `accept` calls before WireMock
  had actually finished loading its mappings.
- `frontend` now `depends_on: backend: condition: service_healthy` (previously no condition at
  all) — the dashboard's first real API call now has a much better chance of hitting a backend
  that's actually ready, right after `docker compose up --build` returns.
- `db`'s existing `pg_isready` healthcheck was already correct and is unchanged.
- Both `.dockerignore` files were extended (`.git`, `*.log`, `.vscode`/`.idea`, `.DS_Store`,
  `coverage` on the frontend side) — none of these belong in a build context, and some
  (accidentally-committed IDE state, `.git`) could otherwise bloat the image or leak local file
  paths into build logs.
- The stale `# TODO: keep as "validate"...` and `# TODO: tune once integration-tested...` comments
  in `application.yml` were removed — the first was rephrased as a plain explanatory comment (the
  decision it described is not actually open, no fix needed), the second removed as part of
  deleting the inert `resilience4j.timelimiter` block entirely (see decision 7 above).

## Aspectos dejados fuera y por qué (full list)

- **422 on POST /candidates** — see above, no distinguishing business rule found.
- **`Restrict`/`Promote` endpoints** — domain/application stubs exist, no controller, no OpenAPI
  contract for them (see decision 1b above).
- **No authentication/authorization** — out of scope per the README, which describes "a
  supervisor" acting without specifying an auth model.
- **No idempotency key handling on POST /candidates** — not requested by the OpenAPI (no header
  parameter defined for it).

## Checklist de revisión final (mapeada a los criterios de evaluación del README)

- [x] **Architecture and design** — enforced as a build-time check, not just a javadoc claim:
      `HexagonalArchitectureTest` (ArchUnit, iteration 13) asserts `domain` imports no framework
      package, `domain`/`application` never depend inward-to-outward, and `@Transactional` lives
      only in `application.service`. All 4 rules pass.
- [x] **Business logic** — `domain.model` fully implemented; **all 7 OpenAPI endpoints** work end
      to end for real (verified against Postgres and, for `accept`, the WireMock country service
      through a genuinely wired Circuit Breaker — see "Progress log", iterations 5-9), including
      the internal→external status mapping, the confirmed "ban only from ON_PROBATION" decision,
      the country-check fail-safe (verified by actually stopping `country-service` mid-test), and
      the potential-suppliers scoring/bonus formula matched exactly against the README's worked
      example, all empirically confirmed, not just unit-tested. Reapply-after-refusal (decision 1a,
      iteration 16) closes the one behavior that was previously left unimplemented; optimistic
      locking and the concurrent-DUNS race (decision 6) close the two remaining concurrency gaps.
- [x] **Code quality** — removed the last stale "TODO: implement" javadoc from the 7
      `application.service` classes that had them (iteration 14; the domain/web/persistence
      layers were already clean). Constructor injection used throughout, no field injection
      anywhere in the codebase.
- [x] **Testing — 100% green, zero `@Disabled` stubs.** `domain.model`, all `application.service`
      classes, both web mapper classes, both `@WebMvcTest` controller classes,
      `SupplierPersistenceAdapterTest` (real Testcontainers PostgreSQL — includes the README's
      exact worked example seeded as real rows, plus the score-tie stable-pagination test), and
      `CountryCheckAdapterTest` (embedded WireMock inside a narrowly-scoped `@SpringBootTest` that
      exercises the real resilience4j Circuit Breaker AOP proxy, including proving the
      short-circuit path receives zero real requests once the breaker trips, and the new bounded-
      -time timeout test). See "Progress log", iterations 10-12 and 16 for exact counts. Iteration
      16 additionally adds `ConcurrencyIntegrationTest` (two real-Postgres tests: optimistic-lock
      lost-update prevention and the concurrent-duplicate-DUNS race) and the frontend's
      `Dashboard.test.tsx` (10 integration tests covering loading/success/error/empty/pagination/
      filters/default-and-toggled-sort/request-race/filter-reset). All 7 endpoints were also
      verified manually end-to-end via `curl`/`psql`/stopping containers against real Postgres and
      WireMock.
- [x] **Performance and scalability** — confirmed `findPotentialSuppliers` never materializes
      more than one page of entities (everything happens in one native SQL query). `EXPLAIN
      (ANALYZE, BUFFERS)` run against 300,000 seeded rows (iteration 13): the
      `status`-based index is genuinely used by the planner; the `country`-based index is not
      (Postgres sorts explicitly instead) — traced to a structural property of the confirmed
      rate-independent-bonus decision (§4), not a missing index, since ranking requires
      materializing the whole per-country eligible population before any `LIMIT` can apply. See
      "Progress log" iteration 13 and §4 below for the full analysis and the documented
      denormalization option left out of scope.
- [x] **Frontend components** — every README frontend requirement implemented (search with
      min-250 validation, sortable/filterable results table, client-side name/DUNS/country/rating
      filters, limit/offset pagination with result count, distinct loading/error/empty states —
      including the client-filtered-to-zero case, not just the server-empty one) and verified in a
      real browser against the real backend, not just by reading the code. See "Progress log"
      iteration 15 for the two real bugs (native `min` validation swallowing the custom message;
      the missing client-filtered empty state) and the CORS gap it also surfaced and fixed.
      Iteration 16 adds request-race cancellation (`AbortController`), filter reset on new search,
      the visible-vs-total count distinction, and moves `aria-sort` onto the `<th>` with an
      accessible sorted-direction announcement for screen readers (see decision 8/9).
- [x] **Docker Compose** — `docker compose up --build` (all four services) boots cleanly end to
      end; verified by seeding real data through the live API and driving the running dashboard in
      a browser, covering search, sorting, filtering, both empty-state paths, and pagination
      across a real second page. See "Progress log" iteration 15. Iteration 16 adds real
      healthchecks for `backend`/`country-service`, `service_healthy` dependency conditions, and
      pins the WireMock image version (see decision 9).
- [x] **Documentation** — this file's "Design decisions", "Progress log", and checklist are kept
      in sync with iteration 16's work: reapply-after-refusal replaces the earlier no-reapply
      decision (no claim that the diagram overrides the written requirement remains anywhere in
      this file), and the new concurrency/timeout/frontend/Docker decisions are documented above.

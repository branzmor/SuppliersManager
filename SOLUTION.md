# SOLUTION.md

Status: **skeleton only**. This iteration delivers the full hexagonal/DDD package structure,
ports, DTOs (1:1 with the OpenAPI contract), controller/entity/mapper stubs, and test stubs, all
compilable but with `UnsupportedOperationException("TODO")` in place of business logic. No
business rule is implemented yet.

## How to start

```bash
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Country service (WireMock, provided): http://localhost:8088
- Postgres: localhost:5432 (user/pass/db: `supplier`)

**Known current limitation**: `backend/src/main/resources/db/migration/V1__create_supplier_record_table.sql`
is intentionally left fully commented out for this skeleton iteration (per the task's own
instruction: "puede ir comentado ... sin ejecutar todavía"). `spring.jpa.hibernate.ddl-auto` is
set to `validate`, so **the backend container will fail to start** until that migration is
uncommented (or otherwise implemented) — there is no table for Hibernate to validate
`SupplierRecordEntity` against yet. This is expected for this delivery; the next iteration
uncomments/finalizes the migration as part of implementing `application.service`.

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

### 1. FSM diagram vs. the written spec — two confirmed deviations

Before writing any code, the FSM diagram (`wiki/iop-techtest-fsm-supplier.png`) was compared
against the README text and the task's own state-machine description. Two discrepancies were
found and resolved with the stakeholder (recorded here so the reasoning survives into the
interview):

**a) No `reapply()` — REFUSED is terminal, like BANNED.**
The README's prose says "a refused candidacy allows the candidate to reapply," but the diagram
draws `Declined` flowing straight into a terminal state with no edge back to `Candidate`.
**Decision: the diagram wins.** `SupplierStatus.REFUSED` is terminal exactly like `BANNED` — there
is no `reapply()` operation anywhere in the codebase. A new `POST /candidates` for a DUNS already
in `REFUSED` status is rejected with the same `CandidateAlreadyExistsException` (409, "Candidate
already exists") used for any other non-`BANNED` existing record — no new exception type was
needed, the existing 6-exception table already covers it once `REFUSED` is treated as "a record
exists." **This is a deliberate deviation from the literal README sentence — flag it explicitly
in the interview**, since a reasonable alternative reading of the business text would implement
reapply. See `domain.model.SupplierStatus` and `SupplierRecord` javadoc for the in-code record of
this decision.

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
into the JVM. `SupplierRepositoryPort#findPotentialSuppliers` is contracted to do everything in
one query: filter (`annual_turnover > rate`, `status != BANNED`), bonus via
`DENSE_RANK() OVER (PARTITION BY country ORDER BY annual_turnover)` to find the two lowest unique
turnovers per country, score computation, `ORDER BY score DESC`, and `LIMIT/OFFSET`. See
`infrastructure.persistence.repository.SupplierRecordJpaRepository` (native `@Query` stub) and the
proposed indexes in `V1__create_supplier_record_table.sql` (`(country, annual_turnover)` for the
bonus window function, `(status, annual_turnover)` for the filter/order).

### 5. Known gap — 422 on `POST /candidates`

The OpenAPI declares a `422 Unprocessable Content` response on `POST /candidates`, but nothing in
the README's business rules triggers it distinctly from `400` (invalid request schema — handled
by Bean Validation) or `409` (duplicate/banned — handled by the two domain exceptions).
**Left unimplemented.** `GlobalExceptionHandler` documents this gap in its class javadoc rather
than fabricating a business rule to justify a 422 response. If a distinction becomes clear later
(e.g. semantically valid-but-nonsensical input, like a country code that doesn't exist per the
country service, as opposed to a country that is merely banned), this is the natural place to add
it.

## Aspectos dejados fuera y por qué (full list)

- **422 on POST /candidates** — see above, no distinguishing business rule found.
- **`Restrict`/`Promote` endpoints** — domain/application stubs exist, no controller, no OpenAPI
  contract for them (see decision 1b above).
- **Reapply after refusal** — intentionally not implemented, per decision 1a above (deviates from
  the literal README sentence; flag in interview).
- **Flyway migration left commented** — per the task's own instruction for this skeleton
  iteration; the backend will not fully boot until it's uncommented.
- **No authentication/authorization** — out of scope per the README, which describes "a
  supervisor" acting without specifying an auth model.
- **No idempotency key handling on POST /candidates** — not requested by the OpenAPI (no header
  parameter defined for it).

## Checklist de revisión final (mapeada a los criterios de evaluación del README)

- [ ] **Architecture and design** — domain has zero framework imports; verify with a build-time
      check (e.g. ArchUnit) before calling this done.
- [ ] **Business logic** — every `UnsupportedOperationException("TODO")` in `domain.model` and
      `application.service` replaced with real logic; every guard in the exception table actually
      throws the right exception in the right order.
- [ ] **Code quality** — remove now-stale TODO javadoc comments as each piece is implemented; keep
      constructor injection, no field injection.
- [ ] **Testing** — un-`@Disabled` each test stub as its production code lands; the
      `SupplierPersistenceAdapterTest` bonus-calculation test against the README's worked example
      (200k/200k/200k/210k/250k) is the single highest-value test in the whole suite — do not skip
      it.
- [ ] **Performance and scalability** — confirm `findPotentialSuppliers` never materializes more
      than one page of entities; run `EXPLAIN` on the final query against a seeded 100k+ row table.
- [ ] **Frontend components** — every component under `src/components` currently returns `null`;
      confirm loading/error/empty states are reachable and distinct once wired.
- [ ] **Docker Compose** — `docker compose up --build` must boot db → backend → frontend cleanly
      once the Flyway migration is uncommented; re-verify after that change.
- [ ] **Documentation** — keep this file's "Design decisions" section in sync with any further
      pivots, especially if the two confirmed FSM deviations above are revisited.

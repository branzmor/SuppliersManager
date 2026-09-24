# End-to-end tests

Browser-level tests for the potential suppliers dashboard. The scenarios are written in Gherkin
and run with Playwright against the **whole stack**, in its own Docker containers:

```
Chromium ─▶ frontend (nginx + built SPA) ─▶ backend (Spring Boot) ─▶ PostgreSQL
                                                        └──────────▶ country service (WireMock)
```

## Running

Requirements: Node 20+ and a running Docker daemon. Nothing else. The stack is built from
`backend/` and `frontend/`, so no JDK or Maven is needed.

```bash
cd e2e
npm ci
npx playwright install chromium   # once per machine
npm run e2e                       # headless: build + start stack, seed, run, tear down
```

| Command | Purpose |
|---|---|
| `npm run e2e` | Full headless run (the one CI should use) |
| `npm run e2e:headed` | Same, with a visible browser |
| `npm run e2e:ui` | Playwright UI mode: pick scenarios, time-travel through steps |
| `npm run e2e:debug` | Playwright Inspector, step by step |
| `npm run e2e:report` | Open the HTML report of the last run (`playwright-report/`) |
| `npm run lint` / `npm run typecheck` | ESLint (incl. `eslint-plugin-playwright`) / `tsc` |

Each `e2e*` script runs `bddgen` first, which turns the `.feature` files into Playwright specs in
`.features-gen/` (git-ignored). You can pass normal Playwright options after `--`, e.g.
`npm run e2e -- --grep "@network"` or `npm run e2e -- --project dashboard`.

| Variable | Default | Effect |
|---|---|---|
| `E2E_FRONTEND_PORT` / `E2E_BACKEND_PORT` | `15173` / `18080` | Host ports of the E2E stack. They differ from the regular `docker compose up` stack, so both can run at once |
| `E2E_KEEP_STACK=1` | unset | Leave the stack running after the run so you can inspect it. The next run recreates it anyway |
| `CI` | unset | Enables 2 retries, 2 workers and `forbidOnly` |

Diagnostics: failures keep a screenshot and video. The first retry records a trace. All of them
are linked from the HTML report. If the stack fails to become healthy, the last container logs are
printed before the run aborts.

## Layout

```
e2e/
├── features/
│   ├── dashboard/        search, filters, sorting, pagination (genuine E2E, read-only)
│   ├── lifecycle/        @lifecycle: candidate → supplier → banned, as seen on the dashboard
│   └── network/          @network: loading and failure states (intercepted network)
├── steps/                step definitions: one file per area, thin, delegating to the page object
├── pages/DashboardPage.ts  the single page object (the app has one page)
├── support/
│   ├── fixtures.ts         Playwright fixtures + createBdd() (dashboard, searchEndpoint, supplierApi, scenario)
│   ├── supplier-api.ts     typed client for the backend REST API (arranging lifecycle state)
│   ├── search-endpoint.ts  records and, for @network scenarios, intercepts GET /suppliers/potential
│   ├── scenario-state.ts   per-scenario data (unique suppliers, last API response)
│   ├── reference-suppliers.ts  the seeded catalogue the dashboard features assert against
│   ├── global-setup.ts / global-teardown.ts / stack.ts   stack lifecycle and seeding
│   └── env.ts              ports/URLs (shared with docker-compose.e2e.yml)
├── docker-compose.e2e.yml  isolated stack: own project name, tmpfs database
└── playwright.config.ts
```

Flow: `.feature` → `bddgen` → generated spec → step definition → `DashboardPage` / `SupplierApi`
→ Playwright → real application.

## Design decisions

**Why Playwright.** It gives auto-waiting, web-first assertions, network interception and
trace/video diagnostics in one tool. It also has first-class TypeScript support, which matches
the frontend.

**Why Gherkin (via `playwright-bdd`).** The scenarios read as the business rules from the
challenge statement (eligibility by turnover, score order, the minimum amount of 250, lifecycle
visibility), so a reviewer can check coverage without reading code. `playwright-bdd` compiles
features into ordinary Playwright tests instead of running a separate Cucumber runner. Fixtures,
parallelism, retries, UI mode, traces and the HTML report therefore all work unchanged.

**The stack is started in `globalSetup`, not with `webServer`.** `webServer` manages a single
foreground process polled on one URL. This suite needs four detached services with healthchecks
(`docker compose up --wait`), a guaranteed empty database before seeding, and removal afterwards.

**Test data strategy.**
- *Isolated environment.* The stack runs as its own Compose project with Postgres on tmpfs, so
  every run starts from an empty, freshly migrated database. It never touches the developer's
  `docker compose up` data.
- *Reference catalogue.* `global-setup.ts` seeds 12 suppliers **through the public API**
  (register + accept). This way the data passes through the real validation, country check and
  state machine. The dashboard features only read, so they run fully in parallel and can assert
  exact rows, order, formatting and scores. The expected scores in the features are
  hand-computed from `reference-suppliers.ts`, which includes the per-country small-supplier bonus.
  A `Background` step re-verifies the catalogue through the API, so a broken precondition fails
  clearly.
- *Lifecycle scenarios own their data.* Each creates suppliers with a random DUNS, in a country
  no other scenario uses, with a turnover (≥ 1 000 000 000 000 €) far above the catalogue. The
  scenario then searches for "its turnover − 1", which lists only suppliers created at that moment,
  so the result always fits on one page. That makes "is *not* offered" a real check, not a
  pagination artefact.
- *Ordering without coupling.* Lifecycle suppliers would appear in every catalogue search, so
  the `lifecycle` Playwright project `dependencies`-waits for the `dashboard` project. No test
  depends on another test's side effects, and each scenario gets a fresh browser context.
- No `waitForTimeout` anywhere. Synchronisation relies on web-first assertions and, for the
  loading state, on holding the response until the step releases it.

**Genuine vs intercepted.** Everything under `dashboard/` and `lifecycle/` is genuine E2E. Nothing
is mocked: browser → nginx → Spring → Postgres/WireMock. Only the `@network` feature uses
`page.route` on `GET /suppliers/potential`, to reproduce a 500 with an `info` message, a 503
without a body, a dropped connection, and a slow response. None of these can be triggered on
demand against the real backend. Even there, the loading and recovery scenarios still get their
data from the real backend. `SearchEndpoint` is also used, without intercepting anything, to prove
that invalid amounts never reach the server.

**Selectors.** Only user-facing locators are used: roles, labels, accessible names and visible
text. The single frontend change was accessibility-driven. The rating checkboxes now form a
`role="group"` labelled "Rating". Before, screen readers announced them as bare "A", "B", …; now
the tests can also scope to that group.

**Scope.** The dashboard is the application's only UI. Candidate/supplier management has no
screens, so lifecycle scenarios arrange state through the API and assert what the buyer sees,
plus the resulting backend status where that adds meaning (`Active`/`Disqualified`). API-only
rules (e.g. 409s on duplicate DUNS) are already covered by the backend's controller and contract
tests and are not repeated here.

**Known limitation.** In `npm run e2e:ui` the stack is seeded once per UI session. Re-running the
`dashboard` scenarios *after* lifecycle ones in the same session will see the extra suppliers.
Restart UI mode to reset.

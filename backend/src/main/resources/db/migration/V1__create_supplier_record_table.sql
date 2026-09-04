-- Schema for the single SupplierRecord aggregate (see domain.model.SupplierRecord and
-- infrastructure.persistence.entity.SupplierRecordEntity, which this table backs 1:1 under
-- Spring Boot's default camelCase -> snake_case column naming).
--
-- Column types were picked to satisfy Hibernate's ddl-auto=validate exactly (verified against a
-- running container, not just read): a plain String field (`country`) validates against
-- VARCHAR, but an @Enumerated(EnumType.STRING) field with @Column(length = 1)
-- (`sustainabilityRating`) validates against CHAR(1) instead — Hibernate 6 picks CHAR for
-- length-1 enum-as-string columns specifically. Non-obvious; don't "fix" one without
-- re-verifying in Docker.
--
-- The UNIQUE(duns) constraint is what makes the three integrity rules in README §"Integrity
-- Rules" automatic: only one candidacy, only one supplier, never both, all for the same DUNS.
--
-- Indexes for /suppliers/potential (README §6, 100k-1M rows):
--   - idx_supplier_record_country_turnover backs the DENSE_RANK() OVER (PARTITION BY country
--     ORDER BY annual_turnover) window function used for the small-supplier bonus.
--   - idx_supplier_record_status_turnover backs the "annual_turnover > :rate AND status !=
--     'BANNED'" filter + score-descending ordering.

CREATE TABLE supplier_record (
    id                      BIGSERIAL PRIMARY KEY,
    duns                    INTEGER NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    country                 VARCHAR(2) NOT NULL,
    annual_turnover         BIGINT NOT NULL CHECK (annual_turnover >= 0),
    status                  VARCHAR(20) NOT NULL
        CHECK (status IN ('CANDIDATE', 'ACTIVE', 'ON_PROBATION', 'REFUSED', 'BANNED')),
    sustainability_rating   CHAR(1)
        CHECK (sustainability_rating IN ('A', 'B', 'C', 'D', 'E')),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_supplier_record_duns UNIQUE (duns)
);

CREATE INDEX idx_supplier_record_country_turnover
    ON supplier_record (country, annual_turnover)
    WHERE status <> 'BANNED';

CREATE INDEX idx_supplier_record_status_turnover
    ON supplier_record (status, annual_turnover);

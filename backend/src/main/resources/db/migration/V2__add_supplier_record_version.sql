-- Adds optimistic-locking support to supplier_record (see
-- infrastructure.persistence.entity.SupplierRecordEntity's @Version field).
--
-- Existing rows default to version 0, matching the value Hibernate assigns to a newly-persisted
-- entity whose @Version field is null - so a row written before this migration and one written
-- after are indistinguishable to the optimistic-locking check.
--
-- Purely additive - does not touch V1's table definition, columns, or constraints.

ALTER TABLE supplier_record
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

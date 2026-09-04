package com.inditex.supplier.infrastructure.persistence.adapter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * TODO coverage — Testcontainers-backed integration test (real PostgreSQL, {@code @SpringBootTest}
 * or {@code @DataJpaTest} with {@code @Testcontainers}), since this is where the
 * SQL-only score/bonus/pagination requirement (README §6) must actually be verified:
 * <ul>
 *   <li>{@code findByDuns} / {@code save} round-trip preserves all fields including status and
 *       rating.</li>
 *   <li>{@code save} on an existing DUNS updates in place (upsert semantics via
 *       {@code UNIQUE(duns)}), never creates a duplicate row.</li>
 *   <li>{@code findPotentialSuppliers}: excludes {@code BANNED} suppliers.</li>
 *   <li>{@code findPotentialSuppliers}: excludes suppliers with
 *       {@code annualTurnover <= rate} (strict inequality).</li>
 *   <li>{@code findPotentialSuppliers}: score formula matches
 *       {@code turnover * 0.1 * ratingConstant * bonus} exactly.</li>
 *   <li>{@code findPotentialSuppliers}: small-supplier bonus — reproduce the README example
 *       (s1:200k, s2:200k, s3:200k, s4:210k, s5:250k in the same country) and assert s1-s4 get
 *       the 1.25x bonus, s5 does not.</li>
 *   <li>{@code findPotentialSuppliers}: results ordered by score descending.</li>
 *   <li>{@code findPotentialSuppliers}: {@code limit}/{@code offset} paginate correctly and
 *       {@code totalCount} reflects the full matching set, not just the page.</li>
 *   <li>Consider a dedicated perf/volume test seeding a large synthetic dataset to sanity-check
 *       the query plan uses the indexes from {@code V1__create_supplier_record_table.sql} (e.g.
 *       via {@code EXPLAIN}), given the 100k-1M row requirement.</li>
 * </ul>
 */
class SupplierPersistenceAdapterTest {

    @Test
    @Disabled("TODO: implement with Testcontainers PostgreSQL - see class javadoc")
    void saveAndFindByDunsRoundTrip() {
    }

    @Test
    @Disabled("TODO: implement with Testcontainers PostgreSQL - see class javadoc")
    void findPotentialSuppliersExcludesBannedAndBelowRate() {
    }

    @Test
    @Disabled("TODO: implement with Testcontainers PostgreSQL - see class javadoc")
    void findPotentialSuppliersAppliesSmallSupplierBonusPerReadmeExample() {
    }

    @Test
    @Disabled("TODO: implement with Testcontainers PostgreSQL - see class javadoc")
    void findPotentialSuppliersOrdersByScoreDescendingAndPaginates() {
    }
}

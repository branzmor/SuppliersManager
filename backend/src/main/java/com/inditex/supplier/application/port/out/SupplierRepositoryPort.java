package com.inditex.supplier.application.port.out;

import com.inditex.supplier.domain.exception.CandidateAlreadyExistsException;
import com.inditex.supplier.domain.exception.SupplierBannedException;
import com.inditex.supplier.domain.model.Duns;
import com.inditex.supplier.domain.model.SupplierRecord;

import java.util.Optional;

/**
 * Driven port for persisting and querying {@link SupplierRecord} aggregates.
 *
 * <p>Implemented by {@code infrastructure.persistence.adapter.SupplierPersistenceAdapter} on top
 * of Spring Data JPA. The single {@code UNIQUE(duns)} constraint on the backing table is what
 * makes the aggregate's integrity rules automatic (see {@link SupplierRecord} javadoc).
 */
public interface SupplierRepositoryPort {

    /**
     * @return the aggregate for the given DUNS, if a row exists (regardless of its current
     *     status — visibility filtering per resource type happens in
     *     {@code application.service}, not here).
     */
    Optional<SupplierRecord> findByDuns(Duns duns);

    /**
     * Persists a new or updated aggregate. Because {@link Duns} is the identity and the table has
     * a {@code UNIQUE(duns)} constraint, this is effectively an upsert keyed by DUNS.
     *
     * <p>Two concurrency failure modes are translated into the same business exceptions a caller
     * already handles for the non-racing case, rather than surfacing as a raw 500:
     * <ul>
     *   <li>a concurrent update of the exact same row (lost-update prevention via the entity's
     *       {@code @Version} column) throws
     *       {@code org.springframework.orm.ObjectOptimisticLockingFailureException} — left
     *       untranslated here since it isn't a domain exception, but mapped to {@code 409} by
     *       {@code GlobalExceptionHandler};</li>
     *   <li>a concurrent <em>insert</em> for the same DUNS racing past the caller's own
     *       {@code findByDuns} check (last line of defense: the {@code UNIQUE(duns)} constraint
     *       itself) throws {@link SupplierBannedException} or {@link CandidateAlreadyExistsException},
     *       resolved by re-reading whichever row won the race.</li>
     * </ul>
     */
    SupplierRecord save(SupplierRecord record);

    /**
     * Resolves the page of potential suppliers for a given order {@code rate}, entirely in SQL:
     * filtering ({@code annual_turnover > rate}, not {@code BANNED}), score and "small supplier
     * bonus" computation (bonus requires a {@code DENSE_RANK() OVER (PARTITION BY country ORDER
     * BY annual_turnover)} to find the two lowest unique turnovers per country), descending-score
     * ordering (tie-broken by DUNS ascending, so the ordering is total and pagination is stable
     * even when several suppliers share the exact same score), and {@code limit}/{@code offset}
     * pagination.
     *
     * <p><strong>Must never load the full dataset into memory</strong> — the expected volume is
     * 100,000 to 1,000,000 suppliers (README §6, "Performance and scalability").
     *
     * @param rate the order amount to simulate (query param {@code rate}, minimum 250)
     * @param limit page size, 1..10 (OpenAPI {@code QueryLimit}, default 10)
     * @param offset zero-based offset (OpenAPI {@code QueryOffset}, default 0)
     * @return the requested page plus the total count of matching suppliers
     */
    PotentialSuppliersPage findPotentialSuppliers(long rate, int limit, int offset);
}

package com.inditex.supplier.infrastructure.persistence.repository;

import com.inditex.supplier.infrastructure.persistence.entity.SupplierRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link SupplierRecordEntity}.
 *
 * <p>{@link #findPotentialSuppliersRaw} and {@link #countPotentialSuppliers} back
 * {@code SupplierRepositoryPort#findPotentialSuppliers}. Both run entirely in SQL — filter,
 * bonus (via {@code DENSE_RANK() OVER (PARTITION BY country ORDER BY annual_turnover)} to find
 * the two lowest unique turnovers per country), score, ordering and pagination — because the
 * expected volume is 100k-1M rows (README §6). No entity is fetched and post-processed in Java.
 *
 * <p><strong>Population for the small-supplier bonus ranking</strong> (confirmed project
 * decision): the {@code DENSE_RANK()} window is computed over ALL {@code ACTIVE}/
 * {@code ON_PROBATION} suppliers of a country — never restricted to the current {@code rate}'s
 * eligible subset. The bonus is a stable, rate-independent property of "being one of the two
 * smallest suppliers in your country," matching the README's worked example, which describes 5
 * suppliers in a country with no rate in the picture at all. {@code CANDIDATE}/{@code REFUSED}
 * (no rating yet) and {@code BANNED} (not a potential supplier at all, per README) are excluded
 * from the ranking population, not just from the final eligible result.
 */
public interface SupplierRecordJpaRepository extends JpaRepository<SupplierRecordEntity, Long> {

    Optional<SupplierRecordEntity> findByDuns(Integer duns);

    /**
     * Eligibility ({@code annual_turnover > :rate}) is applied only in the outer query, after the
     * bonus ranking has already been computed over the full country population — see class
     * javadoc. {@code score = annual_turnover * 0.1 * rating_constant * bonus}, {@code bonus} is
     * 1.25 for the two lowest unique turnovers per country, else 1.
     *
     * <p><strong>Stable pagination</strong>: {@code ORDER BY score DESC, duns ASC} — {@code score}
     * alone is not a unique key (multiple suppliers can tie exactly), so a {@code LIMIT}/
     * {@code OFFSET} query ordered by {@code score} alone would be ordered non-deterministically
     * among tied rows (Postgres gives no ordering guarantee for ties without a fully-specifying
     * {@code ORDER BY}), which can duplicate or skip rows across consecutive pages. Breaking ties
     * by {@code duns} (already unique) makes the ordering total and pagination stable.
     */
    @Query(value = """
            WITH ranked AS (
                SELECT duns, name, country, annual_turnover, status, sustainability_rating,
                       DENSE_RANK() OVER (PARTITION BY country ORDER BY annual_turnover) AS turnover_rank
                FROM supplier_record
                WHERE status IN ('ACTIVE', 'ON_PROBATION')
            )
            SELECT
                duns,
                name,
                country,
                annual_turnover AS "annualTurnover",
                status,
                sustainability_rating AS "sustainabilityRating",
                (annual_turnover * 0.1 *
                    (CASE sustainability_rating
                        WHEN 'A' THEN 1.0
                        WHEN 'B' THEN 0.75
                        WHEN 'C' THEN 0.5
                        WHEN 'D' THEN 0.25
                        WHEN 'E' THEN 0.1
                    END) *
                    (CASE WHEN turnover_rank <= 2 THEN 1.25 ELSE 1.0 END)
                )::double precision AS score
            FROM ranked
            WHERE annual_turnover > :rate
            ORDER BY score DESC, duns ASC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<PotentialSupplierProjection> findPotentialSuppliersRaw(@Param("rate") long rate,
                                                                 @Param("limit") int limit,
                                                                 @Param("offset") int offset);

    /**
     * Companion {@code COUNT(*)} mirroring the eligibility filter of
     * {@link #findPotentialSuppliersRaw} (same population, no need to compute the bonus/rank just
     * to count rows), for {@code Pagination.total}.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM supplier_record
            WHERE status IN ('ACTIVE', 'ON_PROBATION') AND annual_turnover > :rate
            """, nativeQuery = true)
    long countPotentialSuppliers(@Param("rate") long rate);
}

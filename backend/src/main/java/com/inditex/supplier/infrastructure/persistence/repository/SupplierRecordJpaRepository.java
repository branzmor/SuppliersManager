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
 * {@code SupplierRepositoryPort#findPotentialSuppliers}. Both MUST run entirely in SQL — filter,
 * bonus (via {@code DENSE_RANK() OVER (PARTITION BY country ORDER BY annual_turnover)} to find
 * the two lowest unique turnovers per country), score, ordering and pagination — because the
 * expected volume is 100k-1M rows (README §6). Do not fetch entities and post-process in Java.
 *
 * <p>TODO: implement {@code findPotentialSuppliersRaw} as a native query (JPQL cannot express
 * {@code DENSE_RANK()}); a native projection or a dedicated read-model DTO is recommended over
 * returning entities directly.
 */
public interface SupplierRecordJpaRepository extends JpaRepository<SupplierRecordEntity, Long> {

    Optional<SupplierRecordEntity> findByDuns(Integer duns);

    /**
     * TODO: native SQL using {@code DENSE_RANK()} for the small-supplier bonus, filtering
     * {@code annual_turnover > :rate AND status != 'BANNED'}, ordering by the computed score
     * descending, and applying {@code LIMIT :limit OFFSET :offset}.
     */
    @Query(value = "SELECT * FROM supplier_record WHERE 1 = 0", nativeQuery = true)
    List<SupplierRecordEntity> findPotentialSuppliersRaw(@Param("rate") long rate,
                                                          @Param("limit") int limit,
                                                          @Param("offset") int offset);

    /**
     * TODO: companion COUNT(*) query mirroring the same filter as
     * {@link #findPotentialSuppliersRaw}, for {@code Pagination.total}.
     */
    @Query(value = "SELECT 0", nativeQuery = true)
    long countPotentialSuppliers(@Param("rate") long rate);
}

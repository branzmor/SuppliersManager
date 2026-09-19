package com.supplier.management.infrastructure.persistence.repository;

/**
 * Spring Data JPA interface projection for the native
 * {@link SupplierRecordJpaRepository#findPotentialSuppliersRaw} query. {@code score} is not a
 * real column — it is computed entirely in SQL (see that method's query) and only exists in the
 * context of this projection, never on {@link com.supplier.management.infrastructure.persistence.entity.SupplierRecordEntity}.
 *
 * <p>Getter names must match the native query's column aliases (case-sensitive for the
 * double-quoted camelCase aliases — Postgres folds unquoted identifiers to lowercase).
 */
public interface PotentialSupplierProjection {

    Integer getDuns();

    String getName();

    String getCountry();

    Long getAnnualTurnover();

    String getStatus();

    String getSustainabilityRating();

    Double getScore();
}

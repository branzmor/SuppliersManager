package com.supplier.management.domain.model;

/**
 * Sustainability rating assigned by a supervisor on {@code accept()} (README §1).
 *
 * <p>Drives two business rules:
 * <ul>
 *   <li>{@code A} or {@code B} → the candidate becomes {@link SupplierStatus#ACTIVE}.</li>
 *   <li>{@code C}, {@code D} or {@code E} → the candidate becomes
 *       {@link SupplierStatus#ON_PROBATION}.</li>
 * </ul>
 *
 * <p>{@link #ratingConstant()} mirrors the {@code rating_constant} used in the potential
 * suppliers score formula. The authoritative computation happens in SQL for performance at the
 * 100k–1M supplier scale (see {@code SupplierRepositoryPort#findPotentialSuppliers}); this
 * constant is kept here so domain unit tests can assert the same values without touching the
 * database.
 */
public enum SustainabilityRating {
    A(1.0),
    B(0.75),
    C(0.5),
    D(0.25),
    E(0.1);

    private final double ratingConstant;

    SustainabilityRating(double ratingConstant) {
        this.ratingConstant = ratingConstant;
    }

    public double ratingConstant() {
        return ratingConstant;
    }

    /**
     * @return true if this rating leads to {@link SupplierStatus#ACTIVE} on acceptance
     *     (i.e. {@code A} or {@code B}).
     */
    public boolean qualifiesForActive() {
        return this == A || this == B;
    }
}

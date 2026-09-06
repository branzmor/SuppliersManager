package com.inditex.supplier.domain.model;

/**
 * Annual turnover of a candidate/supplier, expressed in euros.
 *
 * <p>Business rules that depend on this value object:
 * <ul>
 *   <li>A candidate cannot be {@code accept()}-ed if {@code value < 1_000_000} (README §1).</li>
 *   <li>A supplier is eligible as a potential supplier only if its turnover is strictly
 *       greater than the requested order {@code rate} (README §"Potential Suppliers").</li>
 *   <li>The "small supplier bonus" is based on the two lowest unique turnovers per country —
 *       computed in SQL, not here (see {@code application.port.out.SupplierRepositoryPort}).</li>
 * </ul>
 *
 * @param value the annual turnover in euros, must be &gt;= 0
 */
public record AnnualTurnover(long value) {

    public static final long MINIMUM_ACCEPTABLE_TURNOVER = 1_000_000L;

    public AnnualTurnover {
        if (value < 0) {
            throw new IllegalArgumentException("annualTurnover must be >= 0, got " + value);
        }
    }

    /**
     * @return true if this turnover meets the minimum required to accept a candidate.
     */
    public boolean meetsMinimumForAcceptance() {
        return value >= MINIMUM_ACCEPTABLE_TURNOVER;
    }

    /**
     * @param rate the order amount to simulate
     * @return true if this turnover is strictly greater than {@code rate} (potential supplier
     *     eligibility rule).
     */
    public boolean isEligibleFor(long rate) {
        return value > rate;
    }
}

package com.inditex.supplier.domain.model;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * TODO coverage: negative values rejected; {@code meetsMinimumForAcceptance()} boundary at
 * exactly 1,000,000 (inclusive, per README "less than one million" i.e. &gt;= 1M passes);
 * {@code isEligibleFor(rate)} boundary — strictly greater than rate required (README
 * "Potential Suppliers" eligibility rule), so turnover == rate must NOT be eligible.
 */
class AnnualTurnoverTest {

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void rejectsNegativeValues() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void meetsMinimumForAcceptanceBoundary() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void isEligibleForRequiresStrictlyGreaterThanRate() {
    }
}

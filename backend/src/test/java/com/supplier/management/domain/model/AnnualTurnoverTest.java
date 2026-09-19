package com.supplier.management.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Negative values rejected; {@code meetsMinimumForAcceptance()} boundary at exactly 1,000,000
 * (inclusive, per README "less than one million" i.e. &gt;= 1M passes); {@code isEligibleFor(rate)}
 * boundary — strictly greater than rate required (README "Potential Suppliers" eligibility rule),
 * so turnover == rate must NOT be eligible.
 */
class AnnualTurnoverTest {

    @Test
    void rejectsNegativeValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> new AnnualTurnover(-1L));
    }

    @Test
    void meetsMinimumForAcceptanceBoundary() {
        assertThat(new AnnualTurnover(999_999L).meetsMinimumForAcceptance()).isFalse();
        assertThat(new AnnualTurnover(1_000_000L).meetsMinimumForAcceptance()).isTrue();
        assertThat(new AnnualTurnover(1_000_001L).meetsMinimumForAcceptance()).isTrue();
    }

    @Test
    void isEligibleForRequiresStrictlyGreaterThanRate() {
        assertThat(new AnnualTurnover(250L).isEligibleFor(250L)).isFalse();
        assertThat(new AnnualTurnover(251L).isEligibleFor(250L)).isTrue();
    }
}

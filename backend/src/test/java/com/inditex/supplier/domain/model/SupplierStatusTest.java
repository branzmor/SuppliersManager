package com.inditex.supplier.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code isTerminal()} true for {@code REFUSED} and {@code BANNED} only (confirmed no-reapply
 * decision — see {@link SupplierStatus} javadoc), false for
 * {@code CANDIDATE}/{@code ACTIVE}/{@code ON_PROBATION}.
 */
class SupplierStatusTest {

    @Test
    void onlyRefusedAndBannedAreTerminal() {
        assertThat(SupplierStatus.REFUSED.isTerminal()).isTrue();
        assertThat(SupplierStatus.BANNED.isTerminal()).isTrue();
        assertThat(SupplierStatus.CANDIDATE.isTerminal()).isFalse();
        assertThat(SupplierStatus.ACTIVE.isTerminal()).isFalse();
        assertThat(SupplierStatus.ON_PROBATION.isTerminal()).isFalse();
    }
}

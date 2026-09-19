package com.supplier.management.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code isTerminal()} is true only for {@code BANNED} (see {@link SupplierStatus} javadoc):
 * {@code REFUSED} can still transition back to {@code CANDIDATE} via
 * {@code SupplierRecord#reapply}, so it must not be reported as terminal.
 */
class SupplierStatusTest {

    @Test
    void onlyBannedIsTerminal() {
        assertThat(SupplierStatus.BANNED.isTerminal()).isTrue();
        assertThat(SupplierStatus.REFUSED.isTerminal()).isFalse();
        assertThat(SupplierStatus.CANDIDATE.isTerminal()).isFalse();
        assertThat(SupplierStatus.ACTIVE.isTerminal()).isFalse();
        assertThat(SupplierStatus.ON_PROBATION.isTerminal()).isFalse();
    }
}

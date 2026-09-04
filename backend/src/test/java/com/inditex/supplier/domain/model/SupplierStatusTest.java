package com.inditex.supplier.domain.model;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * TODO coverage: {@code isTerminal()} true for {@code REFUSED} and {@code BANNED} only (confirmed
 * no-reapply decision — see {@link SupplierStatus} javadoc), false for
 * {@code CANDIDATE}/{@code ACTIVE}/{@code ON_PROBATION}.
 */
class SupplierStatusTest {

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void onlyRefusedAndBannedAreTerminal() {
    }
}

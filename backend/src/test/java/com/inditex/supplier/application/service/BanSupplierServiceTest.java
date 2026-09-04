package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TODO coverage: not found → {@code SupplierRecordNotFoundException}; happy path ON_PROBATION →
 * BANNED, saved; ACTIVE (and any other status) → {@code SupplierNotBannableException} — the
 * ACTIVE case is the important regression test for the confirmed "ban only from ON_PROBATION"
 * decision, don't skip it.
 */
@ExtendWith(MockitoExtension.class)
class BanSupplierServiceTest {

    @Mock
    private SupplierRepositoryPort supplierRepositoryPort;

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void throwsNotFoundWhenNoRecordExists() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void bansSupplierOnProbationSuccessfully() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void throwsWhenStatusIsActiveNotOnProbation() {
    }
}

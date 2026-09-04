package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TODO coverage: returns present {@code Optional} for {@code CANDIDATE}/{@code REFUSED} records;
 * empty for {@code ACTIVE}/{@code ON_PROBATION}/{@code BANNED} (visibility filter, see
 * {@code SOLUTION.md}); empty when no record exists at all.
 */
@ExtendWith(MockitoExtension.class)
class GetCandidateServiceTest {

    @Mock
    private SupplierRepositoryPort supplierRepositoryPort;

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void returnsRecordWhenStatusIsCandidateOrRefused() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void returnsEmptyWhenStatusIsNotVisibleAsCandidate() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void returnsEmptyWhenNoRecordExists() {
    }
}

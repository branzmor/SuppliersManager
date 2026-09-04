package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TODO coverage: not found → {@code SupplierRecordNotFoundException}; happy path CANDIDATE →
 * REFUSED, saved; non-CANDIDATE → {@code CandidateNotRefusableException}, no save.
 */
@ExtendWith(MockitoExtension.class)
class RefuseCandidateServiceTest {

    @Mock
    private SupplierRepositoryPort supplierRepositoryPort;

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void throwsNotFoundWhenNoRecordExists() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void refusesCandidateSuccessfully() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void throwsWhenNotCandidateStatus() {
    }
}

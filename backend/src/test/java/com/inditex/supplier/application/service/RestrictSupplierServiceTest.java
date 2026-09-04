package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TODO coverage (extension, no controller wiring yet — see {@code SOLUTION.md}): ACTIVE →
 * ON_PROBATION, saved; non-ACTIVE → {@code SupplierNotRestrictableException}; not found →
 * {@code SupplierRecordNotFoundException}.
 */
@ExtendWith(MockitoExtension.class)
class RestrictSupplierServiceTest {

    @Mock
    private SupplierRepositoryPort supplierRepositoryPort;

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void restrictsActiveSupplierToOnProbation() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void throwsWhenNotActive() {
    }
}

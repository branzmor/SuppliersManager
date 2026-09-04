package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TODO coverage: returns present {@code Optional} for {@code ACTIVE}/{@code ON_PROBATION}/
 * {@code BANNED}; empty for {@code CANDIDATE}/{@code REFUSED} (visibility filter); empty when no
 * record exists.
 */
@ExtendWith(MockitoExtension.class)
class GetSupplierServiceTest {

    @Mock
    private SupplierRepositoryPort supplierRepositoryPort;

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void returnsRecordWhenStatusIsActiveOnProbationOrBanned() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void returnsEmptyWhenStatusIsNotVisibleAsSupplier() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void returnsEmptyWhenNoRecordExists() {
    }
}

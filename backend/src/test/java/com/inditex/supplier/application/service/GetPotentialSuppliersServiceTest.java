package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TODO coverage: this service is a thin pass-through, so unit tests here should mainly assert
 * correct delegation (rate/limit/offset forwarded verbatim, result mapped without loss) — the
 * actual scoring/bonus/pagination correctness belongs in
 * {@code infrastructure.persistence.adapter.SupplierPersistenceAdapterTest} (integration, real
 * SQL) precisely because README §6 requires it to run in the database, not in Java.
 */
@ExtendWith(MockitoExtension.class)
class GetPotentialSuppliersServiceTest {

    @Mock
    private SupplierRepositoryPort supplierRepositoryPort;

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void delegatesToRepositoryPortWithGivenParameters() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void mapsPortResultToUseCaseResultWithoutLoss() {
    }
}

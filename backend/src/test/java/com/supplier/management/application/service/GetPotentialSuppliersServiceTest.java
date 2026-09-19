package com.supplier.management.application.service;

import com.supplier.management.application.port.in.GetPotentialSuppliersUseCase.Query;
import com.supplier.management.application.port.in.GetPotentialSuppliersUseCase.Result;
import com.supplier.management.application.port.out.PotentialSuppliersPage;
import com.supplier.management.application.port.out.ScoredSupplier;
import com.supplier.management.application.port.out.SupplierRepositoryPort;
import com.supplier.management.domain.model.AnnualTurnover;
import com.supplier.management.domain.model.CountryCode;
import com.supplier.management.domain.model.Duns;
import com.supplier.management.domain.model.SupplierRecord;
import com.supplier.management.domain.model.SupplierStatus;
import com.supplier.management.domain.model.SustainabilityRating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This service is a thin pass-through, so unit tests here mainly assert correct delegation
 * (rate/limit/offset forwarded verbatim, result mapped without loss) — the actual
 * scoring/bonus/pagination correctness belongs in
 * {@code infrastructure.persistence.adapter.SupplierPersistenceAdapterTest} (real SQL) precisely
 * because README §6 requires it to run in the database, not in Java.
 */
@ExtendWith(MockitoExtension.class)
class GetPotentialSuppliersServiceTest {

    @Mock
    private SupplierRepositoryPort supplierRepositoryPort;

    private GetPotentialSuppliersService service;

    @BeforeEach
    void setUp() {
        service = new GetPotentialSuppliersService(supplierRepositoryPort);
    }

    @Test
    void delegatesToRepositoryPortWithGivenParameters() {
        when(supplierRepositoryPort.findPotentialSuppliers(500L, 5, 10))
                .thenReturn(new PotentialSuppliersPage(List.of(), 0L));

        service.getPotentialSuppliers(new Query(500L, 5, 10));

        verify(supplierRepositoryPort).findPotentialSuppliers(500L, 5, 10);
    }

    @Test
    void mapsPortResultToUseCaseResultWithoutLoss() {
        SupplierRecord record = SupplierRecord.reconstitute(new Duns(123_456_789), "Zippers & Buttons",
                new CountryCode("ES"), new AnnualTurnover(2_000_000L), SupplierStatus.ACTIVE, SustainabilityRating.A);
        ScoredSupplier scoredSupplier = new ScoredSupplier(record, 250_000.0);
        when(supplierRepositoryPort.findPotentialSuppliers(500L, 5, 10))
                .thenReturn(new PotentialSuppliersPage(List.of(scoredSupplier), 42L));

        Result result = service.getPotentialSuppliers(new Query(500L, 5, 10));

        assertThat(result.suppliers()).containsExactly(scoredSupplier);
        assertThat(result.limit()).isEqualTo(5);
        assertThat(result.offset()).isEqualTo(10);
        assertThat(result.total()).isEqualTo(42L);
    }
}

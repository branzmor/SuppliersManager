package com.supplier.management.application.service;

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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Returns present {@code Optional} for {@code ACTIVE}/{@code ON_PROBATION}/{@code BANNED}; empty
 * for {@code CANDIDATE}/{@code REFUSED} (visibility filter); empty when no record exists.
 */
@ExtendWith(MockitoExtension.class)
class GetSupplierServiceTest {

    private static final Duns DUNS = new Duns(123_456_789);

    @Mock
    private SupplierRepositoryPort supplierRepositoryPort;

    private GetSupplierService service;

    @BeforeEach
    void setUp() {
        service = new GetSupplierService(supplierRepositoryPort);
    }

    private static SupplierRecord recordIn(SupplierStatus status) {
        boolean hasRating = status != SupplierStatus.CANDIDATE && status != SupplierStatus.REFUSED;
        return SupplierRecord.reconstitute(DUNS, "Zippers & Buttons", new CountryCode("ES"),
                new AnnualTurnover(2_000_000L), status, hasRating ? SustainabilityRating.B : null);
    }

    @Test
    void returnsRecordWhenStatusIsActiveOnProbationOrBanned() {
        for (SupplierStatus status : new SupplierStatus[] {SupplierStatus.ACTIVE, SupplierStatus.ON_PROBATION, SupplierStatus.BANNED}) {
            when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(recordIn(status)));
            assertThat(service.getByDuns(DUNS.value())).isPresent();
        }
    }

    @Test
    void returnsEmptyWhenStatusIsNotVisibleAsSupplier() {
        for (SupplierStatus status : new SupplierStatus[] {SupplierStatus.CANDIDATE, SupplierStatus.REFUSED}) {
            when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(recordIn(status)));
            assertThat(service.getByDuns(DUNS.value())).isEmpty();
        }
    }

    @Test
    void returnsEmptyWhenNoRecordExists() {
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.empty());
        assertThat(service.getByDuns(DUNS.value())).isEmpty();
    }
}

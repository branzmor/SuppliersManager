package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import com.inditex.supplier.domain.model.AnnualTurnover;
import com.inditex.supplier.domain.model.CountryCode;
import com.inditex.supplier.domain.model.Duns;
import com.inditex.supplier.domain.model.SupplierRecord;
import com.inditex.supplier.domain.model.SupplierStatus;
import com.inditex.supplier.domain.model.SustainabilityRating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Returns present {@code Optional} for {@code CANDIDATE}/{@code REFUSED} records; empty for
 * {@code ACTIVE}/{@code ON_PROBATION}/{@code BANNED} (visibility filter, see
 * {@code SOLUTION.md}); empty when no record exists at all.
 */
@ExtendWith(MockitoExtension.class)
class GetCandidateServiceTest {

    private static final Duns DUNS = new Duns(123_456_789);

    @Mock
    private SupplierRepositoryPort supplierRepositoryPort;

    private GetCandidateService service;

    @BeforeEach
    void setUp() {
        service = new GetCandidateService(supplierRepositoryPort);
    }

    private static SupplierRecord recordIn(SupplierStatus status) {
        boolean hasRating = status != SupplierStatus.CANDIDATE && status != SupplierStatus.REFUSED;
        return SupplierRecord.reconstitute(DUNS, "Zippers & Buttons", new CountryCode("ES"),
                new AnnualTurnover(2_000_000L), status, hasRating ? SustainabilityRating.B : null);
    }

    @Test
    void returnsRecordWhenStatusIsCandidateOrRefused() {
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(recordIn(SupplierStatus.CANDIDATE)));
        assertThat(service.getByDuns(DUNS.value())).isPresent();

        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(recordIn(SupplierStatus.REFUSED)));
        assertThat(service.getByDuns(DUNS.value())).isPresent();
    }

    @Test
    void returnsEmptyWhenStatusIsNotVisibleAsCandidate() {
        for (SupplierStatus status : new SupplierStatus[] {SupplierStatus.ACTIVE, SupplierStatus.ON_PROBATION, SupplierStatus.BANNED}) {
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

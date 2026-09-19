package com.supplier.management.application.service;

import com.supplier.management.application.port.out.SupplierRepositoryPort;
import com.supplier.management.domain.exception.SupplierNotPromotableException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Extension (no controller wiring yet — see {@code SOLUTION.md}): ON_PROBATION → ACTIVE, saved;
 * non-ON_PROBATION → {@code SupplierNotPromotableException}.
 */
@ExtendWith(MockitoExtension.class)
class PromoteSupplierServiceTest {

    private static final Duns DUNS = new Duns(123_456_789);

    @Mock
    private SupplierRepositoryPort supplierRepositoryPort;

    private PromoteSupplierService service;

    @BeforeEach
    void setUp() {
        service = new PromoteSupplierService(supplierRepositoryPort);
    }

    private static SupplierRecord recordIn(SupplierStatus status) {
        return SupplierRecord.reconstitute(DUNS, "Zippers & Buttons", new CountryCode("ES"),
                new AnnualTurnover(2_000_000L), status, SustainabilityRating.D);
    }

    @Test
    void promotesOnProbationSupplierToActive() {
        SupplierRecord record = recordIn(SupplierStatus.ON_PROBATION);
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(record));

        service.promote(DUNS.value());

        assertThat(record.status()).isEqualTo(SupplierStatus.ACTIVE);
        verify(supplierRepositoryPort).save(record);
    }

    @Test
    void throwsWhenNotOnProbation() {
        SupplierRecord record = recordIn(SupplierStatus.ACTIVE);
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.promote(DUNS.value())).isInstanceOf(SupplierNotPromotableException.class);
    }
}

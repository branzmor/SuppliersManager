package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import com.inditex.supplier.domain.exception.SupplierNotRestrictableException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Extension (no controller wiring yet — see {@code SOLUTION.md}): ACTIVE → ON_PROBATION, saved;
 * non-ACTIVE → {@code SupplierNotRestrictableException}.
 */
@ExtendWith(MockitoExtension.class)
class RestrictSupplierServiceTest {

    private static final Duns DUNS = new Duns(123_456_789);

    @Mock
    private SupplierRepositoryPort supplierRepositoryPort;

    private RestrictSupplierService service;

    @BeforeEach
    void setUp() {
        service = new RestrictSupplierService(supplierRepositoryPort);
    }

    private static SupplierRecord recordIn(SupplierStatus status) {
        return SupplierRecord.reconstitute(DUNS, "Zippers & Buttons", new CountryCode("ES"),
                new AnnualTurnover(2_000_000L), status, SustainabilityRating.A);
    }

    @Test
    void restrictsActiveSupplierToOnProbation() {
        SupplierRecord record = recordIn(SupplierStatus.ACTIVE);
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(record));

        service.restrict(DUNS.value());

        assertThat(record.status()).isEqualTo(SupplierStatus.ON_PROBATION);
        verify(supplierRepositoryPort).save(record);
    }

    @Test
    void throwsWhenNotActive() {
        SupplierRecord record = recordIn(SupplierStatus.ON_PROBATION);
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.restrict(DUNS.value())).isInstanceOf(SupplierNotRestrictableException.class);
    }
}

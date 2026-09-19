package com.supplier.management.application.service;

import com.supplier.management.application.port.out.SupplierRepositoryPort;
import com.supplier.management.domain.exception.CandidateNotRefusableException;
import com.supplier.management.domain.exception.SupplierRecordNotFoundException;
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
 * Not found → {@code SupplierRecordNotFoundException}; happy path CANDIDATE → REFUSED, saved;
 * non-CANDIDATE → {@code CandidateNotRefusableException}, no save.
 */
@ExtendWith(MockitoExtension.class)
class RefuseCandidateServiceTest {

    private static final Duns DUNS = new Duns(123_456_789);

    @Mock
    private SupplierRepositoryPort supplierRepositoryPort;

    private RefuseCandidateService service;

    @BeforeEach
    void setUp() {
        service = new RefuseCandidateService(supplierRepositoryPort);
    }

    @Test
    void throwsNotFoundWhenNoRecordExists() {
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.refuse(DUNS.value())).isInstanceOf(SupplierRecordNotFoundException.class);
    }

    @Test
    void refusesCandidateSuccessfully() {
        SupplierRecord record = SupplierRecord.apply(DUNS, "Zippers & Buttons", new CountryCode("ES"),
                new AnnualTurnover(2_000_000L));
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(record));

        service.refuse(DUNS.value());

        assertThat(record.status()).isEqualTo(SupplierStatus.REFUSED);
        verify(supplierRepositoryPort).save(record);
    }

    @Test
    void throwsWhenNotCandidateStatus() {
        SupplierRecord record = SupplierRecord.reconstitute(DUNS, "Zippers & Buttons", new CountryCode("ES"),
                new AnnualTurnover(2_000_000L), SupplierStatus.ACTIVE, SustainabilityRating.A);
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.refuse(DUNS.value())).isInstanceOf(CandidateNotRefusableException.class);
    }
}

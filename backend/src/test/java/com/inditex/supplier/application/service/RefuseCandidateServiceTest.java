package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import com.inditex.supplier.domain.exception.CandidateNotRefusableException;
import com.inditex.supplier.domain.exception.SupplierRecordNotFoundException;
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

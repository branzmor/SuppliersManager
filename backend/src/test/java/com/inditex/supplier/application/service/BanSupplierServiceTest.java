package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import com.inditex.supplier.domain.exception.SupplierNotBannableException;
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
 * Not found → {@code SupplierRecordNotFoundException}; happy path ON_PROBATION → BANNED, saved;
 * ACTIVE (and any other status) → {@code SupplierNotBannableException} — the ACTIVE case is the
 * important regression test for the confirmed "ban only from ON_PROBATION" decision.
 */
@ExtendWith(MockitoExtension.class)
class BanSupplierServiceTest {

    private static final Duns DUNS = new Duns(123_456_789);

    @Mock
    private SupplierRepositoryPort supplierRepositoryPort;

    private BanSupplierService service;

    @BeforeEach
    void setUp() {
        service = new BanSupplierService(supplierRepositoryPort);
    }

    private static SupplierRecord recordIn(SupplierStatus status) {
        return SupplierRecord.reconstitute(DUNS, "Zippers & Buttons", new CountryCode("ES"),
                new AnnualTurnover(2_000_000L), status, SustainabilityRating.D);
    }

    @Test
    void throwsNotFoundWhenNoRecordExists() {
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.ban(DUNS.value())).isInstanceOf(SupplierRecordNotFoundException.class);
    }

    @Test
    void bansSupplierOnProbationSuccessfully() {
        SupplierRecord record = recordIn(SupplierStatus.ON_PROBATION);
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(record));

        service.ban(DUNS.value());

        assertThat(record.status()).isEqualTo(SupplierStatus.BANNED);
        verify(supplierRepositoryPort).save(record);
    }

    @Test
    void throwsWhenStatusIsActiveNotOnProbation() {
        SupplierRecord record = recordIn(SupplierStatus.ACTIVE);
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.ban(DUNS.value())).isInstanceOf(SupplierNotBannableException.class);
        assertThat(record.status()).isEqualTo(SupplierStatus.ACTIVE);
    }
}

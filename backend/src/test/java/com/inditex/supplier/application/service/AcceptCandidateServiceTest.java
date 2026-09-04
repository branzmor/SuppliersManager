package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.out.CountryCheckPort;
import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import com.inditex.supplier.domain.exception.CandidateNotAcceptableException;
import com.inditex.supplier.domain.exception.CountryCheckUnavailableException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mocks both ports:
 * <ul>
 *   <li>No record for DUNS → {@code SupplierRecordNotFoundException}.</li>
 *   <li>Happy path: country not banned, rating A/B → {@code ACTIVE}; C/D/E → {@code ON_PROBATION}; saved.</li>
 *   <li>{@code CountryCheckPort} throws {@code CountryCheckUnavailableException} →
 *       {@code CandidateNotAcceptableException} (fail-safe, README §4) — critical test, don't skip.</li>
 *   <li>Country banned (port returns true) → {@code CandidateNotAcceptableException}.</li>
 *   <li>Turnover below 1M → {@code CandidateNotAcceptableException}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AcceptCandidateServiceTest {

    private static final Duns DUNS = new Duns(123_456_789);
    private static final CountryCode COUNTRY = new CountryCode("ES");

    @Mock
    private SupplierRepositoryPort supplierRepositoryPort;

    @Mock
    private CountryCheckPort countryCheckPort;

    private AcceptCandidateService service;

    @BeforeEach
    void setUp() {
        service = new AcceptCandidateService(supplierRepositoryPort, countryCheckPort);
    }

    private static SupplierRecord candidate(long turnover) {
        return SupplierRecord.apply(DUNS, "Zippers & Buttons", COUNTRY, new AnnualTurnover(turnover));
    }

    @Test
    void throwsNotFoundWhenNoRecordExists() {
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.accept(DUNS.value(), SustainabilityRating.A))
                .isInstanceOf(SupplierRecordNotFoundException.class);
    }

    @Test
    void acceptsWithGoodRatingBecomesActive() {
        SupplierRecord record = candidate(2_000_000L);
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(record));
        when(countryCheckPort.isBanned(COUNTRY)).thenReturn(false);

        service.accept(DUNS.value(), SustainabilityRating.A);

        assertThat(record.status()).isEqualTo(SupplierStatus.ACTIVE);
        verify(supplierRepositoryPort).save(record);
    }

    @Test
    void failSafeWhenCountryCheckUnavailable() {
        SupplierRecord record = candidate(2_000_000L);
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(record));
        when(countryCheckPort.isBanned(COUNTRY)).thenThrow(new CountryCheckUnavailableException("down", null));

        assertThatThrownBy(() -> service.accept(DUNS.value(), SustainabilityRating.A))
                .isInstanceOf(CandidateNotAcceptableException.class);
        assertThat(record.status()).isEqualTo(SupplierStatus.CANDIDATE);
        verify(supplierRepositoryPort, never()).save(any());
    }

    @Test
    void rejectsWhenCountryBanned() {
        SupplierRecord record = candidate(2_000_000L);
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(record));
        when(countryCheckPort.isBanned(COUNTRY)).thenReturn(true);

        assertThatThrownBy(() -> service.accept(DUNS.value(), SustainabilityRating.A))
                .isInstanceOf(CandidateNotAcceptableException.class);
        verify(supplierRepositoryPort, never()).save(any());
    }

    @Test
    void rejectsWhenTurnoverBelowMinimum() {
        SupplierRecord record = candidate(500_000L);
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(record));
        when(countryCheckPort.isBanned(COUNTRY)).thenReturn(false);

        assertThatThrownBy(() -> service.accept(DUNS.value(), SustainabilityRating.A))
                .isInstanceOf(CandidateNotAcceptableException.class);
        verify(supplierRepositoryPort, never()).save(any());
    }
}

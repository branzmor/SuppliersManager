package com.supplier.management.application.service;

import com.supplier.management.application.port.in.RegisterCandidateUseCase.RegisterCandidateCommand;
import com.supplier.management.application.port.out.SupplierRepositoryPort;
import com.supplier.management.domain.exception.CandidateAlreadyExistsException;
import com.supplier.management.domain.exception.SupplierBannedException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mocks {@link SupplierRepositoryPort}:
 * <ul>
 *   <li>No existing record for DUNS → saves a new {@code CANDIDATE} record.</li>
 *   <li>Existing record in {@code BANNED} → {@code SupplierBannedException}, no save.</li>
 *   <li>Existing record in {@code REFUSED} → reapplies (updates fields, clears rating, back to
 *       {@code CANDIDATE}) and saves.</li>
 *   <li>Existing record in any other status → {@code CandidateAlreadyExistsException}, no save.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class RegisterCandidateServiceTest {

    private static final Duns DUNS = new Duns(123_456_789);
    private static final CountryCode COUNTRY = new CountryCode("ES");
    private static final AnnualTurnover TURNOVER = new AnnualTurnover(2_000_000L);

    @Mock
    private SupplierRepositoryPort supplierRepositoryPort;

    private RegisterCandidateService service;

    @BeforeEach
    void setUp() {
        service = new RegisterCandidateService(supplierRepositoryPort);
    }

    @Test
    void registersNewCandidateWhenNoExistingRecord() {
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.empty());
        when(supplierRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SupplierRecord result = service.register(
                new RegisterCandidateCommand(DUNS.value(), "Zippers & Buttons", COUNTRY.isoCode(), TURNOVER.value()));

        assertThat(result.status()).isEqualTo(SupplierStatus.CANDIDATE);
        assertThat(result.duns()).isEqualTo(DUNS);
        verify(supplierRepositoryPort).save(any());
    }

    @Test
    void throwsSupplierBannedWhenExistingRecordIsBanned() {
        SupplierRecord banned = SupplierRecord.reconstitute(DUNS, "Zippers & Buttons", COUNTRY, TURNOVER,
                SupplierStatus.BANNED, null);
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(banned));

        assertThatThrownBy(() -> service.register(
                new RegisterCandidateCommand(DUNS.value(), "Zippers & Buttons", COUNTRY.isoCode(), TURNOVER.value())))
                .isInstanceOf(SupplierBannedException.class);
        verify(supplierRepositoryPort, never()).save(any());
    }

    @Test
    void throwsCandidateAlreadyExistsForAnyOtherExistingStatus() {
        SupplierRecord active = SupplierRecord.reconstitute(DUNS, "Zippers & Buttons", COUNTRY, TURNOVER,
                SupplierStatus.ACTIVE, SustainabilityRating.A);
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.register(
                new RegisterCandidateCommand(DUNS.value(), "Zippers & Buttons", COUNTRY.isoCode(), TURNOVER.value())))
                .isInstanceOf(CandidateAlreadyExistsException.class);
        verify(supplierRepositoryPort, never()).save(any());
    }

    @Test
    void reappliesWhenExistingRecordIsRefused() {
        SupplierRecord refused = SupplierRecord.reconstitute(DUNS, "Old Name", COUNTRY, TURNOVER,
                SupplierStatus.REFUSED, null);
        when(supplierRepositoryPort.findByDuns(DUNS)).thenReturn(Optional.of(refused));
        when(supplierRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CountryCode newCountry = new CountryCode("FR");
        AnnualTurnover newTurnover = new AnnualTurnover(5_000_000L);
        SupplierRecord result = service.register(
                new RegisterCandidateCommand(DUNS.value(), "New Name", newCountry.isoCode(), newTurnover.value()));

        assertThat(result.status()).isEqualTo(SupplierStatus.CANDIDATE);
        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.country()).isEqualTo(newCountry);
        assertThat(result.annualTurnover()).isEqualTo(newTurnover);
        assertThat(result.sustainabilityRating()).isNull();
        verify(supplierRepositoryPort).save(refused);
    }
}

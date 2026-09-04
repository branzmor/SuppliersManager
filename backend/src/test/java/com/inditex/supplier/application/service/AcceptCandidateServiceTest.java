package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.out.CountryCheckPort;
import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TODO coverage (mock both ports):
 * <ul>
 *   <li>No record for DUNS → {@code SupplierRecordNotFoundException}.</li>
 *   <li>Happy path: country not banned, rating A/B → {@code ACTIVE}; C/D/E → {@code ON_PROBATION}; saved.</li>
 *   <li>{@code CountryCheckPort} throws {@code CountryCheckUnavailableException} →
 *       {@code CandidateNotAcceptableException} (fail-safe, README §4) — critical test, don't skip.</li>
 *   <li>Country banned (port returns true) → {@code CandidateNotAcceptableException}.</li>
 *   <li>Turnover below 1M → {@code CandidateNotAcceptableException}, country check not even
 *       necessarily invoked (decide short-circuit order and test it).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AcceptCandidateServiceTest {

    @Mock
    private SupplierRepositoryPort supplierRepositoryPort;

    @Mock
    private CountryCheckPort countryCheckPort;

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void throwsNotFoundWhenNoRecordExists() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void acceptsWithGoodRatingBecomesActive() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void failSafeWhenCountryCheckUnavailable() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void rejectsWhenCountryBanned() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void rejectsWhenTurnoverBelowMinimum() {
    }
}

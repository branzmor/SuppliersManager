package com.supplier.management.application.service;

import com.supplier.management.application.port.in.AcceptCandidateUseCase;
import com.supplier.management.application.port.out.CountryCheckPort;
import com.supplier.management.application.port.out.SupplierRepositoryPort;
import com.supplier.management.domain.exception.CountryCheckUnavailableException;
import com.supplier.management.domain.exception.SupplierRecordNotFoundException;
import com.supplier.management.domain.model.Duns;
import com.supplier.management.domain.model.SupplierRecord;
import com.supplier.management.domain.model.SustainabilityRating;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements {@link AcceptCandidateUseCase}:
 * <ol>
 *   <li>Look up by DUNS, throw {@code SupplierRecordNotFoundException} if absent.</li>
 *   <li>Resolve {@code CountryCheckPort#isBanned} for the record's country; on
 *       {@code CountryCheckUnavailableException}, treat as banned (fail-safe, README §4).</li>
 *   <li>Delegate to {@code SupplierRecord#accept(rating, countryBanned)}.</li>
 *   <li>Save the updated aggregate.</li>
 * </ol>
 */
@Service
public class AcceptCandidateService implements AcceptCandidateUseCase {

    private final SupplierRepositoryPort supplierRepositoryPort;
    private final CountryCheckPort countryCheckPort;

    public AcceptCandidateService(SupplierRepositoryPort supplierRepositoryPort, CountryCheckPort countryCheckPort) {
        this.supplierRepositoryPort = supplierRepositoryPort;
        this.countryCheckPort = countryCheckPort;
    }

    @Override
    @Transactional
    public void accept(int duns, SustainabilityRating rating) {
        Duns candidateDuns = new Duns(duns);
        SupplierRecord record = supplierRepositoryPort.findByDuns(candidateDuns)
                .orElseThrow(() -> new SupplierRecordNotFoundException(candidateDuns));

        boolean countryBanned;
        try {
            countryBanned = countryCheckPort.isBanned(record.country());
        } catch (CountryCheckUnavailableException e) {
            // Fail-safe (README §4): never assume a country is not banned when the check fails.
            countryBanned = true;
        }

        record.accept(rating, countryBanned);
        supplierRepositoryPort.save(record);
    }
}

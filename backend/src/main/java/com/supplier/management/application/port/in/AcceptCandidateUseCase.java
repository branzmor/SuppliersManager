package com.supplier.management.application.port.in;

import com.supplier.management.domain.exception.CandidateNotAcceptableException;
import com.supplier.management.domain.exception.SupplierRecordNotFoundException;
import com.supplier.management.domain.model.SustainabilityRating;

/**
 * Use case for {@code POST /candidates/{duns}/accept} (OpenAPI {@code operationId: acceptCandidate}).
 *
 * <p>Resolves the country-check guard (via {@code CountryCheckPort}, fail-safe on
 * {@code CountryCheckUnavailableException}) before delegating to
 * {@code SupplierRecord#accept}. Returns nothing on success (HTTP 204).
 */
public interface AcceptCandidateUseCase {

    /**
     * @throws SupplierRecordNotFoundException if no record exists for the DUNS (controller maps
     *     to 404)
     * @throws CandidateNotAcceptableException if {@code status != CANDIDATE}, the country is
     *     banned/unavailable, or {@code annualTurnover < 1,000,000}
     */
    void accept(int duns, SustainabilityRating rating);
}

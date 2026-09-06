package com.inditex.supplier.application.port.in;

import com.inditex.supplier.domain.exception.CandidateAlreadyExistsException;
import com.inditex.supplier.domain.exception.SupplierBannedException;
import com.inditex.supplier.domain.model.SupplierRecord;

/**
 * Use case for {@code POST /candidates} (OpenAPI {@code operationId: addCandidate}).
 */
public interface RegisterCandidateUseCase {

    /**
     * If a record already exists for the DUNS in {@code REFUSED} status, this reapplies (updates
     * the record in place with the newly submitted data and moves it back to {@code CANDIDATE})
     * instead of failing — see {@code SupplierRecord#reapply} and {@code SupplierStatus} javadoc.
     *
     * @throws CandidateAlreadyExistsException if a record already exists for the DUNS in any
     *     status other than {@code REFUSED} or {@code BANNED}
     * @throws SupplierBannedException if a record already exists for the DUNS in {@code BANNED}
     *     status
     */
    SupplierRecord register(RegisterCandidateCommand command);

    record RegisterCandidateCommand(int duns, String name, String country, long annualTurnover) {
    }
}

package com.inditex.supplier.application.port.in;

import com.inditex.supplier.domain.exception.CandidateAlreadyExistsException;
import com.inditex.supplier.domain.exception.SupplierBannedException;
import com.inditex.supplier.domain.model.SupplierRecord;

/**
 * Use case for {@code POST /candidates} (OpenAPI {@code operationId: addCandidate}).
 */
public interface RegisterCandidateUseCase {

    /**
     * @throws CandidateAlreadyExistsException if a record already exists for the DUNS in any
     *     non-BANNED status (including {@code REFUSED} — no reapply, see
     *     {@code SupplierStatus} javadoc)
     * @throws SupplierBannedException if a record already exists for the DUNS in {@code BANNED}
     *     status
     */
    SupplierRecord register(RegisterCandidateCommand command);

    record RegisterCandidateCommand(int duns, String name, String country, long annualTurnover) {
    }
}

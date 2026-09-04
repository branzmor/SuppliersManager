package com.inditex.supplier.application.port.in;

import com.inditex.supplier.domain.exception.CandidateNotRefusableException;
import com.inditex.supplier.domain.exception.SupplierRecordNotFoundException;

/**
 * Use case for {@code POST /candidates/{duns}/refuse} (OpenAPI {@code operationId: refuseCandidate}).
 */
public interface RefuseCandidateUseCase {

    /**
     * @throws SupplierRecordNotFoundException if no record exists for the DUNS (controller maps
     *     to 404)
     * @throws CandidateNotRefusableException if {@code status != CANDIDATE}
     */
    void refuse(int duns);
}

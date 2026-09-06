package com.inditex.supplier.application.port.in;

import com.inditex.supplier.domain.model.SupplierRecord;

import java.util.Optional;

/**
 * Use case for {@code GET /candidates/{duns}} (OpenAPI {@code operationId: getCandidate}).
 *
 * <p>Per {@code SOLUTION.md} §"API pública vs. estado interno": returns a value only if the
 * record's internal status is {@code CANDIDATE} or {@code REFUSED}; any other status (including
 * a record that has become a supplier) is treated as not found by the caller (controller returns
 * 404).
 */
public interface GetCandidateUseCase {

    Optional<SupplierRecord> getByDuns(int duns);
}

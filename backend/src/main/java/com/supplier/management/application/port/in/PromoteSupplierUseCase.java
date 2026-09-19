package com.supplier.management.application.port.in;

import com.supplier.management.domain.exception.SupplierNotPromotableException;

/**
 * Extension use case (see {@code SupplierRecord#promote} and {@code SOLUTION.md}): not wired to
 * any controller yet, since the OpenAPI contract has no corresponding endpoint. Added as a stub
 * per project decision to keep the diagram's {@code Promote} edge (On Probation → Active)
 * represented in the codebase for discussion, without inventing an unspecified public endpoint.
 */
public interface PromoteSupplierUseCase {

    /**
     * @throws SupplierNotPromotableException if {@code status != ON_PROBATION}
     */
    void promote(int duns);
}

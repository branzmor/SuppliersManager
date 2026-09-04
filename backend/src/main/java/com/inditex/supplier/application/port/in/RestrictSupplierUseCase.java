package com.inditex.supplier.application.port.in;

import com.inditex.supplier.domain.exception.SupplierNotRestrictableException;

/**
 * Extension use case (see {@code SupplierRecord#restrict} and {@code SOLUTION.md}): not wired to
 * any controller yet, since the OpenAPI contract has no corresponding endpoint. Added as a stub
 * per project decision to keep the diagram's {@code Restrict} edge (Active → On Probation)
 * represented in the codebase for discussion, without inventing an unspecified public endpoint.
 */
public interface RestrictSupplierUseCase {

    /**
     * @throws SupplierNotRestrictableException if {@code status != ACTIVE}
     */
    void restrict(int duns);
}

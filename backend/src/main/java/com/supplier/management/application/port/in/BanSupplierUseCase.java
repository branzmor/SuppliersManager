package com.supplier.management.application.port.in;

import com.supplier.management.domain.exception.SupplierNotBannableException;
import com.supplier.management.domain.exception.SupplierRecordNotFoundException;

/**
 * Use case for {@code POST /suppliers/{duns}/ban} (OpenAPI {@code operationId: banSupplier}).
 */
public interface BanSupplierUseCase {

    /**
     * @throws SupplierRecordNotFoundException if no record exists for the DUNS (controller maps
     *     to 404)
     * @throws SupplierNotBannableException if {@code status != ON_PROBATION} (confirmed decision:
     *     an {@code ACTIVE} supplier cannot be banned directly)
     */
    void ban(int duns);
}

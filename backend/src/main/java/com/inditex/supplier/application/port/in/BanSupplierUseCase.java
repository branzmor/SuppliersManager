package com.inditex.supplier.application.port.in;

import com.inditex.supplier.domain.exception.SupplierNotBannableException;
import com.inditex.supplier.domain.exception.SupplierRecordNotFoundException;

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

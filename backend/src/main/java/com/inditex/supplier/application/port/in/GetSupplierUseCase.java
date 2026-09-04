package com.inditex.supplier.application.port.in;

import com.inditex.supplier.domain.model.SupplierRecord;

import java.util.Optional;

/**
 * Use case for {@code GET /suppliers/{duns}} (OpenAPI {@code operationId: getSupplier}).
 *
 * <p>Per {@code SOLUTION.md} §"API pública vs. estado interno": returns a value only if the
 * record's internal status is {@code ACTIVE}, {@code ON_PROBATION}, or {@code BANNED}. The
 * internal→external status mapping ({@code ACTIVE}/{@code ON_PROBATION} → {@code Active},
 * {@code BANNED} → {@code Disqualified}) happens in {@code infrastructure.web.mapper}, not here.
 */
public interface GetSupplierUseCase {

    Optional<SupplierRecord> getByDuns(int duns);
}

package com.inditex.supplier.domain.exception;

import com.inditex.supplier.domain.model.Duns;

/**
 * Thrown when an operation targets a DUNS for which no {@code SupplierRecord} exists at all.
 *
 * <p>Not part of the 6-exception table in the original spec (that table only covers the 409
 * conflicts) — added because {@code POST /candidates/{duns}/accept},
 * {@code POST /candidates/{duns}/refuse} and {@code POST /suppliers/{duns}/ban} all declare a
 * {@code 404} response in the OpenAPI contract for "not found". Documented as an addition in
 * {@code SOLUTION.md}.
 *
 * <p>Mapped by {@code GlobalExceptionHandler} to {@code 404 Not Found} (no body, per the
 * OpenAPI {@code NotFound} response which declares no content schema).
 */
public class SupplierRecordNotFoundException extends RuntimeException {

    public SupplierRecordNotFoundException(Duns duns) {
        super("No record found for DUNS " + duns.value());
    }
}

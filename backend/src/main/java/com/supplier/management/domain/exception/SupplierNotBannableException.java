package com.supplier.management.domain.exception;

import com.supplier.management.domain.model.Duns;

/**
 * Thrown by {@code SupplierRecord#ban} when {@code status != ON_PROBATION}.
 *
 * <p>Confirmed project decision: {@code ban()} is only valid from {@code ON_PROBATION}, not from
 * {@code ACTIVE} — an active supplier must first be restricted (see the diagram-only,
 * not-yet-wired {@code restrict()} extension) before it can be banned.
 *
 * <p>Mapped by {@code GlobalExceptionHandler} to {@code 409 Conflict}, body
 * {@code {"info": "Supplier can not be banned"}}.
 */
public class SupplierNotBannableException extends RuntimeException {

    public SupplierNotBannableException(Duns duns) {
        super("Supplier can not be banned for DUNS " + duns.value());
    }
}

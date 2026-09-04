package com.inditex.supplier.domain.exception;

import com.inditex.supplier.domain.model.Duns;

/**
 * Thrown when {@code POST /candidates} targets a DUNS whose {@code SupplierRecord} is in
 * {@code BANNED} status. {@code BANNED} is terminal: a banned supplier can never become a
 * candidate or supplier again for that DUNS.
 *
 * <p>Mapped by {@code GlobalExceptionHandler} to {@code 409 Conflict}, body
 * {@code {"info": "Supplier banned"}}.
 */
public class SupplierBannedException extends RuntimeException {

    public SupplierBannedException(Duns duns) {
        super("Supplier banned for DUNS " + duns.value());
    }
}

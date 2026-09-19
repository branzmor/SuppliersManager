package com.supplier.management.domain.exception;

import com.supplier.management.domain.model.Duns;

/**
 * Extension exception (see {@code SupplierRecord#restrict} and {@code SOLUTION.md}): thrown when
 * {@code status != ACTIVE}. Not part of the OpenAPI-defined contract yet — no controller maps
 * this to an HTTP response today. Kept here so the domain stub compiles and the intent is
 * documented for when/if the contract is extended with the diagram's {@code Restrict} edge.
 */
public class SupplierNotRestrictableException extends RuntimeException {

    public SupplierNotRestrictableException(Duns duns) {
        super("Supplier can not be restricted for DUNS " + duns.value());
    }
}

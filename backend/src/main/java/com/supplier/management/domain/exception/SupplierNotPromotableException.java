package com.supplier.management.domain.exception;

import com.supplier.management.domain.model.Duns;

/**
 * Extension exception (see {@code SupplierRecord#promote} and {@code SOLUTION.md}): thrown when
 * {@code status != ON_PROBATION}. Not part of the OpenAPI-defined contract yet — no controller
 * maps this to an HTTP response today. Kept here so the domain stub compiles and the intent is
 * documented for when/if the contract is extended with the diagram's {@code Promote} edge.
 */
public class SupplierNotPromotableException extends RuntimeException {

    public SupplierNotPromotableException(Duns duns) {
        super("Supplier can not be promoted for DUNS " + duns.value());
    }
}

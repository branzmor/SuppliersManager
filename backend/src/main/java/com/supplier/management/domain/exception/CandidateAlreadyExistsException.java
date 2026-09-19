package com.supplier.management.domain.exception;

import com.supplier.management.domain.model.Duns;

/**
 * Thrown when {@code POST /candidates} targets a DUNS for which a {@code SupplierRecord} already
 * exists in a status other than {@code REFUSED} or {@code BANNED} (i.e. {@code CANDIDATE},
 * {@code ACTIVE}, or {@code ON_PROBATION}). A {@code REFUSED} record instead triggers
 * {@code SupplierRecord#reapply} (see {@code SupplierStatus} javadoc); a {@code BANNED} record
 * triggers {@link SupplierBannedException}.
 *
 * <p>Mapped by {@code GlobalExceptionHandler} to {@code 409 Conflict}, body
 * {@code {"info": "Candidate already exists"}}.
 */
public class CandidateAlreadyExistsException extends RuntimeException {

    public CandidateAlreadyExistsException(Duns duns) {
        super("Candidate already exists for DUNS " + duns.value());
    }
}

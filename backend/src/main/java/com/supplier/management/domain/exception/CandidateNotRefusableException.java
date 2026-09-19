package com.supplier.management.domain.exception;

import com.supplier.management.domain.model.Duns;

/**
 * Thrown by {@code SupplierRecord#refuse} when {@code status != CANDIDATE}.
 *
 * <p>Mapped by {@code GlobalExceptionHandler} to {@code 409 Conflict}, body
 * {@code {"info": "Candidate can not be refused"}}.
 */
public class CandidateNotRefusableException extends RuntimeException {

    public CandidateNotRefusableException(Duns duns) {
        super("Candidate can not be refused for DUNS " + duns.value());
    }
}

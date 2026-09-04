package com.inditex.supplier.domain.exception;

import com.inditex.supplier.domain.model.Duns;

/**
 * Thrown when {@code POST /candidates} targets a DUNS for which a {@code SupplierRecord} already
 * exists in any non-{@code BANNED} status (i.e. {@code CANDIDATE}, {@code ACTIVE},
 * {@code ON_PROBATION}, or {@code REFUSED} — the latter per the project's no-reapply decision,
 * see {@code SupplierStatus} javadoc).
 *
 * <p>Mapped by {@code GlobalExceptionHandler} to {@code 409 Conflict}, body
 * {@code {"info": "Candidate already exists"}}.
 */
public class CandidateAlreadyExistsException extends RuntimeException {

    public CandidateAlreadyExistsException(Duns duns) {
        super("Candidate already exists for DUNS " + duns.value());
    }
}

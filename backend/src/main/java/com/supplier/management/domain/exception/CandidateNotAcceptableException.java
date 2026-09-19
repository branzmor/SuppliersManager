package com.supplier.management.domain.exception;

import com.supplier.management.domain.model.Duns;

/**
 * Thrown by {@code SupplierRecord#accept} when any acceptance guard fails:
 * <ul>
 *   <li>{@code status != CANDIDATE}</li>
 *   <li>the candidate's country is on the non-approved countries list</li>
 *   <li>{@code annualTurnover < 1,000,000}</li>
 * </ul>
 *
 * <p>Also reused (per README §4 "fail-safe") by
 * {@code CountryCheckUnavailableException}-triggering paths: when the circuit breaker for the
 * country service is open or the call fails, the acceptance must be refused rather than assume
 * the country is not banned — surfaced to the client with this same message.
 *
 * <p>Mapped by {@code GlobalExceptionHandler} to {@code 409 Conflict}, body
 * {@code {"info": "Candidate can not be accepted"}}.
 */
public class CandidateNotAcceptableException extends RuntimeException {

    public CandidateNotAcceptableException(Duns duns, String reason) {
        super("Candidate can not be accepted for DUNS " + duns.value() + ": " + reason);
    }
}

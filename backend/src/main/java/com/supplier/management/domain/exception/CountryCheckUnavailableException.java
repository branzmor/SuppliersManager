package com.supplier.management.domain.exception;

/**
 * Thrown by {@code infrastructure.external.country.CountryCheckAdapter} when the external
 * country service cannot be reached or the resilience4j Circuit Breaker is open.
 *
 * <p><strong>Fail-safe rule:</strong> never assume a country is not banned when this happens.
 * {@code application.service} callers must catch this and treat it exactly like "country is
 * banned", surfacing {@code CandidateNotAcceptableException} to the client — reusing its
 * {@code 409 "Candidate can not be accepted"} message rather than a distinct one, per
 * README §4.
 */
public class CountryCheckUnavailableException extends RuntimeException {

    public CountryCheckUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

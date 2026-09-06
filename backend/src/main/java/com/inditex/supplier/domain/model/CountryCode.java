package com.inditex.supplier.domain.model;

/**
 * ISO 3166-1 alpha-2 country code where the candidate/supplier is headquartered.
 *
 * <p>Used by {@code application.port.out.CountryCheckPort} to determine whether the country is
 * on the non-approved countries list (README §1, acceptance rules).
 *
 * @param isoCode the 2-letter uppercase ISO code, e.g. {@code ES}, {@code FR}, {@code PT}
 */
public record CountryCode(String isoCode) {

    /**
     * Design decision: strict validation, no normalization. A lowercase or mixed-case code (e.g.
     * {@code "es"}) is rejected rather than silently upper-cased — the OpenAPI examples are all
     * uppercase and callers (web DTOs, JPA entities) are expected to pass through exactly what
     * they received rather than this value object papering over inconsistent casing upstream.
     */
    public CountryCode {
        if (isoCode == null || !isoCode.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("isoCode must be exactly 2 uppercase letters, got " + isoCode);
        }
    }
}

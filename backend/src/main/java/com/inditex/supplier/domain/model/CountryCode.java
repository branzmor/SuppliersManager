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

    public CountryCode {
        // TODO: validate isoCode has length 2 and is uppercase alphabetic, throw IllegalArgumentException otherwise.
    }
}

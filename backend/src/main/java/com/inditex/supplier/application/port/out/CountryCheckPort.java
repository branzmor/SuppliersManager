package com.inditex.supplier.application.port.out;

import com.inditex.supplier.domain.exception.CountryCheckUnavailableException;
import com.inditex.supplier.domain.model.CountryCode;

/**
 * Driven port for checking whether a country is on the non-approved countries list.
 *
 * <p>Implemented by {@code infrastructure.external.country.CountryCheckAdapter}, which calls the
 * external country service (contract:
 * {@code wiki/itx-iop_tech-supplier_flow-country-openapi3_1.yaml}) wrapped in a resilience4j
 * Circuit Breaker.
 */
public interface CountryCheckPort {

    /**
     * @param country the country to check
     * @return true if the country is banned (non-approved)
     * @throws CountryCheckUnavailableException if the external service call fails or the circuit
     *     breaker is open. Callers must treat this fail-safe, i.e. as if the country were banned —
     *     never assume approval when the check could not be completed.
     */
    boolean isBanned(CountryCode country);
}

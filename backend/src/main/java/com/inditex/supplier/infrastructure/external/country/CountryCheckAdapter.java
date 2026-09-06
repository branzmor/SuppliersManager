package com.inditex.supplier.infrastructure.external.country;

import com.inditex.supplier.application.port.out.CountryCheckPort;
import com.inditex.supplier.domain.exception.CountryCheckUnavailableException;
import com.inditex.supplier.domain.model.CountryCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;

/**
 * Implements {@link CountryCheckPort}, wrapping {@link CountryClient} calls in a resilience4j
 * Circuit Breaker (instance name {@code countryService}, configured in {@code application.yml}
 * under {@code resilience4j.circuitbreaker.instances.countryService}).
 *
 * <p><strong>Fail-safe contract:</strong> both the circuit-open fallback and any exception from
 * {@link CountryClient} must surface as {@link CountryCheckUnavailableException} — never silently
 * return {@code false} (not banned). See README §4 and {@code SupplierRecord} javadoc.
 */
@Component
public class CountryCheckAdapter implements CountryCheckPort {

    private final CountryClient countryClient;

    public CountryCheckAdapter(CountryClient countryClient) {
        this.countryClient = countryClient;
    }

    @Override
    @CircuitBreaker(name = "countryService", fallbackMethod = "fallbackIsBanned")
    public boolean isBanned(CountryCode country) {
        return countryClient.getCountry(country.isoCode()).isBanned();
    }

    /**
     * resilience4j fallback — invoked when the circuit is open or {@link #isBanned} throws.
     * Must always throw {@link CountryCheckUnavailableException}, never return a boolean, so the
     * fail-safe rule cannot be accidentally bypassed by a future change to this method's body.
     */
    private boolean fallbackIsBanned(CountryCode country, Throwable throwable) {
        throw new CountryCheckUnavailableException(
                "Country check unavailable for " + country.isoCode(), throwable);
    }
}

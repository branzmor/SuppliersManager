package com.supplier.management.infrastructure.external.country;

import com.supplier.management.infrastructure.external.country.dto.CountryResponseDto;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin HTTP client for {@code GET /countries/{country}} on the external country service
 * (WireMock-mocked in {@code docker-compose.yml}, service {@code country-service}).
 *
 * <p>No resilience concerns here — the Circuit Breaker wraps calls to this client in
 * {@link CountryCheckAdapter}, keeping this class a plain HTTP transport concern.
 */
@Component
public class CountryClient {

    private final RestClient restClient;

    public CountryClient(RestClient.Builder restClientBuilder, Environment environment) {
        this.restClient = restClientBuilder.baseUrl(environment.getProperty("country-service.base-url")).build();
    }

    /**
     * @param isoCode 2-letter ISO 3166-1 alpha-2 country code
     * @return the country's ban status
     * @throws org.springframework.web.client.RestClientException on any transport/HTTP failure
     *     (including 404, which the country service's OpenAPI declares) — the caller
     *     ({@link CountryCheckAdapter}) is responsible for wrapping this into a domain-level
     *     {@code CountryCheckUnavailableException} when appropriate.
     */
    public CountryResponseDto getCountry(String isoCode) {
        return restClient.get()
                .uri("/countries/{country}", isoCode)
                .retrieve()
                .body(CountryResponseDto.class);
    }
}

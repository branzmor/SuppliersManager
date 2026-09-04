package com.inditex.supplier.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Provides the {@link RestClient.Builder} used by
 * {@code infrastructure.external.country.CountryClient}.
 *
 * <p>resilience4j itself is configured declaratively in {@code application.yml}
 * ({@code resilience4j.circuitbreaker.instances.countryService}) — no Java config needed for the
 * Circuit Breaker beyond the {@code @CircuitBreaker} annotation in {@code CountryCheckAdapter}
 * and the {@code spring-boot-starter-aop} dependency that makes the annotation's proxying work.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}

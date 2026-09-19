package com.supplier.management.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Provides the {@link RestClient.Builder} used by
 * {@code infrastructure.external.country.CountryClient}, configured with real, externalizable
 * HTTP connect/read timeouts.
 *
 * <p>{@code RestClient}'s calls are synchronous — a {@code @TimeLimiter} annotation has no effect
 * on a blocking call like this one (it only wraps a {@code CompletableFuture}/{@code Supplier}
 * that runs asynchronously), so a slow-but-not-erroring country service would otherwise hang the
 * calling thread indefinitely regardless of what {@code resilience4j.timelimiter} said in
 * {@code application.yml}. The actual bound comes from
 * {@link SimpleClientHttpRequestFactory#setConnectTimeout} /
 * {@link SimpleClientHttpRequestFactory#setReadTimeout} instead, which enforce a real socket-level
 * timeout on the blocking call itself — that's also what turns a hung country service into a
 * {@code RestClientException}, which {@code CountryCheckAdapter}'s {@code @CircuitBreaker}
 * fallback then converts into the fail-safe {@code CountryCheckUnavailableException}.
 *
 * <p>resilience4j's Circuit Breaker itself is configured declaratively in {@code application.yml}
 * ({@code resilience4j.circuitbreaker.instances.countryService}) — no Java config needed beyond
 * the {@code @CircuitBreaker} annotation in {@code CountryCheckAdapter} and the
 * {@code spring-boot-starter-aop} dependency that makes the annotation's proxying work.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder(
            @Value("${country-service.connect-timeout-ms:1000}") int connectTimeoutMs,
            @Value("${country-service.read-timeout-ms:2000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return RestClient.builder().requestFactory(requestFactory);
    }
}

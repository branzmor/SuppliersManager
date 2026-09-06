package com.inditex.supplier.infrastructure.external.country;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.inditex.supplier.domain.exception.CountryCheckUnavailableException;
import com.inditex.supplier.domain.model.CountryCode;
import com.inditex.supplier.infrastructure.config.RestClientConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test against a real (embedded) WireMock server — exercises the actual
 * resilience4j Circuit Breaker AOP proxy, which a plain unit test instantiating
 * {@link CountryCheckAdapter} directly cannot do (the {@code @CircuitBreaker} annotation only
 * takes effect when the bean is proxied inside a Spring context with the resilience4j aspect
 * registered).
 *
 * <p>The context is deliberately scoped to just {@link CountryClient}/{@link CountryCheckAdapter}/
 * {@link RestClientConfig} (via explicit {@code classes}, not the full
 * {@code SupplierManagementApplication}) with {@code @EnableAutoConfiguration} added back only
 * for resilience4j's Circuit Breaker AOP — this test has nothing to do with persistence or the
 * web layer and would otherwise need a real database just to let the context start.
 */
@SpringBootTest(
        classes = {CountryClient.class, CountryCheckAdapter.class, RestClientConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                // Short on purpose: readTimeoutRespondsWithinBoundedTimeOnSlowCountryService below
                // stubs a deliberately slow WireMock response and asserts the call still returns
                // (via the read timeout, not by waiting the full delay) within a bounded time.
                "country-service.connect-timeout-ms=300",
                "country-service.read-timeout-ms=300"
        })
@EnableAutoConfiguration
class CountryCheckAdapterTest {

    private static final WireMockServer WIRE_MOCK = new WireMockServer(options().dynamicPort());

    @DynamicPropertySource
    static void countryServiceProperties(DynamicPropertyRegistry registry) {
        WIRE_MOCK.start();
        registry.add("country-service.base-url", WIRE_MOCK::baseUrl);
    }

    @AfterAll
    static void stopWireMock() {
        WIRE_MOCK.stop();
    }

    @Autowired
    private CountryCheckAdapter countryCheckAdapter;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetState() {
        WIRE_MOCK.resetAll();
        // The Circuit Breaker bean is a Spring-managed singleton shared across test methods (the
        // context is cached) — reset it so one test's failures never leak into the next.
        circuitBreakerRegistry.circuitBreaker("countryService").reset();
    }

    @Test
    void returnsFalseWhenCountryNotBanned() {
        WIRE_MOCK.stubFor(get(urlEqualTo("/countries/ES")).willReturn(okJson("{\"name\":\"ES\",\"isBanned\":false}")));

        assertThat(countryCheckAdapter.isBanned(new CountryCode("ES"))).isFalse();
    }

    @Test
    void returnsTrueWhenCountryBanned() {
        WIRE_MOCK.stubFor(get(urlEqualTo("/countries/PT")).willReturn(okJson("{\"name\":\"PT\",\"isBanned\":true}")));

        assertThat(countryCheckAdapter.isBanned(new CountryCode("PT"))).isTrue();
    }

    @Test
    void throwsUnavailableWhenCountryNotFound() {
        WIRE_MOCK.stubFor(get(urlEqualTo("/countries/XX")).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> countryCheckAdapter.isBanned(new CountryCode("XX")))
                .isInstanceOf(CountryCheckUnavailableException.class);
    }

    @Test
    void respondsWithinBoundedTimeAndFailsSafeOnSlowCountryService() {
        // No @CircuitBreaker state has tripped yet at this point (fresh reset in @BeforeEach), so
        // this call reaches WireMock for real and must be bounded by the read timeout itself
        // (300ms, see the class-level @SpringBootTest properties) - not by the resilience4j
        // TimeLimiter config removed from application.yml, which never applied to this
        // synchronous RestClient call in the first place (see RestClientConfig javadoc).
        WIRE_MOCK.stubFor(get(urlEqualTo("/countries/ES"))
                .willReturn(aResponse().withFixedDelay(5_000).withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"ES\",\"isBanned\":false}")));

        Instant start = Instant.now();
        assertThatThrownBy(() -> countryCheckAdapter.isBanned(new CountryCode("ES")))
                .isInstanceOf(CountryCheckUnavailableException.class);
        Duration elapsed = Duration.between(start, Instant.now());

        // Comfortably above the 300ms read timeout (allows for scheduling/JVM jitter) but far
        // below the 5s the stub would otherwise force the caller to wait for.
        assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    void circuitBreakerOpensAfterRepeatedFailures() {
        WIRE_MOCK.stubFor(get(urlPathMatching("/countries/.*")).willReturn(aResponse().withStatus(500)));

        // Default config (application.yml): minimum-number-of-calls=5, failure-rate-threshold=50%.
        // 5 consecutive failures -> 100% failure rate -> circuit trips to OPEN.
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> countryCheckAdapter.isBanned(new CountryCode("ES")))
                    .isInstanceOf(CountryCheckUnavailableException.class);
        }

        WIRE_MOCK.resetRequests();

        // The circuit is now open: this call must be short-circuited by resilience4j itself,
        // never reaching WireMock at all — proven by asserting zero requests were received.
        assertThatThrownBy(() -> countryCheckAdapter.isBanned(new CountryCode("ES")))
                .isInstanceOf(CountryCheckUnavailableException.class);
        WIRE_MOCK.verify(0, getRequestedFor(urlPathMatching("/countries/.*")));
    }
}

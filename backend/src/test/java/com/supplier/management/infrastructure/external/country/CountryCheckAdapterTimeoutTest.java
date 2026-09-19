package com.supplier.management.infrastructure.external.country;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.supplier.management.domain.exception.CountryCheckUnavailableException;
import com.supplier.management.domain.model.CountryCode;
import com.supplier.management.infrastructure.config.RestClientConfig;
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
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Dedicated, isolated context for the one scenario that legitimately needs a short, deliberate
 * HTTP read timeout: a country service that responds, but too slowly.
 *
 * <p>This used to be one test method inside {@code CountryCheckAdapterTest}, sharing that class's
 * single {@code @SpringBootTest} context and its class-level {@code country-service.read-timeout-ms}
 * override. Setting that override to 300ms so this test could stay fast made the whole context
 * flaky: the same 300ms bound also applied to the plain 200/404 functional tests in that class,
 * which stub a near-instant WireMock response with no artificial delay — under host load (parallel
 * compilation, other test suites, `docker pull` running concurrently) the wall-clock time for that
 * "instant" response occasionally crept past 300ms for reasons that have nothing to do with the
 * code under test, producing an intermittent {@code SocketTimeoutException} in tests that have
 * nothing to do with timeouts.
 *
 * <p>Splitting this scenario into its own context fixes that: the short timeout now only ever
 * applies here, against a WireMock stub that deliberately delays 5 seconds — 300ms of host jitter
 * added on top of a 300ms bound is still trivially far below the 5s the stub would otherwise force
 * the caller to wait for, so this test stays both fast and deterministic. The functional tests in
 * {@link CountryCheckAdapterTest} now run with the production-default 1000ms/2000ms timeouts from
 * {@code application.yml}, which is a "reasonable" bound for a near-instant stub even under load.
 */
@SpringBootTest(
        classes = {CountryClient.class, CountryCheckAdapter.class, RestClientConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "country-service.connect-timeout-ms=300",
                "country-service.read-timeout-ms=300"
        })
@EnableAutoConfiguration
class CountryCheckAdapterTimeoutTest {

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
        circuitBreakerRegistry.circuitBreaker("countryService").reset();
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
}

package com.inditex.supplier.infrastructure.external.country;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.inditex.supplier.domain.exception.CountryCheckUnavailableException;
import com.inditex.supplier.domain.model.CountryCode;
import com.inditex.supplier.infrastructure.config.RestClientConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the {@code countryService} Circuit Breaker actually recovers — {@code OPEN} ->
 * {@code HALF_OPEN} -> {@code CLOSED} — once the country service starts responding again, not just
 * that it opens under repeated failure (already covered by
 * {@link CountryCheckAdapterTest#circuitBreakerOpensAfterRepeatedFailures()}).
 *
 * <p>Own dedicated {@code @SpringBootTest} context (like {@link CountryCheckAdapterTimeoutTest})
 * so its short {@code wait-duration-in-open-state} override — needed to observe the recovery
 * without a long real wait — never leaks into the other two contexts, none of which need it.
 * resilience4j does not proactively poll for recovery
 * ({@code automatic-transition-from-open-to-half-open-enabled} defaults to {@code false}): the
 * first call attempted after the wait duration elapses is itself the one that flips the breaker to
 * {@code HALF_OPEN} and is let through as the trial call. Polling that call with Awaitility (bounded,
 * short interval, no {@code Thread.sleep}) is therefore both how the transition is triggered and how
 * the test waits for it deterministically.
 */
@SpringBootTest(
        classes = {CountryClient.class, CountryCheckAdapter.class, RestClientConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                // Only the wait duration and the half-open trial size are overridden here; the
                // rest of resilience4j.circuitbreaker.instances.countryService (minimum-number-of-
                // calls=5, failure-rate-threshold=50%, sliding-window-size=10) is inherited
                // unchanged from application.yml — this is a test-only context override, not a
                // change to the production default (10s) in application.yml itself.
                "resilience4j.circuitbreaker.instances.countryService.wait-duration-in-open-state=300ms",
                "resilience4j.circuitbreaker.instances.countryService.permitted-number-of-calls-in-half-open-state=1"
        })
@EnableAutoConfiguration
class CountryCheckAdapterCircuitBreakerRecoveryTest {

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

    private CircuitBreaker breaker;

    @BeforeEach
    void resetState() {
        WIRE_MOCK.resetAll();
        breaker = circuitBreakerRegistry.circuitBreaker("countryService");
        breaker.reset();
    }

    @Test
    void circuitBreakerTransitionsFromOpenToHalfOpenToClosedOnceTheCountryServiceRecovers() {
        List<CircuitBreaker.State> transitions = new CopyOnWriteArrayList<>();
        breaker.getEventPublisher().onStateTransition(event -> transitions.add(event.getStateTransition().getToState()));

        WIRE_MOCK.stubFor(get(urlPathMatching("/countries/.*")).willReturn(aResponse().withStatus(500)));
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> countryCheckAdapter.isBanned(new CountryCode("ES")))
                    .isInstanceOf(CountryCheckUnavailableException.class);
        }
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // The country service recovers. resilience4j has no way of knowing this yet — the breaker
        // stays OPEN, short-circuiting every call, until wait-duration-in-open-state elapses.
        WIRE_MOCK.stubFor(get(urlPathMatching("/countries/.*"))
                .willReturn(okJson("{\"name\":\"ES\",\"isBanned\":false}")));

        // Bounded polling, no Thread.sleep: each attempt either finds the breaker still OPEN (the
        // call is rejected, the assertion fails, Awaitility retries) or finds the wait duration has
        // elapsed (the call is let through as the HALF_OPEN trial, succeeds, and — with a single
        // permitted-number-of-calls-in-half-open-state and 0% failures in that trial — the breaker
        // closes immediately).
        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(50))
                .untilAsserted(() -> {
                    assertThatCode(() -> countryCheckAdapter.isBanned(new CountryCode("ES")))
                            .doesNotThrowAnyException();
                    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
                });

        int halfOpenIndex = transitions.indexOf(CircuitBreaker.State.HALF_OPEN);
        int closedIndex = transitions.lastIndexOf(CircuitBreaker.State.CLOSED);
        assertThat(halfOpenIndex).as("breaker must have passed through HALF_OPEN: %s", transitions).isNotEqualTo(-1);
        assertThat(closedIndex).as("breaker must have closed after HALF_OPEN: %s", transitions).isGreaterThan(halfOpenIndex);
    }
}

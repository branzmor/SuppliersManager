package com.inditex.supplier.infrastructure.external.country;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * TODO coverage — can reuse the WireMock mappings already provided in {@code ./wiremock}
 * ({@code country-banned.json}, {@code country-not-banned.json}, {@code country-not-found.json})
 * by pointing the client at a WireMock instance started for the test (Testcontainers WireMock
 * module, or the docker-compose {@code country-service} if running as an integration test):
 * <ul>
 *   <li>Country not banned → {@code isBanned} returns {@code false}.</li>
 *   <li>Country banned → {@code isBanned} returns {@code true}.</li>
 *   <li>Country not found (404) → treat as {@code CountryCheckUnavailableException} (fail-safe;
 *       decide and document whether "unknown country" should really fail-safe like a genuine
 *       outage, or deserves its own semantics — the current stub treats any non-2xx as
 *       unavailable).</li>
 *   <li><strong>Circuit breaker opens after repeated failures</strong> → subsequent calls fail
 *       fast with {@code CountryCheckUnavailableException} without hitting the network — this is
 *       the resilience4j requirement from README §2, don't skip it.</li>
 * </ul>
 */
class CountryCheckAdapterTest {

    @Test
    @Disabled("TODO: implement against WireMock mappings in ./wiremock - see class javadoc")
    void returnsFalseWhenCountryNotBanned() {
    }

    @Test
    @Disabled("TODO: implement against WireMock mappings in ./wiremock - see class javadoc")
    void returnsTrueWhenCountryBanned() {
    }

    @Test
    @Disabled("TODO: implement against WireMock mappings in ./wiremock - see class javadoc")
    void throwsUnavailableWhenCountryNotFound() {
    }

    @Test
    @Disabled("TODO: implement against WireMock mappings in ./wiremock - see class javadoc")
    void circuitBreakerOpensAfterRepeatedFailures() {
    }
}

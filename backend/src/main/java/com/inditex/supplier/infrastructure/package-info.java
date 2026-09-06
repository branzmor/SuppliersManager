/**
 * Infrastructure layer — adapters plugged into the hexagon's ports.
 *
 * <ul>
 *   <li>{@code web} — driving/primary adapters: REST controllers, request/response DTOs
 *       (mapped 1:1 to the OpenAPI schemas), web-to-domain mappers and the
 *       {@code @RestControllerAdvice} that translates domain exceptions into HTTP responses.</li>
 *   <li>{@code persistence} — driven/secondary adapter implementing
 *       {@code application.port.out.SupplierRepositoryPort} on top of Spring Data JPA. The JPA
 *       entity is intentionally separate from the domain aggregate.</li>
 *   <li>{@code external.country} — driven/secondary adapter implementing
 *       {@code application.port.out.CountryCheckPort}, calling the external country service
 *       through a resilience4j Circuit Breaker.</li>
 *   <li>{@code config} — framework wiring (resilience4j, etc.).</li>
 * </ul>
 *
 * <p>Nothing in {@code domain} or {@code application} may depend on this package; dependencies
 * only ever point inward.
 */
package com.inditex.supplier.infrastructure;

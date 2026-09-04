/**
 * Domain layer — the hexagon's core.
 *
 * <p>Contains the {@code SupplierRecord} aggregate (identity = DUNS), its value objects,
 * enums and business exceptions. This package models the single source of truth for the
 * supplier lifecycle described in the FSM (README §1 and the interview-clarified deviations
 * documented in {@code SOLUTION.md}).
 *
 * <p><strong>Rules enforced for this package:</strong>
 * <ul>
 *   <li>No dependency on Spring, JPA, Jackson or any other framework — plain Java 21 only.</li>
 *   <li>No {@code @Transactional}, no persistence annotations, no web annotations.</li>
 *   <li>Fully unit-testable without a Spring context.</li>
 *   <li>Invariants (state transitions, acceptance guards) live here, not in
 *       {@code application.service} nor in {@code infrastructure}.</li>
 * </ul>
 */
package com.inditex.supplier.domain;

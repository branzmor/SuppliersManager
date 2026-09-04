/**
 * Application layer — orchestrates the domain to fulfil each use case.
 *
 * <p>{@code port.in} declares one interface per use case (the driving/primary ports invoked by
 * {@code infrastructure.web.controller}). {@code port.out} declares the driven/secondary ports
 * the domain needs from the outside world (persistence, the external country service).
 * {@code service} contains the use case implementations.
 *
 * <p><strong>Rules enforced for this package:</strong>
 * <ul>
 *   <li>Transactional boundaries ({@code @Transactional}) belong here, never in
 *       {@code domain}.</li>
 *   <li>Services depend only on {@code domain} and on {@code application.port.out} interfaces —
 *       never on {@code infrastructure} classes directly (dependency inversion).</li>
 *   <li>Services translate domain outcomes into use case results; they do not know about HTTP,
 *       JPA entities or DTOs.</li>
 * </ul>
 */
package com.inditex.supplier.application;

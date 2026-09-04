package com.inditex.supplier.infrastructure.persistence.entity;

/**
 * Persistence-layer mirror of {@code domain.model.SupplierStatus}. Kept as a distinct type
 * (rather than reusing the domain enum directly with {@code @Enumerated}) so the domain module
 * never needs a JPA annotation and the stored representation can evolve independently of the
 * domain's vocabulary.
 */
public enum SupplierStatusJpa {
    CANDIDATE, ACTIVE, ON_PROBATION, REFUSED, BANNED
}

package com.supplier.management.infrastructure.persistence.entity;

/**
 * Persistence-layer mirror of {@code domain.model.SustainabilityRating}. {@code null} for records
 * still in {@code CANDIDATE} or {@code REFUSED} status (rating is only assigned on acceptance).
 */
public enum SustainabilityRatingJpa {
    A, B, C, D, E
}

package com.supplier.management.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code ratingConstant()} matches the README table (A=1, B=0.75, C=0.5, D=0.25, E=0.1);
 * {@code qualifiesForActive()} true only for A/B.
 */
class SustainabilityRatingTest {

    @Test
    void ratingConstantsMatchSpec() {
        assertThat(SustainabilityRating.A.ratingConstant()).isEqualTo(1.0);
        assertThat(SustainabilityRating.B.ratingConstant()).isEqualTo(0.75);
        assertThat(SustainabilityRating.C.ratingConstant()).isEqualTo(0.5);
        assertThat(SustainabilityRating.D.ratingConstant()).isEqualTo(0.25);
        assertThat(SustainabilityRating.E.ratingConstant()).isEqualTo(0.1);
    }

    @Test
    void qualifiesForActiveOnlyForAAndB() {
        assertThat(SustainabilityRating.A.qualifiesForActive()).isTrue();
        assertThat(SustainabilityRating.B.qualifiesForActive()).isTrue();
        assertThat(SustainabilityRating.C.qualifiesForActive()).isFalse();
        assertThat(SustainabilityRating.D.qualifiesForActive()).isFalse();
        assertThat(SustainabilityRating.E.qualifiesForActive()).isFalse();
    }
}

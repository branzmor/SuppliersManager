package com.inditex.supplier.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /candidates/{duns}/accept}, 1:1 with OpenAPI schema
 * {@code CandidateAccept}.
 */
public record CandidateAcceptRequestDto(
        @NotNull SustainabilityRatingDto sustainabilityRating
) {
}

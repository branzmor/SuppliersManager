package com.supplier.management.infrastructure.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /candidates}, 1:1 with OpenAPI schema {@code Candidate}.
 */
public record CandidateRequestDto(
        @NotNull @Min(0) Long annualTurnover,
        @NotBlank @Size(min = 2, max = 2) String country,
        @NotNull @Min(100_000_000) @Max(999_999_999) Integer duns,
        @NotBlank String name
) {
}

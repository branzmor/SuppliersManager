package com.supplier.management.infrastructure.web.dto;

/**
 * Response body for {@code POST /candidates} (201) and {@code GET /candidates/{duns}} (200),
 * 1:1 with OpenAPI schema {@code Candidate}.
 */
public record CandidateResponseDto(
        Long annualTurnover,
        String country,
        Integer duns,
        String name
) {
}

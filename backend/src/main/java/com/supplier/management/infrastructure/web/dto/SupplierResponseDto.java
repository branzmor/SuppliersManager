package com.supplier.management.infrastructure.web.dto;

/**
 * Response body for {@code GET /suppliers/{duns}}, 1:1 with OpenAPI schema {@code Supplier}
 * ({@code allOf: [Candidate, {status, sustainabilityRating}]}, flattened here).
 */
public record SupplierResponseDto(
        Long annualTurnover,
        String country,
        Integer duns,
        String name,
        SupplierStatusDto status,
        SustainabilityRatingDto sustainabilityRating
) {
}

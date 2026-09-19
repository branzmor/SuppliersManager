package com.supplier.management.infrastructure.web.dto;

/**
 * One item of {@code PotentialSuppliers.data}, 1:1 with OpenAPI schema {@code PotentialSupplier}
 * ({@code allOf: [Supplier, {score}]}, flattened here).
 */
public record PotentialSupplierResponseDto(
        Long annualTurnover,
        String country,
        Integer duns,
        String name,
        SupplierStatusDto status,
        SustainabilityRatingDto sustainabilityRating,
        Double score
) {
}

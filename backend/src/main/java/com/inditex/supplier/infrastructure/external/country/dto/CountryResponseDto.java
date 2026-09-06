package com.inditex.supplier.infrastructure.external.country.dto;

/**
 * 1:1 with the country service's OpenAPI schema {@code Country}
 * ({@code itx-iop_tech-supplier_flow-country-openapi3_1.yaml}).
 */
public record CountryResponseDto(
        String name,
        boolean isBanned
) {
}

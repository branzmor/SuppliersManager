package com.inditex.supplier.infrastructure.web.dto;

import java.util.List;

/**
 * Response body for {@code GET /suppliers/potential}, 1:1 with OpenAPI schema
 * {@code PotentialSuppliers}.
 */
public record PotentialSuppliersResponseDto(
        List<PotentialSupplierResponseDto> data,
        PaginationDto pagination
) {
}

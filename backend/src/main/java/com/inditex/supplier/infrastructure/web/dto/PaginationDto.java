package com.inditex.supplier.infrastructure.web.dto;

/**
 * 1:1 with OpenAPI schema {@code Pagination}.
 */
public record PaginationDto(
        Integer limit,
        Integer offset,
        Integer total
) {
}

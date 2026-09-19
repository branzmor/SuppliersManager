package com.supplier.management.infrastructure.web.dto;

/**
 * 1:1 with OpenAPI schema {@code Pagination}.
 */
public record PaginationDto(
        Integer limit,
        Integer offset,
        Integer total
) {
}

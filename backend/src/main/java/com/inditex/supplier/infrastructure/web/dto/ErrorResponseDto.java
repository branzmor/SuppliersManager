package com.inditex.supplier.infrastructure.web.dto;

/**
 * 1:1 with OpenAPI schema {@code Error}, used by 400/409/422 responses.
 */
public record ErrorResponseDto(
        String info
) {
}

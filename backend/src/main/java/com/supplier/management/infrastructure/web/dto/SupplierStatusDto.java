package com.supplier.management.infrastructure.web.dto;

/**
 * 1:1 with OpenAPI schema {@code Supplier.status} enum {@code [Active, Disqualified]}.
 *
 * <p>Deliberately only 2 values, unlike the 5-value internal {@code SupplierStatus} — the mapping
 * from internal to this DTO lives exclusively in {@code infrastructure.web.mapper.SupplierWebMapper}
 * (see {@code SOLUTION.md} §"API pública vs. estado interno"). Never add {@code ON_PROBATION} here.
 */
public enum SupplierStatusDto {
    Active, Disqualified
}

package com.inditex.supplier.domain.model;

/**
 * DUNS (Data Universal Numbering System) — the identity of a {@link SupplierRecord}.
 *
 * <p>Range constrained to the 9-digit format used by the API contract
 * ({@code PathDuns} parameter in the main OpenAPI spec: {@code minimum: 100000000},
 * {@code maximum: 999999999}).
 *
 * @param value the 9-digit DUNS number
 */
public record Duns(int value) {

    private static final int MIN = 100_000_000;
    private static final int MAX = 999_999_999;

    public Duns {
        // TODO: validate value is between MIN and MAX (inclusive), throw IllegalArgumentException otherwise.
    }
}

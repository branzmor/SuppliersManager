package com.inditex.supplier.infrastructure.web.controller;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * TODO coverage — {@code @WebMvcTest(SupplierController.class)} with mocked use cases:
 * <ul>
 *   <li>{@code GET /suppliers/potential?rate=250} → 200 with {@code data}/{@code pagination}.</li>
 *   <li>{@code GET /suppliers/potential?rate=249} → 400 (below minimum, Bean Validation).</li>
 *   <li>{@code GET /suppliers/potential?rate=250&limit=11} → 400 ({@code limit} max is 10).</li>
 *   <li>{@code GET /suppliers/{duns}} found → 200 with {@code status} as {@code Active} or
 *       {@code Disqualified} only (never {@code OnProbation}); not visible/not found → 404.</li>
 *   <li>{@code POST /suppliers/{duns}/ban} → 204 / 404 / 409.</li>
 * </ul>
 */
class SupplierControllerTest {

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void potentialSuppliersReturns200WithPagination() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void potentialSuppliersReturns400WhenRateBelowMinimum() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void potentialSuppliersReturns400WhenLimitAboveMaximum() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void getSupplierReturns404WhenNotVisible() {
    }

    @Test
    @Disabled("TODO: implement - see class javadoc")
    void banSupplierReturns409WhenNotOnProbation() {
    }
}

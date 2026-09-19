package com.supplier.management.application.port.out;

import java.util.List;

/**
 * Result of {@link SupplierRepositoryPort#findPotentialSuppliers}.
 *
 * @param suppliers the page of results (already scored, bonus-applied and ordered by the query),
 *     size &lt;= {@code limit}
 * @param totalCount total number of matching suppliers across all pages (OpenAPI
 *     {@code Pagination.total}) — computed via a {@code COUNT(*)} companion query or a window
 *     function, never by loading and counting the full result set in memory
 */
public record PotentialSuppliersPage(List<ScoredSupplier> suppliers, long totalCount) {
}

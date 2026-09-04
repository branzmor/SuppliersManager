package com.inditex.supplier.application.port.out;

import com.inditex.supplier.domain.model.SupplierRecord;

/**
 * A {@link SupplierRecord} together with the {@code score} computed by the potential-suppliers
 * SQL query (README §"Potential Suppliers"):
 * <pre>
 *   score = annual_turnover * 0.1 * rating_constant * bonus
 * </pre>
 * The score is intentionally not a field on {@link SupplierRecord} itself — it only exists in
 * the context of a given order {@code rate} and is not part of the aggregate's own state.
 *
 * @param record the underlying supplier aggregate
 * @param score the calculated score, already including the small-supplier bonus if applicable
 */
public record ScoredSupplier(SupplierRecord record, double score) {
}

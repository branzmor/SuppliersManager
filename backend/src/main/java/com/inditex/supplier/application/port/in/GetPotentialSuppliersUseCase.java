package com.inditex.supplier.application.port.in;

import com.inditex.supplier.application.port.out.ScoredSupplier;

import java.util.List;

/**
 * Use case for {@code GET /suppliers/potential} (OpenAPI {@code operationId: potentialSuppliers}).
 *
 * <p>Delegates the actual filtering/scoring/bonus/pagination entirely to
 * {@code SupplierRepositoryPort#findPotentialSuppliers} (must run in SQL — see that port's
 * javadoc on the 100k-1M volume requirement). This use case is a thin orchestration layer: input
 * validation of {@code rate}/{@code limit}/{@code offset} beyond what Bean Validation already
 * covers on the DTO, plus translating the port result into the use case's own result shape.
 */
public interface GetPotentialSuppliersUseCase {

    Result getPotentialSuppliers(Query query);

    record Query(long rate, int limit, int offset) {
    }

    record Result(List<ScoredSupplier> suppliers, int limit, int offset, long total) {
    }
}

package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.in.GetPotentialSuppliersUseCase;
import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TODO: implement. Thin pass-through to {@code SupplierRepositoryPort#findPotentialSuppliers} —
 * all filtering/scoring/bonus/ordering/pagination happens in SQL there, not here.
 */
@Service
public class GetPotentialSuppliersService implements GetPotentialSuppliersUseCase {

    private final SupplierRepositoryPort supplierRepositoryPort;

    public GetPotentialSuppliersService(SupplierRepositoryPort supplierRepositoryPort) {
        this.supplierRepositoryPort = supplierRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Result getPotentialSuppliers(Query query) {
        throw new UnsupportedOperationException("TODO");
    }
}

package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.in.RestrictSupplierUseCase;
import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extension stub (see {@code SupplierRecord#restrict} and {@code SOLUTION.md}) — not wired to any
 * controller, no OpenAPI endpoint exists yet.
 */
@Service
public class RestrictSupplierService implements RestrictSupplierUseCase {

    private final SupplierRepositoryPort supplierRepositoryPort;

    public RestrictSupplierService(SupplierRepositoryPort supplierRepositoryPort) {
        this.supplierRepositoryPort = supplierRepositoryPort;
    }

    @Override
    @Transactional
    public void restrict(int duns) {
        throw new UnsupportedOperationException("TODO");
    }
}

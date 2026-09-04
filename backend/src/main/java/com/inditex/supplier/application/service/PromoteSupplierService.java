package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.in.PromoteSupplierUseCase;
import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import com.inditex.supplier.domain.exception.SupplierRecordNotFoundException;
import com.inditex.supplier.domain.model.Duns;
import com.inditex.supplier.domain.model.SupplierRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extension stub (see {@code SupplierRecord#promote} and {@code SOLUTION.md}) — not wired to any
 * controller, no OpenAPI endpoint exists yet.
 */
@Service
public class PromoteSupplierService implements PromoteSupplierUseCase {

    private final SupplierRepositoryPort supplierRepositoryPort;

    public PromoteSupplierService(SupplierRepositoryPort supplierRepositoryPort) {
        this.supplierRepositoryPort = supplierRepositoryPort;
    }

    @Override
    @Transactional
    public void promote(int duns) {
        Duns supplierDuns = new Duns(duns);
        SupplierRecord record = supplierRepositoryPort.findByDuns(supplierDuns)
                .orElseThrow(() -> new SupplierRecordNotFoundException(supplierDuns));
        record.promote();
        supplierRepositoryPort.save(record);
    }
}

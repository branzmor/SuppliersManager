package com.supplier.management.application.service;

import com.supplier.management.application.port.in.PromoteSupplierUseCase;
import com.supplier.management.application.port.out.SupplierRepositoryPort;
import com.supplier.management.domain.exception.SupplierRecordNotFoundException;
import com.supplier.management.domain.model.Duns;
import com.supplier.management.domain.model.SupplierRecord;
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

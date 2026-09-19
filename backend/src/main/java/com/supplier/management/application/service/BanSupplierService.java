package com.supplier.management.application.service;

import com.supplier.management.application.port.in.BanSupplierUseCase;
import com.supplier.management.application.port.out.SupplierRepositoryPort;
import com.supplier.management.domain.exception.SupplierRecordNotFoundException;
import com.supplier.management.domain.model.Duns;
import com.supplier.management.domain.model.SupplierRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Looks up by DUNS (throws {@code SupplierRecordNotFoundException} if absent), delegates to
 * {@code SupplierRecord#ban}, saves.
 */
@Service
public class BanSupplierService implements BanSupplierUseCase {

    private final SupplierRepositoryPort supplierRepositoryPort;

    public BanSupplierService(SupplierRepositoryPort supplierRepositoryPort) {
        this.supplierRepositoryPort = supplierRepositoryPort;
    }

    @Override
    @Transactional
    public void ban(int duns) {
        Duns supplierDuns = new Duns(duns);
        SupplierRecord record = supplierRepositoryPort.findByDuns(supplierDuns)
                .orElseThrow(() -> new SupplierRecordNotFoundException(supplierDuns));
        record.ban();
        supplierRepositoryPort.save(record);
    }
}

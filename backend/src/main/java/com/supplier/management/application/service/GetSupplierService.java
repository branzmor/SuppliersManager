package com.supplier.management.application.service;

import com.supplier.management.application.port.in.GetSupplierUseCase;
import com.supplier.management.application.port.out.SupplierRepositoryPort;
import com.supplier.management.domain.model.Duns;
import com.supplier.management.domain.model.SupplierRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Looks up by DUNS, then filters with {@code SupplierRecord#isVisibleAsSupplier} — returns
 * empty if the record exists but is not visible as a supplier (e.g. it is still a plain
 * CANDIDATE).
 */
@Service
public class GetSupplierService implements GetSupplierUseCase {

    private final SupplierRepositoryPort supplierRepositoryPort;

    public GetSupplierService(SupplierRepositoryPort supplierRepositoryPort) {
        this.supplierRepositoryPort = supplierRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<SupplierRecord> getByDuns(int duns) {
        return supplierRepositoryPort.findByDuns(new Duns(duns)).filter(SupplierRecord::isVisibleAsSupplier);
    }
}

package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.in.GetSupplierUseCase;
import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import com.inditex.supplier.domain.model.Duns;
import com.inditex.supplier.domain.model.SupplierRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TODO: implement. Look up by DUNS, then filter with
 * {@code SupplierRecord#isVisibleAsSupplier} — return empty if the record exists but is not
 * visible as a supplier (e.g. it is still a plain CANDIDATE).
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

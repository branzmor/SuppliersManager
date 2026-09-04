package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.in.BanSupplierUseCase;
import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TODO: implement. Look up by DUNS (throw {@code SupplierRecordNotFoundException} if absent),
 * delegate to {@code SupplierRecord#ban}, save.
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
        throw new UnsupportedOperationException("TODO");
    }
}

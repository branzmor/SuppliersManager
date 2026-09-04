package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.in.RefuseCandidateUseCase;
import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TODO: implement. Look up by DUNS (throw {@code SupplierRecordNotFoundException} if absent),
 * delegate to {@code SupplierRecord#refuse}, save.
 */
@Service
public class RefuseCandidateService implements RefuseCandidateUseCase {

    private final SupplierRepositoryPort supplierRepositoryPort;

    public RefuseCandidateService(SupplierRepositoryPort supplierRepositoryPort) {
        this.supplierRepositoryPort = supplierRepositoryPort;
    }

    @Override
    @Transactional
    public void refuse(int duns) {
        throw new UnsupportedOperationException("TODO");
    }
}

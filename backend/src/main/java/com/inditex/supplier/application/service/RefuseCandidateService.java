package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.in.RefuseCandidateUseCase;
import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import com.inditex.supplier.domain.exception.SupplierRecordNotFoundException;
import com.inditex.supplier.domain.model.Duns;
import com.inditex.supplier.domain.model.SupplierRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Looks up by DUNS (throws {@code SupplierRecordNotFoundException} if absent), delegates to
 * {@code SupplierRecord#refuse}, saves.
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
        Duns candidateDuns = new Duns(duns);
        SupplierRecord record = supplierRepositoryPort.findByDuns(candidateDuns)
                .orElseThrow(() -> new SupplierRecordNotFoundException(candidateDuns));
        record.refuse();
        supplierRepositoryPort.save(record);
    }
}

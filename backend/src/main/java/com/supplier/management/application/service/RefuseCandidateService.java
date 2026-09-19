package com.supplier.management.application.service;

import com.supplier.management.application.port.in.RefuseCandidateUseCase;
import com.supplier.management.application.port.out.SupplierRepositoryPort;
import com.supplier.management.domain.exception.SupplierRecordNotFoundException;
import com.supplier.management.domain.model.Duns;
import com.supplier.management.domain.model.SupplierRecord;
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

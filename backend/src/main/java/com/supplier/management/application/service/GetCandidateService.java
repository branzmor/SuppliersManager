package com.supplier.management.application.service;

import com.supplier.management.application.port.in.GetCandidateUseCase;
import com.supplier.management.application.port.out.SupplierRepositoryPort;
import com.supplier.management.domain.model.Duns;
import com.supplier.management.domain.model.SupplierRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Looks up by DUNS, then filters with {@code SupplierRecord#isVisibleAsCandidate} — returns
 * empty if the record exists but is not visible as a candidate (e.g. it became ACTIVE).
 */
@Service
public class GetCandidateService implements GetCandidateUseCase {

    private final SupplierRepositoryPort supplierRepositoryPort;

    public GetCandidateService(SupplierRepositoryPort supplierRepositoryPort) {
        this.supplierRepositoryPort = supplierRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<SupplierRecord> getByDuns(int duns) {
        return supplierRepositoryPort.findByDuns(new Duns(duns)).filter(SupplierRecord::isVisibleAsCandidate);
    }
}

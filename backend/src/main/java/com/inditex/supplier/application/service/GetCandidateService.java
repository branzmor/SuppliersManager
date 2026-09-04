package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.in.GetCandidateUseCase;
import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import com.inditex.supplier.domain.model.SupplierRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TODO: implement. Look up by DUNS, then filter with
 * {@code SupplierRecord#isVisibleAsCandidate} — return empty if the record exists but is not
 * visible as a candidate (e.g. it became ACTIVE).
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
        throw new UnsupportedOperationException("TODO");
    }
}

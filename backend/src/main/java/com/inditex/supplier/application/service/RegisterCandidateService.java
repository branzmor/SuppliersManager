package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.in.RegisterCandidateUseCase;
import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import com.inditex.supplier.domain.model.SupplierRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TODO: implement.
 * <ol>
 *   <li>Look up {@code SupplierRepositoryPort#findByDuns}.</li>
 *   <li>If present and {@code BANNED} → throw {@code SupplierBannedException}.</li>
 *   <li>If present and any other status → throw {@code CandidateAlreadyExistsException}
 *       (covers {@code REFUSED} too, per the no-reapply decision).</li>
 *   <li>Otherwise build via {@code SupplierRecord#apply} and {@code save}.</li>
 * </ol>
 */
@Service
public class RegisterCandidateService implements RegisterCandidateUseCase {

    private final SupplierRepositoryPort supplierRepositoryPort;

    public RegisterCandidateService(SupplierRepositoryPort supplierRepositoryPort) {
        this.supplierRepositoryPort = supplierRepositoryPort;
    }

    @Override
    @Transactional
    public SupplierRecord register(RegisterCandidateCommand command) {
        throw new UnsupportedOperationException("TODO");
    }
}

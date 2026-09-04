package com.inditex.supplier.application.service;

import com.inditex.supplier.application.port.in.RegisterCandidateUseCase;
import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import com.inditex.supplier.domain.exception.CandidateAlreadyExistsException;
import com.inditex.supplier.domain.exception.SupplierBannedException;
import com.inditex.supplier.domain.model.AnnualTurnover;
import com.inditex.supplier.domain.model.CountryCode;
import com.inditex.supplier.domain.model.Duns;
import com.inditex.supplier.domain.model.SupplierRecord;
import com.inditex.supplier.domain.model.SupplierStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
        Duns duns = new Duns(command.duns());
        Optional<SupplierRecord> existing = supplierRepositoryPort.findByDuns(duns);
        if (existing.isPresent()) {
            if (existing.get().status() == SupplierStatus.BANNED) {
                throw new SupplierBannedException(duns);
            }
            throw new CandidateAlreadyExistsException(duns);
        }
        SupplierRecord candidate = SupplierRecord.apply(
                duns, command.name(), new CountryCode(command.country()), new AnnualTurnover(command.annualTurnover()));
        return supplierRepositoryPort.save(candidate);
    }
}

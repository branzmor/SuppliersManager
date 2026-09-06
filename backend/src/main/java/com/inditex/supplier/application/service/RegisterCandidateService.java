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
 * Implements {@link RegisterCandidateUseCase}:
 * <ol>
 *   <li>Look up {@code SupplierRepositoryPort#findByDuns}.</li>
 *   <li>If present and {@code BANNED} → throw {@code SupplierBannedException}.</li>
 *   <li>If present and {@code REFUSED} → delegate to {@code SupplierRecord#reapply} and save.</li>
 *   <li>If present and any other status → throw {@code CandidateAlreadyExistsException}.</li>
 *   <li>Otherwise build via {@code SupplierRecord#apply} and {@code save}.</li>
 * </ol>
 *
 * <p>{@code supplierRepositoryPort.save} is also the last line of defense against a concurrent
 * insert for the same DUNS racing past the {@code findByDuns} check above (the {@code UNIQUE(duns)}
 * constraint at the database level) — see {@code SupplierPersistenceAdapter}, which translates
 * that race into the same {@code CandidateAlreadyExistsException}/{@code SupplierBannedException}
 * this service already throws for the non-racing case.
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
        CountryCode country = new CountryCode(command.country());
        AnnualTurnover annualTurnover = new AnnualTurnover(command.annualTurnover());

        Optional<SupplierRecord> existing = supplierRepositoryPort.findByDuns(duns);
        if (existing.isPresent()) {
            SupplierRecord record = existing.get();
            if (record.status() == SupplierStatus.BANNED) {
                throw new SupplierBannedException(duns);
            }
            if (record.status() == SupplierStatus.REFUSED) {
                record.reapply(command.name(), country, annualTurnover);
                return supplierRepositoryPort.save(record);
            }
            throw new CandidateAlreadyExistsException(duns);
        }
        SupplierRecord candidate = SupplierRecord.apply(duns, command.name(), country, annualTurnover);
        return supplierRepositoryPort.save(candidate);
    }
}

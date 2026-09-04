package com.inditex.supplier.infrastructure.persistence.adapter;

import com.inditex.supplier.application.port.out.PotentialSuppliersPage;
import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import com.inditex.supplier.domain.model.Duns;
import com.inditex.supplier.domain.model.SupplierRecord;
import com.inditex.supplier.infrastructure.persistence.entity.SupplierRecordEntity;
import com.inditex.supplier.infrastructure.persistence.mapper.SupplierPersistenceMapper;
import com.inditex.supplier.infrastructure.persistence.repository.SupplierRecordJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Implements {@link SupplierRepositoryPort} on top of Spring Data JPA.
 */
@Component
public class SupplierPersistenceAdapter implements SupplierRepositoryPort {

    private final SupplierRecordJpaRepository jpaRepository;
    private final SupplierPersistenceMapper persistenceMapper;

    public SupplierPersistenceAdapter(SupplierRecordJpaRepository jpaRepository,
                                       SupplierPersistenceMapper persistenceMapper) {
        this.jpaRepository = jpaRepository;
        this.persistenceMapper = persistenceMapper;
    }

    @Override
    public Optional<SupplierRecord> findByDuns(Duns duns) {
        return jpaRepository.findByDuns(duns.value()).map(persistenceMapper::toDomain);
    }

    @Override
    public SupplierRecord save(SupplierRecord record) {
        SupplierRecordEntity entity = jpaRepository.findByDuns(record.duns().value())
                .map(existing -> {
                    persistenceMapper.updateEntity(existing, record);
                    return existing;
                })
                .orElseGet(() -> persistenceMapper.toEntity(record));
        return persistenceMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public PotentialSuppliersPage findPotentialSuppliers(long rate, int limit, int offset) {
        // TODO: delegate to jpaRepository.findPotentialSuppliersRaw + countPotentialSuppliers,
        // map rows to ScoredSupplier (score already computed in SQL).
        throw new UnsupportedOperationException("TODO");
    }
}

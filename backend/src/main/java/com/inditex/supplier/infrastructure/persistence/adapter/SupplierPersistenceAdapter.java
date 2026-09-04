package com.inditex.supplier.infrastructure.persistence.adapter;

import com.inditex.supplier.application.port.out.PotentialSuppliersPage;
import com.inditex.supplier.application.port.out.ScoredSupplier;
import com.inditex.supplier.application.port.out.SupplierRepositoryPort;
import com.inditex.supplier.domain.model.AnnualTurnover;
import com.inditex.supplier.domain.model.CountryCode;
import com.inditex.supplier.domain.model.Duns;
import com.inditex.supplier.domain.model.SupplierRecord;
import com.inditex.supplier.domain.model.SupplierStatus;
import com.inditex.supplier.domain.model.SustainabilityRating;
import com.inditex.supplier.infrastructure.persistence.entity.SupplierRecordEntity;
import com.inditex.supplier.infrastructure.persistence.mapper.SupplierPersistenceMapper;
import com.inditex.supplier.infrastructure.persistence.repository.PotentialSupplierProjection;
import com.inditex.supplier.infrastructure.persistence.repository.SupplierRecordJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
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
        List<PotentialSupplierProjection> rows = jpaRepository.findPotentialSuppliersRaw(rate, limit, offset);
        long total = jpaRepository.countPotentialSuppliers(rate);
        List<ScoredSupplier> suppliers = rows.stream().map(this::toScoredSupplier).toList();
        return new PotentialSuppliersPage(suppliers, total);
    }

    private ScoredSupplier toScoredSupplier(PotentialSupplierProjection row) {
        SupplierRecord record = SupplierRecord.reconstitute(
                new Duns(row.getDuns()),
                row.getName(),
                new CountryCode(row.getCountry()),
                new AnnualTurnover(row.getAnnualTurnover()),
                SupplierStatus.valueOf(row.getStatus()),
                SustainabilityRating.valueOf(row.getSustainabilityRating()));
        return new ScoredSupplier(record, row.getScore());
    }
}

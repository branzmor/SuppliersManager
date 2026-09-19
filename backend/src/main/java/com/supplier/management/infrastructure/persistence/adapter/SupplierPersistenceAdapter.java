package com.supplier.management.infrastructure.persistence.adapter;

import com.supplier.management.application.port.out.PotentialSuppliersPage;
import com.supplier.management.application.port.out.ScoredSupplier;
import com.supplier.management.application.port.out.SupplierRepositoryPort;
import com.supplier.management.domain.exception.CandidateAlreadyExistsException;
import com.supplier.management.domain.exception.SupplierBannedException;
import com.supplier.management.domain.model.AnnualTurnover;
import com.supplier.management.domain.model.CountryCode;
import com.supplier.management.domain.model.Duns;
import com.supplier.management.domain.model.SupplierRecord;
import com.supplier.management.domain.model.SupplierStatus;
import com.supplier.management.domain.model.SustainabilityRating;
import com.supplier.management.infrastructure.persistence.entity.SupplierRecordEntity;
import com.supplier.management.infrastructure.persistence.mapper.SupplierPersistenceMapper;
import com.supplier.management.infrastructure.persistence.repository.PotentialSupplierProjection;
import com.supplier.management.infrastructure.persistence.repository.SupplierRecordJpaRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Implements {@link SupplierRepositoryPort} on top of Spring Data JPA.
 */
@Component
public class SupplierPersistenceAdapter implements SupplierRepositoryPort {

    private static final String DUNS_UNIQUE_CONSTRAINT_NAME = "uk_supplier_record_duns";

    private final SupplierRecordJpaRepository jpaRepository;
    private final SupplierPersistenceMapper persistenceMapper;
    private final TransactionTemplate requiresNewTransactionTemplate;

    public SupplierPersistenceAdapter(SupplierRecordJpaRepository jpaRepository,
                                       SupplierPersistenceMapper persistenceMapper,
                                       PlatformTransactionManager transactionManager) {
        this.jpaRepository = jpaRepository;
        this.persistenceMapper = persistenceMapper;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        // Programmatic, not @Transactional (see HexagonalArchitectureTest#onlyApplicationServiceUsesTransactional)
        // - and it has to be REQUIRES_NEW specifically, see resolveConstraintViolation javadoc.
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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
        try {
            // saveAndFlush (not save) so a version conflict or a racing duplicate-DUNS insert
            // surfaces here, synchronously, rather than being deferred to whenever the enclosing
            // @Transactional service method's transaction happens to commit.
            return persistenceMapper.toDomain(jpaRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            throw resolveConstraintViolation(record.duns(), ex);
        }
    }

    /**
     * Last line of defense against a concurrent {@code INSERT} for the same DUNS racing past the
     * caller's own {@code findByDuns} existence check (see {@code RegisterCandidateService}): the
     * {@code uk_supplier_record_duns} unique constraint rejects the loser at the database level,
     * which must be translated into the same business exception the non-racing case already
     * throws, not left to bubble up as an unmapped 500.
     *
     * <p>Deliberately narrow: only a {@link DataIntegrityViolationException} whose root cause is
     * specifically this constraint is translated; any other integrity violation is rethrown
     * as-is rather than being swallowed into a misleading "candidate already exists".
     *
     * <p><strong>Must re-read the winning row in a brand-new transaction ({@code PROPAGATION_
     * REQUIRES_NEW}), never the current one.</strong> {@code save()} is normally called from
     * inside an outer {@code @Transactional} service method, so the failed {@code saveAndFlush}
     * above has already left that transaction's persistence context poisoned — Hibernate does not
     * discard the failed pending insert, so any further operation against the same session
     * (including a plain read) re-triggers the identical auto-flush failure instead of running.
     * A first version of this method queried through the ambient transaction and turned every
     * racing insert into an unmapped 500 instead of the intended 409 — found only by actually
     * firing two concurrent {@code POST /candidates} at the real running stack, not by the
     * adapter-level test alone (which happened to call {@code save()} standalone, outside any
     * enclosing transaction, and so never exercised this path). See
     * {@code RegisterCandidateServiceConcurrencyIntegrationTest}.
     */
    private RuntimeException resolveConstraintViolation(Duns duns, DataIntegrityViolationException ex) {
        if (!violatesDunsUniqueConstraint(ex)) {
            return ex;
        }
        SupplierStatus winningStatus = requiresNewTransactionTemplate.execute(status ->
                jpaRepository.findByDuns(duns.value())
                        .map(SupplierRecordEntity::getStatus)
                        .map(s -> SupplierStatus.valueOf(s.name()))
                        .orElse(null));
        return winningStatus == SupplierStatus.BANNED
                ? new SupplierBannedException(duns)
                : new CandidateAlreadyExistsException(duns);
    }

    private boolean violatesDunsUniqueConstraint(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && DUNS_UNIQUE_CONSTRAINT_NAME.equals(constraintViolation.getConstraintName())) {
                return true;
            }
        }
        return false;
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

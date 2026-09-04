package com.inditex.supplier.infrastructure.persistence.mapper;

import com.inditex.supplier.domain.model.SupplierRecord;
import com.inditex.supplier.infrastructure.persistence.entity.SupplierRecordEntity;
import org.springframework.stereotype.Component;

/**
 * Maps between the {@link SupplierRecord} domain aggregate and {@link SupplierRecordEntity}.
 *
 * <p>{@code toDomain} must use {@code SupplierRecord#reconstitute} (never {@code #apply}) since
 * the entity already represents a persisted, possibly-transitioned state.
 */
@Component
public class SupplierPersistenceMapper {

    public SupplierRecord toDomain(SupplierRecordEntity entity) {
        throw new UnsupportedOperationException("TODO");
    }

    public SupplierRecordEntity toEntity(SupplierRecord record) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Applies the mutable fields of {@code record} onto an already-managed {@code entity}
     * (update path), so JPA dirty-checking flushes only the changed columns instead of a full
     * replace-by-id.
     */
    public void updateEntity(SupplierRecordEntity entity, SupplierRecord record) {
        throw new UnsupportedOperationException("TODO");
    }
}
